package com.example.themagicofknowledge.screens;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.example.themagicofknowledge.R;
import com.example.themagicofknowledge.models.Question;
import com.example.themagicofknowledge.models.UserChild;
import com.example.themagicofknowledge.utils.GameProgressManager;
import com.example.themagicofknowledge.utils.SharedPreferencesUtil;
import com.google.firebase.database.*;

import java.util.ArrayList;
import java.util.List;

public class ImageRecognitionGameActivity extends AppCompatActivity {

    private List<Question> questions = new ArrayList<>();
    private int currentIndex = 0;
    private int attempts = 0;
    private long startTime;
    private String subject;

    private TextView tvQuestion;
    private ImageView ivQuestionMedia;
    private Button btnAns1, btnAns2, btnAns3, btnAns4;
    private ProgressBar testProgress;

    private UserChild currentChild;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_image_recognition_game);

        subject = getIntent().getStringExtra("subject");
        if (subject == null) subject = "general";

        initViews();

        currentChild = SharedPreferencesUtil.getCurrentChild(this);
        if (currentChild == null) {
            Toast.makeText(this, "לא נבחר ילד", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        startTime = System.currentTimeMillis();
        loadAttempts();
    }

    private void loadAttempts() {
        GameProgressManager.getAttempts(
                currentChild.getParentId(),
                currentChild.getId(),
                currentChild.getAgeGroup(),
                subject,
                result -> {
                    attempts = result;
                    loadQuestions();
                }
        );
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

    private void loadQuestions() {
        String level = currentChild.getAgeGroup();
        String path = "level_" + level.replace("-", "_");

        DatabaseReference ref = FirebaseDatabase.getInstance()
                .getReference("Games")
                .child("imageRecognition")
                .child(path)
                .child(subject);

        ref.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                questions.clear();
                for (DataSnapshot ds : snapshot.getChildren()) {
                    Question q = ds.getValue(Question.class);
                    if (q != null) questions.add(q);
                }

                if (questions.isEmpty()) {
                    Toast.makeText(ImageRecognitionGameActivity.this,
                            "אין שאלות", Toast.LENGTH_SHORT).show();
                    finish();
                    return;
                }

                testProgress.setMax(questions.size());
                showQuestion();
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(ImageRecognitionGameActivity.this,
                        error.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void showQuestion() {
        if (currentIndex >= questions.size()) {
            finishGame(true);
            return;
        }

        Question q = questions.get(currentIndex);
        testProgress.setProgress(currentIndex + 1);
        tvQuestion.setText(q.getQuestionText());

        String mediaUrl = q.getMediaUrl();
        if (mediaUrl != null && !mediaUrl.isEmpty()) {
            int resId = getResources().getIdentifier(mediaUrl, "drawable", getPackageName());
            ivQuestionMedia.setImageResource(resId != 0 ? resId : R.drawable.wizard_placeholder);
        }

        List<String> op = q.getOptions();
        btnAns1.setText(op.get(0));
        btnAns2.setText(op.get(1));
        btnAns3.setText(op.get(2));
        btnAns4.setText(op.get(3));

        btnAns1.setOnClickListener(v -> checkAnswer(0));
        btnAns2.setOnClickListener(v -> checkAnswer(1));
        btnAns3.setOnClickListener(v -> checkAnswer(2));
        btnAns4.setOnClickListener(v -> checkAnswer(3));
    }

    private void checkAnswer(int selectedIndex) {
        Question q = questions.get(currentIndex);

        btnAns1.setEnabled(false);
        btnAns2.setEnabled(false);
        btnAns3.setEnabled(false);
        btnAns4.setEnabled(false);

        if (selectedIndex == q.getCorrectAnswerIndex()) {
            Toast.makeText(this, "נכון! ✅", Toast.LENGTH_SHORT).show();
            currentIndex++;
            new android.os.Handler().postDelayed(() -> {
                btnAns1.setEnabled(true);
                btnAns2.setEnabled(true);
                btnAns3.setEnabled(true);
                btnAns4.setEnabled(true);
                showQuestion();
            }, 1000);
        } else {
            Toast.makeText(this, "לא נכון ❌ מתחילים מחדש!", Toast.LENGTH_SHORT).show();
            new android.os.Handler().postDelayed(() -> {
                btnAns1.setEnabled(true);
                btnAns2.setEnabled(true);
                btnAns3.setEnabled(true);
                btnAns4.setEnabled(true);
                // מתחיל מחדש
                currentIndex = 0;
                attempts++;
                showQuestion();
            }, 1500);
        }
    }

    private void finishGame(boolean success) {
        long timeSeconds = (System.currentTimeMillis() - startTime) / 1000;
        attempts++;

        GameProgressManager.saveProgress(
                currentChild.getParentId(),
                currentChild.getId(),
                currentChild.getAgeGroup(),
                subject,
                success,
                attempts,
                timeSeconds
        );

        // מעבר למסך סיום
        Intent intent = new Intent(this, GameResultActivity.class);
        intent.putExtra("success", success);
        intent.putExtra("attempts", attempts);
        intent.putExtra("subject", subject);
        startActivity(intent);
        finish();
    }
}