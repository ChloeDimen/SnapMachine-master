package com.dimen.snapmachine.encoder;

import android.annotation.SuppressLint;
import android.media.MediaCodec;
import android.media.MediaCodecInfo;
import android.media.MediaCodecList;
import android.media.MediaFormat;
import android.util.Log;

import java.io.IOException;
import java.nio.ByteBuffer;

/**
 * @Author：JETIPC1 时间 :${DATA}
 * 项目名：SnapMachine
 * 包名：com.dimen.snapmachine.encoder
 * 类名：
 * 简述：解码
 */

public class Encode {

    private MediaCodec mediaCodec;
    int m_width;
    int m_height;
    byte[] m_info = null;

    private int mColorFormat;
    private MediaCodecInfo codecInfo;
    private static final String MIME_TYPE = "video/avc"; // H.264 Advanced Video
    private byte[] yuv420 = null;

    @SuppressLint("NewApi")
    public Encode(int width, int height, int framerate, int bitrate) {

        m_width = width;
        m_height = height;
        Log.v("dimen", "AvcEncoder:" + m_width + "+" + m_height);
        yuv420 = new byte[width * height * 3 / 2];

        try {
            mediaCodec = MediaCodec.createEncoderByType("video/avc");
        } catch (IOException e) {
            e.printStackTrace();
        }

        MediaFormat mediaFormat = MediaFormat.createVideoFormat("video/avc", width, height);
        mediaFormat.setInteger(MediaFormat.KEY_BIT_RATE, bitrate); //设置视频码率
        mediaFormat.setInteger(MediaFormat.KEY_FRAME_RATE, framerate); //设置视频fps
        mediaFormat.setInteger(MediaFormat.KEY_COLOR_FORMAT, MediaCodecInfo.CodecCapabilities.COLOR_FormatYUV420SemiPlanar);//COLOR_FormatSurface COLOR_FormatYUV420SemiPlanar
        mediaFormat.setInteger(MediaFormat.KEY_I_FRAME_INTERVAL, 1);//关键帧间隔时间 单位s

        mediaCodec.configure(mediaFormat, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE);
        mediaCodec.start();
        //selectCodec("video/avc");
    }

