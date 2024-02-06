package com.demo.ncnndemo.activity;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.FileProvider;
import androidx.recyclerview.widget.LinearLayoutManager;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;

import com.demo.ncnndemo.Adapter.FolderAdapter;
import com.demo.ncnndemo.databinding.ActivityAppFileChoosenBinding;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

//选择APP内的文件，并返回Uri
public class AppFileChosenActivity extends AppCompatActivity {

    ActivityAppFileChoosenBinding binding;
    private FolderAdapter folderAdapter;
    private List<File> folderList;
    private static final int REQUEST_APP_FILE_CODE = 1;  // 用于标识请求的常量


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityAppFileChoosenBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        initView();
    }

    private void initView() {
        // 获取传递过来的文件夹路径
        String folderPath = getIntent().getStringExtra("folderPath");
        if (folderPath != null){
            folderList = getFilesInFolder(folderPath);
        } else {
            folderList = getFoldersInAppDirectory(); // 自定义方法，获取应用目录下的文件夹列表
        }
        binding.recyclerView.setLayoutManager(new LinearLayoutManager(this));

        folderAdapter = new FolderAdapter(folderList, new FolderAdapter.OnItemClickListener() {
            @Override
            public void onFolderClick(File folder) {
                showAppFileChosenActivity(folder);
            }

            @Override
            public void onFileClick(File file) {
                // 处理文件项的点击事件
                // 这里可以实现打开文件的操作
//                openFile(file);
                //将文件uri返回给页面
                finishWithResult(file);
            }

        });
        binding.recyclerView.setAdapter(folderAdapter);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == REQUEST_APP_FILE_CODE) {
            // 处理嵌套的 Activity 返回的结果
            if (resultCode == Activity.RESULT_OK) {
                // 设置嵌套的 Activity 的返回结果
                String fileUri = data.getStringExtra("file_uri");
                Intent resultIntent = new Intent();
                resultIntent.putExtra("file_uri",fileUri );
                setResult(Activity.RESULT_OK, resultIntent);
                finish();  // 关闭当前 Activity
            } else if (resultCode == Activity.RESULT_CANCELED) {
                // 处理用户取消的情况
            }
        }
    }

    private void finishWithResult(File file) {
        Uri uri = FileProvider.getUriForFile(this, "com.demo.ncnndemo.provider", file);
        Intent resultIntent = new Intent();
        resultIntent.putExtra("file_uri", uri.toString());
        setResult(Activity.RESULT_OK, resultIntent);
        finish();  // 关闭当前 Activity
    }

    private void showAppFileChosenActivity(File folder) {
        Intent intent = new Intent(this, AppFileChosenActivity.class);
        intent.putExtra("folderPath", folder.getAbsolutePath());
        startActivityForResult(intent,REQUEST_APP_FILE_CODE);
    }

    private List<File> getFilesInFolder(String folderPath) {
        List<File> files = new ArrayList<>();
        File folder = new File(folderPath);
        File[] folderFiles = folder.listFiles();

        if (folderFiles != null) {
            files.addAll(Arrays.asList(folderFiles));
        }

        return files;
    }

    private List<File> getFoldersInAppDirectory() {
        List<File> folders = new ArrayList<>();
        File appDirectory = new File(getExternalFilesDir(null).getAbsolutePath()); // 获取应用目录
        File[] files = appDirectory.listFiles();

        if (files != null) {
            for (File file : files) {
                if (file.isDirectory()) {
                    folders.add(file);
                }
            }
        }

        return folders;
    }

    private void openFile(File file) {
        // 实现打开文件的逻辑，例如使用 Intent 打开相关应用
        // 创建一个 Intent，指定 Action 和 Data
        Intent intent = new Intent(Intent.ACTION_VIEW);
        Uri uri = FileProvider.getUriForFile(this, "com.demo.ncnndemo.provider", file);
        intent.setDataAndType(uri, "audio/wav"); // 你可以替换成适当的 MIME 类型

        // 添加读取 Uri 的权限
        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);

        // 检查是否有应用能够处理这个 Intent
        if (intent.resolveActivity(getPackageManager()) != null) {
            // 有应用能够处理这个 Intent，启动相应的应用
            startActivity(intent);
        } else {
            // 没有应用能够处理这个 Intent，显示错误信息或采取其他措施
            // 可以根据需要自定义逻辑
        }
    }
}