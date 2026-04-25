package com.example.quizzapp_security.anti_cheat;

import android.content.Context;
import android.util.Log;
import androidx.annotation.NonNull;
import androidx.camera.core.CameraSelector;
import androidx.camera.core.ImageCapture;
import androidx.camera.core.ImageCaptureException;
import androidx.camera.core.Preview;
import androidx.camera.lifecycle.ProcessCameraProvider;
import androidx.camera.view.PreviewView;
import androidx.core.content.ContextCompat;
import androidx.lifecycle.LifecycleOwner;
import com.google.common.util.concurrent.ListenableFuture;
import java.io.File;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class CameraModule {
    private ImageCapture imageCapture;
    private final ExecutorService cameraExecutor;
    private final Context context;
    private PreviewView previewView;

    public CameraModule(Context context) {
        this.context = context;
        this.cameraExecutor = Executors.newSingleThreadExecutor();
    }

    public void setPreviewView(PreviewView previewView) {
        this.previewView = previewView;
    }

    public void startCamera(LifecycleOwner lifecycleOwner) {
        ListenableFuture<ProcessCameraProvider> cameraProviderFuture = ProcessCameraProvider.getInstance(context);
        cameraProviderFuture.addListener(() -> {
            try {
                ProcessCameraProvider cameraProvider = cameraProviderFuture.get();
                
                // Préparer la Preview
                Preview preview = new Preview.Builder().build();
                if (previewView != null) {
                    preview.setSurfaceProvider(previewView.getSurfaceProvider());
                    Log.d("CameraModule", "SurfaceProvider configuré");
                }

                // Préparer la Capture d'image
                imageCapture = new ImageCapture.Builder()
                        .setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)
                        .build();

                // Utiliser la caméra frontale par défaut
                CameraSelector cameraSelector = CameraSelector.DEFAULT_FRONT_CAMERA;

                try {
                    // Détacher tout avant de rattacher
                    cameraProvider.unbindAll();
                    
                    // Attacher à l'activité
                    if (previewView != null) {
                        cameraProvider.bindToLifecycle(lifecycleOwner, cameraSelector, preview, imageCapture);
                    } else {
                        cameraProvider.bindToLifecycle(lifecycleOwner, cameraSelector, imageCapture);
                    }
                    Log.d("CameraModule", "Caméra liée avec succès");
                } catch (Exception exc) {
                    Log.e("CameraModule", "Échec de la liaison", exc);
                }
                
            } catch (ExecutionException | InterruptedException e) {
                Log.e("CameraModule", "Erreur provider: " + e.getMessage());
            }
        }, ContextCompat.getMainExecutor(context));
    }

    public void capturePhoto(File outputFile, OnPhotoCapturedListener listener) {
        if (imageCapture == null) {
            Log.e("CameraModule", "imageCapture est null");
            return;
        }

        ImageCapture.OutputFileOptions outputOptions = new ImageCapture.OutputFileOptions.Builder(outputFile).build();

        imageCapture.takePicture(outputOptions, cameraExecutor, new ImageCapture.OnImageSavedCallback() {
            @Override
            public void onImageSaved(@NonNull ImageCapture.OutputFileResults outputFileResults) {
                listener.onSuccess(outputFile);
            }

            @Override
            public void onError(@NonNull ImageCaptureException exception) {
                Log.e("CameraModule", "Erreur capture: " + exception.getMessage());
            }
        });
    }

    public interface OnPhotoCapturedListener {
        void onSuccess(File file);
    }
}