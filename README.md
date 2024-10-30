# 安卓声音分类模型Demo



## 前言

本项目模型由Pytorch训练，模型识别主要包含以下声音分类项bird,chicken,cough,dream_talk,footstep,knock_door,snore,thunderstorm,whistle,wind，项目还提供webRTC降噪与自动增益算法、实时录音分类、大型音频文件截取分类等功能，可以用于广泛的场景，为此提供示例帮助技术的发展与应用。

<img src="C:\Users\HuangMinglun\AppData\Roaming\Typora\typora-user-images\image-20241030102804246.png" alt="image-20241030102804246" style="zoom: 50%;" />





## 如何使用

使用AndroidStudio打开项目，并sync项目即可，将会下载对应的pytorch_android依赖。





## 功能介绍



### 分析代码文件

将会读取Assets.audio中的文件进行分析。

修改其中的fileName即可。

```
String fileName = "023500.wav";
double[] audioAsFloatArray = AudioUtils.loadAudioAsDoubleArrayByAssets( this,"audio/" + fileName);
PytorchRepository.AudioClassifyResult audioClassifyResult = PytorchRepository.getInstance().audioClassify(getApplicationContext(),audioAsFloatArray);
```



### 批量降噪、自动增益

使用的是google开源的webRTC降噪算法，通过JNI进行接入，实现对原始音频的预处理。进行多次降噪、自动增益提高识别的准确性，其中模型使用的训练集均经过相同的算法降噪增益，确保训练集与实际场景保持较小的偏差。

根据选取的文件夹，将会读取文件夹下的所有音频文件，进行降噪增益并保存。

```java
private void startFileNsxAgc(List<File> folderList) {
    binding.startNsxAgcForAllFileButton.setText("处理中");
    ThreadPool.runOnThread(new Runnable() {
        @Override
        public void run() {
            for (File file : folderList){
                String label = removeExtension(file.getName());
                try {
                    List<File> wavFiles = getWavFiles(file);
                    for (File wavFile : wavFiles){
                        // 获取Uri
                        Uri fileUri = FileProvider.getUriForFile(getApplicationContext(), "com.demo.ncnndemo.provider", wavFile);
                        String fileName = getFileNameFromUri(getApplicationContext(), fileUri);
                        AudioUtils.ChunkWavFile chunkWavFile = AudioUtils.readAndChunkWavFile(getApplicationContext(), fileUri, CHUNK_SIZE);
                        while (true){
                            if (isAudioNsxAgc){
                                sleep(50);
                            } else {
                                handleNsxAgcForByteArray(chunkWavFile,label,fileName);
                                break;
                            }
                        }
                    }
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            }

            ThreadPool.runOnMainThread(new Runnable() {
                @Override
                public void run() {
                    binding.startNsxAgcForAllFileButton.setText("处理完成");
                }
            });
        }
    });
}
```



### 实时录音

通过采集麦克风数据进行实时分析并保存，具体格式为16K、16位的单声道音频数据。与模型训练时的音频格式需要保持一致。在实时分析完成后，在“打开APP存储目录”下即可查看分析并保存的结果，同时也可以查看在过程中降噪增益后的效果，以及原始的音频数据。

通过读取麦克风的音频数据，合并为5S，计算分贝，并降噪增益后，调用PytorchRepository.getInstance().audioClassify(getApplicationContext(),doubles);进行分类，并将结果显示在activity中。

