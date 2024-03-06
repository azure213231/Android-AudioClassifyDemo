package com.demo.ncnndemo.activity;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;

import com.demo.ncnndemo.dialog.DefaultDialog;
import com.demo.ncnndemo.utils.PermissionUtils;
import com.demo.ncnndemo.repository.PytorchRepository;
import com.demo.ncnndemo.utils.ThreadPool;
import com.demo.ncnndemo.utils.ToastUtil;
import com.demo.ncnndemo.databinding.ActivityMainBinding;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity {

    // Used to load the 'ncnndemo' library on application startup.
//    static {
//        System.loadLibrary("ncnndemo");
//    }

//    private Integer SIZE = 40;

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
                public void onCancelCLick() {
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

        binding.fileNsxAgc.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                showFileNsxAgcActivity();
            }
        });

        binding.fileClear.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                showFileClearDialog();
            }
        });

        try {
            PackageInfo packageInfo = getPackageManager().getPackageInfo(getPackageName(), 0);
            String versionName = packageInfo.versionName;
            binding.appVersion.setText("版本信息：" + versionName);
            // 现在你可以使用versionName来做任何你需要的事情，比如显示在UI上或者打印出来。
        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
        }
    }

    private void showFileClearDialog() {
        DefaultDialog defaultDialog = new DefaultDialog(this, "清除文件", "将清除保存的声音文件", new DefaultDialog.onClickListener() {
            @Override
            public void onConfirmCLick() {
                fileClear();
            }

            @Override
            public void onCancelCLick() {

            }
        });
        defaultDialog.show();
    }

    private void fileClear() {
        String absolutePath = getExternalFilesDir(null).getAbsolutePath();
        List<String> filePathList = new ArrayList<>();
        filePathList.add(absolutePath + "/audioClassifyNSX");
        filePathList.add(absolutePath + "/audioClassify");
        filePathList.add(absolutePath + "/PSGClassify");
        filePathList.add(absolutePath + "/NsxAgcFiles");
        for (String filePath : filePathList){
            File folder = new File(filePath);
            deleteFolder(folder);
        }
        ToastUtil.showToast(this,"清除成功", Gravity.CENTER);
    }

    public static void deleteFolder(File folder) {
        File[] files = folder.listFiles();
        if (files != null) {
            for (File file : files) {
                if (file.isDirectory()) {
                    // 递归删除子文件夹
                    deleteFolder(file);
                } else {
                    // 删除文件
                    file.delete();
                }
            }
        }
        // 删除空文件夹或者子文件夹后的空文件夹
        folder.delete();
    }

    private void showFileNsxAgcActivity() {
        Intent intent = new Intent(this, FileNsxAgcActivity.class);
        startActivity(intent);
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