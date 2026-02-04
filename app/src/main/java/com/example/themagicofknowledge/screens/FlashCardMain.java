package com.example.themagicofknowledge.screens;

import android.annotation.SuppressLint;
import android.os.Bundle;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.media.MediaPlayer;
import android.media.AudioManager;



import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;

import com.example.themagicofknowledge.R;
import com.example.themagicofknowledge.models.FlashCard;

import java.util.ArrayList;

public class FlashCardMain extends AppCompatActivity {

    private TextView tvTitle, tvCardText;
    private ImageView imgCard;
    private CardView card;
    private ImageButton btnNext, btnPrev, btnPlaySound;
    ;

    private ArrayList<FlashCard> cards;
    private int currentIndex = 0;
    private boolean showingImage = true;

    @SuppressLint("MissingInflatedId")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_flash_card_main);

        // חיבור רכיבים
        tvTitle = findViewById(R.id.tvTitle);  // כותרת עליונה
        tvCardText = findViewById(R.id.tvCardText);
        imgCard = findViewById(R.id.imgCard);
        card = findViewById(R.id.card);
        btnNext = findViewById(R.id.btnNext);
        btnPrev = findViewById(R.id.btnPrev);
        btnPlaySound = findViewById(R.id.btnPlaySound);

        // קבלת נושא מה-Intent
        String subject = getIntent().getStringExtra("subject");
        if (subject != null) {
            tvTitle.setText(subject);
        }

        // יצירת הכרטיסים לפי הנושא
        createCards(subject);

        // הצגת הכרטיס הראשון
        showImage();

        // היפוך כרטיס בלחיצה
        card.setOnClickListener(v -> flipCard());

        // כפתורי ניווט
        btnNext.setOnClickListener(v -> {
            currentIndex = (currentIndex + 1) % cards.size();
            showingImage = true;
            showImage();
        });

        btnPrev.setOnClickListener(v -> {
            currentIndex = (currentIndex - 1 + cards.size()) % cards.size();
            showingImage = true;
            showImage();
        });

        btnPlaySound.setOnClickListener(v -> playSound());

    }

    private void createCards(String subject) {
        cards = new ArrayList<>();
        if (subject == null) return;

        switch (subject) {
            case "animals":
                tvTitle.setText("חיות");
                cards.add(new FlashCard(R.drawable.ss_animals_gir, "גירפה" , R.raw.gir));
                cards.add(new FlashCard(R.drawable.ss_animals_dog, "כלב", R.raw.dog));
                cards.add(new FlashCard(R.drawable.ss_animals_el, "פיל", R.raw.elephant));
                cards.add(new FlashCard(R.drawable.ss_animals_cat, "חתול", R.raw.cat));
                cards.add(new FlashCard(R.drawable.ss_animals_lion, "אריה", R.raw.lion));
                cards.add(new FlashCard(R.drawable.ss_animals_tiger, "נמר", R.raw.tiger));
                cards.add(new FlashCard(R.drawable.ss_animals_bear, "דוב", R.raw.bear));
                cards.add(new FlashCard(R.drawable.ss_animals_monkey, "קוף", R.raw.monkey));
                cards.add(new FlashCard(R.drawable.ss_animals_zebra, "זברה", R.raw.zebra));
                cards.add(new FlashCard(R.drawable.ss_animals_horse, "סוס", R.raw.horse));
                cards.add(new FlashCard(R.drawable.ss_animals_donkey, "חמור", R.raw.donkey));
                cards.add(new FlashCard(R.drawable.ss_animals_cow, "פרה", R.raw.cow));
                cards.add(new FlashCard(R.drawable.ss_animals_sheep, "כבשה", R.raw.sheep));
                cards.add(new FlashCard(R.drawable.ss_animals_snake, "נחש", R.raw.snake));
                cards.add(new FlashCard(R.drawable.ss_animals_pig, "חזיר", R.raw.pig));
                cards.add(new FlashCard(R.drawable.ss_animals_chicken, "תרנגול", R.raw.chicken));
                cards.add(new FlashCard(R.drawable.ss_animals_duck, "ברווז", R.raw.duck));
                cards.add(new FlashCard(R.drawable.ss_animals_fish, "דג", R.raw.fish));
                cards.add(new FlashCard(R.drawable.ss_animals_turtle, "צב", R.raw.turtle));
                cards.add(new FlashCard(R.drawable.ss_animals_rabbit, "ארנב", R.raw.rabbit));
                cards.add(new FlashCard(R.drawable.ss_animals_frog, "צפרדע", R.raw.frog));
                break;

            case "colors":
                tvTitle.setText("צבעים");
                cards.add(new FlashCard(R.drawable.ss_colors_red, "אדום", R.raw.red));
                cards.add(new FlashCard(R.drawable.ss_colors_blue, "כחול", R.raw.blue));
                cards.add(new FlashCard(R.drawable.ss_colors_green, "ירוק", R.raw.green));
                cards.add(new FlashCard(R.drawable.ss_colors_yellow, "צהוב", R.raw.yellow));
                cards.add(new FlashCard(R.drawable.ss_colors_orange, "כתום", R.raw.orange));
                cards.add(new FlashCard(R.drawable.ss_colors_purple, "סגול", R.raw.purple));
                cards.add(new FlashCard(R.drawable.ss_colors_pink, "ורוד", R.raw.pink));
                cards.add(new FlashCard(R.drawable.ss_colors_black, "שחור", R.raw.black));
                cards.add(new FlashCard(R.drawable.ss_colors_white, "לבן", R.raw.white));
                cards.add(new FlashCard(R.drawable.ss_colors_gray, "אפור", R.raw.gray));
                cards.add(new FlashCard(R.drawable.ss_colors_brown, "חום", R.raw.brown));
                cards.add(new FlashCard(R.drawable.ss_colors_light_blue, "תכלת", R.raw.light_blue));
                cards.add(new FlashCard(R.drawable.ss_colors_dark_green, "ירוק כהה", R.raw.dark_green));
                cards.add(new FlashCard(R.drawable.ss_colors_gold, "זהב", R.raw.gold));
                cards.add(new FlashCard(R.drawable.ss_colors_silver, "כסף", R.raw.silver));
                cards.add(new FlashCard(R.drawable.ss_colors_beige, "בז׳", R.raw.beige));
                cards.add(new FlashCard(R.drawable.ss_colors_turquoise, "טורקיז", R.raw.turquoise));

                break;

            case "numbers":
                tvTitle.setText("מספרים");
                cards.add(new FlashCard(R.drawable.ss_numbers_1, "אחת", R.raw.one));
                cards.add(new FlashCard(R.drawable.ss_numbers_2, "שתיים", R.raw.two));
                cards.add(new FlashCard(R.drawable.ss_numbers_3, "שלוש", R.raw.three));
                cards.add(new FlashCard(R.drawable.ss_numbers_4, "ארבע", R.raw.four));
                cards.add(new FlashCard(R.drawable.ss_numbers_5, "חמש", R.raw.five));
                cards.add(new FlashCard(R.drawable.ss_numbers_6, "שש", R.raw.six));
                cards.add(new FlashCard(R.drawable.ss_numbers_7, "שבע", R.raw.seven));
                cards.add(new FlashCard(R.drawable.ss_numbers_8, "שמונה", R.raw.eight));
                cards.add(new FlashCard(R.drawable.ss_numbers_9, "תשע", R.raw.nine));
                cards.add(new FlashCard(R.drawable.ss_numbers_10, "עשר", R.raw.ten));
                cards.add(new FlashCard(R.drawable.ss_numbers_20, "עשרים", R.raw.twenty));
                cards.add(new FlashCard(R.drawable.ss_numbers_30, "שלושים", R.raw.thirty));
                cards.add(new FlashCard(R.drawable.ss_numbers_40, "ארבעים", R.raw.forty));
                cards.add(new FlashCard(R.drawable.ss_numbers_50, "חמישים", R.raw.fifty));
                cards.add(new FlashCard(R.drawable.ss_numbers_60, "שישים", R.raw.sixty));
                cards.add(new FlashCard(R.drawable.ss_numbers_70, "שבעים", R.raw.seventy));
                cards.add(new FlashCard(R.drawable.ss_numbers_80, "שמונים", R.raw.eighty));
                cards.add(new FlashCard(R.drawable.ss_numbers_90, "תשעים", R.raw.ninety));
                cards.add(new FlashCard(R.drawable.ss_numbers_100, "מאה", R.raw.hundred));

                break;

            case "letters":
                tvTitle.setText("אותיות");
                cards.add(new FlashCard(R.drawable.ss_letters_a, "אלף", R.raw.alef));
                cards.add(new FlashCard(R.drawable.ss_letters_b, "בית", R.raw.bet));
                cards.add(new FlashCard(R.drawable.ss_letters_g, "גימל", R.raw.gimel));
                cards.add(new FlashCard(R.drawable.ss_letters_d, "דלת", R.raw.dalet));
                cards.add(new FlashCard(R.drawable.ss_letters_h, "הא", R.raw.he));
                cards.add(new FlashCard(R.drawable.ss_letters_v, "ויו", R.raw.vav));
                cards.add(new FlashCard(R.drawable.ss_letters_z, "זין", R.raw.zayin));
                cards.add(new FlashCard(R.drawable.ss_letters_ch, "חית", R.raw.chet));
                cards.add(new FlashCard(R.drawable.ss_letters_t, "טית", R.raw.tet));
                cards.add(new FlashCard(R.drawable.ss_letters_y, "יוד", R.raw.yod));
                cards.add(new FlashCard(R.drawable.ss_letters_k, "כף", R.raw.kaf));
                cards.add(new FlashCard(R.drawable.ss_letters_l, "למד", R.raw.lamed));
                cards.add(new FlashCard(R.drawable.ss_letters_m, "מם", R.raw.mem));
                cards.add(new FlashCard(R.drawable.ss_letters_n, "נון", R.raw.nun));
                cards.add(new FlashCard(R.drawable.ss_letters_s, "סמך", R.raw.samekh));
                cards.add(new FlashCard(R.drawable.ss_letters_ayin, "עין", R.raw.ayin));
                cards.add(new FlashCard(R.drawable.ss_letters_p, "פא", R.raw.pe));
                cards.add(new FlashCard(R.drawable.ss_letters_ts, "צדי", R.raw.tsadi));
                cards.add(new FlashCard(R.drawable.ss_letters_kof, "קוף", R.raw.kof));
                cards.add(new FlashCard(R.drawable.ss_letters_r, "ריש", R.raw.reish));
                cards.add(new FlashCard(R.drawable.ss_letters_sh, "שין", R.raw.shin));
                cards.add(new FlashCard(R.drawable.ss_letters_tav, "תיו", R.raw.tav));
                break;

            case "shapes":
                tvTitle.setText("צורות");
                cards.add(new FlashCard(R.drawable.ss_shapes_circle, "עיגול", R.raw.circle));
                cards.add(new FlashCard(R.drawable.ss_shapes_square, "ריבוע", R.raw.square));
                cards.add(new FlashCard(R.drawable.ss_shapes_triangle, "משולש", R.raw.triangle));
                cards.add(new FlashCard(R.drawable.ss_shapes_rectangle, "מלבן", R.raw.rectangle));
                cards.add(new FlashCard(R.drawable.ss_shapes_oval, "אליפסה", R.raw.oval));
                cards.add(new FlashCard(R.drawable.ss_shapes_diamond, "מעוין", R.raw.diamond));
                cards.add(new FlashCard(R.drawable.ss_shapes_parallelogram, "מקבילית", R.raw.parallelogram));
                cards.add(new FlashCard(R.drawable.ss_shapes_trapezoid, "טרפז", R.raw.trapezoid));
                cards.add(new FlashCard(R.drawable.ss_shapes_diamond1, "דלתון", R.raw.delton));
                cards.add(new FlashCard(R.drawable.ss_shapes_pentagon, "מחומש", R.raw.pentagon));
                cards.add(new FlashCard(R.drawable.ss_shapes_hexagon, "משושה", R.raw.hexagon));
                cards.add(new FlashCard(R.drawable.ss_shapes_heptagon, "משובע", R.raw.heptagon));
                cards.add(new FlashCard(R.drawable.ss_shapes_octagon, "מתומן", R.raw.octagon));
                cards.add(new FlashCard(R.drawable.ss_shapes_nonagon, "מתושע", R.raw.nonagon));
                cards.add(new FlashCard(R.drawable.ss_shapes_decagon, "מעושר", R.raw.decagon));
                cards.add(new FlashCard(R.drawable.ss_shapes_star, "כוכב", R.raw.star));
                cards.add(new FlashCard(R.drawable.ss_shapes_heart, "לב", R.raw.heart));
                cards.add(new FlashCard(R.drawable.ss_shapes_crescent, "סהר", R.raw.crescent));
                cards.add(new FlashCard(R.drawable.ss_shapes_arrow, "חץ", R.raw.arrow));

                break;

            case "bodyparts":
                tvTitle.setText("חלקי גוף");
                cards.add(new FlashCard(R.drawable.ss_body_head, "ראש", R.raw.head));
                cards.add(new FlashCard(R.drawable.ss_body_hair, "שיער", R.raw.hair));
                cards.add(new FlashCard(R.drawable.ss_body_eye, "עין", R.raw.eye));
                cards.add(new FlashCard(R.drawable.ss_body_ear, "אוזן", R.raw.ear));
                cards.add(new FlashCard(R.drawable.ss_body_nose, "אף", R.raw.nose));
                cards.add(new FlashCard(R.drawable.ss_body_mouth, "פה", R.raw.mouth));
                cards.add(new FlashCard(R.drawable.ss_body_teeth, "שיניים", R.raw.teeth));
                cards.add(new FlashCard(R.drawable.ss_body_neck, "צוואר", R.raw.neck));
                cards.add(new FlashCard(R.drawable.ss_body_shoulder, "כתף", R.raw.shoulder));
                cards.add(new FlashCard(R.drawable.ss_body_chest, "חזה", R.raw.chest));
                cards.add(new FlashCard(R.drawable.ss_body_stomach, "בטן", R.raw.stomach));
                cards.add(new FlashCard(R.drawable.ss_body_back, "גב", R.raw.back));
                cards.add(new FlashCard(R.drawable.ss_body_arm, "זרוע", R.raw.arm));
                cards.add(new FlashCard(R.drawable.ss_body_elbow, "מרפק", R.raw.elbow));
                cards.add(new FlashCard(R.drawable.ss_body_hand, "יד", R.raw.hand));
                cards.add(new FlashCard(R.drawable.ss_body_finger, "אצבע", R.raw.finger));
                cards.add(new FlashCard(R.drawable.ss_body_leg, "רגל", R.raw.leg));
                cards.add(new FlashCard(R.drawable.ss_body_knee, "ברך", R.raw.knee));
                cards.add(new FlashCard(R.drawable.ss_body_foot, "כף רגל", R.raw.foot));

                break;

        }

    }

    private void showImage() {
        imgCard.setVisibility(ImageView.VISIBLE);
        tvCardText.setVisibility(TextView.GONE);
        btnPlaySound.setVisibility(Button.GONE);  // הכפתור מוסתר
        imgCard.setImageResource(cards.get(currentIndex).getImageResId());
    }

    private void showText() {
        tvCardText.setVisibility(TextView.VISIBLE);
        imgCard.setVisibility(ImageView.GONE);
        btnPlaySound.setVisibility(Button.VISIBLE); // הכפתור מופיע
        tvCardText.setText(cards.get(currentIndex).getAnswer());
    }


    private void flipCard() {
        if (showingImage) {
            showText();
        } else {
            showImage();
        }
        showingImage = !showingImage;
    }

    private void playSound() {

        AudioManager audioManager = (AudioManager) getSystemService(AUDIO_SERVICE);
        audioManager.setStreamVolume(
                AudioManager.STREAM_MUSIC,
                audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC),
                0
        );

        MediaPlayer mp = MediaPlayer.create(this, cards.get(currentIndex).getSoundResId());

        mp.setVolume(2.0f, 2.0f); // שמאל, ימין (0.0 עד 1.0)

        mp.start();
        mp.setOnCompletionListener(MediaPlayer::release);
    }


}
