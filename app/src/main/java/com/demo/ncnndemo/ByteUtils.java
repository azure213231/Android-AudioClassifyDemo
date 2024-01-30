package com.demo.ncnndemo;

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
}
