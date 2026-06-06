package com.example.themagicofknowledge.screens;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.example.themagicofknowledge.R;
import com.example.themagicofknowledge.models.UserParent;
import com.example.themagicofknowledge.services.DatabaseService;
import com.example.themagicofknowledge.utils.Validator;

import java.util.function.UnaryOperator;

public class ForgotPasswordActivity extends AppCompatActivity implements View.OnClickListener {

    private EditText etEmail, etNewPassword, etUsername;
    private Button btnUpdatePassword;
    private DatabaseService databaseService;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_forgot_password);

        // UI Insets
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        TextView tvBackToLogin = findViewById(R.id.tvBackToLogin);
        tvBackToLogin.setOnClickListener(v -> {
            Intent intent = new Intent(ForgotPasswordActivity.this, LoginActivity.class);
            startActivity(intent);
            finish();  // סוגר את המסך הזה
        });

        // כפתור חזרה
        Button btnGoBack = findViewById(R.id.goBackBtn4);
        btnGoBack.setOnClickListener(v -> finish());

        // Init services
        databaseService = DatabaseService.getInstance();

        // Init UI
        etEmail = findViewById(R.id.et_email);
        etUsername = ((com.google.android.material.textfield.TextInputLayout)
                findViewById(R.id.usernameInputLayout)).getEditText();
        etNewPassword = findViewById(R.id.et_new_password);
        btnUpdatePassword = findViewById(R.id.btn_update_password);

        btnUpdatePassword.setOnClickListener(this);
    }

    @Override
    public void onClick(View view) {
        if (view.getId() == R.id.btn_update_password) {
            updatePassword();
        }
    }

    private void updatePassword() {
        String email = etEmail.getText().toString().trim();
        String newPassword = etNewPassword.getText().toString().trim();

        if (!Validator.isEmailValid(email)) {
            etEmail.setError("נא הזן כתובת אימייל חוקית");
            etEmail.requestFocus();
            return;
        }

        if (!Validator.isPasswordValid(newPassword)) {
            etNewPassword.setError("הסיסמה חייבת להיות לפחות 6 תווים");
            etNewPassword.requestFocus();
            return;
        }

        String username = etUsername.getText().toString().trim();

        if (username.isEmpty()) {
            etUsername.setError("נא הזן שם משתמש");
            etUsername.requestFocus();
            return;
        }

        databaseService.getUserByEmail(email, new DatabaseService.DatabaseCallback<UserParent>() {
            @Override
            public void onCompleted(UserParent user) {
                if (user == null) {
                    etEmail.setError("האימייל לא נמצא");
                    return;
                }

                // ← בדיקה שגם שם המשתמש תואם
                if (!user.getUserName().equals(username)) {
                    etUsername.setError("שם המשתמש לא תואם לאימייל");
                    etUsername.requestFocus();
                    return;
                }

                // שניהם תואמים - עדכן סיסמה
                user.setPassword(newPassword);
                databaseService.updateUser(user.id, userParentServer -> {
                    if (userParentServer != null)
                        userParentServer.setPassword(newPassword);
                    return userParentServer;
                }, new DatabaseService.DatabaseCallback<UserParent>() {
                    @Override
                    public void onCompleted(UserParent u) {
                        Toast.makeText(ForgotPasswordActivity.this,
                                "הסיסמה עודכנה בהצלחה! ✅", Toast.LENGTH_SHORT).show();
                        Intent intent = new Intent(ForgotPasswordActivity.this, LoginActivity.class);
                        startActivity(intent);
                        finish();
                    }

                    @Override
                    public void onFailed(Exception e) {
                        Toast.makeText(ForgotPasswordActivity.this,
                                "שגיאה בעדכון הסיסמה", Toast.LENGTH_SHORT).show();
                    }
                });
            }

            @Override
            public void onFailed(Exception e) {
                Toast.makeText(ForgotPasswordActivity.this,
                        "שגיאה בקבלת המשתמש", Toast.LENGTH_SHORT).show();
            }
        });
    }
}
