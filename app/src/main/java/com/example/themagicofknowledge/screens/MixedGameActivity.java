package com.example.themagicofknowledge.screens;

import android.content.SharedPreferences;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.speech.tts.TextToSpeech;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.DragEvent;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.GridView;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.RatingBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;

import com.example.themagicofknowledge.R;
import com.example.themagicofknowledge.adapter.KeyboardAdapter;
import com.example.themagicofknowledge.adapter.MemoryAdapter;
import com.example.themagicofknowledge.models.MemoryCard;
import com.example.themagicofknowledge.models.Pair;
import com.example.themagicofknowledge.models.Question;
import com.example.themagicofknowledge.models.SentenceQuestion;
import com.example.themagicofknowledge.models.UnifiedQuestion;
import com.example.themagicofknowledge.models.UserChild;
import com.example.themagicofknowledge.services.DatabaseService;
import com.example.themagicofknowledge.utils.SharedPreferencesUtil;
import com.google.android.material.button.MaterialButton;
import com.google.firebase.database.DataSnapshot;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * MixedGameActivity — מסך המשחק הראשי של האפליקציה.
 * מכיל 5 סוגי משחקים שמוצגים אחד אחד לפי רמת הילד:
 * רמות 3-4 ו-5-6: שמע (AUDIO) + התאמה (MATCHING) + זיכרון (MEMORY)
 * רמה 7-8: תמונה (IMAGE) + השלמת משפט (SENTENCE) + זיכרון (MEMORY)
 */
public class MixedGameActivity extends BaseActivity {

    // תג לזיהוי ב-Logcat
    private static final String TAG = "MixedGameActivity";

    // מערך האותיות העבריות למקלדת בשאלות השלמת משפט
    private final String[] hebrewLetters = {
            "ו", "ה", "ד", "ג", "ב", "א", "DEL",
            "מ", "ל", "כ", "י", "ט", "ח", "ז",
            "ר", "ק", "צ", "פ", "ע", "ס", "נ",
            "ץ", "ף", "ן", "ם", "ך", "ת", "ש",
    };

    private EditText etAnswer;
    private List<UnifiedQuestion> allQuestions = new ArrayList<>();
    private int currentIndex = 0;
    private int attempts = 0;
    private int cardSize;
    private String subject;
    private boolean isProcessingAnswer = false;
    private long gameStartTime;
    private UserChild currentChild;
    private TextToSpeech tts;
    private ProgressBar globalProgress;

    private TextView tvQuestionTitle;
    private TextView tvProgressText;
    private View containerSelection, containerMatching, containerSentence, containerMemory;

    private LinearLayout btnPlayAudio;
    private ImageView ivQuestionImage;
    private View imageCardContainer;
    private final ImageView[] selectionButtons = new ImageView[4];
    private final TextView[] selectionTextViews = new TextView[4];

    private final String[] buttonColors = {"#FF9800", "#4CAF50", "#2196F3", "#E91E63"};
    private LinearLayout leftColumn, rightColumn;
    private int matchesFound = 0;
    private int totalPairs;
    private int tasksToLoad = 3;
    private int tasksCompleted = 0;
    private GridView gvMemoryBoard;
    private final List<MemoryCard> memoryCards = new ArrayList<>();
    private MemoryAdapter memoryAdapter;
    private MemoryCard firstMemorySelected = null;
    private MemoryCard secondMemorySelected = null;
    private boolean isMemoryBusy = false;

    @Override
    protected boolean hasSideMenu() {
        return false;
    }

    @Override
    protected boolean showToolbar() {
        return false;
    }

    /**
     * נקודת הכניסה של המסך.
     * שולף את הנושא והילד, מאתחל רכיבים, ומתחיל טעינת נתונים.
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_mixed_game);

        // שליפת הנושא מה-Intent שפתח את המסך
        subject = getIntent().getStringExtra("subject");

        // שליפת הילד המחובר מה-SharedPreferences
        currentChild = SharedPreferencesUtil.getCurrentChild(this);

        // הגנה — אם חסר נתון קריטי, סוגר את המסך
        if (currentChild == null || subject == null) {
            Log.e(TAG, "Child or subject is null. Finishing.");
            finish();
            return;
        }

        // שמירת זמן תחילת המשחק לחישוב משך המשחק
        gameStartTime = System.currentTimeMillis();

        initViews();    // חיבור רכיבי ה-UI
        setupTTS();     // אתחול מנוע הקריאה בקול
        loadAllData();  // טעינת שאלות מ-Firebase
    }

    /**
     * מחבר את כל רכיבי ה-UI לפי ה-ID שלהם מה-XML.
     * גם מחשב את גודל כרטיס הזיכרון לפי גודל המסך.
     */
    private void initViews() {
        globalProgress = findViewById(R.id.globalProgress);
        tvProgressText = findViewById(R.id.tvProgressText);
        tvQuestionTitle = findViewById(R.id.tvMixedQuestionTitle);

        containerSelection = findViewById(R.id.containerSelection);
        containerMatching = findViewById(R.id.containerMatching);
        containerSentence = findViewById(R.id.containerSentence);
        containerMemory = findViewById(R.id.containerMemory);

        imageCardContainer = findViewById(R.id.imageCardContainer);

        // כפתור הקשבה + כפתור יציאה
        btnPlayAudio = findViewById(R.id.btnMixedPlayAudio);
        findViewById(R.id.btnExit).setOnClickListener(v -> showExitDialog());

        ivQuestionImage = findViewById(R.id.ivMixedImage);

        // 4 כפתורי תשובה — תמונה וטקסט לכל אחד
        selectionButtons[0] = findViewById(R.id.btnAns1);
        selectionButtons[1] = findViewById(R.id.btnAns2);
        selectionButtons[2] = findViewById(R.id.btnAns3);
        selectionButtons[3] = findViewById(R.id.btnAns4);

        selectionTextViews[0] = findViewById(R.id.tvAns1);
        selectionTextViews[1] = findViewById(R.id.tvAns2);
        selectionTextViews[2] = findViewById(R.id.tvAns3);
        selectionTextViews[3] = findViewById(R.id.tvAns4);

        // עמודות משחק ההתאמה
        leftColumn = findViewById(R.id.mixedLeftColumn);
        rightColumn = findViewById(R.id.mixedRightColumn);

        gvMemoryBoard = findViewById(R.id.gvMemoryBoard);

        // חישוב גודל כרטיס זיכרון: (רוחב מסך - ריפוד) / 3 עמודות
        DisplayMetrics metrics = getResources().getDisplayMetrics();
        int screenWidth = metrics.widthPixels - (int) (24 * metrics.density);
        cardSize = (screenWidth - (int) (24 * metrics.density)) / 3;
    }

