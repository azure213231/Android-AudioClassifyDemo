package com.demo.ncnndemo.repository;

import static com.demo.ncnndemo.activity.AssetsAudioClassify.assetFilePath;

import android.content.Context;
import android.util.Log;

import com.demo.ncnndemo.data.SoundClassed;
import com.demo.ncnndemo.utils.WebRTCAudioUtils;
import com.demo.ncnndemo.utils.AudioUtils;
import com.demo.ncnndemo.utils.ByteUtils;
import com.demo.ncnndemo.utils.FilterBankProcessor;

import org.pytorch.IValue;
import org.pytorch.Module;
import org.pytorch.Tensor;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class PytorchRepository {
    private static volatile PytorchRepository instance;
    private WebRTCAudioUtils webRTCAudioUtils;
    //降噪，然后增益，循环次数
    private Integer agcAndNsxNum = 1;
    private Integer agcInstListNum = 1;
    private static List<Long> agcInstList = new ArrayList<>();
    private Integer nsxInstListNum = 1;
    private static List<Long> nsxInstList = new ArrayList<>();
    //降噪后增益次数
    private Integer nsxInstList2Num = 1;
    private static List<Long> nsxInstList2 = new ArrayList<>();
    private PytorchRepository() {
    }

    public static PytorchRepository getInstance() {
        if (instance == null) {
            synchronized (PytorchRepository.class) {
                if (instance == null) {
                    instance = new PytorchRepository();
                }
            }
        }

        return instance;
    }

    //训练模型
    private Module module;

    public boolean init(Context context) throws Exception{
        try {
            module = Module.load(assetFilePath(context, "fbank-model20240228.pt"));
            return true;
        } catch (Exception e){
            throw new Exception(e);
        }
    }

    public AudioClassifyResult audioClassify(Context context,double[] audioAsFloatArray) throws Exception{
        try {
            //初始化webrtc降噪
            if (webRTCAudioUtils == null){
                webRTCAudioUtils = new WebRTCAudioUtils();
            }
            webRTCCreate();
//            long nsxId = webRTCAudioUtils.nsxCreate();
//            webRTCAudioUtils.nsxInit(nsxId,16000);
//            webRTCAudioUtils.nsxSetPolicy(nsxId,2);
//
//            //webRtc自动增益
//            long agcInst = webRTCAudioUtils.agcCreate();
//            webRTCAudioUtils.agcInit(agcInst, 0, 255, 2, 16000);
//            webRTCAudioUtils.agcSetConfig(agcInst, webRTCAudioUtils.getAgcConfig((short) 3, (short) 75, true));

            Integer shortSize = 160;
            short[] shortArray = ByteUtils.convertDoubleArrayToShortArray(audioAsFloatArray);
            short[] padShortArray = AudioUtils.padShortArray(shortArray, shortSize);

            short[][] splitShortArray = ByteUtils.splitShortArray(padShortArray, shortSize);
            short[] nsxAgcShortArray = new short[padShortArray.length];

            for (int i = 0; i < splitShortArray.length; i++){
                short[] outNsxData = new short[shortSize];
                short[] outAgcData = new short[shortSize];
                short[] tempData = new short[shortSize];
                System.arraycopy(splitShortArray[i],0,tempData,0,shortSize);

//                for (long nsxInst : nsxInstList){
//                    webRTCAudioUtils.nsxProcess(nsxInst,tempData,1,outNsxData);
//                    tempData = outNsxData;
//                }
//
//                for (long agcInst : agcInstList){
//                    webRTCAudioUtils.agcProcess(agcInst, tempData, 1, 160, outAgcData, 0, 0, 0, false);
//                    tempData = outAgcData;
//                }
//
//                for (long nsxInst2 : nsxInstList2){
//                    webRTCAudioUtils.nsxProcess(nsxInst2,tempData,1,outNsxData);
//                    tempData = outNsxData;
//                }

                for (int j = 0; j < agcAndNsxNum; j++){
                    webRTCAudioUtils.nsxProcess(nsxInstList.get(j),tempData,1,outNsxData);
                    tempData = outNsxData;

                    webRTCAudioUtils.agcProcess(agcInstList.get(j), tempData, 1, 160, outAgcData, 0, 0, 0, false);
                    tempData = outAgcData;
                }
                System.arraycopy(tempData,0,nsxAgcShortArray,i*shortSize,shortSize);
            }
            double[] nsxAgcDoubleArray = ByteUtils.convertShortArrayToDoubleArray(nsxAgcShortArray);

            //使用分贝数对音频归一化（包含增益）
//            normalize(nsxAgcDoubleArray);
            double[][][] feature = FilterBankProcessor.getFeature(nsxAgcDoubleArray);

//            double[][][] feature = FilterBankProcessor.getFeature(audioAsFloatArray);
            // 将三维的 double 转换为 一维float
            float[] featureFloat = ByteUtils.convertToFloatArray(feature);

            // 获取数组的形状
            int dim1 = feature.length;
            int dim2 = feature[0].length;
            int dim3 = feature[0][0].length;

            long shape[]={dim1,dim2,dim3};
            Tensor inputTensor=Tensor.fromBlob(featureFloat,shape);//tensor初始化方法
            // 将输入张量传递给模型
            IValue input = IValue.from(inputTensor);
            IValue output = module.forward(input);

            // 处理模型的输出
            Tensor outputTensor = output.toTensor();

            // 假设 outputTensor 是你的模型的输出张量
            long[] outputShape = outputTensor.shape();


            AudioClassifyResult audioClassifyResult = new AudioClassifyResult();
            //判断输出张量形状和对应的类型长度是否一致
            if (SoundClassed.CLASSES.length != outputShape[1]){
                audioClassifyResult.setLabel("error");
            } else {
                // 获取整个输出张量的数据
                float[] outputData = outputTensor.getDataAsFloatArray();

                // 对概率分布进行 Softmax 处理
                float[] softmaxProbabilities = softmax(outputData);

                float maxScore = -Float.MAX_VALUE;
                for (int i = 0; i < softmaxProbabilities.length; i++) {
                    float score = softmaxProbabilities[i];
                    if (softmaxProbabilities[i] < 0.001){
                        score = 0f;
                    }
//                    text = text + SoundClassed.CLASSES[i] + ": " + score + "\n";
                    if (softmaxProbabilities[i] > maxScore) {
                        maxScore = softmaxProbabilities[i];
                        audioClassifyResult.setScore(maxScore);
                        audioClassifyResult.setLabel(SoundClassed.CLASSES[i]);
                    }
                }
            }
            webRTCFree();

            //分贝数
            double decibels = AudioUtils.getAudioDb(audioAsFloatArray);
            AudioUtils.saveAudioClassifyWav(context,"audioClassifyNSX",audioClassifyResult.getLabel(),decibels,audioClassifyResult.getScore(),nsxAgcDoubleArray);

            return audioClassifyResult;
        } catch (Exception e){
            Log.e("TAG", "AudioClassify: "+e.getMessage() );
            throw new Exception(e);
        }
    }

    private void webRTCCreate() {
        nsxInstList.clear();
        for (int i = 0; i < nsxInstListNum; i++) {
            long nsxInst = webRTCAudioUtils.nsxCreate();
            webRTCAudioUtils.nsxInit(nsxInst, 16000);
            webRTCAudioUtils.nsxSetPolicy(nsxInst, 2);
            nsxInstList.add(nsxInst);
        }

        nsxInstList2.clear();
        for (int i = 0; i < nsxInstList2Num; i++) {
            long nsxInst2 = webRTCAudioUtils.nsxCreate();
            webRTCAudioUtils.nsxInit(nsxInst2, 16000);
            webRTCAudioUtils.nsxSetPolicy(nsxInst2, 2);
            nsxInstList2.add(nsxInst2);
        }

        agcInstList.clear();
        for (int i = 0; i < agcInstListNum; i++) {
            long agcInst = webRTCAudioUtils.agcCreate();
            webRTCAudioUtils.agcInit(agcInst, 0, 255, 2, 16000);
            webRTCAudioUtils.agcSetConfig(agcInst, webRTCAudioUtils.getAgcConfig((short) 3, (short) 75, true));
            agcInstList.add(agcInst);
        }
    }

    private void webRTCFree() {
        for (long nsxInst : nsxInstList) {
            webRTCAudioUtils.nsxFree(nsxInst);
        }
        nsxInstList.clear();

        for (long agcInst : agcInstList) {
            webRTCAudioUtils.agcFree(agcInst);
        }
        agcInstList.clear();
    }

    /**
     * 归一化
     * */
    private static void normalize(double[] samples) throws IllegalArgumentException {
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

    private static void gainDb(double[] samples, double gain) {
        for (int i = 0; i < samples.length; i++) {
            samples[i] *= Math.pow(10, gain / 20.0);
        }
    }
    private static double getRmsDb(double[] samples) {
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


    // Softmax 函数的实现
    private float[] softmax(float[] input) {
        float[] softmax = new float[input.length];
        float max = Float.NEGATIVE_INFINITY;
        for (float value : input) {
            max = Math.max(max, value);
        }

        float sum = 0.0f;
        float[] exp = new float[input.length];
        for (int i = 0; i < input.length; i++) {
            exp[i] = (float) Math.exp(input[i] - max);
            sum += exp[i];
        }

        for (int i = 0; i < input.length; i++) {
            softmax[i] = exp[i] / sum;
        }

        return softmax;
    }

    public class AudioClassifyResult{
        private String label;
        private float score;

        public String getLabel() {
            return label;
        }

        public void setLabel(String label) {
            this.label = label;
        }

        public float getScore() {
            return score;
        }

        public void setScore(float score) {
            this.score = score;
        }
    }
}
