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
        boolean success = getIntent().getBooleanExtra("success", false);
        attempts = getIntent().getIntExtra("attempts", 0);
        subject = getIntent().getStringExtra("subject");

        TextView tvTitle = findViewById(R.id.tvResultTitle);
        TextView tvAttempts = findViewById(R.id.tvAttempts);
        Button btnAction = findViewById(R.id.btnAction);

        if (success) {
            tvTitle.setText("כל הכבוד! סיימת את נושא ה" + translateSubject(subject) + "! 🎉");
            tvAttempts.setText("סך הכל טעויות בדרך: " + attempts);
            btnAction.setText("סיום וקבלת כוכב ⭐");

            btnAction.setOnClickListener(v -> markSubjectAsCompleted());
        } else {
            tvTitle.setText("לא נורא, כמעט הצלחת! 💪");
            tvAttempts.setText("בוא ננסה להשתפר...");
            btnAction.setText("נסה שוב מהתחלה");

            btnAction.setOnClickListener(v -> {
                // חזרה לתחילת המשחקים של אותו נושא
                Intent intent = new Intent(this, SentenceCompletionActivity.class); // או המשחק הראשון ברצף
                intent.putExtra("subject", subject);
                intent.putExtra("age", currentChild.getAge());
                startActivity(intent);
                finish();
            });
        }
    }

    private void markSubjectAsCompleted() {
        if (currentChild == null || subject == null) return;

        // עדכון ב-Firebase שהנושא הושלם
        DatabaseReference ref = FirebaseDatabase.getInstance().getReference("Parents")
                .child(currentChild.getParentId())
                .child("children")
                .child(currentChild.getId())
                .child("completedSubjects")
                .child(subject);

        ref.setValue(true).addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                // חזרה למסך בחירת נושאים
                Intent intent = new Intent(this, SelectSubjectActivity.class);
                intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                startActivity(intent);
                finish();
            } else {
                Toast.makeText(this, "שגיאה בשמירת התקדמות", Toast.LENGTH_SHORT).show();
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