package com.demo.ncnndemo.utils;

import android.media.AudioFormat;

import java.util.ArrayList;

import be.tarsos.dsp.AudioDispatcher;
import be.tarsos.dsp.AudioEvent;
import be.tarsos.dsp.AudioProcessor;
import be.tarsos.dsp.io.TarsosDSPAudioFormat;
import be.tarsos.dsp.io.android.AndroidAudioInputStream;
import be.tarsos.dsp.mfcc.MFCC;

public class MFCCProcessor {
    private static final int SAMPLE_RATE = 16000;
    private static final int MFCC_FEATURES = 40; // 选择的MFCC系数数量
    private static final int CHANNEL_CONFIG = AudioFormat.CHANNEL_IN_DEFAULT;
    private static final int AUDIO_FORMAT = AudioFormat.ENCODING_PCM_32BIT;
    private static final int SAMPLES_PER_FRAME = 200;

    private MFCC mfcc;

    public MFCCProcessor() {
        // 初始化MFCC对象
//        bufferSize: 用于计算MFCC的音频帧的大小，通常设置为FFT的大小。它决定了在时域上分析的窗口大小。
//
//        sampleRate: 音频的采样率，表示每秒的采样点数。
//
//        numberOfCepstrumCoeficients: MFCC系数的数量，即最终提取的MFCC特征的维度。
//
//        amountOfFilters: Mel滤波器组的数量，用于在频率域上模拟人类听觉系统对声音的敏感度。
//
//        lowFrequency: Mel滤波器组的最低频率（以赫兹为单位），通常设置为较低的值，例如20Hz。
//
//        highFrequency: Mel滤波器组的最高频率（以赫兹为单位），通常设置为较高的值，以覆盖音频信号的大部分频谱。
        mfcc = new MFCC(SAMPLES_PER_FRAME, SAMPLE_RATE, MFCC_FEATURES, 100, 20, 8000);
    }

    public float[] extractMFCC(double[] audioData) {
        // 创建TarsosDSP的音频格式对象
        TarsosDSPAudioFormat audioFormat = new TarsosDSPAudioFormat(SAMPLE_RATE, 32, 1, true, false);

        // 创建TarsosDSP的音频事件对象
        AudioEvent audioEvent = new AudioEvent(audioFormat);

        // 将音频数据填充到音频事件对象中
//        float[] floatBuffer = new float[audioData.length];
//        System.arraycopy(audioData, 0, floatBuffer, 0, audioData.length);
//        audioEvent.setFloatBuffer(floatBuffer);

        ArrayList<Float> dynamicFloatList = new ArrayList<>();
        if (audioData.length > SAMPLES_PER_FRAME){
            for (int i = 0 ; i < audioData.length/SAMPLES_PER_FRAME ; i++){
                float[] floatBuffer = new float[SAMPLES_PER_FRAME];
                System.arraycopy(audioData, i * SAMPLES_PER_FRAME, floatBuffer, 0, SAMPLES_PER_FRAME);
                audioEvent.setFloatBuffer(floatBuffer);
//                audioEvent.setFloatBuffer(audioData);

                // 计算MFCC特征
                mfcc.process(audioEvent);

                // 获取MFCC系数
                float[] mfccValues = mfcc.getMFCC();

                for (float f : mfccValues){
                    dynamicFloatList.add(f);
                }
            }
        }

        float[] dynamicFloatArray = convertFloatListToArray(dynamicFloatList);
        return dynamicFloatArray;
    }

    // 将ArrayList<Float>转换为float[]
    private static float[] convertFloatListToArray(ArrayList<Float> floatList) {
        float[] floatArray = new float[floatList.size()];
        for (int i = 0; i < floatList.size(); i++) {
            floatArray[i] = floatList.get(i);
        }
        return floatArray;
    }

    public float[] performMFCC(AndroidAudioInputStream audioInputStream) {
        MFCC mfcc = new MFCC(SAMPLES_PER_FRAME, SAMPLE_RATE, MFCC_FEATURES, 40, 300, 3700);

        AudioDispatcher dispatcher = new AudioDispatcher(audioInputStream, SAMPLES_PER_FRAME, SAMPLES_PER_FRAME/2);

        dispatcher.addAudioProcessor(new AudioProcessor() {
            @Override
            public boolean process(AudioEvent audioEvent) {
                mfcc.process(audioEvent);
                return true;
            }

            @Override
            public void processingFinished() {
            }
        });

        dispatcher.run();

        return mfcc.getMFCC();
    }
}
