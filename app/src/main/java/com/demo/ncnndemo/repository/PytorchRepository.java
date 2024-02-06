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

import java.util.Arrays;

public class PytorchRepository {
    private static volatile PytorchRepository instance;

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
            module = Module.load(assetFilePath(context, "fbank-model.pt"));
            return true;
        } catch (Exception e){
            throw new Exception(e);
        }
    }

    public AudioClassifyResult audioClassify(Context context,double[] audioAsFloatArray) throws Exception{
        try {
            //初始化webrtc降噪
            WebRTCAudioUtils webRTCAudioUtils = new WebRTCAudioUtils();
            long nsxId = webRTCAudioUtils.nsxCreate();
            webRTCAudioUtils.nsxInit(nsxId,16000);
            webRTCAudioUtils.nsxSetPolicy(nsxId,2);

            //webRtc自动增益
            long agcInst = webRTCAudioUtils.agcCreate();
            webRTCAudioUtils.agcInit(agcInst, 0, 255, 2, 16000);
            webRTCAudioUtils.agcSetConfig(agcInst, webRTCAudioUtils.getAgcConfig((short) 3, (short) 75, true));

            Integer shortSize = 160;
            short[] shortArray = ByteUtils.convertDoubleArrayToShortArray(audioAsFloatArray);
            short[] padShortArray = AudioUtils.padShortArray(shortArray, shortSize);

            short[][] splitShortArray = ByteUtils.splitShortArray(padShortArray, shortSize);
            short[] nsxAgcShortArray = new short[padShortArray.length];

            for (int i = 0; i < splitShortArray.length; i++){
                short[] outNsxData = new short[shortSize];
                short[] outAgcData = new short[shortSize];
                webRTCAudioUtils.nsxProcess(nsxId,splitShortArray[i],1,outNsxData);
                webRTCAudioUtils.agcProcess(agcInst, outNsxData, 1, 160, outAgcData, 0, 0, 0, false);
                System.arraycopy(outAgcData,0,nsxAgcShortArray,i*shortSize,shortSize);
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

            //分贝数
            double decibels = AudioUtils.getAudioDb(audioAsFloatArray);
            AudioUtils.saveAudioClassifyWav(context,"audioClassifyNSX",audioClassifyResult.getLabel(),decibels,audioClassifyResult.getScore(),nsxAgcDoubleArray);

            return audioClassifyResult;
        }catch (Exception e){
            Log.e("TAG", "AudioClassify: "+e.getMessage() );
            throw new Exception(e);
        }
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
