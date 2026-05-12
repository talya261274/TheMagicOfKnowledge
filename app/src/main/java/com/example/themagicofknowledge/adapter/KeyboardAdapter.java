package com.example.themagicofknowledge.adapter;

import android.content.Context;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.GridView;

import androidx.annotation.NonNull;

import com.example.themagicofknowledge.R;

public class KeyboardAdapter extends ArrayAdapter<String> {

    private final OnKeyClickListener listener;

    public KeyboardAdapter(Context context, String[] letters, OnKeyClickListener listener) {
        super(context, 0, letters);
        this.listener = listener;
    }

    @NonNull
    @Override
    public View getView(int position, View convertView, @NonNull ViewGroup parent) {
        Button btn = (Button) convertView;

        if (btn == null) {
            btn = new Button(getContext());

            // הגדרת גודל הכפתור
            GridView.LayoutParams params = new GridView.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT, 140);
            btn.setLayoutParams(params);

            btn.setTextSize(18);
            btn.setTextColor(Color.parseColor("#1E5F8B")); // כחול כמו שאר האפליקציה
            btn.setTypeface(android.graphics.Typeface.DEFAULT_BOLD);

            btn.setBackgroundResource(R.drawable.keyboard_key_selector);

            btn.setElevation(5); // הוספת צל קטן
        }

        String letter = getItem(position);
        btn.setText(letter);

        if (letter.equals("DEL")) {
            btn.setText("⌫"); // אייקון של מחיקה במקום טקסט
            btn.setBackgroundTintList(ColorStateList.valueOf(Color.parseColor("#FFCDD2"))); // צבע אדמדם עדין
        } else {
            // הרקע הרגיל של המקלדת
            btn.setBackgroundResource(R.drawable.keyboard_key_selector);
        }

        btn.setOnClickListener(v -> {
            if (listener != null) {
                listener.onKeyClick(letter);
            }
        });

        return btn;
    }

    // ממשק שמאפשר ל-Activity להגיב ללחיצות
    public interface OnKeyClickListener {
        void onKeyClick(String letter);
    }
}

