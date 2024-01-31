package com.demo.ncnndemo;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Context;
import android.os.Bundle;
import android.widget.TextView;

import com.demo.ncnndemo.databinding.ActivityAssetsAudioClassfyBinding;

import org.pytorch.IValue;
import org.pytorch.Module;
import org.pytorch.Tensor;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.text.SimpleDateFormat;
import java.util.Date;

public class AssetsAudioClassify extends AppCompatActivity {

    ActivityAssetsAudioClassfyBinding binding;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityAssetsAudioClassfyBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        // Example of a call to a native method
        TextView tv = binding.sampleText;
        tv.setText(NCNNUtils.stringFromJNI());


        try {
            Module module = Module.load(assetFilePath(this, "fbank-model.pt"));

            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd-HH:mm:ss");//yyyy-MM-dd HH:mm:ss

            String fileName = "dogTest-0001.wav";
            double[] audioAsFloatArray = AudioUtils.loadAudioAsDoubleArray( this,"audio/" + fileName);

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
            String savePath = getExternalFilesDir(null).getAbsolutePath() + File.separator + "recordPCMVoice" + File.separator + sdf.format(new Date(System.currentTimeMillis())) + fileName;
            AudioUtils.saveDoubleArrayAsWav(nsxDoubleArray,savePath);

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


            String text = "";
            //判断输出张量形状和对应的类型长度是否一致
            if (SoundClassed.CLASSES.length != outputShape[1]){
                text = "SoundClassed.length error";
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
                    text = text + SoundClassed.CLASSES[i] + ": " + score + "\n";
                    if (softmaxProbabilities[i] > maxScore) {
                        maxScore = softmaxProbabilities[i];
                    }
                }
            }

            tv.setText(text);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Copies specified asset to the file in /files app directory and returns this file absolute path.
     *
     * @return absolute file path
     */
    public static String assetFilePath(Context context, String assetName) throws IOException {
        File file = new File(context.getFilesDir(), assetName);
        if (file.exists() && file.length() > 0) {
            return file.getAbsolutePath();
        }

        try (InputStream is = context.getAssets().open(assetName)) {
            try (OutputStream os = new FileOutputStream(file)) {
                byte[] buffer = new byte[4 * 1024];
                int read;
                while ((read = is.read(buffer)) != -1) {
                    os.write(buffer, 0, read);
                }
                os.flush();
            }
            return file.getAbsolutePath();
        }
    }

    // Softmax 函数的实现
    private static float[] softmax(float[] input) {
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
}