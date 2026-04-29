// ===== הצהרת חבילה =====
// services - חבילה של שירותים (פונקציות שעובדות "מתחת לפני השטח")
package com.example.themagicofknowledge.services;


// ===== Imports =====

// Log - לכתיבת הודעות לוג שאפשר לראות ב-Logcat של Android Studio
import android.util.Log;

// אנוטציות לתיעוד שדות שלא יכולים להיות null או יכולים להיות null
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

// המודל שלנו של הורה
import com.example.themagicofknowledge.models.UserParent;

// ===== הספרייה של Firebase =====

// DataSnapshot - "צילום מצב" של נתונים מ-Firebase
// זה אובייקט שמכיל את הנתונים בנקודת זמן מסוימת
import com.google.firebase.database.DataSnapshot;

// DatabaseError - כל שגיאה שמתרחשת מול Firebase
import com.google.firebase.database.DatabaseError;

// DatabaseReference - "מצביע" למקום מסוים במסד הנתונים
// כמו: ההורה הזה / הילד הזה / השדה isAdmin של המשתמש הזה
import com.google.firebase.database.DatabaseReference;

// FirebaseDatabase - הכניסה הראשית למסד הנתונים
import com.google.firebase.database.FirebaseDatabase;

// MutableData - נתונים שניתן לשנות (משמש בעדכונים אטומיים)
import com.google.firebase.database.MutableData;

// Transaction - עדכון "אטומי" - או הכל מצליח או הכל נכשל
// משמש כשצריך לעדכן ערך שתלוי בערך הקיים
import com.google.firebase.database.Transaction;

// ValueEventListener - מאזין לשינויים בנתונים
import com.google.firebase.database.ValueEventListener;

// אנוטציה דומה ל-@NonNull, רק מספרייה אחרת
import org.jetbrains.annotations.NotNull;

// ===== ספריות Java סטנדרטיות =====
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

// UnaryOperator - "פונקציה שמקבלת אובייקט ומחזירה אובייקט מאותו סוג"
// משמש בעדכוני transaction
import java.util.function.UnaryOperator;


public class DatabaseService {

    // ===== קבועים =====

    // TAG לזיהוי הודעות log שמגיעות מהמחלקה הזו
    // כשנחפש בלוג נכתוב "DatabaseService" ונמצא רק את ההודעות שלנו
    private static final String TAG = "DatabaseService";

    // הנתיב במסד הנתונים שבו שמורים המשתמשים
    // ב-Firebase התיקייה תיראה: /users/{userId}/...
    private static final String USERS_PATH = "users";


    // ===== Singleton Pattern =====

    // static = משתנה משותף לכל המופעים
    // instance = "המופע היחיד" של המחלקה הזו
    private static DatabaseService instance;

    // final = לא ניתן לשנות אחרי אתחול
    // databaseReference = החיבור לבסיס הנתונים של Firebase
    private final DatabaseReference databaseReference;


    // ===== הקונסטרקטור =====
    // private = רק המחלקה עצמה יכולה ליצור מופע (זה חלק מ-Singleton)
    // אם הוא היה public, כל אחד היה יכול ליצור עוד אובייקט
    private DatabaseService() {
        // קבלת המופע של Firebase (גם זה Singleton בעצם)
        FirebaseDatabase firebaseDatabase = FirebaseDatabase.getInstance();

        // קבלת ההפניה לשורש מסד הנתונים
        // אחרי זה אפשר להגיע לכל מקום בעזרת .child("...")
        databaseReference = firebaseDatabase.getReference();
    }


    /// קבלת ה-instance הקיים, או יצירת חדש אם זו הפעם הראשונה
    public static DatabaseService getInstance() {
        // ה-"if" הזה הוא הקסם של Singleton:
        // - בקריאה ראשונה - instance == null, אז יוצרים חדש
        // - בכל קריאה אחרת - instance כבר קיים, מחזירים אותו
        if (instance == null) {
            instance = new DatabaseService();
        }
        return instance;
    }


    // ===== פונקציות עזר פנימיות (private) =====
    // הפונקציות האלה נותנות לנו דרך אחידה לעבוד עם Firebase
    // הציבוריות (public) משתמשות בהן ברקע

