package com.demo.ncnndemo;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Context;
import android.content.res.AssetFileDescriptor;
import android.media.AudioAttributes;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.widget.TextView;

import com.demo.ncnndemo.databinding.ActivityMainBinding;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Arrays;

import org.pytorch.IValue;
import org.pytorch.Module;
import org.pytorch.Tensor;

public class MainActivity extends AppCompatActivity {

    // Used to load the 'ncnndemo' library on application startup.
    static {
        System.loadLibrary("ncnndemo");
    }

    private Integer SIZE = 40;

    private ActivityMainBinding binding;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        // Example of a call to a native method
        TextView tv = binding.sampleText;
        tv.setText(NCNNUtils.stringFromJNI());

        double[] audioAsFloatArray = loadAudioAsFloatArray2(this, "SnoreTest-00019.wav");

        try {

            Module module = Module.load(assetFilePath(this, "fbank-model.pt"));

            double[][][] feature = FilterBankProcessor.getFeature(audioAsFloatArray);
            // 将 double 转换为 float
            float[][][] featureFloat = convertToFloatArray(feature);

            // 获取数组的形状
            int dim1 = featureFloat.length;
            int dim2 = featureFloat[0].length;
            int dim3 = featureFloat[0][0].length;
            // 将 featureFloat 转为一维数组
            float[] flatArray = new float[dim1 * dim2 * dim3];
            for (int i = 0; i < dim1; i++) {
                for (int j = 0; j < dim2; j++) {
                    System.arraycopy(featureFloat[i][j], 0, flatArray, (i * dim2 + j) * dim3, dim3);
                }
            }

            long shape[]={dim1,dim2,dim3};
            Tensor inputTensor=Tensor.fromBlob(flatArray,shape);//tensor初始化方法
            // 将输入张量传递给模型
            IValue input = IValue.from(inputTensor);
            IValue output = module.forward(input);

            // 处理模型的输出
            Tensor outputTensor = output.toTensor();

            // 假设 outputTensor 是你的模型的输出张量
            long[] outputShape = outputTensor.shape();
            int batchSize = (int) outputShape[0];
            int numClasses = (int) outputShape[1];

            // 获取整个输出张量的数据
            float[] outputData = outputTensor.getDataAsFloatArray();

            // 对每个样本应用 Softmax
            // 计算当前样本在数组中的起始索引
            int startIndex = 0;
            // 获取当前样本的输出概率分布
            float[] probabilities = Arrays.copyOfRange(outputData, startIndex, startIndex + numClasses);

            // 对概率分布进行 Softmax 处理
            float[] softmaxProbabilities = softmax(probabilities);

            float maxScore = -Float.MAX_VALUE;
            String text = "";
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

            tv.setText(text);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    // 辅助方法：将三维 double 数组转换为三维 float 数组
    private static float[][][] convertToFloatArray(double[][][] doubleArray) {
        int x = doubleArray.length;
        int y = doubleArray[0].length;
        int z = doubleArray[0][0].length;
        float[][][] floatArray = new float[x][y][z];
        for (int i = 0; i < x; i++) {
            for (int j = 0; j < y; j++) {
                for (int k = 0; k < z; k++) {
                    floatArray[i][j][k] = (float) doubleArray[i][j][k];
                }
            }
        }
        return floatArray;
    }

    /**
     * 提取的音频方法与py中一致
     * */
    public double[] loadAudioAsFloatArray2(Context context, String filename) {
        AssetFileDescriptor assetFileDescriptor = null;
        try {
            MediaPlayer mediaPlayer = new MediaPlayer();
            assetFileDescriptor = context.getAssets().openFd("audio/"+filename);
            mediaPlayer.setDataSource(assetFileDescriptor.getFileDescriptor(), assetFileDescriptor.getStartOffset(), assetFileDescriptor.getLength());

            InputStream inputStream = context.getAssets().open("audio/" + filename);
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

            normalize(floatArray,-20.0,300.0);
            return floatArray;
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private void normalize(double[] audioData, double targetDb, double maxGainDb) {
        if (Double.isInfinite(rmsDb(audioData))) return;

        double gain = targetDb - rmsDb(audioData);

        if (gain > maxGainDb) {
            throw new IllegalArgumentException(String.format(
                    "Cannot normalize segment to %f dB because the potential gain exceeds maxGainDb (%f dB)",
                    targetDb, maxGainDb));
        }

        gainDb(audioData,Math.min(maxGainDb, targetDb - rmsDb(audioData)));
    }

    private void gainDb(double[] samples, double gain) {
        double linearGain = Math.pow(10, gain / 20.0);
        for (int i = 0; i < samples.length; i++) {
            samples[i] *= linearGain;
        }
    }

    private double rmsDb(double[] samples) {
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

    public float[] copyTo_512_80_5(float[] data){
        int i1 = data.length / (SIZE*1);
        float[] floatArray = new float[i1*SIZE*1];
        for (int i = 0; i < i1*SIZE*1; i++) {
            if (i < data.length){
                floatArray[i] = data[i];
            }
        }
        return floatArray;

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
        float max = Float.NEGATIVE_INFINITY;
        for (float value : input) {
            max = Math.max(max, value);
        }

        float sum = 0.0f;
        for (int i = 0; i < input.length; i++) {
            input[i] = (float) Math.exp(input[i] - max);
            sum += input[i];
        }

        for (int i = 0; i < input.length; i++) {
            input[i] /= sum;
        }

        return input;
    }
}