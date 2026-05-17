package com.example.quizzapp_security;

import android.Manifest;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.camera.view.PreviewView;
import androidx.core.app.ActivityCompat;

import com.bumptech.glide.Glide;
import com.example.quizzapp_security.anti_cheat.MicModule;
import com.example.quizzapp_security.anti_cheat.SurveillanceManager;
import com.example.quizzapp_security.models.CheatLog;
import com.example.quizzapp_security.models.SessionLog;
import com.example.quizzapp_security.network.SupabaseApi;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class QuizActivity extends AppCompatActivity implements SurveillanceManager.SurveillanceListener {

    private TextView tvQuestion, tvScore, tvAlert, tvQuestionCounter;
    private ImageView ivQuestion;
    private ProgressBar quizProgressBar;
    private Button btnOp1, btnOp2, btnOp3, btnOp4;
    private android.widget.ImageButton btnLogout;
    private Switch switchMic;
    private List<Question> questionList;
    private ArrayList<String> userAnswers;
    private int currentQuestionIndex = 0;
    private int score = 0;
    
    private String lastGpsError = null;
    private String lastCameraError = null;
    private String lastFaceError = null;
    private String lastMicError = null;
    private ArrayList<CheatLog> cheatLogs = new ArrayList<>();

    private FirebaseFirestore db;
    private FirebaseAuth mAuth;
    private SurveillanceManager surveillanceManager;
    private SupabaseApi supabaseApi;
    private static final String SUPABASE_URL = "https://ngxqgwrblnspkavimkez.supabase.co/";
    private static final String SUPABASE_KEY = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJzdXBhYmFzZSIsInJlZiI6Im5neHFnd3JibG5zcGthdmlta2V6Iiwicm9sZSI6ImFub24iLCJpYXQiOjE3NzQxMDUwMTAsImV4cCI6MjA4OTY4MTAxMH0.XyBaB5D37WQXFV5q34JAHBNr04ASH0dy8bNrpRjh5ik";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_quiz);

        db = FirebaseFirestore.getInstance();
        mAuth = FirebaseAuth.getInstance();
        questionList = new ArrayList<>();
        userAnswers = new ArrayList<>();

        tvQuestion        = findViewById(R.id.tvQuestion);
        tvScore           = findViewById(R.id.tvScore);
        tvAlert           = findViewById(R.id.tvAlert);
        tvQuestionCounter = findViewById(R.id.tvQuestionCounter);
        ivQuestion        = findViewById(R.id.ivQuestion);
        quizProgressBar   = findViewById(R.id.quizProgressBar);
        btnOp1            = findViewById(R.id.btnOp1);
        btnOp2            = findViewById(R.id.btnOp2);
        btnOp3            = findViewById(R.id.btnOp3);
        btnOp4            = findViewById(R.id.btnOp4);
        btnLogout         = findViewById(R.id.btnLogout);
        switchMic = findViewById(R.id.switchMic);

        switchMic.setChecked(false);
        switchMic.setOnCheckedChangeListener((btn, isChecked) -> {
            if (surveillanceManager == null) return;
            if (isChecked) {
                if (ActivityCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED) {
                    surveillanceManager.enableMic();
                    Toast.makeText(this, "Micro activé", Toast.LENGTH_SHORT).show();
                } else {
                    switchMic.setChecked(false);
                    Toast.makeText(this, "Permission microphone refusée", Toast.LENGTH_SHORT).show();
                }
            } else {
                surveillanceManager.disableMic();
                Toast.makeText(this, "Micro désactivé", Toast.LENGTH_SHORT).show();
            }
        });

        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl(SUPABASE_URL)
                .addConverterFactory(GsonConverterFactory.create())
                .build();
        supabaseApi = retrofit.create(SupabaseApi.class);

        if (checkPermissions()) {
            initAntiCheat();
        } else {
            requestPermissions();
        }

        startSessionInSupabase();

        btnLogout.setOnClickListener(v -> {
            if (surveillanceManager != null) surveillanceManager.stopSurveillance();
            endSessionInSupabase(score, questionList.size());
            mAuth.signOut();
            startActivity(new Intent(QuizActivity.this, MainActivity.class));
            finish();
        });

        loadQuestions();
    }

    private void initAntiCheat() {
        PreviewView previewView = findViewById(R.id.previewView);
        surveillanceManager = new SurveillanceManager(this, this, previewView, this);
        surveillanceManager.startSurveillance();
    }

    private boolean checkPermissions() {
        return ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED &&
               ActivityCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED;
    }

    private void requestPermissions() {
        ActivityCompat.requestPermissions(this, new String[]{
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.CAMERA,
                Manifest.permission.RECORD_AUDIO
        }, 100);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == 100) {
            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED &&
                ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
                initAntiCheat();
            }
        }
    }

    @Override
    public void onAlert(String type, String details) {
        runOnUiThread(() -> {
            if (type.equals("GPS"))    lastGpsError    = details;
            if (type.equals("TFLITE")) lastCameraError = details;
            if (type.equals("FACE"))   lastFaceError   = details;
            if (type.equals("MIC"))    lastMicError    = details;
            updateAlertDisplay();
            sendCheatLogToSupabase(type, details);
        });
    }

    @Override
    public void onNormal(String type) {
        runOnUiThread(() -> {
            if (type.equals("GPS"))    lastGpsError    = null;
            if (type.equals("TFLITE")) lastCameraError = null;
            if (type.equals("FACE"))   lastFaceError   = null;
            if (type.equals("MIC"))    lastMicError    = null;
            updateAlertDisplay();
        });
    }

    private void updateAlertDisplay() {
        if (lastFaceError != null) {
            tvAlert.setVisibility(View.VISIBLE);
            tvAlert.setText(lastFaceError);
        } else if (lastMicError != null) {
            tvAlert.setVisibility(View.VISIBLE);
            tvAlert.setText("🎙 Voix détectée — communication suspecte !");
        } else if (lastGpsError != null) {
            tvAlert.setVisibility(View.VISIBLE);
            tvAlert.setText("📍 Hors zone autorisée : " + lastGpsError);
        } else if (lastCameraError != null) {
            tvAlert.setVisibility(View.VISIBLE);
            tvAlert.setText("👁 Comportement suspect détecté !");
        } else {
            tvAlert.setVisibility(View.GONE);
        }
    }

    private static final String TAG_SUPA = "SupabaseAPI";

    private String nowIso() {
        return new java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'",
                java.util.Locale.getDefault()).format(new java.util.Date());
    }

    private void startSessionInSupabase() {
        if (mAuth.getCurrentUser() == null) return;
        String userId = mAuth.getCurrentUser().getUid();
        String email  = mAuth.getCurrentUser().getEmail();
        SessionLog session = new SessionLog(userId, email);
        session.started_at = nowIso();
        supabaseApi.startSession(SUPABASE_KEY, "Bearer " + SUPABASE_KEY,
                "application/json", "return=minimal", session)
                .enqueue(new Callback<Void>() {
                    @Override public void onResponse(Call<Void> call, Response<Void> response) {
                        if (response.isSuccessful()) {
                            android.util.Log.d(TAG_SUPA, "Session démarrée OK");
                        } else {
                            try { android.util.Log.e(TAG_SUPA, "Erreur start session " + response.code() + " : " + response.errorBody().string()); }
                            catch (Exception e) { android.util.Log.e(TAG_SUPA, "Erreur " + response.code()); }
                        }
                    }
                    @Override public void onFailure(Call<Void> call, Throwable t) {
                        android.util.Log.e(TAG_SUPA, "Réseau start session : " + t.getMessage());
                    }
                });
    }

    private void endSessionInSupabase(int score, int total) {
        if (mAuth.getCurrentUser() == null) return;
        String userId = mAuth.getCurrentUser().getUid();
        SessionLog session = new SessionLog(userId, mAuth.getCurrentUser().getEmail());
        session.score    = score;
        session.total    = total;
        session.ended_at = nowIso();
        supabaseApi.endSession(SUPABASE_KEY, "Bearer " + SUPABASE_KEY,
                "application/json", "eq." + userId, "is.null", session)
                .enqueue(new Callback<Void>() {
                    @Override public void onResponse(Call<Void> call, Response<Void> response) {
                        android.util.Log.d(TAG_SUPA, "Session terminée, code=" + response.code());
                    }
                    @Override public void onFailure(Call<Void> call, Throwable t) {
                        android.util.Log.e(TAG_SUPA, "Réseau end session : " + t.getMessage());
                    }
                });
    }

    private void sendCheatLogToSupabase(String type, String details) {
        String userId = mAuth.getCurrentUser() != null ? mAuth.getCurrentUser().getUid() : "unknown";
        CheatLog log = new CheatLog(userId, "quiz_security_01", type, details);
        cheatLogs.add(log);
        supabaseApi.insertLog(SUPABASE_KEY, "Bearer " + SUPABASE_KEY,
                "application/json", "return=minimal", log)
                .enqueue(new Callback<Void>() {
                    @Override public void onResponse(Call<Void> call, Response<Void> response) {
                        if (!response.isSuccessful()) {
                            try { android.util.Log.e(TAG_SUPA, "Erreur cheat_log " + response.code() + " : " + response.errorBody().string()); }
                            catch (Exception e) { android.util.Log.e(TAG_SUPA, "Erreur " + response.code()); }
                        }
                    }
                    @Override public void onFailure(Call<Void> call, Throwable t) {
                        android.util.Log.e(TAG_SUPA, "Réseau cheat_log : " + t.getMessage());
                    }
                });
    }

    private void loadQuestions() {
        db.collection("Quizzes").orderBy("order", Query.Direction.ASCENDING).get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful() && task.getResult() != null) {
                        for (QueryDocumentSnapshot document : task.getResult()) {
                            questionList.add(document.toObject(Question.class));
                        }
                        if (!questionList.isEmpty()) displayQuestion();
                    }
                });
    }

    private void displayQuestion() {
        if (currentQuestionIndex >= questionList.size()) return;
        Question q = questionList.get(currentQuestionIndex);
        Animation fadeIn = AnimationUtils.loadAnimation(this, R.anim.fade_in);
        findViewById(R.id.questionCard).startAnimation(fadeIn);
        tvQuestion.setText(q.getQuestion());
        tvQuestionCounter.setText((currentQuestionIndex + 1) + " / " + questionList.size());
        btnOp1.setText(q.getOption1());
        btnOp2.setText(q.getOption2());
        btnOp3.setText(q.getOption3());
        btnOp4.setText(q.getOption4());
        int progress = (int) (((float) (currentQuestionIndex + 1) / questionList.size()) * 100);
        quizProgressBar.setProgress(progress);
        if (q.getImage() != null && !q.getImage().isEmpty()) {
            ivQuestion.setVisibility(View.VISIBLE);
            Glide.with(this).load(q.getImage()).into(ivQuestion);
        } else {
            ivQuestion.setVisibility(View.GONE);
        }
        View.OnClickListener listener = v -> {
            String chosen = ((Button) v).getText().toString();
            userAnswers.add(chosen);
            if (chosen.equals(q.getAnswer())) {
                score++;
                tvScore.setText("Score: " + score);
            }
            nextQuestion();
        };
        btnOp1.setOnClickListener(listener); btnOp2.setOnClickListener(listener);
        btnOp3.setOnClickListener(listener); btnOp4.setOnClickListener(listener);
    }

    private void nextQuestion() {
        currentQuestionIndex++;
        if (currentQuestionIndex < questionList.size()) {
            displayQuestion();
        } else {
            if (surveillanceManager != null) surveillanceManager.stopSurveillance();
            endSessionInSupabase(score, questionList.size());
            Intent intent = new Intent(this, ResultActivity.class);
            intent.putExtra("SCORE", score);
            intent.putExtra("TOTAL", questionList.size());
            intent.putExtra("QUESTIONS", new ArrayList<>(questionList));
            intent.putStringArrayListExtra("USER_ANSWERS", userAnswers);
            intent.putExtra("CHEAT_LOGS", cheatLogs);
            startActivity(intent);
            finish();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (surveillanceManager != null) surveillanceManager.stopSurveillance();
        endSessionInSupabase(score, questionList.size());
    }
}