package com.example.themagicofknowledge.screens;

import android.os.Bundle;
import android.widget.TextView;

import com.example.themagicofknowledge.R;

public class MainActivity extends BaseActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // טוען את התוכן הייחודי של המסך
        setContentView(R.layout.activity_main_content);

        // עכשיו אפשר למצוא את הרכיבים
        TextView welcomeText = findViewById(R.id.tvWelcome);
        if (welcomeText != null) {
            welcomeText.setText("ברוך הבא למסך הראשי!");
        }
    }

    @Override
    protected boolean hasSideMenu() {
        return true; // יש תפריט צד במסך זה
    }
}
