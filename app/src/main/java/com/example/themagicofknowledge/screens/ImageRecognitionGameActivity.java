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
import com.example.themagicofknowledge.models.GameProgress;
import com.example.themagicofknowledge.models.Question;
import com.example.themagicofknowledge.models.UserChild;
import com.example.themagicofknowledge.services.DatabaseService;
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
    private ImageView ivGameAvatar;
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
        int step = getIntent().getIntExtra("gameStep", 1);
        testProgress.setProgress(step);
        attempts = 0;
        loadAttempts();
    }

    private void loadAttempts() {
        GameProgressManager.getAttempts(
                currentChild.getParentId(),
                currentChild.getId(),
                currentChild.getAgeGroup(),
                subject,
                result -> {
                    attempts = result + 1 ;
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

        testProgress.setMax(3);

    }

    private void loadQuestions() {
        String level = currentChild.getAgeGroup();
        String path = "level_" + level.replace("-", "_");

        DatabaseReference ref = FirebaseDatabase.getInstance()
                .getReference("Games")
                .child("imageRecognition")
                .child(path);

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
        Button[] buttons = {btnAns1, btnAns2, btnAns3, btnAns4};
        Button selectedButton = buttons[selectedIndex];

        if (selectedIndex == q.getCorrectAnswerIndex()) {
            // תשובה נכונה - צובעים בירוק
            selectedButton.setBackgroundTintList(android.content.res.ColorStateList.valueOf(android.graphics.Color.GREEN));
            Toast.makeText(this, "כל הכבוד! 🌟", Toast.LENGTH_SHORT).show();

            currentIndex++;
            new android.os.Handler().postDelayed(() -> {
                resetButtons();
                showQuestion();
            }, 1000);
        } else {
            // תשובה שגויה - צובעים באדום ומרעידים
            selectedButton.setBackgroundTintList(android.content.res.ColorStateList.valueOf(android.graphics.Color.RED));

            // אנימציית רעד קלה (Shake)
            selectedButton.animate().translationX(20).setDuration(50).withEndAction(() ->
                    selectedButton.animate().translationX(-20).setDuration(50).withEndAction(() ->
                            selectedButton.animate().translationX(0).setDuration(50).start()
                    ).start()
            ).start();

            Toast.makeText(this, "לא נורא, ננסה שוב! 💪", Toast.LENGTH_SHORT).show();

            new android.os.Handler().postDelayed(() -> {
                resetButtons();
                currentIndex = 0; // מתחיל מחדש לפי הלוגיקה שלך
                attempts++;
                showQuestion();
            }, 1500);
        }
    }

    // פונקציית עזר להחזרת הצבעים המקוריים
    private void resetButtons() {
        btnAns1.setBackgroundTintList(android.content.res.ColorStateList.valueOf(android.graphics.Color.parseColor("#FF9800")));
        btnAns2.setBackgroundTintList(android.content.res.ColorStateList.valueOf(android.graphics.Color.parseColor("#4CAF50")));
        btnAns3.setBackgroundTintList(android.content.res.ColorStateList.valueOf(android.graphics.Color.parseColor("#2196F3")));
        btnAns4.setBackgroundTintList(android.content.res.ColorStateList.valueOf(android.graphics.Color.parseColor("#E91E63")));
    }

    private void finishGame(boolean success) {
        long currentTimeSeconds = (System.currentTimeMillis() - startTime) / 1000;

        Intent intent = new Intent(this, SentenceCompletionActivity.class);

        intent.putExtra("subject", subject);
        intent.putExtra("age", currentChild.getAge());
        intent.putExtra("totalAttempts", attempts);
        intent.putExtra("totalTime", currentTimeSeconds);
        intent.putExtra("gameStep", 2);
        startActivity(intent);
        finish();
    }
}