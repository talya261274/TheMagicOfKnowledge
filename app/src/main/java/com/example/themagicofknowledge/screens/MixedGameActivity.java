package com.example.themagicofknowledge.screens;


// ===== Imports =====

// ----- כלים גרפיים -----
// ColorStateList - "רשימת מצבי צבע" - לכפתורים שמשנים צבע במצבים שונים
import android.content.res.ColorStateList;
// Color - מחלקה עם פונקציות להמרת צבעים (parseColor, RGB וכו')
import android.graphics.Color;

// ----- כלים בסיסיים של אנדרואיד -----
import android.os.Bundle;
// Handler + Looper - לתזמון פעולות (כמו "תעשה את זה אחרי שנייה")
// משתמשים בזה כדי להציג תשובה נכונה למשך שנייה לפני שעוברים לשאלה הבאה
import android.os.Handler;
import android.os.Looper;

// TextToSpeech - מחלקה שמשמיעה טקסט בקול ("יודע לדבר")
// משמש לקריאת השאלות באודיו
import android.speech.tts.TextToSpeech;

// DisplayMetrics - מידע על המסך (רוחב, גובה, צפיפות פיקסלים)
import android.util.DisplayMetrics;
import android.util.Log;

// DragEvent - אירועי גרירה (drag and drop)
// משמש במשחק התאמת הזוגות
import android.view.DragEvent;
import android.view.View;

// רכיבי UI שונים
import android.widget.Button;
import android.widget.EditText;
import android.widget.GridView;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.RatingBar;     // כוכבים (לדיאלוג סיום)
import android.widget.TextView;

import androidx.annotation.NonNull;
// AlertDialog - דיאלוג חלון קופץ ("האם להמשיך?")
import androidx.appcompat.app.AlertDialog;
// AppCompatActivity - האב של ה-Activity (שימי לב - לא BaseActivity!)
import androidx.appcompat.app.AppCompatActivity;

import com.example.themagicofknowledge.R;

// ----- האדפטרים שלנו -----
// אדפטר = מחלקה שמתרגמת נתונים לתצוגה
// KeyboardAdapter = למקלדת העברית במשחק השלמת המשפט
import com.example.themagicofknowledge.adapter.KeyboardAdapter;
// MemoryAdapter = לקלפים במשחק הזיכרון
import com.example.themagicofknowledge.adapter.MemoryAdapter;

// ----- המודלים שלנו -----
import com.example.themagicofknowledge.models.MemoryCard;     // קלף במשחק הזיכרון
import com.example.themagicofknowledge.models.Pair;           // זוג במשחק ההתאמה
import com.example.themagicofknowledge.models.Question;       // שאלת בחירה
import com.example.themagicofknowledge.models.SentenceQuestion; // שאלת השלמת משפט
import com.example.themagicofknowledge.models.UnifiedQuestion;  // "שאלה מאוחדת" - מאחד את כל הסוגים
import com.example.themagicofknowledge.models.UserChild;

// ----- שירותים -----
import com.example.themagicofknowledge.services.DatabaseService;
import com.example.themagicofknowledge.utils.SharedPreferencesUtil;

// MaterialButton - כפתור עם עיצוב Material Design (יותר מודרני מ-Button רגיל)
import com.google.android.material.button.MaterialButton;

// ----- Firebase -----
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.Query;       // שאילתה ל-Firebase
import com.google.firebase.database.ValueEventListener;

import java.util.*;

public class MixedGameActivity extends AppCompatActivity {

    private static final String TAG = "MixedGameActivity";

    // ===== מערך של אותיות עבריות למקלדת =====
    // הסדר הוא ימין-לשמאל (כי עברית RTL)
    // DEL = כפתור מחיקה
    private final String[] hebrewLetters = {
            "ו", "ה", "ד", "ג", "ב", "א", "DEL",
            "מ", "ל", "כ", "י", "ט", "ח", "ז",
            "ר", "ק", "צ", "פ", "ע", "ס", "נ",
            "ץ", "ף", "ן", "ם", "ך", "ת", "ש",
    };

    // ===== נתוני המשחק =====

    // רשימת כל השאלות מכל הסוגים (UnifiedQuestion עוטף כל סוג)
    private List<UnifiedQuestion> allQuestions = new ArrayList<>();

    // איזו שאלה מוצגת כרגע (0 = ראשונה)
    private int currentIndex = 0;

    // כמה טעויות עשה הילד (חשוב לחישוב ציון)
    private int attempts = 0;

    // גודל קלף בפיקסלים (לחישוב גודל הרשת)
    private int cardSize;

    // הנושא הנוכחי (animals/colors/numbers וכו')
    private String subject;

    // דגל למניעת לחיצות כפולות בזמן עיבוד תשובה
    // אם הילד לוחץ פעמיים מהר, רק הלחיצה הראשונה תיספר
    private boolean isProcessingAnswer = false;

    // המידע על הילד הפעיל (גיל, רמה וכו')
    private UserChild currentChild;

    // אובייקט TextToSpeech להשמעת קול
    private TextToSpeech tts;


    // ===== רכיבי UI =====

    // הפס המראה כמה התקדמנו
    private ProgressBar globalProgress;

    // הכותרת המראה את סוג השאלה הנוכחי
    private TextView tvQuestionTitle;

    // 4 הקונטיינרים (אזורי תצוגה) של 4 סוגי שאלות
    // בכל זמן רק אחד מהם גלוי
    private View containerSelection,    // לAUDIO/IMAGE
            containerMatching,     // להתאמת זוגות
            containerSentence,     // להשלמת משפט
            containerMemory;       // למשחק זיכרון


    // ===== רכיבים לשאלות בחירה (AUDIO/IMAGE) =====

    // כפתור השמעת השאלה (רק במצב AUDIO)
    private MaterialButton btnPlayAudio;

    // התמונה של השאלה (רק במצב IMAGE)
    private ImageView ivQuestionImage;

    // 4 כפתורי תשובה
    // final = ההפניה למערך לא תשתנה (אבל התוכן שלו כן)
    private final MaterialButton[] selectionButtons = new MaterialButton[4];

