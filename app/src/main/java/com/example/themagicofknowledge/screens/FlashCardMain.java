package com.example.themagicofknowledge.screens;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.speech.tts.TextToSpeech;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.themagicofknowledge.R;
import com.example.themagicofknowledge.models.FlashCard;
import com.example.themagicofknowledge.models.UserChild;
import com.example.themagicofknowledge.models.UserParent;
import com.example.themagicofknowledge.services.DatabaseService;
import com.example.themagicofknowledge.utils.SharedPreferencesUtil;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.firebase.database.DataSnapshot;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public class FlashCardMain extends AppCompatActivity {

    private TextView tvTitle, tvCardText, tvProgress;
    private ImageView imgCard;
    private MaterialCardView card;
    private MaterialButton btnStartGame;
    private MaterialButton btnNext, btnPrev;
    private FloatingActionButton btnPlaySound;

    private UserChild currentChild;
    private String subject;

    private ArrayList<FlashCard> cards;
    private int currentIndex = 0;
    private boolean showingImage = true;

    private TextToSpeech tts;
    private boolean ttsReady = false;

    private long startTime;

    private static final Map<String, String> SUBJECT_COLORS = new HashMap<>();
    static {
        SUBJECT_COLORS.put("animals", "#29A6F0");
        SUBJECT_COLORS.put("colors", "#4CA621");
        SUBJECT_COLORS.put("numbers", "#E097A4");
        SUBJECT_COLORS.put("letters", "#F4D248");
        SUBJECT_COLORS.put("shapes", "#FF914D");
        SUBJECT_COLORS.put("bodyparts", "#FF5757");
    }

    @SuppressLint("MissingInflatedId")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_flash_card_main);

        currentChild = SharedPreferencesUtil.getCurrentChild(this);
        subject = getIntent().getStringExtra("subject");

        if (currentChild == null || subject == null) {
            finish();
            return;
        }

        startTime = System.currentTimeMillis();

        initViews();
        initializeTTS();
        loadCardsFromFirebase();
    }

    private void initViews() {
        tvTitle = findViewById(R.id.tvTitle);
        tvCardText = findViewById(R.id.tvCardText);
        tvProgress = findViewById(R.id.tvProgress);
        imgCard = findViewById(R.id.imgCard);
        card = findViewById(R.id.card);
        btnNext = findViewById(R.id.btnNext);
        btnPrev = findViewById(R.id.btnPrev);
        btnPlaySound = findViewById(R.id.btnPlaySound);
        btnStartGame = findViewById(R.id.btnStartGame);
    }

    private void loadCardsFromFirebase() {
        DatabaseService.getInstance().loadFlashCards(subject, new DatabaseService.DatabaseCallback<DataSnapshot>() {
            @Override
            public void onCompleted(DataSnapshot snapshot) {
                cards = new ArrayList<>();
                for (DataSnapshot ds : snapshot.getChildren()) {
                    FlashCard flashCard = ds.getValue(FlashCard.class);
                    if (flashCard != null) cards.add(flashCard);
                }

                if (cards.isEmpty()) {
                    Toast.makeText(FlashCardMain.this, "לא נמצאו כרטיסים לנושא זה", Toast.LENGTH_SHORT).show();
                    finish();
                    return;
                }

                updateTitle();
                showImage();
                setupClickListeners();
            }

            @Override
            public void onFailed(Exception e) {
                Toast.makeText(FlashCardMain.this, "שגיאה בטעינה", Toast.LENGTH_SHORT).show();
                finish();
            }
        });
    }

    private void setupClickListeners() {
        card.setOnClickListener(v -> flipCard());

        btnNext.setOnClickListener(v -> {
            currentIndex = (currentIndex + 1) % cards.size();
            if (currentIndex == cards.size() - 1) {
                btnStartGame.setVisibility(View.VISIBLE);
            }
            showingImage = true;
            showImage();
        });

        btnPrev.setOnClickListener(v -> {
            currentIndex = (currentIndex - 1 + cards.size()) % cards.size();
            showingImage = true;
            showImage();
        });

        btnStartGame.setOnClickListener(v -> handleStartGame());

        btnPlaySound.setOnClickListener(v -> speakWord(cards.get(currentIndex).getAnswer()));

        MaterialButton btnBackToSubjects = findViewById(R.id.btnBackToSubjects);
        btnBackToSubjects.setOnClickListener(v -> showBackConfirmationDialog());
    }

    private void updateTitle() {
        switch (subject) {
            case "animals": tvTitle.setText("חיות"); break;
            case "colors": tvTitle.setText("צבעים"); break;
            case "numbers": tvTitle.setText("מספרים"); break;
            case "letters": tvTitle.setText("אותיות"); break;
            case "shapes": tvTitle.setText("צורות"); break;
            case "bodyparts": tvTitle.setText("חלקי גוף"); break;
        }
        updateTitleColor();
    }

    private void updateTitleColor() {
        String colorHex = SUBJECT_COLORS.get(subject);
        if (colorHex != null) {
            tvTitle.setTextColor(Color.parseColor(colorHex));
            tvTitle.setShadowLayer(8, 3, 3, darkenColor(colorHex, 0.5f));
        }
    }

    private int darkenColor(String hexColor, float factor) {
        int color = Color.parseColor(hexColor);
        int r = (int) (Color.red(color) * factor);
        int g = (int) (Color.green(color) * factor);
        int b = (int) (Color.blue(color) * factor);
        return Color.rgb(r, g, b);
    }

    private void showImage() {
        imgCard.setVisibility(View.VISIBLE);
        tvCardText.setVisibility(View.GONE);
        btnPlaySound.setVisibility(View.GONE);
        String imageName = cards.get(currentIndex).getImageResId();
        int resId = getResources().getIdentifier(imageName, "drawable", getPackageName());
        imgCard.setImageResource(resId != 0 ? resId : R.drawable.logo);
        updateProgress();
    }

    private void showText() {
        tvCardText.setVisibility(View.VISIBLE);
        imgCard.setVisibility(View.GONE);
        btnPlaySound.setVisibility(View.VISIBLE);
        tvCardText.setText(cards.get(currentIndex).getAnswer());
        updateProgress();
    }

    private void updateProgress() {
        if (tvProgress != null && cards != null) {
            tvProgress.setText(String.format(Locale.getDefault(), "%d / %d", currentIndex + 1, cards.size()));
        }
    }

    private void flipCard() {
        if (showingImage) showText();
        else showImage();
        showingImage = !showingImage;
    }

    private void handleStartGame() {
        getSharedPreferences("flashcard_prefs", MODE_PRIVATE)
                .edit()
                .putBoolean("seen_" + currentChild.getId() + "_" + subject, true)
                .apply();

        long timeSpent = (System.currentTimeMillis() - startTime) / 1000;
        UserParent user = SharedPreferencesUtil.getUser(this);
        if (user == null) return;

        DatabaseService.getInstance().updateDetailedProgress(
                user.getId(),
                currentChild.getId(),
                currentChild.getAgeGroup(),
                subject,
                0,
                timeSpent,
                15,
                0
        );

        Intent intent = new Intent(this, MixedGameActivity.class);
        intent.putExtra("subject", subject);
        startActivity(intent);
        finish();
    }

    private void showBackConfirmationDialog() {
        final android.app.Dialog dialog = new android.app.Dialog(this);
        dialog.setContentView(R.layout.custom_action_dialog);
        if (dialog.getWindow() != null)
            dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);

        TextView tvTitle = dialog.findViewById(R.id.tvDialogTitle);
        TextView tvMessage = dialog.findViewById(R.id.tvDialogMessage);
        MaterialButton btnConfirm = dialog.findViewById(R.id.btnConfirm);
        TextView btnCancel = dialog.findViewById(R.id.btnCancel);

        tvTitle.setText("לחזור לבחירת נושא?");
        tvMessage.setText("ההתקדמות שלך תישמר");
        btnConfirm.setText("כן, חזור");
        btnCancel.setText("לא, הישאר");

        btnConfirm.setOnClickListener(v -> {
            dialog.dismiss();
            Intent intent = new Intent(FlashCardMain.this, SelectSubjectActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
            startActivity(intent);
            finish();
        });

        btnCancel.setOnClickListener(v -> dialog.dismiss());
        dialog.show();
    }

    private void initializeTTS() {
        tts = new TextToSpeech(this, status -> {
            if (status == TextToSpeech.SUCCESS) {
                int result = tts.setLanguage(new Locale("he", "IL"));
                ttsReady = (result != TextToSpeech.LANG_MISSING_DATA
                        && result != TextToSpeech.LANG_NOT_SUPPORTED);
            }
        });
    }

    private void speakWord(String word) {
        if (ttsReady) tts.speak(word, TextToSpeech.QUEUE_FLUSH, null, null);
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