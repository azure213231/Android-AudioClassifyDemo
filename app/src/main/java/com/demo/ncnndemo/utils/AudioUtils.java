package com.demo.ncnndemo.utils;

import android.content.Context;
import android.content.res.AssetFileDescriptor;
import android.media.AudioAttributes;
import android.media.AudioFormat;
import android.media.MediaPlayer;
import android.util.Log;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;

public class AudioUtils {
    private static final String TAG = "AudioUtils";

    /**
     * 读取wav文件的byte后，先提取为浮点型，在进行分贝值的归一化（同python代码逻辑）
     * */
    public static double[] loadWavAudioAsDoubleArray(byte[] audioData) {
        try {
            //采样率
            Integer sampleRate = ByteUtils.getIntFromByte(audioData, 24, 4); // 获取采样率

            // 设置音频格式
            Integer audioFormat  = ByteUtils.getIntFromByte(audioData, 20, 2);    // 编码格式
            //通道数
            Integer channelCount = ByteUtils.getIntFromByte(audioData, 22, 2);    // 声道数
            //位宽，每个采样点的bit数
            Integer sampleSizeInBits = ByteUtils.getIntFromByte(audioData, 34, 2); // 采样位数
            // 计算每帧的字节数
            int frameSize = (sampleSizeInBits / 8) * channelCount;
            // 一个样本的时间（以秒为单位）
            double sampleDuration = 1000.0 / sampleRate;
            // 总样本数
            int totalSamples = audioData.length / frameSize;
            // 总时间（以秒为单位）
            int duration = (int) (totalSamples * sampleDuration);
            int frameCount = duration * sampleRate / 1000; // 计算帧数

            // 将音频数据分割到每帧
            // 将音频数据转换为浮点数数组
            byte[] audioPcmData = new byte[frameSize * frameCount];
//            double[] doubleArray = new double[frameCount];
//             计算实际需要复制的字节数
            int remainingBytes = audioData.length - 44; // 假设 44 是 WAV 文件头的大小
            int copyBytes = Math.min(remainingBytes, frameSize * frameCount);
            // 进行复制
            System.arraycopy(audioData, 44, audioPcmData, 0, copyBytes);

            double[] doubles = new double[0];
            if (audioFormat == 1){
                doubles = convert32IntPCMToDoubleArray(audioPcmData);
            } else if (audioFormat == 3){
                doubles = convert32FloatPCMToDoubleArray(audioPcmData);
            }

            return doubles;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * 32位深pcm的byte数组，先转化为浮点型，在进行归一化（同python）
     * */
    public static double[] pcmAudioByteArray2DoubleArray(byte[] audioPcmData,int audioFormat) {
        try {
            double[] doubles = new double[0];
            if (audioFormat == 1){
                doubles = convert32IntPCMToDoubleArray(audioPcmData);
            } else if (audioFormat == 3){
                doubles = convert32FloatPCMToDoubleArray(audioPcmData);
            }

            return doubles;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * 提取的音频方法与py中一致(从assets中提取)
     * */
    public static double[] loadAudioAsDoubleArrayByAssets(Context context, String filename) {
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
            Integer audioFormat  = ByteUtils.getIntFromByte(audioData, 20, 2);    // 音频数据的编码格式
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

            //数据长度，单位字节
            Integer dataSize = ByteUtils.getIntFromByte(audioData, 40, 4);

            double[] doubles = new double[0];
            if (audioFormat == 1){
                doubles = convert32IntPCMToDoubleArray(audioPcmData);
            } else if (audioFormat == 3){
                doubles = convert32FloatPCMToDoubleArray(audioPcmData);
            }

            return doubles;
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * 32位整型编码格式的byte[]转化为double[]
     * */
    private static double[] convert32IntPCMToDoubleArray(byte[] pcmBytes) {
        int numSamples = pcmBytes.length / 4; // Assuming 32-bit PCM (4 bytes per sample)
        double[] pcmDoubles = new double[numSamples];

        ByteBuffer buffer = ByteBuffer.wrap(pcmBytes);
        buffer.order(ByteOrder.LITTLE_ENDIAN);

        for (int i = 0; i < numSamples; i++) {
            int pcmValue = buffer.getInt();
            double pcmDouble = pcmValue / (double) Integer.MAX_VALUE;
            pcmDoubles[i] = pcmDouble;
        }

        return pcmDoubles;
    }

    /**
     * 32位浮点型编码格式的byte[]转化为double[]
     * */
    private static double[] convert32FloatPCMToDoubleArray(byte[] pcmBytes) {
        int bytesPerSample = 4;  // 32位PCM，每个样本占4字节
        int sampleCount = pcmBytes.length / bytesPerSample;
        double[] doubleArray = new double[sampleCount];

        for (int i = 0; i < sampleCount; i++) {
            // 从byte数组中读取每个样本的值
            int sampleValue = byteArrayToInt(pcmBytes, i * bytesPerSample);

            float v = Float.intBitsToFloat(sampleValue);
            doubleArray[i] = v;
        }

        return doubleArray;
    }

    /**
     * 归一化
     * */
    public static void normalize(double[] samples, double targetDb, double maxGainDb) throws IllegalArgumentException {
        double rmsDb = getRmsDb(samples);
        if (Double.NEGATIVE_INFINITY == rmsDb) {
            return;
        }

        double gain = targetDb - rmsDb;

        if (gain > maxGainDb) {
            throw new IllegalArgumentException("无法将段规范化到 " + targetDb + " dB，因为可能的增益已经超过maxGainDb (" + maxGainDb + " dB)");
        }

        gainDb(samples,Math.min(maxGainDb, targetDb - rmsDb));
    }

    /**
     * 归一化
     * */
    public static void normalize(double[] samples) throws IllegalArgumentException {
        double targetDb = -20.0;
        double maxGainDb = 300.0;
        double rmsDb = getRmsDb(samples);
        if (Double.NEGATIVE_INFINITY == rmsDb) {
            return;
        }

        double gain = targetDb - rmsDb;

        if (gain > maxGainDb) {
            throw new IllegalArgumentException("无法将段规范化到 " + targetDb + " dB，因为可能的增益已经超过maxGainDb (" + maxGainDb + " dB)");
        }

        gainDb(samples,Math.min(maxGainDb, targetDb - rmsDb));
    }

    public static void gainDb(double[] samples, double gain) {
        for (int i = 0; i < samples.length; i++) {
            samples[i] *= Math.pow(10, gain / 20.0);
        }
    }
    public static double getRmsDb(double[] samples) {
        double meanSquare = calculateMeanSquare(samples);

        // Check for NaN and return a default value or handle appropriately
        if (Double.isNaN(meanSquare)) {
            // Handle NaN case, e.g., return a default value
            return 0.0;
        }

        return 10 * Math.log10(meanSquare);
    }

    private static double calculateMeanSquare(double[] samples) {
        double sumSquare = Arrays.stream(samples)
                .map(x -> Math.pow(x, 2))
                .sum();

        // Check for zero division
        if (samples.length == 0) {
            // Handle division by zero, e.g., return a default value
            return 0.0;
        }

        return sumSquare / samples.length;
    }

    private static float bytesToFloat(byte[] bytes, int offset) {
        int value = 0;
        for (int i = 0; i < 4; i++) {
            value |= (bytes[offset + i] & 0xFF) << (i * 8);
        }
        return Float.intBitsToFloat(value);
    }

    public static void saveAudioClassifyWav(Context context,String classify,double[] audioData){
        SimpleDateFormat daySdf = new SimpleDateFormat("yyyy-MM-dd");//yyyy-MM-dd HH:mm:ss
        SimpleDateFormat secondSdf = new SimpleDateFormat("HH:mm:ss");//yyyy-MM-dd HH:mm:ss

        long currentTimeMillis = System.currentTimeMillis();
        long twentySleepTimestampOfDay = DateUtils.getTwentySleepTimestampOfDay(currentTimeMillis);

        Date date = new Date(twentySleepTimestampOfDay);
        String day = daySdf.format(date);

        Date currentDate = new Date(currentTimeMillis);
        String second = secondSdf.format(currentDate);

        String savePath = context.getExternalFilesDir(null).getAbsolutePath() + File.separator + "audioClassify" + File.separator
                + day + File.separator + classify + File.separator + second + ".wav";
        Log.e("TAG", "run: "+savePath );
        AudioUtils.saveDoubleArrayAsWav(audioData,savePath);
    }

    public static void saveAudioClassifyNSXWav(Context context,String classify,double[] audioData){
        SimpleDateFormat daySdf = new SimpleDateFormat("yyyy-MM-dd");//yyyy-MM-dd HH:mm:ss
        SimpleDateFormat secondSdf = new SimpleDateFormat("HH:mm:ss");//yyyy-MM-dd HH:mm:ss

        long currentTimeMillis = System.currentTimeMillis();
        long twentySleepTimestampOfDay = DateUtils.getTwentySleepTimestampOfDay(currentTimeMillis);

        Date date = new Date(twentySleepTimestampOfDay);
        String day = daySdf.format(date);

        Date currentDate = new Date(currentTimeMillis);
        String second = secondSdf.format(currentDate);

        String savePath = context.getExternalFilesDir(null).getAbsolutePath() + File.separator + "audioClassifyNSX" + File.separator
                + day + File.separator + classify + File.separator + second + ".wav";
        Log.e("TAG", "run: "+savePath );
        AudioUtils.saveDoubleArrayAsWav(audioData,savePath);
    }

    /**
     * 保存音频(编码格式为32位整型)
     * */
    public static void saveDoubleArrayAsWav(double[] pcmData, String outputFilePath) {
        int SAMPLE_RATE = 16000;
        int BITS_PER_SAMPLE = 32;
        int CHANNELS = 1;
        // 将double数组缩放到32位深度范围内
        float scaleFactor = (float) Math.pow(2, BITS_PER_SAMPLE - 1) - 1;
        float[] scaledData = new float[pcmData.length];
        for (int i = 0; i < pcmData.length; i++) {
            scaledData[i] = (float) (pcmData[i] * scaleFactor);
        }

        ByteBuffer byteBuffer = ByteBuffer.allocateDirect(scaledData.length * (BITS_PER_SAMPLE / 8)).order(ByteOrder.LITTLE_ENDIAN);
        for (float value : scaledData) {
            byteBuffer.putInt((int) value);
        }

        try {
            File file = new File(outputFilePath);
            createFileRecursively(file);
            FileOutputStream fos = new FileOutputStream(file);

            // 添加WAV文件头
            fos.write("RIFF".getBytes());
            fos.write(intToBytes(36 + byteBuffer.capacity(), 4));  // Sub-chunk 1 size
            fos.write("WAVE".getBytes());
            fos.write("fmt ".getBytes());
            fos.write(intToBytes(16, 4));  // Sub-chunk 1 size
            fos.write(shortToBytes((short) 1, 2));  // AudioFormat (PCM)
            fos.write(shortToBytes((short) CHANNELS, 2));  // Number of channels
            fos.write(intToBytes(SAMPLE_RATE, 4));  // Sample rate
            fos.write(intToBytes(SAMPLE_RATE * CHANNELS * (BITS_PER_SAMPLE / 8), 4));  // Byte rate
            fos.write(shortToBytes((short) (CHANNELS * (BITS_PER_SAMPLE / 8)), 2));  // Block align
            fos.write(shortToBytes((short) BITS_PER_SAMPLE, 2));  // Bits per sample

            fos.write("data".getBytes());
            fos.write(intToBytes(byteBuffer.capacity(), 4));  // Sub-chunk 2 size
            fos.write(byteBuffer.array());

            fos.close();
            System.out.println();
            Log.i(TAG, "saveDoubleArrayAsWav: WAV文件保存成功：" + file.getAbsolutePath());
        } catch (Exception e) {
            Log.e(TAG, "saveDoubleArrayAsWav: WAV文件保存失败: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private static void createFileRecursively(File file) throws IOException {
        // 获取文件的父目录
        File parentDirectory = file.getParentFile();

        // 判断父目录是否存在，如果不存在则递归创建
        if (!parentDirectory.exists()) {
            if (parentDirectory.mkdirs()) {
                System.out.println("父目录创建成功: " + parentDirectory.getAbsolutePath());
            } else {
                createFileRecursively(parentDirectory);  // 如果父目录创建失败，不继续创建文件
            }
        }

        // 判断文件是否存在，如果不存在则创建
        if (!file.exists()) {
            if (file.createNewFile()) {
                System.out.println("文件创建成功: " + file.getAbsolutePath());
            } else {
                System.out.println("文件创建失败");
            }
        } else {
            System.out.println("文件已存在");
        }
    }

    private static byte[] intToBytes(int value, int size) {
        byte[] bytes = new byte[size];
        for (int i = 0; i < size; i++) {
            bytes[i] = (byte) (value >>> (i * 8));
        }
        return bytes;
    }

    private static byte[] shortToBytes(short value, int size) {
        byte[] bytes = new byte[size];
        for (int i = 0; i < size; i++) {
            bytes[i] = (byte) (value >>> (i * 8));
        }
        return bytes;
    }

    public static short[] padShortArray(short[] originalArray, int targetLength) {
        int originalLength = originalArray.length;
        int paddingLength = targetLength - (originalLength % targetLength);
        int newLength = originalLength + paddingLength;

        short[] paddedArray = Arrays.copyOf(originalArray, newLength);
        // 此时 paddedArray 的长度为 targetLength 的倍数，且原始数组的元素被复制到前面

        // 如果需要在末尾填充0，可以取消下一行的注释
         Arrays.fill(paddedArray, originalLength, newLength, (short) 0);

        return paddedArray;
    }

    public static double getAudioDb(double[] doubles){
        //分贝数
        double amplitude = calculateAmplitude(doubles);
        double decibels = calculateDecibels(amplitude);
        return decibels;
    }
    private static double calculateAmplitude(double[] floatBuffer) {
        int floatCount = floatBuffer.length;
        double sum = 0;
        for (int i = 0; i < floatCount; i++) {
            sum += floatBuffer[i] * floatBuffer[i];
        }
        return Math.sqrt(sum / floatCount);
    }

    private static double calculateDecibels(double amplitude) {
        double REFERENCE_AMPLITUDE = 20.0e-6;
        return 20 * Math.log10(amplitude / REFERENCE_AMPLITUDE);
    }

    private static int byteArrayToInt(byte[] byteArray, int offset) {
        return (byteArray[offset + 3] & 0xFF) << 24 |
                (byteArray[offset + 2] & 0xFF) << 16 |
                (byteArray[offset + 1] & 0xFF) << 8 |
                (byteArray[offset] & 0xFF);
    }
}
