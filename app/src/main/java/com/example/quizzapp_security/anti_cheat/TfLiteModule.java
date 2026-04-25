package com.example.quizzapp_security.anti_cheat;

import android.content.Context;
import android.content.res.AssetFileDescriptor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.util.Log;
import org.tensorflow.lite.Interpreter;
import org.tensorflow.lite.support.common.ops.NormalizeOp;
import org.tensorflow.lite.support.image.ImageProcessor;
import org.tensorflow.lite.support.image.TensorImage;
import org.tensorflow.lite.support.image.ops.ResizeOp;
import java.io.FileInputStream;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;

public class TfLiteModule {
    private Interpreter interpreter;
    private final int IMAGE_SIZE = 224;
    private static final String TAG = "TfLiteModule";

    public TfLiteModule(Context context, String modelPath) {
        try {
            interpreter = new Interpreter(loadModelFile(context, modelPath));
            Log.d(TAG, "Modèle TFLite chargé : " + modelPath);
        } catch (Exception e) {
            Log.e(TAG, "Erreur chargement modèle : " + e.getMessage());
        }
    }

    private MappedByteBuffer loadModelFile(Context context, String modelPath) throws Exception {
        AssetFileDescriptor fileDescriptor = context.getAssets().openFd(modelPath);
        FileInputStream inputStream = new FileInputStream(fileDescriptor.getFileDescriptor());
        FileChannel fileChannel = inputStream.getChannel();
        long startOffset = fileDescriptor.getStartOffset();
        long declaredLength = fileDescriptor.getDeclaredLength();
        return fileChannel.map(FileChannel.MapMode.READ_ONLY, startOffset, declaredLength);
    }

    public boolean analyzeImage(String imagePath) {
        if (interpreter == null) return false;

        Bitmap bitmap = BitmapFactory.decodeFile(imagePath);
        if (bitmap == null) return false;

        TensorImage tensorImage = new TensorImage(org.tensorflow.lite.DataType.FLOAT32);
        tensorImage.load(bitmap);

        // Adaptation pour Teachable Machine (Normalisation -1 à 1 souvent utilisée)
        ImageProcessor imageProcessor = new ImageProcessor.Builder()
                .add(new ResizeOp(IMAGE_SIZE, IMAGE_SIZE, ResizeOp.ResizeMethod.BILINEAR))
                .add(new NormalizeOp(127.5f, 127.5f)) 
                .build();

        tensorImage = imageProcessor.process(tensorImage);

        float[][] output = new float[1][2];
        interpreter.run(tensorImage.getBuffer(), output);

        float scoreClass0 = output[0][0]; // Class 1 dans labels.txt
        float scoreClass1 = output[0][1]; // Class 2 dans labels.txt

        Log.d(TAG, "Analyse - Class 1: " + String.format("%.2f", scoreClass0) + " | Class 2: " + String.format("%.2f", scoreClass1));

        // PAR DÉFAUT : On considère Class 2 (index 1) comme la triche.
        // Si c'est l'inverse dans votre modèle, changez scoreClass1 par scoreClass0.
        return scoreClass1 > 0.75f;
    }

    public void close() {
        if (interpreter != null) interpreter.close();
    }
}