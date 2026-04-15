package com.example.themagicofknowledge.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;
import java.util.Locale;

import com.example.themagicofknowledge.models.SubjectStat;
import com.example.themagicofknowledge.R;

public class SubjectProgressAdapter extends RecyclerView.Adapter<SubjectProgressAdapter.ViewHolder> {

    private List<SubjectStat> statsList;

    public SubjectProgressAdapter(List<SubjectStat> statsList) {
        this.statsList = statsList;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        // ודאי ששם הקובץ ב-layout הוא item_subject_progress
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_subject_progress, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        SubjectStat stat = statsList.get(position);

        // השם כבר מגיע מתורגם מה-Activity, אז פשוט מציגים אותו
        holder.tvName.setText("נושא: " + stat.getSubjectName());

        holder.tvAttempts.setText("טעויות: " + stat.getAttempts());

        // פורמט זמן
        long totalSeconds = stat.getTimeInSeconds();
        long minutes = totalSeconds / 60;
        long seconds = totalSeconds % 60;
        String timeFormatted = String.format(Locale.getDefault(), "%02d:%02d", minutes, seconds);
        holder.tvTime.setText("זמן: " + timeFormatted);

        // עדכון הפרוגרס בר
        int progress = stat.getProgressPercent();
        holder.pbProgress.setProgress(progress);
        holder.tvProgressPercent.setText(progress + "%");

        // שינוי צבע הפרוגרס בר לירוק אם הנושא הושלם
        if (stat.isCompleted()) {
            holder.pbProgress.setIndicatorColor(android.graphics.Color.parseColor("#4CAF50")); // ירוק
            holder.tvProgressPercent.setText("הושלם! ✓");
            holder.tvProgressPercent.setTextColor(android.graphics.Color.parseColor("#4CAF50"));
        } else {
            holder.pbProgress.setIndicatorColor(android.graphics.Color.parseColor("#2196F3")); // כחול רגיל
            holder.tvProgressPercent.setTextColor(android.graphics.Color.GRAY);
        }

        // בחירת אייקון - שימי לב: השתמשי בשמות בעברית כי זה מה שנשלח מה-Activity
        int iconRes;
        String name = stat.getSubjectName();
        if (name.equals("חיות")) iconRes = R.drawable.ic_animals;
        else if (name.equals("מספרים")) iconRes = R.drawable.ic_numbers;
        else if (name.equals("צבעים")) iconRes = R.drawable.ic_colors;
        else if (name.equals("אותיות")) iconRes = R.drawable.ic_letters;
        else if (name.equals("צורות")) iconRes = R.drawable.ic_shapes;
        else if (name.equals("חלקי גוף")) iconRes = R.drawable.ic_bodyparts;
        else iconRes = R.drawable.ic_subject_placeholder;

        holder.ivIcon.setImageResource(iconRes);
    }

    @Override
    public int getItemCount() {
        return statsList != null ? statsList.size() : 0;
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvName, tvAttempts, tvTime, tvProgressPercent;
        ImageView ivIcon;
        com.google.android.material.progressindicator.LinearProgressIndicator pbProgress;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvName = itemView.findViewById(R.id.tvSubjectName);
            tvAttempts = itemView.findViewById(R.id.tvAttempts);
            tvTime = itemView.findViewById(R.id.tvTime);
            tvProgressPercent = itemView.findViewById(R.id.tvProgressPercent);
            pbProgress = itemView.findViewById(R.id.pbSubjectProgress);
            ivIcon = itemView.findViewById(R.id.ivSubjectIcon);
        }
    }

    private String translateSubject(String sub) {
        if (sub == null) return "";
        switch (sub) {
            case "animals": return "חיות";
            case "numbers": return "מספרים";
            case "colors": return "צבעים";
            case "letters": return "אותיות";
            case "shapes": return "צורות";
            case "bodyparts": return "חלקי גוף";
            default: return sub;
        }
    }
}