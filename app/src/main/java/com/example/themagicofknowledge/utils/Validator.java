package com.example.themagicofknowledge.utils;

import android.util.Patterns;

import androidx.annotation.Nullable;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

/// מחלקה לבדיקת תקינות קלט משתמש
/// מכילה פונקציות סטטיות לבדיקת אימייל, סיסמה, טלפון, שם וכו'
public class Validator {

    /// בדיקה אם האימייל תקין
    /// @param email האימייל לבדיקה
    /// @return true אם האימייל תקין, false אחרת
    public static boolean isEmailValid(@Nullable String email) {
        return email != null && Patterns.EMAIL_ADDRESS.matcher(email).matches();
    }

    /// בדיקה אם הסיסמה תקינה
    /// @param password הסיסמה לבדיקה
    /// @return true אם הסיסמה תקינה (לפחות 6 תווים), false אחרת
    public static boolean isPasswordValid(@Nullable String password) {
        return password != null && password.length() >= 6;
    }

    /// בדיקה אם מספר הטלפון תקין
    /// @param phone מספר הטלפון לבדיקה
    /// @return true אם הטלפון תקין (לפחות 10 ספרות), false אחרת
    public static boolean isPhoneValid(@Nullable String phone) {
        return phone != null && phone.length() >= 10 && Patterns.PHONE.matcher(phone).matches();
    }


    /// בדיקה אם השם תקין
    /// @param name השם לבדיקה
    /// @return true אם השם תקין (לפחות 2 תווים), false אחרת
    public static boolean isNameValid(@Nullable String name) {
        return name != null && name.length() >= 2;
    }


    /// בדיקה אם שם המשתמש תקין
    /// חייב להכיל לפחות 3 תווים, אותיות באנגלית ומספרים בלבד
    /// @param uName שם המשתמש לבדיקה
    /// @return true אם שם המשתמש תקין, false אחרת
    public static boolean isUserNameValid(String uName) {

        // בדיקה שלא null ולא ריק
        if (uName == null || uName.trim().isEmpty()) {
            return false;
        }

        // בדיקה שיש לפחות 3 תווים
        if (uName.length() < 3) {
            return false;
        }

        // בדיקה שיש רק אותיות באנגלית ומספרים
        if (!uName.matches("[a-zA-Z0-9]+")) {
            return false;
        }

        return true;
    }


    /// בדיקה אם תאריך הלידה תקין
    /// הפורמט הנדרש: dd/MM/yyyy
    /// התאריך לא יכול להיות בעתיד
    /// @param birthDate תאריך הלידה לבדיקה
    /// @return true אם התאריך תקין, false אחרת
    public static boolean isParentBirthDateValid(String birthDate) {

        // בדיקה שלא null ולא ריק
        if (birthDate == null || birthDate.trim().isEmpty()) {
            return false;
        }

        // יצירת פורמט תאריך קפדני
        SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy");
        sdf.setLenient(false);

        try {
            // ניסיון להמיר את הטקסט לתאריך
            Date date = sdf.parse(birthDate);
            Date today = new Date();

            // בדיקה שהתאריך לא בעתיד
            if (date.after(today)) {
                return false;
            }

            return true;

        } catch (ParseException e) {
            // הטקסט לא בפורמט תאריך תקין
            return false;
        }
    }
}