```java
private void reduceAudio() {

        byte[] buffer = new byte[BUFFER_SIZE];
        while (isRecording) {
            int readSize = audioRecord.read(buffer, 0, BUFFER_SIZE);
            if (readSize != AudioRecord.ERROR_INVALID_OPERATION && readSize != AudioRecord.ERROR_BAD_VALUE && readSize > 0) {
                // 处理音频数据
                byte[] data = new byte[readSize];
                System.arraycopy(buffer, 0, data, 0, readSize);
                recordedDataList.add(data);

                //如果距离上次分析超过5秒
                if (System.currentTimeMillis() - lastRecordTimeStamp > 5 * 1000){

                    // 合并从上次记录到现在录音数据
                    byte[] finalRecordedData = concatenateByteArrays(recordedDataList);
//                                      double[] doublesDb = AudioUtils.convert32IntPCMToDoubleArray(finalRecordedData);

                    recordedDataList.clear();
                    lastRecordTimeStamp = System.currentTimeMillis();

                    ThreadPool.runOnThread(new Runnable() {
                        @Override
                        public void run() {
                            while (true){
                                try {
                                    if (isAudioClassify){
                                        sleep(50);
                                    } else {
                                        isAudioClassify = true;

                                        double[] doubles = AudioUtils.pcmAudioByteArray2DoubleArray(finalRecordedData,AUDIO_FORMAT);
                                        //分贝数
                                        double decibels = AudioUtils.getAudioDb(doubles);
                                        // 创建 DecimalFormat 对象，指定保留两位小数
                                        DecimalFormat decimalFormat = new DecimalFormat("#.##");
                                        // 格式化 double 类型的数值
                                        String formattedNumber = decimalFormat.format(decibels);

                                        PytorchRepository.AudioClassifyResult audioClassifyResult = PytorchRepository.getInstance().audioClassify(getApplicationContext(),doubles);

                                        classifyNum++;

                                        AudioUtils.saveAudioClassifyWav(getApplicationContext(),"audioClassify",audioClassifyResult.getLabel(),decibels,audioClassifyResult.getScore(),doubles);

                                        sendBorderCast("decibelsResult","db: " + formattedNumber);
                                        sendBorderCast("classifyResult",audioClassifyResult.getLabel() + ": " + audioClassifyResult.getScore());
                                        sendBorderCast("classifyNum",classifyNum.toString());
                                        isAudioClassify = false;
                                        break;
                                    }
                                } catch (Exception e) {
                                    ToastUtil.showToast(getApplicationContext(),"分析失败: " + e.getMessage());
                                }
                            }
                        }
                    });

                }
            }
        }
    }
```



### 选择音频文件分析

选择的文件仅支持-16位-16K采样率-单声道-wav音频格式的文件，长度需要大于5S。超出5S的音频文件将会以为5S为单位，进行切割分析并保存结果。如需要其他格式文件，需要自身在代码中进行音频文件格式的解析与转化。

```
/**
 *  大于10秒的数据的处理逻辑
 * */
private void handleGT10SecondByteArray(List<byte[]> byteChunks, String fileName) {
    ThreadPool.runOnThread(new Runnable() {
        @Override
        public void run() {
            for (int i = 0; i < byteChunks.size(); i++){
                try {
                    byte[] bytes = new byte[CHUNK_SIZE];
                    System.arraycopy(byteChunks.get(i),0,bytes,0,CHUNK_SIZE);
                    double[] doubles = AudioUtils.pcmAudioByteArray2DoubleArray(bytes, audioFormat);
                    double decibels = AudioUtils.getAudioDb(doubles);
                    while (true){
                        if (isAudioNsxAgc){
                            sleep(50);
                        } else {
                            isAudioNsxAgc = true;
                            //识别结果
                            PytorchRepository.AudioClassifyResult audioClassifyResult = PytorchRepository.getInstance().audioClassify(getApplicationContext(),doubles);
                            //统计结果
                            StatisticalAudioClassifyResult(audioClassifyResult);
                            //保存音频
                            AudioUtils.saveAudioClassifyWav(getApplicationContext(),"PSGClassify/"+fileName,audioClassifyResult.getLabel(),decibels,audioClassifyResult.getScore(),doubles);
                            ThreadPool.runOnMainThread(new Runnable() {
                                @Override
                                public void run() {
                                    binding.classifyResult.setText(audioClassifyResult.getLabel() + ": " + audioClassifyResult.getScore());
                                    binding.dbResult.setText("dB: " + decibels);
                                }
                            });
                            isAudioNsxAgc = false;
                            break;
                        }
                    }
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            }

            //处理完所有文件
            ThreadPool.runOnMainThread(new Runnable() {
                @Override
                public void run() {
                    binding.classifyStatus.setText("识别完成（结果保存在APP目录PSGClassify下）");
                    String classifyResult = "";
                    // 打印结果数量
                    for (Map.Entry<String, Integer> entry : resultCountMap.entrySet()) {
                        classifyResult += entry.getKey() + ", Count: " + entry.getValue() + "\n";
                        binding.classifyResult.setText(classifyResult);
                        binding.dbResult.setText("");
                    }
                }
            });
        }
    });
}
```





