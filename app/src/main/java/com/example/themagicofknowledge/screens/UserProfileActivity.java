package com.example.themagicofknowledge.screens;

import android.app.Dialog;
import android.content.Intent;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.cardview.widget.CardView;

import com.example.themagicofknowledge.R;
import com.example.themagicofknowledge.models.UserChild;
import com.example.themagicofknowledge.models.UserParent;
import com.example.themagicofknowledge.models.UserRole;
import com.example.themagicofknowledge.services.DatabaseService;
import com.example.themagicofknowledge.utils.SharedPreferencesUtil;
import com.example.themagicofknowledge.utils.Validator;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.database.FirebaseDatabase;

public class UserProfileActivity extends BaseActivity {

    private static final String TAG = "UserProfileActivity";

    private TextInputEditText etFirstName, etLastName, etEmail, etPhone, etBirthDate, etPassword;
    private TextView tvUserName, tvId;
    private FloatingActionButton btnUpdateAction;
    private LinearLayout containerChildrenLinks;
    private MaterialButton btnSignOut;
    private FrameLayout backButtonContainer;
    private CardView childrenTrackingCard;

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

        // ⭐ הכפתור מוצג רק למנהלים (בכל מקרה - שלהם או של אחרים)
        if (loggedInUser != null && loggedInUser.isAdmin()) {
            backButtonContainer.setVisibility(View.VISIBLE);
        } else {
            backButtonContainer.setVisibility(View.GONE);
        }

        btnSignOut.setOnClickListener(v -> showLogoutDialog());

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
        childrenTrackingCard = findViewById(R.id.childrenTrackingCard);
        backButtonContainer = findViewById(R.id.backButtonContainer);  // ← בדיקה!

        findViewById(R.id.btnBackToUsersList).setOnClickListener(v -> finish());

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

        // ===== הצגת תג מנהל =====
        androidx.cardview.widget.CardView adminBadge = findViewById(R.id.admin_badge);
        if (currentUser.isAdmin()) {
            adminBadge.setVisibility(View.VISIBLE);
        } else {
            adminBadge.setVisibility(View.GONE);
        }

        // ===== הסתרת מעקב ילדים למנהלים =====
        if (currentUser.isAdmin()) {
            // מנהל - מסתירים את הכרטיס לגמרי
            childrenTrackingCard.setVisibility(View.GONE);
        } else {
            // הורה רגיל - מציגים ומאתחלים את הילדים
            childrenTrackingCard.setVisibility(View.VISIBLE);
            setupChildrenProgressLinks();
        }
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
            btnUpdateAction.setImageResource(R.drawable.ic_check_circle);
            btnUpdateAction.setBackgroundTintList(android.content.res.ColorStateList.valueOf(Color.parseColor("#4CAF50")));
            etFirstName.requestFocus();
        } else {
            btnUpdateAction.setImageResource(R.drawable.ic_edit_square);
            btnUpdateAction.setBackgroundTintList(android.content.res.ColorStateList.valueOf(Color.parseColor("#FF9800")));
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

        LayoutInflater inflater = LayoutInflater.from(this);

        for (UserChild child : currentUser.getChildrenList().values()) {
            // יצירת view מהlayout שיצרנו
            View childView = inflater.inflate(R.layout.item_child_carousel, containerChildrenLinks, false);

            // חיבור הרכיבים
            ImageView ivAvatar = childView.findViewById(R.id.iv_child_avatar);
            TextView tvName = childView.findViewById(R.id.tv_child_name);
            TextView tvAge = childView.findViewById(R.id.tv_child_age);

            // הצגת שם וגיל
            tvName.setText(child.getName());
            tvAge.setText("גיל " + child.getAge());

            // הצגת אווטר
            if (child.getAvatar() != null) {
                int resId = getResources().getIdentifier(child.getAvatar(), "drawable", getPackageName());
                ivAvatar.setImageResource(resId != 0 ? resId : R.drawable.logo);
            }

            // לחיצה - מעבר למסך מעקב הילד
            childView.setOnClickListener(v -> {
                Intent intent = new Intent(UserProfileActivity.this, ParentTrackingActivity.class);
                intent.putExtra("SELECTED_CHILD_ID", child.getId());

                // אם צופים בפרופיל של משתמש אחר - שולחים גם את ה-parentId
                if (!isViewingOwnProfile) {
                    intent.putExtra("PARENT_ID", currentUser.getId());
                }
                startActivity(intent);
            });

            containerChildrenLinks.addView(childView);
        }
    }

    // ===== דיאלוג מותאם אישית =====
    public void showCustomDialog(String title, String message, String confirmText, int confirmColor, Runnable onConfirm) {
        Dialog dialog = new Dialog(this);
        dialog.setContentView(R.layout.custom_action_dialog);

        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        }

        TextView tvTitle = dialog.findViewById(R.id.tvDialogTitle);
        TextView tvMessage = dialog.findViewById(R.id.tvDialogMessage);
        MaterialButton btnConfirm = dialog.findViewById(R.id.btnConfirm);
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


    // ===== דיאלוג התנתקות =====
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