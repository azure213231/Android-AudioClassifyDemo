package com.demo.ncnndemo.activity;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.FragmentActivity;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.media.AudioFormat;
import android.media.AudioRecord;
import android.os.Bundle;
import android.view.View;

import com.demo.ncnndemo.dialog.DefaultDialog;
import com.demo.ncnndemo.utils.PermissionUtils;
import com.demo.ncnndemo.utils.ServiceUtils;
import com.demo.ncnndemo.utils.ThreadPool;
import com.demo.ncnndemo.utils.ToastUtil;
import com.demo.ncnndemo.databinding.ActivityRecordAudioClassifyBinding;
import com.demo.ncnndemo.service.AudioClassifyService;

public class RecordAudioClassifyActivity extends AppCompatActivity {

    ActivityRecordAudioClassifyBinding binding;

    private static final int SAMPLE_RATE = 16000;
    private static final int CHANNEL_CONFIG = AudioFormat.CHANNEL_IN_MONO;
    private static final int AUDIO_FORMAT = AudioFormat.ENCODING_PCM_32BIT;
    private static final int BUFFER_SIZE = AudioRecord.getMinBufferSize(SAMPLE_RATE, CHANNEL_CONFIG, AUDIO_FORMAT);
    private AudioClassifyService audioClassifyService;
    private boolean isBound = false;
    private static final String TAG = "RecordAudioClassifyActivity";

    // 处理权限请求结果
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == PermissionUtils.STORAGE_PERMISSION_REQUEST_CODE) {
            // 检查权限授予结果
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // 用户授予了存储权限
            } else {
                // 用户拒绝了存储权限
                ToastUtil.showToast(getApplicationContext(),"没有存储权限，请在设置中打开存储权限");
            }
        } else if (requestCode == PermissionUtils.RECORD_AUDIO_PERMISSION_CODE) {
            // 检查权限授予结果
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // 用户授予了录音权限
                bindAudioClassifyService();
            } else {
                // 用户拒绝了录音权限
                ToastUtil.showToast(getApplicationContext(),"没有录音权限，请在设置中打开录音权限");
            }
        }
    }

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
        IntentFilter filter = new IntentFilter("com.demo.ncnndemo.service.AudioClassifyService.ACTION_UPDATE_UI");
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
        //注销广播接收器
        unregisterReceiver(receiver);
    }

    private void initView() {
        FragmentActivity fragmentActivity = this;
        if (!PermissionUtils.isExternalStoragePermission(this)){
            DefaultDialog defaultDialog = new DefaultDialog(this, "存储权限", "请在设置中存储权限，用于声音分类监测", new DefaultDialog.onClickListener() {
                @Override
                public void onConfirmCLick() {
                    PermissionUtils.reqExternalStoragePermission(fragmentActivity);
                }

                @Override
                public void onCancelCLick() {
                    ToastUtil.showToast(getApplicationContext(),"没有存储权限，请在设置中打开存储权限");
                }
            });
            defaultDialog.show();
        }

        boolean isMyServiceRunning = ServiceUtils.isServiceRunning(getApplicationContext(), AudioClassifyService.class);
        if (isMyServiceRunning) {
            // 服务正在运行
            isBound = true;
            binding.startRecord.setVisibility(View.GONE);
            binding.stopRecord.setVisibility(View.VISIBLE);
        } else {
            // 服务未运行
            isBound = false;
            binding.startRecord.setVisibility(View.VISIBLE);
            binding.stopRecord.setVisibility(View.GONE);
        }

        binding.startRecord.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                // 检查权限
                if (PermissionUtils.isRecordAudioPermission(getApplicationContext())) {
                    // 已经授予权限
                    // 进行录音相关操作
                    bindAudioClassifyService();
                } else {
                    // 请求权限
                    PermissionUtils.reqRecordAudioPermission(fragmentActivity);
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
        // 停止服务
        Intent serviceIntent = new Intent(this, AudioClassifyService.class);
        stopService(serviceIntent);
    }

    private void bindAudioClassifyService() {
        // 开启服务
        Intent serviceIntent = new Intent(this, AudioClassifyService.class);
        ContextCompat.startForegroundService(this, serviceIntent);
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
                } else if (name.equals("classifyNum")){
                    binding.classifyNum.setText("分析次数：" + data);
                }
            }
        });
    }
}