    /**
     * מאתחל את מנוע ה-TTS (קריאת טקסט בקול) בעברית.
     * נקרא פעם אחת ב-onCreate.
     */
    private void setupTTS() {
        tts = new TextToSpeech(this, status -> {
            // status == SUCCESS אומר שהמנוע הצליח להיטען
            if (status == TextToSpeech.SUCCESS) tts.setLanguage(new Locale("he"));
        });
    }

    /**
     * טוען את כל נתוני השאלות מ-Firebase לפי רמת הילד.
     * שולח 3 קריאות במקביל — אחת לכל סוג משחק.
     * רמות 3-4 ו-5-6: AUDIO + MATCHING + MEMORY
     * רמה 7-8: IMAGE + SENTENCE + MEMORY
     */
    private void loadAllData() {
        // המרת "3-4" ל-"3_4" כי Firebase לא תומך במינוס בנתיב
        String level = currentChild.getAgeGroup().replace("-", "_");
        tasksCompleted = 0;
        allQuestions.clear();

        if (level.equals("3_4") || level.equals("5_6")) {
            tasksToLoad = 3;
            loadCategory("Games/audioRecognition/level_" + level + "/" + subject, UnifiedQuestion.Type.AUDIO, Question.class);
            loadCategory("Games/matching/level_" + level + "/" + subject + "/pairs", UnifiedQuestion.Type.MATCHING, Pair.class);
            loadCategory("Games/memoryGame/level_" + level + "/" + subject, UnifiedQuestion.Type.MEMORY, null);
        } else if (level.equals("7_8")) {
            tasksToLoad = 3;
            loadCategory("Games/imageRecognition/level_" + level + "/" + subject, UnifiedQuestion.Type.IMAGE, Question.class);
            loadCategory("Games/sentenceCompletion/level_" + level + "/" + subject, UnifiedQuestion.Type.SENTENCE, SentenceQuestion.class);
            loadCategory("Games/memoryGame/level_" + level + "/" + subject, UnifiedQuestion.Type.MEMORY, null);
        }
    }

    /**
     * טוען קטגוריית שאלות אחת מ-Firebase.
     * @param path     הנתיב ב-Firebase
     * @param type     סוג השאלה: AUDIO / IMAGE / MATCHING / SENTENCE / MEMORY
     * @param modelClass המחלקה שאליה ממירים את הנתונים (Question / Pair / SentenceQuestion)
     */
    private void loadCategory(String path, UnifiedQuestion.Type type, Class<?> modelClass) {
        DatabaseService.getInstance().loadGameData(path, new DatabaseService.DatabaseCallback<DataSnapshot>() {
            @Override
            public void onCompleted(DataSnapshot snapshot) {
                try {
                    if (type == UnifiedQuestion.Type.MEMORY) {
                        // זיכרון: מחלק לקבוצות של 3 פריטים — כל קבוצה = שאלת זיכרון אחת
                        List<DataSnapshot> allItems = new ArrayList<>();
                        for (DataSnapshot item : snapshot.getChildren()) {
                            allItems.add(item);
                        }
                        int chunkSize = 3; // 3 זוגות בכל שאלת זיכרון
                        for (int i = 0; i < allItems.size(); i += chunkSize) {
                            int end = Math.min(i + chunkSize, allItems.size());
                            allQuestions.add(new UnifiedQuestion(type, new ArrayList<>(allItems.subList(i, end))));
                        }

                    } else if (type == UnifiedQuestion.Type.MATCHING) {
                        // התאמה: מחלק לקבוצות של 4 זוגות — כל קבוצה = שאלת התאמה אחת
                        List<Pair> pairs = new ArrayList<>();
                        for (DataSnapshot ds : snapshot.getChildren()) {
                            Pair p = ds.getValue(Pair.class);
                            if (p != null) pairs.add(p);
                        }
                        int chunkSize = 4; // 4 זוגות בכל שאלת התאמה
                        for (int i = 0; i < pairs.size(); i += chunkSize) {
                            int end = Math.min(i + chunkSize, pairs.size());
                            allQuestions.add(new UnifiedQuestion(type, new ArrayList<>(pairs.subList(i, end))));
                        }

                    } else {
                        // שאר הסוגים: כל פריט = שאלה אחת
                        for (DataSnapshot ds : snapshot.getChildren()) {
                            Object data = ds.getValue(modelClass);
                            if (data != null) allQuestions.add(new UnifiedQuestion(type, data));
                        }
                    }
                } catch (Exception e) {
                    Log.e(TAG, "Error loading type=" + type + ": " + e.getMessage());
                }
                // מודיע שקריאה זו הסתיימה
                checkIfLoadingFinished();
            }

            @Override
            public void onFailed(Exception e) {
                Log.e(TAG, "❌ שגיאת Firebase ב-" + type + ": " + e.getMessage());
                // גם בכשלון — מודיעים שהמשימה הסתיימה כדי לא לתקוע
                checkIfLoadingFinished();
            }
        });
    }

    /**
     * נקרא בסיום כל קריאת Firebase.
     * ממתין עד שכל 3 הקריאות חזרו, ואז מערבב ומתחיל את המשחק.
     */
    private void checkIfLoadingFinished() {
        tasksCompleted++;

        // ממשיך רק כשכל 3 הקריאות חזרו
        if (tasksCompleted >= tasksToLoad) {
            if (allQuestions.isEmpty()) {
                // אין שאלות — מציג הודעה וסוגר
                Log.w(TAG, "No questions loaded.");
                new AlertDialog.Builder(this)
                        .setTitle("הקוסם נח כרגע")
                        .setMessage("עוד אין שאלות בנושא הזה. בוא ננסה נושא אחר!")
                        .setPositiveButton("אוקיי", (d, w) -> finish())
                        .setCancelable(false)
                        .show();
                return;
            }

            // מערבב את השאלות כך שהסוגים יופיעו לסירוגין
            allQuestions = interleaveQuestions(allQuestions);

            // לוג לדיבאג — מציג את סדר השאלות
            Log.d(TAG, "📋 סה\"כ שאלות: " + allQuestions.size());
            for (int i = 0; i < allQuestions.size(); i++) {
                Log.d(TAG, "   [" + i + "] " + allQuestions.get(i).getType()
                        + " - data: " + (allQuestions.get(i).getData() == null ? "NULL!" : allQuestions.get(i).getData().getClass().getSimpleName()));
            }

            // בודק אם יש התקדמות שמורה מפעם קודמת
            checkSavedProgress();
        }
    }

