package com.example.themagicofknowledge.screens;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.example.themagicofknowledge.R;
import com.example.themagicofknowledge.utils.SharedPreferencesUtil;

public class Total extends AppCompatActivity {

    Button btn_TUserList , btn_TLogout, btn_SelectSubject , btn_SelectChild;
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

        btn_TUserList = findViewById(R.id.btn_TUserList);
        btn_TUserList.setOnClickListener(view -> {
            Intent intent = new Intent(Total.this, UsersListActivity.class);
            startActivity(intent);
        });

        btn_TLogout = findViewById(R.id.btn_TLogout);
        btn_TLogout.setOnClickListener(view -> {
            signOut();
        });

        btn_SelectSubject = findViewById(R.id.btn_SelectSubject);
        btn_SelectSubject.setOnClickListener(view -> {
            Intent intent = new Intent(Total.this, SelectSubjectActivity.class);
            startActivity(intent);
        });

        btn_SelectChild = findViewById(R.id.btn_SelectChild);
        btn_SelectChild.setOnClickListener(view -> {
            Intent intent = new Intent(Total.this, SelectChildActivity.class);
            startActivity(intent);
        });
    }

        private void signOut () {
                SharedPreferencesUtil.signOutUser(this);
                Intent intent = new Intent(Total.this, LandingActivity.class);
                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                startActivity(intent);
        }
    }
