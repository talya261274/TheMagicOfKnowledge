package com.example.themagicofknowledge.screens;

import android.content.Intent;
import android.os.Bundle;
import android.view.MenuItem;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBarDrawerToggle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;

import com.example.themagicofknowledge.R;
import com.example.themagicofknowledge.models.UserParent;
import com.example.themagicofknowledge.services.DatabaseService;
import com.example.themagicofknowledge.utils.SharedPreferencesUtil;
import com.google.android.material.navigation.NavigationView;

public abstract class BaseActivity extends AppCompatActivity
        implements NavigationView.OnNavigationItemSelectedListener {

    protected DatabaseService databaseService;
    protected DrawerLayout drawerLayout;
    protected NavigationView navigationView;
    protected Toolbar toolbar;

    protected boolean hasSideMenu() {
        return true; // ברירת מחדל – יש Drawer
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        databaseService = DatabaseService.getInstance();

        // טוען את ה-Base XML
        super.setContentView(R.layout.activity_base);

        // Toolbar
        toolbar = findViewById(R.id.toolBar);
        setSupportActionBar(toolbar);

        // Drawer
        drawerLayout = findViewById(R.id.nav_layout);
        navigationView = findViewById(R.id.nav_view);
        navigationView.setNavigationItemSelectedListener(this);

        // עדכון שם המשתמש ב-Header
        updateNavigationHeader();

        if (hasSideMenu()) {
            // הגדרת ActionBar
            if (getSupportActionBar() != null) {
                getSupportActionBar().setDisplayHomeAsUpEnabled(true);
                getSupportActionBar().setDisplayShowTitleEnabled(false);
                getSupportActionBar().setTitle("");
            }

            // הגדרת Toggle
            ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(
                    this,
                    drawerLayout,
                    toolbar,
                    R.string.open_drawer,
                    R.string.close_drawer
            );
            drawerLayout.addDrawerListener(toggle);
            toggle.syncState();

        } else {
            // נעילת התפריט
            drawerLayout.setDrawerLockMode(DrawerLayout.LOCK_MODE_LOCKED_CLOSED);
            navigationView.setVisibility(View.GONE);

            if (getSupportActionBar() != null) {
                getSupportActionBar().setTitle("");
                getSupportActionBar().setDisplayHomeAsUpEnabled(false);
            }
        }
    }

    @Override
    public void setContentView(int layoutResID) {
        setContentLayout(layoutResID);
    }

    // מזריק את ה-layout של המסך לתוך Base
    protected void setContentLayout(int layoutResId) {
        FrameLayout contentFrame = findViewById(R.id.content_frame);
        if (contentFrame != null) {
            getLayoutInflater().inflate(layoutResId, contentFrame, true);
        }
    }

    // עדכון שם המשתמש בתפריט
    protected void updateNavigationHeader() {
        if (navigationView != null) {
            View headerView = navigationView.getHeaderView(0);
            TextView usernameText = headerView.findViewById(R.id.usernameText);

            UserParent currentUser = SharedPreferencesUtil.getUser(this);
            if (currentUser != null && usernameText != null) {
                usernameText.setText("שלום, " + currentUser.getFirstName());
            }
        }
    }

    protected void navigateTo(Class<?> targetActivity) {
        if (!this.getClass().equals(targetActivity)) {
            Intent intent = new Intent(this, targetActivity);
            startActivity(intent);
            finish();
        }
        drawerLayout.closeDrawer(GravityCompat.END);
    }

    @Override
    public boolean onNavigationItemSelected(@NonNull MenuItem item) {
        int id = item.getItemId();

        if (id == R.id.nav_home) {
            navigateTo(Total.class);

        } else if (id == R.id.nav_profile) {
            navigateTo(UserProfileActivity.class);

        } else if (id == R.id.nav_cards) {
            navigateTo(SelectSubjectActivity.class);

        } else if (id == R.id.nav_quiz) {
            navigateTo(PlacementTestActivity.class);

        } else if (id == R.id.nav_progrees) {
            // navigateTo(ProgressActivity.class);

        } else if (id == R.id.nav_logout) {
            drawerLayout.closeDrawer(GravityCompat.END);
            showLogoutDialog();
        }
        return true;
    }

    private void showLogoutDialog() {
        new androidx.appcompat.app.AlertDialog.Builder(this)
                .setTitle("התנתקות")
                .setMessage("האם אתה בטוח שברצונך להתנתק?")
                .setPositiveButton("כן", (dialog, which) -> {
                    SharedPreferencesUtil.signOutUser(this);

                    Intent intent = new Intent(this, LandingActivity.class);
                    intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                    startActivity(intent);
                })
                .setNegativeButton("לא", (dialog, which) -> dialog.dismiss())
                .show();
    }

    @Override
    public void onBackPressed() {
        if (drawerLayout != null && drawerLayout.isDrawerOpen(GravityCompat.END)) {
            drawerLayout.closeDrawer(GravityCompat.END);
        } else {
            super.onBackPressed();
        }
    }
}
