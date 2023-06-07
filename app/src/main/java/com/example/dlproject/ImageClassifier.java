package com.example.dlproject;


import android.content.Context;
import android.graphics.Bitmap;

import org.pytorch.IValue;
import org.pytorch.Module;
import org.pytorch.Tensor;
import org.pytorch.torchvision.TensorImageUtils;

import android.content.res.AssetFileDescriptor;

import java.io.FileInputStream;
import java.io.IOException;
import java.nio.channels.FileChannel;


public class ImageClassifier {
    private static final int INPUT_SIZE = 224; // 모델의 입력 크기
    private static final int NUM_CLASSES = 5; // 분류할 클래스 수

    private Module mModule;

    public ImageClassifier(Context context, String modelPath) {
        // TorchScript 모델 로드
        try {
            // Load the model from the assets directory
            AssetFileDescriptor assetFileDescriptor = context.getAssets().openFd(modelPath);
            FileInputStream fileInputStream = new FileInputStream(assetFileDescriptor.getFileDescriptor());
            FileChannel fileChannel = fileInputStream.getChannel();
            long startOffset = assetFileDescriptor.getStartOffset();
            long declaredLength = assetFileDescriptor.getDeclaredLength();

            // Load the model from the file channel
            mModule = Module.load(String.valueOf(fileChannel.map(FileChannel.MapMode.READ_ONLY, startOffset, declaredLength)));
            fileChannel.close();
            fileInputStream.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public String classifyImage(Bitmap imageBitmap) {
        // 이미지 전처리
        if (mModule == null) return "Module Error";

        Bitmap resizedBitmap = Bitmap.createScaledBitmap(imageBitmap, INPUT_SIZE, INPUT_SIZE, false);

        // 이미지를 Float32 Tensor로 변환
        final Tensor inputTensor = TensorImageUtils.bitmapToFloat32Tensor(resizedBitmap,
                TensorImageUtils.TORCHVISION_NORM_MEAN_RGB, TensorImageUtils.TORCHVISION_NORM_STD_RGB);

        // 모델 입력 데이터로 변환
        final IValue inputs = IValue.from(inputTensor);

        // 모델 실행 및 결과 얻기
        final IValue output = mModule.forward(inputs);
        final Tensor outputTensor = output.toTensor();
        final float[] scores = outputTensor.getDataAsFloatArray();

        // 결과값 중 가장 큰 값의 인덱스 얻기
        int maxIndex = 0;
        float maxScore = scores[0];
        for (int i = 1; i < scores.length; i++) {
            if (scores[i] > maxScore) {
                maxIndex = i;
                maxScore = scores[i];
            }
        }

        String className = getClassName(maxIndex); // 클래스 이름 얻기
        return className;
    }

    private String getClassName(int classIndex) {
        String[] classNames = {"class1", "class2", "class3", "class4", "class5"};
        if (classIndex >= 0 && classIndex < classNames.length) {
            return classNames[classIndex];
        }
        return "Unknown";
    }
}