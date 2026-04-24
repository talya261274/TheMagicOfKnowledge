package com.example.themagicofknowledge.screens;

import android.content.Intent;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
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
import com.example.themagicofknowledge.services.DatabaseService;
import com.example.themagicofknowledge.utils.SharedPreferencesUtil;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

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
        if (subject == null) subject = "animals";

        initViews();

        currentChild = SharedPreferencesUtil.getCurrentChild(this);
        if (currentChild == null) {
            finish();
            return;
        }

        testProgress.setMax(3);
        testProgress.setProgress(1);

        startTime = System.currentTimeMillis();
        attempts = 0;

        loadQuestions();
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
                    Toast.makeText(ImageRecognitionGameActivity.this, "לא נמצאו שאלות", Toast.LENGTH_SHORT).show();
                    finish();
                    return;
                }
                showQuestion();
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
            }
        });
    }

    private void showQuestion() {
        if (currentIndex >= questions.size()) {
            finishGame();
            return;
        }

        Question q = questions.get(currentIndex);
        tvQuestion.setText(q.getQuestionText());

        String mediaUrl = q.getMediaUrl();
        int resId = getResources().getIdentifier(mediaUrl, "drawable", getPackageName());
        ivQuestionMedia.setImageResource(resId != 0 ? resId : R.drawable.wizard_placeholder);

        List<String> op = q.getOptions();
        Button[] buttons = {btnAns1, btnAns2, btnAns3, btnAns4};

        for (int i = 0; i < buttons.length; i++) {
            buttons[i].setText(op.get(i));
            final int index = i;
            buttons[i].setOnClickListener(v -> checkAnswer(index));
        }
    }

    private void checkAnswer(int selectedIndex) {
        Question q = questions.get(currentIndex);
        Button[] buttons = {btnAns1, btnAns2, btnAns3, btnAns4};
        Button selectedButton = buttons[selectedIndex];

        if (selectedIndex == q.getCorrectAnswerIndex()) {
            selectedButton.setBackgroundTintList(ColorStateList.valueOf(Color.GREEN));
            currentIndex++;
            new Handler().postDelayed(() -> {
                resetButtons();
                showQuestion();
            }, 1000);
        } else {
            selectedButton.setBackgroundTintList(ColorStateList.valueOf(Color.RED));
            attempts++;

            selectedButton.animate().translationX(20).setDuration(50).withEndAction(() ->
                    selectedButton.animate().translationX(-20).setDuration(50).withEndAction(() ->
                            selectedButton.animate().translationX(0).setDuration(50).start()
                    ).start()
            ).start();

            new Handler().postDelayed(this::resetButtons, 1500);
        }
    }

    private void resetButtons() {
        btnAns1.setBackgroundTintList(ColorStateList.valueOf(Color.parseColor("#FF9800")));
        btnAns2.setBackgroundTintList(ColorStateList.valueOf(Color.parseColor("#4CAF50")));
        btnAns3.setBackgroundTintList(ColorStateList.valueOf(Color.parseColor("#2196F3")));
        btnAns4.setBackgroundTintList(ColorStateList.valueOf(Color.parseColor("#E91E63")));
    }

    private void finishGame() {
        long timeSeconds = (System.currentTimeMillis() - startTime) / 1000;
        String parentId = SharedPreferencesUtil.getUser(this).getId();

        DatabaseService.getInstance().updateDetailedProgress(
                parentId,
                currentChild.getId(),
                currentChild.getAgeGroup(),
                subject,
                attempts,
                timeSeconds,
                40,
                0
        );

        Intent intent = new Intent(this, SentenceCompletionActivity.class);
        intent.putExtra("subject", subject);
        startActivity(intent);
        finish();
    }
}