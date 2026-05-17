package com.example.quizzapp_security.anti_cheat;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.graphics.Rect;
import android.media.ExifInterface;
import com.google.android.gms.tasks.Tasks;
import com.google.mlkit.vision.common.InputImage;
import com.google.mlkit.vision.face.Face;
import com.google.mlkit.vision.face.FaceDetection;
import com.google.mlkit.vision.face.FaceDetector;
import com.google.mlkit.vision.face.FaceDetectorOptions;
import java.io.IOException;
import java.util.List;

public class FaceCropHelper {

    private static final int PADDING_PERCENT = 20;

    // Charge le bitmap en corrigeant la rotation EXIF (problème courant caméra frontale Android)
    public static Bitmap loadWithRotation(String imagePath) {
        Bitmap bitmap = BitmapFactory.decodeFile(imagePath);
        if (bitmap == null) return null;
        try {
            ExifInterface exif = new ExifInterface(imagePath);
            int orientation = exif.getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL);
            Matrix matrix = new Matrix();
            switch (orientation) {
                case ExifInterface.ORIENTATION_ROTATE_90:    matrix.postRotate(90);   break;
                case ExifInterface.ORIENTATION_ROTATE_180:   matrix.postRotate(180);  break;
                case ExifInterface.ORIENTATION_ROTATE_270:   matrix.postRotate(270);  break;
                case ExifInterface.ORIENTATION_FLIP_HORIZONTAL: matrix.preScale(-1, 1); break;
                default: return bitmap;
            }
            Bitmap rotated = Bitmap.createBitmap(bitmap, 0, 0, bitmap.getWidth(), bitmap.getHeight(), matrix, true);
            bitmap.recycle();
            return rotated;
        } catch (IOException e) {
            return bitmap;
        }
    }

    // Détecte le premier visage dans le bitmap et retourne un recadrage avec marge.
    // Doit être appelé depuis un thread d'arrière-plan (Tasks.await bloque le thread).
    // Retourne null si aucun visage détecté.
    public static Bitmap detectAndCropFace(Bitmap source) {
        FaceDetectorOptions options = new FaceDetectorOptions.Builder()
                .setPerformanceMode(FaceDetectorOptions.PERFORMANCE_MODE_FAST)
                .build();
        FaceDetector detector = FaceDetection.getClient(options);
        try {
            InputImage image = InputImage.fromBitmap(source, 0);
            List<Face> faces = Tasks.await(detector.process(image));
            if (faces.isEmpty()) return null;

            Face face = faces.get(0);
            Rect box = face.getBoundingBox();
            int padX = box.width()  * PADDING_PERCENT / 100;
            int padY = box.height() * PADDING_PERCENT / 100;
            int left   = Math.max(0, box.left   - padX);
            int top    = Math.max(0, box.top    - padY);
            int right  = Math.min(source.getWidth(),  box.right  + padX);
            int bottom = Math.min(source.getHeight(), box.bottom + padY);
            int width  = right - left;
            int height = bottom - top;
            if (width <= 0 || height <= 0) return null;
            return Bitmap.createBitmap(source, left, top, width, height);
        } catch (Exception e) {
            return null;
        } finally {
            detector.close();
        }
    }
}
