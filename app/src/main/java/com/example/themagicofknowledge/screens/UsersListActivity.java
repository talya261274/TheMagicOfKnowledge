package com.example.themagicofknowledge.screens;

import android.app.AlertDialog;
import android.app.DatePickerDialog;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.themagicofknowledge.R;
import com.example.themagicofknowledge.adapter.UserAdapter;
import com.example.themagicofknowledge.models.UserParent;
import com.example.themagicofknowledge.services.DatabaseService;
import com.example.themagicofknowledge.utils.SharedPreferencesUtil;
import com.example.themagicofknowledge.utils.Validator;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.switchmaterial.SwitchMaterial;
import com.google.android.material.textfield.TextInputEditText;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

public class UsersListActivity extends BaseActivity {

    private static final String TAG = "UsersListActivity";
    private UserAdapter userAdapter;
    private TextView tvUserCount;
    private UserParent currentUser;
    private List<UserParent> allUsers = new ArrayList<>(); // רשימה מלאה
    private String currentFilter = "all"; // מצב סינון נוכחי

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // ===== הגנת הרשאה - רק מנהלים יכולים להיות כאן =====
        currentUser = SharedPreferencesUtil.getUser(this);
        if (currentUser == null || !currentUser.isAdmin()) {
            Toast.makeText(this, "אין לך הרשאה לצפות במסך זה", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_users_list);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        initViews();
        initSearch();
        setupRecyclerView();
        setupFab();
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadUsers();
    }


    // ============================================================
    // ===== אתחול מסך =====
    // ============================================================

    private void initViews() {
        tvUserCount = findViewById(R.id.tv_user_count);
    }

    private void setupRecyclerView() {
        RecyclerView usersList = findViewById(R.id.rv_users_list);
        usersList.setLayoutManager(new LinearLayoutManager(this));

        userAdapter = new UserAdapter(new UserAdapter.OnUserClickListener() {
            @Override
            public void onUserClick(UserParent user) {
                Intent intent = new Intent(UsersListActivity.this, UserProfileActivity.class);
                intent.putExtra("USER_ID", user.getId());
                startActivity(intent);
            }

            @Override
            public void onLongUserClick(UserParent user) {
                Log.d(TAG, "User long clicked: " + user);
            }

            @Override
            public void onMakeAdmin(UserParent user) {
                confirmAndUpdateAdminStatus(user, true);
            }

            @Override
            public void onRemoveAdmin(UserParent user) {
                confirmAndUpdateAdminStatus(user, false);
            }

            @Override
            public void onDeleteUser(UserParent user) {
                confirmAndDeleteUser(user);
            }
        });
        usersList.setAdapter(userAdapter);
    }

    private void setupFab() {
        View fabAddUser = findViewById(R.id.fabAddUser);
        if (fabAddUser != null) {
            fabAddUser.setOnClickListener(v -> showAddUserDialog());
        }
    }


    // ============================================================
    // ===== טעינה ומיון של רשימת משתמשים =====
    // ============================================================

    /**
     * טעינת רשימת המשתמשים מ-Firebase ומיון לפי מנהלים תחילה
     * הפונקציה הזאת נקראת בכל פעם שצריך לרענן את הרשימה
     */
    private void loadUsers() {
        databaseService.getUserList(new DatabaseService.DatabaseCallback<List<UserParent>>() {
            @Override
            public void onCompleted(List<UserParent> users) {
                sortUsersByAdminFirst(users);
                allUsers = users; // ← שמור רשימה מלאה
                runOnUiThread(() -> {
                    tvUserCount.setText(String.valueOf(users.size()));
                    userAdapter.setUserList(users);
                });
            }

            @Override
            public void onFailed(Exception e) {
                Log.e(TAG, "Failed to get users list", e);
                runOnUiThread(() -> tvUserCount.setText("שגיאה"));
            }
        });
    }

    /**
     * מיון הרשימה כך שהמנהלים יופיעו תחילה
     */
    private void sortUsersByAdminFirst(List<UserParent> users) {
        Collections.sort(users, (u1, u2) -> {
            // מנהלים תחילה
            if (u1.isAdmin() && !u2.isAdmin()) return -1;
            if (!u1.isAdmin() && u2.isAdmin()) return 1;
            return 0;
        });
    }


    // ============================================================
    // ===== פעולות על משתמש - הפיכה למנהל / הסרה =====
    // ============================================================