    /**
     * מערבב את השאלות כך שסוגים שונים מופיעים לסירוגין.
     * לדוגמה: AUDIO → MATCHING → MEMORY → AUDIO → MATCHING...
     * @param original הרשימה המקורית לפני הערבוב
     * @return רשימה מעורבבת
     */
    private List<UnifiedQuestion> interleaveQuestions(List<UnifiedQuestion> original) {
        // מקבץ שאלות לפי סוג
        Map<UnifiedQuestion.Type, List<UnifiedQuestion>> grouped = new HashMap<>();

        for (UnifiedQuestion.Type type : UnifiedQuestion.Type.values()) {
            grouped.put(type, new ArrayList<>());
        }

        for (UnifiedQuestion q : original) {
            List<UnifiedQuestion> list = grouped.get(q.getType());
            if (list != null) list.add(q);
        }

        // מערבל כל קבוצת סוג בנפרד
        for (List<UnifiedQuestion> list : grouped.values()) {
            Collections.shuffle(list);
        }

        List<UnifiedQuestion> interleaved = new ArrayList<>();
        boolean added;

        // לוקח שאלה אחת מכל סוג בכל סיבוב — עד שנגמרות
        do {
            added = false;
            List<UnifiedQuestion.Type> types = new ArrayList<>(grouped.keySet());
            Collections.shuffle(types); // סדר הסוגים גם הוא אקראי

            for (UnifiedQuestion.Type type : types) {
                List<UnifiedQuestion> list = grouped.get(type);
                if (list != null && !list.isEmpty()) {
                    interleaved.add(list.remove(0));
                    added = true;
                }
            }
        } while (added);

        return interleaved;
    }

    /**
     * שומר את מיקום השאלה הנוכחית ב-SharedPreferences.
     * מאפשר המשך מאותו מקום בפעם הבאה.
     * לא שומר אם currentIndex == 0 (התחלה)
     */
    private void saveProgressLocally() {
        if (currentIndex == 0) return;
        getSharedPreferences("game_progress", MODE_PRIVATE)
                .edit()
                .putInt(subject + "_index", currentIndex)       // מיקום השאלה
                .putString(subject + "_child", currentChild.getId()) // מזהה הילד
                .apply();
    }

    /**
     * מוחק את ההתקדמות השמורה ב-SharedPreferences.
     * נקרא כשהילד בוחר "התחל מחדש" בדיאלוג ההמשך.
     */
    private void clearProgressLocally() {
        getSharedPreferences("game_progress", MODE_PRIVATE)
                .edit()
                .remove(subject + "_index")
                .remove(subject + "_child")
                .apply();
    }

    /**
     * בודק אם יש התקדמות שמורה לנושא הזה עבור הילד הזה.
     * אם כן — מציג דיאלוג המשך/התחלה מחדש.
     * אם לא — מתחיל מהתחלה.
     */
    private void checkSavedProgress() {
        SharedPreferences prefs = getSharedPreferences("game_progress", MODE_PRIVATE);
        int savedIdx = prefs.getInt(subject + "_index", 0);
        String savedChild = prefs.getString(subject + "_child", "");

        // בודק שה-index שמור, שייך לאותו ילד, ותקף (לא מעבר לגבול)
        if (savedIdx > 0 && currentChild.getId().equals(savedChild)
                && savedIdx < allQuestions.size()) {
            showContinueDialog(savedIdx);
        } else {
            showCurrentQuestion();
        }
    }

    /**
     * מציג דיאלוג שואל האם להמשיך מאיפה שעצרו או להתחיל מחדש.
     * @param savedIdx האינדקס השמור
     */
    private void showContinueDialog(int savedIdx) {
        final android.app.Dialog dialog = new android.app.Dialog(this);
        dialog.setContentView(R.layout.dialog_continue_progress);
        if (dialog.getWindow() != null)
            dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);

        // כפתור "המשך" — ממשיך מהאינדקס השמור
        dialog.findViewById(R.id.btnContinue).setOnClickListener(v -> {
            dialog.dismiss();
            currentIndex = savedIdx;
            showCurrentQuestion();
        });

        // כפתור "התחל מחדש" — מוחק שמירה ומתחיל מ-0
        dialog.findViewById(R.id.btnRestart).setOnClickListener(v -> {
            dialog.dismiss();
            clearProgressLocally();
            currentIndex = 0;
            showCurrentQuestion();
        });

