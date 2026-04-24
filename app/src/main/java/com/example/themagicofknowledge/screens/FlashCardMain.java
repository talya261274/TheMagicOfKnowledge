package com.example.themagicofknowledge.screens;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.os.Bundle;
import android.speech.tts.TextToSpeech;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;

import com.example.themagicofknowledge.R;
import com.example.themagicofknowledge.models.FlashCard;
import com.example.themagicofknowledge.models.UserChild;
import com.example.themagicofknowledge.services.DatabaseService;
import com.example.themagicofknowledge.utils.SharedPreferencesUtil;

import java.util.ArrayList;
import java.util.Locale;

public class FlashCardMain extends AppCompatActivity {

    private TextView tvTitle, tvCardText;
    private ImageView imgCard;
    private CardView card;
    private Button btnStartGame;
    private ImageButton btnNext, btnPrev, btnPlaySound;

    private UserChild currentChild;
    private String subject;

    private ArrayList<FlashCard> cards;
    private int currentIndex = 0;
    private boolean showingImage = true;

    private TextToSpeech tts;
    private boolean ttsReady = false;

    private long startTime;

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

        startTime = System.currentTimeMillis(); // התחלת מדידת זמן

        initViews();
        initializeTTS();
        createCards(subject);
        showImage();

        card.setOnClickListener(v -> flipCard());

