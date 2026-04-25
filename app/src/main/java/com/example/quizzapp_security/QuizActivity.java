package com.example.quizzapp_security;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.camera.view.PreviewView;
import androidx.core.app.ActivityCompat;

import com.bumptech.glide.Glide;
import com.example.quizzapp_security.anti_cheat.SurveillanceManager;
import com.example.quizzapp_security.models.CheatLog;
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

    private TextView tvQuestion, tvScore, tvAlert;
    private ImageView ivQuestion;
    private ProgressBar quizProgressBar;
    private Button btnOp1, btnOp2, btnOp3, btnOp4, btnLogout;
    private List<Question> questionList;
    private int currentQuestionIndex = 0;
    private int score = 0;
    
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

        tvQuestion = findViewById(R.id.tvQuestion);
        tvScore = findViewById(R.id.tvScore);
        tvAlert = findViewById(R.id.tvAlert);
        ivQuestion = findViewById(R.id.ivQuestion);
        quizProgressBar = findViewById(R.id.quizProgressBar);
        btnOp1 = findViewById(R.id.btnOp1);
        btnOp2 = findViewById(R.id.btnOp2);
        btnOp3 = findViewById(R.id.btnOp3);
        btnOp4 = findViewById(R.id.btnOp4);
        btnLogout = findViewById(R.id.btnLogout);

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

        btnLogout.setOnClickListener(v -> {
            if (surveillanceManager != null) surveillanceManager.stopSurveillance();
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
                Manifest.permission.CAMERA
        }, 100);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == 100) {
            boolean allGranted = true;
            for (int result : grantResults) {
                if (result != PackageManager.PERMISSION_GRANTED) {
                    allGranted = false;
                    break;
                }
            }
            if (allGranted) initAntiCheat();
        }
    }

    @Override
    public void onAlert(String type, String details) {
        runOnUiThread(() -> {
            tvAlert.setVisibility(View.VISIBLE);
            tvAlert.setText("ALERTE " + type + " : " + details);
            sendCheatLogToSupabase(type, details);
        });
    }

    @Override
    public void onNormal() {
        runOnUiThread(() -> {
            if (tvAlert.getVisibility() == View.VISIBLE) {
                tvAlert.setVisibility(View.GONE);
            }
        });
    }

    private void sendCheatLogToSupabase(String type, String details) {
        String userId = mAuth.getCurrentUser() != null ? mAuth.getCurrentUser().getUid() : "unknown";
        CheatLog log = new CheatLog(userId, "quiz_security_01", type, details);
        
        supabaseApi.insertLog(SUPABASE_KEY, "Bearer " + SUPABASE_KEY, "application/json", "return=minimal", log)
                .enqueue(new Callback<Void>() {
                    @Override public void onResponse(Call<Void> call, Response<Void> response) {}
                    @Override public void onFailure(Call<Void> call, Throwable t) {}
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
            if (((Button) v).getText().toString().equals(q.getAnswer())) {
                score++;
                tvScore.setText("Score: " + score);
            }
            nextQuestion();
        };

        btnOp1.setOnClickListener(listener);
        btnOp2.setOnClickListener(listener);
        btnOp3.setOnClickListener(listener);
        btnOp4.setOnClickListener(listener);
    }

    private void nextQuestion() {
        currentQuestionIndex++;
        if (currentQuestionIndex < questionList.size()) {
            displayQuestion();
        } else {
            if (surveillanceManager != null) surveillanceManager.stopSurveillance();
            Intent intent = new Intent(this, ResultActivity.class);
            intent.putExtra("SCORE", score);
            intent.putExtra("TOTAL", questionList.size());
            startActivity(intent);
            finish();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (surveillanceManager != null) surveillanceManager.stopSurveillance();
    }
}