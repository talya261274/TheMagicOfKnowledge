package com.example.themagicofknowledge.screens;

import android.content.Intent;
import android.os.Bundle;
import android.speech.tts.TextToSpeech;

import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.example.themagicofknowledge.R;
import com.example.themagicofknowledge.models.GameProgress;
import com.example.themagicofknowledge.models.Question;
import com.example.themagicofknowledge.models.UserChild;
import com.example.themagicofknowledge.services.DatabaseService;
import com.example.themagicofknowledge.utils.GameProgressManager;
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
    private MaterialButton btnPlayAudio; // כפתור הרמקול
    private MaterialButton btnAns1, btnAns2, btnAns3, btnAns4;
    private ProgressBar testProgress;

    private UserChild currentChild;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_audio_recognition);

        subject = getIntent().getStringExtra("subject");
        if (subject == null) subject = "general";

        initViews();
        currentChild = SharedPreferencesUtil.getCurrentChild(this);

        if (currentChild == null) {
            finish();
            return;
        }

        startTime = System.currentTimeMillis();

        // אתחול TTS
        tts = new TextToSpeech(this, status -> {
            if (status == TextToSpeech.SUCCESS) {
                tts.setLanguage(new Locale("he"));
            }
        });

        int step = getIntent().getIntExtra("gameStep", 1);
        testProgress.setProgress(step);
        attempts = 0;
        loadAttempts();
    }

    private void initViews() {
        tvQuestion = findViewById(R.id.tvQuestion);
        btnPlayAudio = findViewById(R.id.btnPlayAudio);
        btnAns1 = findViewById(R.id.btnAns1);
        btnAns2 = findViewById(R.id.btnAns2);
        btnAns3 = findViewById(R.id.btnAns3);
        btnAns4 = findViewById(R.id.btnAns4);
        testProgress = findViewById(R.id.testProgress);

        testProgress.setMax(3);

        // כפתור הרמקול משמיע את השאלה בלחיצה
        btnPlayAudio.setOnClickListener(v -> {
            if (!questions.isEmpty()) {
                playQuestionAudio(questions.get(currentIndex).getQuestionText());
            }
        });
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

    private void loadQuestions() {
        String level = currentChild.getAgeGroup();
        String path = "level_" + level.replace("-", "_");

        DatabaseReference ref = FirebaseDatabase.getInstance()
                .getReference("Games")
                .child("audioRecognition")
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
                    finish();
                    return;
                }

                testProgress.setMax(questions.size());
                showQuestion();
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {}
        });
    }

    private void showQuestion() {
        if (currentIndex >= questions.size()) {
            finishGame(true);
            return;
        }

        Question q = questions.get(currentIndex);
        testProgress.setProgress(currentIndex + 1);
        playQuestionAudio(q.getQuestionText());

        String ageGroup = currentChild.getAgeGroup();
        MaterialButton[] buttons = {btnAns1, btnAns2, btnAns3, btnAns4};
        List<String> options = q.getOptions();

        // עדכון כותרת השאלה - עתיד להשתנות
        if (ageGroup.equals("3-4") || ageGroup.equals("5-6")) {
            tvQuestion.setText("הקשיבו לשאלה:");
        } else {
            tvQuestion.setText("הקשיבו לשאלה:");
        }

        for (int i = 0; i < buttons.length; i++) {
            String item = options.get(i);
            final int index = i;

            // ביטול הצביעה האוטומטית של האייקון - מציג את הצבעים המקוריים של ה-Drawable
            buttons[index].setIconTint(null);
            buttons[index].setIconPadding(0); // מבטל רווח מיותר בין האייקון לקצוות

            if (ageGroup.equals("3-4")) {
                // --- גילאי 3-4: תמונות בלבד ---
                int resId = getResources().getIdentifier(item, "drawable", getPackageName());
                buttons[index].setIconResource(resId != 0 ? resId : R.drawable.wizard_placeholder);
                buttons[index].setText("");
                buttons[index].setIconSize(220); // גודל שמתאים לכפתור של 130dp
                buttons[index].setIconGravity(MaterialButton.ICON_GRAVITY_TEXT_START);
                buttons[index].setBackgroundTintList(android.content.res.ColorStateList.valueOf(android.graphics.Color.WHITE));
            }
            else if (ageGroup.equals("5-6")) {
                // --- גילאי 5-6: תמונה גדולה וטקסט קטן למטה ---
                int resId = getResources().getIdentifier(item, "drawable", getPackageName());
                buttons[index].setIconResource(resId != 0 ? resId : R.drawable.wizard_placeholder);

                // הגדרת המילה בעברית
                if (q.getOptionLabels() != null && q.getOptionLabels().size() > i) {
                    buttons[index].setText(q.getOptionLabels().get(i));
                }

                // --- הסוד להגדלה ומרכוז ---
                buttons[index].setIconSize(200);
                buttons[index].setTextSize(14);
                buttons[index].setIconPadding(8);

                buttons[index].setIconGravity(MaterialButton.ICON_GRAVITY_TOP); // תמונה מעל טקסט
                buttons[index].setBackgroundTintList(android.content.res.ColorStateList.valueOf(android.graphics.Color.WHITE));
                buttons[index].setTextColor(android.graphics.Color.DKGRAY); // צבע אפור כהה לטקסט שיהיה פחות דומיננטי
            }
            else {
                // --- גילאי 7-8: מילים בלבד ---
                buttons[index].setIconResource(0);
                buttons[index].setText(item);
                buttons[index].setTextSize(24);

                int[] colors = {android.graphics.Color.parseColor("#9C27B0"),
                        android.graphics.Color.parseColor("#4CAF50"),
                        android.graphics.Color.parseColor("#2196F3"),
                        android.graphics.Color.parseColor("#E91E63")};
                buttons[index].setBackgroundTintList(android.content.res.ColorStateList.valueOf(colors[i]));
                buttons[index].setTextColor(android.graphics.Color.WHITE);
            }

            buttons[index].setOnClickListener(v -> checkAnswer(index));
        }
    }

    private void checkAnswer(int selectedIndex) {
        Question q = questions.get(currentIndex);
        MaterialButton[] buttons = {btnAns1, btnAns2, btnAns3, btnAns4};
        MaterialButton selectedButton = buttons[selectedIndex];

        if (selectedIndex == q.getCorrectAnswerIndex()) {
            selectedButton.setBackgroundTintList(android.content.res.ColorStateList.valueOf(android.graphics.Color.GREEN));
            currentIndex++;
            new android.os.Handler().postDelayed(() -> {
                resetButtons();
                showQuestion();
            }, 1000);
        } else {
            selectedButton.setBackgroundTintList(android.content.res.ColorStateList.valueOf(android.graphics.Color.RED));
            attempts++;
            new android.os.Handler().postDelayed(this::resetButtons, 1000);
        }
    }

    private void resetButtons() {
        btnAns1.setBackgroundTintList(android.content.res.ColorStateList.valueOf(android.graphics.Color.WHITE));
        btnAns2.setBackgroundTintList(android.content.res.ColorStateList.valueOf(android.graphics.Color.WHITE));
        btnAns3.setBackgroundTintList(android.content.res.ColorStateList.valueOf(android.graphics.Color.WHITE));
        btnAns4.setBackgroundTintList(android.content.res.ColorStateList.valueOf(android.graphics.Color.WHITE));
    }

    private void playQuestionAudio(String text) {
        if (tts != null) {
            tts.speak(text, TextToSpeech.QUEUE_FLUSH, null, null);
        }
    }

    private void finishGame(boolean success) {
        long currentTimeSeconds = (System.currentTimeMillis() - startTime) / 1000;

        Intent intent = new Intent(this, MatchingGameActivity.class);

        intent.putExtra("subject", subject);
        intent.putExtra("age", currentChild.getAge());
        intent.putExtra("totalAttempts", attempts);
        intent.putExtra("totalTime", currentTimeSeconds);
        intent.putExtra("gameStep", 2);
        startActivity(intent);
        finish();
    }

    private void startPulseAnimation() {
        // אנימציית פעימה לכפתור הרמקול
        android.view.animation.Animation pulse = new android.view.animation.ScaleAnimation(
                1.0f, 1.1f, 1.0f, 1.1f,
                android.view.animation.Animation.RELATIVE_TO_SELF, 0.5f,
                android.view.animation.Animation.RELATIVE_TO_SELF, 0.5f);

        pulse.setDuration(800);
        pulse.setRepeatMode(android.view.animation.Animation.REVERSE);
        pulse.setRepeatCount(android.view.animation.Animation.INFINITE);
        btnPlayAudio.startAnimation(pulse);
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