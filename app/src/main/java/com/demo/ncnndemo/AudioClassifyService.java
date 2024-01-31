package com.demo.ncnndemo;

import android.annotation.SuppressLint;
import android.app.Service;
import android.content.Intent;
import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaRecorder;
import android.os.Binder;
import android.os.IBinder;
import android.os.PowerManager;
import android.util.Log;
import android.view.View;

import androidx.annotation.Nullable;

import java.text.DecimalFormat;
import java.util.ArrayList;

public class AudioClassifyService  extends Service {
    private static final String TAG = "AudioClassifyService";

    private AudioRecord audioRecord;
    private boolean isRecording = false;
    private ArrayList<byte[]> recordedDataList = new ArrayList<>();
    //上一次音频分析时间
    private long lastRecordTimeStamp;
    private boolean isAudioClassify = false;
    private PowerManager.WakeLock wakeLock;
    private static final int SAMPLE_RATE = 16000;
    private static final int CHANNEL_CONFIG = AudioFormat.CHANNEL_IN_MONO;
    private static final int AUDIO_FORMAT = AudioFormat.ENCODING_PCM_32BIT;
    private static final int BUFFER_SIZE = AudioRecord.getMinBufferSize(SAMPLE_RATE, CHANNEL_CONFIG, AUDIO_FORMAT);

    private final IBinder binder = new AudioClassifyBinder();

    public class AudioClassifyBinder extends Binder {
        AudioClassifyService getService() {
            return AudioClassifyService.this;
        }
    }

    @Override
    public void onCreate() {
        super.onCreate();
        Log.d(TAG, "Service created");
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.d(TAG, "Service started");

        // 在这里执行你希望在后台运行的逻辑

        // 如果服务被系统终止，系统会尝试重新创建服务
        return START_STICKY;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        Log.d(TAG, "Service destroyed");
    }

    @Override
    public IBinder onBind(Intent intent) {
        // 如果服务不提供绑定，则返回 null

        // 开始录音
        startRecording();
        return binder;
    }

    @Override
    public boolean onUnbind(Intent intent) {
        Log.d(TAG, "Service unbound");
        stopRecording();
        return super.onUnbind(intent);
    }

    @SuppressLint("MissingPermission")
    private void startRecording() {
        audioRecord = new AudioRecord(
                MediaRecorder.AudioSource.MIC,
                SAMPLE_RATE,
                CHANNEL_CONFIG,
                AUDIO_FORMAT,
                BUFFER_SIZE
        );
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

//            binding.startRecord.setVisibility(View.GONE);
//            binding.stopRecord.setVisibility(View.VISIBLE);
            sendBorderCast("startRecord","GONE");
            sendBorderCast("stopRecord","VISIBLE");
        }
    }

    private void stopRecording() {
        if (audioRecord.getState() == AudioRecord.STATE_INITIALIZED) {
            isRecording = false;
            audioRecord.stop();
            audioRecord.release();

//            binding.startRecord.setVisibility(View.VISIBLE);
//            binding.stopRecord.setVisibility(View.GONE);
            sendBorderCast("startRecord","VISIBLE");
            sendBorderCast("stopRecord","GONE");
        }
    }

    private void reduceAudio() {

        byte[] buffer = new byte[BUFFER_SIZE];
        while (isRecording) {
            int readSize = audioRecord.read(buffer, 0, BUFFER_SIZE);
            if (readSize != AudioRecord.ERROR_INVALID_OPERATION && readSize != AudioRecord.ERROR_BAD_VALUE && readSize > 0) {
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

                                    //分贝数
                                    double decibels = AudioUtils.getAudioDb(doubles);
                                    // 创建 DecimalFormat 对象，指定保留两位小数
                                    DecimalFormat decimalFormat = new DecimalFormat("#.##");
                                    // 格式化 double 类型的数值
                                    String formattedNumber = decimalFormat.format(decibels);
//                                    binding.decibelsResult.setText("db: " + formattedNumber);
                                    sendBorderCast("decibelsResult","db: " + formattedNumber);

                                    PytorchRepository.AudioClassifyResult audioClassifyResult = PytorchRepository.getInstance().audioClassify(getApplicationContext(),doubles);
//                                    binding.classifyResult.setText(audioClassifyResult.getLabel() + ": " + audioClassifyResult.getScore());
                                    sendBorderCast("classifyResult",audioClassifyResult.getLabel() + ": " + audioClassifyResult.getScore());


                                    if (audioClassifyResult.getScore() > 0.90 && decibels > -50){
                                        AudioUtils.saveAudioClassifyWav(getApplicationContext(),audioClassifyResult.getLabel(),doubles);
                                    }
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

    private void sendBorderCast(String name,String data){
        Intent intent = new Intent("com.example.ACTION_UPDATE_UI");
        intent.putExtra("name", name);
        intent.putExtra("data", data);
        sendBroadcast(intent);
    }
}
