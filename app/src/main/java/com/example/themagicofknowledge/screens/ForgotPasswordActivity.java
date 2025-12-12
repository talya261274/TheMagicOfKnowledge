package com.example.themagicofknowledge.screens;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
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

public class ForgotPasswordActivity extends AppCompatActivity implements View.OnClickListener {

    private EditText etEmail, etNewPassword;
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

        // כפתור חזרה
        Button btnGoBack = findViewById(R.id.goBackBtn4);
        btnGoBack.setOnClickListener(v -> finish());

        // Init services
        databaseService = DatabaseService.getInstance();

        // Init UI
        etEmail = findViewById(R.id.et_email);
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

        // בדיקות תקינות
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

        // מקבל משתמש לפי אימייל
        databaseService.getUserByEmail(email, new DatabaseService.DatabaseCallback<UserParent>() {
            @Override
            public void onCompleted(UserParent user) {
                if (user == null) {
                    etEmail.setError("האימייל לא נמצא");
                    etEmail.requestFocus();
                    Toast.makeText(ForgotPasswordActivity.this,
                            "לא נמצא חשבון עם האימייל הזה",
                            Toast.LENGTH_SHORT
                    ).show();
                    return;
                }

                // מעדכן סיסמה
                user.setPassword(newPassword);
                databaseService.updateUser(user, new DatabaseService.DatabaseCallback<Void>() {
                    @Override
                    public void onCompleted(Void result) {
                        Toast.makeText(ForgotPasswordActivity.this,
                                "הסיסמה עודכנה בהצלחה!",
                                Toast.LENGTH_SHORT
                        ).show();

                        // מעבר לעמוד עדכון פרטים
                        Intent intent = new Intent(ForgotPasswordActivity.this, UpdateDetailsActivity.class);
                        intent.putExtra("USER_ID", user.getId()); // מעביר את ה-ID של המשתמש
                        startActivity(intent);
                        finish();
                    }

                    @Override
                    public void onFailed(Exception e) {
                        Toast.makeText(ForgotPasswordActivity.this,
                                "לא ניתן לעדכן את הסיסמה. נסי שוב.",
                                Toast.LENGTH_SHORT
                        ).show();
                    }
                });
            }

            @Override
            public void onFailed(Exception e) {
                Toast.makeText(ForgotPasswordActivity.this,
                        "שגיאה בקבלת המשתמש",
                        Toast.LENGTH_SHORT
                ).show();
            }
        });
    }
}
