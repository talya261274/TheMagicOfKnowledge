package com.example.themagicofknowledge.screens;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.themagicofknowledge.R;
import com.example.themagicofknowledge.models.UserChild;
import com.example.themagicofknowledge.utils.SharedPreferencesUtil;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

public class GameResultActivity extends AppCompatActivity {

    private UserChild currentChild;
    private String subject;
    private int attempts;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_game_result);

        currentChild = SharedPreferencesUtil.getCurrentChild(this);
        boolean success = getIntent().getBooleanExtra("success", true);
        attempts = getIntent().getIntExtra("totalAttempts", 0);
        long totalTime = getIntent().getLongExtra("totalTime", 0);
        subject = getIntent().getStringExtra("subject");

        TextView tvTitle = findViewById(R.id.tvResultTitle);
        TextView tvAttempts = findViewById(R.id.tvAttempts);
        Button btnAction = findViewById(R.id.btnAction);

        if (success) {
            tvTitle.setText("כל הכבוד! סיימת את נושא ה" + translateSubject(subject) + "! 🎉");
            tvAttempts.setText("סך הכל טעויות בדרך: " + attempts + "\nזמן כולל: " + totalTime + " שניות");
            btnAction.setText("סיום וקבלת כוכב ⭐");

            btnAction.setOnClickListener(v -> markSubjectAsCompleted());
        } else {
            tvTitle.setText("לא נורא, כמעט הצלחת! 💪");
            tvAttempts.setText("בוא ננסה להשתפר...");
            btnAction.setText("נסה שוב מהתחלה");

            btnAction.setOnClickListener(v -> {
                Intent intent;
                String age = currentChild.getAgeGroup();
                if (age.equals("7-8")) {
                    intent = new Intent(this, ImageRecognitionGameActivity.class);
                } else {
                    intent = new Intent(this, AudioRecognitionActivity.class);
                }
                intent.putExtra("subject", subject);
                startActivity(intent);
                finish();
            });
        }
    }

    private void markSubjectAsCompleted() {
        if (currentChild == null || subject == null) return;

        long totalTime = getIntent().getLongExtra("totalTime", 0);

        DatabaseReference ref = FirebaseDatabase.getInstance().getReference("Parents")
                .child(currentChild.getParentId())
                .child("children")
                .child(currentChild.getId());

        ref.child("completedSubjects").child(subject).setValue(true);

        com.example.themagicofknowledge.models.GameProgress finalStats =
                new com.example.themagicofknowledge.models.GameProgress(attempts, true, totalTime);

        ref.child("stats").child(subject).setValue(finalStats).addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                Toast.makeText(this, "התקדמות נשמרה! כל הכבוד! ⭐", Toast.LENGTH_SHORT).show();
                Intent intent = new Intent(this, SelectSubjectActivity.class);
                intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                startActivity(intent);
                finish();
            }
        });
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