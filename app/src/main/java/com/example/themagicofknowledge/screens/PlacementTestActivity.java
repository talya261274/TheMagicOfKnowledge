package com.example.themagicofknowledge.screens;

import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.example.themagicofknowledge.R;
import com.example.themagicofknowledge.models.Question;
import com.example.themagicofknowledge.models.UserChild;
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

    // רכיבי ה-UI
    private TextView tvQuestion;
    private ImageView ivQuestionMedia;
    private Button btnAns1, btnAns2, btnAns3, btnAns4;
    private ProgressBar testProgress;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_placement_test);

        // הגדרת Padding למערכת (EdgeToEdge)
        View mainView = findViewById(R.id.main);
        if (mainView != null) {
            ViewCompat.setOnApplyWindowInsetsListener(mainView, (v, insets) -> {
                Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
                v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
                return insets;
            });
        }

        // אתחול רכיבי ה-UI
        initViews();

        // 1. שליפת הילד שנבחר
        selectedChild = SharedPreferencesUtil.getCurrentChild(this);

        if (selectedChild != null) {
            // 2. טעינת שאלות מה-Firebase לפי קבוצת הגיל שלו
            loadTestQuestions(selectedChild.getAgeGroup());
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

    private void loadTestQuestions(String ageGroup) {
        // התחברות ל-Firebase לנתיב Tests/Age_X-X
        DatabaseReference ref = FirebaseDatabase.getInstance().getReference("Tests")
                .child("Age_" + ageGroup);

        ref.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot snapshot) {
                testQuestions.clear();
                for (DataSnapshot ds : snapshot.getChildren()) {
                    Question q = ds.getValue(Question.class);
                    if (q != null) testQuestions.add(q);
                }

                if (!testQuestions.isEmpty()) {
                    testProgress.setMax(testQuestions.size());
                    showNextQuestion();
                } else {
                    Toast.makeText(PlacementTestActivity.this, "לא נמצאו שאלות לרמה זו", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onCancelled(DatabaseError error) {
                Toast.makeText(PlacementTestActivity.this, "שגיאה בטעינה: " + error.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void showNextQuestion() {
        Question q = testQuestions.get(currentQuestionIndex);

        // עדכון UI
        tvQuestion.setText(q.questionText);
        testProgress.setProgress(currentQuestionIndex + 1);

        // כאן בהמשך נטען תמונה מה-mediaUrl בעזרת Glide

        btnAns1.setText(q.options.get(0));
        btnAns2.setText(q.options.get(1));
        btnAns3.setText(q.options.get(2));
        btnAns4.setText(q.options.get(3));

        // הגדרת לחיצות
        View.OnClickListener listener = view -> {
            int selectedIdx = -1;
            if (view.getId() == R.id.btnAns1) selectedIdx = 0;
            else if (view.getId() == R.id.btnAns2) selectedIdx = 1;
            else if (view.getId() == R.id.btnAns3) selectedIdx = 2;
            else if (view.getId() == R.id.btnAns4) selectedIdx = 3;

            checkAnswer(selectedIdx);
        };

        btnAns1.setOnClickListener(listener);
        btnAns2.setOnClickListener(listener);
        btnAns3.setOnClickListener(listener);
        btnAns4.setOnClickListener(listener);
    }

    private void checkAnswer(int selectedIdx) {
        if (selectedIdx == testQuestions.get(currentQuestionIndex).correctAnswerIndex) {
            score++;
        }

        currentQuestionIndex++;
        if (currentQuestionIndex < testQuestions.size()) {
            showNextQuestion();
        } else {
            finishTest();
        }
    }

    private void finishTest() {
        double finalGrade = ((double) score / testQuestions.size()) * 100;

        // עדכון המודל של הילד
        selectedChild.setGradeAvg(finalGrade);

        // כאן נוסיף בהמשך שמירה ל-Firebase חזרה להורה

        Toast.makeText(this, "סיימת! ציון: " + (int)finalGrade, Toast.LENGTH_LONG).show();
        finish();
    }
}