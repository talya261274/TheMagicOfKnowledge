package com.example.themagicofknowledge.screens;

import android.os.Bundle;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;

import com.example.themagicofknowledge.R;
import com.example.themagicofknowledge.models.FlashCard;

import java.util.ArrayList;

public class FlashCardMain extends AppCompatActivity {

    private TextView tvTitle, tvCardText;
    private ImageView imgCard;
    private CardView card;
    private Button btnNext, btnPrev;

    private ArrayList<FlashCard> cards;
    private int currentIndex = 0;
    private boolean showingImage = true;

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
    }

    private void createCards(String subject) {
        cards = new ArrayList<>();
        if (subject == null) return;

        switch (subject) {
            case "חיות":
                cards.add(new FlashCard(R.drawable.ss_animals_gir, "גירפה"));
                cards.add(new FlashCard(R.drawable.ss_animals_dog, "כלב"));
                cards.add(new FlashCard(R.drawable.ss_animals_el, "פיל"));
                break;
            case "צבעים":
                cards.add(new FlashCard(R.drawable.ss_colors_red, "אדום"));
                cards.add(new FlashCard(R.drawable.ss_colors_blue, "כחול"));
                cards.add(new FlashCard(R.drawable.ss_colors_green, "ירוק"));
                break;
            case "מספרים":
                cards.add(new FlashCard(R.drawable.ss_numbers_1, "אחת"));
                cards.add(new FlashCard(R.drawable.ss_numbers_2, "שתיים"));
                cards.add(new FlashCard(R.drawable.ss_numbers_3, "שלוש"));
                break;
            case "אותיות":
                cards.add(new FlashCard(R.drawable.ss_letters_a, "אלף"));
                cards.add(new FlashCard(R.drawable.ss_letters_b, "בית"));
                cards.add(new FlashCard(R.drawable.ss_letters_g, "גימל"));
                break;
            case "צורות":
                cards.add(new FlashCard(R.drawable.ss_shapes_circle, "עיגול"));
                cards.add(new FlashCard(R.drawable.ss_shapes_square, "ריבוע"));
                cards.add(new FlashCard(R.drawable.ss_shapes_triangle, "משולש"));
                break;
            case "חלקי גוף":
                cards.add(new FlashCard(R.drawable.ss_body_head, "ראש"));
                cards.add(new FlashCard(R.drawable.ss_body_hand, "יד"));
                cards.add(new FlashCard(R.drawable.ss_body_leg, "רגל"));
                break;
        }
    }

    private void showImage() {
        imgCard.setVisibility(ImageView.VISIBLE);
        tvCardText.setVisibility(TextView.GONE);
        imgCard.setImageResource(cards.get(currentIndex).getImageResId());
    }

    private void showText() {
        tvCardText.setVisibility(TextView.VISIBLE);
        imgCard.setVisibility(ImageView.GONE);
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
}
