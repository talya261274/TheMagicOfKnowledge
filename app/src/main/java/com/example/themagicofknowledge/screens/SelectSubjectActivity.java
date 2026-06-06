package com.example.themagicofknowledge.screens;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.cardview.widget.CardView;

import com.example.themagicofknowledge.R;
import com.example.themagicofknowledge.models.UserChild;
import com.example.themagicofknowledge.services.DatabaseService;
import com.example.themagicofknowledge.utils.SharedPreferencesUtil;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.Map;

public class SelectSubjectActivity extends BaseActivity {

    private CardView btnAnimals, btnColors, btnNumbers, btnLetters, btnShapes, btnBodyParts;
    private ImageView ivVAnimals, ivVColors, ivVNumbers, ivVLetters, ivVShapes, ivVBodyParts;
    private UserChild currentChild;
    private boolean levelUpDialogShown = false;

    @Override
    protected boolean hasSideMenu() {
        return true;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_select_subject);
        currentChild = SharedPreferencesUtil.getCurrentChild(this);
        initViews();
        setupClickListeners();
        checkCompletedSubjects();
    }

    private void initViews() {
        btnAnimals = findViewById(R.id.btnAnimals);
        btnColors = findViewById(R.id.btnColors);
        btnNumbers = findViewById(R.id.btnNumbers);
        btnLetters = findViewById(R.id.btnLetters);
        btnShapes = findViewById(R.id.btnShapes);
        btnBodyParts = findViewById(R.id.btnBodyParts);

        ivVAnimals = findViewById(R.id.ivCompletedAnimals);
        ivVColors = findViewById(R.id.ivCompletedColors);
        ivVNumbers = findViewById(R.id.ivCompletedNumbers);
        ivVLetters = findViewById(R.id.ivCompletedLetters);
        ivVShapes = findViewById(R.id.ivCompletedShapes);
        ivVBodyParts = findViewById(R.id.ivCompletedBodyParts);
    }

    private void setupClickListeners() {
        View.OnClickListener listener = v -> {
            String subject = "";
            int id = v.getId();
            if (id == R.id.btnAnimals) subject = "animals";
            else if (id == R.id.btnColors) subject = "colors";
            else if (id == R.id.btnNumbers) subject = "numbers";
            else if (id == R.id.btnLetters) subject = "letters";
            else if (id == R.id.btnShapes) subject = "shapes";
            else if (id == R.id.btnBodyParts) subject = "bodyparts";

            final String finalSubject = subject;

            // בדוק אם כבר ראה את הכרטיסיות
            SharedPreferences prefs = getSharedPreferences("flashcard_prefs", MODE_PRIVATE);
            boolean seenFlashCards = prefs.getBoolean(
                    "seen_" + currentChild.getId() + "_" + finalSubject, false);

            if (!seenFlashCards) {
                // פעם ראשונה - פתח כרטיסיות
                Intent intent = new Intent(SelectSubjectActivity.this, FlashCardMain.class);
                intent.putExtra("subject", finalSubject);
                startActivity(intent);
            } else {
                // כבר ראה - ישר למשחק, עם אפשרות לכרטיסיות
                showPlayOrFlashDialog(finalSubject);
            }
        };

        btnAnimals.setOnClickListener(listener);
        btnColors.setOnClickListener(listener);
        btnNumbers.setOnClickListener(listener);
        btnLetters.setOnClickListener(listener);
        btnShapes.setOnClickListener(listener);
        btnBodyParts.setOnClickListener(listener);
    }

    private void showPlayOrFlashDialog(String subject) {
        final android.app.Dialog dialog = new android.app.Dialog(this);
        dialog.setContentView(R.layout.dialog_play_or_flash);
        if (dialog.getWindow() != null)
            dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);

        dialog.findViewById(R.id.btnPlayNow).setOnClickListener(v -> {
            dialog.dismiss();
            Intent intent = new Intent(this, MixedGameActivity.class);
            intent.putExtra("subject", subject);
            startActivity(intent);
        });

        dialog.findViewById(R.id.btnShowCards).setOnClickListener(v -> {
            dialog.dismiss();
            Intent intent = new Intent(this, FlashCardMain.class);
            intent.putExtra("subject", subject);
            startActivity(intent);
        });

        dialog.setCancelable(true);
        dialog.show();
    }

    private void checkCompletedSubjects() {
        if (currentChild == null) return;
        String ageGroup = currentChild.getAgeGroup();
        if (ageGroup == null || ageGroup.isEmpty()) return;

        DatabaseService.getInstance().listenToChildProgress(currentChild.getParentId(), currentChild.getId(), ageGroup, new DatabaseService.DatabaseCallback<DataSnapshot>() {
            @Override
            public void onCompleted(DataSnapshot snapshot) {
                int completedCount = 0;
                String[] subjects = {"animals", "colors", "numbers", "letters", "shapes", "bodyparts"};
                ImageView[] vImages = {ivVAnimals, ivVColors, ivVNumbers, ivVLetters, ivVShapes, ivVBodyParts};

                for (int i = 0; i < subjects.length; i++) {
                    boolean isComplete = false;
                    if (snapshot.hasChild(subjects[i])) {
                        DataSnapshot subjectSnapshot = snapshot.child(subjects[i]);
                        if (subjectSnapshot.hasChild("completed")) {
                            Boolean completedValue = subjectSnapshot.child("completed").getValue(Boolean.class);
                            isComplete = completedValue != null && completedValue;
                        }
                    }
                    if (vImages[i] != null) vImages[i].setVisibility(isComplete ? View.VISIBLE : View.GONE);
                    if (isComplete) completedCount++;
                }

                if (completedCount >= 6 && !levelUpDialogShown) {
                    levelUpDialogShown = true;
                    new android.os.Handler().postDelayed(() -> handleAllSubjectsCompleted(), 500);
                }
            }
            @Override
            public void onFailed(Exception e) {}
        });
    }

    /**
     * ⭐⭐⭐ בוחרת איזה דיאלוג להציג ⭐⭐⭐
     * אם הילד ברמה 3-4 או 5-6 → דיאלוג רגיל של עליית רמה
     * אם הילד ברמה 7-8 (הרמה האחרונה) → דיאלוג מיוחד של "סיימת את הקסם!"
     */
    private void handleAllSubjectsCompleted() {
        if (currentChild == null) return;

        // בדוק אם כבר הראינו את הדיאלוג והמשתמש בחר "אחר כך"
        SharedPreferences prefs = getSharedPreferences("level_up_prefs", MODE_PRIVATE);
        boolean postponed = prefs.getBoolean("postponed_" + currentChild.getId() + "_" + currentChild.getAgeGroup(), false);

        if (postponed) return; // אל תציג שוב

        String currentLevel = currentChild.getAgeGroup();
        if (currentLevel.equals("7-8")) {
            showFinalAchievementDialog();
        } else {
            showLevelUpDialog();
        }
    }
    /**
     * דיאלוג עליית רמה רגיל (לרמות 3-4 ו-5-6)
     */
    private void showLevelUpDialog() {
        final android.app.Dialog dialog = new android.app.Dialog(this);
        dialog.setContentView(R.layout.dialog_level_up);

        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
        }

        Button btnAction = dialog.findViewById(R.id.btnLevelUp);
        TextView btnNotNow = dialog.findViewById(R.id.btnNotNow);

        if (btnAction != null) {
            btnAction.setOnClickListener(v -> {
                dialog.dismiss();
                handleLevelUpgrade();
            });
        }

        if (btnNotNow != null) {
            btnNotNow.setOnClickListener(v -> {
                // שמור שבחר "אחר כך"
                getSharedPreferences("level_up_prefs", MODE_PRIVATE)
                        .edit()
                        .putBoolean("postponed_" + currentChild.getId() + "_" + currentChild.getAgeGroup(), true)
                        .apply();
                dialog.dismiss();
            });
        }

        dialog.setCancelable(false);
        dialog.show();
    }

    /**
     * ⭐⭐⭐ דיאלוג סיום סופי לרמה 7-8 ⭐⭐⭐
     * מציג "סיימת את הקסם!" עם 2 כפתורים:
     * 1. שחק מההתחלה - איפוס completedSubjects
     * 2. חזור הביתה - finish()
     */
    private void showFinalAchievementDialog() {
        final android.app.Dialog dialog = new android.app.Dialog(this);
        dialog.setContentView(R.layout.dialog_final_achievement);

        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
        }

        Button btnPlayAgain = dialog.findViewById(R.id.btnPlayAgain);
        Button btnGoHome = dialog.findViewById(R.id.btnGoHome);

        if (btnPlayAgain != null) {
            btnPlayAgain.setOnClickListener(v -> {
                dialog.dismiss();
                resetAllSubjects();
            });
        }

        if (btnGoHome != null) {
            btnGoHome.setOnClickListener(v -> {
                dialog.dismiss();
                Intent intent = new Intent(SelectSubjectActivity.this, MainActivity.class);
                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                startActivity(intent);
                finish();
            });
        }

        dialog.setCancelable(false);
        dialog.show();
    }

    /**
     * ⭐⭐⭐  איפוס כל הנושאים ⭐⭐⭐
     * מוחק את completedSubjects כך שהילד יוכל לשחק שוב מההתחלה
     */
    private void resetAllSubjects() {
        String ageGroup = currentChild.getAgeGroup();
        String[] subjects = {"animals", "colors", "numbers", "letters", "shapes", "bodyparts"};

        Map<String, Object> updates = new java.util.HashMap<>();
        for (String subject : subjects) {
            updates.put(subject + "/completed", false);
            updates.put(subject + "/progressPercent", 0);
            updates.put(subject + "/attempts", 0);
            updates.put(subject + "/timeSeconds", 0);
        }

        DatabaseService.getInstance().resetAllSubjectsProgress(currentChild.getParentId(), currentChild.getId(), ageGroup, updates, new DatabaseService.DatabaseCallback<Void>() {
            @Override
            public void onCompleted(Void unused) {
                Toast.makeText(SelectSubjectActivity.this, "מעולה! בואו נתחיל מחדש 🎮", Toast.LENGTH_SHORT).show();
            }
            @Override
            public void onFailed(Exception e) {
                Toast.makeText(SelectSubjectActivity.this, "שגיאה באיפוס: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    /**
     * עליית רמה רגילה (3-4 → 5-6 או 5-6 → 7-8)
     */
    private void handleLevelUpgrade() {
        // מחק את הדגל של "אחר כך"
        getSharedPreferences("level_up_prefs", MODE_PRIVATE)
                .edit()
                .remove("postponed_" + currentChild.getId() + "_" + currentChild.getAgeGroup())
                .apply();

        String currentLevel = currentChild.getAgeGroup();
        String nextLevel;
        if (currentLevel.equals("3-4")) nextLevel = "5-6";
        else if (currentLevel.equals("5-6")) nextLevel = "7-8";
        else return;

        Intent intent = new Intent(SelectSubjectActivity.this, PlacementTestActivity.class);
        intent.putExtra("isUpgrade", true);
        intent.putExtra("targetLevel", nextLevel);
        startActivity(intent);
    }
}