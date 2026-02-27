package com.example.themagicofknowledge.screens;

import android.os.Bundle;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.bumptech.glide.Glide;
import com.example.themagicofknowledge.R;
import com.example.themagicofknowledge.models.Question;
import com.example.themagicofknowledge.models.UserChild;
import com.example.themagicofknowledge.utils.SharedPreferencesUtil;
import com.google.firebase.database.*;

import java.util.ArrayList;
import java.util.List;

public class ImageRecognitionGameActivity extends AppCompatActivity {

    private List<Question> questions = new ArrayList<>();
    private int currentIndex = 0;
    private int score = 0;

    private TextView tvQuestion;
    private ImageView ivQuestionMedia;
    private Button btnAns1, btnAns2, btnAns3, btnAns4;
    private ProgressBar testProgress;

    private UserChild currentChild;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_image_recognition_game); // השם של ה XML שלך

        initViews();

        currentChild = SharedPreferencesUtil.getCurrentChild(this);
        if (currentChild == null) {
            Toast.makeText(this,"לא נבחר ילד",Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        loadQuestions();
    }

    private void initViews() {
        tvQuestion = findViewById(R.id.tvQuestion);
        ivQuestionMedia = findViewById(R.id.ivQuestionMedia);

        btnAns1 = findViewById(R.id.btnAns1);
        btnAns2 = findViewById(R.id.btnAns2);
        btnAns3 = findViewById(R.id.btnAns3);
        btnAns4 = findViewById(R.id.btnAns4);

        testProgress = findViewById(R.id.testProgress);
    }

    private void loadQuestions() {

        String level = currentChild.getAgeGroup();

        DatabaseReference ref = FirebaseDatabase.getInstance()
                .getReference("Games")
                .child("imageRecognition")
                .child("level " + level);

        ref.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {

                questions.clear();

                for (DataSnapshot ds : snapshot.getChildren()) {
                    Question q = ds.getValue(Question.class);
                    if (q != null) questions.add(q);
                }

                if (questions.isEmpty()) {
                    Toast.makeText(ImageRecognitionGameActivity.this,
                            "אין שאלות",
                            Toast.LENGTH_SHORT).show();
                    finish();
                    return;
                }

                testProgress.setMax(questions.size());
                showQuestion();
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(ImageRecognitionGameActivity.this,
                        error.getMessage(),
                        Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void showQuestion() {

        if (currentIndex >= questions.size()) {
            finishGame();
            return;
        }

        Question q = questions.get(currentIndex);

        testProgress.setProgress(currentIndex + 1);
        tvQuestion.setText(q.getQuestionText());

        Glide.with(this)
                .load(q.getMediaUrl())
                .placeholder(R.drawable.wizard_placeholder)
                .into(ivQuestionMedia);

        List<String> op = q.getOptions();

        btnAns1.setText(op.get(0));
        btnAns2.setText(op.get(1));
        btnAns3.setText(op.get(2));
        btnAns4.setText(op.get(3));

        btnAns1.setOnClickListener(v -> checkAnswer(0));
        btnAns2.setOnClickListener(v -> checkAnswer(1));
        btnAns3.setOnClickListener(v -> checkAnswer(2));
        btnAns4.setOnClickListener(v -> checkAnswer(3));
    }

    private void checkAnswer(int selectedIndex) {

        Question q = questions.get(currentIndex);

        if (selectedIndex == q.getCorrectAnswerIndex()) {
            score++;
            Toast.makeText(this,"נכון!",Toast.LENGTH_SHORT).show();
        } else {
            Toast.makeText(this,"לא נכון",Toast.LENGTH_SHORT).show();
        }

        currentIndex++;
        showQuestion();
    }

    private void finishGame() {
        new AlertDialog.Builder(this)
                .setTitle("סיום משחק")
                .setMessage("ניקוד: " + score + " מתוך " + questions.size())
                .setPositiveButton("סגור",(d,w)->finish())
                .show();
    }
}
