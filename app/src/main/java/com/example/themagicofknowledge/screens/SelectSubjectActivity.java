package com.example.themagicofknowledge.screens;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;

import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;

import com.example.themagicofknowledge.R;

public class SelectSubjectActivity extends AppCompatActivity {

    private CardView btnAnimals, btnColors, btnNumbers, btnLetters, btnShapes, btnBodyParts;
    private Button goBackBtn2;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_select_subject); // הקובץ XML שלך

        // חיבור הכרטיסים והכפתור מה־XML
        btnAnimals = findViewById(R.id.btnAnimals);
        btnColors = findViewById(R.id.btnColors);
        btnNumbers = findViewById(R.id.btnNumbers);
        btnLetters = findViewById(R.id.btnLetters);
        btnShapes = findViewById(R.id.btnShapes);
        btnBodyParts = findViewById(R.id.btnBodyParts);
        goBackBtn2 = findViewById(R.id.goBackBtn2);

        // לדוגמה: לחיצה על חיות
        btnAnimals.setOnClickListener(v -> {
            Intent intent = new Intent(SelectSubjectActivity.this, FlashCardMain.class);
            intent.putExtra("subject", "animals"); // אם רוצים להעביר נושא
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

        // כפתור חזרה
        goBackBtn2.setOnClickListener(v -> finish()); // סוגר את המסך ומחזיר למסך הקודם

        /*
        cardAnimals.setOnClickListener(v -> {
            Intent intent = new Intent(this, GameActivity.class);
            intent.putExtra("SUBJECT", "Animals");
            intent.putExtra("LEVEL", currentChild.getAgeGroup()); // נשלח את הרמה ששמרנו ב-SelectChild
            startActivity(intent);
        });

         */

    }
}



