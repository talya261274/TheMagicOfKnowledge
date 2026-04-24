package com.example.themagicofknowledge.screens;

import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.view.Gravity;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.example.themagicofknowledge.R;
import com.example.themagicofknowledge.models.UserChild;
import com.example.themagicofknowledge.models.UserParent;
import com.example.themagicofknowledge.utils.SharedPreferencesUtil;
import com.example.themagicofknowledge.utils.Validator;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.database.FirebaseDatabase;

public class UserProfileActivity extends BaseActivity {

    private static final String TAG = "UserProfileActivity";

    // רכיבי ממשק
    private TextInputEditText etFirstName, etLastName, etEmail, etPhone, etBirthDate, etPassword;
    private TextView tvUserName, tvId, btnUpdateAction;
    private LinearLayout containerChildrenLinks;
    private MaterialButton btnSignOut;

    private UserParent currentUser;
    private boolean isEditMode = false; // האם אנחנו כרגע במצב עריכה?

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_user_profile);

        initViews();
        loadUserData();

        // כפתור התנתקות
        btnSignOut.setOnClickListener(v -> {
            SharedPreferencesUtil.signOutUser(this);
            Intent intent = new Intent(UserProfileActivity.this, LandingActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
        });

        // כפתור "עדכון פרטים" שהופך ל-"שמור שינויים"
        btnUpdateAction.setOnClickListener(v -> {
            if (!isEditMode) {
                setEditMode(true);
            } else {
                validateAndSave();
            }
        });
    }

    private void initViews() {
        tvUserName = findViewById(R.id.tv_user_display_name);
        tvId = findViewById(R.id.tv_user_display_id);
        etFirstName = findViewById(R.id.et_user_first_name);
        etLastName = findViewById(R.id.et_user_last_name);
        etEmail = findViewById(R.id.et_user_email);
        etPhone = findViewById(R.id.et_user_phone);
        etBirthDate = findViewById(R.id.et_user_birth_date);
        etPassword = findViewById(R.id.et_user_password);
        btnSignOut = findViewById(R.id.btn_sign_out);
        btnUpdateAction = findViewById(R.id.et_update); // הטקסט של ה"עדכון"
        containerChildrenLinks = findViewById(R.id.container_children_links);
    }

    private void loadUserData() {
        currentUser = SharedPreferencesUtil.getUser(this);
        if (currentUser != null) {
            tvUserName.setText(currentUser.getUserName());
            tvId.setText(currentUser.getId());
            etFirstName.setText(currentUser.getFirstName());
            etLastName.setText(currentUser.getLastName());
            etEmail.setText(currentUser.getEmail());
            etPhone.setText(currentUser.getPhone());
            etBirthDate.setText(currentUser.getBirthDate());
            etPassword.setText(currentUser.getPassword());

            setupChildrenProgressLinks();
        }
    }

    // פונקציה שפותחת או נועלת את השדות לעריכה
    private void setEditMode(boolean enable) {
        isEditMode = enable;
        etFirstName.setEnabled(enable);
        etLastName.setEnabled(enable);
        etEmail.setEnabled(enable);
        etPhone.setEnabled(enable);
        etBirthDate.setEnabled(enable);
        etPassword.setEnabled(enable);

        if (enable) {
            btnUpdateAction.setText("שמור שינויים");
            btnUpdateAction.setTextColor(getResources().getColor(android.R.color.holo_green_dark));
            etFirstName.requestFocus();
        } else {
            btnUpdateAction.setText("עדכון פרטים");
            btnUpdateAction.setTextColor(getResources().getColor(R.color.update));
        }
    }

    private void validateAndSave() {
        String fName = etFirstName.getText().toString().trim();
        String lName = etLastName.getText().toString().trim();
        String email = etEmail.getText().toString().trim();
        String phone = etPhone.getText().toString().trim();
        String bDate = etBirthDate.getText().toString().trim();
        String pass = etPassword.getText().toString().trim();

        // שימוש ב-Validator (כמו ב-UpdateDetails המקורי שלך)
        if (!Validator.isNameValid(fName)) {
            etFirstName.setError("שם לא תקין");
            return;
        }
        if (!Validator.isEmailValid(email)) {
            etEmail.setError("אימייל לא תקין");
            return;
        }
        if (!Validator.isPasswordValid(pass)) {
            etPassword.setError("סיסמה קצרה מדי");
            return;
        }

        // עדכון האובייקט
        currentUser.setFirstName(fName);
        currentUser.setLastName(lName);
        currentUser.setEmail(email);
        currentUser.setPhone(phone);
        currentUser.setBirthDate(bDate);
        currentUser.setPassword(pass);

        // שמירה ב-Firebase
        FirebaseDatabase.getInstance().getReference("users")
                .child(currentUser.getId())
                .setValue(currentUser)
                .addOnSuccessListener(aVoid -> {
                    SharedPreferencesUtil.saveUser(this, currentUser);
                    setEditMode(false);
                    Toast.makeText(this, "הפרופיל עודכן בהצלחה!", Toast.LENGTH_SHORT).show();
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "שגיאה בעדכון" + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }

    private void setupChildrenProgressLinks() {
        if (currentUser.getChildrenList() == null) return;
        containerChildrenLinks.removeAllViews();
        containerChildrenLinks.setOrientation(LinearLayout.HORIZONTAL); // רשימה אופקית אם תרצי

        for (UserChild child : currentUser.getChildrenList().values()) {
            // ניצור Layout קטן לכל ילד (אייקון מעל שם)
            LinearLayout childLayout = new LinearLayout(this);
            childLayout.setOrientation(LinearLayout.VERTICAL);
            childLayout.setPadding(20, 20, 20, 20);
            childLayout.setGravity(Gravity.CENTER);

            // תמונת האוואטר
            ImageView ivAvatar = new ImageView(this);
            ivAvatar.setLayoutParams(new LinearLayout.LayoutParams(150, 150));

            // כאן את שמה את הלוגיקה שלך לבחירת האייקון לפי מה ששמור ב-child.getAvatar()
            int resId = getResources().getIdentifier(child.getAvatar(), "drawable", getPackageName());
            ivAvatar.setImageResource(resId != 0 ? resId : R.drawable.logo);

            // שם הילד
            TextView tvName = new TextView(this);
            tvName.setText(child.getName());
            tvName.setGravity(Gravity.CENTER);
            tvName.setTextColor(Color.BLACK);
            tvName.setTextSize(Color.BLACK);


            childLayout.addView(ivAvatar);
            childLayout.addView(tvName);

            // לחיצה למעבר לעמוד ההתקדמות
            childLayout.setOnClickListener(v -> {
                Intent intent = new Intent(UserProfileActivity.this, ParentTrackingActivity.class);

                // זה החלק הכי חשוב! להעביר את ה-ID הייחודי של הילד
                intent.putExtra("SELECTED_CHILD_ID", child.getId());

                startActivity(intent);
            });

            containerChildrenLinks.addView(childLayout);
        }
    }
}