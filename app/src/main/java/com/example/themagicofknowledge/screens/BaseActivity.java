package com.example.themagicofknowledge.screens;

import static com.example.themagicofknowledge.models.UserRole.Role.ADMIN;
import static com.example.themagicofknowledge.models.UserRole.Role.CHILD;
import static com.example.themagicofknowledge.models.UserRole.Role.PARENT;

import android.app.Dialog;
import android.content.Intent;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
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
import com.example.themagicofknowledge.models.UserRole;
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

    protected boolean showToolbar() {
        return true;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        try {
            super.onCreate(savedInstanceState);
            databaseService = DatabaseService.getInstance();
            super.setContentView(R.layout.activity_base);

            toolbar = findViewById(R.id.toolBar);
            ivToolbarAvatar = findViewById(R.id.ivToolbarAvatar);
            setSupportActionBar(toolbar);

            drawerLayout = findViewById(R.id.nav_layout);
            navigationView = findViewById(R.id.nav_view);
            navigationView.setNavigationItemSelectedListener(this);

            navigationView.setBackgroundColor(Color.parseColor("#E0F7FA"));
            navigationView.setItemTextAppearance(R.style.NavMenuItemStyle);

            if (hasSideMenu()) {
                setupDrawer();
            } else {
                lockDrawer();
            }

            if (toolbar != null) {
                if (showToolbar()) {
                    toolbar.setVisibility(View.VISIBLE);
                } else {
                    toolbar.setVisibility(View.GONE);
                }
            }
        } catch (Exception e) {
        }
    }
    private void updateNavMenuByRole() {
        if (navigationView == null) return;

        UserParent user = SharedPreferencesUtil.getUser(this);
        if (user == null) return;

        UserChild currentChild = SharedPreferencesUtil.getCurrentChild(this);
        Menu menu = navigationView.getMenu();

        if (user.isAdmin()) {
            // מנהל - מסתיר הכל חוץ מניהול
            hideMenuItem(menu, R.id.nav_subjects);
            hideMenuItem(menu, R.id.nav_profile);
            hideMenuItem(menu, R.id.nav_progress);
            hideMenuItem(menu, R.id.nav_mix);
            hideMenuItem(menu, R.id.nav_back_to_parent);
        } else if (currentChild != null) {
            // ילד - הוא נכנס למצב משחק (יש currentChild)
            hideMenuItem(menu, R.id.nav_profile);
            hideMenuItem(menu, R.id.nav_progress);
            hideMenuItem(menu, R.id.nav_admin_users);
            hideMenuItem(menu, R.id.nav_logout);
            // nav_back_to_parent נשאר גלוי - הילד צריך אותו!
        } else {
            // הורה רגיל - לא בחר ילד עדיין, או במסכי ניהול
            hideMenuItem(menu, R.id.nav_admin_users);
            hideMenuItem(menu, R.id.nav_back_to_parent);  // ← הוספה - הורה לא צריך
        }
    }

    private void setAllMenuItemsVisible(Menu menu, boolean visible) {
        for (int i = 0; i < menu.size(); i++) {
            MenuItem item = menu.getItem(i);
            item.setVisible(visible);
            if (item.hasSubMenu()) {
                Menu subMenu = item.getSubMenu();
                for (int j = 0; j < subMenu.size(); j++) {
                    subMenu.getItem(j).setVisible(visible);
                }
            }
        }
    }

    private void hideMenuItem(Menu menu, int itemId) {
        MenuItem item = menu.findItem(itemId);
        if (item != null) {
            item.setVisible(false);
        }

        // חיפוש בתת-תפריטים
        for (int i = 0; i < menu.size(); i++) {
            MenuItem parentItem = menu.getItem(i);
            if (parentItem.hasSubMenu()) {
                MenuItem subItem = parentItem.getSubMenu().findItem(itemId);
                if (subItem != null) {
                    subItem.setVisible(false);
                }
            }
        }
    }
    @Override
    protected void onStart() {
        super.onStart();
        updateUIComponents();
        updateNavMenuByRole();
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
            UserParent user = SharedPreferencesUtil.getUser(this);
            UserChild currentChild = SharedPreferencesUtil.getCurrentChild(this);
            if (user != null && user.isAdmin()) {
                navigateTo(AdminDashboardActivity.class);
            }
            else  if (currentChild != null) {
                navigateTo(MainActivity.class);
            }
            else
                navigateTo(SelectChildActivity.class);
        }

        // ניווט לפרופיל הורה/משתמש
        else if (id == R.id.nav_subjects) {
            navigateTo(SelectSubjectActivity.class);

        }
        // ניווט לבחירת נושאים (הקלפים)
        else if (id == R.id.nav_profile) {
            navigateTo(UserProfileActivity.class);

        } else if (id == R.id.nav_progress) {
            navigateTo(ParentTrackingActivity.class);

        } else if (id == R.id.nav_mix) {
            navigateTo(MixedGameActivity.class);

        } else if (id == R.id.nav_admin_users) {
            navigateTo(UsersListActivity.class);

        }
        else if (id == R.id.nav_back_to_parent) {
            // אישור עם דיאלוג
            showCustomDialog(
                    "חזרה למצב הורה",
                    "האם להפסיק את התרגול ולחזור למצב הורה?",
                    "כן, חזור",
                    Color.parseColor("#FF9800"),
                    () -> {
                        SharedPreferencesUtil.clearSelectedChild(this);
                        Intent intent = new Intent(this, SelectChildActivity.class);
                        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                        startActivity(intent);
                    }
            );
            return true;
        }

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

    public void showCustomDialog(String title, String message, String confirmText, int confirmColor, Runnable onConfirm) {
        Dialog dialog = new Dialog(this);
        dialog.setContentView(R.layout.custom_action_dialog);

        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        }

        TextView tvTitle = dialog.findViewById(R.id.tvDialogTitle);
        TextView tvMessage = dialog.findViewById(R.id.tvDialogMessage);
        com.google.android.material.button.MaterialButton btnConfirm = dialog.findViewById(R.id.btnConfirm);
        TextView btnCancel = dialog.findViewById(R.id.btnCancel);

        tvTitle.setText(title);
        tvMessage.setText(message);
        btnConfirm.setText(confirmText);
        btnConfirm.setBackgroundTintList(ColorStateList.valueOf(confirmColor));

        btnConfirm.setOnClickListener(v -> {
            onConfirm.run();
            dialog.dismiss();
        });

        btnCancel.setOnClickListener(v -> dialog.dismiss());
        dialog.show();
    }

    // עדכון פונקציית ההתנתקות שתשתמש בדיאלוג החדש
    private void showLogoutDialog() {
        showCustomDialog(
                "התנתקות",
                "האם אתה בטוח שברצונך לצאת?",
                "התנתק",
                Color.parseColor("#FF5252"),
                () -> {
                    SharedPreferencesUtil.signOutUser(this);
                    Intent intent = new Intent(this, LandingActivity.class);
                    intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                    startActivity(intent);
                }
        );
    }
}