    private void confirmAndUpdateAdminStatus(UserParent user, boolean makeAdmin) {
        if (user == null || currentUser == null) return;

        // הגנה - אדמין לא יכול להוריד את עצמו
        if (!makeAdmin && Objects.equals(user.getId(), currentUser.getId())) {
            Toast.makeText(this, "לא ניתן להסיר הרשאה מעצמך", Toast.LENGTH_SHORT).show();
            return;
        }

        String title = makeAdmin ? "הפיכה למנהל" : "הסרת הרשאת מנהל";
        String message = makeAdmin
                ? "האם להפוך את " + user.getFirstName() + " למנהל?"
                : "האם להסיר את הרשאת המנהל מ-" + user.getFirstName() + "?";
        String confirmText = makeAdmin ? "הפוך למנהל" : "הסר הרשאה";
        int confirmColor = makeAdmin ? Color.parseColor("#FF9800") : Color.parseColor("#FF5252");

        showCustomDialog(title, message, confirmText, confirmColor, () ->
                updateAdminStatus(user, makeAdmin)
        );
    }

    private void updateAdminStatus(UserParent user, boolean makeAdmin) {
        databaseService.updateUser(user.getId(), userInDb -> {
            if (userInDb != null) {
                userInDb.setAdmin(makeAdmin);
            }
            return userInDb;
        }, new DatabaseService.DatabaseCallback<UserParent>() {
            @Override
            public void onCompleted(UserParent updatedUser) {
                runOnUiThread(() -> {
                    Toast.makeText(UsersListActivity.this,
                            makeAdmin ? "המשתמש הפך למנהל" : "הרשאת המנהל הוסרה",
                            Toast.LENGTH_SHORT).show();

                    // ⭐ טעינה מחדש - הרשימה תמוין מחדש אוטומטית
                    loadUsers();
                });
            }

            @Override
            public void onFailed(Exception e) {
                Log.e(TAG, "Failed to update admin status", e);
                runOnUiThread(() -> Toast.makeText(UsersListActivity.this,
                        "שגיאה בעדכון", Toast.LENGTH_SHORT).show());
            }
        });
    }


    // ============================================================
    // ===== פעולות על משתמש - מחיקה =====
    // ============================================================

    private void confirmAndDeleteUser(UserParent user) {
        if (user == null || currentUser == null) return;

        // הגנה - אדמין לא יכול למחוק את עצמו
        if (Objects.equals(user.getId(), currentUser.getId())) {
            Toast.makeText(this, "לא ניתן למחוק את המשתמש שלך עצמך", Toast.LENGTH_SHORT).show();
            return;
        }

        showCustomDialog(
                "מחיקת משתמש",
                "האם למחוק את " + user.getFirstName() + " " + user.getLastName() + "?\nכל הנתונים יימחקו לצמיתות!",
                "מחק לצמיתות",
                Color.parseColor("#FF5252"),
                () -> deleteUser(user)
        );
    }

    private void deleteUser(UserParent user) {
        databaseService.deleteUser(user.getId(), new DatabaseService.DatabaseCallback<Void>() {
            @Override
            public void onCompleted(Void object) {
                runOnUiThread(() -> {
                    Toast.makeText(UsersListActivity.this,
                            "המשתמש נמחק בהצלחה", Toast.LENGTH_SHORT).show();

                    // ⭐ טעינה מחדש - המשתמש ייעלם והמונה יתעדכן
                    loadUsers();
                });
            }

            @Override
            public void onFailed(Exception e) {
                Log.e(TAG, "Failed to delete user", e);
                runOnUiThread(() -> Toast.makeText(UsersListActivity.this,
                        "שגיאה במחיקה", Toast.LENGTH_SHORT).show());
            }
        });
    }


    // ============================================================
    // ===== הוספת משתמש חדש - דיאלוג =====
    // ============================================================

    private void showAddUserDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        View dialogView = getLayoutInflater().inflate(R.layout.dialog_add_user, null);
        builder.setView(dialogView);

