package com.demo.ncnndemo.activity;

import androidx.appcompat.app.AppCompatActivity;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.provider.OpenableColumns;
import android.view.Gravity;
import android.view.View;

import com.demo.ncnndemo.repository.PytorchRepository;
import com.demo.ncnndemo.utils.ByteUtils;
import com.demo.ncnndemo.utils.ThreadPool;
import com.demo.ncnndemo.databinding.ActivityChoseAudioFileClassifyBinding;
import com.demo.ncnndemo.utils.AudioUtils;
import com.demo.ncnndemo.utils.ToastUtil;

import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ChoseAudioFileClassifyActivity extends AppCompatActivity {

    private ActivityChoseAudioFileClassifyBinding binding;

    private static final int PICK_AUDIO_REQUEST = 2;
    private static final int REQUEST_APP_FILE_CODE = 1;  // 用于标识请求的常量
    private static final int HEADER_SIZE = 44; // WAV文件头部大小
    private static final int CHUNK_SIZE = 300 * 1024; // 每个块的大小，320KB
    private static Integer audioFormat = 1;
    private Map<String, Integer> resultCountMap = new HashMap<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        binding = ActivityChoseAudioFileClassifyBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        initView();
    }

    private void initView() {
        binding.choseFile.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
            // 启动文件选择器
//                pickAudioFile();
                startAppFileChosenActivity();
            }
        });
    }

    private void startAppFileChosenActivity() {
        Intent intent = new Intent(this, AppFileChosenActivity.class);
        startActivityForResult(intent, REQUEST_APP_FILE_CODE);
    }

    private void pickAudioFile() {
        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
//        intent.setType("audio/wav");
        intent.setType("*/*");  // 选择所有类型的文件
        startActivityForResult(intent, PICK_AUDIO_REQUEST);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == PICK_AUDIO_REQUEST && resultCode == RESULT_OK && data != null) {
            // 获取用户选择的音频文件的 URI
            Uri selectedAudioUri = data.getData();

            // 在这里处理选中的音频文件
            handleSelectedAudio(selectedAudioUri);
        } else if (requestCode == REQUEST_APP_FILE_CODE) {
            if (resultCode == Activity.RESULT_OK) {
                // 处理返回的结果
                if (data != null) {
                    String fileUri = data.getStringExtra("file_uri");
                    // 处理返回的数据
                    Uri uri = Uri.parse(fileUri);
                    handleSelectedAudio(uri);
                }
            } else if (resultCode == Activity.RESULT_CANCELED) {
                // 处理用户取消的情况
            }
        }
    }

    private void handleSelectedAudio(Uri audioUri) {
        try {
            List<byte[]> byteChunks = readAndChunkWavFile(this, audioUri, CHUNK_SIZE);
            resultCountMap.clear();
            binding.filePath.setText(audioUri.getPath());
            binding.classifyStatus.setText("识别中");
            String fileName = removeExtension(getFileNameFromUri(this, audioUri));

            if (byteChunks.size() < 1){
                ToastUtil.showToast(this,"文件长度不足5秒", Gravity.CENTER);
            } else if (byteChunks.size() == 1) {//文件长度为10秒以内
                handleLT10SecondByteArray(byteChunks);
            } else {//文件长度大于10秒
                handleGT10SecondByteArray(byteChunks,fileName);
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

    }

    /**
     *  大于10秒的数据的处理逻辑
     * */
    private void handleGT10SecondByteArray(List<byte[]> byteChunks, String fileName) {
        ThreadPool.runOnThread(new Runnable() {
            @Override
            public void run() {
                for (int i = 0; i < byteChunks.size(); i++){
                    byte[] bytes = new byte[CHUNK_SIZE];
                    System.arraycopy(byteChunks.get(i),0,bytes,0,CHUNK_SIZE);
                    double[] doubles = AudioUtils.pcmAudioByteArray2DoubleArray(bytes, audioFormat);
                    try {
                        double decibels = AudioUtils.getAudioDb(doubles);
                        //识别结果
                        PytorchRepository.AudioClassifyResult audioClassifyResult = PytorchRepository.getInstance().audioClassify(getApplicationContext(),doubles);
                        //统计结果
                        StatisticalAudioClassifyResult(audioClassifyResult);
                        //保存音频
                        AudioUtils.saveAudioClassifyWav(getApplicationContext(),"PSGClassify/"+fileName,audioClassifyResult.getLabel(),decibels,audioClassifyResult.getScore(),doubles);
                        ThreadPool.runOnMainThread(new Runnable() {
                            @Override
                            public void run() {
                                binding.classifyResult.setText(audioClassifyResult.getLabel() + ": " + audioClassifyResult.getScore());
                                binding.dbResult.setText("dB: " + decibels);
                            }
                        });
                    } catch (Exception e) {
                        throw new RuntimeException(e);
                    }
                }

                //处理完所有文件
                ThreadPool.runOnMainThread(new Runnable() {
                    @Override
                    public void run() {
                        binding.classifyStatus.setText("识别完成（结果保存在APP目录PSGClassify下）");
                        String classifyResult = "";
                        // 打印结果数量
                        for (Map.Entry<String, Integer> entry : resultCountMap.entrySet()) {
                            classifyResult += entry.getKey() + ", Count: " + entry.getValue() + "\n";
                            binding.classifyResult.setText(classifyResult);
                            binding.dbResult.setText("");
                        }
                    }
                });
            }
        });
    }

    private void StatisticalAudioClassifyResult(PytorchRepository.AudioClassifyResult audioClassifyResult) {
        String result = audioClassifyResult.getLabel();
        if (resultCountMap.containsKey(result)) {
            int count = resultCountMap.get(result);
            resultCountMap.put(result, count + 1);
        } else {
            resultCountMap.put(result, 1);
        }
    }

    /**
     * 10秒以内数据的处理逻辑
     * */
    private void handleLT10SecondByteArray(List<byte[]> byteChunks) {
        ThreadPool.runOnMainThread(new Runnable() {
            @Override
            public void run() {
                for (int i = 0; i < byteChunks.size(); i++){
                    byte[] bytes = new byte[CHUNK_SIZE];
                    System.arraycopy(byteChunks.get(i),0,bytes,0,CHUNK_SIZE);
                    double[] doubles = AudioUtils.pcmAudioByteArray2DoubleArray(bytes, audioFormat);
                    try {
                        double decibels = AudioUtils.getAudioDb(doubles);
                        PytorchRepository.AudioClassifyResult audioClassifyResult = PytorchRepository.getInstance().audioClassify(getApplicationContext(),doubles);
                        //不保存音频数据
                        //AudioUtils.saveAudioClassifyWav(getApplicationContext(),"ChoseFile",audioClassifyResult.getLabel(),decibels,audioClassifyResult.getScore(),doubles);
                        binding.classifyResult.setText(audioClassifyResult.getLabel() + ": " + audioClassifyResult.getScore());
                        binding.dbResult.setText("dB: " + decibels);
                    } catch (Exception e) {
                        throw new RuntimeException(e);
                    }
                }

                //处理完所有文件
                binding.classifyStatus.setText("识别完成");
            }
        });
    }

    public static List<byte[]> readAndChunkWavFile(Context context, Uri fileUri, int chunkSize) throws IOException {
        List<byte[]> byteChunks = new ArrayList<>();
        ContentResolver contentResolver = context.getContentResolver();
        InputStream inputStream = null;
//        Integer audioFormat = 1;

        try {
            inputStream = contentResolver.openInputStream(fileUri);
            if (inputStream != null) {
                DataInputStream dataInputStream = new DataInputStream(inputStream);
                byte[] header = new byte[44];
                dataInputStream.readFully(header);
                audioFormat  = ByteUtils.getIntFromByte(header, 20, 2);    // 编码格式

                // 跳过WAV文件头，具体跳过的字节数取决于WAV文件格式
//                inputStream.skip(44); // 一般WAV文件头为44字节

                byte[] buffer = new byte[chunkSize];
                int bytesRead;
                while ((bytesRead = inputStream.read(buffer)) != -1) {
                    if (bytesRead == chunkSize) {
                        byteChunks.add(buffer.clone());
                    }
                }
            }

            return byteChunks;
        } finally {
            if (inputStream != null) {
                inputStream.close();
            }
        }
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
}