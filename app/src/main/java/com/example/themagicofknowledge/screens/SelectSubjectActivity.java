package com.example.themagicofknowledge.screens;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.cardview.widget.CardView;
import com.example.themagicofknowledge.R;
import com.example.themagicofknowledge.models.UserChild;
import com.example.themagicofknowledge.utils.SharedPreferencesUtil;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

public class SelectSubjectActivity extends BaseActivity {

    private CardView btnAnimals, btnColors, btnNumbers, btnLetters, btnShapes, btnBodyParts;
    private ImageView ivVAnimals, ivVColors, ivVNumbers, ivVLetters, ivVShapes, ivVBodyParts;
    private UserChild currentChild;

    @Override
    protected boolean hasSideMenu() { return true; }

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

            Intent intent = new Intent(SelectSubjectActivity.this, FlashCardMain.class);
            intent.putExtra("subject", subject);
            startActivity(intent);
        };

        btnAnimals.setOnClickListener(listener);
        btnColors.setOnClickListener(listener);
        btnNumbers.setOnClickListener(listener);
        btnLetters.setOnClickListener(listener);
        btnShapes.setOnClickListener(listener);
        btnBodyParts.setOnClickListener(listener);
    }

    private void checkCompletedSubjects() {
        if (currentChild == null) return;

        DatabaseReference ref = FirebaseDatabase.getInstance().getReference("users")
                .child(currentChild.getParentId())
                .child("childrenList")
                .child(currentChild.getId())
                .child("completedSubjects");

        ref.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                int completedCount = 0;
                String[] subjects = {"animals", "colors", "numbers", "letters", "shapes", "bodyparts"};
                ImageView[] vImages = {ivVAnimals, ivVColors, ivVNumbers, ivVLetters, ivVShapes, ivVBodyParts};

                for (int i = 0; i < subjects.length; i++) {
                    if (snapshot.hasChild(subjects[i])) {
                        Boolean isComplete = snapshot.child(subjects[i]).getValue(Boolean.class);
                        if (isComplete != null && isComplete) {
                            if (vImages[i] != null) vImages[i].setVisibility(View.VISIBLE);
                            completedCount++;
                        }
                    } else {
                        if (vImages[i] != null) vImages[i].setVisibility(View.GONE);
                    }
                }

                if (completedCount >= 6) {
                    new android.os.Handler().postDelayed(() -> showLevelUpDialog(), 500);
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {}
        });
    }

    // הפונקציה הזו חייבת להיות כאן - ישירות תחת ה-Class
    private void showLevelUpDialog() {
        final android.app.Dialog dialog = new android.app.Dialog(this);
        dialog.setContentView(R.layout.dialog_level_up);

        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
        }

        Button btnAction = dialog.findViewById(R.id.btnLevelUp);
        if (btnAction != null) {
            btnAction.setOnClickListener(v -> {
                dialog.dismiss();
                handleLevelUpgrade();
            });
        }
        dialog.setCancelable(false);
        dialog.show();
    }

    private void handleLevelUpgrade() {
        String currentLevel = currentChild.getAgeGroup();
        String nextLevel;

        if (currentLevel.equals("3-4")) nextLevel = "5-6";
        else if (currentLevel.equals("5-6")) nextLevel = "7-8";
        else {
            Toast.makeText(this, "סיימת את כל הרמות! אלוף!", Toast.LENGTH_LONG).show();
            return;
        }

        DatabaseReference childRef = FirebaseDatabase.getInstance().getReference("users")
                .child(currentChild.getParentId())
                .child("childrenList")
                .child(currentChild.getId());

        childRef.child("ageGroup").setValue(nextLevel).addOnSuccessListener(aVoid -> {
            childRef.child("completedSubjects").removeValue().addOnSuccessListener(unused -> {
                currentChild.setAgeGroup(nextLevel);
                SharedPreferencesUtil.saveCurrentChild(SelectSubjectActivity.this, currentChild);

                Intent intent = new Intent(SelectSubjectActivity.this, PlacementTestActivity.class);
                intent.putExtra("isUpgrade", true);
                intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                startActivity(intent);
                finish();
            });
        });
    }
}