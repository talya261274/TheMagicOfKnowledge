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

        holder.tvName.setText("נושא: " + translateSubject(stat.getSubjectName()));

        holder.tvAttempts.setText("טעויות: " + stat.getAttempts());

        long totalSeconds = stat.getTimeInSeconds();
        long minutes = totalSeconds / 60;
        long seconds = totalSeconds % 60;
        String timeFormatted = String.format(Locale.getDefault(), "%02d:%02d", minutes, seconds);

        holder.tvTime.setText("זמן: " + timeFormatted);

        int iconRes;
        switch (stat.getSubjectName()) {
            case "animals":
                iconRes = R.drawable.ic_animals;
                break;
            case "numbers":
                iconRes = R.drawable.ic_numbers;
                break;
            case "colors":
                iconRes = R.drawable.ic_colors;
                break;
            case "letters":
                iconRes = R.drawable.ic_letters;
                break;
            case "shapes":
                iconRes = R.drawable.ic_shapes;
                break;
            case "bodyparts":
                iconRes = R.drawable.ic_bodyparts;
                break;
            default:
                iconRes = R.drawable.ic_subject_placeholder;
                break;
        }
        holder.ivIcon.setImageResource(iconRes);
    }

    @Override
    public int getItemCount() {
        return statsList != null ? statsList.size() : 0;
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvName, tvAttempts, tvTime;
        ImageView ivIcon;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvName = itemView.findViewById(R.id.tvSubjectName);
            tvAttempts = itemView.findViewById(R.id.tvAttempts);
            tvTime = itemView.findViewById(R.id.tvTime);
            ivIcon = itemView.findViewById(R.id.ivSubjectIcon); // חיבור האייקון
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