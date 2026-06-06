// ===== הצהרת חבילה =====
// services - חבילה של שירותים (פונקציות שעובדות "מתחת לפני השטח")
package com.example.themagicofknowledge.services;


// ===== Imports =====
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
import com.google.firebase.database.ServerValue;
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
    private static final String TAG = "DatabaseService";
    private static final String USERS_PATH = "users";
    private static DatabaseService instance;
    private final DatabaseReference databaseReference;
    private DatabaseService() {
        FirebaseDatabase firebaseDatabase = FirebaseDatabase.getInstance();
        databaseReference = firebaseDatabase.getReference();
    }


    /// קבלת ה-instance הקיים, או יצירת חדש אם זו הפעם הראשונה
    public static DatabaseService getInstance() {
        if (instance == null) {
            instance = new DatabaseService();
        }
        return instance;
    }

    /// כתיבת נתון לנתיב מסוים ב-Firebase
    private void writeData(@NotNull final String path, @NotNull final Object data, @Nullable final DatabaseCallback<Void> callback) {

        readData(path).setValue(data, (error, ref) -> {

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
        readData(path).removeValue((error, ref) -> {
            if (error != null) {
                if (callback != null) callback.onFailed(error.toException());
            } else {
                if (callback != null) callback.onCompleted(null);
            }
        });
    }


    /// קבלת DatabaseReference לנתיב מסוים
    private DatabaseReference readData(@NotNull final String path) {
        return databaseReference.child(path);
    }


    /// קריאת אובייקט בודד מ-Firebase
    private <T> void getData(@NotNull final String path, @NotNull final Class<T> clazz, @NotNull final DatabaseCallback<T> callback) {
        readData(path).get().addOnCompleteListener(task -> {

            if (!task.isSuccessful()) {
                Log.e(TAG, "Error getting data", task.getException());
                callback.onFailed(task.getException());
                return; // יציאה מהפונקציה - לא נמשיך
            }

            T data = task.getResult().getValue(clazz);

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

            List<T> tList = new ArrayList<>();

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
    private String generateNewId(@NotNull final String path) {
        return databaseReference.child(path).push().getKey();
    }


    /// פונקציית עזר לעדכון אטומי (Transaction)
    private <T> void runTransaction(@NotNull final String path, @NotNull final Class<T> clazz, @NotNull UnaryOperator<T> function, @NotNull final DatabaseCallback<T> callback) {
        readData(path).runTransaction(new Transaction.Handler() {
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



    /// יצירת ID חדש למשתמש
    public String generateUserId() {
        return generateNewId(USERS_PATH);
    }


    /// יצירת משתמש חדש ב-Firebase
    public void createNewUser(@NotNull final UserParent user, @Nullable final DatabaseCallback<Void> callback) {
        writeData(USERS_PATH + "/" + user.getId(), user, callback);
    }


    /// קבלת משתמש לפי ID
    public void getUser(@NotNull final String uid, @NotNull final DatabaseCallback<UserParent> callback) {
        getData(USERS_PATH + "/" + uid, UserParent.class, callback);
    }


    /// קבלת רשימה של כל המשתמשים
    public void getUserList(@NotNull final DatabaseCallback<List<UserParent>> callback) {
        getDataList(USERS_PATH, UserParent.class, callback);
    }


    /// מחיקת משתמש
    public void deleteUser(@NotNull final String uid, @Nullable final DatabaseCallback<Void> callback) {
        deleteData(USERS_PATH + "/" + uid, callback);
    }


    /// חיפוש משתמש לפי שם משתמש וסיסמה (התחברות)
    public void getUserByUsernameAndPassword(@NotNull final String username, @NotNull final String password, @NotNull final DatabaseCallback<UserParent> callback) {
        getUserList(new DatabaseCallback<List<UserParent>>() {
            @Override
            public void onCompleted(List<UserParent> users) {
                for (UserParent user : users) {
                    if (Objects.equals(user.getUserName(), username) && Objects.equals(user.getPassword(), password)) {
                        callback.onCompleted(user);
                        return;
                    }
                }
                callback.onCompleted(null);
            }

            @Override
            public void onFailed(Exception e) {
                callback.onFailed(e);
            }
        });
    }


    /// בדיקה אם אימייל כבר קיים במערכת
    public void checkIfEmailExists(@NotNull final String email, @NotNull final DatabaseCallback<Boolean> callback) {
        getUserList(new DatabaseCallback<List<UserParent>>() {
            @Override
            public void onCompleted(List<UserParent> users) {
                for (UserParent user : users) {
                    if (Objects.equals(user.getEmail(), email)) {
                        callback.onCompleted(true); // נמצא!
                        return;
                    }
                }
                callback.onCompleted(false);
            }

            @Override
            public void onFailed(Exception e) {
                callback.onFailed(e);
            }
        });
    }


    /// קבלת משתמש לפי אימייל
    public void getUserByEmail(@NotNull final String email, @NotNull final DatabaseCallback<UserParent> callback) {
        readData(USERS_PATH)
                .orderByChild("email")
                .equalTo(email)
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful() && task.getResult().hasChildren()) {
                        DataSnapshot snapshot = task.getResult().getChildren().iterator().next();
                        UserParent user = snapshot.getValue(UserParent.class);
                        callback.onCompleted(user);
                    } else {
                        callback.onCompleted(null);
                    }
                });
    }


    /// עדכון משתמש בעזרת transaction (אטומי)
    public void updateUser(@NotNull String userId, @NotNull UnaryOperator<UserParent> function, @NotNull final DatabaseCallback<UserParent> callback) {
        runTransaction(USERS_PATH + "/" + userId, UserParent.class, function, callback);
    }


    /// יצירת ID חדש לילד
    public String generateChildId(@NotNull String userId) {
        return generateNewId(USERS_PATH + "/" + userId);
    }


    /// מחיקת ילד מההורה
    public void deleteChild(String parentId, String childId, DatabaseCallback<UserParent> callback) {
        updateUser(parentId, new UnaryOperator<UserParent>() {
            @Override
            public UserParent apply(UserParent userParent) {
                if (userParent != null) {
                    userParent.childrenList.remove(childId);
                }
                return userParent;
            }
        }, callback);
    }


    /// עדכון התקדמות מפורטת של ילד בנושא ספציפי
    public void updateDetailedProgress(String parentId, String childId, String ageGroup, String subject,
                                       int extraAttempts, long extraTime, int progressPercent, int lastIdx) {

        String path = USERS_PATH + "/" + parentId + "/childrenList/" + childId
                + "/progress/" + ageGroup + "/" + subject;

        readData(path).get().addOnCompleteListener(task -> {
            long currentAttempts = 0;
            long currentTime = 0;

            if (task.isSuccessful() && task.getResult().exists()) {
                Long att = task.getResult().child("attempts").getValue(Long.class);
                Long time = task.getResult().child("timeSeconds").getValue(Long.class);
                if (att != null) currentAttempts = att;
                if (time != null) currentTime = time;
            }

            Map<String, Object> updates = new HashMap<>();
            updates.put("attempts", currentAttempts + extraAttempts);
            updates.put("timeSeconds", currentTime + extraTime);
            updates.put("progressPercent", progressPercent);
            updates.put("lastQuestionIndex", lastIdx);
            updates.put("completed", progressPercent >= 100);

            readData(path).updateChildren(updates);
        });
    }

    /// איפוס מיקום ההתקדמות במשחק
    public void resetGameIndex(String parentId, String childId, String ageGroup, String subject) {
        String path = USERS_PATH + "/" + parentId + "/childrenList/" + childId
                + "/progress/" + ageGroup + "/" + subject + "/lastQuestionIndex";

        writeData(path, 0, null);
    }

    public interface DatabaseCallback<T> {
        void onCompleted(T object);
        void onFailed(Exception e);
    }

    /// עדכון התקדמות במשחק
    public void updateGameProgress(String parentId, String childId,
                                   String ageGroup, String subject,
                                   int currentIndex, int percent,
                                   long timeSpent, int attempts) {

        String path = USERS_PATH + "/" + parentId + "/childrenList/" + childId
                + "/progress/" + ageGroup + "/" + subject;

        Map<String, Object> updates = new HashMap<>();
        updates.put("lastQuestionIndex", currentIndex);
        updates.put("progressPercent", percent);
        updates.put("completed", percent >= 100);
        updates.put("timeSeconds", ServerValue.increment(timeSpent));
        updates.put("attempts", ServerValue.increment(attempts));

        readData(path).updateChildren(updates);
    }

    /// סימון נושא כהושלם
    public void markSubjectAsCompleted(String parentId, String childId,
                                       String ageGroup, String subject,
                                       int currentIndex,
                                       DatabaseCallback<Void> callback) {

        String path = USERS_PATH + "/" + parentId + "/childrenList/" + childId
                + "/completedSubjects/" + subject;

        writeData(path, true, new DatabaseCallback<Void>() {
            @Override
            public void onCompleted(Void unused) {
                updateDetailedProgress(parentId, childId, ageGroup, subject, 0, 0, 100, currentIndex);
                if (callback != null) callback.onCompleted(null);
            }

            @Override
            public void onFailed(Exception e) {
                if (callback != null) callback.onFailed(e);
            }
        });
    }

    /// טעינת נתוני משחק לפי נתיב
    public void loadGameData(String path, DatabaseCallback<DataSnapshot> callback) {
        readData(path).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                callback.onCompleted(snapshot);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                callback.onFailed(error.toException());
            }
        });
    }

    /// האזנה לשינויים בנתוני ילד (real-time)
    public void listenToChildData(String parentId, String childId, DatabaseCallback<DataSnapshot> callback) {
        String path = USERS_PATH + "/" + parentId + "/childrenList/" + childId;
        readData(path).addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                callback.onCompleted(snapshot);
            }
            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                callback.onFailed(error.toException());
            }
        });
    }

    /// עדכון אווטר של הורה
    public void updateParentAvatar(String parentId, String avatarValue, DatabaseCallback<Void> callback) {
        String path = USERS_PATH + "/" + parentId + "/avatar";
        writeData(path, avatarValue, callback);
    }

    /// עדכון אווטר של ילד
    public void updateChildAvatar(String parentId, String childId, String avatarValue, DatabaseCallback<Void> callback) {
        String path = USERS_PATH + "/" + parentId + "/childrenList/" + childId + "/avatar";
        writeData(path, avatarValue, callback);
    }

    /// עדכון רמת ילד לאחר מבדק
    public void updateChildLevel(String parentId, String childId, Map<String, Object> updates, DatabaseCallback<Void> callback) {
        String path = USERS_PATH + "/" + parentId + "/childrenList/" + childId;
        readData(path).updateChildren(updates)
                .addOnSuccessListener(aVoid -> { if (callback != null) callback.onCompleted(null); })
                .addOnFailureListener(e -> { if (callback != null) callback.onFailed(e); });
    }

    /// שמירת פרופיל משתמש
    public void saveUserProfile(String userId, UserParent user, DatabaseCallback<Void> callback) {
        writeData(USERS_PATH + "/" + userId, user, callback);
    }

    /// טעינת שאלות מבדק
    public void loadPlacementQuestions(String levelPath, DatabaseCallback<DataSnapshot> callback) {
        readData("PlacementTests/" + levelPath).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                callback.onCompleted(snapshot);
            }
            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                callback.onFailed(error.toException());
            }
        });
    }

    /// איפוס התקדמות כל הנושאים
    public void resetAllSubjectsProgress(String parentId, String childId, String ageGroup, Map<String, Object> updates, DatabaseCallback<Void> callback) {
        String path = USERS_PATH + "/" + parentId + "/childrenList/" + childId + "/progress/" + ageGroup;
        readData(path).updateChildren(updates)
                .addOnSuccessListener(aVoid -> { if (callback != null) callback.onCompleted(null); })
                .addOnFailureListener(e -> { if (callback != null) callback.onFailed(e); });
    }

    /// בדיקת עליות רמה לסטטיסטיקות
    public void loadAllUsersData(DatabaseCallback<DataSnapshot> callback) {
        readData(USERS_PATH).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                callback.onCompleted(snapshot);
            }
            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                callback.onFailed(error.toException());
            }
        });
    }

    /// האזנה להתקדמות נושאים של ילד (real-time)
    public void listenToChildProgress(String parentId, String childId, String ageGroup, DatabaseCallback<DataSnapshot> callback) {
        String path = USERS_PATH + "/" + parentId + "/childrenList/" + childId + "/progress/" + ageGroup;
        readData(path).addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                callback.onCompleted(snapshot);
            }
            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                callback.onFailed(error.toException());
            }
        });
    }

    public void loadFlashCards(String subject, DatabaseCallback<DataSnapshot> callback) {
        readData("FlashCards/" + subject).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                callback.onCompleted(snapshot);
            }
            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                callback.onFailed(error.toException());
            }
        });
    }
}