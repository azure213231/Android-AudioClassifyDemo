package com.demo.ncnndemo.activity;

import static android.os.SystemClock.sleep;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.FileProvider;
import androidx.recyclerview.widget.LinearLayoutManager;

import android.annotation.SuppressLint;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.provider.OpenableColumns;
import android.view.Gravity;
import android.view.View;

import com.demo.ncnndemo.Adapter.FolderAdapter;
import com.demo.ncnndemo.databinding.ActivityFileNsxAgcBinding;
import com.demo.ncnndemo.utils.AudioUtils;
import com.demo.ncnndemo.utils.ByteUtils;
import com.demo.ncnndemo.utils.ThreadPool;
import com.demo.ncnndemo.utils.ToastUtil;
import com.demo.ncnndemo.utils.WebRTCAudioUtils;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class FileNsxAgcActivity extends AppCompatActivity {

    ActivityFileNsxAgcBinding binding;
    private FolderAdapter folderAdapter;
    private List<File> folderList;
    private WebRTCAudioUtils webRTCAudioUtils;
    private Integer agcAndNsxNum = 1;
    private Integer agcInstListNum = 1;
    private static List<Long> agcInstList = new ArrayList<>();
    private Integer nsxInstListNum = 1;
    private static List<Long> nsxInstList = new ArrayList<>();
    private Integer CHUNK_SIZE = 300 * 1024;
    private boolean isAudioNsxAgc = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        binding = ActivityFileNsxAgcBinding.inflate(getLayoutInflater());
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
                showFileNsxAgcActivity(folder);
            }

            @Override
            public void onFileClick(File file) {
                // 处理文件项的点击事件
                // 这里可以实现打开文件的操作
                openFile(file);
            }

        });
        binding.recyclerView.setAdapter(folderAdapter);

        binding.startNsxAgcForAllFileButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                startFileNsxAgc(folderList);
            }
        });
    }

    private void startFileNsxAgc(List<File> folderList) {
        binding.startNsxAgcForAllFileButton.setText("处理中");
        ThreadPool.runOnThread(new Runnable() {
            @Override
            public void run() {
                for (File file : folderList){
                    String label = removeExtension(file.getName());
                    try {
                        List<File> wavFiles = getWavFiles(file);
                        for (File wavFile : wavFiles){
                            // 获取Uri
                            Uri fileUri = FileProvider.getUriForFile(getApplicationContext(), "com.demo.ncnndemo.provider", wavFile);
                            String fileName = getFileNameFromUri(getApplicationContext(), fileUri);
                            AudioUtils.ChunkWavFile chunkWavFile = AudioUtils.readAndChunkWavFile(getApplicationContext(), fileUri, CHUNK_SIZE);
                            while (true){
                                if (isAudioNsxAgc){
                                    sleep(50);
                                } else {
                                    handleNsxAgcForByteArray(chunkWavFile,label,fileName);
                                    break;
                                }
                            }
                        }
                    } catch (Exception e) {
                        throw new RuntimeException(e);
                    }
                }

                ThreadPool.runOnMainThread(new Runnable() {
                    @Override
                    public void run() {
                        binding.startNsxAgcForAllFileButton.setText("处理完成");
                    }
                });
            }
        });
    }

    private void handleNsxAgcForByteArray(AudioUtils.ChunkWavFile chunkWavFile, String label, String fileName) {
        isAudioNsxAgc = true;
        List<byte[]> byteChunks = chunkWavFile.getByteChunks();
        Integer audioFormat = chunkWavFile.getAudioFormat();
        for (int i = 0; i < byteChunks.size(); i++){
            byte[] bytes = new byte[CHUNK_SIZE];
            System.arraycopy(byteChunks.get(i),0,bytes,0,CHUNK_SIZE);
            double[] doubles = AudioUtils.pcmAudioByteArray2DoubleArray(bytes, audioFormat);
            try {
                double[] audioNsxAgc = audioNsxAgc(doubles);
                //保存音频
                AudioUtils.saveAudioWav(getApplicationContext(),"NsxAgcFiles" + File.separator + label + File.separator + fileName,audioNsxAgc);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }
        isAudioNsxAgc = false;
    }

    private double[] audioNsxAgc(double[] doubles){
        //初始化webrtc降噪
        if (webRTCAudioUtils == null){
            webRTCAudioUtils = new WebRTCAudioUtils();
        }
        webRTCCreate();

        Integer shortSize = 160;
        short[] shortArray = ByteUtils.convertDoubleArrayToShortArray(doubles);
        short[] padShortArray = AudioUtils.padShortArray(shortArray, shortSize);

        short[][] splitShortArray = ByteUtils.splitShortArray(padShortArray, shortSize);
        short[] nsxAgcShortArray = new short[padShortArray.length];

        for (int i = 0; i < splitShortArray.length; i++){
            short[] outNsxData = new short[shortSize];
            short[] outAgcData = new short[shortSize];
            short[] tempData = new short[shortSize];
            System.arraycopy(splitShortArray[i],0,tempData,0,shortSize);
            for (int j = 0; j < agcAndNsxNum; j++){
                webRTCAudioUtils.nsxProcess(nsxInstList.get(j),tempData,1,outNsxData);
                tempData = outNsxData;

                webRTCAudioUtils.agcProcess(agcInstList.get(j), tempData, 1, 160, outAgcData, 0, 0, 0, false);
                tempData = outAgcData;
            }
            System.arraycopy(tempData,0,nsxAgcShortArray,i*shortSize,shortSize);
        }
        double[] nsxAgcDoubleArray = ByteUtils.convertShortArrayToDoubleArray(nsxAgcShortArray);
        webRTCFree();
        return nsxAgcDoubleArray;
    }

    public static List<File> getWavFiles(File folder) {
        List<File> wavFiles = new ArrayList<>();
        File[] files = folder.listFiles();

        if (files != null) {
            for (File file : files) {
                if (file.isDirectory()) {
                    // 递归遍历子文件夹
                    wavFiles.addAll(getWavFiles(file));
                } else if (file.isFile() && file.getName().toLowerCase().endsWith(".wav")) {
                    // 如果是.wav文件，则添加到列表中
                    wavFiles.add(file);
                }
            }
        } else {
            if (folder.isFile() && folder.getName().toLowerCase().endsWith(".wav")) {
                // 如果是.wav文件，则添加到列表中
                wavFiles.add(folder);
            }
        }

        return wavFiles;
    }

    private void webRTCCreate() {
        nsxInstList.clear();
        for (int i = 0; i < nsxInstListNum; i++) {
            long nsxInst = webRTCAudioUtils.nsxCreate();
            webRTCAudioUtils.nsxInit(nsxInst, 16000);
            webRTCAudioUtils.nsxSetPolicy(nsxInst, 2);
            nsxInstList.add(nsxInst);
        }

        agcInstList.clear();
        for (int i = 0; i < agcInstListNum; i++) {
            long agcInst = webRTCAudioUtils.agcCreate();
            webRTCAudioUtils.agcInit(agcInst, 0, 255, 2, 16000);
            webRTCAudioUtils.agcSetConfig(agcInst, webRTCAudioUtils.getAgcConfig((short) 3, (short) 75, true));
            agcInstList.add(agcInst);
        }
    }

    private void webRTCFree() {
        for (long nsxInst : nsxInstList) {
            webRTCAudioUtils.nsxFree(nsxInst);
        }
        nsxInstList.clear();

        for (long agcInst : agcInstList) {
            webRTCAudioUtils.agcFree(agcInst);
        }
        agcInstList.clear();
    }

    private void showFileNsxAgcActivity(File folder) {
        Intent intent = new Intent(this, FileNsxAgcActivity.class);
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

    @SuppressLint("Range")
    public static String getFileNameFromUri(Context context, Uri uri) {
        String fileName = null;
        if (uri.getScheme().equals("content")) {
            ContentResolver contentResolver = context.getContentResolver();
            Cursor cursor = contentResolver.query(uri, null, null, null, null);
            try {
                if (cursor != null && cursor.moveToFirst()) {
                    fileName = cursor.getString(cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME));
                }
            } finally {
                if (cursor != null) {
                    cursor.close();
                }
            }
        } else if (uri.getScheme().equals("file")) {
            fileName = uri.getLastPathSegment();
        }
        return fileName;
    }

    public static String removeExtension(String fileName) {
        if (fileName == null) {
            return null;
        }
        int lastDotIndex = fileName.lastIndexOf('.');
        if (lastDotIndex == -1) {
            // 文件名中没有找到"."，不包含后缀
            return fileName;
        }
        // 返回文件名中"."之前的部分
        return fileName.substring(0, lastDotIndex);
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