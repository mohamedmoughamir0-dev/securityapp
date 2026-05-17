package com.example.quizzapp_security.anti_cheat;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import androidx.camera.view.PreviewView;
import androidx.lifecycle.LifecycleOwner;
import java.io.File;
import java.io.IOException;

public class SurveillanceManager {

    public static final String PREFS_SECURITY = "security_settings";
    public static final String KEY_MIC_ENABLED = "mic_enabled";

    private final GpsModule gpsModule;
    private final CameraModule cameraModule;
    private final TfLiteModule tfLiteModule;
    private final FaceNetHelper faceNetHelper;
    private final FaceVerificationManager faceVerificationManager;
    private MicModule micModule;
    private final Handler handler = new Handler(Looper.getMainLooper());
    private final Context context;
    private boolean isRunning = false;
    private volatile boolean isCapturing = false;
    private static final int CAPTURE_INTERVAL = 600;

    private int consecutiveCheatFrames = 0;
    private static final int CHEAT_THRESHOLD = 2;

    private int consecutiveFaceFailFrames = 0;
    private static final int FACE_CHEAT_THRESHOLD = 1;

    public interface SurveillanceListener {
        void onAlert(String type, String details);
        void onNormal(String type); // Pour cacher l'alerte
    }

    private final SurveillanceListener listener;

    public SurveillanceManager(Context context, LifecycleOwner lifecycleOwner, PreviewView previewView, SurveillanceListener listener) {
        this.context = context;
        this.listener = listener;
        
        this.gpsModule = new GpsModule(context, new GpsModule.OnLocationAlertListener() {
            @Override
            public void onOutsideZone(String details) {
                listener.onAlert("GPS", details);
            }

            @Override
            public void onInsideZone() {
                listener.onNormal("GPS");
            }
        });

        this.cameraModule = new CameraModule(context);
        if (previewView != null) this.cameraModule.setPreviewView(previewView);
        this.cameraModule.startCamera(lifecycleOwner);
        this.tfLiteModule = new TfLiteModule(context, "model_unquant.tflite");

        this.faceVerificationManager = new FaceVerificationManager(context);
        FaceNetHelper helper = null;
        try {
            helper = new FaceNetHelper(context);
        } catch (Exception e) {
            Log.e("SurveillanceManager", "Erreur chargement FaceNet: " + e.getMessage());
        }
        this.faceNetHelper = helper;

    }

    public void enableMic() {
        if (micModule != null) micModule.stop();
        micModule = new MicModule(new MicModule.OnSpeechListener() {
            @Override
            public void onSpeechDetected() {
                Log.w("Surveillance", "VOIX DÉTECTÉE");
                listener.onAlert("MIC", "Voix détectée — communication suspecte");
            }
            @Override
            public void onSilence() {
                listener.onNormal("MIC");
            }
        });
        if (isRunning) micModule.start();
    }

    public void disableMic() {
        if (micModule != null) {
            micModule.stop();
            micModule = null;
        }
        listener.onNormal("MIC");
    }

    public void startSurveillance() {
        if (isRunning) return;
        isRunning = true;
        if (micModule != null) micModule.start();
        scheduleTasks();
    }

    public void stopSurveillance() {
        isRunning = false;
        handler.removeCallbacksAndMessages(null);
        if (tfLiteModule != null) tfLiteModule.close();
        if (faceNetHelper != null) faceNetHelper.close();
        if (micModule != null) micModule.stop();
    }

    private void scheduleTasks() {
        if (!isRunning) return;
        gpsModule.checkLocation();
        captureAndAnalyze();
        handler.postDelayed(this::scheduleTasks, CAPTURE_INTERVAL);
    }

    private void captureAndAnalyze() {
        if (isCapturing) return; // éviter la queue si ML Kit est encore en cours
        isCapturing = true;
        File photoFile = new File(context.getCacheDir(), "anti_cheat_" + System.currentTimeMillis() + ".jpg");
        cameraModule.capturePhoto(photoFile, file -> {
            try {
                // --- Behaviour detection (TFLite model) ---
                boolean isCheating = tfLiteModule.analyzeImage(file.getAbsolutePath());
                if (isCheating) {
                    consecutiveCheatFrames++;
                    if (consecutiveCheatFrames >= CHEAT_THRESHOLD) {
                        Log.w("Surveillance", "TRICHE CONFIRMÉE");
                        listener.onAlert("TFLITE", "Comportement suspect détecté");
                    }
                } else {
                    consecutiveCheatFrames = 0;
                    listener.onNormal("TFLITE");
                }

                // --- Face verification (FaceNet, offline) ---
                if (faceNetHelper == null) {
                    Log.e("Surveillance", "FaceNet non chargé — vérification faciale impossible");
                } else {
                    float[] embedding = faceNetHelper.getEmbedding(file.getAbsolutePath());
                    if (embedding == null) {
                        // Caméra couverte ou aucun visage — alerte immédiate
                        consecutiveFaceFailFrames++;
                        if (consecutiveFaceFailFrames >= FACE_CHEAT_THRESHOLD) {
                            Log.w("Surveillance", "AUCUN VISAGE DÉTECTÉ");
                            listener.onAlert("FACE", "⚠ Aucun visage détecté !");
                        }
                    } else if (faceVerificationManager.hasEmbedding()
                            && !faceVerificationManager.verify(embedding)) {
                        // Visage présent mais mauvaise personne
                        consecutiveFaceFailFrames++;
                        if (consecutiveFaceFailFrames >= FACE_CHEAT_THRESHOLD) {
                            Log.w("Surveillance", "VISAGE NON RECONNU");
                            listener.onAlert("FACE", "⚠ Visage non reconnu !");
                        }
                    } else {
                        consecutiveFaceFailFrames = 0;
                        listener.onNormal("FACE");
                    }
                }
            } catch (Exception e) {
                Log.e("Surveillance", "Erreur analyse: " + e.getMessage());
            } finally {
                if (file.exists()) file.delete();
                isCapturing = false; // libère pour la prochaine capture
            }
        });
    }
}
