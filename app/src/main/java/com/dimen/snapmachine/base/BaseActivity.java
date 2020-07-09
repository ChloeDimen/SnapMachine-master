package com.dimen.snapmachine.base;

import android.content.Intent;
import android.os.Bundle;
import android.os.PersistableBundle;
import android.os.StrictMode;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;

/**
 * @Author：JETIPC1 时间 :${DATA}
 * 项目名：SnapMachine
 * 包名：com.dimen.snapmachine.base
 * 类名：
 * 简述：
 */

public abstract class BaseActivity extends AppCompatActivity {
    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
       StrictMode.setThreadPolicy(new StrictMode.ThreadPolicy.Builder()
                .detectDiskReads()
                .detectDiskWrites()
                .detectAll()   // or .detectAll() for all detectable problems
                .penaltyLog()
                .build());
        StrictMode.setVmPolicy(new StrictMode.VmPolicy.Builder()
                .detectLeakedSqlLiteObjects()
                .detectLeakedClosableObjects()
                .penaltyLog()
                .penaltyDeath()
                .build());
        super.onCreate(savedInstanceState);
        setContentView();
        initView();
        intData();
        initListener();

    }

    protected abstract void setContentView();

    protected abstract void initView();

    protected abstract void intData();

    protected abstract void initListener();

    protected void startActivity(Class<?> clazz) {
        Intent intent = new Intent(this, clazz);
        startActivity(intent);
    }
}