        btnNext.setOnClickListener(v -> {
            currentIndex = (currentIndex + 1) % cards.size();
            // אם הגיע לכרטיס האחרון - חשיפת כפתור המשחק
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
    }

    private void initViews() {
        tvTitle = findViewById(R.id.tvTitle);
        tvCardText = findViewById(R.id.tvCardText);
        imgCard = findViewById(R.id.imgCard);
        card = findViewById(R.id.card);
        btnNext = findViewById(R.id.btnNext);
        btnPrev = findViewById(R.id.btnPrev);
        btnPlaySound = findViewById(R.id.btnPlaySound);
        btnStartGame = findViewById(R.id.btnStartGame);
    }

    private void handleStartGame() {
        long timeSpent = (System.currentTimeMillis() - startTime) / 1000;
        String parentId = SharedPreferencesUtil.getUser(this).getId();

        DatabaseService.getInstance().updateDetailedProgress(
                parentId,
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


    private void initializeTTS() {
        tts = new TextToSpeech(this, status -> {
            if (status == TextToSpeech.SUCCESS) {
                int result = tts.setLanguage(new Locale("he", "IL"));
                ttsReady = (result != TextToSpeech.LANG_MISSING_DATA && result != TextToSpeech.LANG_NOT_SUPPORTED);
            }
        });
    }

    private void speakWord(String word) {
        if (ttsReady) {
            tts.speak(word, TextToSpeech.QUEUE_FLUSH, null, null);
        }
    }

    private void createCards(String subject) {
        cards = new ArrayList<>();
        if (subject == null) return;

        switch (subject) {
            case "animals":
                tvTitle.setText("חיות");
                cards.add(new FlashCard(R.drawable.ss_animals_gir, "גירפה"));
                cards.add(new FlashCard(R.drawable.ss_animals_dog, "כלב"));
                cards.add(new FlashCard(R.drawable.ss_animals_el, "פיל"));
                cards.add(new FlashCard(R.drawable.ss_animals_cat, "חתול"));
                cards.add(new FlashCard(R.drawable.ss_animals_lion, "אריה"));
                cards.add(new FlashCard(R.drawable.ss_animals_tiger, "נמר"));
                cards.add(new FlashCard(R.drawable.ss_animals_bear, "דוב"));
                cards.add(new FlashCard(R.drawable.ss_animals_monkey, "קוף"));
                cards.add(new FlashCard(R.drawable.ss_animals_zebra, "זברה"));
                cards.add(new FlashCard(R.drawable.ss_animals_horse, "סוס"));
                cards.add(new FlashCard(R.drawable.ss_animals_donkey, "חמור"));
                cards.add(new FlashCard(R.drawable.ss_animals_cow, "פרה"));
                cards.add(new FlashCard(R.drawable.ss_animals_sheep, "כבשה"));
                cards.add(new FlashCard(R.drawable.ss_animals_snake, "נחש"));
                cards.add(new FlashCard(R.drawable.ss_animals_pig, "חזיר"));
                cards.add(new FlashCard(R.drawable.ss_animals_chicken, "תרנגול"));
                cards.add(new FlashCard(R.drawable.ss_animals_duck, "ברווז"));
                cards.add(new FlashCard(R.drawable.ss_animals_fish, "דג"));
                cards.add(new FlashCard(R.drawable.ss_animals_turtle, "צב"));
                cards.add(new FlashCard(R.drawable.ss_animals_rabbit, "ארנב"));
                cards.add(new FlashCard(R.drawable.ss_animals_frog, "צפרדע"));
                break;

            case "colors":
                tvTitle.setText("צבעים");
                cards.add(new FlashCard(R.drawable.ss_colors_red, "אדום"));
                cards.add(new FlashCard(R.drawable.ss_colors_blue, "כחול"));
                cards.add(new FlashCard(R.drawable.ss_colors_green, "ירוק"));
                cards.add(new FlashCard(R.drawable.ss_colors_yellow, "צהוב"));
                cards.add(new FlashCard(R.drawable.ss_colors_orange, "כתום"));
                cards.add(new FlashCard(R.drawable.ss_colors_purple, "סגול"));
                cards.add(new FlashCard(R.drawable.ss_colors_pink, "ורוד"));
                cards.add(new FlashCard(R.drawable.ss_colors_black, "שחור"));
                cards.add(new FlashCard(R.drawable.ss_colors_white, "לבן"));
                cards.add(new FlashCard(R.drawable.ss_colors_gray, "אפור"));
                cards.add(new FlashCard(R.drawable.ss_colors_brown, "חום"));
                cards.add(new FlashCard(R.drawable.ss_colors_light_blue, "תכלת"));
                cards.add(new FlashCard(R.drawable.ss_colors_dark_green, "ירוק כהה"));
                cards.add(new FlashCard(R.drawable.ss_colors_gold, "זהב"));
                cards.add(new FlashCard(R.drawable.ss_colors_silver, "כסף"));
                cards.add(new FlashCard(R.drawable.ss_colors_beige, "בז׳"));
                cards.add(new FlashCard(R.drawable.ss_colors_turquoise, "טורקיז"));
                break;

            case "numbers":
                tvTitle.setText("מספרים");
                cards.add(new FlashCard(R.drawable.ss_numbers_1, "אחת"));
                cards.add(new FlashCard(R.drawable.ss_numbers_2, "שתיים"));
                cards.add(new FlashCard(R.drawable.ss_numbers_3, "שלוש"));
                cards.add(new FlashCard(R.drawable.ss_numbers_4, "ארבע"));
                cards.add(new FlashCard(R.drawable.ss_numbers_5, "חמש"));
                cards.add(new FlashCard(R.drawable.ss_numbers_6, "שש"));
                cards.add(new FlashCard(R.drawable.ss_numbers_7, "שבע"));
                cards.add(new FlashCard(R.drawable.ss_numbers_8, "שמונה"));
                cards.add(new FlashCard(R.drawable.ss_numbers_9, "תשע"));
                cards.add(new FlashCard(R.drawable.ss_numbers_10, "עשר"));
                cards.add(new FlashCard(R.drawable.ss_numbers_20, "עשרים"));
                cards.add(new FlashCard(R.drawable.ss_numbers_30, "שלושים"));
                cards.add(new FlashCard(R.drawable.ss_numbers_40, "ארבעים"));
                cards.add(new FlashCard(R.drawable.ss_numbers_50, "חמישים"));
                cards.add(new FlashCard(R.drawable.ss_numbers_60, "שישים"));
                cards.add(new FlashCard(R.drawable.ss_numbers_70, "שבעים"));
                cards.add(new FlashCard(R.drawable.ss_numbers_80, "שמונים"));
                cards.add(new FlashCard(R.drawable.ss_numbers_90, "תשעים"));
                cards.add(new FlashCard(R.drawable.ss_numbers_100, "מאה"));
                break;

            case "letters":
                tvTitle.setText("אותיות");
                cards.add(new FlashCard(R.drawable.ss_letters_a, "אלף"));
                cards.add(new FlashCard(R.drawable.ss_letters_b, "בית"));
                cards.add(new FlashCard(R.drawable.ss_letters_g, "גימל"));
                cards.add(new FlashCard(R.drawable.ss_letters_d, "דלת"));
                cards.add(new FlashCard(R.drawable.ss_letters_h, "הא"));
                cards.add(new FlashCard(R.drawable.ss_letters_v, "ויו"));
                cards.add(new FlashCard(R.drawable.ss_letters_z, "זין"));
                cards.add(new FlashCard(R.drawable.ss_letters_ch, "חית"));
                cards.add(new FlashCard(R.drawable.ss_letters_t, "טית"));
                cards.add(new FlashCard(R.drawable.ss_letters_y, "יוד"));
                cards.add(new FlashCard(R.drawable.ss_letters_k, "כף"));
                cards.add(new FlashCard(R.drawable.ss_letters_l, "למד"));
                cards.add(new FlashCard(R.drawable.ss_letters_m, "מם"));
                cards.add(new FlashCard(R.drawable.ss_letters_n, "נון"));
                cards.add(new FlashCard(R.drawable.ss_letters_s, "סמך"));
                cards.add(new FlashCard(R.drawable.ss_letters_ayin, "עין"));
                cards.add(new FlashCard(R.drawable.ss_letters_p, "פא"));
                cards.add(new FlashCard(R.drawable.ss_letters_ts, "צדי"));
                cards.add(new FlashCard(R.drawable.ss_letters_kof, "קוף"));
                cards.add(new FlashCard(R.drawable.ss_letters_r, "ריש"));
                cards.add(new FlashCard(R.drawable.ss_letters_sh, "שין"));
                cards.add(new FlashCard(R.drawable.ss_letters_tav, "תיו"));
                break;

            case "shapes":
                tvTitle.setText("צורות");
                cards.add(new FlashCard(R.drawable.ss_shapes_circle, "עיגול"));
                cards.add(new FlashCard(R.drawable.ss_shapes_square, "ריבוע"));
                cards.add(new FlashCard(R.drawable.ss_shapes_triangle, "משולש"));
                cards.add(new FlashCard(R.drawable.ss_shapes_rectangle, "מלבן"));
                cards.add(new FlashCard(R.drawable.ss_shapes_oval, "אליפסה"));
                cards.add(new FlashCard(R.drawable.ss_shapes_diamond, "מעוין"));
                cards.add(new FlashCard(R.drawable.ss_shapes_parallelogram, "מקבילית"));
                cards.add(new FlashCard(R.drawable.ss_shapes_trapezoid, "טרפז"));
                cards.add(new FlashCard(R.drawable.ss_shapes_diamond1, "דלתון"));
                cards.add(new FlashCard(R.drawable.ss_shapes_pentagon, "מחומש"));
                cards.add(new FlashCard(R.drawable.ss_shapes_hexagon, "משושה"));
                cards.add(new FlashCard(R.drawable.ss_shapes_heptagon, "משובע"));
                cards.add(new FlashCard(R.drawable.ss_shapes_octagon, "מתומן"));
                cards.add(new FlashCard(R.drawable.ss_shapes_nonagon, "מתושע"));
                cards.add(new FlashCard(R.drawable.ss_shapes_decagon, "מעושר"));
                cards.add(new FlashCard(R.drawable.ss_shapes_star, "כוכב"));
                cards.add(new FlashCard(R.drawable.ss_shapes_heart, "לב"));
                cards.add(new FlashCard(R.drawable.ss_shapes_crescent, "סהר"));
                cards.add(new FlashCard(R.drawable.ss_shapes_arrow, "חץ"));
                break;

            case "bodyparts":
                tvTitle.setText("חלקי גוף");
                cards.add(new FlashCard(R.drawable.ss_body_head, "ראש"));
                cards.add(new FlashCard(R.drawable.ss_body_hair, "שיער"));
                cards.add(new FlashCard(R.drawable.ss_body_eye, "עין"));
                cards.add(new FlashCard(R.drawable.ss_body_ear, "אוזן"));
                cards.add(new FlashCard(R.drawable.ss_body_nose, "אף"));
                cards.add(new FlashCard(R.drawable.ss_body_mouth, "פה"));
                cards.add(new FlashCard(R.drawable.ss_body_teeth, "שיניים"));
                cards.add(new FlashCard(R.drawable.ss_body_neck, "צוואר"));
                cards.add(new FlashCard(R.drawable.ss_body_shoulder, "כתף"));
                cards.add(new FlashCard(R.drawable.ss_body_chest, "חזה"));
                cards.add(new FlashCard(R.drawable.ss_body_stomach, "בטן"));
                cards.add(new FlashCard(R.drawable.ss_body_back, "גב"));
                cards.add(new FlashCard(R.drawable.ss_body_arm, "זרוע"));
                cards.add(new FlashCard(R.drawable.ss_body_elbow, "מרפק"));
                cards.add(new FlashCard(R.drawable.ss_body_hand, "יד"));
                cards.add(new FlashCard(R.drawable.ss_body_finger, "אצבע"));
                cards.add(new FlashCard(R.drawable.ss_body_leg, "רגל"));
                cards.add(new FlashCard(R.drawable.ss_body_knee, "ברך"));
                cards.add(new FlashCard(R.drawable.ss_body_foot, "כף רגל"));
                break;
        }
    }

    private void showImage() {
        imgCard.setVisibility(ImageView.VISIBLE);
        tvCardText.setVisibility(TextView.GONE);
        btnPlaySound.setVisibility(Button.GONE);
        imgCard.setImageResource(cards.get(currentIndex).getImageResId());
    }

    private void showText() {
        tvCardText.setVisibility(TextView.VISIBLE);
        imgCard.setVisibility(ImageView.GONE);
        btnPlaySound.setVisibility(Button.VISIBLE);
        tvCardText.setText(cards.get(currentIndex).getAnswer());
    }

    private void flipCard() {
        if (showingImage) showText();
        else showImage();
        showingImage = !showingImage;
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