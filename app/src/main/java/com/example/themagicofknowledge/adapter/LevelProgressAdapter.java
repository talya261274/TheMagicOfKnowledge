package com.example.themagicofknowledge.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.themagicofknowledge.R;
import com.example.themagicofknowledge.models.LevelProgress;

import java.util.List;

public class LevelProgressAdapter extends RecyclerView.Adapter<LevelProgressAdapter.LevelViewHolder> {

    private List<LevelProgress> levels;

    public LevelProgressAdapter(List<LevelProgress> levels) {
        this.levels = levels;
    }

    @NonNull
    @Override
    public LevelViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_level_progress, parent, false);
        return new LevelViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull LevelViewHolder holder, int position) {
        LevelProgress level = levels.get(position);

        // === הצגת כותרת ===
        holder.tvLevelTitle.setText("רמה " + level.getAgeGroup());

        // אימוג'י לפי רמה
        String emoji = "🎯";
        if (level.getAgeGroup().equals("3-4")) emoji = "🌱";
        else if (level.getAgeGroup().equals("5-6")) emoji = "🌟";
        else if (level.getAgeGroup().equals("7-8")) emoji = "🏆";
        holder.tvLevelEmoji.setText(emoji);

        // === סטטוס ===
        if (level.isLocked()) {
            holder.tvLevelStatus.setText("נעול 🔒");
            holder.tvLevelStatus.setTextColor(0xFF9E9E9E);
            holder.tvArrow.setVisibility(View.GONE);
        } else if (level.isCurrent()) {
            holder.tvLevelStatus.setText("רמה נוכחית ▶️");
            holder.tvLevelStatus.setTextColor(0xFFFF9800);
            holder.tvArrow.setVisibility(View.VISIBLE);
        } else if (level.isCompleted()) {
            holder.tvLevelStatus.setText("הושלם ✅");
            holder.tvLevelStatus.setTextColor(0xFF4CAF50);
            holder.tvArrow.setVisibility(View.VISIBLE);
        } else {
            holder.tvLevelStatus.setText("בתהליך");
            holder.tvLevelStatus.setTextColor(0xFF2196F3);
            holder.tvArrow.setVisibility(View.VISIBLE);
        }

        // === סטטיסטיקות ===
        long totalTimeMinutes = level.getTotalTimeSeconds() / 60;
        holder.tvLevelTotalTime.setText(totalTimeMinutes + " דקות");
        holder.tvLevelTotalAttempts.setText(level.getTotalAttempts() + " ניסיונות");
        holder.tvLevelAvgPercent.setText(level.getAveragePercent() + "%");

        // === מצב פתוח/סגור ===
        if (level.isLocked()) {
            // נעול - מציג רק את ההודעה
            holder.lockedLayout.setVisibility(level.isExpanded() ? View.VISIBLE : View.GONE);
            holder.summaryLayout.setVisibility(View.GONE);
            holder.rvSubjects.setVisibility(View.GONE);
            holder.tvArrow.setText(level.isExpanded() ? "▲" : "▼");
        } else if (level.isExpanded()) {
            // פתוח - מציג סטטיסטיקות ונושאים
            holder.summaryLayout.setVisibility(View.VISIBLE);
            holder.rvSubjects.setVisibility(View.VISIBLE);
            holder.lockedLayout.setVisibility(View.GONE);
            holder.tvArrow.setText("▲");

            // טוען את האדפטר של הנושאים
            holder.rvSubjects.setLayoutManager(
                    new LinearLayoutManager(holder.itemView.getContext()));
            holder.rvSubjects.setAdapter(
                    new SubjectProgressAdapter(level.getSubjects()));
        } else {
            // סגור
            holder.summaryLayout.setVisibility(View.GONE);
            holder.rvSubjects.setVisibility(View.GONE);
            holder.lockedLayout.setVisibility(View.GONE);
            holder.tvArrow.setText("▼");
        }

        // === לחיצה על הכרטיס - פתיחה/סגירה ===
        holder.headerLayout.setOnClickListener(v -> {
            level.setExpanded(!level.isExpanded());
            notifyItemChanged(holder.getAdapterPosition());
        });
    }

    @Override
    public int getItemCount() {
        return levels.size();
    }

    static class LevelViewHolder extends RecyclerView.ViewHolder {
        LinearLayout headerLayout, summaryLayout, lockedLayout;
        TextView tvLevelTitle, tvLevelStatus, tvLevelEmoji, tvArrow;
        TextView tvLevelTotalTime, tvLevelTotalAttempts, tvLevelAvgPercent;
        RecyclerView rvSubjects;

        public LevelViewHolder(@NonNull View itemView) {
            super(itemView);
            headerLayout = itemView.findViewById(R.id.headerLayout);
            summaryLayout = itemView.findViewById(R.id.summaryLayout);
            lockedLayout = itemView.findViewById(R.id.lockedLayout);
            tvLevelTitle = itemView.findViewById(R.id.tvLevelTitle);
            tvLevelStatus = itemView.findViewById(R.id.tvLevelStatus);
            tvLevelEmoji = itemView.findViewById(R.id.tvLevelEmoji);
            tvArrow = itemView.findViewById(R.id.tvArrow);
            tvLevelTotalTime = itemView.findViewById(R.id.tvLevelTotalTime);
            tvLevelTotalAttempts = itemView.findViewById(R.id.tvLevelTotalAttempts);
            tvLevelAvgPercent = itemView.findViewById(R.id.tvLevelAvgPercent);
            rvSubjects = itemView.findViewById(R.id.rvSubjects);
        }
    }
}