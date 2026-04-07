package com.example.themagicofknowledge.screens;

import android.content.Intent;
import android.os.Bundle;

import androidx.cardview.widget.CardView;

import com.example.themagicofknowledge.R;

public class SelectSubjectActivity extends BaseActivity  {

    private CardView btnAnimals, btnColors, btnNumbers, btnLetters, btnShapes, btnBodyParts;

    @Override
    protected boolean hasSideMenu() {
        return true;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_select_subject);

        btnAnimals = findViewById(R.id.btnAnimals);
        btnColors = findViewById(R.id.btnColors);
        btnNumbers = findViewById(R.id.btnNumbers);
        btnLetters = findViewById(R.id.btnLetters);
        btnShapes = findViewById(R.id.btnShapes);
        btnBodyParts = findViewById(R.id.btnBodyParts);

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
}



