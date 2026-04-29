// ===== הצהרת חבילה (Package) =====
// כל מחלקה ב-Java שייכת ל"חבילה" - תיקייה לוגית של קבצים קשורים.
// המחלקה הזו נמצאת בחבילה utils (כלי עזר) של האפליקציה שלנו.
package com.example.themagicofknowledge.utils;


// ===== Imports - ייבוא מחלקות מספריות חיצוניות =====

// Patterns - מחלקה של אנדרואיד שמכילה ביטויים רגולריים מוכנים
// (כמו: איך נראה אימייל תקין? איך נראה מספר טלפון תקין?)
import android.util.Patterns;

// @Nullable - אנוטציה שאומרת ל-Android Studio:
// "השדה הזה יכול להיות null - שים לב!"
// זה עוזר למנוע NullPointerException
import androidx.annotation.Nullable;

// ParseException - שגיאה שנזרקת כשניסיון להמיר טקסט לתאריך נכשל
// (למשל אם הטקסט הוא "abc" במקום "25/12/2024")
import java.text.ParseException;

// SimpleDateFormat - מחלקה שיודעת להמיר טקסט לתאריך ולהפך
// לפי פורמט מסוים (כמו "dd/MM/yyyy")
import java.text.SimpleDateFormat;

// Date - מחלקה שמייצגת תאריך ושעה ב-Java
import java.util.Date;


// ===== הערות JavaDoc =====
// השלוש קווים נטויים (///) הם "הערות תיעוד".
// הם מופיעים כשעוברים עם העכבר על שם המחלקה ב-IDE.
// ככה מתעדים מה המחלקה עושה כדי שיהיה קל למפתחים אחרים להבין.

/// Validator class to validate user input.
/// This class contains static methods to validate user input,
/// like email, password, phone, name etc.


// ===== הצהרת המחלקה =====
// public - אומר שהמחלקה נגישה מכל מקום באפליקציה
// class - אומר שזו מחלקה (Class) - תבנית לאובייקטים
// Validator - השם של המחלקה (מקובל להתחיל באות גדולה)
public class Validator {


    // ===== פונקציה לבדיקת אימייל =====

    /// Check if the email is valid
    ///
    /// @param email email to validate
    /// @return true if the email is valid, false otherwise
    /// @see Patterns#EMAIL_ADDRESS

    // public - הפונקציה נגישה מכל מקום
    // static - הפונקציה שייכת למחלקה ולא לאובייקט - אפשר לקרוא לה ישירות:
    //         Validator.isEmailValid("aaa@bbb.com")
    //         בלי ליצור אובייקט חדש (new Validator())
    // boolean - הפונקציה מחזירה true או false
    // isEmailValid - שם הפונקציה (מקובל להתחיל ב-is/has בפונקציות שמחזירות boolean)
    // @Nullable String email - פרמטר מסוג String, יכול להיות null
    public static boolean isEmailValid(@Nullable String email) {
        // הסבר על הביטוי המלא:

        // 1. email != null
        //    - בודק שה-email לא ריק (null = "אין כלום")
        //    - אם הוא null - מחזיר false מיד (ולא ממשיך לבדיקה הבאה)
        //    - זה חשוב! אם ננסה להריץ פעולה על null נקבל קריסה (NullPointerException)

        // 2. && (AND לוגי)
        //    - שתי הבדיקות חייבות להיות true כדי שהתוצאה תהיה true
        //    - אם הראשונה false - לא נטרח לבדוק את השנייה (זה נקרא "short-circuit")

        // 3. Patterns.EMAIL_ADDRESS.matcher(email).matches()
        //    - Patterns.EMAIL_ADDRESS - תבנית מוכנה של אנדרואיד לאימייל
        //    - .matcher(email) - יוצר אובייקט שיבדוק את המחרוזת שלנו מול התבנית
        //    - .matches() - מחזיר true אם המחרוזת תואמת לתבנית
        //
        // לדוגמה:
        // "talya@gmail.com" → true (תואם לתבנית של אימייל)
        // "talya"           → false (חסר @ ודומיין)
        // "talya@"          → false (חסר דומיין)
        return email != null && Patterns.EMAIL_ADDRESS.matcher(email).matches();
    }


    // ===== פונקציה לבדיקת סיסמה =====

    /// Check if the password is valid
    ///
    /// @param password password to validate
    /// @return true if the password is valid, false otherwise
    public static boolean isPasswordValid(@Nullable String password) {
        // 1. password != null - הסיסמה לא ריקה
        // 2. password.length() >= 6 - הסיסמה היא לפחות 6 תווים
        //
        // .length() מחזיר את מספר התווים במחרוזת
        // >= 6 = "גדול או שווה ל-6"
        //
        // לדוגמה:
        // "12345"     → false (רק 5 תווים)
        // "123456"    → true (6 תווים בדיוק)
        // "password1" → true (9 תווים)
        return password != null && password.length() >= 6;
    }


    // ===== פונקציה לבדיקת מספר טלפון =====

    /// Check if the phone number is valid
    ///
    /// @param phone phone number to validate
    /// @return true if the phone number is valid, false otherwise
    /// @see Patterns#PHONE
    public static boolean isPhoneValid(@Nullable String phone) {
        // שלוש בדיקות בסדר:

        // 1. phone != null
        //    - מספר הטלפון לא ריק

        // 2. phone.length() >= 10
        //    - לפחות 10 ספרות (כי מספר ישראלי הוא 10 ספרות: 050-1234567)

        // 3. Patterns.PHONE.matcher(phone).matches()
        //    - תבנית מוכנה של אנדרואיד לבדיקת טלפון
        //    - מאפשרת מקפים, רווחים, סוגריים וכו'
        //
        // לדוגמה:
        // "0501234567"   → true (10 ספרות בלי מקפים)
        // "050-1234567"  → true (עם מקף)
        // "abc123"       → false (אותיות באמצע)
        // "12345"        → false (קצר מדי)
        return phone != null && phone.length() >= 10 && Patterns.PHONE.matcher(phone).matches();
    }


