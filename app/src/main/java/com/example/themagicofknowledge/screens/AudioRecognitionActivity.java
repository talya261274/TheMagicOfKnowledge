package com.example.themagicofknowledge.screens;

import android.content.Intent;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.speech.tts.TextToSpeech;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.example.themagicofknowledge.R;
import com.example.themagicofknowledge.models.Question;
import com.example.themagicofknowledge.models.UserChild;
import com.example.themagicofknowledge.utils.SharedPreferencesUtil;
import com.google.android.material.button.MaterialButton;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class AudioRecognitionActivity extends AppCompatActivity {
    private TextToSpeech tts;
    private List<Question> questions = new ArrayList<>();
    private int currentIndex = 0;
    private int attempts = 0;
    private long startTime;
    private String subject;

    private TextView tvQuestion;
    private MaterialButton btnPlayAudio;
    private MaterialButton btnAns1, btnAns2, btnAns3, btnAns4;
    private ProgressBar testProgress;

    private UserChild currentChild;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_audio_recognition);

        subject = getIntent().getStringExtra("subject");
        if (subject == null) subject = "animals";

        currentChild = SharedPreferencesUtil.getCurrentChild(this);
        if (currentChild == null) {
            finish();
            return;
        }

        initViews();
        startTime = System.currentTimeMillis();

        tts = new TextToSpeech(this, status -> {
            if (status == TextToSpeech.SUCCESS) {
                tts.setLanguage(new Locale("he"));
            }
        });

        testProgress.setMax(3);
        testProgress.setProgress(1);

        loadQuestions();
    }

    private void initViews() {
        tvQuestion = findViewById(R.id.tvQuestion);
        btnPlayAudio = findViewById(R.id.btnPlayAudio);
        btnAns1 = findViewById(R.id.btnAns1);
        btnAns2 = findViewById(R.id.btnAns2);
        btnAns3 = findViewById(R.id.btnAns3);
        btnAns4 = findViewById(R.id.btnAns4);
        testProgress = findViewById(R.id.testProgress);

        btnPlayAudio.setOnClickListener(v -> {
            if (!questions.isEmpty()) {
                playQuestionAudio(questions.get(currentIndex).getQuestionText());
            }
        });
    }

    private void loadQuestions() {
        String level = currentChild.getAgeGroup(); // "3-4" או "5-6"
        String path = "level_" + level.replace("-", "_");

        DatabaseReference ref = FirebaseDatabase.getInstance()
                .getReference("Games")
                .child("audioRecognition")
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
                    Toast.makeText(AudioRecognitionActivity.this, "לא נמצאו שאלות לנושא זה", Toast.LENGTH_SHORT).show();
                    finish();
                    return;
                }
                showQuestion();
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {}
        });
    }

    private void showQuestion() {
        if (currentIndex >= questions.size()) {
            finishGame();
            return;
        }

        Question q = questions.get(currentIndex);
        playQuestionAudio(q.getQuestionText());

        String ageGroup = currentChild.getAgeGroup();
        MaterialButton[] buttons = {btnAns1, btnAns2, btnAns3, btnAns4};
        List<String> options = q.getOptions();

        tvQuestion.setText("הקשיבו לשאלה:");

        for (int i = 0; i < buttons.length; i++) {
            final int index = i;
            String item = options.get(i);

            buttons[index].setIconTint(null);

            int resId = getResources().getIdentifier(item, "drawable", getPackageName());
            buttons[index].setIconResource(resId != 0 ? resId : R.drawable.wizard_placeholder);

            buttons[index].setText("");
            buttons[index].setIconSize(220);
            buttons[index].setIconGravity(MaterialButton.ICON_GRAVITY_TEXT_START);

            buttons[index].setOnClickListener(v -> checkAnswer(index));
        }
    }

    private void checkAnswer(int selectedIndex) {
        Question q = questions.get(currentIndex);
        MaterialButton[] buttons = {btnAns1, btnAns2, btnAns3, btnAns4};
        MaterialButton selectedButton = buttons[selectedIndex];

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
            new Handler().postDelayed(this::resetButtons, 1000);
        }
    }

    private void resetButtons() {
        ColorStateList white = ColorStateList.valueOf(Color.WHITE);
        for (MaterialButton btn : new MaterialButton[]{btnAns1, btnAns2, btnAns3, btnAns4}) {
            btn.setBackgroundTintList(white);
        }
    }

    private void finishGame() {
        long timeSeconds = (System.currentTimeMillis() - startTime) / 1000;

        Intent intent = new Intent(this, MatchingGameActivity.class);

        intent.putExtra("subject", subject);
        intent.putExtra("totalAttempts", attempts);
        intent.putExtra("totalTime", timeSeconds);
        intent.putExtra("gameStep", 2);

        startActivity(intent);
        finish();
    }

    private void playQuestionAudio(String text) {
        if (tts != null) {
            tts.speak(text, TextToSpeech.QUEUE_FLUSH, null, null);
        }
    }

    @Override
    protected void onDestroy() {
        if (tts != null) {
            tts.stop();
            tts.shutdown();
        }
        super.onDestroy();
    }
}