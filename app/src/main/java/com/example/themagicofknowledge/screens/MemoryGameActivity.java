package com.example.themagicofknowledge.screens;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.DisplayMetrics;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.FrameLayout;
import android.widget.GridView;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.example.themagicofknowledge.R;
import com.example.themagicofknowledge.models.UserChild;
import com.example.themagicofknowledge.utils.SharedPreferencesUtil;

import java.util.ArrayList;
import java.util.Collections;

public class MemoryGameActivity extends AppCompatActivity {

    private GridView gridView;
    private ArrayList<Card> cards;
    private CardAdapter adapter;
    private ProgressBar testProgress;

    private Card firstSelected = null;
    private Card secondSelected = null;
    private boolean isBusy = false;

    private int attemptsFromBefore;
    private long timeFromBefore;
    private String subject;
    private UserChild currentChild;
    private int currentAttempts = 0;
    private long startTime;

    private int cardSize;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_memory_game);

        testProgress = findViewById(R.id.testProgress);
        if (testProgress != null) {
            testProgress.setMax(3);
            testProgress.setProgress(3);
        }

        attemptsFromBefore = getIntent().getIntExtra("totalAttempts", 0);
        timeFromBefore = getIntent().getLongExtra("totalTime", 0);
        subject = getIntent().getStringExtra("subject");
        if (subject == null) subject = "general";

        currentChild = SharedPreferencesUtil.getCurrentChild(this);
        startTime = System.currentTimeMillis();

        gridView = findViewById(R.id.gridViewCards);

        DisplayMetrics metrics = getResources().getDisplayMetrics();

        // חישוב רוחב זמין: רוחב המסך פחות ה-Padding של ה-Layout (24dp)
        int screenWidth = metrics.widthPixels - (int)(24 * metrics.density);

        // חישוב רווחים בין קלפים: יש לנו 3 עמודות, אז יש 2 רווחים של 12dp ביניהן
        int totalSpacing = (int)(24 * metrics.density);

        // הגודל הסופי של כל כרטיס
        cardSize = (screenWidth - totalSpacing) / 3;

        loadCards(); // טען את כל הזוגות
        adapter = new CardAdapter();
        gridView.setAdapter(adapter);

        gridView.setOnItemClickListener((parent, view, position, id) -> {
            if (isBusy) return;

            Card selected = cards.get(position);
            if (selected.isMatched || selected.isFlipped) return;

            selected.isFlipped = true;
            adapter.notifyDataSetChanged();

            if (firstSelected == null) {
                firstSelected = selected;
            } else {
                secondSelected = selected;
                checkMatch();
            }
        });
    }

    // טען את הכרטיסים – תמונות ומילים
    private void loadCards() {
        cards = new ArrayList<>();

        // רמה 3–4 דוגמא – חיות
        cards.add(new Card(R.drawable.ss_animals_dog, null, "כלב"));
        cards.add(new Card(0, "כלב", "כלב"));
        cards.add(new Card(R.drawable.ss_animals_cat, null, "חתול"));
        cards.add(new Card(0, "חתול", "חתול"));
        cards.add(new Card(R.drawable.ss_animals_gir, null, "גירפה"));
        cards.add(new Card(0, "גירפה", "גירפה"));

        // רמה 3–4 דוגמא – צבעים
        cards.add(new Card(R.drawable.ss_colors_red, null, "אדום"));
        cards.add(new Card(0, "אדום", "אדום"));
        cards.add(new Card(R.drawable.ss_colors_blue, null, "כחול"));
        cards.add(new Card(0, "כחול", "כחול"));
        cards.add(new Card(R.drawable.ss_colors_pink, null, "ורוד"));
        cards.add(new Card(0, "ורוד", "ורוד"));

        Collections.shuffle(cards); // ערבוב הכרטיסיות
    }

    private void checkMatch() {
        isBusy = true;
        new Handler().postDelayed(() -> {
            if (firstSelected.matchId.equals(secondSelected.matchId)) {
                firstSelected.isMatched = true;
                secondSelected.isMatched = true;
            } else {
                firstSelected.isFlipped = false;
                secondSelected.isFlipped = false;
                currentAttempts++;
            }

            firstSelected = null;
            secondSelected = null;
            adapter.notifyDataSetChanged();
            isBusy = false;

            // בדיקה אם המשחק נגמר
            boolean allMatched = true;
            for (Card c : cards) {
                if (!c.isMatched) {
                    allMatched = false;
                    break;
                }
            }

            if (allMatched) {
                Toast.makeText(MemoryGameActivity.this, "כל הזוגות נמצאו! כל הכבוד!", Toast.LENGTH_LONG).show();
                new Handler(android.os.Looper.getMainLooper()).postDelayed(this::finishGame, 1500);
            }
        }, 800);
    }

    private void finishGame() {
        long currentTimeSeconds = (System.currentTimeMillis() - startTime) / 1000;

        int finalTotalAttempts = attemptsFromBefore + currentAttempts;
        long finalTotalTime = timeFromBefore + currentTimeSeconds;

        com.example.themagicofknowledge.models.GameProgress finalProgress =
                new com.example.themagicofknowledge.models.GameProgress(finalTotalAttempts, true, finalTotalTime);

        com.example.themagicofknowledge.services.DatabaseService.getInstance().updateChildGameProgress(
                currentChild.getParentId(),
                currentChild.getId(),
                currentChild.getAgeGroup(),
                subject,
                finalProgress,
                null
        );

        Intent intent = new Intent(this, GameResultActivity.class);
        intent.putExtra("success", true);
        intent.putExtra("subject", subject);
        intent.putExtra("totalAttempts", finalTotalAttempts);
        intent.putExtra("totalTime", finalTotalTime);

        startActivity(intent);
        finish();
    }

    // מחלקת כרטיס
    private class Card {
        int imageRes;     // אם 0 → כרטיס מילה
        String text;      // מילה, או null אם כרטיס תמונה
        String matchId;   // מזהה ייחודי לזוג
        boolean isFlipped = false;
        boolean isMatched = false;

        Card(int imageRes, String text, String matchId) {
            this.imageRes = imageRes;
            this.text = text;
            this.matchId = matchId;
        }
    }

    // Adapter להצגת הכרטיסים
    private class CardAdapter extends BaseAdapter {

        @Override
        public int getCount() {
            return cards.size();
        }

        @Override
        public Object getItem(int position) {
            return cards.get(position);
        }

        @Override
        public long getItemId(int position) {
            return position;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            Card card = cards.get(position);

            if (convertView == null) {
                convertView = getLayoutInflater().inflate(R.layout.card_item, parent, false);
            }

            // הגדרת גודל ריבועי
            View cardContainer = convertView.findViewById(R.id.cardContainer);
            ViewGroup.LayoutParams params = cardContainer.getLayoutParams();
            params.width = cardSize;
            params.height = cardSize;
            cardContainer.setLayoutParams(params);

            FrameLayout cardBackground = convertView.findViewById(R.id.cardBackground);
            ImageView ivCardBack = convertView.findViewById(R.id.ivCardBack);
            ImageView imageView = convertView.findViewById(R.id.cardImage);
            TextView textView = convertView.findViewById(R.id.cardText);

            if (card.isFlipped || card.isMatched) {
                // מצב פתוח:
                ivCardBack.setVisibility(View.GONE); // מעלימים את הגב לחלוטין!
                cardBackground.setBackgroundColor(android.graphics.Color.WHITE); // מוודאים שהרקע לבן חלק

                if (card.imageRes != 0) {
                    imageView.setVisibility(View.VISIBLE);
                    imageView.setImageResource(card.imageRes);
                    textView.setVisibility(View.GONE);
                } else {
                    textView.setVisibility(View.VISIBLE);
                    textView.setText(card.text);
                    imageView.setVisibility(View.GONE);
                }
            } else {
                // מצב סגור:
                ivCardBack.setVisibility(View.VISIBLE); // מראים רק את הגב
                imageView.setVisibility(View.GONE);
                textView.setVisibility(View.GONE);
                // אפשר להגדיר כאן צבע רקע שקוף כדי שהגב יבלוט
                cardBackground.setBackgroundColor(android.graphics.Color.TRANSPARENT);
            }

            return convertView;
        }

    }
}
