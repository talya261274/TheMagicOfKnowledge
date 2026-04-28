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

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        new Handler(Looper.getMainLooper()).postDelayed(() -> {
            Intent intent;
            if (SharedPreferencesUtil.isUserLoggedIn(this)) {
                UserParent user = SharedPreferencesUtil.getUser(this);
                if (user != null && user.isAdmin()) {
                    intent = new Intent(SplashActivity.this, AdminDashboardActivity.class);
                } else {
                    intent = new Intent(SplashActivity.this, SelectChildActivity.class);
                }
            } else {
                intent = new Intent(SplashActivity.this, LandingActivity.class);
            }
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
            finish();
        }, SPLASH_DISPLAY_TIME);    }

    private void checkUserAndNavigate() {
        if (SharedPreferencesUtil.isUserLoggedIn(this)) {
            // משתמש מחובר - נרענן את הנתונים מ-Firebase
            UserParent localUser = SharedPreferencesUtil.getUser(this);

            DatabaseService.getInstance().getUser(localUser.getId(),
                    new DatabaseService.DatabaseCallback<UserParent>() {
                        @Override
                        public void onCompleted(UserParent freshUser) {
                            if (freshUser != null) {
                                // עדכון הנתונים המקומיים עם הגרסה העדכנית
                                SharedPreferencesUtil.saveUser(SplashActivity.this, freshUser);
                                Log.d(TAG, "User refreshed: " + freshUser.getUserName()
                                        + ", isAdmin=" + freshUser.isAdmin());
                            }
                            navigateToHome();
                        }

                        @Override
                        public void onFailed(Exception e) {
                            Log.e(TAG, "Failed to refresh user", e);
                            // גם אם הרענון נכשל - נמשיך עם הנתונים הקיימים
                            navigateToHome();
                        }
                    });
        } else {
            navigateToLanding();
        }
    }

    private void navigateToHome() {
        Intent intent = new Intent(SplashActivity.this, SelectChildActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }

    private void navigateToLanding() {
        Intent intent = new Intent(SplashActivity.this, LandingActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }
}