package com.example.themagicofknowledge.screens;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.example.themagicofknowledge.R;
import com.example.themagicofknowledge.models.UserParent;
import com.example.themagicofknowledge.services.DatabaseService;
import com.example.themagicofknowledge.utils.SharedPreferencesUtil;

public class SplashActivity extends AppCompatActivity {

    private static final String TAG = "SplashActivity";
    private static final int SPLASH_DISPLAY_TIME = 2000;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash);

        // טיפול ב-Insets
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        // המתנה של 2 שניות ואז ניווט
        new Handler(Looper.getMainLooper()).postDelayed(this::navigateToCorrectScreen, SPLASH_DISPLAY_TIME);
    }


    // ===== בחירת המסך הנכון לפי המשתמש =====
    private void navigateToCorrectScreen() {
        Intent intent;

        if (SharedPreferencesUtil.isUserLoggedIn(this)) {
            // יש משתמש מחובר - בודקים אם הוא מנהל
            UserParent user = SharedPreferencesUtil.getUser(this);

            if (user != null && user.isAdmin()) {
                // מנהל - שולחים למסך ניהול
                Log.d(TAG, "Admin user detected: " + user.getUserName());
                intent = new Intent(SplashActivity.this, AdminDashboardActivity.class);
            } else {
                // הורה רגיל - שולחים לבחירת ילד
                Log.d(TAG, "Regular user detected");
                intent = new Intent(SplashActivity.this, SelectChildActivity.class);
            }
        } else {
            // אין משתמש מחובר - מסך פתיחה
            Log.d(TAG, "No user logged in");
            intent = new Intent(SplashActivity.this, LandingActivity.class);
        }

        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }
}