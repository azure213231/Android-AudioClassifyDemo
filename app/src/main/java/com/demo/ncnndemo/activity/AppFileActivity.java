package com.demo.ncnndemo.activity;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.FileProvider;
import androidx.recyclerview.widget.LinearLayoutManager;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;

import com.demo.ncnndemo.Adapter.FolderAdapter;
import com.demo.ncnndemo.databinding.ActivityAppFileBinding;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class AppFileActivity extends AppCompatActivity {

    ActivityAppFileBinding binding;
    private FolderAdapter folderAdapter;
    private List<File> folderList;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityAppFileBinding.inflate(getLayoutInflater());
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
                showAppFileActivity(folder);
            }

            @Override
            public void onFileClick(File file) {
                // 处理文件项的点击事件
                // 这里可以实现打开文件的操作
                openFile(file);
            }

        });
        binding.recyclerView.setAdapter(folderAdapter);

        binding.fileNum.setText("文件总数：" + folderList.size());
    }

    private void showAppFileActivity(File folder) {
        Intent intent = new Intent(this, AppFileActivity.class);
        intent.putExtra("folderPath", folder.getAbsolutePath());
        startActivity(intent);
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