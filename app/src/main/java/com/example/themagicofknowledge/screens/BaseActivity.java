package com.example.themagicofknowledge.screens;

import android.app.Dialog;
import android.content.Intent;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

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
import com.example.themagicofknowledge.utils.ImageUtil;
import com.example.themagicofknowledge.utils.SharedPreferencesUtil;
import com.google.android.material.navigation.NavigationView;

public abstract class BaseActivity extends AppCompatActivity
        implements NavigationView.OnNavigationItemSelectedListener {

    protected DatabaseService databaseService;
    protected DrawerLayout drawerLayout;
    protected NavigationView navigationView;
    protected Toolbar toolbar;
    protected ImageView ivToolbarAvatar;
    protected View cardToolbarAvatar;

    protected boolean hasSideMenu() { return true; }
    protected boolean showToolbar() { return true; }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        databaseService = DatabaseService.getInstance();
        super.setContentView(R.layout.activity_base);

        // אתחול רכיבי ה-UI
        toolbar = findViewById(R.id.toolBar);
        ivToolbarAvatar = findViewById(R.id.ivToolbarAvatar);
        cardToolbarAvatar = findViewById(R.id.cardToolbarAvatar);
        drawerLayout = findViewById(R.id.nav_layout);
        navigationView = findViewById(R.id.nav_view);

        setSupportActionBar(toolbar);
        setupNavigationStyles();

        if (hasSideMenu()) {
            setupDrawer();
        } else {
            lockDrawer();
        }

        if (toolbar != null) {
            toolbar.setVisibility(showToolbar() ? View.VISIBLE : View.GONE);
        }

        setupClickListeners();
    }

    private void setupClickListeners() {
        // 1. לחיצה על הפס (Toolbar) - חזרה לדף הבית לפי סוג משתמש
        toolbar.setOnClickListener(v -> handleHomeNavigation());

        // 2. לחיצה על התמונה (Avatar) - מעבר לפרופיל (רק להורה/מנהל)
        View.OnClickListener profileListener = v -> {
            UserParent user = SharedPreferencesUtil.getUser(this);
            UserChild currentChild = SharedPreferencesUtil.getCurrentChild(this);

            if (user != null) {
                // מנהל או הורה עוברים לעמוד הפרופיל
                navigateTo(UserProfileActivity.class);
            } else if (currentChild != null) {
                // לילד אין עמוד פרופיל
                Toast.makeText(this, "עמוד פרופיל זמין להורים בלבד", Toast.LENGTH_SHORT).show();
            }
        };

        if (ivToolbarAvatar != null) ivToolbarAvatar.setOnClickListener(profileListener);
        if (cardToolbarAvatar != null) cardToolbarAvatar.setOnClickListener(profileListener);
    }

    private void handleHomeNavigation() {
        UserParent user = SharedPreferencesUtil.getUser(this);
        UserChild currentChild = SharedPreferencesUtil.getCurrentChild(this);

        if (user != null && user.isAdmin()) {
            navigateTo(AdminDashboardActivity.class);
        } else if (currentChild != null) {
            navigateTo(MainActivity.class);
        } else {
            navigateTo(SelectChildActivity.class);
        }
    }

    private void setupDrawer() {
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setDisplayShowTitleEnabled(false);
        }

        // לחיצה על כפתור ה"המבורגר" פותחת את התפריט
        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(
                this, drawerLayout, toolbar, R.string.open_drawer, R.string.close_drawer);

        // כאן אנחנו מוודאים שה-Toggle עצמו מנהל את פתיחת התפריט בלחיצה על האייקון
        drawerLayout.addDrawerListener(toggle);
        toggle.syncState();
    }

    // --- לוגיקת תפריט וניווט ---

    private void updateNavMenuByRole() {
        if (navigationView == null) return;
        UserParent user = SharedPreferencesUtil.getUser(this);
        if (user == null) return;

        Menu menu = navigationView.getMenu();
        setAllMenuItemsVisible(menu, true);

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

    @Override
    public boolean onNavigationItemSelected(@NonNull MenuItem item) {
        int id = item.getItemId();

        if (id == R.id.nav_home) {
            handleHomeNavigation();
        } else if (id == R.id.nav_subjects) {
            navigateTo(SelectSubjectActivity.class);
        } else if (id == R.id.nav_profile) {
            navigateTo(UserProfileActivity.class);
        } else if (id == R.id.nav_progress) {
            navigateTo(ParentTrackingActivity.class);
        } else if (id == R.id.nav_admin_users) {
            navigateTo(UsersListActivity.class);
        } else if (id == R.id.nav_statistics) {
            navigateTo(StatisticsActivity.class);
        } else if (id == R.id.nav_back_to_parent) {
            showParentPasswordDialog();
            return true;
        } else if (id == R.id.nav_logout) {
            showLogoutDialog();
            return true;
        }

        if (drawerLayout != null) {
            drawerLayout.closeDrawer(GravityCompat.START);
        }
        return true;
    }

    // --- פונקציות עזר ו-UI ---

    protected void navigateTo(Class<?> targetActivity) {
        if (!this.getClass().equals(targetActivity)) {
            Intent intent = new Intent(this, targetActivity);
            startActivity(intent);
        }
        if (drawerLayout != null) drawerLayout.closeDrawer(GravityCompat.START);
    }

    protected void updateUIComponents() {
        UserChild currentChild = SharedPreferencesUtil.getCurrentChild(this);
        updateToolbarAvatar(currentChild);
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

    private void setupNavigationStyles() {
        if (navigationView == null) return;
        navigationView.setNavigationItemSelectedListener(this);
        navigationView.setItemTextAppearance(R.style.NavMenuItemStyle);
        navigationView.setItemIconTintList(null);
        navigationView.setItemTextColor(createNavColorStateList());
        navigationView.setItemBackground(createNavItemBackground());
        navigationView.setBackgroundColor(Color.WHITE);
    }

    private void lockDrawer() {
        if (drawerLayout != null) {
            drawerLayout.setDrawerLockMode(DrawerLayout.LOCK_MODE_LOCKED_CLOSED);
            navigationView.setVisibility(View.GONE);
        }
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

    private void hideMenuItem(Menu menu, int itemId) {
        MenuItem item = menu.findItem(itemId);
        if (item != null) item.setVisible(false);
    }

    private void setAllMenuItemsVisible(Menu menu, boolean visible) {
        for (int i = 0; i < menu.size(); i++) {
            menu.getItem(i).setVisible(visible);
        }
    }

    @Override
    protected void onStart() {
        super.onStart();
        updateUIComponents();
        updateNavMenuByRole();
    }

    // --- דיאלוגים ---

    public void showCustomDialog(String title, String message, String confirmText, int confirmColor, Runnable onConfirm) {
        Dialog dialog = new Dialog(this);
        dialog.setContentView(R.layout.custom_action_dialog);
        if (dialog.getWindow() != null) dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));

        TextView tvTitle = dialog.findViewById(R.id.tvDialogTitle);
        TextView tvMessage = dialog.findViewById(R.id.tvDialogMessage);
        com.google.android.material.button.MaterialButton btnConfirm = dialog.findViewById(R.id.btnConfirm);
        TextView btnCancel = dialog.findViewById(R.id.btnCancel);

        tvTitle.setText(title);
        tvMessage.setText(message);
        btnConfirm.setText(confirmText);
        btnConfirm.setBackgroundTintList(ColorStateList.valueOf(confirmColor));

        btnConfirm.setOnClickListener(v -> { onConfirm.run(); dialog.dismiss(); });
        btnCancel.setOnClickListener(v -> dialog.dismiss());
        dialog.show();
    }

    protected void showLogoutDialog() {
        showCustomDialog("התנתקות", "האם אתה בטוח שברצונך לצאת?", "התנתק", Color.parseColor("#FF5252"), () -> {
            SharedPreferencesUtil.signOutUser(this);
            Intent intent = new Intent(this, LandingActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
        });
    }

    private void showParentPasswordDialog() {
        final Dialog dialog = new Dialog(this);
        dialog.setContentView(R.layout.dialog_parent_password);
        if (dialog.getWindow() != null) dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);

        android.widget.EditText etPassword = dialog.findViewById(R.id.etParentPassword);
        com.google.android.material.button.MaterialButton btnConfirm = dialog.findViewById(R.id.btnConfirmPassword);
        TextView btnCancel = dialog.findViewById(R.id.btnCancelPassword);
        TextView tvError = dialog.findViewById(R.id.tvPasswordError);

        btnConfirm.setOnClickListener(v -> {
            String entered = etPassword.getText().toString().trim();
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
            }
        });
        btnCancel.setOnClickListener(v -> dialog.dismiss());
        dialog.show();
    }

    private ColorStateList createNavColorStateList() {
        int[][] states = new int[][] { new int[] { android.R.attr.state_checked }, new int[] { -android.R.attr.state_checked } };
        int[] colors = new int[] { Color.parseColor("#1E5F8B"), Color.parseColor("#2C3E50") };
        return new ColorStateList(states, colors);
    }

    private android.graphics.drawable.Drawable createNavItemBackground() {
        android.graphics.drawable.StateListDrawable stateList = new android.graphics.drawable.StateListDrawable();
        android.graphics.drawable.GradientDrawable selected = new android.graphics.drawable.GradientDrawable();
        selected.setColor(Color.parseColor("#1A1E5F8B"));
        selected.setCornerRadius(50f);
        stateList.addState(new int[]{ android.R.attr.state_checked }, selected);
        stateList.addState(new int[]{}, new ColorDrawable(Color.TRANSPARENT));
        return stateList;
    }
}