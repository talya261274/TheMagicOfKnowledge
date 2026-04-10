package com.example.themagicofknowledge.screens;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import com.example.themagicofknowledge.R;
import com.example.themagicofknowledge.models.UserChild;
import com.example.themagicofknowledge.utils.SharedPreferencesUtil;
import com.google.android.material.navigation.NavigationView;

public class MainActivity extends BaseActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main_content);

        // 1. מציאת הרכיבים במסך
        TextView tvWelcomeChild = findViewById(R.id.tvWelcomeChild);
        ImageView ivWelcomeAvatar = findViewById(R.id.ivWelcomeAvatar);
        Button btnStart = findViewById(R.id.btnStartJourney);

        // 2. שליפת נתוני הילד
        UserChild currentChild = SharedPreferencesUtil.getCurrentChild(this);

        if (currentChild != null) {
            // עדכון השם
            tvWelcomeChild.setText("שלום, " + currentChild.getName() + "!");

            // עדכון האוואטר הגדול באמצע המסך
            String avatarName = currentChild.getAvatar();
            if (avatarName != null && !avatarName.isEmpty()) {
                int resId = getResources().getIdentifier(avatarName, "drawable", getPackageName());
                if (resId != 0) {
                    ivWelcomeAvatar.setImageResource(resId);
                }
            }
        }

        btnStart.setOnClickListener(v -> {
            Intent intent = new Intent(MainActivity.this, SelectSubjectActivity.class);
            startActivity(intent);
        });
    }

    @Override
    protected void onStart() {
        super.onStart();
        updateNavHeader(); // מעדכן גם את התפריט הצידי
    }

    private void updateNavHeader() {
        NavigationView navigationView = findViewById(R.id.nav_view);
        if (navigationView != null) {
            View headerView = navigationView.getHeaderView(0);
            TextView navUserName = headerView.findViewById(R.id.nav_user_name);
            ImageView navUserImage = headerView.findViewById(R.id.nav_user_image);

            UserChild currentChild = SharedPreferencesUtil.getCurrentChild(this);

            if (currentChild != null) {
                navUserName.setText("שלום, " + currentChild.getName());

                String avatarName = currentChild.getAvatar();
                if (avatarName != null && !avatarName.isEmpty()) {
                    int resId = getResources().getIdentifier(avatarName, "drawable", getPackageName());
                    if (resId != 0) {
                        navUserImage.setImageResource(resId);
                    }
                }
            }
        }
    }

    @Override
    protected boolean hasSideMenu() {
        return true;
    }
}