    // 4 צבעים לכפתורי התשובה (כתום, ירוק, כחול, ורוד)
    private final String[] buttonColors = {"#FF9800", "#4CAF50", "#2196F3", "#E91E63"};


    // ===== רכיבים למשחק ההתאמה =====

    // 2 עמודות של פריטים להתאים ביניהם
    private LinearLayout leftColumn, rightColumn;

    // כמה התאמות נכונות מצא הילד עד עכשיו
    private int matchesFound = 0;

    // סך הכל זוגות בשאלה הנוכחית
    private int totalPairs;


    // ===== ניהול טעינת הנתונים =====

    // tasksToLoad = כמה סוגי שאלות צריכים להיטען (3 לכל גיל)
    private int tasksToLoad = 3;

    // tasksCompleted = כמה כבר נטענו
    // כשהשניים שווים - יודעים שכל הנתונים מוכנים
    private int tasksCompleted = 0;


    // ===== רכיבים למשחק הזיכרון =====

    // GridView - לוח רשת של קלפים
    private GridView gvMemoryBoard;

    // רשימת הקלפים במשחק
    private final List<MemoryCard> memoryCards = new ArrayList<>();

    // האדפטר שמציג את הקלפים על המסך
    private MemoryAdapter memoryAdapter;

    // מעקב אחר הקלפים שהילד הפך
    // כשהוא הופך 2 - בודקים אם הם זהים
    private MemoryCard firstMemorySelected = null;
    private MemoryCard secondMemorySelected = null;

    // דגל שמונע מהילד להפוך קלפים בזמן בדיקת התאמה
    private boolean isMemoryBusy = false;

    // ===== הפונקציה הראשית - נקראת בעת יצירת המסך =====
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // טעינת ה-Layout
        setContentView(R.layout.activity_mixed_game);

        // ===== קבלת הנתונים מהמסך הקודם =====
        // getIntent() - מחזיר את ה-Intent ששלח אותנו לכאן
        // getStringExtra("subject") - קבלת מחרוזת שצורפה ל-Intent
        subject = getIntent().getStringExtra("subject");

        // קבלת הילד הפעיל מ-SharedPreferences
        currentChild = SharedPreferencesUtil.getCurrentChild(this);

        // ===== בדיקת תקינות - הגנה מקריסה =====
        // אם משום מה אין ילד או נושא - יוצאים מהמסך
        if (currentChild == null || subject == null) {
            Log.e(TAG, "Child or subject is null. Finishing.");
            finish();  // סוגרים את המסך
            return;    // יוצאים מהפונקציה
        }

        // אתחול רכיבי UI (חיבור למשתנים)
        initViews();

        // אתחול ה-TextToSpeech
        setupTTS();

