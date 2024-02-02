package com.demo.ncnndemo;

import android.annotation.SuppressLint;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Intent;
import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaRecorder;
import android.os.Binder;
import android.os.Build;
import android.os.IBinder;
import android.os.PowerManager;
import android.util.Log;
import android.view.View;

import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;

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
    private static final int NOTIFICATION_ID = 1;
    private static final String CHANNEL_ID = "YourChannelId";
    private Notification notification;

    @Override
    public void onCreate() {
        super.onCreate();
        Log.d(TAG, "Service created");

        // 在服务停止时移除通知
        // 创建通知渠道（适用于 Android 8.0 及以上版本）
        createNotificationChannel();

        // 创建通知
        notification = buildNotification();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        Log.d(TAG, "Service destroyed");
        stopRecording();
        stopForeground(true);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        // 将服务置于前台
        startForeground(NOTIFICATION_ID, notification);
        startRecording();
        return super.onStartCommand(intent, flags, startId);
    }



    @Override
    public IBinder onBind(Intent intent) {
        // 如果服务不提供绑定，则返回 null
        return null;
    }

    @Override
    public boolean onUnbind(Intent intent) {
        Log.d(TAG, "Service unbound");
        return super.onUnbind(intent);
    }

    private Notification buildNotification() {
        // 创建通知
        NotificationCompat.Builder builder = new NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle("AudioClassify")
                .setContentText("startRecord")
                .setSmallIcon(R.mipmap.ic_launcher)
                .setVisibility(NotificationCompat.VISIBILITY_PUBLIC);

        return builder.build();
    }

    private void createNotificationChannel() {
        // 创建通知渠道（适用于 Android 8.0 及以上版本）
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            CharSequence name = "AudioClassify";
            String description = "startRecord";
            int importance = NotificationManager.IMPORTANCE_DEFAULT;
            NotificationChannel channel = new NotificationChannel(CHANNEL_ID, name, importance);
            channel.setDescription(description);

            NotificationManager notificationManager = getSystemService(NotificationManager.class);
            notificationManager.createNotificationChannel(channel);
        }
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

            sendBorderCast("startRecord","GONE");
            sendBorderCast("stopRecord","VISIBLE");
        }
    }

    private void stopRecording() {
        if (audioRecord.getState() == AudioRecord.STATE_INITIALIZED) {
            isRecording = false;
            audioRecord.stop();
            audioRecord.release();

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
                                    sendBorderCast("decibelsResult","db: " + formattedNumber);

                                    PytorchRepository.AudioClassifyResult audioClassifyResult = PytorchRepository.getInstance().audioClassify(getApplicationContext(),doubles);
                                    sendBorderCast("classifyResult",audioClassifyResult.getLabel() + ": " + audioClassifyResult.getScore());

                                    if (decibels > 35){
                                        //识别率90%以上按照识别结果保存
                                        if (audioClassifyResult.getScore() > 0.90){
                                            AudioUtils.saveAudioClassifyWav(getApplicationContext(),audioClassifyResult.getLabel(),doubles);
                                        } else {
                                            //声音很大，但是识别结果都不匹配
                                            if (decibels > 60){
                                                AudioUtils.saveAudioClassifyWav(getApplicationContext(),"unknown",doubles);
                                            }
                                        }
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
        Intent intent = new Intent("com.demo.ncnndemo.AudioClassifyService.ACTION_UPDATE_UI");
        intent.putExtra("name", name);
        intent.putExtra("data", data);
        sendBroadcast(intent);
    }
}