    /// כתיבת נתון לנתיב מסוים ב-Firebase
    private void writeData(@NotNull final String path, @NotNull final Object data, @Nullable final DatabaseCallback<Void> callback) {
        // readData(path) - מחזיר DatabaseReference למקום הספציפי
        // .setValue(data, callback) - שומר את הנתון, ואז מודיע לנו אם הצליח/נכשל
        readData(path).setValue(data, (error, ref) -> {
            // ה-(error, ref) -> {...} זה Lambda - פונקציה אנונימית
            // היא רצה אחרי שה-setValue מסתיים

            if (error != null) {
                // אם הייתה שגיאה - מודיעים ב-callback (אם קיים)
                if (callback != null) callback.onFailed(error.toException());
            } else {
                // אם הצליח - מודיעים שהושלם
                if (callback != null) callback.onCompleted(null);
            }
        });
    }


    /// מחיקת נתון מנתיב מסוים
    private void deleteData(@NotNull final String path, @Nullable final DatabaseCallback<Void> callback) {
        // .removeValue(callback) - מוחק את הנתון
        readData(path).removeValue((error, ref) -> {
            if (error != null) {
                if (callback != null) callback.onFailed(error.toException());
            } else {
                if (callback != null) callback.onCompleted(null);
            }
        });
    }


    /// קבלת DatabaseReference לנתיב מסוים
    // למשל: readData("users/abc123") יחזיר הפניה ל-/users/abc123
    private DatabaseReference readData(@NotNull final String path) {
        return databaseReference.child(path);
    }


    /// קריאת אובייקט בודד מ-Firebase
    // <T> = "כל סוג" (Generic)
    // Class<T> clazz = הסוג שאליו נמיר את הנתון (לדוגמה: UserParent.class)
    private <T> void getData(@NotNull final String path, @NotNull final Class<T> clazz, @NotNull final DatabaseCallback<T> callback) {
        // .get() - מבקש את הנתון פעם אחת
        // .addOnCompleteListener(...) - מוסיף מאזין שיופעל כשהפעולה תסתיים
        readData(path).get().addOnCompleteListener(task -> {

            // task.isSuccessful() = האם הקריאה הצליחה?
            if (!task.isSuccessful()) {
                Log.e(TAG, "Error getting data", task.getException());
                callback.onFailed(task.getException());
                return; // יציאה מהפונקציה - לא נמשיך
            }

            // המרת הנתון מ-Firebase לאובייקט מהסוג שביקשנו
            // למשל: ה-JSON של Firebase יומר ל-UserParent
            T data = task.getResult().getValue(clazz);

            // קוראים ל-callback ומעבירים את הנתונים
            callback.onCompleted(data);
        });
    }


    /// קריאת רשימה של אובייקטים מ-Firebase
    private <T> void getDataList(@NotNull final String path, @NotNull final Class<T> clazz, @NotNull final DatabaseCallback<List<T>> callback) {
        readData(path).get().addOnCompleteListener(task -> {
            if (!task.isSuccessful()) {
                Log.e(TAG, "Error getting data", task.getException());
                callback.onFailed(task.getException());
                return;
            }

            // יצירת רשימה ריקה
            List<T> tList = new ArrayList<>();

            // .getChildren() - מחזיר את כל התת-תיקיות (children)
            // למשל: אם הנתיב הוא "users", זה יחזיר את כל המשתמשים
            // .forEach(...) - לכל אחד מהם:
            task.getResult().getChildren().forEach(dataSnapshot -> {
                // ממירים אותו לאובייקט מהסוג הנכון
                T t = dataSnapshot.getValue(clazz);
                // מוסיפים לרשימה
                tList.add(t);
            });

            // מחזירים את הרשימה השלמה
            callback.onCompleted(tList);
        });
    }


    /// יצירת מזהה ייחודי חדש (ID)
    // Firebase יודע ליצור IDs ייחודיים אוטומטית בעזרת push()
    // הם נראים ככה: "-OqyBx_boQLwcXkwsIIy"
    private String generateNewId(@NotNull final String path) {
        // .child(path) - מצביעים על הנתיב
        // .push() - יוצרים נתיב חדש עם ID ייחודי
        // .getKey() - מחזירים את ה-ID שנוצר
        return databaseReference.child(path).push().getKey();
    }


