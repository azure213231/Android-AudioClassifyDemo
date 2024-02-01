package com.demo.ncnndemo;

import android.Manifest;
import android.app.Activity;
import android.app.NotificationManager;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.provider.Settings;
import android.view.View;

import androidx.activity.result.ActivityResult;
import androidx.activity.result.ActivityResultCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.FragmentActivity;

import javax.security.auth.callback.Callback;

public class PermissionUtils {
    public static final Integer STORAGE_PERMISSION_REQUEST_CODE = 1;
    public static final Integer RECORD_AUDIO_PERMISSION_CODE = 2;

    public static boolean isNotificationPermission(Context context){
        //检查通知权限
        // 在需要发送通知的地方检查并请求通知权限
        NotificationManager notificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        return notificationManager.areNotificationsEnabled();
    }

    public static void reqNotificationPermission(Context context){
        Intent intent = new Intent();

        // 根据不同的 Android 版本设置不同的 Action
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            intent.setAction("android.settings.APP_NOTIFICATION_SETTINGS");
            intent.putExtra("android.provider.extra.APP_PACKAGE", context.getPackageName());
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            intent.setAction("android.settings.APP_NOTIFICATION_SETTINGS");
            intent.putExtra("app_package", context.getPackageName());
            intent.putExtra("app_uid", context.getApplicationInfo().uid);
        } else {
            intent.setAction("android.settings.APPLICATION_DETAILS_SETTINGS");
            intent.addCategory(Intent.CATEGORY_DEFAULT);
            intent.setData(Uri.parse("package:" + context.getPackageName()));
        }

        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        context.startActivity(intent);
    }

    public static boolean isExternalStoragePermission(Context context) {
        //判断Android 13 系统上的存储权限
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            return Environment.isExternalStorageManager();
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            // 先判断有没有权限
            if (ActivityCompat.checkSelfPermission(context, Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED &&
                    ContextCompat.checkSelfPermission(context, Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED) {
                return true;
            }
            return false;
        }
        return true;
    }

    public static void reqExternalStoragePermission(FragmentActivity fragmentActivity) {
        //判断Android 13 系统上的存储权限
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            Intent intent = new Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION);
            intent.setData(Uri.parse("package:" + fragmentActivity.getPackageName()));
            fragmentActivity.startActivityForResult(intent, STORAGE_PERMISSION_REQUEST_CODE);
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            // 先判断有没有权限
            ActivityCompat.requestPermissions(fragmentActivity, new String[]{Manifest.permission.READ_EXTERNAL_STORAGE, Manifest.permission.WRITE_EXTERNAL_STORAGE}, STORAGE_PERMISSION_REQUEST_CODE);
        }
    }

    public static boolean isRecordAudioPermission(Context context) {
        return ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO)
                == PackageManager.PERMISSION_GRANTED;
    }

    public static void reqRecordAudioPermission(FragmentActivity fragmentActivity) {
        ActivityCompat.requestPermissions(fragmentActivity, new String[]{Manifest.permission.RECORD_AUDIO},
                RECORD_AUDIO_PERMISSION_CODE);
    }

    public abstract static class Callback{
        public void onReqExternalStoragePermission(Boolean result) {}
        public void onReqRecordAudioPermission(Boolean result) {}
    }
}
