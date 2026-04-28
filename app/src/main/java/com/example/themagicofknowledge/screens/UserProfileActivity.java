package com.example.themagicofknowledge.screens;

import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.example.themagicofknowledge.R;
import com.example.themagicofknowledge.models.UserChild;
import com.example.themagicofknowledge.models.UserParent;
import com.example.themagicofknowledge.models.UserRole;
import com.example.themagicofknowledge.services.DatabaseService;
import com.example.themagicofknowledge.utils.SharedPreferencesUtil;
import com.example.themagicofknowledge.utils.Validator;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.database.FirebaseDatabase;

public class UserProfileActivity extends BaseActivity {

    private static final String TAG = "UserProfileActivity";

    private TextInputEditText etFirstName, etLastName, etEmail, etPhone, etBirthDate, etPassword;
    private TextView tvUserName, tvId, btnUpdateAction;
    private LinearLayout containerChildrenLinks;
    private MaterialButton btnSignOut;

    private UserParent currentUser;       // המשתמש שמוצג בפרופיל (אולי לא המחובר)
    private UserParent loggedInUser;      // המשתמש המחובר (לבדיקת הרשאות)
    private boolean isViewingOwnProfile = true;
    private boolean isEditMode = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // הגנה - ילדים לא רואים פרופיל
        if (UserRole.isChild(this)) {
            Toast.makeText(this, "מסך זה אינו זמין", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        setContentView(R.layout.activity_user_profile);

        initViews();

        // 1. בדיקה אם הגיע USER_ID דרך ה-Intent (כלומר נכנסנו מרשימת משתמשים)
        String requestedUserId = getIntent().getStringExtra("USER_ID");
        loggedInUser = SharedPreferencesUtil.getUser(this);

        if (requestedUserId != null && loggedInUser != null
                && !requestedUserId.equals(loggedInUser.getId())) {
            // צופים בפרופיל של מישהו אחר - טוענים מ-Firebase
            isViewingOwnProfile = false;
            loadUserFromFirebase(requestedUserId);
        } else {
            // המשתמש המחובר צופה בפרופיל שלו עצמו
            isViewingOwnProfile = true;
            currentUser = loggedInUser;
            populateUI();
        }

        btnSignOut.setOnClickListener(v -> {
            SharedPreferencesUtil.signOutUser(this);
            Intent intent = new Intent(UserProfileActivity.this, LandingActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
        });

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
        btnUpdateAction = findViewById(R.id.et_update);
        containerChildrenLinks = findViewById(R.id.container_children_links);
    }

    private void loadUserFromFirebase(String userId) {
        DatabaseService.getInstance().getUser(userId, new DatabaseService.DatabaseCallback<UserParent>() {
            @Override
            public void onCompleted(UserParent user) {
                if (user != null) {
                    currentUser = user;
                    populateUI();
                } else {
                    Toast.makeText(UserProfileActivity.this, "המשתמש לא נמצא", Toast.LENGTH_SHORT).show();
                    finish();
                }
            }

            @Override
            public void onFailed(Exception e) {
                Toast.makeText(UserProfileActivity.this, "שגיאה בטעינת המשתמש", Toast.LENGTH_SHORT).show();
                finish();
            }
        });
    }

    private void populateUI() {
        if (currentUser == null) return;

        tvUserName.setText(currentUser.getUserName());
        tvId.setText(currentUser.getId());
        etFirstName.setText(currentUser.getFirstName());
        etLastName.setText(currentUser.getLastName());
        etEmail.setText(currentUser.getEmail());
        etPhone.setText(currentUser.getPhone());
        etBirthDate.setText(currentUser.getBirthDate());
        etPassword.setText(currentUser.getPassword());

        if (!isViewingOwnProfile) {
            btnSignOut.setVisibility(View.GONE);
        }

        setupChildrenProgressLinks();
    }

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

        currentUser.setFirstName(fName);
        currentUser.setLastName(lName);
        currentUser.setEmail(email);
        currentUser.setPhone(phone);
        currentUser.setBirthDate(bDate);
        currentUser.setPassword(pass);

        FirebaseDatabase.getInstance().getReference("users")
                .child(currentUser.getId())
                .setValue(currentUser)
                .addOnSuccessListener(aVoid -> {
                    // אם זה המשתמש המחובר - מעדכנים גם את ה-SharedPreferences
                    if (isViewingOwnProfile) {
                        SharedPreferencesUtil.saveUser(this, currentUser);
                    }
                    setEditMode(false);
                    Toast.makeText(this, "הפרופיל עודכן בהצלחה!", Toast.LENGTH_SHORT).show();
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "שגיאה בעדכון " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }

    private void setupChildrenProgressLinks() {
        if (currentUser.getChildrenList() == null) return;
        containerChildrenLinks.removeAllViews();
        containerChildrenLinks.setOrientation(LinearLayout.HORIZONTAL);

        for (UserChild child : currentUser.getChildrenList().values()) {
            LinearLayout childLayout = new LinearLayout(this);
            childLayout.setOrientation(LinearLayout.VERTICAL);
            childLayout.setPadding(20, 20, 20, 20);
            childLayout.setGravity(Gravity.CENTER);

            ImageView ivAvatar = new ImageView(this);
            ivAvatar.setLayoutParams(new LinearLayout.LayoutParams(150, 150));

            int resId = getResources().getIdentifier(child.getAvatar(), "drawable", getPackageName());
            ivAvatar.setImageResource(resId != 0 ? resId : R.drawable.logo);

            TextView tvName = new TextView(this);
            tvName.setText(child.getName());
            tvName.setGravity(Gravity.CENTER);
            tvName.setTextColor(Color.BLACK);
            tvName.setTextSize(16);

            childLayout.addView(ivAvatar);
            childLayout.addView(tvName);

            childLayout.setOnClickListener(v -> {
                Intent intent = new Intent(UserProfileActivity.this, ParentTrackingActivity.class);
                intent.putExtra("SELECTED_CHILD_ID", child.getId());
                // אם צופים בפרופיל של משתמש אחר - שולחים גם את ה-parentId
                if (!isViewingOwnProfile) {
                    intent.putExtra("PARENT_ID", currentUser.getId());
                }
                startActivity(intent);
            });

            containerChildrenLinks.addView(childLayout);
        }
    }
}