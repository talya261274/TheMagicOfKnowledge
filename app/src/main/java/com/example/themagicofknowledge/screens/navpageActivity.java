package com.example.themagicofknowledge.screens;

import android.os.Bundle;

import com.example.themagicofknowledge.R;

public class navpageActivity extends BaseActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // טוען רק את התוכן הייחודי של המסך
        setContentView(R.layout.activity_navpage);
    }
}