package com.example.themagicofknowledge.screens;

import android.content.Intent;
import android.graphics.drawable.AnimatedVectorDrawable;
import android.os.Bundle;
import android.widget.Button;
import android.widget.ImageView;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.example.themagicofknowledge.R;
import com.example.themagicofknowledge.models.UserParent;
import com.example.themagicofknowledge.utils.SharedPreferencesUtil;

public class LandingActivity extends AppCompatActivity {

    UserParent UserParent;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_landing);

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        UserParent = SharedPreferencesUtil.getUser(LandingActivity.this);
        if(SharedPreferencesUtil.isUserLoggedIn(LandingActivity.this)){
            Intent intent = new Intent(LandingActivity.this, Total.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
            finish();
            return;
        }

//        ImageView wandImage = findViewById(R.id.wand);
//        AnimatedVectorDrawable wandAnimation = (AnimatedVectorDrawable) wandImage.getDrawable();
//        wandAnimation.start();

        Button button = findViewById(R.id.btn_Main_toLogin);
        button.setOnClickListener(view -> {
            Intent intent = new Intent(LandingActivity.this, LoginActivity.class);
            startActivity(intent);
        });

        Button button2 = findViewById(R.id.btn_Main_toregister);
        button2.setOnClickListener(view -> {
            Intent intent = new Intent(LandingActivity.this, RegisterActivity.class);
            startActivity(intent);
        });

    }
}