        AlertDialog dialog = builder.create();
        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
        }

        // מציאת השדות
        TextInputEditText etFirstName = dialogView.findViewById(R.id.et_dialog_first_name);
        TextInputEditText etLastName = dialogView.findViewById(R.id.et_dialog_last_name);
        TextInputEditText etEmail = dialogView.findViewById(R.id.et_dialog_email);
        TextInputEditText etPhone = dialogView.findViewById(R.id.et_dialog_phone);
        TextInputEditText etBirthDate = dialogView.findViewById(R.id.et_dialog_birth_date);
        TextInputEditText etUsername = dialogView.findViewById(R.id.et_dialog_username);
        TextInputEditText etPassword = dialogView.findViewById(R.id.et_dialog_password);
        TextInputEditText etConfirmPassword = dialogView.findViewById(R.id.et_dialog_confirm_password);
        SwitchMaterial switchAdmin = dialogView.findViewById(R.id.switch_dialog_admin);
        MaterialButton btnSave = dialogView.findViewById(R.id.btn_dialog_save);
        MaterialButton btnCancel = dialogView.findViewById(R.id.btn_dialog_cancel);

        // בחירת תאריך לידה
        etBirthDate.setOnClickListener(v -> showDatePicker(etBirthDate));

        // שמירה
        btnSave.setOnClickListener(v -> handleAddUser(
                etFirstName, etLastName, etEmail, etPhone, etBirthDate,
                etUsername, etPassword, etConfirmPassword, switchAdmin, dialog
        ));

        // ביטול
        btnCancel.setOnClickListener(v -> dialog.dismiss());

        dialog.show();
    }

    private void showDatePicker(TextInputEditText etBirthDate) {
        final Calendar calendar = Calendar.getInstance();
        int year = calendar.get(Calendar.YEAR);
        int month = calendar.get(Calendar.MONTH);
        int day = calendar.get(Calendar.DAY_OF_MONTH);

        DatePickerDialog datePickerDialog = new DatePickerDialog(
                UsersListActivity.this,
                (view, selectedYear, selectedMonth, selectedDay) -> {
                    String date = String.format(Locale.getDefault(),
                            "%02d/%02d/%04d",
                            selectedDay, selectedMonth + 1, selectedYear);
                    etBirthDate.setText(date);
                },
                year, month, day
        );

        // מקסימום - לפני 16 שנה
        Calendar maxDate = Calendar.getInstance();
        maxDate.add(Calendar.YEAR, -16);
        datePickerDialog.getDatePicker().setMaxDate(maxDate.getTimeInMillis());

        datePickerDialog.show();
    }

    private void handleAddUser(
            TextInputEditText etFirstName, TextInputEditText etLastName,
            TextInputEditText etEmail, TextInputEditText etPhone,
            TextInputEditText etBirthDate, TextInputEditText etUsername,
            TextInputEditText etPassword, TextInputEditText etConfirmPassword,
            SwitchMaterial switchAdmin, AlertDialog dialog
    ) {
        // שליפת נתונים
        String firstName = etFirstName.getText().toString().trim();
        String lastName = etLastName.getText().toString().trim();
        String email = etEmail.getText().toString().trim();
        String phone = etPhone.getText().toString().trim();
        String birthDate = etBirthDate.getText().toString().trim();
        String username = etUsername.getText().toString().trim();
        String password = etPassword.getText().toString();
        String confirmPassword = etConfirmPassword.getText().toString();
        boolean isAdmin = switchAdmin.isChecked();

        // בדיקות קלט
        if (!Validator.isNameValid(firstName)) {
            etFirstName.setError("שם קצר מדי");
            etFirstName.requestFocus();
            return;
        }
        if (!Validator.isNameValid(lastName)) {
            etLastName.setError("שם קצר מדי");
            etLastName.requestFocus();
            return;
        }
        if (!Validator.isEmailValid(email)) {
            etEmail.setError("אימייל לא תקין");
            etEmail.requestFocus();
            return;
        }
        if (!Validator.isPhoneValid(phone)) {
            etPhone.setError("טלפון לא תקין");
            etPhone.requestFocus();
            return;
        }
        if (!Validator.isParentBirthDateValid(birthDate)) {
            etBirthDate.setError("תאריך לא תקין");
            etBirthDate.requestFocus();
            return;
        }
        if (!Validator.isUserNameValid(username)) {
            etUsername.setError("שם משתמש לא תקין");
            etUsername.requestFocus();
            return;
        }
        if (!Validator.isPasswordValid(password)) {
            etPassword.setError("סיסמה קצרה מדי");
            etPassword.requestFocus();
            return;
        }
        if (!password.equals(confirmPassword)) {
            etConfirmPassword.setError("סיסמאות לא תואמות");
            etConfirmPassword.requestFocus();
            return;
        }

        // יצירת ID חדש
        String newUserId = databaseService.generateUserId();
        if (newUserId == null) {
            Toast.makeText(this, "שגיאה ביצירת מזהה למשתמש", Toast.LENGTH_SHORT).show();
            return;
        }

        // יצירת אובייקט המשתמש
        UserParent newUser = new UserParent();
        newUser.setId(newUserId);
        newUser.setFirstName(firstName);
        newUser.setLastName(lastName);
        newUser.setEmail(email);
        newUser.setPhone(phone);
        newUser.setBirthDate(birthDate);
        newUser.setUserName(username);
        newUser.setPassword(password);
        newUser.setAdmin(isAdmin);

        // שמירה ל-Firebase
        databaseService.createNewUser(newUser, new DatabaseService.DatabaseCallback<Void>() {
            @Override
            public void onCompleted(Void result) {
                runOnUiThread(() -> {
                    Toast.makeText(UsersListActivity.this,
                            "משתמש נוצר בהצלחה!", Toast.LENGTH_SHORT).show();
                    dialog.dismiss();

                    // ⭐ טעינה מחדש - המשתמש החדש יופיע במיקום הנכון
                    loadUsers();
                });
            }

            @Override
            public void onFailed(Exception e) {
                runOnUiThread(() -> {
                    Log.e(TAG, "Error creating user", e);
                    Toast.makeText(UsersListActivity.this,
                            "שגיאה בשמירה: " + e.getMessage(), Toast.LENGTH_LONG).show();
                });
            }
        });
    }

    private void initSearch() {
        com.google.android.material.textfield.TextInputEditText etSearch = findViewById(R.id.etSearch);

        etSearch.addTextChangedListener(new android.text.TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {
                filterUsers(s.toString(), currentFilter);
            }
            @Override public void afterTextChanged(android.text.Editable s) {}
        });

        findViewById(R.id.btnFilterAll).setOnClickListener(v -> {
            currentFilter = "all";
            updateFilterButtons("all");
            filterUsers(etSearch.getText().toString(), "all");
        });

        findViewById(R.id.btnFilterAdmins).setOnClickListener(v -> {
            currentFilter = "admins";
            updateFilterButtons("admins");
            filterUsers(etSearch.getText().toString(), "admins");
        });

        findViewById(R.id.btnFilterUsers).setOnClickListener(v -> {
            currentFilter = "users";
            updateFilterButtons("users");
            filterUsers(etSearch.getText().toString(), "users");
        });
    }

    private void filterUsers(String query, String filter) {
        List<UserParent> filtered = new ArrayList<>();
        String lowerQuery = query.toLowerCase().trim();

        for (UserParent user : allUsers) {
            // סינון לפי תפקיד
            if (filter.equals("admins") && !user.isAdmin()) continue;
            if (filter.equals("users") && user.isAdmin()) continue;

            // סינון לפי חיפוש
            if (!lowerQuery.isEmpty()) {
                boolean matchName = (user.getFirstName() + " " + user.getLastName())
                        .toLowerCase().contains(lowerQuery);
                boolean matchEmail = user.getEmail() != null &&
                        user.getEmail().toLowerCase().contains(lowerQuery);
                boolean matchPhone = user.getPhone() != null &&
                        user.getPhone().contains(lowerQuery);
                if (!matchName && !matchEmail && !matchPhone) continue;
            }

            filtered.add(user);
        }

        tvUserCount.setText(String.valueOf(filtered.size()));
        userAdapter.setUserList(filtered);
    }

    private void updateFilterButtons(String active) {
        com.google.android.material.button.MaterialButton btnAll = findViewById(R.id.btnFilterAll);
        com.google.android.material.button.MaterialButton btnAdmins = findViewById(R.id.btnFilterAdmins);
        com.google.android.material.button.MaterialButton btnUsers = findViewById(R.id.btnFilterUsers);

        // איפוס כולם
        btnAll.setBackgroundTintList(android.content.res.ColorStateList.valueOf(
                active.equals("all") ? android.graphics.Color.parseColor("#1E5F8B") : android.graphics.Color.TRANSPARENT));
        btnAll.setTextColor(active.equals("all") ? android.graphics.Color.WHITE : android.graphics.Color.parseColor("#1E5F8B"));

        btnAdmins.setBackgroundTintList(android.content.res.ColorStateList.valueOf(
                active.equals("admins") ? android.graphics.Color.parseColor("#FF9800") : android.graphics.Color.TRANSPARENT));
        btnAdmins.setTextColor(active.equals("admins") ? android.graphics.Color.WHITE : android.graphics.Color.parseColor("#FF9800"));

        btnUsers.setBackgroundTintList(android.content.res.ColorStateList.valueOf(
                active.equals("users") ? android.graphics.Color.parseColor("#1E5F8B") : android.graphics.Color.TRANSPARENT));
        btnUsers.setTextColor(active.equals("users") ? android.graphics.Color.WHITE : android.graphics.Color.parseColor("#1E5F8B"));
    }
}