    // ===== פונקציה לבדיקת שם פרטי/משפחה =====

    /// Check if the name is valid
    ///
    /// @param name name to validate
    /// @return true if the name is valid, false otherwise
    public static boolean isNameValid(@Nullable String name) {
        // בדיקה פשוטה - לפחות 2 תווים
        // (כי שם פרטי או משפחה צריך להיות לפחות 2 אותיות, כמו "לי" או "Jo")
        //
        // לדוגמה:
        // "א"      → false (תו אחד בלבד)
        // "טל"    → true
        // "טליה"  → true
        return name != null && name.length() >= 2;
    }


    // ===== פונקציה לבדיקת שם משתמש =====

    public static boolean isUserNameValid(String uName) {

        // ===== שלב 1: בדיקה שלא null וגם לא ריק =====
        // uName.trim() - מסיר רווחים בהתחלה ובסוף ("  abc  " → "abc")
        // .isEmpty() - בודק אם המחרוזת ריקה ("")
        //
        // המשמעות: גם "" וגם "   " (רווחים בלבד) ייחשבו לא תקינים
        if (uName == null || uName.trim().isEmpty()) {
            return false; // לא ריק
        }

        // ===== שלב 2: לפחות 3 תווים =====
        // שם משתמש חייב להיות לפחות 3 תווים
        if (uName.length() < 3) {
            return false;
        }

        // ===== שלב 3: רק אותיות באנגלית או מספרים =====
        // .matches(...) מקבל ביטוי רגולרי (Regex) ובודק אם המחרוזת תואמת
        //
        // הסבר על הביטוי "[a-zA-Z0-9]+":
        // - [a-z] = כל אות קטנה באנגלית
        // - [A-Z] = כל אות גדולה באנגלית
        // - [0-9] = כל ספרה
        // - + = "אחד או יותר" מהתווים האלה
        //
        // ! לפני התנאי = "לא" (אם לא מתאים, החזר false)
        //
        // לדוגמה:
        // "tal123"  → true
        // "talya"   → true
        // "Tal_123" → false (קו תחתון לא מותר)
        // "טל"      → false (אותיות עבריות לא מותרות)
        // "tal!"    → false (סימן קריאה לא מותר)
        if (!uName.matches("[a-zA-Z0-9]+")) {
            return false;
        }

        return true; // ← בסוף מחזיר true אם הכל תקין
    }


    // ===== פונקציה לבדיקת תאריך לידה של הורה =====

    public static boolean isParentBirthDateValid(String birthDate) {

        // ===== שלב 1: בדיקה שלא null ולא ריק =====
        if (birthDate == null || birthDate.trim().isEmpty()) {
            return false; // חייב להזין תאריך
        }

        // ===== שלב 2: יצירת אובייקט שיודע לפרש את הפורמט שלנו =====
        // SimpleDateFormat מקבל את הפורמט הצפוי של התאריך
        // "dd/MM/yyyy" אומר:
        // - dd = יום (2 ספרות)
        // - MM = חודש (2 ספרות)
        // - yyyy = שנה (4 ספרות)
        //
        // לדוגמה: "25/12/1990" יתאים לפורמט הזה
        SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy");

        // setLenient(false) - אומר ל-SimpleDateFormat להיות קפדני
        // אם false → "32/13/1990" יזרוק שגיאה (אין יום 32 ואין חודש 13)
        // אם true (ברירת מחדל) → "32/13/1990" יומר ל-"01/02/1991" באופן אוטומטי (לא רצוי!)
        sdf.setLenient(false);

        // ===== שלב 3: ניסיון להמיר את הטקסט לתאריך =====
        // try-catch = "נסה לעשות, ואם תהיה שגיאה - תפוס אותה"
        // זה חשוב כי sdf.parse() יכול לזרוק ParseException אם הטקסט לא תקין
        try {
            // ממיר את הטקסט לאובייקט Date
            // לדוגמה: "25/12/1990" → Date שמייצג את 25 בדצמבר 1990
            Date date = sdf.parse(birthDate);

            // יוצר אובייקט Date שמייצג את התאריך והשעה הנוכחיים
            Date today = new Date();

            // ===== שלב 4: בדיקה שהתאריך לא בעתיד =====
            // .after(other) → true אם התאריך הנוכחי הוא אחרי other
            // אם תאריך הלידה אחרי היום (בעתיד) → לא הגיוני, מחזיר false
            //
            // לדוגמה:
            // אם היום 29/04/2026 ומישהו מכניס 01/01/2030 → false (בעתיד)
            // אם מכניס 01/01/1990 → true (בעבר, תקין)
            if (date.after(today)) {
                return false;
            }

            return true; // תקין

        } catch (ParseException e) {
            // אם sdf.parse() נכשל (למשל אם הטקסט הוא "abc" או "25-12-1990")
            // הוא זורק ParseException, ואנחנו תופסים אותה כאן
            // ומחזירים false כי הטקסט לא בפורמט תאריך תקין
            return false; // פורמט תאריך לא תקין
        }
    }
}