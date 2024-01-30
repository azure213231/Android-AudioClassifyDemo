package com.demo.ncnndemo;

public class ByteUtils {

    public static Integer getIntFromByte(byte[] bytes,Integer offset,Integer num){
        int value = 0;
        for (int i = 0; i < num; i++){
            value |= (bytes[offset + i] & 0xff) << (i * 8);
        }
        return value;
    }
}
