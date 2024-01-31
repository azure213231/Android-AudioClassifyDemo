package com.demo.ncnndemo;

import static com.demo.ncnndemo.AssetsAudioClassify.assetFilePath;

import android.content.Context;
import android.util.Log;

import org.pytorch.IValue;
import org.pytorch.Module;
import org.pytorch.Tensor;

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

            Integer shortSize = 160;
            short[] shortArray = ByteUtils.convertDoubleArrayToShortArray(audioAsFloatArray);
            short[] padShortArray = AudioUtils.padShortArray(shortArray, shortSize);

            short[][] splitShortArray = ByteUtils.splitShortArray(padShortArray, shortSize);
            short[] nsxShortArray = new short[padShortArray.length];

            for (int i = 0; i < splitShortArray.length; i++){
                short[] outNsxData = new short[shortSize];
                webRTCAudioUtils.nsxProcess(nsxId,splitShortArray[i],1,outNsxData);
                System.arraycopy(outNsxData,0,nsxShortArray,i*shortSize,shortSize);
            }
            double[] nsxDoubleArray = ByteUtils.convertShortArrayToDoubleArray(nsxShortArray);

            double[][][] feature = FilterBankProcessor.getFeature(nsxDoubleArray);
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

            if (audioClassifyResult.getScore() > 0.8){
                AudioUtils.saveAudioClassifyNSXWav(context,audioClassifyResult.getLabel(),nsxDoubleArray);
            }

            return audioClassifyResult;
        }catch (Exception e){
            Log.e("TAG", "AudioClassify: "+e.getMessage() );
            throw new Exception(e);
        }
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
