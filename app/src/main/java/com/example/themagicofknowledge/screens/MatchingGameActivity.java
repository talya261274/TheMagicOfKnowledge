package com.example.themagicofknowledge.screens;

import android.content.ClipData;
import android.content.Intent;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.DragEvent;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.example.themagicofknowledge.R;
import com.example.themagicofknowledge.models.GameProgress;
import com.example.themagicofknowledge.models.Pair;
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
import java.util.Collections;
import java.util.List;

public class MatchingGameActivity extends AppCompatActivity {

    private LinearLayout leftColumn, rightColumn;
    private ProgressBar testProgress;
    private DatabaseReference mDatabase;
    private String ageGroup;
    private int matchesFound = 0;
    private int totalPairs = 0;
    private long startTime;
    private int attempts = 0;
    private String subject;

    private int attemptsFromBefore;
    private long timeFromBefore;

    private android.speech.tts.TextToSpeech tts;

    private UserChild currentChild;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_matching_game);

        testProgress = findViewById(R.id.testProgress);
        if (testProgress != null) {
            testProgress.setMax(3);
            testProgress.setProgress(2);
        }

        attemptsFromBefore = getIntent().getIntExtra("totalAttempts", 0);
        timeFromBefore = getIntent().getLongExtra("totalTime", 0);
        subject = getIntent().getStringExtra("subject");
        if (subject == null) subject = "animals";

        currentChild = SharedPreferencesUtil.getCurrentChild(this);
        if (currentChild != null) {
            ageGroup = currentChild.getAgeGroup();
        } else {
            ageGroup = "3-4";
        }

        leftColumn = findViewById(R.id.leftColumn);
        rightColumn = findViewById(R.id.rightColumn);

        loadGameData();
        startTime = System.currentTimeMillis();

        tts = new android.speech.tts.TextToSpeech(this, status -> {
            if (status != android.speech.tts.TextToSpeech.SUCCESS) {
                Toast.makeText(this, "שגיאה באתחול הדיבור", Toast.LENGTH_SHORT).show();
            } else {
                tts.setLanguage(new java.util.Locale("he")); // הגדרת עברית
            }
        });
    }

    private void loadGameData() {
        String path = "level_" + ageGroup.replace("-", "_");

        DatabaseReference ref = FirebaseDatabase.getInstance().getReference("Games")
                .child("matchingGame")
                .child(path)
                .child(subject)
                .child("pairs");

        ref.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                List<Pair> pairsList = new ArrayList<>();
                for (DataSnapshot ds : snapshot.getChildren()) {
                    Pair p = ds.getValue(Pair.class);
                    if (p != null) pairsList.add(p);
                }

                if (pairsList.isEmpty()) {
                    Toast.makeText(MatchingGameActivity.this, "לא נמצאו נתונים בנתיב" + path, Toast.LENGTH_SHORT).show();
                    finish();
                } else {
                    setupGame(pairsList);
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {}
        });
    }

    private void setupGame(List<Pair> pairs) {
        totalPairs = pairs.size();

        List<Pair> shuffledRight = new ArrayList<>(pairs);
        Collections.shuffle(shuffledRight);

        for (int i = 0; i < pairs.size(); i++) {
            addLeftItem(pairs.get(i));
            addRightItem(shuffledRight.get(i));
        }
    }

    private void addLeftItem(Pair pair) {
        MaterialButton btn = new MaterialButton(this);
        setupContent(btn, pair.getLeft(), pair.getId());
        btn.setTag(pair.getId());
        styleButton(btn);

        btn.setOnLongClickListener(v -> {
            ClipData data = ClipData.newPlainText("pairId", String.valueOf(v.getTag()));
            v.startDragAndDrop(data, new View.DragShadowBuilder(v), v, 0);
            v.setVisibility(View.INVISIBLE);
            return true;
        });

        leftColumn.addView(btn);
    }

    private void addRightItem(Pair pair) {
        MaterialButton btn = new MaterialButton(this);
        setupContent(btn, pair.getRight(), pair.getId());
        btn.setTag(pair.getId());
        styleButton(btn);

        btn.setOnDragListener((v, event) -> {
            switch (event.getAction()) {
                case DragEvent.ACTION_DRAG_STARTED: return true;
                case DragEvent.ACTION_DRAG_ENTERED:
                    v.setBackgroundTintList(ColorStateList.valueOf(Color.parseColor("#FFE082")));
                    return true;
                case DragEvent.ACTION_DRAG_EXITED:
                    v.setBackgroundTintList(ColorStateList.valueOf(Color.WHITE));
                    return true;
                case DragEvent.ACTION_DROP:
                    String droppedId = event.getClipData().getItemAt(0).getText().toString();

                    if (droppedId.equals(String.valueOf(v.getTag()))) {
                        // הצלחה בהתאמה
                        v.setBackgroundTintList(ColorStateList.valueOf(Color.GREEN));
                        v.setEnabled(false);
                        ((View) event.getLocalState()).setVisibility(View.GONE);
                        matchesFound++;
                        checkIfGameFinished(); // בודקים אם סיימנו הכל
                    } else {
                        // טעות בהתאמה
                        attempts++;
                        v.setBackgroundTintList(ColorStateList.valueOf(Color.RED));
                        new Handler(Looper.getMainLooper()).postDelayed(() ->
                                v.setBackgroundTintList(ColorStateList.valueOf(Color.WHITE)), 500);
                    }
                    return true;
                case DragEvent.ACTION_DRAG_ENDED:
                    if (!event.getResult()) {
                        ((View) event.getLocalState()).setVisibility(View.VISIBLE);
                    }
                    return true;
            }
            return false;
        });

        rightColumn.addView(btn);
    }

    private void setupContent(MaterialButton btn, String content, String matchId) {
        if (content == null) return;

        if (content.startsWith("ss_")) {
            int resId = getResources().getIdentifier(content, "drawable", getPackageName());
            btn.setIconResource(resId != 0 ? resId : R.drawable.wizard_placeholder);
            btn.setText("");
            btn.setIconSize(180);
            btn.setIconTint(null);
        }
        else if (content.equalsIgnoreCase("audio") || content.equalsIgnoreCase("speaker")) {
            btn.setIconResource(R.drawable.ic_volume_up);
            btn.setText("");
            btn.setIconSize(150);
            btn.setIconTint(ColorStateList.valueOf(Color.parseColor("#2196F3")));

            btn.setOnClickListener(v -> {
                if (tts != null && matchId != null) {
                    tts.speak(matchId, android.speech.tts.TextToSpeech.QUEUE_FLUSH, null, "tts1");
                }
            });
        }
        else {
            btn.setText(content);
            btn.setIconResource(0);
            btn.setTextSize(20);
        }
    }
    private void styleButton(MaterialButton btn) {
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, 220);
        params.setMargins(10, 10, 10, 10);
        btn.setLayoutParams(params);
        btn.setBackgroundTintList(ColorStateList.valueOf(Color.WHITE));
        btn.setTextColor(Color.BLACK);
        btn.setCornerRadius(20);
        btn.setStrokeWidth(3);
        btn.setStrokeColor(ColorStateList.valueOf(Color.LTGRAY));
    }

    private void checkIfGameFinished() {
        if (matchesFound == totalPairs && totalPairs > 0) {
            finishGame();
        }
    }

    private void finishGame() {
        long currentTimeSeconds = (System.currentTimeMillis() - startTime) / 1000;

        int totalAttemptsSoFar = attemptsFromBefore + attempts;
        long totalTimeSoFar = timeFromBefore + currentTimeSeconds;

        Intent intent = new Intent(this, MemoryGameActivity.class);
        intent.putExtra("subject", subject);
        intent.putExtra("totalAttempts", totalAttemptsSoFar);
        intent.putExtra("totalTime", totalTimeSoFar);
        intent.putExtra("gameStep", 3);

        startActivity(intent);
        finish();
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