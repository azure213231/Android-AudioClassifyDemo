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


        //检查通知权限
        // 在需要发送通知的地方检查并请求通知权限
        NotificationManager notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        if (!notificationManager.areNotificationsEnabled()){
            openNotificationSettings();
        }
//        if (!NotificationManagerCompat.from(this).areNotificationsEnabled()) {
//            openNotificationSettings();
//        }

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



    public void openNotificationSettings() {
        Intent intent = new Intent();

        // 根据不同的 Android 版本设置不同的 Action
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            intent.setAction("android.settings.APP_NOTIFICATION_SETTINGS");
            intent.putExtra("android.provider.extra.APP_PACKAGE", getPackageName());
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            intent.setAction("android.settings.APP_NOTIFICATION_SETTINGS");
            intent.putExtra("app_package", getPackageName());
            intent.putExtra("app_uid", getApplicationInfo().uid);
        } else {
            intent.setAction("android.settings.APPLICATION_DETAILS_SETTINGS");
            intent.addCategory(Intent.CATEGORY_DEFAULT);
            intent.setData(Uri.parse("package:" + getPackageName()));
        }

        startActivity(intent);
    }
}