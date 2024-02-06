package com.demo.ncnndemo.activity;

import androidx.appcompat.app.AppCompatActivity;

import android.app.Activity;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;

import com.demo.ncnndemo.repository.PytorchRepository;
import com.demo.ncnndemo.utils.ThreadPool;
import com.demo.ncnndemo.databinding.ActivityChoseAudioFileClassifyBinding;
import com.demo.ncnndemo.utils.AudioUtils;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;

public class ChoseAudioFileClassifyActivity extends AppCompatActivity {

    private ActivityChoseAudioFileClassifyBinding binding;

    private static final int PICK_AUDIO_REQUEST = 2;
    private static final int REQUEST_APP_FILE_CODE = 1;  // 用于标识请求的常量

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
        byte[] bytes = readBytesFromUri(this, audioUri);

        ThreadPool.runOnMainThread(new Runnable() {
            @Override
            public void run() {
                binding.filePath.setText(audioUri.getPath());
                double[] doubles = AudioUtils.loadWavAudioAsDoubleArray(bytes);
                try {
                    double decibels = AudioUtils.getAudioDb(doubles);
                    PytorchRepository.AudioClassifyResult audioClassifyResult = PytorchRepository.getInstance().audioClassify(getApplicationContext(),doubles);
                    binding.classifyResult.setText(audioClassifyResult.getLabel() + ": " + audioClassifyResult.getScore());
                    binding.dbResult.setText("dB: " + decibels);
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            }
        });

    }

    public static byte[] readBytesFromUri(Context context, Uri fileUri) {
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();

        try {
            ContentResolver contentResolver = context.getContentResolver();
            InputStream inputStream = contentResolver.openInputStream(fileUri);

            if (inputStream != null) {
                byte[] buffer = new byte[1024];
                int bytesRead;

                while ((bytesRead = inputStream.read(buffer)) != -1) {
                    byteArrayOutputStream.write(buffer, 0, bytesRead);
                }

                inputStream.close();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        return byteArrayOutputStream.toByteArray();
    }
}