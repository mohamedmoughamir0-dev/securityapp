package com.example.quizzapp_security;

import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.example.quizzapp_security.models.CheatLog;
import java.util.List;

public class CheatLogAdapter extends RecyclerView.Adapter<CheatLogAdapter.ViewHolder> {

    private final List<CheatLog> logs;

    public CheatLogAdapter(List<CheatLog> logs) {
        this.logs = logs;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_cheat_log, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        CheatLog log = logs.get(position);

        int color = colorForType(log.alert_type);
        String label = labelForType(log.alert_type);

        holder.tvDot.setBackgroundColor(color);
        holder.tvType.setText(label);
        holder.tvType.setBackgroundColor(color);
        holder.tvDetails.setText(log.details);
        holder.tvTime.setText(log.timestamp != null ? log.timestamp : "");
    }

    @Override
    public int getItemCount() { return logs.size(); }

    private int colorForType(String type) {
        if (type == null) return Color.GRAY;
        switch (type) {
            case "FACE":   return Color.parseColor("#F44336");
            case "GPS":    return Color.parseColor("#FF9800");
            case "MIC":    return Color.parseColor("#9C27B0");
            case "TFLITE": return Color.parseColor("#2196F3");
            default:       return Color.parseColor("#607D8B");
        }
    }

    private String labelForType(String type) {
        if (type == null) return "INCONNU";
        switch (type) {
            case "FACE":   return "👤 VISAGE";
            case "GPS":    return "📍 GPS";
            case "MIC":    return "🎙 MICRO";
            case "TFLITE": return "👁 CAMÉRA";
            default:       return type;
        }
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        View tvDot;
        TextView tvType, tvDetails, tvTime;

        ViewHolder(View view) {
            super(view);
            tvDot     = view.findViewById(R.id.tvDot);
            tvType    = view.findViewById(R.id.tvType);
            tvDetails = view.findViewById(R.id.tvDetails);
            tvTime    = view.findViewById(R.id.tvTime);
        }
    }
}
