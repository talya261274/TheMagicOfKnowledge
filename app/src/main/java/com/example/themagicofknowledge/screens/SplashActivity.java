package com.example.themagicofknowledge.screens;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.core.graphics.Insets;

import com.example.themagicofknowledge.R;
import com.example.themagicofknowledge.utils.SharedPreferencesUtil;

public class SplashActivity extends AppCompatActivity {

    private static final String TAG = "SplashActivity";
    private static final int SPLASH_DISPLAY_TIME = 2000; // 2 שניות

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash);

        // Edge to edge (למנוע חיתוך UI עם סטטוס בר/ניווט בר)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        // משתמשים ב-Handler במקום Thread
        new Handler(Looper.getMainLooper()).postDelayed(() -> {
            Intent intent;
            if (SharedPreferencesUtil.isUserLoggedIn(this)) {
                Log.d(TAG, "User signed in, redirecting to MainActivity");
                intent = new Intent(SplashActivity.this, SelectChildActivity.class);
            } else {
                Log.d(TAG, "User not signed in, redirecting to LandingActivity");
                intent = new Intent(SplashActivity.this, LandingActivity.class);
            }
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
            finish();
        }, SPLASH_DISPLAY_TIME);
    }
}
