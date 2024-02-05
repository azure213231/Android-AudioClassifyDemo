package com.demo.ncnndemo.utils;

import java.nio.ByteBuffer;
import java.util.Arrays;

public class ByteUtils {

    public static Integer getIntFromByte(byte[] bytes,Integer offset,Integer num){
        int value = 0;
        for (int i = 0; i < num; i++){
            value |= (bytes[offset + i] & 0xff) << (i * 8);
        }
        return value;
    }

    /**
     * 将三维double数组转化为三维float数组
     * */
    public static float[][][] convertToFloatArray3(double[][][] doubleArray) {
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
     * 将三维float数组转化为一维float数组
     * */
    public static float[] convertToFloatArray1(float[][][] featureFloat) {
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
        return flatArray;
    }

    /**
     * 将三维double数组转化为一维float数组
     * */
    public static float[] convertToFloatArray(double[][][] doubleArray) {
        int x = doubleArray.length;
        int y = doubleArray[0].length;
        int z = doubleArray[0][0].length;
//        float[][][] floatArray = new float[x][y][z];
        float[] floatArray = new float[x * y * z];
        for (int i = 0; i < x; i++) {
            for (int j = 0; j < y; j++) {
                for (int k = 0; k < z; k++) {
                    floatArray[i * (y * z) + j * z + k ] = (float) doubleArray[i][j][k];
                }
            }
        }
        return floatArray;
    }

    /**
     * float[]转化为short数组
     * */
    public static short[] convertFloatArrayToShortArray(float[] floatArray) {
        short[] shortArray = new short[floatArray.length];

        // 缩放因子，可以根据具体情况进行调整
        float scaleFactor = Short.MAX_VALUE;

        for (int i = 0; i < floatArray.length; i++) {
            // 将float值进行缩放，然后转换为short值
            shortArray[i] = (short) (floatArray[i] * scaleFactor);
        }

        return shortArray;
    }

    /**
     * double[]转化为short数组
     * */
    public static short[] convertDoubleArrayToShortArray(double[] doubleArray) {
        short[] shortArray = new short[doubleArray.length];

        // 缩放因子，可以根据具体情况进行调整
        double scaleFactor = Short.MAX_VALUE / 2.0;

        for (int i = 0; i < doubleArray.length; i++) {
            // 将double值进行缩放，然后转换为short值
            shortArray[i] = (short) (doubleArray[i] * scaleFactor);
        }

        return shortArray;
    }

    /**
     * short分割
     * */
    public static short[][] splitShortArray(short[] shortArray, int chunkSize) {
        int arrayLength = shortArray.length;

        // 计算需要分割成多少个大小为chunkSize的数组
        int numOfChunks = (int) Math.ceil((double) arrayLength / chunkSize);

        // 创建二维数组
        short[][] result = new short[numOfChunks][chunkSize];

        // 分割数组
        for (int i = 0; i < numOfChunks; i++) {
            int startIdx = i * chunkSize;
            int endIdx = Math.min(startIdx + chunkSize, arrayLength);
            result[i] = Arrays.copyOfRange(shortArray, startIdx, endIdx);
        }

        return result;
    }

    /**
     * short[]转化为float数组
     * */
    public static float[] convertShortArrayToFloatArray(short[] shortArray) {
        float[] floatArray = new float[shortArray.length];

        // 缩放因子，可以根据具体情况进行调整
        float scaleFactor = 1.0f / Short.MAX_VALUE;

        for (int i = 0; i < shortArray.length; i++) {
            // 将short值进行缩放，然后转换为float值
            floatArray[i] = shortArray[i] * scaleFactor;
        }

        return floatArray;
    }

    /**
     * short[]转化为double数组
     * */
    public static double[] convertShortArrayToDoubleArray(short[] shortArray) {
        double[] doubleArray = new double[shortArray.length];

        // 缩放因子，可以根据具体情况进行调整
        double scaleFactor = 2.0 / Short.MAX_VALUE;

        for (int i = 0; i < shortArray.length; i++) {
            // 将short值进行缩放，然后转换为double值
            doubleArray[i] = shortArray[i] * scaleFactor;
        }

        return doubleArray;
    }

}
