package com.example.quizzapp_security;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.bumptech.glide.Glide;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.List;

public class QuizActivity extends AppCompatActivity {

    private TextView tvQuestion, tvScore;
    private ImageView ivQuestion;
    private Button btnOp1, btnOp2, btnOp3, btnOp4, btnLogout;
    private List<Question> questionList;
    private int currentQuestionIndex = 0;
    private int score = 0;
    private FirebaseFirestore db;
    private FirebaseAuth mAuth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_quiz);

        db = FirebaseFirestore.getInstance();
        mAuth = FirebaseAuth.getInstance();
        questionList = new ArrayList<>();

        tvQuestion = findViewById(R.id.tvQuestion);
        tvScore = findViewById(R.id.tvScore);
        ivQuestion = findViewById(R.id.ivQuestion);
        btnOp1 = findViewById(R.id.btnOp1);
        btnOp2 = findViewById(R.id.btnOp2);
        btnOp3 = findViewById(R.id.btnOp3);
        btnOp4 = findViewById(R.id.btnOp4);
        btnLogout = findViewById(R.id.btnLogout);

        btnLogout.setOnClickListener(v -> {
            mAuth.signOut();
            Intent intent = new Intent(QuizActivity.this, MainActivity.class);
            startActivity(intent);
            finish();
        });

        loadQuestions();
    }

    private void loadQuestions() {
        db.collection("Quizzes")
                .orderBy("order", Query.Direction.ASCENDING)
                .get()
                .addOnCompleteListener(task -> {
            if (task.isSuccessful() && task.getResult() != null) {
                questionList.clear();
                for (QueryDocumentSnapshot document : task.getResult()) {
                    Question q = document.toObject(Question.class);
                    questionList.add(q);
                }
                if (!questionList.isEmpty()) {
                    displayQuestion();
                } else {
                    tvQuestion.setText("Aucune question trouvée.");
                }
            } else {
                Toast.makeText(this, "Erreur de chargement: " + task.getException().getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void displayQuestion() {
        Question q = questionList.get(currentQuestionIndex);
        tvQuestion.setText(q.getQuestion());
        btnOp1.setText(q.getOption1());
        btnOp2.setText(q.getOption2());
        btnOp3.setText(q.getOption3());
        btnOp4.setText(q.getOption4());

        if (q.getImage() != null && !q.getImage().isEmpty()) {
            ivQuestion.setVisibility(View.VISIBLE);
            if (q.getImage().startsWith("http")) {
                Glide.with(this).load(q.getImage()).into(ivQuestion);
            } else {
                int imageId = getResources().getIdentifier(q.getImage(), "drawable", getPackageName());
                if (imageId != 0) {
                    ivQuestion.setImageResource(imageId);
                } else {
                    ivQuestion.setVisibility(View.GONE);
                }
            }
        } else {
            ivQuestion.setVisibility(View.GONE);
        }

        View.OnClickListener listener = v -> {
            Button b = (Button) v;
            if (b.getText().toString().equals(q.getAnswer())) {
                score++;
                tvScore.setText("Score: " + score);
                Toast.makeText(this, "Correct !", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(this, "Faux ! C'était: " + q.getAnswer(), Toast.LENGTH_SHORT).show();
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
            tvQuestion.setText("Quiz terminé !\nVotre score: " + score + "/" + questionList.size());
            ivQuestion.setVisibility(View.GONE);
            btnOp1.setVisibility(View.GONE);
            btnOp2.setVisibility(View.GONE);
            btnOp3.setVisibility(View.GONE);
            btnOp4.setVisibility(View.GONE);
        }
    }
}