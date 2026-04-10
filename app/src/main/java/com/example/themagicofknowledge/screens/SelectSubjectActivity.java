package com.example.themagicofknowledge.screens;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;

import androidx.cardview.widget.CardView;

import com.example.themagicofknowledge.R;
import com.example.themagicofknowledge.models.UserChild;
import com.example.themagicofknowledge.utils.SharedPreferencesUtil;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

public class SelectSubjectActivity extends BaseActivity  {

    private CardView btnAnimals, btnColors, btnNumbers, btnLetters, btnShapes, btnBodyParts;
    private ImageView ivVAnimals, ivVColors, ivVNumbers, ivVLetters, ivVShapes, ivVBodyParts;
    private UserChild currentChild;

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
        checkCompletedSubjects();

        btnAnimals.setOnClickListener(v -> {
            Intent intent = new Intent(SelectSubjectActivity.this, FlashCardMain.class);
            intent.putExtra("subject", "animals");
            startActivity(intent);
        });

        btnColors.setOnClickListener(v -> {
            Intent intent = new Intent(SelectSubjectActivity.this, FlashCardMain.class);
            intent.putExtra("subject", "colors");
            startActivity(intent);
        });

        btnNumbers.setOnClickListener(v -> {
            Intent intent = new Intent(SelectSubjectActivity.this, FlashCardMain.class);
            intent.putExtra("subject", "numbers");
            startActivity(intent);
        });

        btnLetters.setOnClickListener(v -> {
            Intent intent = new Intent(SelectSubjectActivity.this, FlashCardMain.class);
            intent.putExtra("subject", "letters");
            startActivity(intent);
        });

        btnShapes.setOnClickListener(v -> {
            Intent intent = new Intent(SelectSubjectActivity.this, FlashCardMain.class);
            intent.putExtra("subject", "shapes");
            startActivity(intent);
        });

        btnBodyParts.setOnClickListener(v -> {
            Intent intent = new Intent(SelectSubjectActivity.this, FlashCardMain.class);
            intent.putExtra("subject", "bodyparts");
            startActivity(intent);
        });
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

    private void checkCompletedSubjects() {
        if (currentChild == null) return;

        DatabaseReference ref = FirebaseDatabase.getInstance().getReference("Parents")
                .child(currentChild.getParentId())
                .child("children")
                .child(currentChild.getId())
                .child("completedSubjects");

        ref.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot snapshot) {
                // בדיקה עבור כל נושא - אם הערך הוא true, נציג את ה-V
                if (snapshot.hasChild("animals")) ivVAnimals.setVisibility(View.VISIBLE);
                if (snapshot.hasChild("colors")) ivVColors.setVisibility(View.VISIBLE);
                if (snapshot.hasChild("numbers")) ivVNumbers.setVisibility(View.VISIBLE);
                if (snapshot.hasChild("letters")) ivVLetters.setVisibility(View.VISIBLE);
                if (snapshot.hasChild("shapes")) ivVShapes.setVisibility(View.VISIBLE);
                if (snapshot.hasChild("bodyparts")) ivVBodyParts.setVisibility(View.VISIBLE);
            }

            @Override
            public void onCancelled(DatabaseError error) {}
        });
    }
}



