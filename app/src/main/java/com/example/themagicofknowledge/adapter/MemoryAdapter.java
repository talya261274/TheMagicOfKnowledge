package com.example.themagicofknowledge.adapter;

import android.content.Context;
import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TextView;

import com.example.themagicofknowledge.R;
import com.example.themagicofknowledge.models.MemoryCard;

import java.util.List;

public class MemoryAdapter extends BaseAdapter {

    private Context context;
    private List<MemoryCard> cards;
    private int cardSize;

    public MemoryAdapter(Context context, List<MemoryCard> cards, int cardSize) {
        this.context = context;
        this.cards = cards;
        this.cardSize = cardSize;
    }

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
        MemoryCard card = cards.get(position);

        if (convertView == null) {
            convertView = LayoutInflater.from(context).inflate(R.layout.item_card, parent, false);
        }

        // הגדרת גודל ישירות על convertView
        ViewGroup.LayoutParams params = convertView.getLayoutParams();
        if (params == null) {
            params = new ViewGroup.LayoutParams(cardSize, cardSize);
        } else {
            params.width = cardSize;
            params.height = cardSize;
        }
        convertView.setLayoutParams(params);

        FrameLayout cardBackground = convertView.findViewById(R.id.cardBackground);
        TextView ivCardBack = convertView.findViewById(R.id.ivCardBack);
        ImageView imageView = convertView.findViewById(R.id.cardImage);
        TextView textView = convertView.findViewById(R.id.cardText);

        androidx.cardview.widget.CardView cardView =
                (androidx.cardview.widget.CardView) convertView;


        if (card.isMatched) {
            cardView.setCardBackgroundColor(Color.parseColor("#C8F5C8"));
            if (card.imageRes != 0) {
                imageView.setVisibility(View.VISIBLE);
                imageView.setImageResource(card.imageRes);
                imageView.setBackgroundColor(Color.parseColor("#C8F5C8"));
                textView.setVisibility(View.GONE);
            } else {
                textView.setVisibility(View.VISIBLE);
                textView.setText(card.text);
                textView.setBackgroundColor(Color.parseColor("#C8F5C8"));
                imageView.setVisibility(View.GONE);
            }
        } else if (card.isFlipped) {
            cardView.setCardBackgroundColor(Color.WHITE);
            imageView.setBackgroundColor(Color.WHITE);
            textView.setBackgroundColor(Color.WHITE);
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
            cardView.setCardBackgroundColor(Color.TRANSPARENT);
            ivCardBack.setVisibility(View.VISIBLE);
            imageView.setVisibility(View.GONE);
            textView.setVisibility(View.GONE);
            imageView.setBackgroundColor(Color.TRANSPARENT);
            textView.setBackgroundColor(Color.TRANSPARENT);
        }

        return convertView;
    }
}