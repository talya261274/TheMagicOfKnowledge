// ===== הצהרת חבילה =====
// המחלקה הזו נמצאת ב-utils (כלי עזר), כמו Validator שראינו קודם
package com.example.themagicofknowledge.utils;


// ===== Imports =====

// Context - אובייקט שמייצג את "הסביבה" של האפליקציה
// כל Activity הוא Context. צריך אותו כדי לגשת לקבצי האפליקציה
import android.content.Context;

// SharedPreferences - המחלקה הראשית שעוזרת לשמור נתונים על המכשיר
import android.content.SharedPreferences;

// @Nullable - אנוטציה שאומרת "הערך הזה יכול להיות null"
import androidx.annotation.Nullable;

// המודלים שלנו - המחלקות שמייצגות הורה וילד באפליקציה
import com.example.themagicofknowledge.models.UserChild;
import com.example.themagicofknowledge.models.UserParent;

// Gson - ספרייה של גוגל שיודעת להמיר אובייקטים ל-JSON ולהפך
// JSON זה פורמט טקסט פופולרי לשמירת נתונים, נראה ככה:
// {"name":"טל","age":25}
import com.google.gson.Gson;


/// Utility class for shared preferences operations
public class SharedPreferencesUtil {

    // ===== קבועים (Constants) =====

    // private - רק המחלקה הזו יכולה לגשת
    // static - שייך למחלקה ולא לאובייקט (כל המופעים חולקים את אותו ערך)
    // final - לא ניתן לשנות אחרי האתחול (קבוע)
    //
    // זה השם של "הפנקס" שלנו - שם הקובץ שאנדרואיד שומר בו את כל הנתונים שלנו
    // השם צריך להיות ייחודי כדי שלא יתנגש עם אפליקציות אחרות
    // (לכן משתמשים בשם החבילה של האפליקציה)
    private static final String PREF_NAME = "com.example.testapp.PREFERENCE_FILE_KEY";

    // המפתח הספציפי שבו נשמור את הילד הנבחר כרגע
    // לכל ערך שאנחנו שומרים יש "מפתח" שדרכו אנחנו ניגשים אליו אחר כך
    private static final String KEY_SELECTED_CHILD = "selected_child";


    // ===== פונקציות פנימיות גנריות =====
    // הפונקציות עם "private" הן עזר פנימי - רק הפונקציות הציבוריות (public)
    // משתמשות בהן. ככה אנחנו מסתירים את המורכבות של SharedPreferences

    /// פונקציה לשמירת מחרוזת
    private static void saveString(Context context, String key, String value) {
        // שלב 1: קבלת אובייקט ה-SharedPreferences
        // context.getSharedPreferences() פותח את "הפנקס" שלנו
        // הפרמטרים:
        //   - PREF_NAME = שם הקובץ
        //   - Context.MODE_PRIVATE = מצב פרטי (רק האפליקציה שלנו תוכל לקרוא)
        SharedPreferences sharedPreferences = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);

        // שלב 2: יצירת "עורך" - בלעדיו אי אפשר לשנות נתונים
        // זה כמו לפתוח את הפנקס במצב כתיבה
        SharedPreferences.Editor editor = sharedPreferences.edit();

        // שלב 3: כתיבת הערך
        // editor.putString(key, value) - אומר: "שמור את value תחת key"
        editor.putString(key, value);

