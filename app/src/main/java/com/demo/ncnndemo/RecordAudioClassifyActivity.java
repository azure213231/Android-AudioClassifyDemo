package com.demo.ncnndemo;

import static com.demo.ncnndemo.AssetsAudioClassify.assetFilePath;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.Manifest;
import android.content.pm.PackageManager;
import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaRecorder;
import android.os.Bundle;
import android.util.Log;
import android.view.View;

import com.demo.ncnndemo.databinding.ActivityRecordAudioClassifyBinding;

import org.pytorch.Module;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;

public class RecordAudioClassifyActivity extends AppCompatActivity {

    ActivityRecordAudioClassifyBinding binding;

    private AudioRecord audioRecord;
    private boolean isRecording = false;
    private static final int SAMPLE_RATE = 16000;
    private static final int CHANNEL_CONFIG = AudioFormat.CHANNEL_IN_MONO;
    private static final int AUDIO_FORMAT = AudioFormat.ENCODING_PCM_32BIT;
    private static final int BUFFER_SIZE = AudioRecord.getMinBufferSize(SAMPLE_RATE, CHANNEL_CONFIG, AUDIO_FORMAT);
    private static final int RECORD_AUDIO_PERMISSION_CODE = 1;
    private ArrayList<byte[]> recordedDataList = new ArrayList<>();
    //上一次音频分析时间
    private long lastRecordTimeStamp;
    private boolean isAudioClassify = false;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityRecordAudioClassifyBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        initView();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        stopRecording();
    }

    private void initView() {
        // 检查权限
        if (checkRecordAudioPermission()) {
            // 已经授予权限
            // 进行录音相关操作
        } else {
            // 请求权限
            requestRecordAudioPermission();
        }

        audioRecord = new AudioRecord(
                MediaRecorder.AudioSource.MIC,
                SAMPLE_RATE,
                CHANNEL_CONFIG,
                AUDIO_FORMAT,
                BUFFER_SIZE
        );

        binding.startRecord.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                // 开始录音
                startRecording();
            }
        });

        binding.stopRecord.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                stopRecording();
            }
        });
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (requestCode == RECORD_AUDIO_PERMISSION_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // 用户授予了录音权限
                // 进行录音相关操作
            } else {
                // 用户拒绝了录音权限
                // 可以在这里提供一些反馈或者禁用相关功能
                ToastUtil.showToast(this,"没有录音权限");
            }
        }
    }

    private boolean checkRecordAudioPermission() {
        return ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO)
                == PackageManager.PERMISSION_GRANTED;
    }

    private void requestRecordAudioPermission() {
        ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.RECORD_AUDIO},
                RECORD_AUDIO_PERMISSION_CODE);
    }

    private void startRecording() {
        if (audioRecord.getState() == AudioRecord.STATE_INITIALIZED) {
            lastRecordTimeStamp = System.currentTimeMillis();
            isRecording = true;
            audioRecord.startRecording();

            // 开启新线程用于读取PCM数据
            ThreadPool.runOnThread(new Runnable() {
                @Override
                public void run() {
                    reduceAudio();
                }
            });

            binding.startRecord.setVisibility(View.GONE);
            binding.stopRecord.setVisibility(View.VISIBLE);
        }
    }

    private void stopRecording() {
        if (audioRecord.getState() == AudioRecord.STATE_INITIALIZED) {
            isRecording = false;
            audioRecord.stop();
            audioRecord.release();

            binding.startRecord.setVisibility(View.VISIBLE);
            binding.stopRecord.setVisibility(View.GONE);
        }
    }

    private void reduceAudio() {

        byte[] buffer = new byte[BUFFER_SIZE];
        while (isRecording) {
            int readSize = audioRecord.read(buffer, 0, BUFFER_SIZE);
            if (readSize != AudioRecord.ERROR_INVALID_OPERATION && readSize != AudioRecord.ERROR_BAD_VALUE) {
                // 处理音频数据
                byte[] data = new byte[readSize];
                System.arraycopy(buffer, 0, data, 0, readSize);
                recordedDataList.add(data);

                //如果距离上次分析超过3秒
                if (System.currentTimeMillis() - lastRecordTimeStamp > 5 * 1000){

                    // 合并从上次记录到现在录音数据
                    byte[] finalRecordedData = concatenateByteArrays(recordedDataList);
                    double[] doubles = AudioUtils.bytesToDoubles(finalRecordedData);

//                    AudioUtils.saveAudioClassifyWav(this,"test",doubles);

                    recordedDataList.clear();
                    lastRecordTimeStamp = System.currentTimeMillis();

                    if (!isAudioClassify){
                        ThreadPool.runOnMainThread(new Runnable() {
                            @Override
                            public void run() {
                                try {
                                    isAudioClassify = true;
                                    String classify = PytorchRepository.getInstance().audioClassify(doubles);
                                    binding.classifyResult.setText(classify);

//                                    SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd-HH:mm:ss");//yyyy-MM-dd HH:mm:ss
//                                    String savePath = getExternalFilesDir(null).getAbsolutePath() + File.separator + "audioClassify" + File.separator
//                                            + classify.substring(0,classify.indexOf(":")) + File.separator + sdf.format(new Date(System.currentTimeMillis())) + ".wav";
//                                    Log.e("TAG", "run: "+savePath );
                                    AudioUtils.saveAudioClassifyWav(getApplicationContext(),classify.substring(0,classify.indexOf(":")),doubles);
                                } catch (Exception e) {
                                    ToastUtil.showToast(getApplicationContext(),"分析失败: " + e.getMessage());
                                }
                                isAudioClassify = false;
                            }
                        });
                    }
                }
            }
        }
    }

    private byte[] concatenateByteArrays(ArrayList<byte[]> arrayList) {
        int totalLength = 0;
        for (byte[] array : arrayList) {
            totalLength += array.length;
        }

        byte[] result = new byte[totalLength];
        int currentIndex = 0;

        for (byte[] array : arrayList) {
            System.arraycopy(array, 0, result, currentIndex, array.length);
            currentIndex += array.length;
        }

        return result;
    }
}