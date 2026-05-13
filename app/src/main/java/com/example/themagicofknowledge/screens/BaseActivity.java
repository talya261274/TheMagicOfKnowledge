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
import com.example.themagicofknowledge.utils.ImageUtil;
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
        super.onCreate(savedInstanceState);
        databaseService = DatabaseService.getInstance();
        super.setContentView(R.layout.activity_base);

        toolbar = findViewById(R.id.toolBar);
        ivToolbarAvatar = findViewById(R.id.ivToolbarAvatar);
        setSupportActionBar(toolbar);

        drawerLayout = findViewById(R.id.nav_layout);
        navigationView = findViewById(R.id.nav_view);
        navigationView.setNavigationItemSelectedListener(this);

        navigationView.setItemTextAppearance(R.style.NavMenuItemStyle);
        navigationView.setItemIconTintList(null);
        navigationView.setItemTextColor(createNavColorStateList());
        navigationView.setItemBackground(createNavItemBackground());

        navigationView.setBackgroundColor(Color.WHITE);
        navigationView.setItemIconTintList(null);
        navigationView.setItemTextColor(createNavColorStateList());
        navigationView.setItemBackground(createNavItemBackground());        navigationView.setItemTextAppearance(R.style.NavMenuItemStyle);

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

        ivToolbarAvatar.setOnClickListener(v -> {
            UserParent user = SharedPreferencesUtil.getUser(this);
            UserChild currentChild = SharedPreferencesUtil.getCurrentChild(this);

            if (user != null && user.isAdmin()) {
                navigateTo(AdminDashboardActivity.class);
            } else if (currentChild != null) {
                navigateTo(MainActivity.class);
            } else {
                navigateTo(SelectChildActivity.class);
            }
        });

        toolbar.setOnClickListener(v -> {
            if (drawerLayout != null) {
                drawerLayout.openDrawer(GravityCompat.START);
            }
        });

        findViewById(R.id.cardToolbarAvatar).setOnClickListener(v -> {
            UserParent user = SharedPreferencesUtil.getUser(this);
            UserChild currentChild = SharedPreferencesUtil.getCurrentChild(this);

            if (user != null && user.isAdmin()) {
                navigateTo(AdminDashboardActivity.class);
            } else if (currentChild != null) {
                navigateTo(MainActivity.class);
            } else {
                navigateTo(SelectChildActivity.class);
            }
        });
    }

    private void updateNavMenuByRole() {
        if (navigationView == null) return;

        UserParent user = SharedPreferencesUtil.getUser(this);
        if (user == null) return;

        Menu menu = navigationView.getMenu();
        for (int i = 0; i < menu.size(); i++) {
            menu.getItem(i).setVisible(true);
        }

        UserChild currentChild = SharedPreferencesUtil.getCurrentChild(this);

        if (user.isAdmin()) {
            hideMenuItem(menu, R.id.nav_subjects);
            hideMenuItem(menu, R.id.nav_progress);
            hideMenuItem(menu, R.id.nav_back_to_parent);
        } else if (currentChild != null) {
            hideMenuItem(menu, R.id.nav_profile);
            hideMenuItem(menu, R.id.nav_progress);
            hideMenuItem(menu, R.id.nav_admin_users);
            hideMenuItem(menu, R.id.nav_logout);
            hideMenuItem(menu, R.id.nav_statistics);
        } else {
            hideMenuItem(menu, R.id.nav_admin_users);
            hideMenuItem(menu, R.id.nav_back_to_parent);
            hideMenuItem(menu, R.id.nav_statistics);

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

    protected void updateUIComponents() {
        UserChild currentChild = SharedPreferencesUtil.getCurrentChild(this);

        // 1. עדכון האוואטר ב-Toolbar (במקום הלוגו)
        updateToolbarAvatar(currentChild);

        // 2. עדכון תפריט הצד (שם ותמונה)
        updateNavigationHeader(currentChild);
    }

    protected void updateToolbarAvatar(UserChild child) {
        if (ivToolbarAvatar == null) return;

        if (child != null && child.getAvatar() != null) {
            ImageUtil.loadAvatar(this, ivToolbarAvatar, child.getAvatar());
        } else {
            UserParent parent = SharedPreferencesUtil.getUser(this);
            if (parent != null && parent.getAvatar() != null) {
                ImageUtil.loadAvatar(this, ivToolbarAvatar, parent.getAvatar());
            } else {
                ivToolbarAvatar.setImageResource(R.drawable.logo);
            }
        }
    }

    protected void updateNavigationHeader(UserChild child) {
        if (navigationView == null) return;
        View headerView = navigationView.getHeaderView(0);
        TextView usernameText = headerView.findViewById(R.id.nav_user_name);
        ImageView userImage = headerView.findViewById(R.id.nav_user_image);

        if (child != null) {
            if (usernameText != null) usernameText.setText("שלום, " + child.getName());
            if (userImage != null) ImageUtil.loadAvatar(this, userImage, child.getAvatar());
        } else {
            UserParent currentUser = SharedPreferencesUtil.getUser(this);
            if (currentUser != null) {
                if (usernameText != null) usernameText.setText("שלום, " + currentUser.getFirstName());
                if (userImage != null) ImageUtil.loadAvatar(this, userImage, currentUser.getAvatar());
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
        else if (id == R.id.nav_profile) {
            navigateTo(UserProfileActivity.class);

        } else if (id == R.id.nav_progress) {
            navigateTo(ParentTrackingActivity.class);

        } else if (id == R.id.nav_admin_users) {
            navigateTo(UsersListActivity.class);

        }
        else if (id == R.id.nav_statistics) {
            navigateTo(StatisticsActivity.class);

        }
        else if (id == R.id.nav_back_to_parent) {
            showParentPasswordDialog();
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
    protected void showLogoutDialog() {
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

    private void showParentPasswordDialog() {
        final android.app.Dialog dialog = new android.app.Dialog(this);
        dialog.setContentView(R.layout.dialog_parent_password);
        if (dialog.getWindow() != null)
            dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);

        android.widget.EditText etPassword = dialog.findViewById(R.id.etParentPassword);
        com.google.android.material.button.MaterialButton btnConfirm = dialog.findViewById(R.id.btnConfirmPassword);
        android.widget.TextView btnCancel = dialog.findViewById(R.id.btnCancelPassword);
        android.widget.TextView tvError = dialog.findViewById(R.id.tvPasswordError);

        btnConfirm.setOnClickListener(v -> {
            String entered = etPassword.getText().toString().trim();

            // בדיקת קלט
            if (entered.isEmpty()) {
                tvError.setVisibility(View.VISIBLE);
                tvError.setText("אנא הכנס סיסמה ❌");
                return;
            }

            UserParent parent = SharedPreferencesUtil.getUser(this);

            if (parent != null && entered.equals(parent.getPassword())) {
                dialog.dismiss();
                SharedPreferencesUtil.clearSelectedChild(this);
                Intent intent = new Intent(this, SelectChildActivity.class);
                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                startActivity(intent);
            } else {
                tvError.setVisibility(View.VISIBLE);
                tvError.setText("סיסמה שגויה ❌");
                etPassword.setText("");
            }
        });

        btnCancel.setOnClickListener(v -> dialog.dismiss());

        dialog.setCancelable(true);
        dialog.show();
    }

    private android.content.res.ColorStateList createNavColorStateList() {
        int[][] states = new int[][] {
                new int[] { android.R.attr.state_checked },
                new int[] { -android.R.attr.state_checked }
        };
        int[] colors = new int[] {
                Color.parseColor("#1E5F8B"),
                Color.parseColor("#2C3E50")
        };
        return new android.content.res.ColorStateList(states, colors);
    }

    private android.graphics.drawable.Drawable createNavItemBackground() {
        android.graphics.drawable.StateListDrawable stateList =
                new android.graphics.drawable.StateListDrawable();

        android.graphics.drawable.GradientDrawable selected =
                new android.graphics.drawable.GradientDrawable();
        selected.setColor(Color.parseColor("#1A1E5F8B"));
        selected.setCornerRadius(50f);

        stateList.addState(new int[]{ android.R.attr.state_checked }, selected);
        stateList.addState(new int[]{},
                new android.graphics.drawable.ColorDrawable(Color.TRANSPARENT));
        return stateList;
    }
}