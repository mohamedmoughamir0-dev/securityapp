package com.example.quizzapp_security;

import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import java.util.List;

public class ResultAdapter extends RecyclerView.Adapter<ResultAdapter.ViewHolder> {

    private static final int COLOR_CORRECT    = Color.parseColor("#4CAF50");
    private static final int COLOR_WRONG      = Color.parseColor("#F44336");
    private static final int COLOR_CORRECT_BG = Color.parseColor("#1B3A2A");
    private static final int COLOR_WRONG_BG   = Color.parseColor("#3A1B1B");

    private final List<Question> questions;
    private final List<String>   userAnswers;

    public ResultAdapter(List<Question> questions, List<String> userAnswers) {
        this.questions   = questions;
        this.userAnswers = userAnswers;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_result_question, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Question q          = questions.get(position);
        String userAnswer   = position < userAnswers.size() ? userAnswers.get(position) : "";
        boolean isCorrect   = userAnswer.equals(q.getAnswer());

        holder.tvNumber.setText(String.valueOf(position + 1));
        holder.tvQuestion.setText(q.getQuestion());
        holder.tvUserAnswer.setText("Votre réponse : " + (userAnswer.isEmpty() ? "—" : userAnswer));

        if (isCorrect) {
            holder.colorBar.setBackgroundColor(COLOR_CORRECT);
            holder.tvIcon.setText("✓");
            holder.tvIcon.setTextColor(COLOR_CORRECT);
            holder.tvUserAnswer.setBackgroundColor(COLOR_CORRECT_BG);
            holder.tvCorrectAnswer.setVisibility(View.GONE);
        } else {
            holder.colorBar.setBackgroundColor(COLOR_WRONG);
            holder.tvIcon.setText("✗");
            holder.tvIcon.setTextColor(COLOR_WRONG);
            holder.tvUserAnswer.setBackgroundColor(COLOR_WRONG_BG);
            holder.tvCorrectAnswer.setVisibility(View.VISIBLE);
            holder.tvCorrectAnswer.setText("✓  Bonne réponse : " + q.getAnswer());
        }
    }

    @Override
    public int getItemCount() { return questions.size(); }

    static class ViewHolder extends RecyclerView.ViewHolder {
        View     colorBar;
        TextView tvNumber, tvQuestion, tvIcon, tvUserAnswer, tvCorrectAnswer;

        ViewHolder(View view) {
            super(view);
            colorBar        = view.findViewById(R.id.colorBar);
            tvNumber        = view.findViewById(R.id.tvNumber);
            tvQuestion      = view.findViewById(R.id.tvQuestion);
            tvIcon          = view.findViewById(R.id.tvIcon);
            tvUserAnswer    = view.findViewById(R.id.tvUserAnswer);
            tvCorrectAnswer = view.findViewById(R.id.tvCorrectAnswer);
        }
    }
}