    /// פונקציית עזר לעדכון אטומי (Transaction)
    // משמש כשצריך לעדכן ערך באופן בטוח גם אם משתמש אחר עודכן בו זמנית
    // למשל: עדכון ספירת ניסיונות במשחק - אם 2 משתמשים מנסים בו זמנית, אנחנו לא רוצים לאבד נתונים
    private <T> void runTransaction(@NotNull final String path, @NotNull final Class<T> clazz, @NotNull UnaryOperator<T> function, @NotNull final DatabaseCallback<T> callback) {
        readData(path).runTransaction(new Transaction.Handler() {

            // doTransaction רץ עם הנתונים הנוכחיים
            // הפונקציה צריכה לבצע את השינוי ולהחזיר את הנתונים החדשים
            @NonNull
            @Override
            public Transaction.Result doTransaction(@NonNull MutableData currentData) {
                // קבלת הנתון הנוכחי
                T currentValue = currentData.getValue(clazz);

                // אם אין נתון - יוצרים חדש על ידי קריאה ל-function עם null
                // אם יש נתון - מעדכנים אותו
                if (currentValue == null) currentValue = function.apply(null);
                else currentValue = function.apply(currentValue);

                // שמירת הערך החדש
                currentData.setValue(currentValue);

                // מציינים שהעדכון הצליח
                return Transaction.success(currentData);
            }

            // onComplete רץ אחרי שהעדכון מסתיים (הצליח או נכשל)
            @Override
            public void onComplete(@Nullable DatabaseError error, boolean committed, @Nullable DataSnapshot currentData) {
                if (error != null) {
                    Log.e(TAG, "Transaction failed", error.toException());
                    callback.onFailed(error.toException());
                    return;
                }

                // החזרת הנתון המעודכן
                T result = currentData != null ? currentData.getValue(clazz) : null;
                callback.onCompleted(result);
            }
        });
    }


    // ===== פונקציות ציבוריות לעבודה עם משתמשים =====

    /// יצירת ID חדש למשתמש
    public String generateUserId() {
        return generateNewId(USERS_PATH);
    }


    /// יצירת משתמש חדש ב-Firebase
    // שימוש: בעת הרשמה ב-RegisterActivity
    public void createNewUser(@NotNull final UserParent user, @Nullable final DatabaseCallback<Void> callback) {
        // הנתיב יהיה: users/{userId}
        // השומרים את כל אובייקט המשתמש שם
        writeData(USERS_PATH + "/" + user.getId(), user, callback);
    }


    /// קבלת משתמש לפי ID
    // שימוש: בעת טעינת פרופיל
    public void getUser(@NotNull final String uid, @NotNull final DatabaseCallback<UserParent> callback) {
        // הנתיב: users/{uid}
        // הסוג: UserParent (כדי להמיר את הנתונים)
        getData(USERS_PATH + "/" + uid, UserParent.class, callback);
    }


    /// קבלת רשימה של כל המשתמשים
    // שימוש: ב-UsersListActivity (פאנל מנהל)
    public void getUserList(@NotNull final DatabaseCallback<List<UserParent>> callback) {
        getDataList(USERS_PATH, UserParent.class, callback);
    }


    /// מחיקת משתמש
    // שימוש: כשמנהל מוחק משתמש
    public void deleteUser(@NotNull final String uid, @Nullable final DatabaseCallback<Void> callback) {
        deleteData(USERS_PATH + "/" + uid, callback);
    }


