package com.example.quizzapp_security;

import android.graphics.Color;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.example.quizzapp_security.models.CheatLog;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class CheatLogsActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_cheat_logs);

        @SuppressWarnings("unchecked")
        ArrayList<CheatLog> logs = (ArrayList<CheatLog>)
                getIntent().getSerializableExtra("CHEAT_LOGS");

        TextView btnBack         = findViewById(R.id.btnBack);
        LinearLayout statsRow    = findViewById(R.id.statsRow);
        LinearLayout emptyLayout = findViewById(R.id.emptyLayout);
        RecyclerView recycler    = findViewById(R.id.recyclerLogs);

        btnBack.setOnClickListener(v -> finish());

        if (logs == null || logs.isEmpty()) {
            emptyLayout.setVisibility(View.VISIBLE);
            recycler.setVisibility(View.GONE);
            return;
        }

        buildStatsRow(statsRow, logs);

        recycler.setLayoutManager(new LinearLayoutManager(this));
        recycler.setAdapter(new CheatLogAdapter(logs));
    }

    private void buildStatsRow(LinearLayout row, List<CheatLog> logs) {
        Map<String, Integer> counts = new HashMap<>();
        for (CheatLog log : logs) {
            String key = log.alert_type != null ? log.alert_type : "?";
            counts.put(key, counts.getOrDefault(key, 0) + 1);
        }

        String[][] types = {
            {"FACE",   "👤", "#F44336"},
            {"GPS",    "📍", "#FF9800"},
            {"MIC",    "🎙", "#9C27B0"},
            {"TFLITE", "👁", "#2196F3"}
        };

        for (String[] t : types) {
            String key = t[0];
            if (!counts.containsKey(key)) continue;

            LinearLayout card = new LinearLayout(this);
            card.setOrientation(LinearLayout.VERTICAL);
            card.setGravity(Gravity.CENTER);
            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                    0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f);
            params.setMargins(8, 0, 8, 0);
            card.setLayoutParams(params);
            card.setPadding(12, 12, 12, 12);
            card.setBackgroundColor(Color.parseColor("#252540"));

            TextView emoji = new TextView(this);
            emoji.setText(t[1] + " " + counts.get(key));
            emoji.setTextColor(Color.parseColor(t[2]));
            emoji.setTextSize(16f);
            emoji.setTypeface(null, android.graphics.Typeface.BOLD);
            emoji.setGravity(Gravity.CENTER);

            TextView label = new TextView(this);
            label.setText(key);
            label.setTextColor(Color.parseColor("#888AAA"));
            label.setTextSize(10f);
            label.setGravity(Gravity.CENTER);

            card.addView(emoji);
            card.addView(label);
            row.addView(card);
        }
    }
}