        dialog.setCancelable(false); // לא ניתן לסגור בלחיצה מחוץ לדיאלוג
        dialog.show();
    }

    /**
     * הפונקציה המרכזית — מציגה את השאלה הנוכחית לפי סוגה.
     * נקראת בכל מעבר בין שאלות.
     */
    private void showCurrentQuestion() {
        isProcessingAnswer = false; // מאפשר לחיצות מחדש

        // אם עברנו את כל השאלות — סיום המשחק
        if (currentIndex >= allQuestions.size()) {
            finishGame();
            return;
        }

        hideAllLayouts();    // מסתיר את כל הקונטיינרים
        updateProgressBar(); // מעדכן את פס ההתקדמות

        UnifiedQuestion uq = allQuestions.get(currentIndex);

        // הגנה מפני נתונים פגומים
        if (uq == null || uq.getData() == null || uq.getType() == null) {
            Log.e(TAG, "❌ uq is null or invalid at index " + currentIndex);
            currentIndex++;
            showCurrentQuestion(); // מדלג לשאלה הבאה
            return;
        }

        Log.d(TAG, "Showing question " + currentIndex + " type: " + uq.getType());

        // מפנה לפונקציית ההצגה המתאימה לפי סוג השאלה
        try {
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
            // אם יש שגיאה בהצגה — מדלג לשאלה הבאה
            Log.e(TAG, "Error displaying question: " + e.getMessage());
            currentIndex++;
            showCurrentQuestion();
        }
    }

    /**
     * מעדכן את פס ההתקדמות ואת טקסט האחוז.
     * הנוסחה: מתחיל מ-15% (אחרי הכרטיסיות) ועולה עד 100%
     */
    private void updateProgressBar() {
        if (allQuestions.isEmpty()) return;
        int percent = 15 + (int) (((double) currentIndex / allQuestions.size()) * 85);
        globalProgress.setProgress(percent);
        if (tvProgressText != null) tvProgressText.setText(percent + "%");
    }

    /**
     * מציג שאלת שמע — מנגן את המילה ומציג 4 תמונות לבחירה.
     * @param q אובייקט השאלה עם הטקסט והאפשרויות
     */
    private void displayAudioQuestion(Question q) {
        Log.d(TAG, "🔊 הצגת שאלת שמע");

        containerSelection.setVisibility(View.VISIBLE);
        resetSelectionButtonColors(); // מאפס צבעי כפתורים לברירת מחדל
        btnPlayAudio.setVisibility(View.VISIBLE);   // מציג כפתור הקשבה
        imageCardContainer.setVisibility(View.GONE); // מסתיר תמונת שאלה

        tvQuestionTitle.setText("האזינו לשאלה 🎧");

        // מנגן את המילה אוטומטית בכניסה לשאלה
        playTTS(q.getQuestionText());

        // לחיצה על הכפתור — מנגן שוב
        btnPlayAudio.setOnClickListener(v -> playTTS(q.getQuestionText()));

        setupSelectionButtons(q); // בונה את 4 כפתורי התשובה
    }

    /**
     * מציג שאלת תמונה — מציג תמונה ו-4 תשובות טקסט לבחירה.
     * @param q אובייקט השאלה עם ה-URL של התמונה והאפשרויות
     */
    private void displayImageQuestion(Question q) {
        Log.d(TAG, "🖼️ הצגת שאלת תמונה");
        Log.d(TAG, "   - mediaUrl: " + q.getMediaUrl());

        containerSelection.setVisibility(View.VISIBLE);
        resetSelectionButtonColors();
        btnPlayAudio.setVisibility(View.GONE);          // מסתיר כפתור שמע
        imageCardContainer.setVisibility(View.VISIBLE); // מציג כרטיס תמונה
        ivQuestionImage.setVisibility(View.VISIBLE);

        tvQuestionTitle.setText(q.getQuestionText());

        // טעינת תמונת השאלה לפי שם הקובץ מה-drawable
        int resId = getResources().getIdentifier(q.getMediaUrl(), "drawable", getPackageName());
        Log.d(TAG, "   - resId: " + resId + (resId == 0 ? " ❌ לא נמצא!" : " ✅"));

        // אם התמונה לא נמצאה — מציג placeholder
        ivQuestionImage.setImageResource(resId != 0 ? resId : R.drawable.wizard_placeholder1);

        setupSelectionButtons(q);
    }

    /**
     * מציג שאלת השלמת משפט — תמונת רמז, משפט חסר, ומקלדת עברית.
     * @param sq אובייקט השאלה עם המשפט, התשובה, ותמונת הרמז
     */
    private void displaySentenceQuestion(SentenceQuestion sq) {
        containerSentence.setVisibility(View.VISIBLE);
        tvQuestionTitle.setText("השלימו את המשפט ✏️");

        // מציאת רכיבי ה-UI של שאלת המשפט
        ImageView ivHint = findViewById(R.id.ivMixedSentenceHint);
        TextView tvSentence = findViewById(R.id.tvMixedSentenceText);
        EditText etAnswer = findViewById(R.id.etMixedAnswer);
        GridView mixedKeyboard = findViewById(R.id.mixedKeyboard);
        mixedKeyboard.setNestedScrollingEnabled(false); // מונע גלילה בתוך גלילה
        Button btnCheck = findViewById(R.id.btnCheck);

        // טעינת תמונת הרמז
        int resId = getResources().getIdentifier(sq.getHintImage(), "drawable", getPackageName());
        ivHint.setImageResource(resId != 0 ? resId : R.drawable.wizard_placeholder1);

        // הצגת המשפט ואיפוס שדה הקלט
        tvSentence.setText(sq.getSentence());
        etAnswer.setText("");
        etAnswer.setTextColor(Color.parseColor("#1E5F8B"));

        // הגדרת המקלדת העברית — לחיצה על אות מוסיפה לשדה, DEL מוחקת
        KeyboardAdapter adapter = new KeyboardAdapter(this, hebrewLetters, letter -> {
            if (letter.equals("DEL")) {
                String str = etAnswer.getText().toString();
                if (!str.isEmpty()) etAnswer.setText(str.substring(0, str.length() - 1));
            } else {
                etAnswer.append(letter);
            }
        });
        mixedKeyboard.setAdapter(adapter);

        // לחיצת "בדוק" — משווה את הקלט לתשובה הנכונה
        btnCheck.setOnClickListener(v -> {
            if (isProcessingAnswer) return; // מונע לחיצה כפולה

            String userAns = etAnswer.getText().toString().trim();

            if (userAns.equalsIgnoreCase(sq.getCorrectAnswer())) {
                // תשובה נכונה — צובע ירוק וממשיך
                isProcessingAnswer = true;
                shakeAndColorAnswer(etAnswer, true);
                new Handler(Looper.getMainLooper()).postDelayed(() -> handleCorrect(), 800);
            } else {
                // תשובה שגויה — רעד ואיפוס
                shakeAndColorAnswer(etAnswer, false);
            }
        });
    }

    /**
     * מציג פידבק ויזואלי על שדה הקלט — ירוק לנכון, אדום + רעד לשגוי.
     * @param etAnswer שדה הקלט
     * @param isCorrect האם התשובה נכונה
     */
    private void shakeAndColorAnswer(EditText etAnswer, boolean isCorrect) {
        // צביעת הטקסט — ירוק/אדום
        int color = isCorrect ? Color.parseColor("#4CAF50") : Color.parseColor("#F44336");
        etAnswer.setTextColor(color);

        if (!isCorrect) {
            // אנימציית רעד — 5 תנועות ימין-שמאל
            etAnswer.animate()
                    .translationX(16).setDuration(50).withEndAction(() ->
                            etAnswer.animate()
                                    .translationX(-16).setDuration(50).withEndAction(() ->
                                            etAnswer.animate()
                                                    .translationX(10).setDuration(50).withEndAction(() ->
                                                            etAnswer.animate()
                                                                    .translationX(-10).setDuration(50).withEndAction(() ->
                                                                            etAnswer.animate()
                                                                                    .translationX(0).setDuration(50).start()
                                                                    ).start()).start()).start()).start();

            // אחרי 800ms — מאפס צבע וטקסט לניסיון הבא
            etAnswer.postDelayed(() -> {
                etAnswer.setTextColor(Color.parseColor("#1E5F8B"));
                etAnswer.setText("");
            }, 800);
        } else {
            // נכון — מחזיר לצבע כחול אחרי 600ms
            etAnswer.postDelayed(() ->
                    etAnswer.setTextColor(Color.parseColor("#1E5F8B")), 600);
        }
    }

    /**
     * מציג משחק זיכרון — בונה זוגות כרטיסים ומאזין ללחיצות.
     * @param items רשימת DataSnapshot — כל פריט הוא זוג כרטיסים
     */
    private void displayMemoryGame(List<DataSnapshot> items) {
        containerMemory.setVisibility(View.VISIBLE);
        tvQuestionTitle.setText("מצאו את הזוגות 🎴");
        memoryCards.clear(); // מנקה כרטיסים מסיבוב קודם

        // בניית זוגות כרטיסים מהנתונים של Firebase
        for (DataSnapshot ds : items) {
            String imageName = ds.child("image").getValue(String.class);
            String displayName = ds.child("name").getValue(String.class);

            if (imageName != null && displayName != null) {
                int resId = getResources().getIdentifier(imageName, "drawable", getPackageName());
                addPairToMemoryList(resId, displayName); // מוסיף זוג לרשימה
            }
        }

        Collections.shuffle(memoryCards); // מערבל את מיקומי הכרטיסים

        // מגדיר את האדפטר ומחבר ל-GridView
        memoryAdapter = new MemoryAdapter(this, memoryCards, cardSize);
        gvMemoryBoard.setAdapter(memoryAdapter);

        // מאזין ללחיצות על כרטיסים
        gvMemoryBoard.setOnItemClickListener((parent, view, position, id) -> {
            if (isMemoryBusy) return; // חוסם לחיצות בזמן בדיקת זוג

            MemoryCard selected = memoryCards.get(position);

            // מתעלם מכרטיס שכבר הותאם או פתוח
            if (selected.isMatched() || selected.isFlipped()) return;

            // הופך את הכרטיס ומרענן תצוגה
            selected.setFlipped(true);
            memoryAdapter.notifyDataSetChanged();

            if (firstMemorySelected == null) {
                firstMemorySelected = selected; // לחיצה ראשונה — שומר
            } else {
                secondMemorySelected = selected; // לחיצה שנייה — בודק
                checkMemoryMatch();
            }
        });
    }

    /**
     * מוסיף זוג כרטיסי זיכרון לרשימה.
     * לרמות 3-4 ו-5-6: זוג תמונות זהות.
     * לרמה 7-8: תמונה אחת + כרטיס טקסט.
     * @param resId מזהה התמונה
     * @param name שם הפריט — גם משמש כ-matchId לזיהוי זוג
     */
    private void addPairToMemoryList(int resId, String name) {
        String ageGroup = currentChild.getAgeGroup();

        if (ageGroup.equals("3-4") || ageGroup.equals("5-6")) {
            // שתי תמונות זהות — הילד מוצא זוג תמונות
            memoryCards.add(new MemoryCard(resId, null, name));
            memoryCards.add(new MemoryCard(resId, null, name));
        } else {
            // תמונה + טקסט — הילד מתאים תמונה למילה
            memoryCards.add(new MemoryCard(resId, null, name)); // כרטיס תמונה
            memoryCards.add(new MemoryCard(0, name, name));     // כרטיס טקסט
        }
    }

    /**
     * בודק האם שני הכרטיסים שנבחרו הם זוג נכון.
     * ממתין שנייה לפני הבדיקה כדי שהילד יראה את שני הכרטיסים.
     */
    private void checkMemoryMatch() {
        isMemoryBusy = true; // חוסם לחיצות נוספות בזמן הבדיקה

        new Handler(Looper.getMainLooper()).postDelayed(() -> {
            // השוואת matchId של שני הכרטיסים — אותו שם = זוג נכון
            if (firstMemorySelected.getMatchId().equals(secondMemorySelected.getMatchId())) {
                // זוג נכון — מסמן כהותאם
                firstMemorySelected.setMatched(true);
                secondMemorySelected.setMatched(true);
                firstMemorySelected.setFlipped(false);
                secondMemorySelected.setFlipped(false);

                firstMemorySelected = null;
                secondMemorySelected = null;
                memoryAdapter.notifyDataSetChanged();
                isMemoryBusy = false;

                // בודק אם כל הזוגות הותאמו
                if (checkAllMemoryMatched()) handleCorrect();
            } else {
                // זוג שגוי — סוגר את שני הכרטיסים
                firstMemorySelected.setFlipped(false);
                secondMemorySelected.setFlipped(false);
                firstMemorySelected = null;
                secondMemorySelected = null;
                memoryAdapter.notifyDataSetChanged();
                isMemoryBusy = false;
            }
        }, 1000); // מחכה שנייה שהילד יראה את שני הכרטיסים
    }

    /**
     * בודק האם כל כרטיסי הזיכרון הותאמו.
     * @return true אם כולם הותאמו, false אחרת
     */
    private boolean checkAllMemoryMatched() {
        for (MemoryCard c : memoryCards) {
            if (!c.isMatched()) return false; // נמצא כרטיס שעוד לא הותאם
        }
        return true; // כולם הותאמו
    }

    /**
     * מציג שאלת התאמה — שתי עמודות של כרטיסים לגרירה ושחרור.
     * @param pairs רשימת זוגות — כל זוג יש לו left, right, ו-id
     */
    private void displayMatchingQuestion(List<Pair> pairs) {
        containerMatching.setVisibility(View.VISIBLE);
        tvQuestionTitle.setText("התאימו את הזוגות 🔗");

        // מנקה עמודות מסיבוב קודם
        leftColumn.removeAllViews();
        rightColumn.removeAllViews();

        matchesFound = 0;
        totalPairs = pairs.size();

        // מערבל כל עמודה בנפרד — הזוג הנכון לא יהיה מול מקבילו
        List<Pair> leftSide = new ArrayList<>(pairs);
        List<Pair> rightSide = new ArrayList<>(pairs);
        Collections.shuffle(leftSide);
        Collections.shuffle(rightSide);

        // בונה את הכרטיסים בכל עמודה
        for (Pair p : leftSide) setupMatchingView(p.getLeft(), p.getId(), leftColumn);
        for (Pair p : rightSide) setupMatchingView(p.getRight(), p.getId(), rightColumn);
    }

    /**
     * בונה כרטיס אחד במשחק ההתאמה ומגדיר את אירועי הגרירה.
     * @param content התוכן להצגה: טקסט / שם תמונה / "audio"
     * @param id מזהה הזוג — נשמר כ-Tag על הכרטיס
     * @param column העמודה שאליה מוסיפים את הכרטיס
     */
    private void setupMatchingView(String content, String id, LinearLayout column) {
        // יצירת כרטיס מה-layout
        com.google.android.material.card.MaterialCardView card =
                (com.google.android.material.card.MaterialCardView) getLayoutInflater()
                        .inflate(R.layout.item_matching_button, column, false);

        // שמירת מזהה הזוג על הכרטיס — הסוד לבדיקת התאמה!
        card.setTag(id);

        TextView answerText = card.findViewById(R.id.answerText);
        ImageView answerImage = card.findViewById(R.id.answerImage);

        // הגדרת תוכן הכרטיס לפי סוגו
        if (content != null) {
            if (content.startsWith("ss_")) {
                // תמונה — שם מתחיל ב-ss_
                int resId = getResources().getIdentifier(content, "drawable", getPackageName());
                answerImage.setImageResource(resId != 0 ? resId : R.drawable.wizard_placeholder1);
                answerImage.setVisibility(View.VISIBLE);
                answerText.setVisibility(View.GONE);
            } else if (content.equalsIgnoreCase("audio") || content.equalsIgnoreCase("speaker")) {
                // כפתור שמע — לחיצה מנגנת את המילה
                answerText.setText("🔊");
                answerText.setTextSize(32);
                answerImage.setVisibility(View.GONE);
                card.setOnClickListener(v -> playTTS(id)); // מנגן את ה-id כטקסט
            } else {
                // טקסט רגיל
                answerText.setText(content);
                answerText.setVisibility(View.VISIBLE);
                answerImage.setVisibility(View.GONE);
            }
        }

        // לחיצה ארוכה — מתחילה גרירה
        card.setOnLongClickListener(v -> {
            // כרטיס שהותאם לא נגרר
            if (Boolean.TRUE.equals(v.getTag(R.id.tag_matched))) {
                return false;
            }
            // יצירת "צל" שנע עם האצבע
            View.DragShadowBuilder shadowBuilder = new View.DragShadowBuilder(v);
            v.startDragAndDrop(null, shadowBuilder, v, 0);
            // מסתיר את הכרטיס המקורי בזמן הגרירה
            v.setVisibility(View.INVISIBLE);
            return true;
        });

        // מאזין לאירועי גרירה — כל כרטיס מאזין לכרטיסים שנגררים עליו
        card.setOnDragListener((v, event) -> {
            switch (event.getAction()) {

                case DragEvent.ACTION_DRAG_STARTED:
                    // חייב להחזיר true כדי להמשיך לקבל אירועים
                    return true;

                case DragEvent.ACTION_DRAG_ENTERED:
                    // האצבע נכנסה מעל כרטיס זה — צבע ורדרד כרמז
                    if (!Boolean.TRUE.equals(v.getTag(R.id.tag_matched))) {
                        ((com.google.android.material.card.MaterialCardView) v)
                                .setCardBackgroundColor(Color.parseColor("#ffe4e1"));
                    }
                    return true;

                case DragEvent.ACTION_DRAG_EXITED:
                    // האצבע יצאה — חוזר ללבן
                    if (!Boolean.TRUE.equals(v.getTag(R.id.tag_matched))) {
                        ((com.google.android.material.card.MaterialCardView) v)
                                .setCardBackgroundColor(Color.WHITE);
                    }
                    return true;

                case DragEvent.ACTION_DROP:
                    // שחרור — בדיקת התאמה
                    View draggedView = (View) event.getLocalState(); // הכרטיס שנגרר

                    if (draggedView != null
                            && draggedView.getParent() != v.getParent() // עמודות שונות
                            && draggedView.getTag().equals(v.getTag())  // אותו id = זוג נכון!
                            && !Boolean.TRUE.equals(v.getTag(R.id.tag_matched))) { // לא הותאם כבר
                        // זוג נכון!
                        markCardAsMatched(v);
                        markCardAsMatched(draggedView);
                        matchesFound++;
                        if (matchesFound == totalPairs) handleCorrect(); // כל הזוגות!
                    } else {
                        // זוג שגוי — רעד ומחזיר את הכרטיס
                        if (!Boolean.TRUE.equals(v.getTag(R.id.tag_matched))) handleWrong(v);
                        if (draggedView != null) draggedView.setVisibility(View.VISIBLE);
                    }
                    return true;

                case DragEvent.ACTION_DRAG_ENDED:
                    // הגרירה נגמרה — אם לא נחת על כרטיס, מחזיר לנראות
                    if (!event.getResult()) card.setVisibility(View.VISIBLE);
                    return true;
            }
            return false;
        });

        // לחיצה רגילה — מציג הבהוב כחול
        card.setOnClickListener(v -> {
            if (Boolean.TRUE.equals(v.getTag(R.id.tag_matched))) return;
            ((com.google.android.material.card.MaterialCardView) v)
                    .setCardBackgroundColor(Color.parseColor("#B3E5FC"));
            new Handler(Looper.getMainLooper()).postDelayed(() ->
                    ((com.google.android.material.card.MaterialCardView) v)
                            .setCardBackgroundColor(Color.WHITE), 300);
        });

        column.addView(card); // מוסיף לעמודה
    }

    /**
     * מסמן כרטיס כ"הותאם" — צובע ירוק, מנטרל לחיצה וגרירה, ומציג אנימציה.
     * @param view הכרטיס שהותאם
     */
    private void markCardAsMatched(View view) {
        view.setTag(R.id.tag_matched, true); // מסמן כהותאם — מונע גרירה/בדיקה חוזרת
        view.setVisibility(View.VISIBLE);
        view.setAlpha(1f);
        view.setEnabled(false);      // לא ניתן ללחיצה
        view.setClickable(false);
        view.setLongClickable(false); // לא ניתן לגרירה

        // צביעה ירוקה בהירה
        if (view instanceof com.google.android.material.card.MaterialCardView) {
            ((com.google.android.material.card.MaterialCardView) view)
                    .setCardBackgroundColor(Color.parseColor("#c8f5c8"));
        } else {
            view.setBackgroundTintList(ColorStateList.valueOf(Color.parseColor("#c8f5c8")));
        }

        // אנימציית "פעימה" — מתכווץ ומתרחב
        view.animate().scaleX(0.95f).scaleY(0.95f).setDuration(200)
                .withEndAction(() -> view.animate().scaleX(1f).scaleY(1f).setDuration(200).start()).start();
    }

    /**
     * בונה את 4 כפתורי התשובה לשאלות AUDIO ו-IMAGE.
     * אם האפשרות היא תמונה — מציג ImageView, אחרת TextView.
     * @param q אובייקט השאלה עם האפשרויות ואינדקס התשובה הנכונה
     */
    private void setupSelectionButtons(Question q) {
        List<String> options = q.getOptions();
        if (options == null || options.size() < 4) return;

        for (int i = 0; i < 4; i++) {
            final int index = i;
            String item = options.get(i);

            // ניסיון לטעון כתמונה מה-drawable
            int resId = getResources().getIdentifier(item, "drawable", getPackageName());

            if (resId != 0) {
                // יש תמונה — מציג ImageView
                selectionButtons[i].setImageResource(resId);
                selectionButtons[i].setVisibility(View.VISIBLE);
                selectionTextViews[i].setVisibility(View.GONE);
            } else {
                // אין תמונה — מציג טקסט
                selectionButtons[i].setVisibility(View.GONE);
                selectionTextViews[i].setText(item);
                selectionTextViews[i].setVisibility(View.VISIBLE);
            }

            // מאזין לחיצה — זהה לתמונה ולטקסט
            View.OnClickListener listener = v -> {
                if (isProcessingAnswer) return; // מונע לחיצה כפולה
                isProcessingAnswer = true;

                if (index == q.getCorrectAnswerIndex()) {
                    // תשובה נכונה
                    shakeAndColorCard(index, true);
                    new Handler(Looper.getMainLooper()).postDelayed(() -> handleCorrect(), 600);
                } else {
                    // תשובה שגויה — מאפשר ניסיון נוסף
                    isProcessingAnswer = false;
                    shakeAndColorCard(index, false);
                }
            };

            selectionButtons[i].setOnClickListener(listener);
            selectionTextViews[i].setOnClickListener(listener);
        }
    }

    /**
     * מציג פידבק ויזואלי על כרטיס תשובה — ירוק לנכון, אדום + רעד לשגוי.
     * @param index אינדקס הכרטיס (0-3)
     * @param isCorrect האם התשובה נכונה
     */
    private void shakeAndColorCard(int index, boolean isCorrect) {
        int[] cardIds = {R.id.cardAns1, R.id.cardAns2, R.id.cardAns3, R.id.cardAns4};
        View card = findViewById(cardIds[index]);
        if (!(card instanceof com.google.android.material.card.MaterialCardView)) return;

        com.google.android.material.card.MaterialCardView cardView =
                (com.google.android.material.card.MaterialCardView) card;

        // צבע ירוק לנכון, אדום לשגוי
        int feedbackColor = isCorrect ? Color.parseColor("#00ff00") : Color.parseColor("#F44336");
        cardView.setCardBackgroundColor(feedbackColor);

        if (!isCorrect) {
            // אנימציית רעד — 5 תנועות ימין-שמאל
            card.animate()
                    .translationX(16).setDuration(50).withEndAction(() ->
                            card.animate()
                                    .translationX(-16).setDuration(50).withEndAction(() ->
                                            card.animate()
                                                    .translationX(10).setDuration(50).withEndAction(() ->
                                                            card.animate()
                                                                    .translationX(-10).setDuration(50).withEndAction(() ->
                                                                            card.animate()
                                                                                    .translationX(0).setDuration(50).start()
                                                                    ).start()).start()).start()).start();
        }

        // החזרת הצבע המקורי של הכרטיס
        card.postDelayed(() ->
                        cardView.setCardBackgroundColor(Color.parseColor(buttonColors[index])),
                isCorrect ? 600 : 800 // נכון — מהר יותר
        );
    }

    /**
     * נקרא אחרי כל תשובה נכונה.
     * מקדם את האינדקס, שומר התקדמות, ומציג שאלה הבאה.
     */
    private void handleCorrect() {
        currentIndex++;              // מתקדם לשאלה הבאה
        saveProgressLocally();       // שומר ב-SharedPreferences מיד
        updateProgressInFirebase();  // שולח סטטיסטיקות ל-Firebase
        // מחכה שנייה לפני הצגת השאלה הבאה
        new Handler(Looper.getMainLooper()).postDelayed(this::showCurrentQuestion, 1000);
    }

    /**
     * נקרא אחרי תשובה שגויה.
     * מוסיף לספירת הטעויות ומציג רעד + אדום על הכרטיס.
     * @param view הכרטיס שנלחץ בטעות
     */
    private void handleWrong(View view) {
        attempts++; // מוסיף לספירת הטעויות — ישפיע על הציון

        view.setBackgroundTintList(ColorStateList.valueOf(Color.RED));

        // אנימציית רעד
        view.animate().translationX(20).setDuration(50).withEndAction(() ->
                view.animate().translationX(-20).setDuration(50).withEndAction(() ->
                        view.animate().translationX(0).setDuration(50).start()
                ).start()
        ).start();

        // מחזיר לצבע לבן אחרי 800ms
        view.postDelayed(() -> {
            if (view instanceof EditText) {
                view.setBackgroundTintList(null);
            } else {
                view.setBackgroundTintList(ColorStateList.valueOf(Color.WHITE));
            }
        }, 800);
    }

    /**
     * שולח את נתוני ההתקדמות ל-Firebase.
     * מחשב אחוז, זמן, וטעויות — ושולח לעדכון.
     * נקרא אחרי כל תשובה נכונה, ביציאה, ובסיום המשחק.
     */
    private void updateProgressInFirebase() {
        if (allQuestions.isEmpty() || currentIndex == 0) return;
        if (SharedPreferencesUtil.getUser(this) == null) return;

        // חישוב אחוז ההתקדמות: מתחיל מ-15% (אחרי כרטיסיות) עד 100%
        int percent = 15 + (int) (((double) currentIndex / allQuestions.size()) * 85);
        String parentId = SharedPreferencesUtil.getUser(this).getId();

        // חישוב זמן בשניות מאז תחילת המשחק (או מאז העדכון האחרון)
        long timeSpent = (System.currentTimeMillis() - gameStartTime) / 1000;

        DatabaseService.getInstance().updateGameProgress(
                parentId,
                currentChild.getId(),
                currentChild.getAgeGroup(),
                subject,
                currentIndex,
                percent,
                timeSpent,
                attempts
        );

        updateProgressBar();
        gameStartTime = System.currentTimeMillis(); // מאפס שעון לחישוב הבא
        attempts = 0; // מאפס טעויות לחישוב הבא
    }

    /**
     * מאפס את צבעי 4 כפתורי התשובה לצבעים המקוריים שלהם.
     * נקרא בתחילת כל שאלת AUDIO/IMAGE חדשה.
     */
    private void resetSelectionButtonColors() {
        int[] cardIds = {R.id.cardAns1, R.id.cardAns2, R.id.cardAns3, R.id.cardAns4};
        for (int i = 0; i < 4; i++) {
            View card = findViewById(cardIds[i]);
            if (card instanceof com.google.android.material.card.MaterialCardView) {
                ((com.google.android.material.card.MaterialCardView) card)
                        .setCardBackgroundColor(Color.parseColor(buttonColors[i]));
            }
        }
    }

    /**
     * מסתיר את כל קונטיינרי המשחק.
     * נקרא לפני הצגת כל שאלה חדשה כדי לנקות את המסך.
     */
    private void hideAllLayouts() {
        containerSelection.setVisibility(View.GONE);
        containerMatching.setVisibility(View.GONE);
        containerSentence.setVisibility(View.GONE);
        containerMemory.setVisibility(View.GONE);
    }

    /**
     * מנגן טקסט בקול עברית דרך מנוע ה-TTS.
     * QUEUE_FLUSH — מבטל קריאה קודמת ומתחיל חדשה מיד.
     * @param text הטקסט לקריאה
     */
    private void playTTS(String text) {
        if (tts != null) {
            tts.speak(text, TextToSpeech.QUEUE_FLUSH, null, null);
        }
    }

    /**
     * מסיים את המשחק — מחשב ציון ומציג דיאלוג מתאים.
     * ציון >= 80: הצלחה + שמירת "הושלם" ב-Firebase
     * ציון < 80: הצעה לנסות שוב
     */
    private void finishGame() {
        updateProgressInFirebase(); // עדכון אחרון לפני סיום

        // נוסחת ציון: ככל שיש יותר טעויות — הציון יורד
        double score = ((double) allQuestions.size() / (allQuestions.size() + attempts)) * 100;

        if (score >= 80) {
            markSubjectAsCompletedInFirebase(); // שומר completed=true
            showSuccessDialog(score);
        } else {
            showTryAgainDialog(score);
        }
    }

    /**
     * מציג דיאלוג יציאה עם אפשרות לשמור התקדמות.
     * נקרא מכפתור היציאה.
     */
    private void showExitDialog() {
        showCustomDialog(
                "לצאת מהמשחק?",
                "אל דאגה! ההתקדמות שלך נשמרת ותוכל להמשיך בפעם הבאה 🌟",
                "יציאה",
                Color.parseColor("#FF9800"),
                () -> {
                    updateProgressInFirebase(); // שומר לפני יציאה
                    finish();
                }
        );
    }

    /**
     * מציג דיאלוג "כמעט הצלחת" עם אפשרות לנסות שוב.
     * @param score הציון שהתקבל (0-100)
     */
    private void showTryAgainDialog(double score) {
        final android.app.Dialog dialog = new android.app.Dialog(this);
        dialog.setContentView(R.layout.dialog_test_result);

        if (dialog.getWindow() != null)
            dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);

        TextView tvTitle = dialog.findViewById(R.id.tvDialogTitle);
        TextView tvMessage = dialog.findViewById(R.id.tvDialogMessage);
        ImageView ivStatus = dialog.findViewById(R.id.ivStatusIcon);
        Button btnAction = dialog.findViewById(R.id.btnDialogAction);
        Button btnStay = dialog.findViewById(R.id.btnStayAtCurrentLevel);
        RatingBar ratingBar = dialog.findViewById(R.id.dialogRatingBar);

        if (tvTitle != null) tvTitle.setText("כמעט הצלחת!");
        if (tvMessage != null) tvMessage.setText("קיבלת " + (int) score + " נקודות.\nכדי לקבל את ה-V צריך לפחות 80.");
        if (ivStatus != null) ivStatus.setImageResource(R.drawable.ic_medal);
        if (ratingBar != null) ratingBar.setRating((float) (score / 20)); // 5 כוכבים = 100 נקודות

        if (btnAction != null) {
            btnAction.setText("אני רוצה לנסות שוב!");
            btnAction.setOnClickListener(v -> {
                dialog.dismiss();
                // איפוס מלא ותחילה מחדש
                currentIndex = 0;
                attempts = 0;
                gameStartTime = System.currentTimeMillis();
                Collections.shuffle(allQuestions); // סדר שאלות שונה
                showCurrentQuestion();
            });
        }

        if (btnStay != null) {
            btnStay.setVisibility(View.VISIBLE);
            btnStay.setText("אולי אחר כך");
            btnStay.setOnClickListener(v -> {
                dialog.dismiss();
                finish();
            });
        }

        dialog.setCancelable(false);
        dialog.show();
    }

    /**
     * מציג דיאלוג הצלחה — "וואו! אלוף!"
     * @param score הציון שהתקבל (>= 80)
     */
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
        if (tvMessage != null) tvMessage.setText("סיימת את התרגול בהצלחה!\nעכשיו מופיע לך V בתפריט.");
        if (ivStatus != null) ivStatus.setImageResource(R.drawable.ic_trophy);
        if (ratingBar != null) ratingBar.setRating((float) (score / 20));

        if (btnStay != null) btnStay.setVisibility(View.GONE); // אין כפתור "אחר כך" בהצלחה

        if (btnAction != null) {
            btnAction.setText("חזור לתפריט");
            btnAction.setOnClickListener(v -> { dialog.dismiss(); finish(); });
        }

        dialog.setCancelable(false);
        dialog.show();
    }

    /**
     * שומר ב-Firebase שהנושא הושלם (completed = true).
     * נקרא רק כשהציון >= 80.
     */
    private void markSubjectAsCompletedInFirebase() {
        if (SharedPreferencesUtil.getUser(this) == null) return;
        String parentId = SharedPreferencesUtil.getUser(this).getId();

        DatabaseService.getInstance().markSubjectAsCompleted(
                parentId,
                currentChild.getId(),
                currentChild.getAgeGroup(),
                subject,
                currentIndex,
                null
        );
    }

    /**
     * נקרא כשהמסך נסגר.
     * חובה לעצור ולסגור את ה-TTS כדי לא לגרום לדליפת זיכרון.
     */
    @Override
    protected void onDestroy() {
        if (tts != null) {
            tts.stop();     // עוצר קריאה פעילה
            tts.shutdown(); // משחרר משאבים
        }
        super.onDestroy();
    }
}