package com.example.themagicofknowledge.screens;

import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
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
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.example.themagicofknowledge.R;
import com.example.themagicofknowledge.adapter.KeyboardAdapter;
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

public class PlacementTestActivity extends BaseActivity {

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
    private static final String PREF_PLACEMENT_INDEX = "placement_index_";
    private static final String PREF_PLACEMENT_CORRECT = "placement_correct_";

    // רכיבי UI כלליים
    private TextView tvQuestion;
    private ImageView ivQuestionMedia;
    private View cardMedia;
    private View answersContainer;
    private ProgressBar testProgress;

    // רכיבי רמה 1+2 (לחצנים)
    private MaterialButton btnPlayAudio;
    private View audioContainer;
    private ImageView[] choiceButtons = new ImageView[4];
    private TextView[] choiceTextViews = new TextView[4];

    // רכיבי רמה 3 (מקלדת)
    private View containerKeyboard;
    private GridView keyboardGrid;
    private EditText etAnswer;
    private Button btnSubmit;
    private View cardMediaKeyboard;
    private ImageView ivQuestionMediaKeyboard;
    private final String[] hebrewLetters = {
            "ו", "ה", "ד", "ג", "ב", "א", "DEL",
            "מ", "ל", "כ", "י", "ט", "ח", "ז",
            "ר", "ק", "צ", "פ", "ע", "ס", "נ",
            "ץ", "ף", "ן", "ם", "ך", "ת", "ש",
    };

    @Override
    protected boolean hasSideMenu() {
        return false;
    }

    @Override
    protected boolean showToolbar() {
        return false;
    }

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
        audioContainer = findViewById(R.id.audioContainer);
        cardMedia = findViewById(R.id.cardMedia);

        findViewById(R.id.btnExit).setOnClickListener(v -> showExitDialog());

        choiceButtons[0] = findViewById(R.id.btnAns1);
        choiceButtons[1] = findViewById(R.id.btnAns2);
        choiceButtons[2] = findViewById(R.id.btnAns3);
        choiceButtons[3] = findViewById(R.id.btnAns4);

        choiceTextViews[0] = findViewById(R.id.tvAns1);
        choiceTextViews[1] = findViewById(R.id.tvAns2);
        choiceTextViews[2] = findViewById(R.id.tvAns3);
        choiceTextViews[3] = findViewById(R.id.tvAns4);

        cardMediaKeyboard = findViewById(R.id.cardMediaKeyboard);
        ivQuestionMediaKeyboard = findViewById(R.id.ivQuestionMediaKeyboard);
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
                    loadProgress(); //
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

        // הצג דיאלוג פתיחה פעם אחת בלבד
        if (isUpgradeMode && currentQuestionIndex == 0) {
            isUpgradeMode = false;

            final android.app.Dialog dialog = new android.app.Dialog(this);
            dialog.setContentView(R.layout.dialog_upgrade_intro);
            if (dialog.getWindow() != null)
                dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);

            dialog.findViewById(R.id.btnStartTest).setOnClickListener(v -> {
                dialog.dismiss();
                showNextQuestion();
            });

            dialog.findViewById(R.id.btnNotNow).setOnClickListener(v -> {
                dialog.dismiss();
                finish();
            });

