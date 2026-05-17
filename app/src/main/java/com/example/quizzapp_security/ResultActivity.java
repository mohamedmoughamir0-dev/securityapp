package com.example.quizzapp_security;

import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.example.quizzapp_security.models.CheatLog;
import java.util.ArrayList;

public class ResultActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_result);

        int score = getIntent().getIntExtra("SCORE", 0);
        int total = getIntent().getIntExtra("TOTAL", 0);
        ArrayList<String> userAnswers = getIntent().getStringArrayListExtra("USER_ANSWERS");
        @SuppressWarnings("unchecked")
        ArrayList<Question> questions = (ArrayList<Question>) getIntent().getSerializableExtra("QUESTIONS");
        @SuppressWarnings("unchecked")
        ArrayList<CheatLog> cheatLogs = (ArrayList<CheatLog>) getIntent().getSerializableExtra("CHEAT_LOGS");

        TextView tvTrophy       = findViewById(R.id.tvTrophy);
        TextView tvFinalScore   = findViewById(R.id.tvFinalScore);
        TextView tvPercentage   = findViewById(R.id.tvPercentage);
        TextView tvMessage      = findViewById(R.id.tvMessage);
        TextView tvCorrectCount = findViewById(R.id.tvCorrectCount);
        TextView tvWrongCount   = findViewById(R.id.tvWrongCount);
        RecyclerView recycler   = findViewById(R.id.recyclerResults);
        Button btnRestart       = findViewById(R.id.btnRestart);

        int pct   = total > 0 ? (score * 100 / total) : 0;
        int wrong = total - score;

        tvFinalScore.setText(score + "/" + total);
        tvPercentage.setText(pct + "%");
        tvCorrectCount.setText(String.valueOf(score));
        tvWrongCount.setText(String.valueOf(wrong));

        if (pct >= 80) {
            tvTrophy.setText("🏆");
            tvPercentage.setTextColor(Color.parseColor("#4CAF50"));
            tvMessage.setText("Excellent ! Parfaite maîtrise.");
        } else if (pct >= 60) {
            tvTrophy.setText("👍");
            tvPercentage.setTextColor(Color.parseColor("#FF9800"));
            tvMessage.setText("Bien joué ! Continuez ainsi.");
        } else if (pct >= 40) {
            tvTrophy.setText("📚");
            tvPercentage.setTextColor(Color.parseColor("#FF5722"));
            tvMessage.setText("Encore un effort !");
        } else {
            tvTrophy.setText("💪");
            tvPercentage.setTextColor(Color.parseColor("#F44336"));
            tvMessage.setText("À retravailler. Ne lâchez pas !");
        }

        if (questions != null && userAnswers != null) {
            recycler.setLayoutManager(new LinearLayoutManager(this));
            recycler.setAdapter(new ResultAdapter(questions, userAnswers));
        }

        Button btnCheatLogs = findViewById(R.id.btnCheatLogs);
        if (cheatLogs != null && !cheatLogs.isEmpty()) {
            btnCheatLogs.setVisibility(View.VISIBLE);
            btnCheatLogs.setText("⚠️  " + cheatLogs.size() + " alerte(s) détectée(s)");
        }
        btnCheatLogs.setOnClickListener(v -> {
            Intent intent = new Intent(ResultActivity.this, CheatLogsActivity.class);
            intent.putExtra("CHEAT_LOGS", cheatLogs);
            startActivity(intent);
        });

        btnRestart.setOnClickListener(v -> {
            Intent intent = new Intent(ResultActivity.this, MainActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
            startActivity(intent);
            finish();
        });
    }
}
