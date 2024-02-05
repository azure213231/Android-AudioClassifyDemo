package com.demo.ncnndemo.utils;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;

public class WebRTCAudioUtils {
    static {
        System.loadLibrary("webrtc-lib");
    }

    /**
     * 创建ns实例
     *
     * @return 成功时返回ns实例，失败返回-1
     */
    public native long nsCreate();

    /**
     * 初始化ns
     *
     * @param frequency
     *            采样率
     */
    public native int nsInit(long nsHandler, int frequency);

    /**
     * 设置降噪策略 等级越高，效果越明显
     *
     * @param mode
     *            0: Mild, 1: Medium , 2: Aggressive
     */
    public native int nsSetPolicy(long nsHandler, int mode);

    /**
     * 核心处理方法 sample_H与outData_H 我不是很懂，希望有明白的可以指点下
     *
     * @param sample
     *            低频段音频数据-输入
     * @param sample_H
     *            高频段音频数据-输入
     * @param outData
     *            低频段音频数据-输出
     * @param outData_H
     *            高频段音频数据-输出
     */
    public native int nsProcess(long nsHandler, float[] spframe, int num_bands,float[] outframe);

    /**
     * 销毁实例
     */
    public native int nsFree(long nsHandler);


    public native long nsxCreate();

    public native int nsxInit(long nsxHandler, int frequency);

    public native int nsxSetPolicy(long nsxHandler, int mode);

    public native int nsxProcess(long nsxHandler, short[] speechFrame, int num_bands,short[] outframe);

    public native int nsxFree(long nsxHandler);


    public native long aecmCreate();

    public native int aecmInit(long aecmInst, int sampFreq);

    public native int aecmFree(long aecmInst);

    public native int aecmBufferFarend(long aecmInst, short[] farend, int nrOfSamples);

    public native int aecmProcess(long aecmInst, short[] nearendNoisy, short[] nearendClean, short[] out, int nrOfSamples, int msInSndCardBuf);

    public class AecmConfig {
        private short cngMode;
        private short echoMode;

        public AecmConfig(short cngMode, short echoMode) {
            this.cngMode = cngMode;
            this.echoMode = echoMode;
        }
    }

    public AecmConfig getAecmConfig(short cngMode, short echoMode){
        return new AecmConfig(cngMode, echoMode);
    }

    public native int aecmSetConfig(long aecmInst, AecmConfig config);


    public native long agcCreate();

    public native int agcFree(long agcInst);

    public native int agcInit(long agcInst, int minLevel, int maxLevel, int agcMode, int fs);

    public class AgcConfig {
        private short targetLevelDbfs;
        private short compressionGaindB;
        private boolean limiterEnable;

        public AgcConfig(short targetLevelDbfs, short compressionGaindB, boolean limiterEnable) {
            this.targetLevelDbfs = targetLevelDbfs;
            this.compressionGaindB = compressionGaindB;
            this.limiterEnable = limiterEnable;
        }
    }

    public AgcConfig getAgcConfig(short targetLevelDbfsV, short compressionGaindBV, boolean limiterEnableV){
        return new WebRTCAudioUtils.AgcConfig(targetLevelDbfsV,compressionGaindBV,limiterEnableV);
    }

    public native int agcSetConfig(long agcInst, AgcConfig config);

    public native int agcProcess(long agcInst, short[] inNear,int num_bands,int samples,short[] out,
                                 int inMicLevel,int outMicLevel,int echo,boolean saturationWarning);

    public native int agcAddFarend(long agcInst,short[] inFar,int samples);

    public native int agcAddMic(long agcInst,short[] inMic,int num_bands,int samples);

    public native int agcVirtualMic(long agcInst,short[] inMic,int num_bands,int samples,int micLevelIn,int micLevelOut);



    public short[] byetArrayToShortArray(byte[] data) {
        short[] outDataend = new short[data.length / 2];
        ByteBuffer.wrap(data).order(ByteOrder.LITTLE_ENDIAN).asShortBuffer().get(outDataend);
        return outDataend;
    }

    // shortArraytobyteArray
    public byte[] shortArrayToByteArry(short[] data) {
        byte[] byteVal = new byte[data.length * 2];
        for (int i = 0; i < data.length; i++) {
            byteVal[i * 2] = (byte) (data[i] & 0xff);
            byteVal[i * 2 + 1] = (byte) ((data[i] & 0xff00) >> 8);
        }
        return byteVal;
    }
}
