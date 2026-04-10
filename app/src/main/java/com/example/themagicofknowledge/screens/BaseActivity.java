package com.example.themagicofknowledge.screens;

import android.content.Intent;
import android.os.Bundle;
import android.view.MenuItem;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBarDrawerToggle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;

import com.example.themagicofknowledge.R;
import com.example.themagicofknowledge.models.UserChild;
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
    protected ImageView ivToolbarAvatar; // הוספנו משתנה לאוואטר ב-Toolbar

    protected boolean hasSideMenu() {
        return true;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        databaseService = DatabaseService.getInstance();

        // טוען את ה-Base XML
        super.setContentView(R.layout.activity_base);

        // אתחול ה-Toolbar והאוואטר שעליו
        toolbar = findViewById(R.id.toolBar);
        ivToolbarAvatar = findViewById(R.id.ivToolbarAvatar);
        setSupportActionBar(toolbar);

        // Drawer
        drawerLayout = findViewById(R.id.nav_layout);
        navigationView = findViewById(R.id.nav_view);
        navigationView.setNavigationItemSelectedListener(this);

        if (hasSideMenu()) {
            setupDrawer();
        } else {
            lockDrawer();
        }
    }

    /**
     * פונקציה שמתבצעת בכל פעם שחוזרים למסך.
     * זה מוודא שהשם והתמונה יתעדכנו גם אם החלפנו ילד רגע לפני.
     */
    @Override
    protected void onStart() {
        super.onStart();
        updateUIComponents();
    }

    private void updateUIComponents() {
        UserChild currentChild = SharedPreferencesUtil.getCurrentChild(this);

        // 1. עדכון האוואטר ב-Toolbar (במקום הלוגו)
        updateToolbarAvatar(currentChild);

        // 2. עדכון תפריט הצד (שם ותמונה)
        updateNavigationHeader(currentChild);
    }

    private void updateToolbarAvatar(UserChild child) {
        if (ivToolbarAvatar == null) return;

        if (child != null && child.getAvatar() != null && !child.getAvatar().isEmpty()) {
            int resId = getResources().getIdentifier(child.getAvatar(), "drawable", getPackageName());
            if (resId != 0) {
                ivToolbarAvatar.setImageResource(resId);
            } else {
                ivToolbarAvatar.setImageResource(R.drawable.logo); // ברירת מחדל
            }
        } else {
            ivToolbarAvatar.setImageResource(R.drawable.logo); // אם אין ילד, נציג לוגו
        }
    }

    protected void updateNavigationHeader(UserChild child) {
        if (navigationView != null) {
            View headerView = navigationView.getHeaderView(0);
            TextView usernameText = headerView.findViewById(R.id.nav_user_name);
            ImageView userImage = headerView.findViewById(R.id.nav_user_image);

            if (child != null) {
                // שם הילד עם "שלום"
                if (usernameText != null) usernameText.setText("שלום, " + child.getName());

                // אוואטר הילד בתפריט הצד
                String avatarName = child.getAvatar();
                if (avatarName != null && !avatarName.isEmpty() && userImage != null) {
                    int resId = getResources().getIdentifier(avatarName, "drawable", getPackageName());
                    if (resId != 0) userImage.setImageResource(resId);
                }
            } else {
                // אם אין ילד נבחר, מציגים את שם ההורה
                UserParent currentUser = SharedPreferencesUtil.getUser(this);
                if (currentUser != null && usernameText != null) {
                    usernameText.setText("שלום, " + currentUser.getFirstName());
                }
            }
        }
    }

    private void setupDrawer() {
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setDisplayShowTitleEnabled(false);
        }

        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(
                this, drawerLayout, toolbar, R.string.open_drawer, R.string.close_drawer);
        drawerLayout.addDrawerListener(toggle);
        toggle.syncState();
    }

    private void lockDrawer() {
        drawerLayout.setDrawerLockMode(DrawerLayout.LOCK_MODE_LOCKED_CLOSED);
        navigationView.setVisibility(View.GONE);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(false);
        }
    }

    @Override
    public void setContentView(int layoutResID) {
        FrameLayout contentFrame = findViewById(R.id.content_frame);
        if (contentFrame != null) {
            getLayoutInflater().inflate(layoutResID, contentFrame, true);
        }
    }

    protected void navigateTo(Class<?> targetActivity) {
        if (!this.getClass().equals(targetActivity)) {
            Intent intent = new Intent(this, targetActivity);
            startActivity(intent);
            // לא תמיד נרצה לעשות finish(), תלוי אם זה מסך ראשי או לא
        }
        drawerLayout.closeDrawer(GravityCompat.START);
    }

    @Override
    public boolean onNavigationItemSelected(@NonNull MenuItem item) {
        int id = item.getItemId();

        // ניווט לדף הבית (Main)
        if (id == R.id.nav_home) {
            navigateTo(MainActivity.class);

        }
        // ניווט לפרופיל הורה/משתמש
        else if (id == R.id.nav_profile) {
            navigateTo(UserProfileActivity.class);

        }
        // ניווט לבחירת נושאים (הקלפים)
        else if (id == R.id.nav_cards) {
            navigateTo(SelectSubjectActivity.class);

        }
        // משחק בחירה מרובה
        else if (id == R.id.nav_game1) {
            navigateTo(ImageRecognitionGameActivity.class);

        }
        // משחק הזיכרון
        else if (id == R.id.nav_game2) {
            navigateTo(MemoryGameActivity.class);

        }
        // משחק זיהוי קול
        else if (id == R.id.nav_game3) {
            navigateTo(AudioRecognitionActivity.class);

        }
        // משחק התאמת זוגות - גרירה
        else if (id == R.id.nav_game4) {
            navigateTo(MatchingGameActivity.class);

        }

        // משחק השלמת משפט
        else if (id == R.id.nav_game5) {
            navigateTo(SentenceCompletionActivity.class);

        }
        /**
        // ניווט למסך הסטטיסטיקה והתקדמות הילדים
        else if (id == R.id.nav_progrees) {
            navigateTo(GameProgressActivity.class);

        }
        **/

        // התנתקות מהמערכת
        else if (id == R.id.nav_logout) {
            drawerLayout.closeDrawer(GravityCompat.START);
            showLogoutDialog();
            return true; // עוצרים כאן כי יש דיאלוג, לא עוברים מסך מיד
        }

        // סגירת התפריט לאחר הבחירה
        if (drawerLayout != null) {
            drawerLayout.closeDrawer(GravityCompat.START);
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
                .setNegativeButton("לא", null)
                .show();
    }

    @Override
    public void onBackPressed() {
        if (drawerLayout != null && drawerLayout.isDrawerOpen(GravityCompat.START)) {
            drawerLayout.closeDrawer(GravityCompat.START);
        } else {
            super.onBackPressed();
        }
    }
}