    /// חיפוש משתמש לפי שם משתמש וסיסמה (התחברות)
    // לא משתמשים ב-Firebase Authentication, אז אנחנו מבצעים בדיקה ידנית
    public void getUserByUsernameAndPassword(@NotNull final String username, @NotNull final String password, @NotNull final DatabaseCallback<UserParent> callback) {
        // קודם מקבלים את כל הרשימה
        getUserList(new DatabaseCallback<List<UserParent>>() {
            @Override
            public void onCompleted(List<UserParent> users) {
                // עוברים על כל משתמש ובודקים אם יש התאמה
                for (UserParent user : users) {
                    // Objects.equals - דרך בטוחה להשוות מחרוזות (גם אם null)
                    if (Objects.equals(user.getUserName(), username) && Objects.equals(user.getPassword(), password)) {
                        // מצאנו! מחזירים את המשתמש
                        callback.onCompleted(user);
                        return; // יציאה מהפונקציה
                    }
                }
                // אם הגענו לכאן - לא מצאנו, מחזירים null
                callback.onCompleted(null);
            }

            @Override
            public void onFailed(Exception e) {
                // אם הקריאה ל-Firebase נכשלה
                callback.onFailed(e);
            }
        });
    }


    /// בדיקה אם אימייל כבר קיים במערכת
    // שימוש: בהרשמה - לא רוצים שמשתמשים יירשמו עם אימייל כפול
    public void checkIfEmailExists(@NotNull final String email, @NotNull final DatabaseCallback<Boolean> callback) {
        getUserList(new DatabaseCallback<List<UserParent>>() {
            @Override
            public void onCompleted(List<UserParent> users) {
                // עוברים על כל המשתמשים ובודקים אימייל
                for (UserParent user : users) {
                    if (Objects.equals(user.getEmail(), email)) {
                        callback.onCompleted(true); // נמצא!
                        return;
                    }
                }
                callback.onCompleted(false); // לא נמצא
            }

            @Override
            public void onFailed(Exception e) {
                callback.onFailed(e);
            }
        });
    }


    /// קבלת משתמש לפי אימייל
    // שימוש: ב-ForgotPasswordActivity (שחזור סיסמה)
    public void getUserByEmail(@NotNull final String email, @NotNull final DatabaseCallback<UserParent> callback) {
        // ⚡ אופטימיזציה - משתמשים ב-Firebase Query במקום למשוך הכל ולחפש
        databaseReference.child(USERS_PATH)
                // orderByChild("email") - מארגן לפי שדה האימייל
                .orderByChild("email")
                // equalTo(email) - מסנן רק את אלה שתואמים
                .equalTo(email)
                .get()
                .addOnCompleteListener(task -> {
                    // hasChildren() - האם יש לפחות תוצאה אחת?
                    if (task.isSuccessful() && task.getResult().hasChildren()) {
                        // Firebase מחזיר תמיד רשימה, גם אם יש תוצאה אחת
                        // .iterator().next() = "תן לי את הראשון"
                        DataSnapshot snapshot = task.getResult().getChildren().iterator().next();
                        UserParent user = snapshot.getValue(UserParent.class);
                        callback.onCompleted(user);
                    } else {
                        callback.onCompleted(null);
                    }
                });
    }


    /// עדכון משתמש בעזרת transaction (אטומי)
    // שימוש: כשרוצים לעדכן רק חלק מהשדות בלי לאבד שדות אחרים
    public void updateUser(@NotNull String userId, @NotNull UnaryOperator<UserParent> function, @NotNull final DatabaseCallback<UserParent> callback) {
        runTransaction(USERS_PATH + "/" + userId, UserParent.class, function, callback);
    }


    /// יצירת ID חדש לילד
    // הילדים שמורים בתוך ההורה: users/{parentId}/childrenList/{childId}
    public String generateChildId(@NotNull String userId) {
        return generateNewId(USERS_PATH + "/" + userId);
    }


    /// מחיקת ילד מההורה
    public void deleteChild(String parentId, String childId, DatabaseCallback<Void> callback) {
        // הגנה - אם פרמטרים לא תקינים, החזר שגיאה
        if (parentId == null || childId == null) {
            if (callback != null) callback.onFailed(new Exception("Parent ID or Child ID is null"));
            return;
        }

        // ניווט לנתיב הספציפי של הילד:
        // users -> parentId -> childrenList -> childId
        databaseReference.child("users")
                .child(parentId)
                .child("childrenList")
                .child(childId)
                // .removeValue() - מוחק את כל התת-עץ הזה
                .removeValue()
                // .addOnSuccessListener - אם הצליח
                .addOnSuccessListener(aVoid -> {
                    if (callback != null) callback.onCompleted(null);
                })
                // .addOnFailureListener - אם נכשל
                .addOnFailureListener(e -> {
                    if (callback != null) callback.onFailed(e);
                });
    }


