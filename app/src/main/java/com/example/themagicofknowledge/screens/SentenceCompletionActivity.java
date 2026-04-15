package com.example.themagicofknowledge.screens;

import android.content.Intent;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.widget.Button;
import android.widget.EditText;
import android.widget.GridView;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.example.themagicofknowledge.R;
import com.example.themagicofknowledge.adapter.KeyboardAdapter;
import com.example.themagicofknowledge.models.SentenceQuestion;
import com.example.themagicofknowledge.models.UserChild;
import com.example.themagicofknowledge.utils.SharedPreferencesUtil;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.List;

public class SentenceCompletionActivity extends AppCompatActivity {

    private TextView tvSentence;
    private EditText etAnswer;
    private ImageView ivHint;
    private Button btnCheck;
    private GridView keyboardGrid;
    private ProgressBar testProgress;

    private int attemptsFromBefore;
    private long timeFromBefore;

    private List<SentenceQuestion> questions = new ArrayList<>();
    private int currentIndex = 0;
    private int attempts = 0;
    private long startTime;
    private String subject;
    private UserChild currentChild;

    private final String[] hebrewLetters = {
            // שורה 1: כפתור מחיקה + א עד ו
            "ו", "ה", "ד", "ג", "ב", "א", "DEL",

            // שורה 2: ז עד מ
            "מ", "ל", "כ", "י", "ט", "ח", "ז",

            // שורה 3: נ עד ר
            "ר", "ק", "צ", "פ", "ע", "ס", "נ",

            // שורה 4: האותיות הסופיות + ש ו- ת
            "ץ", "ף", "ן", "ם", "ך", "ת", "ש",
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_sentence_completion);

        attemptsFromBefore = getIntent().getIntExtra("totalAttempts", 0);
        timeFromBefore = getIntent().getLongExtra("totalTime", 0);
        subject = getIntent().getStringExtra("subject");
        if (subject == null) subject = "animals";

        currentChild = SharedPreferencesUtil.getCurrentChild(this);
        if (currentChild == null) { finish(); return; }

        initViews();
        setupCustomKeyboard();

        testProgress.setMax(3);
        int step = getIntent().getIntExtra("gameStep", 2);
        testProgress.setProgress(step);

        startTime = System.currentTimeMillis();
        loadQuestions();
    }

    private void initViews() {
        tvSentence = findViewById(R.id.tvMixedSentenceText);
        etAnswer = findViewById(R.id.etMixedAnswer);
        ivHint = findViewById(R.id.ivMixedSentenceHint);
        btnCheck = findViewById(R.id.btnCheck);
        keyboardGrid = findViewById(R.id.mixedKeyboard);
        testProgress = findViewById(R.id.globalProgress);
    }

    private void loadQuestions() {
        DatabaseReference ref = FirebaseDatabase.getInstance().getReference("Games")
                .child("sentenceCompletion").child("level_7_8").child(subject);

        ref.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                questions.clear();
                for (DataSnapshot ds : snapshot.getChildren()) {
                    SentenceQuestion q = ds.getValue(SentenceQuestion.class);
                    if (q != null) questions.add(q);
                }

                if (!questions.isEmpty()) {
                    showQuestion();
                } else {
                    Toast.makeText(SentenceCompletionActivity.this, "לא נמצאו שאלות לנושא", Toast.LENGTH_SHORT).show();
                    finish();
                }
            }
            @Override
            public void onCancelled(@NonNull DatabaseError error) {}
        });
    }

    private void showQuestion() {
        if (currentIndex < questions.size()) {
            SentenceQuestion q = questions.get(currentIndex);
            tvSentence.setText(q.getSentence());
            etAnswer.setText("");
            etAnswer.setError(null);

            int resId = getResources().getIdentifier(q.getHintImage(), "drawable", getPackageName());
            ivHint.setImageResource(resId != 0 ? resId : R.drawable.wizard_placeholder);
        } else {
            finishGame();
        }
    }

    private void checkAnswer() {
        if (questions.isEmpty()) return;

        String userAns = etAnswer.getText().toString().trim();
        String correctAns = questions.get(currentIndex).getCorrectAnswer();

        if (userAns.equalsIgnoreCase(correctAns)) {
            etAnswer.setBackgroundTintList(ColorStateList.valueOf(Color.GREEN));
            Toast.makeText(this, "מצוין! ✨", Toast.LENGTH_SHORT).show();

            new Handler().postDelayed(() -> {
                etAnswer.setBackgroundTintList(null);
                currentIndex++;
                showQuestion();
            }, 600);

        } else {
            attempts++;
            etAnswer.setBackgroundTintList(ColorStateList.valueOf(Color.RED));
            etAnswer.setError("נסה שוב...");

            new Handler().postDelayed(() -> {
                etAnswer.setBackgroundTintList(null);
            }, 600);
        }
    }

    private void finishGame() {
        long thisGameTime = (System.currentTimeMillis() - startTime) / 1000;

        int totalAttemptsSoFar = attemptsFromBefore + attempts;
        long totalTimeSoFar = timeFromBefore + thisGameTime;

        Intent intent = new Intent(this, MemoryGameActivity.class);
        intent.putExtra("subject", subject);
        intent.putExtra("totalAttempts", totalAttemptsSoFar);
        intent.putExtra("totalTime", totalTimeSoFar);
        intent.putExtra("gameStep", 3); // התחנה האחרונה

        startActivity(intent);
        finish();
    }

    private void setupCustomKeyboard() {
        KeyboardAdapter adapter = new KeyboardAdapter(this, hebrewLetters, new KeyboardAdapter.OnKeyClickListener() {
            @Override
            public void onKeyClick(String letter) {
                if (letter.equals("DEL")) {
                    String str = etAnswer.getText().toString();
                    if (str.length() > 0) {
                        etAnswer.setText(str.substring(0, str.length() - 1));
                    }
                } else {
                    etAnswer.append(letter);
                }
            }
        });
        keyboardGrid.setAdapter(adapter);
    }
}