        // טעינת כל השאלות מ-Firebase
        loadAllData();
    }


    // ===== חיבור הרכיבים מה-XML למשתנים =====
    private void initViews() {
        // findViewById לכל רכיב לפי ה-id שלו ב-XML
        globalProgress = findViewById(R.id.globalProgress);
        tvQuestionTitle = findViewById(R.id.tvMixedQuestionTitle);

        // ארבעת הקונטיינרים (לכל סוג שאלה יש קונטיינר משלו)
        containerSelection = findViewById(R.id.containerSelection);
        containerMatching = findViewById(R.id.containerMatching);
        containerSentence = findViewById(R.id.containerSentence);
        containerMemory = findViewById(R.id.containerMemory);

        btnPlayAudio = findViewById(R.id.btnMixedPlayAudio);
        ivQuestionImage = findViewById(R.id.ivMixedImage);

        // 4 כפתורי תשובה
        selectionButtons[0] = findViewById(R.id.btnAns1);
        selectionButtons[1] = findViewById(R.id.btnAns2);
        selectionButtons[2] = findViewById(R.id.btnAns3);
        selectionButtons[3] = findViewById(R.id.btnAns4);

        leftColumn = findViewById(R.id.mixedLeftColumn);
        rightColumn = findViewById(R.id.mixedRightColumn);

        gvMemoryBoard = findViewById(R.id.gvMemoryBoard);

        // ===== חישוב גודל קלף למשחק הזיכרון =====
        // זה חישוב מתמטי כדי שהקלפים יתאימו למסך
        DisplayMetrics metrics = getResources().getDisplayMetrics();
        // metrics.widthPixels = רוחב המסך בפיקסלים
        // metrics.density = כמה פיקסלים בכל dp (יחידה לוגית)
        // 24 * metrics.density = 24dp של padding
        int screenWidth = metrics.widthPixels - (int) (24 * metrics.density);
        // 3 קלפים בשורה, 24dp רווחים ביניהם
        cardSize = (screenWidth - (int) (24 * metrics.density)) / 3;
    }


    // ===== אתחול TextToSpeech (הסבר קולי בעברית) =====
    private void setupTTS() {
        // יוצרים אובייקט TTS עם callback שיופעל אחרי שהוא מוכן
        tts = new TextToSpeech(this, status -> {
            // אם האתחול הצליח - מגדירים שפה לעברית
            if (status == TextToSpeech.SUCCESS) tts.setLanguage(new Locale("he"));
        });
    }

    // ===== טעינת כל סוגי השאלות מ-Firebase =====
    // כל גיל מקבל סוגי שאלות שונים שמתאימים לרמה שלו
    private void loadAllData() {
        // הגיל הוא במבנה "3-4" אבל ב-Firebase שמורים כ-"3_4"
        String level = currentChild.getAgeGroup().replace("-", "_");

        // הפניה לתיקיית "Games" ב-Firebase
        DatabaseReference rootRef = FirebaseDatabase.getInstance().getReference("Games");

        // איפוס מונה הטעינה
        tasksCompleted = 0;
        allQuestions.clear();

        // ===== החלטה איזה סוגי משחקים לטעון לפי הגיל =====
        if (level.equals("3_4") || level.equals("5_6")) {
            // גיל קטן: AUDIO + MATCHING + MEMORY
            // (אין שאלות טקסט כי הילדים עוד לא קוראים)
            tasksToLoad = 3;

            // טעינה של כל סוג שאלה - שלוש קריאות מקבילות ל-Firebase
            loadCategory(rootRef.child("audioRecognition").child("level_" + level).child(subject),
                    UnifiedQuestion.Type.AUDIO, Question.class);
            loadCategory(rootRef.child("matching").child("level_" + level).child(subject).child("pairs"),
                    UnifiedQuestion.Type.MATCHING, Pair.class);
            loadCategory(rootRef.child("memoryGame").child("level_" + level).child(subject),
                    UnifiedQuestion.Type.MEMORY, DataSnapshot.class);

        } else if (level.equals("7_8")) {
            // גיל גדול: IMAGE + SENTENCE + MEMORY (קריאה!)
            tasksToLoad = 3;
            loadCategory(rootRef.child("imageRecognition").child("level_" + level).child(subject),
                    UnifiedQuestion.Type.IMAGE, Question.class);
            loadCategory(rootRef.child("sentenceCompletion").child("level_" + level).child(subject),
                    UnifiedQuestion.Type.SENTENCE, SentenceQuestion.class);
            loadCategory(rootRef.child("memoryGame").child("level_" + level).child(subject),
                    UnifiedQuestion.Type.MEMORY, DataSnapshot.class);

        } else {
            // ברירת מחדל - אם הגיל לא מוכר
            tasksToLoad = 1;
            loadCategory(rootRef.child("audioRecognition").child("level_3_4").child(subject),
                    UnifiedQuestion.Type.AUDIO, Question.class);
        }
    }


    // ===== טעינת קטגוריה אחת מ-Firebase =====
    // משתמשים בה לכל סוג שאלה
    private void loadCategory(Query query, UnifiedQuestion.Type type, Class<?> modelClass) {
        // addListenerForSingleValueEvent - "תקרא לי פעם אחת עם הנתונים"
        query.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                try {
                    // ===== טיפול לפי סוג השאלה =====

                    if (type == UnifiedQuestion.Type.MEMORY) {
                        // משחק זיכרון - מחלקים את הפריטים לקבוצות של 3
                        // כל קבוצה תהיה משחק זיכרון נפרד
                        List<DataSnapshot> allItems = new ArrayList<>();
                        for (DataSnapshot item : snapshot.getChildren()) {
                            allItems.add(item);
                        }
                        // chunkSize = גודל הקבוצה
                        int chunkSize = 3;
                        // לולאה שמחלקת ל-chunks של 3
                        for (int i = 0; i < allItems.size(); i += chunkSize) {
                            // Math.min - מבטיח שלא נצא מגבולות הרשימה
                            int end = Math.min(i + chunkSize, allItems.size());
                            // subList - מחזיר תת-רשימה
                            allQuestions.add(new UnifiedQuestion(type, new ArrayList<>(allItems.subList(i, end))));
                        }

                    } else if (type == UnifiedQuestion.Type.MATCHING) {
                        // משחק התאמה - מחלקים לקבוצות של 4 זוגות
                        List<Pair> pairs = new ArrayList<>();
                        for (DataSnapshot ds : snapshot.getChildren()) {
                            // המרת הנתון לאובייקט Pair
                            Pair p = ds.getValue(Pair.class);
                            if (p != null) pairs.add(p);
                        }
                        if (!pairs.isEmpty()) {
                            int chunkSize = 4;
                            for (int i = 0; i < pairs.size(); i += chunkSize) {
                                int end = Math.min(i + chunkSize, pairs.size());
                                allQuestions.add(new UnifiedQuestion(type, new ArrayList<>(pairs.subList(i, end))));
                            }
                        }

                    } else {
                        // AUDIO/IMAGE/SENTENCE - שאלה אחת = ערך אחד
                        for (DataSnapshot ds : snapshot.getChildren()) {
                            // ממירים לפי הסוג שהועבר (Question או SentenceQuestion)
                            Object data = ds.getValue(modelClass);
                            if (data != null) allQuestions.add(new UnifiedQuestion(type, data));
                        }
                    }
                } catch (Exception e) {
                    // אם משהו השתבש בהמרה - רושמים בלוג ולא קורסים
                    Log.e(TAG, "Error parsing " + type + " data: " + e.getMessage());
                }

                // בכל מקרה - מסמנים שעוד טעינה הסתיימה
                checkIfLoadingFinished();
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.e(TAG, "Database error loading " + type + ": " + error.getMessage());
                checkIfLoadingFinished();
            }
        });
    }


    // ===== בדיקה אם כל הטעינות הסתיימו =====
    // פונקציה שנקראת אחרי כל טעינה כדי לבדוק אם זה הזמן להתחיל
    private void checkIfLoadingFinished() {
        tasksCompleted++;  // מוסיפים 1 למונה

        // אם כל הטעינות הסתיימו
        if (tasksCompleted >= tasksToLoad) {

            // אם משום מה לא נטענו שאלות - מציגים הודעה ויוצאים
            if (allQuestions.isEmpty()) {
                Log.w(TAG, "No questions loaded.");
                new AlertDialog.Builder(this)
                        .setTitle("הקוסם נח כרגע")
                        .setMessage("עוד אין שאלות בנושא הזה. בוא ננסה נושא אחר!")
                        .setPositiveButton("אוקיי", (d, w) -> finish())
                        .setCancelable(false)  // לא ניתן לסגור עם כפתור Back
                        .show();
                return;
            }

            // עירבוב חכם של השאלות (לא רק random רגיל)
            allQuestions = interleaveQuestions(allQuestions);

            // בדיקה אם יש התקדמות שמורה (לחזור מאיפה שעצרנו)
            checkSavedProgress();
        }
    }


    // ===== עירבוב חכם של השאלות =====
    // המטרה: לסדר את השאלות כך שלא יהיו 3 שאלות אודיו ברצף
    // הטכניקה: מקבצים לפי סוג, מערבבים כל קבוצה, ואז שולפים אחת מכל סוג בכל סבב
    private List<UnifiedQuestion> interleaveQuestions(List<UnifiedQuestion> original) {
        // יצירת מפה: סוג שאלה -> רשימת שאלות מהסוג הזה
        Map<UnifiedQuestion.Type, List<UnifiedQuestion>> grouped = new HashMap<>();

        // יצירת רשימה ריקה לכל סוג
        for (UnifiedQuestion.Type type : UnifiedQuestion.Type.values()) {
            grouped.put(type, new ArrayList<>());
        }

        // קיבוץ השאלות לפי סוג
        for (UnifiedQuestion q : original) {
            List<UnifiedQuestion> list = grouped.get(q.getType());
            if (list != null) list.add(q);
        }

        // עירבוב כל קבוצה בנפרד (כדי שגם הסדר בתוך כל סוג יהיה אקראי)
        for (List<UnifiedQuestion> list : grouped.values()) {
            Collections.shuffle(list);
        }

        // עכשיו - בנייה של רשימה מעורבבת
        List<UnifiedQuestion> interleaved = new ArrayList<>();
        boolean added;

        // do-while = רץ לפחות פעם אחת, וממשיך כל עוד התנאי מתקיים
        do {
            added = false;

            // עירבוב של סדר הסוגים בכל סבב
            // ככה לא יהיה תמיד אותו סדר (AUDIO, IMAGE, MATCHING...)
            List<UnifiedQuestion.Type> types = new ArrayList<>(grouped.keySet());
            Collections.shuffle(types);

            // לקיחה של שאלה אחת מכל סוג
            for (UnifiedQuestion.Type type : types) {
                List<UnifiedQuestion> list = grouped.get(type);
                if (list != null && !list.isEmpty()) {
                    // remove(0) - מסיר את הראשון ומחזיר אותו
                    interleaved.add(list.remove(0));
                    added = true;
                }
            }
        } while (added);  // ממשיכים עד שכל הקבוצות ריקות

        return interleaved;
    }

    // ===== בדיקה אם יש התקדמות שמורה =====
    // אם הילד התחיל לתרגל ועצר באמצע - שואלים אם להמשיך
    private void checkSavedProgress() {
        // הגנה - אם משום מה אין משתמש מחובר
        if (SharedPreferencesUtil.getUser(this) == null) {
            showCurrentQuestion();
            return;
        }

        // קבלת ה-ID של ההורה
        String pId = SharedPreferencesUtil.getUser(this).getId();

        // הפניה לנתיב של ההתקדמות הספציפית הזו
        DatabaseReference ref = FirebaseDatabase.getInstance().getReference("users")
                .child(pId).child("childrenList").child(currentChild.getId())
                .child("progress").child(currentChild.getAgeGroup()).child(subject);

        // בדיקת שדה lastQuestionIndex (השאלה האחרונה שהילד היה בה)
        ref.child("lastQuestionIndex").addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (snapshot.exists()) {
                    Integer savedIdx = snapshot.getValue(Integer.class);

                    // savedIdx > 0 = יש התקדמות
                    // savedIdx < allQuestions.size() = ההתקדמות עדיין רלוונטית
                    if (savedIdx != null && savedIdx > 0 && savedIdx < allQuestions.size()) {
                        showContinueDialog(savedIdx);
                        return;
                    }
                }
                // אין התקדמות - מתחילים מההתחלה
                showCurrentQuestion();
            }
            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                // אם הקריאה נכשלה - מתחילים מההתחלה
                showCurrentQuestion();
            }
        });
    }


    // ===== דיאלוג "להמשיך מאיפה שעצרנו?" =====
    private void showContinueDialog(int savedIdx) {
        new AlertDialog.Builder(this)
                .setTitle("להמשיך מאיפה שעצרנו?")
                .setMessage("נראה שכבר התחלת לתרגל נושא זה.")
                .setPositiveButton("כן", (d, w) -> {
                    // ממשיכים מהמקום השמור
                    currentIndex = savedIdx;
                    showCurrentQuestion();
                })
                .setNegativeButton("מהתחלה", (d, w) -> {
                    // מתחילים מהתחלה
                    currentIndex = 0;
                    showCurrentQuestion();
                })
                .setCancelable(false).show();
    }

    // ===== הצגת השאלה הנוכחית =====
    // הפונקציה הזו נקראת אחרי כל תשובה - היא בוחרת איזה סוג שאלה להציג
    private void showCurrentQuestion() {
        isProcessingAnswer = false;  // איפוס דגל הלחיצה הכפולה

        // אם הגענו לסוף הרשימה - גמרנו!
        if (currentIndex >= allQuestions.size()) {
            finishGame();
            return;
        }

        hideAllLayouts();  // מסתירים את כל הקונטיינרים
        updateProgressInFirebase();  // מעדכנים את ההתקדמות בענן

        // קבלת השאלה הנוכחית
        UnifiedQuestion uq = allQuestions.get(currentIndex);
        Log.d(TAG, "Showing question " + currentIndex + " type: " + uq.getType());

        try {
            // switch על הסוג - להציג את הקונטיינר הנכון
            switch (uq.getType()) {
                case AUDIO:
                    displayAudioQuestion((Question) uq.getData());
                    break;
                case IMAGE:
                    displayImageQuestion((Question) uq.getData());
                    break;
                case MATCHING:
                    displayMatchingQuestion((List<Pair>) uq.getData());
                    break;
                case SENTENCE:
                    displaySentenceQuestion((SentenceQuestion) uq.getData());
                    break;
                case MEMORY:
                    displayMemoryGame((List<DataSnapshot>) uq.getData());
                    break;
            }
        } catch (Exception e) {
            // אם משהו השתבש - דולגים לשאלה הבאה
            Log.e(TAG, "Error displaying question: " + e.getMessage());
            currentIndex++;
            showCurrentQuestion();
        }
    }


    // ===== הצגת שאלת אודיו =====
    // הילד שומע מילה ובוחר תמונה
    private void displayAudioQuestion(Question q) {
        containerSelection.setVisibility(View.VISIBLE);  // מציגים את הקונטיינר
        resetSelectionButtonColors();  // מאפסים צבעי כפתורים
        btnPlayAudio.setVisibility(View.VISIBLE);  // מציגים כפתור אודיו
        ivQuestionImage.setVisibility(View.GONE);  // מסתירים את התמונה
        tvQuestionTitle.setText("הקשיבו לשאלה:");

        playTTS(q.getQuestionText());  // משמיעים את השאלה אוטומטית

        // כשלוחצים על כפתור האודיו - משמיעים שוב
        btnPlayAudio.setOnClickListener(v -> playTTS(q.getQuestionText()));

        // הגדרת 4 כפתורי התשובה
        setupSelectionButtons(q);
    }


    // ===== הצגת שאלת תמונה =====
    // הילד רואה תמונה ובוחר את שמה
    private void displayImageQuestion(Question q) {
        containerSelection.setVisibility(View.VISIBLE);
        resetSelectionButtonColors();
        btnPlayAudio.setVisibility(View.GONE);  // מסתירים אודיו
        ivQuestionImage.setVisibility(View.VISIBLE);  // מציגים תמונה
        tvQuestionTitle.setText(q.getQuestionText());

        // טעינת התמונה מ-drawable לפי השם
        // getIdentifier - מחזיר את ה-ID של drawable לפי שם
        int resId = getResources().getIdentifier(q.getMediaUrl(), "drawable", getPackageName());
        // אם לא נמצאה - מציגים תמונת ברירת מחדל
        ivQuestionImage.setImageResource(resId != 0 ? resId : R.drawable.wizard_placeholder);

        setupSelectionButtons(q);
    }


    // ===== הצגת שאלת השלמת משפט =====
    // הילד רואה משפט עם מילה חסרה ומקליד אותה
    private void displaySentenceQuestion(SentenceQuestion sq) {
        containerSentence.setVisibility(View.VISIBLE);
        tvQuestionTitle.setText("השלם את המשפט:");

        // קבלת רכיבי UI ספציפיים לסוג הזה
        ImageView ivHint = findViewById(R.id.ivMixedSentenceHint);
        TextView tvSentence = findViewById(R.id.tvMixedSentenceText);
        EditText etAnswer = findViewById(R.id.etMixedAnswer);
        GridView keyboardGrid = findViewById(R.id.mixedKeyboard);
        Button btnCheck = findViewById(R.id.btnCheck);

        // הגדרת תמונת רמז
        int resId = getResources().getIdentifier(sq.getHintImage(), "drawable", getPackageName());
        ivHint.setImageResource(resId != 0 ? resId : R.drawable.wizard_placeholder);

        // הגדרת המשפט והניקוי שדה התשובה
        tvSentence.setText(sq.getSentence());
        etAnswer.setText("");
        etAnswer.setBackgroundTintList(null);  // איפוס צבע (יכול להיות אדום מתשובה קודמת)

        // יצירת מקלדת עברית מותאמת
        // KeyboardAdapter - אדפטר שלנו שמטפל באותיות
        KeyboardAdapter adapter = new KeyboardAdapter(this, hebrewLetters, letter -> {
            // callback - מה לעשות כשלוחצים על אות
            if (letter.equals("DEL")) {
                // מחיקת אות אחרונה
                String str = etAnswer.getText().toString();
                if (!str.isEmpty()) etAnswer.setText(str.substring(0, str.length() - 1));
            } else {
                // הוספת אות לסוף
                etAnswer.append(letter);
            }
        });
        keyboardGrid.setAdapter(adapter);

        // כפתור הבדיקה
        btnCheck.setOnClickListener(v -> {
            if (isProcessingAnswer) return;  // הגנה מלחיצה כפולה

            // קבלת תשובת המשתמש (ניקוי רווחים)
            String userAns = etAnswer.getText().toString().trim();

            // השוואה ללא רגישות לאותיות גדולות/קטנות
            if (userAns.equalsIgnoreCase(sq.getCorrectAnswer())) {
                isProcessingAnswer = true;
                etAnswer.setBackgroundTintList(ColorStateList.valueOf(Color.GREEN));
                handleCorrect();  // תשובה נכונה
            } else {
                handleWrong(etAnswer);  // תשובה שגויה
            }
        });
    }

    // ===== הצגת משחק זיכרון =====
    // הילד מוצא זוגות של קלפים זהים
    private void displayMemoryGame(List<DataSnapshot> items) {
        containerMemory.setVisibility(View.VISIBLE);
        tvQuestionTitle.setText("משחק הזיכרון - מצאו את הזוגות!");
        memoryCards.clear();  // ניקוי קלפים מהמשחק הקודם

        // יצירת קלפים מכל הפריטים
        for (DataSnapshot ds : items) {
            // קבלת שם התמונה והשם המוצג
            String imageName = ds.child("image").getValue(String.class);
            String displayName = ds.child("name").getValue(String.class);

            if (imageName != null && displayName != null) {
                int resId = getResources().getIdentifier(imageName, "drawable", getPackageName());
                addPairToMemoryList(resId, displayName);
            }
        }

        // עירבוב הקלפים על הלוח
        Collections.shuffle(memoryCards);

        // יצירת אדפטר ושליחתו ל-GridView
        memoryAdapter = new MemoryAdapter(this, memoryCards, cardSize);
        gvMemoryBoard.setAdapter(memoryAdapter);

        // מאזין ללחיצה על קלף
        gvMemoryBoard.setOnItemClickListener((parent, view, position, id) -> {
            if (isMemoryBusy) return;  // אם בודק התאמה - לא לוחצים

            MemoryCard selected = memoryCards.get(position);

            // אם הקלף כבר הותאם או הפוך - לא עושים כלום
            if (selected.isMatched() || selected.isFlipped()) return;

            selected.setFlipped(true);  // הופכים את הקלף
            memoryAdapter.notifyDataSetChanged();  // מעדכנים את התצוגה

            // לוגיקת בחירה - שתי לחיצות בודקים התאמה
            if (firstMemorySelected == null) {
                firstMemorySelected = selected;  // קלף ראשון
            } else {
                secondMemorySelected = selected;  // קלף שני
                checkMemoryMatch();  // בודקים אם זוג
            }
        });
    }


    // ===== הוספת זוג קלפים לרשימה =====
    // התנהגות שונה לפי גיל - חכם!
    private void addPairToMemoryList(int resId, String name) {
        String ageGroup = currentChild.getAgeGroup();

        if (ageGroup.equals("3-4") || ageGroup.equals("5-6")) {
            // גיל קטן - 2 קלפי תמונה זהים
            memoryCards.add(new MemoryCard(resId, null, name));
            memoryCards.add(new MemoryCard(resId, null, name));
        } else {
            // גיל גדול - קלף תמונה + קלף טקסט (פיתוח קריאה)
            memoryCards.add(new MemoryCard(resId, null, name));
            memoryCards.add(new MemoryCard(0, name, name));  // resId=0 = רק טקסט
        }
    }


    // ===== בדיקת התאמה במשחק הזיכרון =====
    private void checkMemoryMatch() {
        isMemoryBusy = true;  // חוסמים לחיצות נוספות

        // משתמשים ב-Handler להמתין שניה לפני הבדיקה
        // (כדי שהילד יספיק לראות את הקלפים)
        new Handler(Looper.getMainLooper()).postDelayed(() -> {
            // השוואה - האם המזהה של שני הקלפים זהה?
            if (firstMemorySelected.getMatchId().equals(secondMemorySelected.getMatchId())) {
                // התאמה!
                firstMemorySelected.setMatched(true);
                secondMemorySelected.setMatched(true);

                // אם כל הזוגות הותאמו - תשובה נכונה
                if (checkAllMemoryMatched()) handleCorrect();
            } else {
                // לא התאמה - הופכים את הקלפים בחזרה
                firstMemorySelected.setFlipped(false);
                secondMemorySelected.setFlipped(false);
                attempts++;  // מוסיפים טעות
            }

            // איפוס לסבב הבא
            firstMemorySelected = null;
            secondMemorySelected = null;
            memoryAdapter.notifyDataSetChanged();
            isMemoryBusy = false;
        }, 800);  // המתנה של 800 מילישניות
    }


    // ===== בדיקה אם כל הקלפים הותאמו =====
    private boolean checkAllMemoryMatched() {
        for (MemoryCard c : memoryCards) {
            if (!c.isMatched()) return false;  // אם יש לפחות אחד שלא הותאם
        }
        return true;  // כולם הותאמו!
    }

    // ===== הצגת משחק התאמת זוגות =====
    private void displayMatchingQuestion(List<Pair> pairs) {
        containerMatching.setVisibility(View.VISIBLE);
        tvQuestionTitle.setText("התאימו את הזוגות!");

        // ניקוי תוכן קודם משתי העמודות
        leftColumn.removeAllViews();
        rightColumn.removeAllViews();

        matchesFound = 0;
        totalPairs = pairs.size();

        // עירבוב נפרד לכל עמודה (הסדר לא יהיה זהה)
        List<Pair> leftSide = new ArrayList<>(pairs);
        List<Pair> rightSide = new ArrayList<>(pairs);
        Collections.shuffle(leftSide);
        Collections.shuffle(rightSide);

        // יצירת View לכל פריט
        for (Pair p : leftSide) setupMatchingView(p.getLeft(), p.getId(), leftColumn);
        for (Pair p : rightSide) setupMatchingView(p.getRight(), p.getId(), rightColumn);
    }


    // ===== יצירת View אחד למשחק ההתאמה =====
    private void setupMatchingView(String content, String id, LinearLayout column) {
        // יצירת כפתור מקובץ XML
        MaterialButton btn = (MaterialButton) getLayoutInflater().inflate(R.layout.item_matching_button, column, false);
        // setTag - שומר ערך נסתר ב-View. ככה נדע איזה זוג זה
        btn.setTag(id);

        if (content != null) {
            // ===== החלטה איך להציג את התוכן =====

            if (content.startsWith("ss_")) {
                // תמונה (כל התמונות מתחילות ב-ss_)
                int resId = getResources().getIdentifier(content, "drawable", getPackageName());
                btn.setIconResource(resId != 0 ? resId : R.drawable.wizard_placeholder);
                btn.setText("");
                btn.setIconSize(180);
                btn.setIconTint(null);

            } else if (content.equalsIgnoreCase("audio") || content.equalsIgnoreCase("speaker")) {
                // קלף קולי - אייקון רמקול שמשמיע אודיו
                btn.setIconResource(R.drawable.ic_volume_up);
                btn.setText("");
                btn.setIconSize(150);
                btn.setIconTint(ColorStateList.valueOf(Color.parseColor("#2196F3")));

                // בלחיצה - להשמיע את ה-id (שזה השם של החיה/צבע וכו')
                btn.setOnClickListener(v -> playTTS(id));

            } else {
                // טקסט רגיל
                btn.setText(content);
                btn.setIconResource(0);
            }
        }

        // ===== הגדרת drag (גרירה) =====
        // OnLongClickListener = לחיצה ארוכה
        btn.setOnLongClickListener(v -> {
            // יצירת "צל" של ה-View שיוצג בזמן הגרירה
            View.DragShadowBuilder shadowBuilder = new View.DragShadowBuilder(v);
            // התחלת גרירה
            v.startDragAndDrop(null, shadowBuilder, v, 0);
            v.setVisibility(View.INVISIBLE);  // מסתירים את המקור
            return true;
        });

        // ===== הגדרת drop (שחרור על יעד) =====
        btn.setOnDragListener((v, event) -> {
            switch (event.getAction()) {
                case DragEvent.ACTION_DRAG_STARTED:
                    // התחילה גרירה - חייבים להחזיר true כדי לקבל אירועים
                    return true;

                case DragEvent.ACTION_DRAG_ENTERED:
                    // הגרירה נכנסה לאזור הזה - מסמנים בצהוב
                    v.setBackgroundTintList(ColorStateList.valueOf(Color.parseColor("#FFE082")));
                    return true;

                case DragEvent.ACTION_DRAG_EXITED:
                    // הגרירה יצאה - חוזרים ללבן
                    v.setBackgroundTintList(ColorStateList.valueOf(Color.WHITE));
                    return true;

                case DragEvent.ACTION_DROP:
                    // הגרירה שוחררה כאן!
                    View draggedView = (View) event.getLocalState();  // ה-View שגררנו

                    // בדיקה - האם זה זוג נכון?
                    // 1. draggedView != null - יש view גרור
                    // 2. parent שונה - לא גוררים בתוך אותה עמודה
                    // 3. tags זהים - אותו זוג
                    if (draggedView != null && draggedView.getParent() != v.getParent()
                            && draggedView.getTag().equals(v.getTag())) {
                        // התאמה!
                        v.setVisibility(View.INVISIBLE);
                        draggedView.setVisibility(View.INVISIBLE);
                        matchesFound++;

                        // אם כל הזוגות הותאמו
                        if (matchesFound == totalPairs) handleCorrect();
                    } else {
                        // לא התאמה - מציגים שגיאה
                        handleWrong(v);
                        if (draggedView != null) draggedView.setVisibility(View.VISIBLE);
                    }
                    return true;

                case DragEvent.ACTION_DRAG_ENDED:
                    // הגרירה הסתיימה (הצלחה או כישלון)
                    if (!event.getResult()) btn.setVisibility(View.VISIBLE);
                    return true;
            }
            return false;
        });

        // הוספת ה-View לעמודה
        column.addView(btn);
    }


    // ===== הגדרת 4 כפתורי התשובה (לAUDIO/IMAGE) =====
    private void setupSelectionButtons(Question q) {
        List<String> options = q.getOptions();

        // לכל כפתור (4 בסך הכל)
        for (int i = 0; i < 4; i++) {
            final int index = i;  // final כי משתמשים ב-Lambda
            String item = options.get(i);

            // בדיקה אם הערך הוא תמונה (יש drawable כזה?)
            int resId = getResources().getIdentifier(item, "drawable", getPackageName());

            if (resId != 0) {
                // יש תמונה - מציגים אותה
                selectionButtons[i].setIconResource(resId);
                selectionButtons[i].setIconTint(null);  // ללא tint
                selectionButtons[i].setText("");
                selectionButtons[i].setIconSize(220);
                selectionButtons[i].setIconGravity(MaterialButton.ICON_GRAVITY_TEXT_START);
            } else {
                // אין תמונה - מציגים טקסט
                selectionButtons[i].setIconResource(0);
                selectionButtons[i].setText(item);
            }

            // מאזין ללחיצה
            selectionButtons[i].setOnClickListener(v -> {
                if (isProcessingAnswer) return;
                isProcessingAnswer = true;

                // השוואה לתשובה הנכונה
                if (index == q.getCorrectAnswerIndex()) {
                    handleCorrect();
                } else {
                    isProcessingAnswer = false;  // נוכל לנסות שוב
                    handleWrong(selectionButtons[index]);
                }
            });
        }
    }

    // ===== טיפול בתשובה נכונה =====
    private void handleCorrect() {
        currentIndex++;  // מתקדמים לשאלה הבאה

        // המתנה של שניה לפני המעבר (כדי שהילד יראה את הצבע הירוק)
        new Handler(Looper.getMainLooper()).postDelayed(this::showCurrentQuestion, 1000);
        // this::showCurrentQuestion = method reference (קיצור ל-Lambda)
        // שווה ערך ל: () -> showCurrentQuestion()
    }


    // ===== טיפול בתשובה שגויה =====
    private void handleWrong(View view) {
        attempts++;  // מוסיפים טעות

        // שמירת הצבע המקורי
        final ColorStateList originalTint = view.getBackgroundTintList();

        // הצגת אדום
        view.setBackgroundTintList(ColorStateList.valueOf(Color.RED));

        // ===== אנימציית רעידה =====
        // הזזה ימינה
        view.animate().translationX(20).setDuration(50).withEndAction(() ->
                // הזזה שמאלה
                view.animate().translationX(-20).setDuration(50).withEndAction(() ->
                        // חזרה למרכז
                        view.animate().translationX(0).setDuration(50).start()
                ).start()
        ).start();

        // החזרת הצבע המקורי אחרי שניה
        new Handler(Looper.getMainLooper()).postDelayed(() -> {
            boolean restored = false;

            // אם זה אחד מ-4 כפתורי התשובה - מחזירים לצבע הראשוני
            for (int i = 0; i < selectionButtons.length; i++) {
                if (view == selectionButtons[i]) {
                    view.setBackgroundTintList(ColorStateList.valueOf(Color.parseColor(buttonColors[i])));
                    restored = true;
                    break;
                }
            }

            // אחרת - חוזרים לצבע המקורי שנשמר
            if (!restored) view.setBackgroundTintList(originalTint);
        }, 1000);
    }


    // ===== עדכון התקדמות ב-Firebase =====
    // נקרא בכל שאלה כדי לעדכן את אחוז ההתקדמות
    private void updateProgressInFirebase() {
        if (allQuestions.isEmpty()) return;

        // חישוב אחוז התקדמות (15-100, כי 0-15 מיועד ל-flashcards)
        int percent = 15 + (int) (((double) currentIndex / allQuestions.size()) * 85);

        if (SharedPreferencesUtil.getUser(this) == null) return;
        String pId = SharedPreferencesUtil.getUser(this).getId();

        // שימוש ב-DatabaseService שראינו!
        DatabaseService.getInstance().updateDetailedProgress(
                pId, currentChild.getId(), currentChild.getAgeGroup(),
                subject, 0, 0, percent, currentIndex
        );

        // עדכון פס ההתקדמות במסך
        globalProgress.setProgress(percent);
    }


    // ===== איפוס צבעי כפתורי התשובה =====
    private void resetSelectionButtonColors() {
        for (int i = 0; i < selectionButtons.length; i++) {
            if (selectionButtons[i] != null) {
                selectionButtons[i].setBackgroundTintList(
                        ColorStateList.valueOf(Color.parseColor(buttonColors[i]))
                );
                selectionButtons[i].setTextColor(Color.WHITE);
            }
        }
    }


    // ===== הסתרת כל הקונטיינרים =====
    // קוראים לזה לפני הצגת קונטיינר חדש
    private void hideAllLayouts() {
        containerSelection.setVisibility(View.GONE);
        containerMatching.setVisibility(View.GONE);
        containerSentence.setVisibility(View.GONE);
        containerMemory.setVisibility(View.GONE);
    }


    // ===== השמעת טקסט בקול =====
    private void playTTS(String text) {
        if (tts != null) {
            // QUEUE_FLUSH = "תפסיק את מה שאתה מקריא ותתחיל מחדש"
            tts.speak(text, TextToSpeech.QUEUE_FLUSH, null, null);
        }
    }


    // ===== סיום המשחק =====
    private void finishGame() {
        // ===== חישוב ציון =====
        // הנוסחה: (מספר שאלות / (מספר שאלות + טעויות)) * 100
        // לדוגמה: 10 שאלות + 2 טעויות = 10/12 * 100 = 83.3
        double score = ((double) allQuestions.size() / (allQuestions.size() + attempts)) * 100;

        // אם הציון מעל 80 - הצלחה
        if (score >= 80) {
            markSubjectAsCompletedInFirebase();
            showSuccessDialog(score);
        } else {
            showTryAgainDialog(score);  // מציע לנסות שוב
        }
    }


    // ===== דיאלוג "נסה שוב" =====
    private void showTryAgainDialog(double score) {
        // יצירת דיאלוג מותאם
        final android.app.Dialog dialog = new android.app.Dialog(this);
        dialog.setContentView(R.layout.dialog_test_result);

        // רקע שקוף (כדי לראות את הקצוות העגולים שלנו)
        if (dialog.getWindow() != null)
            dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);

        // קבלת הרכיבים מהדיאלוג
        TextView tvTitle = dialog.findViewById(R.id.tvDialogTitle);
        TextView tvMessage = dialog.findViewById(R.id.tvDialogMessage);
        ImageView ivStatus = dialog.findViewById(R.id.ivStatusIcon);
        Button btnAction = dialog.findViewById(R.id.btnDialogAction);
        Button btnStay = dialog.findViewById(R.id.btnStayAtCurrentLevel);
        RatingBar ratingBar = dialog.findViewById(R.id.dialogRatingBar);

        // הגדרת תוכן
        if (tvTitle != null) tvTitle.setText("כמעט הצלחת!");
        if (tvMessage != null) tvMessage.setText("קיבלת " + (int) score + " נקודות.\nכדי לקבל את ה-V צריך לפחות 80.");
        if (ivStatus != null) ivStatus.setImageResource(R.drawable.ic_medal);
        // ratingBar - כוכבים. score/20 = מתוך 5 כוכבים (כי score עד 100)
        if (ratingBar != null) ratingBar.setRating((float) (score / 20));

        if (btnAction != null) {
            btnAction.setText("אני רוצה לנסות שוב!");
            btnAction.setOnClickListener(v -> {
                dialog.dismiss();
                // איפוס המשחק והתחלה מחדש (עם עירבוב חדש)
                currentIndex = 0;
                attempts = 0;
                Collections.shuffle(allQuestions);
                showCurrentQuestion();
            });
        }

        if (btnStay != null) {
            btnStay.setVisibility(View.VISIBLE);
            btnStay.setText("אולי אחר כך");
            btnStay.setOnClickListener(v -> {
                dialog.dismiss();
                finish();  // סוגרים את המסך
            });
        }

        dialog.setCancelable(false);  // לא ניתן לסגור עם Back
        dialog.show();
    }


    // ===== דיאלוג הצלחה =====
    // דומה ל-tryAgain אבל עם טקסטים אחרים
    private void showSuccessDialog(double score) {
        final android.app.Dialog dialog = new android.app.Dialog(this);
        dialog.setContentView(R.layout.dialog_test_result);
        if (dialog.getWindow() != null)
            dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);

        TextView tvTitle = dialog.findViewById(R.id.tvDialogTitle);
        TextView tvMessage = dialog.findViewById(R.id.tvDialogMessage);
        ImageView ivStatus = dialog.findViewById(R.id.ivStatusIcon);
        RatingBar ratingBar = dialog.findViewById(R.id.dialogRatingBar);
        Button btnAction = dialog.findViewById(R.id.btnDialogAction);
        Button btnStay = dialog.findViewById(R.id.btnStayAtCurrentLevel);

        if (tvTitle != null) tvTitle.setText("וואו! אלוף!");
        if (tvMessage != null) tvMessage.setText("סיימת את התרגול בהצלחה!\nעכשיו מופיע לך V ירוק בתפריט.");
        if (ivStatus != null) ivStatus.setImageResource(R.drawable.ic_trophy);
        if (ratingBar != null) ratingBar.setRating((float) (score / 20));

        // אין צורך בכפתור "להישאר" בהצלחה
        if (btnStay != null) btnStay.setVisibility(View.GONE);

        if (btnAction != null) {
            btnAction.setText("חזור לתפריט");
            btnAction.setOnClickListener(v -> { dialog.dismiss(); finish(); });
        }

        dialog.setCancelable(false);
        dialog.show();
    }


    // ===== סימון נושא כהושלם ב-Firebase =====
    private void markSubjectAsCompletedInFirebase() {
        if (SharedPreferencesUtil.getUser(this) == null) return;
        String parentId = SharedPreferencesUtil.getUser(this).getId();

        // נתיב: users/{parentId}/childrenList/{childId}/completedSubjects
        DatabaseReference ref = FirebaseDatabase.getInstance().getReference("users")
                .child(parentId).child("childrenList").child(currentChild.getId())
                .child("completedSubjects");

        // שמירה: completedSubjects/{subject} = true
        ref.child(subject).setValue(true).addOnSuccessListener(aVoid -> {
            // אחרי הצלחה - גם מעדכנים את ההתקדמות ל-100%
            DatabaseService.getInstance().updateDetailedProgress(
                    parentId, currentChild.getId(), currentChild.getAgeGroup(),
                    subject, 0, 0, 100, currentIndex
            );
        });
    }


    // ===== ניקוי כשה-Activity נסגר =====
    // חשוב! TTS צורך משאבים, חייבים לשחרר אותו
    @Override
    protected void onDestroy() {
        if (tts != null) {
            tts.stop();      // מפסיק להקריא
            tts.shutdown();  // משחרר משאבים
        }
        super.onDestroy();
    }
}