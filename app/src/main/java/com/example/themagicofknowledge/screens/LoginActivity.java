// ===== הצהרת חבילה =====
// screens - חבילת המסכים של האפליקציה (כל ה-Activities)
package com.example.themagicofknowledge.screens;


// ===== Imports =====

// Intent - "כוונה" - אובייקט שמשמש למעבר בין מסכים
// או להפעלת פעולות (כמו פתיחת מצלמה, שליחת SMS וכו')
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


/// Activity for logging in the user
// ===== הצהרת המחלקה =====
// extends BaseActivity - יורש מ-BaseActivity (שיורש מ-AppCompatActivity)
//                       ככה כל המסכים שלנו חולקים פונקציונליות משותפת (תפריט צד וכו')
//
// implements View.OnClickListener - "מקיים את החוזה" של מאזין ללחיצות
//                                    זה מחייב אותנו לכתוב פונקציה onClick()
public class LoginActivity extends BaseActivity implements View.OnClickListener {

    // ===== קבועים =====

    // TAG לזיהוי הודעות log שמגיעות מהמסך הזה
    private static final String TAG = "LoginActivity";


    // ===== משתנים לרכיבי ה-UI =====

    // EditText - שדות שהמשתמש מקליד בהם
    // etUName = שם משתמש, etPassword = סיסמה
    // (et = מקצור של editText, מוסכמה לשמות)
    private EditText etUName, etPassword;

    // הכפתור "התחבר"
    private Button btnLogin;


    // ===== Override של פונקציות מ-BaseActivity =====

    // showToolbar() - האם להציג את ה-Toolbar העליון?
    // החזרנו false כי במסך התחברות לא רוצים אותו
    @Override
    protected boolean showToolbar() {
        return false;
    }

    // hasSideMenu() - האם יש תפריט צד?
    // החזרנו false כי לא רוצים תפריט צד במסך התחברות
    // (איך תפתחי את התפריט אם עדיין לא התחברת?)
    @Override
    protected boolean hasSideMenu() {
        return false;
    }


    // ===== הפונקציה הראשית של ה-Activity =====
    // onCreate() נקראת ברגע שה-Activity נוצר
    // זה המקום שבו מאתחלים את הכל
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        // קריאה לפונקציה של ההורה (BaseActivity)
        // זה הכרחי - אם נשכח, האפליקציה תקרוס
        super.onCreate(savedInstanceState);

        // הפעלת מצב Edge-to-Edge
        EdgeToEdge.enable(this);

        // ===== הגדרת ה-Layout =====
        // setContentView - אומר לאנדרואיד "טען את ה-XML הזה והצג אותו"
        // R.layout.activity_login = ההפניה לקובץ activity_login.xml
        setContentView(R.layout.activity_login);

