package com.demo.ncnndemo;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.NotificationManagerCompat;

import android.app.NotificationManager;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.view.View;
import com.demo.ncnndemo.databinding.ActivityMainBinding;

public class MainActivity extends AppCompatActivity {

    // Used to load the 'ncnndemo' library on application startup.
    static {
        System.loadLibrary("ncnndemo");
    }

    private Integer SIZE = 40;

    private ActivityMainBinding binding;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        initView();
    }

    private void initView() {
        try {
            PytorchRepository.getInstance().init(getApplicationContext());
            ToastUtil.showToast(getApplicationContext(),"模型初始化成功");
        } catch (Exception e) {
            ToastUtil.showToast(getApplicationContext(),"模型初始化失败");
        }

        if (!PermissionUtils.isNotificationPermission(this)){
            DefaultDialog defaultDialog = new DefaultDialog(this, "通知权限", "请在设置中授权通知权限，用于声音分类监测", new DefaultDialog.onClickListener() {
                @Override
                public void onConfirmCLick() {
                    PermissionUtils.reqNotificationPermission(getApplicationContext());
                }

                @Override
                void onCancelCLick() {
                    ToastUtil.showToast(getApplicationContext(),"没有通知权限，请在设置中打开通知权限");
                }
            });
            defaultDialog.show();
        }

        binding.assetsAudioClassifyLinearLayout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                showAssetsAudioClassifyActivity();
            }
        });

        binding.recordAudioClassifyLinearLayout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                showRecordAudioClassifyActivity();
            }
        });

        binding.choseAudioFileClassify.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                showChoseAudioFileClassifyActivity();
            }
        });

        binding.openAppFile.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                showAppFileActivity();
            }
        });
    }

    private void showAppFileActivity() {
        Intent intent = new Intent(this, AppFileActivity.class);
        startActivity(intent);
    }

    private void showChoseAudioFileClassifyActivity() {
        Intent intent = new Intent(this, ChoseAudioFileClassifyActivity.class);
        startActivity(intent);
    }

    private void showAssetsAudioClassifyActivity() {
        Intent intent = new Intent(this, AssetsAudioClassify.class);
        startActivity(intent);
    }

    private void showRecordAudioClassifyActivity() {
        Intent intent = new Intent(this, RecordAudioClassifyActivity.class);
        startActivity(intent);
    }

}