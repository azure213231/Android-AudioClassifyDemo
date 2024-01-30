package com.demo.ncnndemo;

import android.content.Context;
import android.content.res.AssetFileDescriptor;
import android.media.AudioAttributes;
import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioTrack;
import android.media.MediaPlayer;
import android.util.Log;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;

public class AudioUtils {
    private static final String TAG = "AudioUtils";

    /**
     * 提取的音频方法与py中一致
     * */
    public static double[] loadAudioAsDoubleArray(Context context, String filename) {
        AssetFileDescriptor assetFileDescriptor = null;
        try {
            MediaPlayer mediaPlayer = new MediaPlayer();
            assetFileDescriptor = context.getAssets().openFd(filename);
            mediaPlayer.setDataSource(assetFileDescriptor.getFileDescriptor(), assetFileDescriptor.getStartOffset(), assetFileDescriptor.getLength());

            InputStream inputStream = context.getAssets().open(filename);
            byte[] audioData = new byte[inputStream.available()];
            inputStream.read(audioData);
            inputStream.close();

            // 设置音频属性
            mediaPlayer.setAudioAttributes(new AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_MEDIA)
                    .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                    .build());

            // 准备 MediaPlayer
            mediaPlayer.prepare();

            // 获取音频信息
            int duration = mediaPlayer.getDuration(); // 获取音频时长（毫秒）
            //采样率
            Integer sampleRate = ByteUtils.getIntFromByte(audioData, 24, 4); // 获取采样率
            int frameCount = duration * sampleRate / 1000; // 计算帧数

            // 设置音频格式
            //通道数
            Integer channelCount = ByteUtils.getIntFromByte(audioData, 22, 2);    // 声道数
            //位宽，每个采样点的bit数
            Integer sampleSizeInBits = ByteUtils.getIntFromByte(audioData, 34, 2); // 采样位数

            // 计算每帧的字节数
            int frameSize = (sampleSizeInBits / 8) * channelCount;

            // 将音频数据分割到每帧
            // 将音频数据转换为浮点数数组
            byte[] audioPcmData = new byte[frameSize * frameCount];
            double[] floatArray = new double[frameCount];
            // 计算实际需要复制的字节数
            int remainingBytes = audioData.length - 44; // 假设 44 是 WAV 文件头的大小
            int copyBytes = Math.min(remainingBytes, frameSize * frameCount);
            // 进行复制
            System.arraycopy(audioData, 44, audioPcmData, 0, copyBytes);

            //编码格式0x01表示pcm
            Integer audioFormat = ByteUtils.getIntFromByte(audioData, 20, 2);
            //数据长度，单位字节
            Integer dataSize = ByteUtils.getIntFromByte(audioData, 40, 4);

            for (int i = 0; i < frameCount; i++) {
                float value = bytesToFloat(audioPcmData, i * frameSize);
                floatArray[i] = value;
            }

            //归一化
            normalize(floatArray,-20.0,300.0);
            return floatArray;
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
    private static void normalize(double[] audioData, double targetDb, double maxGainDb) {
        if (Double.isInfinite(rmsDb(audioData))) return;

        double gain = targetDb - rmsDb(audioData);

        if (gain > maxGainDb) {
            throw new IllegalArgumentException(String.format(
                    "Cannot normalize segment to %f dB because the potential gain exceeds maxGainDb (%f dB)",
                    targetDb, maxGainDb));
        }

        gainDb(audioData,Math.min(maxGainDb, targetDb - rmsDb(audioData)));
    }

    private static void gainDb(double[] samples, double gain) {
        double linearGain = Math.pow(10, gain / 20.0);
        for (int i = 0; i < samples.length; i++) {
            samples[i] *= linearGain;
        }
    }

    private static double rmsDb(double[] samples) {
        double sum = 0;
        for (double sample : samples) {
            sum += sample * sample;
        }
        double rms = Math.sqrt(sum / samples.length);
        return 20.0 * Math.log10(rms);
    }

    private static float bytesToFloat(byte[] bytes, int offset) {
        int value = 0;
        for (int i = 0; i < 4; i++) {
            value |= (bytes[offset + i] & 0xFF) << (i * 8);
        }
        return Float.intBitsToFloat(value);
    }

    private static int getBytesPerSample(int encoding) {
        switch (encoding) {
            case AudioFormat.ENCODING_PCM_8BIT:
                return 1;
            case AudioFormat.ENCODING_PCM_16BIT:
                return 2;
            case AudioFormat.ENCODING_PCM_FLOAT:
                return 4;
            default:
                return 2; // 默认为16位PCM
        }
    }

    private static short byteArrayToShort(byte byte1, byte byte2, int encoding) {
        switch (encoding) {
            case AudioFormat.ENCODING_PCM_8BIT:
                return (short) (byte1 << 8);
            case AudioFormat.ENCODING_PCM_16BIT:
                return (short) ((byte2 << 8) | (byte1 & 0xFF));
            case AudioFormat.ENCODING_PCM_FLOAT:
                // 根据需要实现浮点数转换为short的逻辑
                // 这里只是简单地将浮点数转换为16位整数
                return (short) (byte2 << 8);
            default:
                return (short) ((byte2 << 8) | (byte1 & 0xFF)); // 默认为16位PCM
        }
    }
}
