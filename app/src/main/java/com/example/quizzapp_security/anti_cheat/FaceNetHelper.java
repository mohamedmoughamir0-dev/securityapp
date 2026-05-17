package com.example.quizzapp_security.anti_cheat;

import android.content.Context;
import android.content.res.AssetFileDescriptor;
import android.graphics.Bitmap;
import org.tensorflow.lite.DataType;
import org.tensorflow.lite.Interpreter;
import org.tensorflow.lite.support.common.ops.NormalizeOp;
import org.tensorflow.lite.support.image.ImageProcessor;
import org.tensorflow.lite.support.image.TensorImage;
import org.tensorflow.lite.support.image.ops.ResizeOp;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;

public class FaceNetHelper {

    private static final int INPUT_SIZE = 160;
    private static final int EMBEDDING_SIZE = 128;
    static final String MODEL_FILE = "facenet.tflite";

    private final Interpreter interpreter;
    private final ImageProcessor imageProcessor;

    public FaceNetHelper(Context context) throws IOException {
        interpreter = new Interpreter(loadModelFile(context));
        imageProcessor = new ImageProcessor.Builder()
                .add(new ResizeOp(INPUT_SIZE, INPUT_SIZE, ResizeOp.ResizeMethod.BILINEAR))
                .add(new NormalizeOp(127.5f, 127.5f)) // maps [0,255] → [-1,1]
                .build();
    }

    private MappedByteBuffer loadModelFile(Context context) throws IOException {
        AssetFileDescriptor afd = context.getAssets().openFd(MODEL_FILE);
        FileInputStream fis = new FileInputStream(afd.getFileDescriptor());
        FileChannel channel = fis.getChannel();
        return channel.map(FileChannel.MapMode.READ_ONLY, afd.getStartOffset(), afd.getDeclaredLength());
    }

    public float[] getEmbedding(String imagePath) {
        Bitmap bitmap = FaceCropHelper.loadWithRotation(imagePath);
        if (bitmap == null) return null;
        Bitmap face = FaceCropHelper.detectAndCropFace(bitmap);
        bitmap.recycle();
        if (face == null) return null;
        float[] result = getEmbedding(face);
        face.recycle();
        return result;
    }

    public float[] getEmbedding(Bitmap bitmap) {
        TensorImage tensorImage = new TensorImage(DataType.FLOAT32);
        tensorImage.load(bitmap);
        tensorImage = imageProcessor.process(tensorImage);

        float[][] output = new float[1][EMBEDDING_SIZE];
        interpreter.run(tensorImage.getBuffer(), output);
        return output[0];
    }

    public void close() {
        if (interpreter != null) interpreter.close();
    }
}
