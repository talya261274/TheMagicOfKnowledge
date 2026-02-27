package com.example.themagicofknowledge.screens;

import android.os.Bundle;
import android.os.Handler;
import android.util.DisplayMetrics;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.FrameLayout;
import android.widget.GridView;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.example.themagicofknowledge.R;

import java.util.ArrayList;
import java.util.Collections;

public class MatchingGameActivity extends AppCompatActivity {

    private GridView gridView;
    private ArrayList<Card> cards;
    private CardAdapter adapter;

    private Card firstSelected = null;
    private Card secondSelected = null;
    private boolean isBusy = false;

    private int cardSize; // גודל דינמי לכרטיסים

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_matching_game);

        gridView = findViewById(R.id.gridViewCards);

        // קבע גודל כרטיסים דינמי לפי רוחב המסך
        DisplayMetrics metrics = getResources().getDisplayMetrics();
        int screenWidth = metrics.widthPixels - 64; // margin padding
        cardSize = screenWidth / 3; // 3 עמודות

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
                Toast.makeText(MatchingGameActivity.this, "כל הזוגות נמצאו! כל הכבוד!", Toast.LENGTH_LONG).show();
            }
        }, 800); // 0.8 שניות, מהיר יותר לילדים
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

            FrameLayout background = convertView.findViewById(R.id.cardBackground);
            ViewGroup.LayoutParams params = background.getLayoutParams();
            params.width = cardSize;
            params.height = cardSize;
            background.setLayoutParams(params);
            ImageView imageView = convertView.findViewById(R.id.cardImage);
            TextView textView = convertView.findViewById(R.id.cardText);

            // הצגת כרטיס בהתאם לסוגו
            if (card.imageRes != 0) {
                imageView.setVisibility(card.isFlipped || card.isMatched ? View.VISIBLE : View.GONE);
                imageView.setImageResource(card.imageRes);
                textView.setVisibility(View.GONE);
            } else {
                textView.setVisibility(card.isFlipped || card.isMatched ? View.VISIBLE : View.GONE);
                textView.setText(card.text);
                imageView.setVisibility(View.GONE);
            }

            return convertView;
        }

    }
}
