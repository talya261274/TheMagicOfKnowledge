package com.example.themagicofknowledge.screens;

import android.content.Intent;
import android.os.Bundle;
import android.speech.tts.TextToSpeech;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.GridView;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.RatingBar;
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
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class PlacementTestActivity extends AppCompatActivity {

    // משתני נתונים
    private List<Question> testQuestions = new ArrayList<>();
    private int currentQuestionIndex = 0;
    private int correctAnswersCount = 0;
    private boolean isFirstAttempt = true;
    private boolean isUpgradeMode = false;
    private boolean isNewChild = false;  // ⭐ חדש - מציין אם זה ילד חדש
    private UserChild selectedChild;
    private String currentLevel;
    private TextToSpeech tts;

    // רכיבי UI כלליים
    private TextView tvQuestion;
    private ImageView ivQuestionMedia;
    private View cardMedia;
    private View answersContainer;
    private ProgressBar testProgress;

    // רכיבי רמה 1+2 (לחצנים)
    private MaterialButton btnPlayAudio;
    private MaterialButton[] choiceButtons = new MaterialButton[4];

    // רכיבי רמה 3 (מקלדת)
    private View containerKeyboard;
    private GridView keyboardGrid;
    private EditText etAnswer;
    private Button btnSubmit;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_placement_test);
        initViews();
        initTTS();

        selectedChild = SharedPreferencesUtil.getCurrentChild(this);
        if (selectedChild != null) {
            currentLevel = selectedChild.getAgeGroup();
            loadQuestionsForCurrentLevel();
        } else {
            finish();
        }

        // ⭐ קבלת הפרמטרים מה-Intent
        isUpgradeMode = getIntent().getBooleanExtra("isUpgrade", false);
        isNewChild = getIntent().getBooleanExtra("isNewChild", false);
    }

    private void initViews() {
        tvQuestion = findViewById(R.id.tvQuestion);
        ivQuestionMedia = findViewById(R.id.ivQuestionMedia);
        cardMedia = findViewById(R.id.cardMedia);
        btnPlayAudio = findViewById(R.id.btnPlayAudio);
        testProgress = findViewById(R.id.testProgress);
        answersContainer = findViewById(R.id.answersContainer);

        choiceButtons[0] = findViewById(R.id.btnAns1);
        choiceButtons[1] = findViewById(R.id.btnAns2);
        choiceButtons[2] = findViewById(R.id.btnAns3);
        choiceButtons[3] = findViewById(R.id.btnAns4);

        containerKeyboard = findViewById(R.id.containerKeyboard);
        keyboardGrid = findViewById(R.id.placementKeyboard);
        etAnswer = findViewById(R.id.etPlacementAnswer);
        btnSubmit = findViewById(R.id.btnSubmitPlacement);
    }

    private void initTTS() {
        tts = new TextToSpeech(this, status -> {
            if (status == TextToSpeech.SUCCESS) {
                tts.setLanguage(new Locale("he"));
            }
        });
    }

    private void loadQuestionsForCurrentLevel() {
        String levelPath = "level_" + currentLevel.replace("-", "_");
        DatabaseReference ref = FirebaseDatabase.getInstance()
                .getReference("PlacementTests")
                .child(levelPath);

        ref.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
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
                    // ⭐ אם אין שאלות - שמירת ציון ברירת מחדל ומעבר ל-Main
                    saveDefaultScoreAndProceed();
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(PlacementTestActivity.this, "שגיאה בטעינה", Toast.LENGTH_SHORT).show();
                saveDefaultScoreAndProceed();
            }
        });
    }

    /**
     * ⭐ אם אין שאלות במבדק - שומרים ציון ברירת מחדל וממשיכים
     */
    private void saveDefaultScoreAndProceed() {
        updateChildLevelInFirebase(currentLevel, 70.0);  // ציון ברירת מחדל
    }

    private void showNextQuestion() {
        if (currentQuestionIndex >= testQuestions.size()) {
            finishTest();
            return;
        }

        Question q = testQuestions.get(currentQuestionIndex);
        testProgress.setProgress(currentQuestionIndex + 1);

        cardMedia.setVisibility(View.GONE);
        btnPlayAudio.setVisibility(View.GONE);
        answersContainer.setVisibility(View.GONE);
        containerKeyboard.setVisibility(View.GONE);

        if (isUpgradeMode && currentQuestionIndex == 0) {
            tvQuestion.setText("כל הכבוד על עליית הרמה! בוא נראה אם אתה מוכן לאתגר החדש:");
        } else {
            if (currentLevel.equals("3-4")) {
                btnPlayAudio.setVisibility(View.VISIBLE);
                answersContainer.setVisibility(View.VISIBLE);
                tvQuestion.setText("הקשיבו וביחרו בתמונה הנכונה:");
                btnPlayAudio.setOnClickListener(v -> playAudio(q.getQuestionText()));
                playAudio(q.getQuestionText());
                setupChoiceButtons(q.getOptions(), true);
            } else if (currentLevel.equals("5-6")) {
                cardMedia.setVisibility(View.VISIBLE);
                answersContainer.setVisibility(View.VISIBLE);
                tvQuestion.setText("מה מופיע בתמונה?");
                int resId = getResources().getIdentifier(q.getMediaUrl(), "drawable", getPackageName());
                ivQuestionMedia.setImageResource(resId != 0 ? resId : R.drawable.wizard_placeholder);
                setupChoiceButtons(q.getOptions(), false);
            } else if (currentLevel.equals("7-8")) {
                containerKeyboard.setVisibility(View.VISIBLE);
                tvQuestion.setText(q.getQuestionText());
                String correctWord = q.getOptions().get(q.getCorrectAnswerIndex());
                setupPlacementKeyboard(correctWord);
            }
        }
    }

    private void setupChoiceButtons(List<String> options, boolean isImageMode) {
        for (int i = 0; i < choiceButtons.length; i++) {
            if (isImageMode) {
                int resId = getResources().getIdentifier(options.get(i), "drawable", getPackageName());
                choiceButtons[i].setIconResource(resId != 0 ? resId : R.drawable.wizard_placeholder);
                choiceButtons[i].setText("");
                choiceButtons[i].setIconSize(120);
            } else {
                choiceButtons[i].setIcon(null);
                choiceButtons[i].setText(options.get(i));
            }
            final int index = i;
            choiceButtons[i].setOnClickListener(v -> checkAnswer(index));
        }
    }

    private void setupPlacementKeyboard(String correctWord) {
        etAnswer.setText("");
        String[] alphabet = {"א", "ב", "ג", "ד", "ה", "ו", "ז", "ח", "ט", "י", "כ", "ל", "מ", "נ", "ס", "ע", "פ", "צ", "ק", "ר", "ש", "ת"};

        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, alphabet);
        keyboardGrid.setAdapter(adapter);

        keyboardGrid.setOnItemClickListener((parent, view, position, id) -> {
            etAnswer.append(alphabet[position]);
        });

        btnSubmit.setOnClickListener(v -> {
            if (etAnswer.getText().toString().trim().equals(correctWord)) {
                checkAnswer(999);
            } else {
                isFirstAttempt = false;
                Toast.makeText(this, "תשובה שגויה, נסה שוב", Toast.LENGTH_SHORT).show();
                etAnswer.setText("");
            }
        });
    }

    private void checkAnswer(int selectedIdx) {
        Question q = testQuestions.get(currentQuestionIndex);
        if (selectedIdx == 999 || selectedIdx == q.getCorrectAnswerIndex()) {
            if (isFirstAttempt) correctAnswersCount++;
            currentQuestionIndex++;
            isFirstAttempt = true;
            showNextQuestion();
        } else {
            isFirstAttempt = false;
            Toast.makeText(this, "תשובה שגויה, נסה שוב", Toast.LENGTH_SHORT).show();
        }
    }

    private void playAudio(String text) {
        if (tts != null) tts.speak(text, TextToSpeech.QUEUE_FLUSH, null, null);
    }

    private void finishTest() {
        double percent = ((double) correctAnswersCount / testQuestions.size()) * 100;
        String newLevel = determineNewLevel(currentLevel, percent);

        if (isUpgradeMode && percent < 60) {
            showResultDialog(percent, currentLevel, "איזה אומץ! ניסית רמה קשה יותר. בוא נתאמן עוד קצת על הנושאים כאן כדי להיות אלופים!");
        } else {
            showResultDialog(percent, newLevel, "כל הכבוד! סיימת את המבחן בהצלחה.");
        }
    }

    private String determineNewLevel(String currentLevel, double percent) {
        if (percent >= 90) {
            if (currentLevel.equals("3-4")) return "5-6";
            if (currentLevel.equals("5-6")) return "7-8";
        } else if (percent < 60) {
            if (currentLevel.equals("7-8")) return "5-6";
            if (currentLevel.equals("5-6")) return "3-4";
        }
        return currentLevel;
    }

    private void showResultDialog(double percent, String recommendedLevel, String message) {
        final android.app.Dialog dialog = new android.app.Dialog(this);
        dialog.setContentView(R.layout.dialog_test_result);

        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
        }

        TextView tvTitle = dialog.findViewById(R.id.tvDialogTitle);
        TextView tvMessage = dialog.findViewById(R.id.tvDialogMessage);
        ImageView ivStatus = dialog.findViewById(R.id.ivStatusIcon);
        RatingBar ratingBar = dialog.findViewById(R.id.dialogRatingBar);

        Button btnRecommended = dialog.findViewById(R.id.btnDialogAction);
        Button btnStay = dialog.findViewById(R.id.btnStayAtCurrentLevel);

        if (ratingBar != null) {
            ratingBar.setRating((float) (percent / 20));
        }

        tvMessage.setText(message + "\nהרמה המומלצת עבורך: " + recommendedLevel);

        if (percent >= 90) {
            ivStatus.setImageResource(R.drawable.ic_trophy);
            tvTitle.setText("מדהים!");
        } else if (percent >= 60) {
            ivStatus.setImageResource(R.drawable.ic_rocket);
            tvTitle.setText("כל הכבוד!");
        } else {
            ivStatus.setImageResource(R.drawable.ic_medal);
            tvTitle.setText("נחמד מאוד!");
        }

        btnRecommended.setText("התחל רמה " + recommendedLevel);
        btnRecommended.setOnClickListener(v -> {
            dialog.dismiss();
            updateChildLevelInFirebase(recommendedLevel, percent);
        });

        if (btnStay != null) {
            if (recommendedLevel.equals(currentLevel)) {
                btnStay.setVisibility(View.GONE);
            } else {
                btnStay.setVisibility(View.VISIBLE);
                btnStay.setText("אני מעדיף להישאר ברמה " + currentLevel);
                btnStay.setOnClickListener(v -> {
                    dialog.dismiss();
                    updateChildLevelInFirebase(currentLevel, percent);
                });
            }
        }

        dialog.setCancelable(false);
        dialog.show();
    }

    /**
     * ⭐⭐⭐ הפונקציה המעודכנת ⭐⭐⭐
     * עכשיו שומרת גם את lastPlacementScore ומעבירה ל-MainActivity
     */
    private void updateChildLevelInFirebase(String level, double grade) {
        if (selectedChild == null || selectedChild.getParentId() == null || selectedChild.getId() == null) {
            Toast.makeText(this, "שגיאה: לא נמצאו נתוני ילד", Toast.LENGTH_SHORT).show();
            return;
        }

        String parentId = selectedChild.getParentId();
        String childId = selectedChild.getId();

        DatabaseReference childRef = FirebaseDatabase.getInstance()
                .getReference("users")
                .child(parentId)
                .child("childrenList")
                .child(childId);

        Map<String, Object> updates = new HashMap<>();
        updates.put("ageGroup", level);
        updates.put("lastPlacementScore", grade);  // ⭐ שדה חדש - שומר שעבר מבדק

        childRef.updateChildren(updates)
                .addOnSuccessListener(aVoid -> {
                    // עדכון האובייקט המקומי
                    selectedChild.setAgeGroup(level);
                    selectedChild.setLastPlacementScore(grade);  // ⭐ עדכון השדה החדש
                    SharedPreferencesUtil.saveCurrentChild(this, selectedChild);

                    // ⭐⭐⭐ מעבר ל-MainActivity ⭐⭐⭐
                    Toast.makeText(this, "כל הכבוד! בוא נתחיל לשחק 🎮", Toast.LENGTH_SHORT).show();
                    Intent intent = new Intent(this, MainActivity.class);
                    intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                    startActivity(intent);
                    finish();
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "שגיאה בשמירת הנתונים: " + e.getMessage(), Toast.LENGTH_LONG).show();
                });
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