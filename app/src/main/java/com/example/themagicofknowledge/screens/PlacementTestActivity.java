package com.example.themagicofknowledge.screens;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.bumptech.glide.Glide; // ודאי שהוספת Glide ב-build.gradle
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

    private TextView tvQuestion;
    private ImageView ivQuestionMedia;
    private Button btnAns1, btnAns2, btnAns3, btnAns4;
    private ProgressBar testProgress;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_placement_test);

        // קריאה לפונקציה שמחברת את ה-UI לקוד
        initViews();

        selectedChild = SharedPreferencesUtil.getCurrentChild(this);

        if (selectedChild != null) {
            loadGeneralPlacementQuestions();
        } else {
            finish();
        }
    }

    // הפונקציה ששאלת עליה - כאן היא גרה
    private void initViews() {
        tvQuestion = findViewById(R.id.tvQuestion);
        ivQuestionMedia = findViewById(R.id.ivQuestionMedia);
        btnAns1 = findViewById(R.id.btnAns1);
        btnAns2 = findViewById(R.id.btnAns2);
        btnAns3 = findViewById(R.id.btnAns3);
        btnAns4 = findViewById(R.id.btnAns4);
        testProgress = findViewById(R.id.testProgress);
    }

    private void loadGeneralPlacementQuestions() {
        DatabaseReference ref = FirebaseDatabase.getInstance().getReference("PlacementTest");
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
                    Toast.makeText(PlacementTestActivity.this, "בנק השאלות ריק!", Toast.LENGTH_SHORT).show();
                }
            }
            @Override
            public void onCancelled(DatabaseError error) {}
        });
    }

    private void showNextQuestion() {
        Question q = testQuestions.get(currentQuestionIndex);

        tvQuestion.setText(q.questionText);
        testProgress.setProgress(currentQuestionIndex + 1);

        // טעינת תמונה במידה ויש URL בשאלה
        if (q.mediaUrl != null && !q.mediaUrl.isEmpty()) {
            Glide.with(this).load(q.mediaUrl).into(ivQuestionMedia);
        }

        btnAns1.setText(q.options.get(0));
        btnAns2.setText(q.options.get(1));
        btnAns3.setText(q.options.get(2));
        btnAns4.setText(q.options.get(3));

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
        double percent = ((double) score / testQuestions.size()) * 100;
        String determinedLevel;

        if (percent < 40) determinedLevel = "3-4";
        else if (percent < 80) determinedLevel = "5-6";
        else determinedLevel = "7-8";

        selectedChild.setAgeGroup(determinedLevel);
        selectedChild.setGradeAvg(percent);

        updateChildLevelInFirebase(determinedLevel, percent);
    }

    private void updateChildLevelInFirebase(String level, double grade) {
        String parentId = SharedPreferencesUtil.getUser(this).getId();
        DatabaseReference childRef = FirebaseDatabase.getInstance().getReference("Users")
                .child(parentId)
                .child("childrenList")
                .child(selectedChild.getId());

        childRef.child("ageGroup").setValue(level);
        childRef.child("gradeAvg").setValue(grade)
                .addOnSuccessListener(aVoid -> {
                    SharedPreferencesUtil.saveCurrentChild(this, selectedChild);
                    Toast.makeText(this, "הקוסם קבע שאתה ברמה: " + level, Toast.LENGTH_LONG).show();
                    startActivity(new Intent(this, Total.class));
                    finish();
                });
    }
}