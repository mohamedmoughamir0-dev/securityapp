package com.example.quizzapp_security.anti_cheat;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import androidx.camera.view.PreviewView;
import androidx.lifecycle.LifecycleOwner;
import java.io.File;

public class SurveillanceManager {
    private final GpsModule gpsModule;
    private final CameraModule cameraModule;
    private final TfLiteModule tfLiteModule;
    private final Handler handler = new Handler(Looper.getMainLooper());
    private final Context context;
    private boolean isRunning = false;
    private static final int CAPTURE_INTERVAL = 5000;
    
    private int consecutiveCheatFrames = 0;
    private static final int CHEAT_THRESHOLD = 2; // Il faut 2 détections de suite pour alerter

    public interface SurveillanceListener {
        void onAlert(String type, String details);
        void onNormal(); // Pour cacher l'alerte
    }

    private final SurveillanceListener listener;

    public SurveillanceManager(Context context, LifecycleOwner lifecycleOwner, PreviewView previewView, SurveillanceListener listener) {
        this.context = context;
        this.listener = listener;
        this.gpsModule = new GpsModule(context, details -> listener.onAlert("GPS", details));
        this.cameraModule = new CameraModule(context);
        if (previewView != null) this.cameraModule.setPreviewView(previewView);
        this.cameraModule.startCamera(lifecycleOwner);
        this.tfLiteModule = new TfLiteModule(context, "model_unquant.tflite");
    }

    public void startSurveillance() {
        if (isRunning) return;
        isRunning = true;
        scheduleTasks();
    }

    public void stopSurveillance() {
        isRunning = false;
        handler.removeCallbacksAndMessages(null);
        if (tfLiteModule != null) tfLiteModule.close();
    }

    private void scheduleTasks() {
        if (!isRunning) return;
        gpsModule.checkLocation();
        captureAndAnalyze();
        handler.postDelayed(this::scheduleTasks, CAPTURE_INTERVAL);
    }

    private void captureAndAnalyze() {
        File photoFile = new File(context.getCacheDir(), "anti_cheat_" + System.currentTimeMillis() + ".jpg");
        cameraModule.capturePhoto(photoFile, file -> {
            boolean isCheating = tfLiteModule.analyzeImage(file.getAbsolutePath());
            
            if (isCheating) {
                consecutiveCheatFrames++;
                if (consecutiveCheatFrames >= CHEAT_THRESHOLD) {
                    Log.w("Surveillance", "TRICHE CONFIRMÉE");
                    listener.onAlert("TFLITE", "Comportement suspect détecté");
                }
            } else {
                consecutiveCheatFrames = 0;
                listener.onNormal(); // Tout va bien, on efface l'alerte
            }
            
            if (file.exists()) file.delete();
        });
    }
}