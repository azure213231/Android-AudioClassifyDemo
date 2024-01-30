package com.demo.ncnndemo;

import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaCodec;
import android.media.MediaExtractor;
import android.media.MediaFormat;
import android.os.Environment;
import android.text.TextUtils;
import android.util.Log;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;

public class AudioFormatUtils {
    private void convertWavToPcm() {
        int SAMPLE_RATE = 44100;
        int CHANNEL_CONFIG = AudioFormat.CHANNEL_IN_MONO;
        int AUDIO_FORMAT = AudioFormat.ENCODING_PCM_16BIT;
        int BUFFER_SIZE = AudioRecord.getMinBufferSize(SAMPLE_RATE, CHANNEL_CONFIG, AUDIO_FORMAT);
        try {
            // 输入的 WAV 文件路径
            String wavFilePath = Environment.getExternalStorageDirectory().getPath() + "/input.wav";
            // 输出的 PCM 文件路径
            String pcmFilePath = Environment.getExternalStorageDirectory().getPath() + "/output.pcm";

            // 创建输入流读取 WAV 文件
            FileInputStream wavInputStream = new FileInputStream(wavFilePath);

            // 创建输出流写入 PCM 文件
            FileOutputStream pcmOutputStream = new FileOutputStream(pcmFilePath);

            byte[] buffer = new byte[BUFFER_SIZE];
            int bytesRead;

            // 跳过 WAV 文件头部
            wavInputStream.skip(44);

            // 读取数据并写入 PCM 文件
            while ((bytesRead = wavInputStream.read(buffer)) != -1) {
                pcmOutputStream.write(buffer, 0, bytesRead);
            }

            // 关闭流
            wavInputStream.close();
            pcmOutputStream.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
