#include <jni.h>
#include <string>
#include <android/log.h>
#include "net.h" // ncnn头文件
#include <android/asset_manager_jni.h>
#include <cmath>
#include <iostream>

extern "C" JNIEXPORT jstring JNICALL
Java_com_demo_ncnndemo_utils_NCNNUtils_stringFromJNI(
        JNIEnv* env,
        jclass clazz) {
    std::string hello = "Hello from C++";
    return env->NewStringUTF(hello.c_str());
}

extern "C" JNIEXPORT jstring JNICALL
Java_com_demo_ncnndemo_utils_NCNNUtils_loadModel(JNIEnv *env, jclass clazz, jobject assetManager, jfloatArray audio_data) {

    // 加载声音分类模型
    AAssetManager* mgr = AAssetManager_fromJava(env, assetManager);
    ncnn::Net model;
    model.load_param(mgr, "best-()-()-80.param");
    model.load_model(mgr, "best-()-()-80.bin");

    // 获取声音数据
    jfloat* sound = env->GetFloatArrayElements(audio_data, nullptr);
    int sound_length = env->GetArrayLength(audio_data);
    // 创建 ncnn 输入层
//    ncnn::Mat input = ncnn::Mat(80,512, 5);
//    ncnn::Mat input = ncnn::Mat(80,5, sound_length/80/5);
//    ncnn::Mat input = ncnn::Mat(80,5, sound_length/80/5);

//    ncnn::Mat input = ncnn::Mat(80,sound_length/80, 1);
//    crash
    ncnn::Mat input = ncnn::Mat(sound_length/80,1, 80);

//    ncnn::Mat input = ncnn::Mat(80,1, sound_length/80);
//    ncnn::Mat input = ncnn::Mat(80, 1,1,5, sound_length/80/5,1);

    // 创建一个ncnn::Mat对象来保存输入张量
//    ncnn::Mat input = ncnn::Mat(80,512, 5);


    float* input_data = (float*)input.data;
    int input_width = input.w;
    int input_height = input.h;
    int input_channels = input.c;

    // 将音频数据转换为输入张量
    for (int i = 0; i < input_height; i++) {
        float* inputPtr = input.channel(0).row(i); // 获取第一个通道的第i行指针
        for (int j = 0; j < input_width; ++j) {
            inputPtr[j] = sound[i * 80 + j]; // 将音频数据赋值给输入张量
        }
    }


// 复制numpy数组中的数据到ncnn::Mat对象
//    memcpy(input, sound, sizeof(float) * sound_length);
// 将声音数据写入输入张量
    // 将声音数据写入输入张量
//    for (int i = 0; i < sound_length; i++) {
//        int x = i % input_width;
//        int y = (i / input_width) % input_height;
//        int c = (i / (input_width * input_height)) % input_channels;
//
//        // 假设声音数据是单声道的
//        float value = sound[i];
//
//        // 将声音数据写入输入张量
//        if (c * input_width * input_height + y * input_width + x < input_width * input_height * input_channels){
//            input_data[c * input_width * input_height + y * input_width + x] = value;
//        }
//    }

// 将input_tensor传递给ncnn模型进行推理
    ncnn::Extractor ex = model.create_extractor();
    ex.set_vulkan_compute(true);
    ex.input("modelInput", input);   // 使用与导出的ONNX模型中输入名称相匹配的名称


// 运行推理
    ncnn::Mat output;

    ex.extract("modelOutput", output);  // 使用与导出的ONNX模型中输出名称相匹配的名称


    // get result
    int batch_size = output.c;
    int num_classes = output.w;
    float* output_data = (float*)output.data;
//    float* res1 = output.row(0);
    float* res2 = output.row(1);
    float score0 = res2[0];
    float score1 = res2[1];
//    float* res3 = output.channel(0);
//    float* res4 = output.channel(1);

    for (int i = 0; i < batch_size; i++) {
        float* ptr = output.channel(i);
        for (int j = 0; j < num_classes; j++) {
            float confidence = ptr[j];
            // 处理每个标签的置信度
        }
    }

    // 释放声音数据
    // 释放 float 数组的指针
    env->ReleaseFloatArrayElements(audio_data, sound, JNI_ABORT);
    // 输出分类结果

    std::string result = "Sound classified.";

    // 返回分类结果
    if (!std::isnan(*output_data)){
        result = "success.";
    } else {
        result = "fail.";
    }
    return env->NewStringUTF(result.c_str());
}