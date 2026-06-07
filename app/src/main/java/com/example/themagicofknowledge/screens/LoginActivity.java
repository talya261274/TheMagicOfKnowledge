// ===== הצהרת חבילה =====
// screens - חבילת המסכים של האפליקציה (כל ה-Activities)
package com.example.themagicofknowledge.screens;
import android.content.Intent;

// Bundle - "חבילה" של נתונים שאנדרואיד שומר בין מצבי האפליקציה
// (למשל בעת סיבוב המסך, ה-Activity נוצר מחדש וה-Bundle נשמר)
import android.os.Bundle;

// Log - לכתיבת הודעות לוג שאפשר לראות ב-Logcat של Android Studio
import android.util.Log;

// View - הבסיס לכל רכיב UI (כפתור, טקסט, תמונה וכו')
import android.view.View;

// רכיבי UI שאנחנו משתמשים בהם
import android.widget.Button;       // כפתור
import android.widget.EditText;     // שדה הזנת טקסט
import android.widget.TextView;     // טקסט להצגה (לא לעריכה)

// EdgeToEdge - תכונה חדשה של אנדרואיד שמאפשרת לאפליקציה
// לתפוס את כל המסך כולל אזור הסטטוס בר וניווט בר
import androidx.activity.EdgeToEdge;

// Insets ו-WindowInsetsCompat - עוזרים לטפל ב"אזורים שמורים" של המסך
// (סטטוס בר למעלה, ניווט בר למטה) כדי שהתוכן לא ייחתך
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

// R - מחלקה שנוצרת אוטומטית ע"י Android Studio
// היא מכילה הפניות לכל המשאבים: layouts, strings, drawables, ids וכו'
import com.example.themagicofknowledge.R;

// המחלקות שלנו - שראינו כבר!
import com.example.themagicofknowledge.models.UserParent;        // המודל
import com.example.themagicofknowledge.services.DatabaseService; // שירות Firebase
import com.example.themagicofknowledge.utils.SharedPreferencesUtil; // שמירה מקומית
import com.example.themagicofknowledge.utils.Validator;          // בדיקת תקינות

public class LoginActivity extends BaseActivity implements View.OnClickListener {

    private static final String TAG = "LoginActivity";
    private EditText etUName, etPassword;
    private Button btnLogin;

    @Override
    protected boolean showToolbar() {
        return false;
    }

    @Override
    protected boolean hasSideMenu() {
        return false;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_login);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.login), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);

            return insets;
        });

        Button btnGoBack = findViewById(R.id.goBackBtn);
        btnGoBack.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(LoginActivity.this, LandingActivity.class);
                startActivity(intent);
            }
        });

        TextView tvForgotPassword = findViewById(R.id.tvForgotPassword);

        tvForgotPassword.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(LoginActivity.this, ForgotPasswordActivity.class);
                startActivity(intent);
            }
        });

        TextView tvSignUp = findViewById(R.id.tvSignUp);

        tvSignUp.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(LoginActivity.this, RegisterActivity.class);
                startActivity(intent);
            }
        });

        etUName = findViewById(R.id.et_login_user_name);
        etPassword = findViewById(R.id.et_login_password);
        btnLogin = findViewById(R.id.btn_login_login);
        btnLogin.setOnClickListener(this);
    }

    @Override
    public void onClick(View v) {
        if (v.getId() == btnLogin.getId()) {
            String UName = etUName.getText().toString() + "";
            String password = etPassword.getText().toString() + "";
            if (!checkInput(UName, password)) {
                return;
            }

            loginUser(UName, password);
        }
    }
    private boolean checkInput(String UName, String password) {
        if (!Validator.isUserNameValid(UName)) {
            etUName.setError("שם משתמש לא חוקי");
            etUName.requestFocus();
            return false;
        }

        if (!Validator.isPasswordValid(password)) {
            etPassword.setError("הסיסמה חייבת להיות באורך של לפחות 6 תווים");
            etPassword.requestFocus();
            return false;
        }

        return true;
    }
    private void loginUser(String UName, String password) {
        databaseService.getUserByUsernameAndPassword(UName, password,
                new DatabaseService.DatabaseCallback<UserParent>() {
                    @Override
                    public void onCompleted(UserParent user) {

                        if (user == null) {
                            etPassword.setError("שם משתמש או סיסמה לא חוקיים");
                            etPassword.requestFocus();
                            return;
                        }

                        SharedPreferencesUtil.saveUser(LoginActivity.this, user);

                        Intent mainIntent;
                        if (user.isAdmin()) {
                            mainIntent = new Intent(LoginActivity.this, AdminDashboardActivity.class);
                        } else {
                            mainIntent = new Intent(LoginActivity.this, SelectChildActivity.class);
                        }
                        mainIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);

                        startActivity(mainIntent);
                    }
                    @Override
                    public void onFailed(Exception e) {
                        Log.e(TAG, "onFailed: Failed to retrieve user data", e);

                        etPassword.setError("שם משתמש או סיסמה לא חוקיים");
                        etPassword.requestFocus();

                        SharedPreferencesUtil.signOutUser(LoginActivity.this);
                    }
                }
        );
    }
}