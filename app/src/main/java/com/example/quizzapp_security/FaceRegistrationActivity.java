package com.example.quizzapp_security;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.camera.view.PreviewView;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import com.example.quizzapp_security.anti_cheat.CameraModule;
import com.example.quizzapp_security.anti_cheat.FaceNetHelper;
import com.example.quizzapp_security.anti_cheat.FaceVerificationManager;
import com.google.firebase.auth.FirebaseAuth;
import java.io.File;
import java.io.IOException;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;

public class FaceRegistrationActivity extends AppCompatActivity {

    private static final String SUPABASE_URL    = "https://ngxqgwrblnspkavimkez.supabase.co/";
    private static final String SUPABASE_KEY    = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJzdXBhYmFzZSIsInJlZiI6Im5neHFnd3JibG5zcGthdmlta2V6Iiwicm9sZSI6ImFub24iLCJpYXQiOjE3NzQxMDUwMTAsImV4cCI6MjA4OTY4MTAxMH0.XyBaB5D37WQXFV5q34JAHBNr04ASH0dy8bNrpRjh5ik";
    private static final String STORAGE_BUCKET  = "face-registrations";
    private static final int    REQ_CAMERA      = 101;

    // Section capture
    private LinearLayout captureLayout;
    private PreviewView  previewView;
    private Button       btnCapture, btnSkip;
    private ProgressBar  progressBar;
    private TextView     tvStatus;

    // Section résultat
    private LinearLayout resultLayout;
    private TextView     tvResultIcon, tvResultTitle, tvResultSubtitle;
    private androidx.cardview.widget.CardView successInfoCard;
    private Button       btnContinue, btnRetry;

    private CameraModule            cameraModule;
    private FaceNetHelper           faceNetHelper;
    private FaceVerificationManager faceVerificationManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_face_registration);

        // Capture
        captureLayout  = findViewById(R.id.captureLayout);
        previewView    = findViewById(R.id.previewView);
        btnCapture     = findViewById(R.id.btnCapture);
        btnSkip        = findViewById(R.id.btnSkip);
        progressBar    = findViewById(R.id.progressBar);
        tvStatus       = findViewById(R.id.tvStatus);

        // Résultat
        resultLayout      = findViewById(R.id.resultLayout);
        tvResultIcon      = findViewById(R.id.tvResultIcon);
        tvResultTitle     = findViewById(R.id.tvResultTitle);
        tvResultSubtitle  = findViewById(R.id.tvResultSubtitle);
        successInfoCard   = findViewById(R.id.successInfoCard);
        btnContinue       = findViewById(R.id.btnContinue);
        btnRetry          = findViewById(R.id.btnRetry);

        faceVerificationManager = new FaceVerificationManager(this);
        btnSkip.setOnClickListener(v -> goToQuiz());
        btnContinue.setOnClickListener(v -> goToQuiz());
        btnRetry.setOnClickListener(v -> showCaptureState());

        try {
            faceNetHelper = new FaceNetHelper(this);
        } catch (Exception e) {
            showErrorState("Impossible de charger FaceNet.\n" + e.getMessage());
            return;
        }

        btnCapture.setOnClickListener(v -> captureAndRegister());
        initCamera();
    }

    private void initCamera() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
                == PackageManager.PERMISSION_GRANTED) {
            cameraModule = new CameraModule(this);
            cameraModule.setPreviewView(previewView);
            cameraModule.startCamera(this);
        } else {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.CAMERA}, REQ_CAMERA);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQ_CAMERA) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                cameraModule = new CameraModule(this);
                cameraModule.setPreviewView(previewView);
                cameraModule.startCamera(this);
            } else {
                showErrorState("Permission caméra refusée.\nAutorisez la caméra dans les paramètres.");
            }
        }
    }

    private void captureAndRegister() {
        if (cameraModule == null) {
            initCamera();
            return;
        }
        btnCapture.setEnabled(false);
        progressBar.setVisibility(View.VISIBLE);
        tvStatus.setText("Analyse en cours…");

        File photoFile = new File(getCacheDir(), "face_reg_" + System.currentTimeMillis() + ".jpg");
        cameraModule.capturePhoto(photoFile, file -> {
            try {
                float[] embedding = faceNetHelper.getEmbedding(file.getAbsolutePath());

                if (embedding == null) {
                    file.delete();
                    runOnUiThread(() -> showErrorState("Aucun visage détecté.\nVérifiez l'éclairage et recentrez votre visage."));
                    return;
                }

                faceVerificationManager.saveEmbedding(embedding);

                String uid = FirebaseAuth.getInstance().getCurrentUser() != null
                        ? FirebaseAuth.getInstance().getCurrentUser().getUid() : null;
                if (uid != null) uploadFacePhoto(file, uid);
                else file.delete();

                runOnUiThread(() -> showSuccessState());
            } catch (Exception e) {
                file.delete();
                runOnUiThread(() -> showErrorState("Erreur lors de l'analyse : " + e.getMessage()));
            }
        });
    }

    private void showSuccessState() {
        captureLayout.setVisibility(View.GONE);
        resultLayout.setVisibility(View.VISIBLE);

        tvResultIcon.setText("✅");
        tvResultTitle.setText("Identité vérifiée !");
        tvResultSubtitle.setText("Votre visage a été enregistré avec succès");
        successInfoCard.setVisibility(View.VISIBLE);
        btnContinue.setVisibility(View.VISIBLE);
        btnRetry.setVisibility(View.GONE);
        progressBar.setVisibility(View.GONE);
    }

    private void showErrorState(String message) {
        progressBar.setVisibility(View.GONE);

        captureLayout.setVisibility(View.GONE);
        resultLayout.setVisibility(View.VISIBLE);

        tvResultIcon.setText("❌");
        tvResultTitle.setText("Échec de la capture");
        tvResultSubtitle.setText(message);
        successInfoCard.setVisibility(View.GONE);
        btnContinue.setVisibility(View.GONE);
        btnRetry.setVisibility(View.VISIBLE);
    }

    private void showCaptureState() {
        resultLayout.setVisibility(View.GONE);
        captureLayout.setVisibility(View.VISIBLE);
        btnCapture.setEnabled(true);
        tvStatus.setText("Appuyez sur Capturer quand vous êtes prêt");
    }

    private void uploadFacePhoto(File photoFile, String uid) {
        new Thread(() -> {
            try {
                RequestBody body = RequestBody.create(photoFile, MediaType.parse("image/jpeg"));
                Request request = new Request.Builder()
                        .url(SUPABASE_URL + "storage/v1/object/" + STORAGE_BUCKET + "/" + uid + "/profile.jpg")
                        .put(body)
                        .addHeader("Authorization", "Bearer " + SUPABASE_KEY)
                        .addHeader("Content-Type", "image/jpeg")
                        .build();
                new OkHttpClient().newCall(request).execute();
                Log.d("FaceReg", "Photo uploadée pour uid: " + uid);
            } catch (Exception e) {
                Log.e("FaceReg", "Échec upload photo: " + e.getMessage());
            } finally {
                photoFile.delete();
            }
        }).start();
    }

    private void goToQuiz() {
        startActivity(new Intent(this, QuizActivity.class));
        finish();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (faceNetHelper != null) faceNetHelper.close();
    }
}
