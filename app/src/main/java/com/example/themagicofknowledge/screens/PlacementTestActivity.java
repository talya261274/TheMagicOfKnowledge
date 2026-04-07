package com.example.themagicofknowledge.screens;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.example.themagicofknowledge.R;
import com.example.themagicofknowledge.models.Question;
import com.example.themagicofknowledge.models.UserChild;
import com.example.themagicofknowledge.models.UserParent;
import com.example.themagicofknowledge.utils.SharedPreferencesUtil;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.List;

public class PlacementTestActivity extends AppCompatActivity {

    private List<Question> testQuestions = new ArrayList<>();
    private int currentQuestionIndex = 0;
    private int score = 0;
    private UserChild selectedChild;
    private String currentLevel; // הרמה הנוכחית של הילד

    private TextView tvQuestion;
    private ImageView ivQuestionMedia;
    private Button btnAns1, btnAns2, btnAns3, btnAns4;
    private ProgressBar testProgress;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_placement_test);

        initViews();

        selectedChild = SharedPreferencesUtil.getCurrentChild(this);

        if (selectedChild != null) {
            currentLevel = selectedChild.getAgeGroup(); // שמירת הרמה הנוכחית
            loadQuestionsForCurrentLevel();
        } else {
            Toast.makeText(this, "שגיאה: לא נבחר ילד", Toast.LENGTH_SHORT).show();
            finish();
        }
    }

    private void initViews() {
        tvQuestion = findViewById(R.id.tvQuestion);
        ivQuestionMedia = findViewById(R.id.ivQuestionMedia);
        btnAns1 = findViewById(R.id.btnAns1);
        btnAns2 = findViewById(R.id.btnAns2);
        btnAns3 = findViewById(R.id.btnAns3);
        btnAns4 = findViewById(R.id.btnAns4);
        testProgress = findViewById(R.id.testProgress);
    }

    // 🎯 טעינת שאלות לפי הרמה הנוכחית של הילד
    private void loadQuestionsForCurrentLevel() {
        String levelPath = "level " + currentLevel; // "level 3-4" למשל

        DatabaseReference ref = FirebaseDatabase.getInstance()
                .getReference("PlacementTest")
                .child(levelPath);

        ref.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                testQuestions.clear();

                for (DataSnapshot ds : snapshot.getChildren()) {
                    Question q = ds.getValue(Question.class);
                    if (q != null) {
                        testQuestions.add(q);
                    }
                }

                if (!testQuestions.isEmpty()) {
                    testProgress.setMax(testQuestions.size());
                    showNextQuestion();
                } else {
                    Toast.makeText(PlacementTestActivity.this,
                            "לא נמצאו שאלות לרמה: " + currentLevel,
                            Toast.LENGTH_SHORT).show();
                    finish();
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(PlacementTestActivity.this,
                        "שגיאה בטעינת שאלות: " + error.getMessage(),
                        Toast.LENGTH_SHORT).show();
                finish();
            }
        });
    }

    private void showNextQuestion() {
        if (currentQuestionIndex >= testQuestions.size()) {
            finishTest();
            return;
        }

        Question q = testQuestions.get(currentQuestionIndex);

        // עדכון פרוגרס
        testProgress.setProgress(currentQuestionIndex + 1);

        // הצגת השאלה
        tvQuestion.setText(q.getQuestionText());

        // הצגת תמונה (אם יש)
        if (q.getMediaUrl() != null && !q.getMediaUrl().isEmpty()) {
            ivQuestionMedia.setVisibility(View.VISIBLE);
            // כאן אפשר להוסיף Glide אם יש תמונות אמיתיות:
            // Glide.with(this).load(q.getMediaUrl()).into(ivQuestionMedia);
        } else {
            ivQuestionMedia.setVisibility(View.GONE);
        }

        // הצגת תשובות
        List<String> options = q.getOptions();
        btnAns1.setText(options.get(0));
        btnAns2.setText(options.get(1));
        btnAns3.setText(options.get(2));
        btnAns4.setText(options.get(3));

        // הוספת לחיצות על כפתורים
        View.OnClickListener listener = view -> {
            int selectedIdx = -1;
            int id = view.getId();

            if (id == R.id.btnAns1) selectedIdx = 0;
            else if (id == R.id.btnAns2) selectedIdx = 1;
            else if (id == R.id.btnAns3) selectedIdx = 2;
            else if (id == R.id.btnAns4) selectedIdx = 3;

            checkAnswer(selectedIdx);
        };

        btnAns1.setOnClickListener(listener);
        btnAns2.setOnClickListener(listener);
        btnAns3.setOnClickListener(listener);
        btnAns4.setOnClickListener(listener);
    }

    private void checkAnswer(int selectedIdx) {
        // בדיקה אם התשובה נכונה
        if (selectedIdx == testQuestions.get(currentQuestionIndex).getCorrectAnswerIndex()) {
            score++;
        }

        currentQuestionIndex++;
        showNextQuestion();
    }

    private void finishTest() {
        // חישוב אחוזים
        double percent = ((double) score / testQuestions.size()) * 100;

        // החלטה על רמה חדשה
        String newLevel = determineNewLevel(currentLevel, percent);

        // עדכון הילד
        selectedChild.setAgeGroup(newLevel);
        selectedChild.setGradeAvg(percent);

        // שמירה ב-Firebase
        updateChildLevelInFirebase(newLevel, percent);
    }

    // 🎯 החלטה חכמה על הרמה החדשה
    private String determineNewLevel(String currentLevel, double percent) {
        String newLevel = currentLevel;
        String message;

        if (percent >= 90) {
            // ✅ ציון מעולה - עליה ברמה (אם אפשר)
            if (currentLevel.equals("3-4")) {
                newLevel = "5-6";
                message = "🎉 מעולה! אתה עולה לרמה 5-6!";
            } else if (currentLevel.equals("5-6")) {
                newLevel = "7-8";
                message = "🎉 מדהים! אתה עולה לרמה 7-8!";
            } else {
                message = "🌟 מושלם! אתה כבר ברמה הגבוהה ביותר!";
            }
        } else if (percent >= 60) {
            // ⚖️ ציון טוב - הישאר באותה רמה
            message = "👍 יפה! אתה נשאר ברמה " + currentLevel;
        } else {
            // ❌ ציון נמוך - ירידה ברמה (אם אפשר)
            if (currentLevel.equals("7-8")) {
                newLevel = "5-6";
                message = "💪 בוא ננסה ברמה קלה יותר: 5-6";
            } else if (currentLevel.equals("5-6")) {
                newLevel = "3-4";
                message = "💪 בוא ננסה ברמה קלה יותר: 3-4";
            } else {
                message = "💪 בוא נתרגל עוד קצת ברמה 3-4";
            }
        }

        // הצגת התוצאות
        showResultDialog(percent, newLevel, message);

        return newLevel;
    }

    private void showResultDialog(double percent, String newLevel, String message) {
        new AlertDialog.Builder(this)
                .setTitle("מבדק הצבה הסתיים!")
                .setMessage(
                        "ציון: " + (int)percent + "%\n" +
                                "תשובות נכונות: " + score + "/" + testQuestions.size() + "\n\n" +
                                message
                )
                .setPositiveButton("המשך", (dialog, which) -> {
                    // כלום - רק סוגר את הדיאלוג
                })
                .setCancelable(false)
                .show();
    }

    private void updateChildLevelInFirebase(String level, double grade) {
        UserParent parent = SharedPreferencesUtil.getUser(this);
        if (parent == null) {
            Toast.makeText(this, "שגיאה: לא נמצא הורה", Toast.LENGTH_SHORT).show();
            return;
        }

        String parentId = parent.getId();
        DatabaseReference childRef = FirebaseDatabase.getInstance()
                .getReference("users")
                .child(parentId)
                .child("childrenList")
                .child(selectedChild.getId());

        // עדכון הרמה והציון
        childRef.child("ageGroup").setValue(level);
        childRef.child("gradeAvg").setValue(grade)
                .addOnSuccessListener(aVoid -> {
                    // עדכון בזיכרון המקומי
                    SharedPreferencesUtil.saveCurrentChild(this, selectedChild);

                    Toast.makeText(this, "הרמה עודכנה בהצלחה!", Toast.LENGTH_SHORT).show();

                    // חזרה למסך הראשי
                    Intent intent = new Intent(this, Total.class);
                    intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                    startActivity(intent);
                    finish();
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "שגיאה בשמירה: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }
}