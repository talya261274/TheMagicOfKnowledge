// ===== הצהרת חבילה =====
// המחלקה הזו נמצאת ב-utils (כלי עזר), כמו Validator שראינו קודם
package com.example.themagicofknowledge.utils;

import android.content.Context;

import android.content.SharedPreferences;

import androidx.annotation.Nullable;

import com.example.themagicofknowledge.models.UserChild;
import com.example.themagicofknowledge.models.UserParent;

import com.google.gson.Gson;

/// Utility class for shared preferences operations
public class SharedPreferencesUtil {
    private static final String PREF_NAME = "com.example.testapp.PREFERENCE_FILE_KEY";
    private static final String KEY_SELECTED_CHILD = "selected_child";


    /// פונקציה לשמירת מחרוזת
    private static void saveString(Context context, String key, String value) {

        SharedPreferences sharedPreferences = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putString(key, value);
        editor.apply();
    }

    /// פונקציה לקריאת מחרוזת
    private static String getString(Context context, String key, String defaultValue) {
        SharedPreferences sharedPreferences = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        return sharedPreferences.getString(key, defaultValue);
    }


    /// מחיקת מפתח ספציפי
    private static void remove(Context context, String key) {
        SharedPreferences sharedPreferences = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPreferences.edit();

        editor.remove(key);
        editor.apply();
    }


    /// בדיקה אם מפתח קיים בפנקס
    private static boolean contains(Context context, String key) {
        SharedPreferences sharedPreferences = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);

        return sharedPreferences.contains(key);
    }


    /// שמירת אובייקט גנרי - <T> אומר "כל סוג שהוא"
    private static <T> void saveObject(Context context, String key, T object) {
        Gson gson = new Gson();
        String json = gson.toJson(object);
        saveString(context, key, json);
    }


    /// קריאת אובייקט גנרי
    private static <T> T getObject(Context context, String key, Class<T> type) {
        String json = getString(context, key, null);

        if (json == null) {
            return null;
        }

        Gson gson = new Gson();
        return gson.fromJson(json, type);
    }


    /// שמירת אובייקט משתמש (הורה) ב-SharedPreferences
    public static void saveUser(Context context, UserParent user) {
        saveObject(context, "user", user);
    }


    /// קבלת אובייקט המשתמש המחובר
    public static UserParent getUser(Context context) {
        if (!isUserLoggedIn(context)) {
            return null;
        }

        return getObject(context, "user", UserParent.class);
    }


    /// יציאה מהמערכת = מחיקת המשתמש מ-SharedPreferences
    public static void signOutUser(Context context) {
        remove(context, "user");
    }


    /// בדיקה אם משתמש מחובר
    public static boolean isUserLoggedIn(Context context) {
        // אם המפתח "user" קיים = המשתמש מחובר
        return contains(context, "user");
    }


    /// שמירת הילד שנבחר כרגע למשחק
    public static void saveCurrentChild(Context context, UserChild child) {
        saveObject(context, KEY_SELECTED_CHILD, child);
    }


    /// שליפת הילד שמשחק כרגע
    public static UserChild getCurrentChild(Context context) {
        return getObject(context, KEY_SELECTED_CHILD, UserChild.class);
    }


    /// איפוס בחירת הילד
    public static void clearSelectedChild(Context context) {
        remove(context, KEY_SELECTED_CHILD);
    }

}