        // שלב 4: שמירה בפועל
        // .apply() - שומר ברקע (לא חוסם את האפליקציה)
        // יש גם .commit() שעושה את אותו דבר אבל עוצר את האפליקציה עד שיגמר
        editor.apply();
    }


    /// פונקציה לקריאת מחרוזת
    private static String getString(Context context, String key, String defaultValue) {
        // פותחים את הפנקס
        SharedPreferences sharedPreferences = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);

        // קוראים את הערך לפי המפתח
        // אם המפתח לא קיים בפנקס - מחזירים את defaultValue (ערך ברירת מחדל)
        // ככה אף פעם לא נקבל NullPointerException
        return sharedPreferences.getString(key, defaultValue);
    }


    /// פונקציה לשמירת מספר שלם
    // אותו רעיון, רק עם int במקום String
    private static void saveInt(Context context, String key, int value) {
        SharedPreferences sharedPreferences = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putInt(key, value);
        editor.apply();
    }


    /// פונקציה לקריאת מספר שלם
    private static int getInt(Context context, String key, int defaultValue) {
        SharedPreferences sharedPreferences = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        return sharedPreferences.getInt(key, defaultValue);
    }


    // ===== פונקציות ציבוריות לניהול הפנקס =====

    /// מחיקה מוחלטת של כל הנתונים בפנקס
    // public - גם בקבצים אחרים אפשר להשתמש
    public static void clear(Context context) {
        SharedPreferences sharedPreferences = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPreferences.edit();

        // editor.clear() - מוחק את הכל
        editor.clear();
        editor.apply();
    }


    /// מחיקת מפתח ספציפי
    private static void remove(Context context, String key) {
        SharedPreferences sharedPreferences = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPreferences.edit();

        // editor.remove(key) - מוחק רק את הערך של key הזה
        // הערכים האחרים נשארים
        editor.remove(key);
        editor.apply();
    }


    /// בדיקה אם מפתח קיים בפנקס
    private static boolean contains(Context context, String key) {
        SharedPreferences sharedPreferences = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);

        // contains(key) - מחזיר true אם המפתח קיים, false אם לא
        return sharedPreferences.contains(key);
    }


    // ===== החלק החכם - שמירת אובייקטים שלמים =====
    // SharedPreferences יודע לשמור רק טיפוסים בסיסיים: String, int, boolean וכו'
    // הוא לא יודע לשמור אובייקט שלם כמו UserParent.
    //
    // הטריק: ממירים את האובייקט ל-JSON (מחרוזת טקסט), שומרים את המחרוזת,
    // וכשרוצים לקרוא - ממירים מחזרה לאובייקט.

    /// שמירת אובייקט גנרי - <T> אומר "כל סוג שהוא"
    // Generic = זה אומר שהפונקציה עובדת עם כל סוג של אובייקט
    // אם נעביר UserParent - זה יעבוד
    // אם נעביר UserChild - זה יעבוד גם
    private static <T> void saveObject(Context context, String key, T object) {
        // יצירת אובייקט Gson
        Gson gson = new Gson();

        // gson.toJson(object) - ממיר את האובייקט למחרוזת JSON
        // לדוגמה:
        // UserParent עם שם "טל" → '{"firstName":"טל","lastName":"חנוני",...}'
        String json = gson.toJson(object);

        // שמירת המחרוזת בעזרת הפונקציה הפנימית שכבר כתבנו
        saveString(context, key, json);
    }


    /// קריאת אובייקט גנרי
    private static <T> T getObject(Context context, String key, Class<T> type) {
        // קריאת המחרוזת מ-SharedPreferences
        String json = getString(context, key, null);

        // אם אין נתונים שמורים - מחזירים null
        if (json == null) {
            return null;
        }

        // המרת המחרוזת חזרה לאובייקט
        Gson gson = new Gson();

        // gson.fromJson(json, type) - אומר ל-Gson:
        // "קח את המחרוזת json הזו, והמר אותה לאובייקט מהסוג type"
        //
        // לדוגמה: '{"firstName":"טל",...}' → UserParent עם השדות מאוכלסים
        return gson.fromJson(json, type);
    }


    // ===== פונקציות ספציפיות למשתמש המחובר (הורה) =====

    /// שמירת אובייקט משתמש (הורה) ב-SharedPreferences
    // משתמשים בזה אחרי התחברות מוצלחת או הרשמה
    public static void saveUser(Context context, UserParent user) {
        // שומרים תחת המפתח "user"
        saveObject(context, "user", user);
    }


    /// קבלת אובייקט המשתמש המחובר
    public static UserParent getUser(Context context) {
        // קודם בודקים אם בכלל יש משתמש מחובר
        // אם אין - מחזירים null מיד (לא מנסים לקרוא ערך לא קיים)
        if (!isUserLoggedIn(context)) {
            return null;
        }

        // קוראים את האובייקט וממירים אותו ל-UserParent
        // UserParent.class אומר ל-Gson: "המר את ה-JSON לאובייקט מסוג UserParent"
        return getObject(context, "user", UserParent.class);
    }


    /// יציאה מהמערכת = מחיקת המשתמש מ-SharedPreferences
    // זה מה שקורה כשלוחצים "התנתק"
    // אחרי זה isUserLoggedIn() יחזיר false
    public static void signOutUser(Context context) {
        remove(context, "user");
    }


    /// בדיקה אם משתמש מחובר
    // משמש בעיקר ב-SplashActivity כדי להחליט אם להעביר ל-LandingActivity
    // (התחברות) או ל-SelectChildActivity (משתמש כבר מחובר)
    public static boolean isUserLoggedIn(Context context) {
        // אם המפתח "user" קיים = המשתמש מחובר
        return contains(context, "user");
    }


    /// קבלת ה-ID של המשתמש המחובר
    // קיצור דרך - במקום לקרוא getUser() ואז .getId(), אפשר לקרוא ישירות
    @Nullable
    public static String getUserId(Context context) {
        UserParent user = getUser(context);

        // הגנה - אם אין משתמש מחובר, מחזירים null במקום לקרוס
        if (user != null) {
            return user.getId();
        }
        return null;
    }


    // ===== פונקציות ספציפיות לילד הפעיל =====
    // אחרי שההורה התחבר, הוא בוחר על איזה ילד הוא משחק עכשיו
    // הילד הזה נשמר ב-SharedPreferences כדי שכל המסכים ידעו מי הילד הפעיל

    /// שמירת הילד שנבחר כרגע למשחק
    public static void saveCurrentChild(Context context, UserChild child) {
        // שומרים תחת המפתח KEY_SELECTED_CHILD ("selected_child")
        saveObject(context, KEY_SELECTED_CHILD, child);
    }


    /// שליפת הילד שמשחק כרגע
    // משמש בכל המסכים של המשחקים כדי לדעת על איזה ילד לעדכן את ההתקדמות
    public static UserChild getCurrentChild(Context context) {
        return getObject(context, KEY_SELECTED_CHILD, UserChild.class);
    }


    /// בדיקה האם נבחר ילד למשחק
    // שימושי במעברי מסכים - אם אין ילד נבחר, להעביר ל-SelectChildActivity
    public static boolean isChildSelected(Context context) {
        return contains(context, KEY_SELECTED_CHILD);
    }


    /// איפוס בחירת הילד
    // למשל - כשהילד לוחץ "חזרה למצב הורה", או כשמתנתקים מהמערכת
    public static void clearSelectedChild(Context context) {
        remove(context, KEY_SELECTED_CHILD);
    }


    // ===== פונקציות נוספות לשמירת ה-ID של הילד =====
    // אלו פונקציות "כפילות" שכנראה נוצרו בטעות - שומרות רק את ה-ID במקום
    // את כל אובייקט הילד. אפשר היה למחוק אותן כי כבר יש את saveCurrentChild
    // ששומר את כל הילד (כולל ה-ID).

    public static void saveCurrentChildId(Context context, String childId) {
        saveString(context, "current_child_id", childId);
    }

    public static String getCurrentChildId(Context context) {
        return getString(context, "current_child_id", null);
    }

}