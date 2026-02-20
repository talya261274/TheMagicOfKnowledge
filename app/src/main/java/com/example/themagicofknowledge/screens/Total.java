package com.example.themagicofknowledge.screens;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;

import com.example.themagicofknowledge.R;
import com.example.themagicofknowledge.utils.SharedPreferencesUtil;

public class Total extends BaseActivity {

    Button btn_TUserList, btn_TLogout, btn_SelectSubject, btn_SelectChild;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // טוען את התוכן
        setContentView(R.layout.activity_total);

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

    @Override
    protected boolean hasSideMenu() {
        return true; // יש תפריט צד במסך זה
    }

    private void signOut() {
        SharedPreferencesUtil.signOutUser(this);
        Intent intent = new Intent(Total.this, LandingActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
    }
}
