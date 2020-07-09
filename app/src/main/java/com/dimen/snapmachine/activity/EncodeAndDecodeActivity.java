package com.dimen.snapmachine.activity;

import android.graphics.ImageFormat;
import android.hardware.Camera;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.os.StrictMode;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import com.dimen.snapmachine.R;
import com.dimen.snapmachine.base.BaseActivity;
import com.dimen.snapmachine.encoder.AvcEncoder;
import com.dimen.snapmachine.encoder.Decode;
import com.dimen.snapmachine.encoder.Encode;
import com.dimen.snapmachine.rtp.RtpSenderWrapper;
import com.dimen.snapmachine.utils.AvcUtils;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.nio.ByteBuffer;
import java.util.Base64;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;

/**
 * 时时硬编码和硬解码
 */
public class EncodeAndDecodeActivity extends BaseActivity implements SurfaceHolder.Callback, Camera.PreviewCallback {
    DatagramSocket socket;
    InetAddress address;

    Encode avcCodec;
    public Camera m_camera;
    SurfaceView   m_prevewview;
    EditText et_id_data;
    Button btn_send;
    SurfaceHolder m_surfaceHolder;
    //屏幕分辨率，每个机型不一样，机器连上adb后输入wm size可获取
    int width = 640;
    int height = 480;
    int framerate = 30;//每秒帧率
    int bitrate =  width*height*3;//编码比特率，
    private RtpSenderWrapper mRtpSenderWrapper;

    byte[] h264 = new byte[width*height*3];


    @Override
    protected void setContentView() {
        setContentView(R.layout.activity_encode_and_decode);
    }

    @Override
    protected void initView() {

        avcCodec = new Encode(width,height,framerate,bitrate);

        m_prevewview =  findViewById(R.id.SurfaceViewPlay);
        et_id_data =  findViewById(R.id.et_id_data);
        btn_send =  findViewById(R.id.btn_send);
        m_surfaceHolder = m_prevewview.getHolder(); // 绑定SurfaceView，取得SurfaceHolder对象
        m_surfaceHolder.setFixedSize(width, height); // 预览大小設置
        m_surfaceHolder.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);
        m_surfaceHolder.addCallback(this);
        if (!et_id_data.getText().toString().trim().isEmpty()) {
            mRtpSenderWrapper = new RtpSenderWrapper(et_id_data.getText().toString().trim(), 5004, false);
        }
    }

    @Override
    protected void intData() {
        //初始化参数

    }

    @Override
    protected void initListener() {
        btn_send.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (!et_id_data.getText().toString().trim().isEmpty()) {
                    mRtpSenderWrapper = new RtpSenderWrapper(et_id_data.getText().toString().trim(), 5004, false);
                }
            }
        });
    }

    @Override
    public void surfaceCreated(SurfaceHolder holder) {
        try {
            m_camera = Camera.open(0);
            //m_camera.setDisplayOrientation(270);
            m_camera.setPreviewDisplay(m_surfaceHolder);

            Camera.Parameters parameters = m_camera.getParameters();
            parameters.setPreviewSize(width, height);
            parameters.setPictureSize(width, height);

            parameters.setPreviewFormat(ImageFormat.NV21);
            m_camera.setParameters(parameters);
            m_camera.setPreviewCallback( this);
            m_camera.startPreview();
            List<Integer> previewFormats = m_camera.getParameters().getSupportedPreviewFormats();
            for (int i = 0; i < previewFormats.size(); i++) {
                Log.e("摄像头支持格式", "surfaceCreated: "+ previewFormats.get(i));
            }
        } catch (IOException e){
            e.printStackTrace();
        }
    }

    @Override
    public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {

    }

    @Override
    public void surfaceDestroyed(SurfaceHolder holder) {
        m_camera.setPreviewCallback(null);  //！！这个必须在前，不然退出出错
        m_camera.release();
        m_camera = null;
        avcCodec.close();
    }

    @Override
    public void onPreviewFrame(byte[] data, Camera camera) {

        int ret = avcCodec.offerEncoder(data, h264);
        if(ret > 0){
            //实时发送数据流
            if (mRtpSenderWrapper!=null) {
                mRtpSenderWrapper.sendAvcPacket(h264, 0, ret, 0);
            }
        }
    }


}
