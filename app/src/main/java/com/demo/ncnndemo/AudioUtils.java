package com.demo.ncnndemo;

import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioTrack;
import android.util.Log;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;

public class AudioUtils {
    private static final String TAG = "AudioUtils";

    public static short[] convertAudioFileToShortArray(File audioFile, int frameCount) {
        short[] audioData = new short[frameCount];

        try {
            FileInputStream fileInputStream = new FileInputStream(audioFile);
            byte[] fileData = new byte[(int) audioFile.length()];
            fileInputStream.read(fileData);
            fileInputStream.close();

            int channelCount = AudioFormat.CHANNEL_OUT_MONO;
            int encoding = AudioFormat.ENCODING_PCM_32BIT;
            int bytesPerSample = getBytesPerSample(encoding);

            int frameSize = frameCount * channelCount * bytesPerSample;
            int bufferSize = AudioTrack.getMinBufferSize(AudioTrack.getNativeOutputSampleRate(AudioManager.STREAM_MUSIC), channelCount, encoding);
            int bufferCount = frameSize / bufferSize;

            AudioTrack audioTrack = new AudioTrack(AudioManager.STREAM_MUSIC, AudioTrack.getNativeOutputSampleRate(AudioManager.STREAM_MUSIC),
                    channelCount, encoding, bufferSize, AudioTrack.MODE_STATIC);

            audioTrack.write(fileData, 0, frameSize);

            audioTrack.setPlaybackHeadPosition(0); // 将播放头位置设置为音频开头

            audioTrack.setLoopPoints(0, bufferCount, -1); // 设置循环播放

//            audioTrack.play(); // 开始播放

            int totalBytesRead = 0;
            int totalFramesRead = 0;

            while (totalFramesRead < frameCount) {
                int availableFrames = audioTrack.getPlaybackHeadPosition() / bytesPerSample / channelCount;
                int framesToRead = Math.min(frameCount - totalFramesRead, availableFrames);
                int bytesToRead = framesToRead * bytesPerSample * channelCount;

//                int bytesRead = audioTrack.read(fileData, totalBytesRead, bytesToRead);
//                if (bytesRead == AudioTrack.ERROR_INVALID_OPERATION || bytesRead == AudioTrack.ERROR_BAD_VALUE) {
//                    Log.e(TAG, "Error reading audio data");
//                    break;
//                }
//
//                totalBytesRead += bytesRead;
                totalFramesRead += framesToRead;
            }

//            audioTrack.stop(); // 停止播放
            audioTrack.release(); // 释放资源

            // 将字节数组转换为short数组
            int shortArrayLength = totalBytesRead / bytesPerSample;
            for (int i = 0; i < shortArrayLength; i++) {
                short sample = byteArrayToShort(fileData[i * bytesPerSample], fileData[i * bytesPerSample + 1], encoding);
                audioData[i] = sample;
            }

        } catch (IOException e) {
            e.printStackTrace();
        }

        return audioData;
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
