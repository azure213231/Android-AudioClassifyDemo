package com.demo.ncnndemo.utils;

import android.content.res.AssetManager;

import java.util.List;

public class NCNNUtils {
    public static native String stringFromJNI();
    public static native String loadModel(AssetManager mgr,float[] soundData);
}
