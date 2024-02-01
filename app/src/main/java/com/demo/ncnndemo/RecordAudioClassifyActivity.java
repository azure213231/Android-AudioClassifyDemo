package com.demo.ncnndemo;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.app.NotificationManagerCompat;
import androidx.core.content.ContextCompat;

import android.Manifest;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.content.pm.PackageManager;
import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaRecorder;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.IBinder;
import android.os.PowerManager;
import android.provider.Settings;
import android.util.Log;
import android.view.View;

import com.demo.ncnndemo.databinding.ActivityRecordAudioClassifyBinding;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.text.DecimalFormat;
import java.util.ArrayList;

public class RecordAudioClassifyActivity extends AppCompatActivity {

    ActivityRecordAudioClassifyBinding binding;

    private static final int SAMPLE_RATE = 16000;
    private static final int CHANNEL_CONFIG = AudioFormat.CHANNEL_IN_MONO;
    private static final int AUDIO_FORMAT = AudioFormat.ENCODING_PCM_32BIT;
    private static final int BUFFER_SIZE = AudioRecord.getMinBufferSize(SAMPLE_RATE, CHANNEL_CONFIG, AUDIO_FORMAT);
    private static final int RECORD_AUDIO_PERMISSION_CODE = 1;
    private static final int STORAGE_PERMISSION_REQUEST_CODE = 2;
    private AudioClassifyService audioClassifyService;
    private boolean isBound = false;
    private static final String TAG = "RecordAudioClassifyActivity";

    // ServiceConnection 用于处理与服务的连接和断开连接
    private ServiceConnection serviceConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            AudioClassifyService.AudioClassifyBinder binder = (AudioClassifyService.AudioClassifyBinder) service;
            audioClassifyService = binder.getService();
            isBound = true;

            Log.d(TAG, "Service connected");
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            audioClassifyService = null;
            isBound = false;

            Log.d(TAG, "Service disconnected");
        }
    };

    private BroadcastReceiver receiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            // 处理接收到的广播，更新 UI 或执行其他操作
            String name = intent.getStringExtra("name");
            String data = intent.getStringExtra("data");
            updateUI(name,data);
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityRecordAudioClassifyBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        //注册广播接收器
        IntentFilter filter = new IntentFilter("com.demo.ncnndemo.AudioClassifyService.ACTION_UPDATE_UI");
        registerReceiver(receiver, filter);

        initView();
    }

    @Override
    protected void onResume() {
        super.onResume();
    }

    @Override
    protected void onPause() {
        super.onPause();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
//        stopRecording();
        // 解绑服务
//        unbindAudioClassifyService();
        //注销广播接收器
        unregisterReceiver(receiver);
    }

    private void initView() {
        // 检查是否已经授予存储权限
//        if (ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
//            // 如果没有权限，请求权限
//            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, STORAGE_PERMISSION_REQUEST_CODE);
//        } else {
//            // 如果已经有权限，执行相应的操作
//        }

        //判断Android 13 系统上的存储权限
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            // 先判断有没有权限
            if (!Environment.isExternalStorageManager()) {
                Intent intent = new Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION);
                intent.setData(Uri.parse("package:" + getPackageName()));
                startActivityForResult(intent, STORAGE_PERMISSION_REQUEST_CODE);
            }
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            // 先判断有没有权限
            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED &&
                    ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED) {
            } else {
                ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.READ_EXTERNAL_STORAGE, Manifest.permission.WRITE_EXTERNAL_STORAGE}, STORAGE_PERMISSION_REQUEST_CODE);
            }
        }



        binding.startRecord.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                // 检查权限
                if (checkRecordAudioPermission()) {
                    // 已经授予权限
                    // 进行录音相关操作
                    bindAudioClassifyService();
                } else {
                    // 请求权限
                    requestRecordAudioPermission();
                }
            }
        });

        binding.stopRecord.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                unbindAudioClassifyService();
            }
        });
    }

    private void unbindAudioClassifyService() {
        // 解绑服务
        if (isBound) {
            unbindService(serviceConnection);
            isBound = false;
        }
    }

    private void bindAudioClassifyService() {
        // 绑定服务
        bindService(new Intent(this, AudioClassifyService.class), serviceConnection, Context.BIND_AUTO_CREATE);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (requestCode == RECORD_AUDIO_PERMISSION_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // 用户授予了录音权限
                // 进行录音相关操作
                bindAudioClassifyService();
            } else {
                // 用户拒绝了录音权限
                // 可以在这里提供一些反馈或者禁用相关功能
                ToastUtil.showToast(this,"没有录音权限，请在设置中打开录音权限");
            }
        } else if (requestCode == STORAGE_PERMISSION_REQUEST_CODE) {
            // 检查用户是否授予了写入外部存储的权限
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // 用户授予了权限，执行相应的操作
//                performOperationsWithPermission();
            } else {
                // 用户拒绝了权限，可以在这里给出相应的提示或处理
                ToastUtil.showToast(this,"没有存储权限，请在设置中打开存储权限");
            }
        }
    }

    private boolean checkRecordAudioPermission() {
        return ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO)
                == PackageManager.PERMISSION_GRANTED;
    }

    private void requestRecordAudioPermission() {
        ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.RECORD_AUDIO},
                RECORD_AUDIO_PERMISSION_CODE);
    }

    private void updateUI(String name,String data) {
        // 在这里更新 UI
        ThreadPool.runOnMainThread(new Runnable() {
            @Override
            public void run() {
                if (name.equals("startRecord")){
                    if (data.equals("GONE")){
                        binding.startRecord.setVisibility(View.GONE);
                    } else if (data.equals("VISIBLE")){
                        binding.startRecord.setVisibility(View.VISIBLE);
                    }
                } else if (name.equals("stopRecord")){
                    if (data.equals("GONE")){
                        binding.stopRecord.setVisibility(View.GONE);
                    } else if (data.equals("VISIBLE")){
                        binding.stopRecord.setVisibility(View.VISIBLE);
                    }
                } else if (name.equals("decibelsResult")){
                    binding.decibelsResult.setText(data);
                } else if (name.equals("classifyResult")){
                    binding.classifyResult.setText(data);
                } else if (name.equals("classifyResult")){
                    binding.classifyResult.setText(data);
                } else if (name.equals("classifyResult")){
                    binding.classifyResult.setText(data);
                } else if (name.equals("classifyResult")){
                    binding.classifyResult.setText(data);
                }
            }
        });
    }
}