    /// עדכון התקדמות מפורטת של ילד בנושא ספציפי
    // הפונקציה הכי מורכבת - היא קוראת קודם את הנתונים הקיימים, מחברת איתם, ושומרת
    public void updateDetailedProgress(String parentId, String childId, String ageGroup, String subject,
                                       int extraAttempts, long extraTime, int progressPercent, int lastIdx) {

        // בניית הנתיב המדויק:
        // users/parentId/childrenList/childId/progress/ageGroup/subject
        // למשל: users/abc/childrenList/xyz/progress/3-4/animals
        DatabaseReference ref = FirebaseDatabase.getInstance().getReference("users")
                .child(parentId)
                .child("childrenList")
                .child(childId)
                .child("progress")
                .child(ageGroup)
                .child(subject);

        // addListenerForSingleValueEvent - "תקרא לי פעם אחת עם הערכים הנוכחיים"
        // (להבדיל מ-addValueEventListener שקורא בכל שינוי)
        ref.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                // קבלת הערכים הקיימים (אם יש)
                long currentAttempts = 0;
                long currentTime = 0;

                // snapshot.exists() - האם בכלל יש נתונים בנתיב הזה?
                if (snapshot.exists()) {
                    // קבלת ערכים בודדים
                    Long att = snapshot.child("attempts").getValue(Long.class);
                    Long time = snapshot.child("timeSeconds").getValue(Long.class);

                    // הגנה על null - אם השדה לא קיים, נשארים עם 0
                    if (att != null) currentAttempts = att;
                    if (time != null) currentTime = time;
                }

                // יצירת מפה (Map) של עדכונים
                // Map = מבנה נתונים של זוגות מפתח-ערך (כמו ב-JavaScript: {"key": value})
                Map<String, Object> updates = new HashMap<>();

                // חיבור הניסיונות הקודמים עם החדשים
                updates.put("attempts", currentAttempts + extraAttempts);

                // חיבור הזמן הקודם עם החדש
                updates.put("timeSeconds", currentTime + extraTime);

                // עדכון אחוז התקדמות (לא מצטבר - זה הערך החדש)
                updates.put("progressPercent", progressPercent);

                // שמירת השאלה האחרונה (חשוב כדי לאפשר "להמשיך מאיפה שעצרנו")
                updates.put("lastQuestionIndex", lastIdx);

                // אם הגיע ל-100% - מסמנים כהושלם
                if (progressPercent >= 100) {
                    updates.put("completed", true);
                } else {
                    updates.put("completed", false);
                }

                // .updateChildren - מעדכן רק את השדות שצוינו, השאר לא נוגעים
                // (להבדיל מ-setValue שמחליף את הכל)
                ref.updateChildren(updates).addOnFailureListener(e ->
                        Log.e(TAG, "Failed to update progress", e)
                );
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                // אם הקריאה הראשונה נכשלה (לפני העדכון)
                Log.e(TAG, "Database error: " + error.getMessage());
            }
        });
    }


    /// איפוס מיקום ההתקדמות במשחק (חוזרים לשאלה 0)
    public void resetGameIndex(String parentId, String childId, String ageGroup, String subject) {
        FirebaseDatabase.getInstance().getReference("users")
                .child(parentId)
                .child("childrenList")
                .child(childId)
                .child("progress")
                .child(ageGroup)
                .child(subject)
                .child("lastQuestionIndex")
                // .setValue(0) - מציבים ערך 0
                .setValue(0);
    }


    // ===== Interface (ממשק) - DatabaseCallback =====
    // הממשק הזה מגדיר "חוזה" - איך נראה callback של תוצאה מ-Firebase
    // <T> = generic - יכול להיות לכל סוג של תוצאה
    public interface DatabaseCallback<T> {
        // נקרא כשהפעולה הצליחה
        void onCompleted(T object);

        // נקרא כשהפעולה נכשלה
        void onFailed(Exception e);
    }
}