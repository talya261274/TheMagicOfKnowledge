package com.example.themagicofknowledge.screens;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.example.themagicofknowledge.R;
import com.example.themagicofknowledge.utils.SharedPreferencesUtil;

public class Total extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_total);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });


        Button btn_TUpdate = findViewById(R.id.btn_TUpdate);
        btn_TUpdate.setOnClickListener(view -> {
            Intent intent = new Intent(Total.this, UpdateDetailsActivity.class);
            startActivity(intent);
        });

        Button btn_TUserList = findViewById(R.id.btn_TUserList);
        btn_TUserList.setOnClickListener(view -> {
            Intent intent = new Intent(Total.this, UsersListActivity.class);
            startActivity(intent);
        });

        Button btn_TLogout = findViewById(R.id.btn_TLogout);
        btn_TLogout.setOnClickListener(view -> {
            signOut();
        });
    }

        private void signOut () {
                SharedPreferencesUtil.signOutUser(this);
                Intent intent = new Intent(Total.this, LandingActivity.class);
                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                startActivity(intent);
        }
    }