    @SuppressLint("NewApi")
    public void close() {
        try {
            mediaCodec.stop();
            mediaCodec.release();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static MediaCodecInfo selectCodec(String mimeType) {
        int numCodecs = MediaCodecList.getCodecCount();
        for (int i = 0; i < numCodecs; i++) {
            MediaCodecInfo codecInfo = MediaCodecList.getCodecInfoAt(i);

            if (!codecInfo.isEncoder()) {
                continue;
            }

            String[] types = codecInfo.getSupportedTypes();
            for (int j = 0; j < types.length; j++) {
                if (types[j].equalsIgnoreCase(mimeType)) {
                    printColorFormat(codecInfo, mimeType);
                    Log.i("selectCodec", "SelectCodec : " + codecInfo.getName());

                    return codecInfo;
                }
            }
        }
        return null;
    }

    //  2135033992对应COLOR_FormatYUV420Flexible  19  21对应COLOR_FormatYUV420SemiPlanar  2130708361
    private static void printColorFormat(MediaCodecInfo codecInfo, String mimeType) {
        MediaCodecInfo.CodecCapabilities capabilities = codecInfo.getCapabilitiesForType(mimeType);
        for (int i = 0; i < capabilities.colorFormats.length; i++) {
            int colorFormat = capabilities.colorFormats[i];
            switch (colorFormat) {
                // these are the formats we know how to handle for this testcase MediaCodecInfo.CodecCapabilities.COLOR_FormatYUV420Planar:
                case MediaCodecInfo.CodecCapabilities.COLOR_FormatYUV420PackedPlanar:
                    Log.e("sss", "COLOR_FormatYUV420PackedPlanar");
                    break;
                case MediaCodecInfo.CodecCapabilities.COLOR_FormatYUV420SemiPlanar:
                    Log.e("sss", "COLOR_FormatYUV420SemiPlanar");
                    break;
                case MediaCodecInfo.CodecCapabilities.COLOR_FormatYUV420PackedSemiPlanar:
                    Log.e("sss", "COLOR_FormatYUV420PackedSemiPlanar");
                    break;

                default:
                    Log.e("sss", "" + colorFormat);
            }
        }
    }

    @SuppressLint("NewApi")
    public int offerEncoder(byte[] input, byte[] output) {
        Log.v("dimen", "解码：数据长度=" + input.length + "  缓存空间长度=" + output.length);
        int pos = 0;
        //必须要转格式，否则录制的内容播放出来为绿屏,颜色不对
        //NV21toI420SemiPlanar(input, yuv420, m_width, m_height);

        byte[] datas = swapNV21to420sp(input, yuv420, m_width, m_height);
        try {
            ByteBuffer[] inputBuffers = mediaCodec.getInputBuffers();
            ByteBuffer[] outputBuffers = mediaCodec.getOutputBuffers();
            int inputBufferIndex = mediaCodec.dequeueInputBuffer(-1);
            if (inputBufferIndex >= 0) {
                ByteBuffer inputBuffer = inputBuffers[inputBufferIndex];
                inputBuffer.clear();
                inputBuffer.put(datas);
                mediaCodec.queueInputBuffer(inputBufferIndex, 0, datas.length, 0, 0);

            }

            MediaCodec.BufferInfo bufferInfo = new MediaCodec.BufferInfo();
            int outputBufferIndex = mediaCodec.dequeueOutputBuffer(bufferInfo, 0);

            while (outputBufferIndex >= 0) {
                ByteBuffer outputBuffer = outputBuffers[outputBufferIndex];
                byte[] outData = new byte[bufferInfo.size];
                outputBuffer.get(outData);

                if (m_info != null) {
                    System.arraycopy(outData, 0, output, pos, outData.length);
                    pos += outData.length;

                } else {//保存pps sps 只有开始时 第一个帧里有， 保存起来后面用
                    ByteBuffer spsPpsBuffer = ByteBuffer.wrap(outData);
                    Log.v("dimen", "swapNV21to420sp:outData:" + outData);
                    Log.v("dimen", "swapNV21to420sp:spsPpsBuffer:" + spsPpsBuffer);
//
                    for (int i = 0; i < outData.length; i++) {
                        Log.e("dimen", "run: get data rtpData[i]=" + i + ":" + outData[i]);//输出SPS和PPS循环
                        // 1920*1080 0 0 0 1 39 66 -32 31 -115 104,7,-128,34,126, 88,64, 0,0,3,0,64,0,0,7,-125,-60,30,-96,0,0,0,1,40,-50,50,72
                        // 1280*720 0 0 0 1 39 66 -32 31 -115 104,5,0,91,-95, 0,0, 3,0,1,0,0,3,0,30,15,16,122,-128,0,0,0,1,40,-50,50,72
                        //640*480  0 0 0 1 39 66 -32 31 -115 104 10 3 -38 16 0 0 3 0 16 0 03 1 -32 -15 7 -88 0 0 0 1 40 -50 50 72
                    }

                    if (spsPpsBuffer.getInt() == 0x00000001) {
                        m_info = new byte[outData.length];
                        System.arraycopy(outData, 0, m_info, 0, outData.length);
                    } else {
                        return -1;
                    }
                }
                if (output[4] == 0x65) {//key frame 编码器生成关键帧时只有 00 00 00 01 65 没有pps sps， 要加上
                    System.arraycopy(m_info, 0, output, 0, m_info.length);
                    System.arraycopy(outData, 0, output, m_info.length, outData.length);
                }
                mediaCodec.releaseOutputBuffer(outputBufferIndex, false);
                outputBufferIndex = mediaCodec.dequeueOutputBuffer(bufferInfo, 0);
            }

        } catch (Throwable t) {
            t.printStackTrace();
        }
        Log.v("dimen", "pos=" + pos);
        return pos;
    }


    //网友提供的，如果swapYV12toI420方法颜色不对可以试下这个方法，不同机型有不同的转码方式
    private byte[] NV21toI420SemiPlanar(byte[] nv21bytes, byte[] i420bytes, int width, int height) {
        Log.v("dimen", "NV21toI420SemiPlanar:::" + width + "+" + height);
        final int iSize = width * height;
        System.arraycopy(nv21bytes, 0, i420bytes, 0, iSize);

        for (int iIndex = 0; iIndex < iSize / 2; iIndex += 2) {
            i420bytes[iSize + iIndex / 2 + iSize / 4] = nv21bytes[iSize + iIndex]; // U
            i420bytes[iSize + iIndex / 2] = nv21bytes[iSize + iIndex + 1]; // V
        }
        return i420bytes;
    }

    //yv12 转 yuv420p  yvu -> yuv
    private void swapYV12toI420(byte[] yv12bytes, byte[] i420bytes, int width, int height) {
        Log.v("dimen", "swapYV12toI420:::" + width + "+" + height);
        Log.v("dimen", "swapYV12toI420:::" + yv12bytes.length + "+" + i420bytes.length + "+" + width * height);
        System.arraycopy(yv12bytes, 0, i420bytes, 0, width * height);
        System.arraycopy(yv12bytes, width * height + width * height / 4, i420bytes, width * height, width * height / 4);
        System.arraycopy(yv12bytes, width * height, i420bytes, width * height + width * height / 4, width * height / 4);
    }

    //public static void arraycopy(Object src,int srcPos,Object dest,int destPos,int length)
    //src:源数组；	srcPos:源数组要复制的起始位置；
    //dest:目的数组；	destPos:目的数组放置的起始位置；	length:复制的长度。
    public static void Nv21ToI420(byte[] data, byte[] dstData, int w, int h) {

        int size = w * h;
        // Y
        System.arraycopy(data, 0, dstData, 0, size);
        for (int i = 0; i < size / 4; i++) {
            dstData[size + i] = data[size + i * 2 + 1]; //U
            dstData[size + size / 4 + i] = data[size + i * 2]; //V
        }
    }

    public static void Nv21ToYuv420SP(byte[] data, byte[] dstData, int w, int h) {
        int size = w * h;
        // Y
        System.arraycopy(data, 0, dstData, 0, size);

        for (int i = 0; i < size / 4; i++) {
            dstData[size + i * 2] = data[size + i * 2 + 1]; //U
            dstData[size + i * 2 + 1] = data[size + i * 2]; //V

        }
    }

    //NV21转YUV420P
    void swapNV21to420p(byte[] input, byte[] output, int width, int height) {

        int nLenY = width * height;
        int nLenU = nLenY / 4;

        //复制Y分量
        System.arraycopy(input, 0, output, 0, width * height);

        for (int i = 0; i < nLenU; i++) {
            output[nLenY + i] = input[nLenY + 2 * i + 1];
            output[nLenY + nLenU + i] = input[nLenY + 2 * i];
        }
    }

    //NV21转YUV420SP
    private byte[] swapNV21to420sp(byte[] input, byte[] output, int width, int height) {
        Log.e("NV21转YUV420SP", "src.length= " + input.length + "  dst.length=" + output.length + "  length=" + width * height);
        //复制Y
        System.arraycopy(input, 0, output, 0, width * height);

        //交换UV
        byte temp;
        for (int i = width * height; i < input.length; i += 2) {
            output[i] = input[i + 1];
            output[i + 1] = input[i];
        }
        return output;
    }


    public void NV21ToNV12(byte[] nv21, byte[] nv12, int width, int height) {
        if (nv21 == null || nv12 == null) return;

        int framesize = width * height;
        int i = 0, j = 0;

        System.arraycopy(nv21, 0, nv12, 0, framesize);

        for (i = 0; i < framesize; i++) {
            nv12[i] = nv21[i];
        }

        for (j = 0; j < framesize / 2; j += 2) {
            nv12[framesize + j - 1] = nv21[j + framesize];
        }

        for (j = 0; j < framesize / 2; j += 2) {
            nv12[framesize + j] = nv21[j + framesize - 1];
        }
    }

}
