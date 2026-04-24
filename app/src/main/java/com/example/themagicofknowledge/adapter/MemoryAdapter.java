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
            convertView = LayoutInflater.from(context).inflate(R.layout.card_item, parent, false);
        }

        // הגדרת גודל הכרטיס
        View cardContainer = convertView.findViewById(R.id.cardContainer);
        if (cardContainer != null) {
            ViewGroup.LayoutParams params = cardContainer.getLayoutParams();
            params.width = cardSize;
            params.height = cardSize;
            cardContainer.setLayoutParams(params);
        }

        FrameLayout cardBackground = convertView.findViewById(R.id.cardBackground);
        ImageView ivCardBack = convertView.findViewById(R.id.ivCardBack);
        ImageView imageView = convertView.findViewById(R.id.cardImage);
        TextView textView = convertView.findViewById(R.id.cardText);

        if (card.isFlipped || card.isMatched) {
            ivCardBack.setVisibility(View.GONE);
            cardBackground.setBackgroundColor(Color.WHITE);
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
            ivCardBack.setVisibility(View.VISIBLE);
            imageView.setVisibility(View.GONE);
            textView.setVisibility(View.GONE);
            cardBackground.setBackgroundColor(Color.TRANSPARENT);
        }

        return convertView;
    }
}