            dialog.setCancelable(false);
            dialog.show();
            return;
        }

        Question q = testQuestions.get(currentQuestionIndex);
        testProgress.setProgress(currentQuestionIndex + 1);

        cardMedia.setVisibility(View.GONE);
        audioContainer.setVisibility(View.GONE);
        answersContainer.setVisibility(View.GONE);
        containerKeyboard.setVisibility(View.GONE);

        if (isUpgradeMode && currentQuestionIndex == 0) {
            tvQuestion.setText("כל הכבוד על עליית הרמה! בוא נראה אם אתה מוכן לאתגר החדש:");
        } else {
            if (currentLevel.equals("3-4")) {
                audioContainer.setVisibility(View.VISIBLE);
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
                ivQuestionMedia.setImageResource(resId != 0 ? resId : R.drawable.wizard_placeholder1);
                setupChoiceButtons(q.getOptions(), false);
            } else if (currentLevel.equals("7-8")) {
                containerKeyboard.setVisibility(View.VISIBLE);
                cardMediaKeyboard.setVisibility(View.VISIBLE);
                tvQuestion.setText(q.getQuestionText());
                int resId = getResources().getIdentifier(q.getMediaUrl(), "drawable", getPackageName());
                ivQuestionMediaKeyboard.setImageResource(resId != 0 ? resId : R.drawable.wizard_placeholder1);
                String correctWord = q.getOptions().get(q.getCorrectAnswerIndex());
                setupPlacementKeyboard(correctWord);
            }
        }
    }

    private void setupChoiceButtons(List<String> options, boolean isImageMode) {
        for (int i = 0; i < choiceButtons.length; i++) {
            if (isImageMode) {
                int resId = getResources().getIdentifier(options.get(i), "drawable", getPackageName());
                choiceButtons[i].setImageResource(resId != 0 ? resId : R.drawable.wizard_placeholder1);
                choiceButtons[i].setVisibility(View.VISIBLE);
                choiceTextViews[i].setVisibility(View.GONE);
            } else {
                choiceButtons[i].setImageResource(0);
                choiceButtons[i].setVisibility(View.GONE);
                choiceTextViews[i].setText(options.get(i));
                choiceTextViews[i].setVisibility(View.VISIBLE);
            }

            final int index = i;
            View.OnClickListener listener = v -> {
                Question q = testQuestions.get(currentQuestionIndex);
                if (index == q.getCorrectAnswerIndex()) {
                    shakeAndColorButton(index, true);
                    // המתן רגע לפני מעבר לשאלה הבאה
                    v.postDelayed(() -> checkAnswer(index), 600);
                } else {
                    shakeAndColorButton(index, false);
                    isFirstAttempt = false;
                }
            };

            choiceButtons[i].setOnClickListener(listener);
            choiceTextViews[i].setOnClickListener(listener);
        }
    }

    private void shakeAndColorAnswer(boolean isCorrect) {
        int color = isCorrect ? Color.parseColor("#4CAF50") : Color.parseColor("#F44336");

        // צביעת המסגרת
        View answerCard = etAnswer.getParent() instanceof View ? (View) etAnswer.getParent() : null;
        etAnswer.setTextColor(color);

        // רעד (רק שגוי)
        if (!isCorrect) {
            etAnswer.animate()
                    .translationX(16).setDuration(50).withEndAction(() ->
                            etAnswer.animate()
                                    .translationX(-16).setDuration(50).withEndAction(() ->
                                            etAnswer.animate()
                                                    .translationX(10).setDuration(50).withEndAction(() ->
                                                            etAnswer.animate()
                                                                    .translationX(-10).setDuration(50).withEndAction(() ->
                                                                            etAnswer.animate()
                                                                                    .translationX(0).setDuration(50).start()
                                                                    ).start()).start()).start()).start();

            // איפוס צבע אחרי שנייה
            etAnswer.postDelayed(() -> {
                etAnswer.setTextColor(Color.parseColor("#1E5F8B"));
                etAnswer.setText("");
            }, 800);
        } else {
            // ירוק - ממשיך אחרי רגע
            etAnswer.postDelayed(() -> {
                etAnswer.setTextColor(Color.parseColor("#1E5F8B"));
            }, 600);
        }
    }

    private void setupPlacementKeyboard(String correctWord) {
        etAnswer.setText("");
        KeyboardAdapter adapter = new KeyboardAdapter(this, hebrewLetters, letter -> {
            if (letter.equals("DEL")) {
                String str = etAnswer.getText().toString();
                if (!str.isEmpty()) etAnswer.setText(str.substring(0, str.length() - 1));
            } else {
                etAnswer.append(letter);
            }
        });
        keyboardGrid.setAdapter(adapter);

        btnSubmit.setOnClickListener(v -> {
            String userAnswer = etAnswer.getText().toString().trim();
            if (userAnswer.equals(correctWord)) {
                shakeAndColorAnswer(true);
                etAnswer.postDelayed(() -> checkAnswer(999), 600);
            } else {
                isFirstAttempt = false;
                shakeAndColorAnswer(false);
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

    private void shakeAndColorButton(int index, boolean isCorrect) {
        int[] cardIds = {R.id.cardAns1, R.id.cardAns2, R.id.cardAns3, R.id.cardAns4};
        String[] colors = {"#FF9800", "#4CAF50", "#2196F3", "#E91E63"};

        View card = findViewById(cardIds[index]);
        int feedbackColor = isCorrect ? Color.parseColor("#00ff00") : Color.parseColor("#F44336");

        if (card instanceof com.google.android.material.card.MaterialCardView) {
            ((com.google.android.material.card.MaterialCardView) card)
                    .setCardBackgroundColor(feedbackColor);
        }

        if (!isCorrect) {
            card.animate()
                    .translationX(16).setDuration(50).withEndAction(() ->
                            card.animate()
                                    .translationX(-16).setDuration(50).withEndAction(() ->
                                            card.animate()
                                                    .translationX(10).setDuration(50).withEndAction(() ->
                                                            card.animate()
                                                                    .translationX(-10).setDuration(50).withEndAction(() ->
                                                                            card.animate()
                                                                                    .translationX(0).setDuration(50).start()
                                                                    ).start()).start()).start()).start();

            card.postDelayed(() -> {
                if (card instanceof com.google.android.material.card.MaterialCardView) {
                    ((com.google.android.material.card.MaterialCardView) card)
                            .setCardBackgroundColor(Color.parseColor(colors[index]));
                }
            }, 800);

        } else {
            // ← הוסף: איפוס צבע ירוק אחרי 600ms
            card.postDelayed(() -> {
                if (card instanceof com.google.android.material.card.MaterialCardView) {
                    ((com.google.android.material.card.MaterialCardView) card)
                            .setCardBackgroundColor(Color.parseColor(colors[index]));
                }
            }, 600);
        }
    }

    private void playAudio(String text) {
        if (tts != null) tts.speak(text, TextToSpeech.QUEUE_FLUSH, null, null);
    }

    private void finishTest() {
        double percent = ((double) correctAnswersCount / testQuestions.size()) * 100;
        String targetLevel = getIntent().getStringExtra("targetLevel");

        if (isUpgradeMode) {
            if (percent >= 60) {
                // הצליח - עלה לרמה החדשה
                showResultDialog(percent, targetLevel, "כל הכבוד! אתה מוכן לרמה החדשה!");
            } else {
                // נכשל - נשאר באותה רמה
                showResultDialog(percent, currentLevel, "כמעט! תתאמן עוד קצת ותנסה שוב 💪");
            }
        } else {
            String newLevel = determineNewLevel(currentLevel, percent);
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

        // כותרת, טקסט ואייקון לפי מצב
        if (recommendedLevel.equals(currentLevel)) {
            tvTitle.setText("יופי! 💪");
            tvMessage.setText("אתה ממש חזק ברמה הזו!\nבוא נמשיך להתאמן ולהשתפר");
            ivStatus.setImageResource(R.drawable.ic_medal);
        } else if (isLevelHigher(recommendedLevel, currentLevel)) {
            tvTitle.setText("מדהים! 🚀");
            tvMessage.setText("אתה מוכן לאתגר חדש!\nעולים לרמה " + recommendedLevel);
            ivStatus.setImageResource(R.drawable.ic_trophy);
        } else {
            tvTitle.setText("לא נורא! 😊");
            tvMessage.setText("בוא נתרגל קצת יותר\nברמה " + recommendedLevel + " ונחזור חזקים 💙");
            ivStatus.setImageResource(R.drawable.ic_rocket);
        }

        btnRecommended.setText("התחל רמה " + recommendedLevel);
        btnRecommended.setOnClickListener(v -> {
            dialog.dismiss();
            updateChildLevelInFirebase(recommendedLevel, percent);
        });

        if (btnStay != null) {
            if (recommendedLevel.equals(currentLevel)) {
                // נשאר באותה רמה - הצג "אולי אחר כך"
                btnStay.setVisibility(View.VISIBLE);
                btnStay.setText("אולי אחר כך 😴");
                btnStay.setOnClickListener(v -> {
                    dialog.dismiss();
                    finish();
                });
            } else {
                // רמה שונה - הצג "להישאר ברמה הנוכחית"
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

    private boolean isLevelHigher(String level, String current) {
        List<String> levels = java.util.Arrays.asList("3-4", "5-6", "7-8");
        return levels.indexOf(level) > levels.indexOf(current);
    }


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
        updates.put("lastPlacementScore", grade);

        if (isNewChild) {
            updates.put("startingLevel", level);
        }

        childRef.updateChildren(updates)
                .addOnSuccessListener(aVoid -> {
                    // עדכון האובייקט המקומי
                    selectedChild.setAgeGroup(level);
                    selectedChild.setLastPlacementScore(grade);  //  עדכון השדה החדש
                    SharedPreferencesUtil.saveCurrentChild(this, selectedChild);

                    //  מעבר ל-MainActivity
                    Toast.makeText(this, "כל הכבוד! בוא נתחיל לשחק 🎮", Toast.LENGTH_SHORT).show();
                    Intent intent = new Intent(this, MainActivity.class);
                    intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                    clearProgress();
                    startActivity(intent);
                    finish();
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "שגיאה בשמירת הנתונים: " + e.getMessage(), Toast.LENGTH_LONG).show();
                });
    }

    private void showExitDialog() {
        boolean hasStarted = currentQuestionIndex > 0;

        String title = hasStarted ? "לצאת מהמבדק?" : "לצאת לפני שמתחילים?";
        String message = hasStarted
                ? "התקדמת כבר " + currentQuestionIndex + " שאלות! נשמור את המקום שלך 💾"
                : "אל דאגה! תוכל לחזור ולהתחיל בפעם הבאה 🌟";
        String btnText = hasStarted ? "שמור וצא" : "יציאה";

        showCustomDialog(
                title,
                message,
                btnText,
                Color.parseColor("#FF9800"),
                () -> {
                    saveProgress();
                    Intent intent = new Intent(this, SelectChildActivity.class);
                    intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                    startActivity(intent);
                    finish();
                }
        );
    }

    private void saveProgress() {
        SharedPreferences prefs = getSharedPreferences("placement_prefs", MODE_PRIVATE);
        prefs.edit()
                .putInt(PREF_PLACEMENT_INDEX + currentLevel, currentQuestionIndex)
                .putInt(PREF_PLACEMENT_CORRECT + currentLevel, correctAnswersCount)
                .apply();
    }

    private void loadProgress() {
        SharedPreferences prefs = getSharedPreferences("placement_prefs", MODE_PRIVATE);
        currentQuestionIndex = prefs.getInt(PREF_PLACEMENT_INDEX + currentLevel, 0);
        correctAnswersCount = prefs.getInt(PREF_PLACEMENT_CORRECT + currentLevel, 0);
    }

    private void clearProgress() {
        SharedPreferences prefs = getSharedPreferences("placement_prefs", MODE_PRIVATE);
        prefs.edit()
                .remove(PREF_PLACEMENT_INDEX + currentLevel)
                .remove(PREF_PLACEMENT_CORRECT + currentLevel)
                .apply();
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