package com.example.themagicofknowledge.screens;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.example.themagicofknowledge.R;

public class GameResultActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_game_result);

        boolean success = getIntent().getBooleanExtra("success", false);
        int attempts = getIntent().getIntExtra("attempts", 1);
        String subject = getIntent().getStringExtra("subject");

        TextView tvTitle = findViewById(R.id.tvResultTitle);
        TextView tvAttempts = findViewById(R.id.tvAttempts);
        Button btnAction = findViewById(R.id.btnAction);

        if (success) {
            tvTitle.setText("כל הכבוד! סיימת בהצלחה! 🎉");
            tvAttempts.setText("מספר ניסיונות: " + attempts);
            btnAction.setText("חזרה לנושאים");
            btnAction.setOnClickListener(v -> {
                Intent intent = new Intent(this, SelectSubjectActivity.class);
                intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                startActivity(intent);
                finish();
            });
        } else {
            tvTitle.setText("נסה שוב! 💪");
            tvAttempts.setText("מספר ניסיונות: " + attempts);
            btnAction.setText("התחל מחדש");
            btnAction.setOnClickListener(v -> {
                Intent intent = new Intent(this, ImageRecognitionGameActivity.class);
                intent.putExtra("subject", subject);
                intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                startActivity(intent);
                finish();
            });
        }
    }
}