        // ===== טיפול ב-Insets (אזורים שמורים) =====
        // setOnApplyWindowInsetsListener - מאזין לשינויים באזורים השמורים
        // findViewById(R.id.login) - מוצא את ה-View עם id="login"
        // (v, insets) -> {...} - Lambda שמופעל עם השינוי
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.login), (v, insets) -> {
            // קבלת הגודל של האזורים השמורים (סטטוס בר וכו')
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());

            // הוספת padding ל-View כדי שלא ייחתך מאחורי האזורים האלה
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);

            return insets;
        });

        // ===== כפתור חזרה למסך הקודם =====
        // findViewById - מוצאים את הכפתור ב-layout
        Button btnGoBack = findViewById(R.id.goBackBtn);

        // הוספת מאזין ללחיצה
        // setOnClickListener - "כשלוחצים, תפעיל את הקוד הזה"
        btnGoBack.setOnClickListener(new View.OnClickListener() {
            // אנחנו יוצרים מחלקה אנונימית (anonymous class)
            // היא מקיימת את ה-interface View.OnClickListener
            @Override
            public void onClick(View view) {
                // יצירת Intent למסך LandingActivity
                // Intent מצריך את ה-Context (this) ואת המסך היעד
                Intent intent = new Intent(LoginActivity.this, LandingActivity.class);

                // startActivity - "פתח את המסך הזה"
                startActivity(intent);
            }
        });

        // ===== כפתור "שכחתי סיסמה" =====
        TextView tvForgotPassword = findViewById(R.id.tvForgotPassword);

        tvForgotPassword.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(LoginActivity.this, ForgotPasswordActivity.class);
                startActivity(intent);
            }
        });

        // ===== כפתור "הירשמו עכשיו" =====
        TextView tvSignUp = findViewById(R.id.tvSignUp);

        tvSignUp.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(LoginActivity.this, RegisterActivity.class);
                startActivity(intent);
            }
        });

        // ===== חיבור הרכיבים מה-XML למשתנים שלנו =====
        // findViewById מקבל id ומחזיר את ה-View הספציפי
        // ה-IDs האלה מוגדרים ב-activity_login.xml
        etUName = findViewById(R.id.et_login_user_name);
        etPassword = findViewById(R.id.et_login_password);
        btnLogin = findViewById(R.id.btn_login_login);

        // ===== הגדרת מאזין ללחיצה על כפתור ההתחברות =====
        // this = "המסך הזה" (כי הוא implements View.OnClickListener)
        // כשהמשתמש ילחץ - תיקרא הפונקציה onClick() שלנו
        btnLogin.setOnClickListener(this);
    }


    // ===== פונקציית onClick - חובה כי implements View.OnClickListener =====
    // הפונקציה הזו נקראת בכל לחיצה על רכיב שהאזין ל-this
    @Override
    public void onClick(View v) {
        // v.getId() - מחזיר את ה-id של הרכיב שלחצנו עליו
        // אנחנו בודקים אם זה כפתור ההתחברות
        if (v.getId() == btnLogin.getId()) {
            Log.d(TAG, "onClick: Login button clicked");

            // ===== קריאת הקלט מהמשתמש =====
            // .getText() - מחזיר Editable
            // .toString() - הופך ל-String
            // + "" - תוספת מיותרת (נשארה מהקוד המקורי), אפשר למחוק
            String UName = etUName.getText().toString() + "";
            String password = etPassword.getText().toString() + "";

            // לוג של הקלט (זה לא בטוח להציג סיסמה בלוג בקוד אמיתי!)
            Log.d(TAG, "onClick: User Name: " + UName);
            Log.d(TAG, "onClick: Password: " + password);

            Log.d(TAG, "onClick: Validating input...");

            // ===== בדיקת תקינות =====
            // אם הקלט לא תקין - יוצאים מהפונקציה
            if (!checkInput(UName, password)) {
                // ! לפני התנאי = "לא"
                // אם checkInput מחזיר false (לא תקין), אנחנו עוצרים
                return;
            }

            Log.d(TAG, "onClick: Logging in user...");

            // ===== ניסיון התחברות =====
            // אם הקלט תקין - ניסה להתחבר
            loginUser(UName, password);
        }
    }


    // ===== בדיקת תקינות הקלט =====
    // משתמשים ב-Validator שראינו!
    private boolean checkInput(String UName, String password) {

        // ===== בדיקת שם משתמש =====
        // !Validator.isUserNameValid(UName) = "אם שם המשתמש לא תקין"
        if (!Validator.isUserNameValid(UName)) {
            Log.e(TAG, "checkInput: Invalid user name");

            // setError - מציג הודעת שגיאה אדומה ליד השדה
            etUName.setError("שם משתמש לא חוקי");

            // requestFocus - שם את הסמן (focus) על השדה
            // ככה המשתמש יודע איפה השגיאה
            etUName.requestFocus();

            return false; // הקלט לא תקין
        }

        // ===== בדיקת סיסמה =====
        if (!Validator.isPasswordValid(password)) {
            Log.e(TAG, "checkInput: Invalid password");
            etPassword.setError("הסיסמה חייבת להיות באורך של לפחות 6 תווים");
            etPassword.requestFocus();
            return false;
        }

        // אם הגענו לכאן - הכל תקין
        return true;
    }


    // ===== פונקציית ההתחברות =====
    // משתמשת ב-DatabaseService שראינו!
    private void loginUser(String UName, String password) {

        // databaseService - שייך ל-BaseActivity (ההורה שלנו) שכבר אתחל אותו
        // קריאה לפונקציה getUserByUsernameAndPassword שראינו ב-DatabaseService
        databaseService.getUserByUsernameAndPassword(UName, password,
                // יוצרים אובייקט callback אנונימי (mini-class)
                new DatabaseService.DatabaseCallback<UserParent>() {

                    // ===== מה לעשות אם הקריאה הצליחה =====
                    @Override
                    public void onCompleted(UserParent user) {

                        // אם user == null - לא נמצא משתמש עם הפרטים האלה
                        if (user == null) {
                            // הצגת שגיאה
                            etPassword.setError("שם משתמש או סיסמה לא חוקיים");
                            etPassword.requestFocus();
                            return; // יציאה מהפונקציה
                        }

                        Log.d(TAG, "onCompleted: User logged in: " + user);

                        // ===== שמירת המשתמש המחובר ב-SharedPreferences =====
                        // משתמשים ב-SharedPreferencesUtil שראינו!
                        // ככה האפליקציה תזכור את המשתמש בפעם הבאה שיפתחו אותה
                        SharedPreferencesUtil.saveUser(LoginActivity.this, user);

                        // ===== החלטה לאן להפנות לפי תפקיד =====
                        Intent mainIntent;
                        if (user.isAdmin()) {
                            // אם המשתמש הוא מנהל - למסך הניהול
                            mainIntent = new Intent(LoginActivity.this, AdminDashboardActivity.class);
                        } else {
                            // אם המשתמש הוא הורה רגיל - למסך בחירת ילד
                            mainIntent = new Intent(LoginActivity.this, SelectChildActivity.class);
                        }

                        // ===== מעבר למסך החדש =====
                        // FLAG_ACTIVITY_NEW_TASK = "התחל משימה חדשה"
                        // FLAG_ACTIVITY_CLEAR_TASK = "מחק את כל המסכים הקודמים"
                        // השילוב הזה אומר: "מחק את LoginActivity ואת כל מה שמאחוריו"
                        // ככה אם המשתמש ילחץ "חזור" הוא לא יחזור ל-LoginActivity
                        mainIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);

                        // פתיחת המסך החדש
                        startActivity(mainIntent);
                    }

                    // ===== מה לעשות אם הקריאה נכשלה =====
                    @Override
                    public void onFailed(Exception e) {
                        Log.e(TAG, "onFailed: Failed to retrieve user data", e);

                        // הצגת שגיאה למשתמש
                        etPassword.setError("שם משתמש או סיסמה לא חוקיים");
                        etPassword.requestFocus();

                        // יציאה מהמערכת (אם משום מה היה משתמש שמור)
                        SharedPreferencesUtil.signOutUser(LoginActivity.this);
                    }
                }
        );
    }
}