package com.example.themagicofknowledge.utils;

import android.util.Patterns;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import androidx.annotation.Nullable;

/// Validator class to validate user input.
/// This class contains static methods to validate user input,
/// like email, password, phone, name etc.

public class Validator {
    /// Check if the email is valid
    ///
    /// @param email email to validate
    /// @return true if the email is valid, false otherwise
    /// @see Patterns#EMAIL_ADDRESS
    public static boolean isEmailValid(@Nullable String email) {
        return email != null && Patterns.EMAIL_ADDRESS.matcher(email).matches();
    }

    /// Check if the password is valid
    ///
    /// @param password password to validate
    /// @return true if the password is valid, false otherwise
    public static boolean isPasswordValid(@Nullable String password) {
        return password != null && password.length() >= 6;
    }


    /// Check if the phone number is valid
    ///
    /// @param phone phone number to validate
    /// @return true if the phone number is valid, false otherwise
    /// @see Patterns#PHONE
    public static boolean isPhoneValid(@Nullable String phone) {
        return phone != null && phone.length() >= 10 && Patterns.PHONE.matcher(phone).matches();
    }

    /// Check if the name is valid
    ///
    /// @param name name to validate
    /// @return true if the name is valid, false otherwise
    public static boolean isNameValid(@Nullable String name) {
        return name != null && name.length() >= 3;
    }

    public static boolean isUserNameValid(String uName) {
        if (uName == null || uName.trim().isEmpty()) {
            return false; // לא ריק
        }

        // שם משתמש חייב להיות לפחות 3 תווים
        if (uName.length() < 3) {
            return false;
        }

        // בדיקה שכל התווים הם אותיות או מספרים בלבד
        // [a-zA-Z0-9]+ -> תווים גדולים וקטנים ומספרים בלבד
        if (!uName.matches("[a-zA-Z0-9]+")) {
            return false;
        }
        return false;
    }


    public static boolean isBirthDateValid(String birthDate) {
        if (birthDate == null || birthDate.trim().isEmpty()) {
            return false; // לא ריק
        }

        // הגדרת פורמט התאריך
        SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy");
        sdf.setLenient(false); // מוודא שהתאריך תקין באמת

        try {
            Date date = sdf.parse(birthDate);

            // בדיקה שהמשתמש לא "מעופף בזמן" (גיל מינימלי 3)
            Calendar cal = Calendar.getInstance();
            cal.add(Calendar.YEAR, -3);
            Date minDate = cal.getTime();

            if (date.after(minDate)) {
                return false; // גיל קטן מדי
            }

            return true; // תאריך חוקי
        } catch (ParseException e) {
            return false; // פורמט שגוי
        }
    }
}

