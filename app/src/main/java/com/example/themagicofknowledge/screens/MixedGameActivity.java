package com.example.themagicofknowledge.screens;

import android.content.res.ColorStateList;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.speech.tts.TextToSpeech;
import android.util.DisplayMetrics;
import android.view.DragEvent;
import android.view.View;
import android.widget.*;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import com.example.themagicofknowledge.R;
import com.example.themagicofknowledge.adapter.MemoryAdapter;
import com.example.themagicofknowledge.models.*;
import com.example.themagicofknowledge.services.DatabaseService;
import com.example.themagicofknowledge.utils.SharedPreferencesUtil;
import com.google.android.material.button.MaterialButton;
import com.google.firebase.database.*;
import java.util.*;

public class MixedGameActivity extends AppCompatActivity {

    private List<UnifiedQuestion> allQuestions = new ArrayList<>();
    private int currentIndex = 0;
    private int attempts = 0;
    private int cardSize;
    private long startTime;
    private String subject;
    private boolean isProcessingAnswer = false;
    private UserChild currentChild;
    private TextToSpeech tts;

    // רכיבי UI כלליים
    private ProgressBar globalProgress;
    private TextView tvQuestionTitle;

    // מכולות (Containers)
    private View containerSelection, containerMatching, containerSentence;

    // רכיבי משחקי בחירה (שמע/תמונה)
    private MaterialButton btnPlayAudio;
    private ImageView ivQuestionImage;
    private MaterialButton[] selectionButtons = new MaterialButton[4];

    // רכיבי משחק ההתאמה
    private LinearLayout leftColumn, rightColumn;
    private View selectedView = null;
    private int matchesFound = 0;
    private int totalPairs;

    // משתני טעינה
    private int tasksToLoad = 5;
    private int tasksCompleted = 0;

    // רכיבי זיכרון
    private View containerMemory;
    private GridView gvMemoryBoard;
    private List<MemoryCard> memoryCards = new ArrayList<>();
    private MemoryAdapter memoryAdapter;
    private MemoryCard firstMemorySelected = null;
    private MemoryCard secondMemorySelected = null;
    private boolean isMemoryBusy = false;

    private final String[] hebrewLetters = {
            "ו", "ה", "ד", "ג", "ב", "א", "DEL",
            "מ", "ל", "כ", "י", "ט", "ח", "ז",
            "ר", "ק", "צ", "פ", "ע", "ס", "נ",
            "ץ", "ף", "ן", "ם", "ך", "ת", "ש",
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_mixed_game);

        subject = getIntent().getStringExtra("subject");
        currentChild = SharedPreferencesUtil.getCurrentChild(this);
        if (currentChild == null) { finish(); return; }

        initViews();
        setupTTS();

        startTime = System.currentTimeMillis();
        loadAllData();
    }

    private void initViews() {
        globalProgress = findViewById(R.id.globalProgress);
        tvQuestionTitle = findViewById(R.id.tvMixedQuestionTitle);

        containerSelection = findViewById(R.id.containerSelection);
        containerMatching = findViewById(R.id.containerMatching);
        containerSentence = findViewById(R.id.containerSentence);

        btnPlayAudio = findViewById(R.id.btnMixedPlayAudio);
        ivQuestionImage = findViewById(R.id.ivMixedImage);

        selectionButtons[0] = findViewById(R.id.btnAns1);
        selectionButtons[1] = findViewById(R.id.btnAns2);
        selectionButtons[2] = findViewById(R.id.btnAns3);
        selectionButtons[3] = findViewById(R.id.btnAns4);

        leftColumn = findViewById(R.id.mixedLeftColumn);
        rightColumn = findViewById(R.id.mixedRightColumn);

        containerMemory = findViewById(R.id.containerMemory);
        gvMemoryBoard = findViewById(R.id.gvMemoryBoard);

        DisplayMetrics metrics = getResources().getDisplayMetrics();
        int screenWidth = metrics.widthPixels - (int)(24 * metrics.density);
        cardSize = (screenWidth - (int)(24 * metrics.density)) / 3;
    }

    private void setupTTS() {
        tts = new TextToSpeech(this, status -> {
            if (status == TextToSpeech.SUCCESS) tts.setLanguage(new Locale("he"));
        });
    }

    // --- שלב 1: טעינת נתונים מכל המקורות ---
    private void loadAllData() {
        String level = currentChild.getAgeGroup().replace("-", "_");
        DatabaseReference rootRef = FirebaseDatabase.getInstance().getReference("Games");

        loadCategory(rootRef.child("audioRecognition").child("level_" + level).child(subject), UnifiedQuestion.Type.AUDIO, Question.class);
        loadCategory(rootRef.child("imageRecognition").child("level_" + level).child(subject), UnifiedQuestion.Type.IMAGE, Question.class);
        loadCategory(rootRef.child("matching").child("level_" + level).child(subject), UnifiedQuestion.Type.MATCHING, Pair.class);
        loadCategory(rootRef.child("sentenceCompletion").child("level_" + level).child(subject), UnifiedQuestion.Type.SENTENCE, SentenceQuestion.class);
        loadCategory(rootRef.child("memoryGame").child("level_" + level).child(subject), UnifiedQuestion.Type.MEMORY, DataSnapshot.class);
    }


    private void loadCategory(Query query, UnifiedQuestion.Type type, Class<?> modelClass) {
        query.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (type == UnifiedQuestion.Type.MEMORY) {
                    // לוגיקה לפיצול משחק הזיכרון ליחידות קטנות
                    List<DataSnapshot> allItems = new ArrayList<>();
                    for (DataSnapshot item : snapshot.getChildren()) {
                        allItems.add(item);
                    }

                    int chunkSize = 3; // כמה זוגות בכל "שלב" של משחק זיכרון
                    for (int i = 0; i < allItems.size(); i += chunkSize) {
                        int end = Math.min(i + chunkSize, allItems.size());
                        List<DataSnapshot> subList = allItems.subList(i, end);

                        // מוסיף כל שלב כ"שאלה" נפרדת ברשימה המעורבבת
                        allQuestions.add(new UnifiedQuestion(type, subList));
                    }
                } else {
                    // שאר המשחקים נטענים כרגיל
                    for (DataSnapshot ds : snapshot.getChildren()) {
                        Object data = ds.getValue(modelClass);
                        if (data != null) allQuestions.add(new UnifiedQuestion(type, data));
                    }
                }
                checkIfLoadingFinished();
            }
            @Override
            public void onCancelled(@NonNull DatabaseError error) { checkIfLoadingFinished(); }
        });
    }

    private void checkIfLoadingFinished() {
        tasksCompleted++;
        if (tasksCompleted == tasksToLoad) {
            if (allQuestions.isEmpty()) {
                new AlertDialog.Builder(this)
                        .setTitle("הקוסם נח כרגע")
                        .setMessage("עוד אין שאלות בנושא הזה. בוא ננסה נושא אחר!")
                        .setPositiveButton("אוקיי", (d, w) -> finish())
                        .setCancelable(false)
                        .show();
                return;
            }
            Collections.shuffle(allQuestions);
            checkSavedProgress();
        }
    }

    // --- שלב 2: בדיקת המשכיות ---
    private void checkSavedProgress() {
        String pId = SharedPreferencesUtil.getUser(this).getId();
        DatabaseReference ref = FirebaseDatabase.getInstance().getReference("users")
                .child(pId).child("childrenList").child(currentChild.getId())
                .child("progress").child(currentChild.getAgeGroup()).child(subject);

        ref.child("lastQuestionIndex").addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (snapshot.exists()) {
                    int savedIdx = snapshot.getValue(Integer.class);
                    if (savedIdx > 0 && savedIdx < allQuestions.size()) {
                        showContinueDialog(savedIdx);
                        return;
                    }
                }
                showCurrentQuestion();
            }
            @Override
            public void onCancelled(@NonNull DatabaseError error) {}
        });
    }

    private void showContinueDialog(int savedIdx) {
        new AlertDialog.Builder(this)
                .setTitle("להמשיך מאיפה שעצרנו?")
                .setMessage("נראה שכבר התחלת לתרגל נושא זה.")
                .setPositiveButton("כן", (d, w) -> { currentIndex = savedIdx; showCurrentQuestion(); })
                .setNegativeButton("מהתחלה", (d, w) -> { currentIndex = 0; showCurrentQuestion(); })
                .setCancelable(false).show();
    }

    // --- שלב 3: מנוע הצגת השאלות ---
    private void showCurrentQuestion() {
        isProcessingAnswer = false;
        if (currentIndex >= allQuestions.size()) {
            finishGame();
            return;
        }

        hideAllLayouts();
        updateProgressInFirebase();

        UnifiedQuestion uq = allQuestions.get(currentIndex);

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
                displayMemoryGame((DataSnapshot) uq.getData());
                break;
        }
    }

    private void displayAudioQuestion(Question q) {
        containerSelection.setVisibility(View.VISIBLE);
        btnPlayAudio.setVisibility(View.VISIBLE);
        ivQuestionImage.setVisibility(View.GONE);
        tvQuestionTitle.setText("הקשיבו לשאלה:");

        playTTS(q.getQuestionText());
        btnPlayAudio.setOnClickListener(v -> playTTS(q.getQuestionText()));
        setupSelectionButtons(q);
    }

    private void displayImageQuestion(Question q) {
        containerSelection.setVisibility(View.VISIBLE);
        resetSelectionButtonColors();
        btnPlayAudio.setVisibility(View.GONE);
        ivQuestionImage.setVisibility(View.VISIBLE);
        tvQuestionTitle.setText(q.getQuestionText());

        int resId = getResources().getIdentifier(q.getMediaUrl(), "drawable", getPackageName());
        ivQuestionImage.setImageResource(resId != 0 ? resId : R.drawable.wizard_placeholder);
        setupSelectionButtons(q);

    }

    private void displaySentenceQuestion(SentenceQuestion sq) {
        containerSentence.setVisibility(View.VISIBLE);
        tvQuestionTitle.setText("השלם את המשפט:");

        ImageView ivHint = findViewById(R.id.ivMixedSentenceHint);
        TextView tvSentence = findViewById(R.id.tvMixedSentenceText);
        EditText etAnswer = findViewById(R.id.etMixedAnswer);
        GridView keyboardGrid = findViewById(R.id.mixedKeyboard);
        Button btnCheck = findViewById(R.id.btnCheck);

        // הגדרת תוכן
        int resId = getResources().getIdentifier(sq.getHintImage(), "drawable", getPackageName());
        ivHint.setImageResource(resId != 0 ? resId : R.drawable.wizard_placeholder);
        tvSentence.setText(sq.getSentence());
        etAnswer.setText("");
        etAnswer.setBackgroundTintList(null); // איפוס צבע מהשאלה הקודמת

        // הגדרת המקלדת המקורית שלך
        com.example.themagicofknowledge.adapter.KeyboardAdapter adapter = new com.example.themagicofknowledge.adapter.KeyboardAdapter(this, hebrewLetters, letter -> {
            if (letter.equals("DEL")) {
                String str = etAnswer.getText().toString();
                if (str.length() > 0) {
                    etAnswer.setText(str.substring(0, str.length() - 1));
                }
            } else {
                etAnswer.append(letter);
            }
        });
        keyboardGrid.setAdapter(adapter);

        // לוגיקת בדיקה
        btnCheck.setOnClickListener(v -> {
            if (isProcessingAnswer) return;
            String userAns = etAnswer.getText().toString().trim();
            if (userAns.equalsIgnoreCase(sq.getCorrectAnswer())) {
                isProcessingAnswer = true;
                etAnswer.setBackgroundTintList(ColorStateList.valueOf(Color.GREEN));
                handleCorrect();
            } else {
                handleWrong(etAnswer); // ישתמש באנימציה והרעד שהגדרנו קודם!
            }
        });
    }

    private void displayMemoryGame(DataSnapshot data) {
        containerMemory.setVisibility(View.VISIBLE);
        tvQuestionTitle.setText("משחק הזיכרון - מצאו את הזוגות!");
        memoryCards.clear();

        List<DataSnapshot> items = (List<DataSnapshot>) data;

        for (DataSnapshot ds : items) {
            String imageName = ds.child("image").getValue(String.class);
            String displayName = ds.child("name").getValue(String.class);

            if (imageName != null && displayName != null) {
                int resId = getResources().getIdentifier(imageName, "drawable", getPackageName());
                addPairToMemoryList(resId, displayName);
            }
        }

        Collections.shuffle(memoryCards);
        memoryAdapter = new MemoryAdapter(this, memoryCards, cardSize);
        gvMemoryBoard.setAdapter(memoryAdapter);

        gvMemoryBoard.setOnItemClickListener((parent, view, position, id) -> {
            if (isMemoryBusy) return;
            MemoryCard selected = memoryCards.get(position);
            if (selected.isMatched || selected.isFlipped) return;

            selected.isFlipped = true;
            memoryAdapter.notifyDataSetChanged();

            if (firstMemorySelected == null) {
                firstMemorySelected = selected;
            } else {
                secondMemorySelected = selected;
                checkMemoryMatch();
            }
        });
    }

    private void addPairToMemoryList(int resId, String name) {
        String ageGroup = currentChild.getAgeGroup();
        if (ageGroup.equals("3-4") || ageGroup.equals("5-6")) {
            memoryCards.add(new MemoryCard(resId, null, name));
            memoryCards.add(new MemoryCard(resId, null, name));
        } else {
            memoryCards.add(new MemoryCard(resId, null, name));
            memoryCards.add(new MemoryCard(0, name, name));
        }
    }

    private void checkMemoryMatch() {
        isMemoryBusy = true;
        new Handler().postDelayed(() -> {
            if (firstMemorySelected.matchId.equals(secondMemorySelected.matchId)) {
                firstMemorySelected.isMatched = true;
                secondMemorySelected.isMatched = true;

                // בדיקה אם הכל נפתר
                if (checkAllMemoryMatched()) {
                    handleCorrect(); // עובר לשאלה המעורבבת הבאה!
                }
            } else {
                firstMemorySelected.isFlipped = false;
                secondMemorySelected.isFlipped = false;
                attempts++; // מעדכן את מונה הטעויות הכללי של המנוע
            }
            firstMemorySelected = null;
            secondMemorySelected = null;
            memoryAdapter.notifyDataSetChanged();
            isMemoryBusy = false;
        }, 800);
    }

    private boolean checkAllMemoryMatched() {
        for (MemoryCard c : memoryCards) if (!c.isMatched) return false;
        return true;
    }

    private void displayMatchingQuestion(List<Pair> pairs) {
        containerMatching.setVisibility(View.VISIBLE);
        tvQuestionTitle.setText("התאימו את הזוגות!");

        leftColumn.removeAllViews();
        rightColumn.removeAllViews();
        matchesFound = 0;
        totalPairs = pairs.size(); // שומרים כמה זוגות צריך למצוא כדי לנצח
        selectedView = null;

        // יצירת רשימות נפרדות לצד ימין ושמאל כדי לערבב אותן
        List<Pair> leftSide = new ArrayList<>(pairs);
        List<Pair> rightSide = new ArrayList<>(pairs);
        Collections.shuffle(leftSide);
        Collections.shuffle(rightSide);

        // הוספת הכפתורים לטור שמאל
        for (Pair p : leftSide) {
            setupMatchingView(p.getLeft(), p.getId(), leftColumn);
        }

        // הוספת הכפתורים לטור ימין
        for (Pair p : rightSide) {
            setupMatchingView(p.getRight(), p.getId(), rightColumn);
        }
    }

    private void setupMatchingView(String content, String id, LinearLayout column) {
        MaterialButton btn = (MaterialButton) getLayoutInflater().inflate(R.layout.item_matching_button, column, false);
        btn.setTag(id);

        // לוגיקת תוכן (מבוסס על setupContent מהמקור)
        if (content != null) {
            if (content.startsWith("ss_")) {
                int resId = getResources().getIdentifier(content, "drawable", getPackageName());
                btn.setIconResource(resId != 0 ? resId : R.drawable.wizard_placeholder);
                btn.setText("");
                btn.setIconSize(180);
                btn.setIconTint(null);
            } else if (content.equalsIgnoreCase("audio") || content.equalsIgnoreCase("speaker")) {
                btn.setIconResource(R.drawable.ic_volume_up);
                btn.setText("");
                btn.setIconSize(150);
                btn.setIconTint(ColorStateList.valueOf(Color.parseColor("#2196F3")));
                btn.setOnClickListener(v -> playTTS(id)); // בדרך כלל ה-ID הוא המילה שצריך להקריא
            } else {
                btn.setText(content);
                btn.setIconResource(0);
            }
        }

        // הגדרת גרירה (Drag)
        btn.setOnLongClickListener(v -> {
            View.DragShadowBuilder shadowBuilder = new View.DragShadowBuilder(v);
            v.startDragAndDrop(null, shadowBuilder, v, 0);
            v.setVisibility(View.INVISIBLE); // מעלים את המקור בזמן הגרירה
            return true;
        });

        // הגדרת יעד (Drop)
        btn.setOnDragListener((v, event) -> {
            switch (event.getAction()) {
                case DragEvent.ACTION_DRAG_STARTED: return true;
                case DragEvent.ACTION_DRAG_ENTERED:
                    v.setBackgroundTintList(ColorStateList.valueOf(Color.parseColor("#FFE082"))); // צהוב בכניסה
                    return true;
                case DragEvent.ACTION_DRAG_EXITED:
                    v.setBackgroundTintList(ColorStateList.valueOf(Color.WHITE)); // חזרה ללבן ביציאה
                    return true;
                case DragEvent.ACTION_DROP:
                    View draggedView = (View) event.getLocalState();
                    if (draggedView.getParent() != v.getParent() && draggedView.getTag().equals(v.getTag())) {
                        // הצלחה
                        v.setVisibility(View.INVISIBLE);
                        draggedView.setVisibility(View.INVISIBLE);
                        matchesFound++;
                        if (matchesFound == totalPairs) handleCorrect();
                    } else {
                        // טעות
                        handleWrong(v);
                        draggedView.setVisibility(View.VISIBLE); // מחזיר את הגרור למקום
                    }
                    return true;
                case DragEvent.ACTION_DRAG_ENDED:
                    if (!event.getResult()) btn.setVisibility(View.VISIBLE); // אם הגרירה בוטלה, נחזיר את הכפתור
                    return true;
            }
            return false;
        });

        column.addView(btn);
    }

    private void setupSelectionButtons(Question q) {
        List<String> options = q.getOptions();
        for (int i = 0; i < 4; i++) {
            final int index = i;
            String item = options.get(i);

            // בדיקה: האם זו שאלת שמע? אם כן, נשים תמונה. אם לא (זיהוי תמונה), נשים טקסט.
            int resId = getResources().getIdentifier(item, "drawable", getPackageName());

            if (resId != 0) {
                // אם נמצאה תמונה (כמו במקור), נציג אותה כפי שעשית
                selectionButtons[i].setIconResource(resId);
                selectionButtons[i].setText(""); // מוחק טקסט
                selectionButtons[i].setIconSize(220);
                selectionButtons[i].setIconGravity(MaterialButton.ICON_GRAVITY_TEXT_START); // מרכז האייקון
            } else {
                // אם אין תמונה, נציג טקסט רגיל
                selectionButtons[i].setIconResource(0);
                selectionButtons[i].setText(item);
            }

            selectionButtons[i].setOnClickListener(v -> {
                if (isProcessingAnswer) return; // אם כבר לחץ - אל תעשה כלום
                isProcessingAnswer = true; // חוסם לחיצות נוספות

                if (index == q.getCorrectAnswerIndex()) {
                    handleCorrect();
                } else {
                    isProcessingAnswer = false; // בשגיאה משחררים כדי שיוכל לנסות שוב
                    handleWrong(selectionButtons[index]);
                }
            });
        }
    }

    private void handleCorrect() {
        //android.media.MediaPlayer.create(this, R.raw.correct_sound).start();
        currentIndex++;
        new Handler().postDelayed(this::showCurrentQuestion, 1000);
    }

    private void handleWrong(View view) {
        //android.media.MediaPlayer.create(this, R.raw.wrong_sound).start();
        attempts++;

        // צביעת הכפתור באדום
        view.setBackgroundTintList(ColorStateList.valueOf(Color.RED));

        // הוסיפי את האנימציה הזו - זה הופך את זה להרבה יותר חי!
        view.animate().translationX(20).setDuration(50).withEndAction(() ->
                view.animate().translationX(-20).setDuration(50).withEndAction(() ->
                        view.animate().translationX(0).setDuration(50).start()
                ).start()
        ).start();

        new Handler().postDelayed(() -> {
            // החזרה לצבע המקורי (לבן או צבע הכפתור)
            view.setBackgroundTintList(ColorStateList.valueOf(Color.WHITE));
        }, 1000);
    }

    private void updateProgressInFirebase() {
        // חישוב אחוז ליניארי מ-15% עד 100%
        int percent = 15 + (int)(((double)currentIndex / allQuestions.size()) * 85);
        String pId = SharedPreferencesUtil.getUser(this).getId();

        DatabaseService.getInstance().updateDetailedProgress(
                pId, currentChild.getId(), currentChild.getAgeGroup(),
                subject, 0, 0, percent, currentIndex
        );
        globalProgress.setProgress(percent);
    }

    private void resetSelectionButtonColors() {
        // מערך הצבעים מהמשחק המקורי שלך
        String[] colors = {"#FF9800", "#4CAF50", "#2196F3", "#E91E63"};
        for (int i = 0; i < selectionButtons.length; i++) {
            if (selectionButtons[i] != null) {
                selectionButtons[i].setBackgroundTintList(ColorStateList.valueOf(Color.parseColor(colors[i])));
                selectionButtons[i].setTextColor(Color.WHITE); // כדי שהטקסט יהיה קריא
            }
        }
    }

    private void hideAllLayouts() {
        containerSelection.setVisibility(View.GONE);
        containerMatching.setVisibility(View.GONE);
        containerSentence.setVisibility(View.GONE);
        containerMemory.setVisibility(View.GONE);
    }

    private void playTTS(String text) {
        if (tts != null) tts.speak(text, TextToSpeech.QUEUE_FLUSH, null, null);
    }

    private void finishGame() {
        long totalTime = (System.currentTimeMillis() - startTime) / 1000;

        // חישוב ציון: (מספר שאלות / (מספר שאלות + טעויות)) * 100
        // ככה אם הוא ענה על הכל נכון בלי טעויות בכלל הוא מקבל 100
        double score = ((double) allQuestions.size() / (allQuestions.size() + attempts)) * 100;

        if (score >= 80) {
            // הצלחה! שומרים את ה-"וי" ב-Firebase
            markSubjectAsCompletedInFirebase();

            // כאן תציגי את דיאלוג ההצלחה שלך (גביע/מזל טוב)
            showSuccessDialog(score);
        } else {
            // הציון נמוך מדי - לא שומרים את ה-"וי"
            // מציגים דיאלוג "כמעט הצלחת, בוא ננסה שוב"
            showTryAgainDialog(score);
        }
    }

    private void showTryAgainDialog(double score) {
        final android.app.Dialog dialog = new android.app.Dialog(this);
        dialog.setContentView(R.layout.dialog_test_result); // משתמשים ב-XML הקיים שלך

        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
        }

        // חיבור הרכיבים מה-XML
        TextView tvTitle = dialog.findViewById(R.id.tvDialogTitle);
        TextView tvMessage = dialog.findViewById(R.id.tvDialogMessage);
        ImageView ivStatus = dialog.findViewById(R.id.ivStatusIcon);
        Button btnAction = dialog.findViewById(R.id.btnDialogAction);
        Button btnStay = dialog.findViewById(R.id.btnStayAtCurrentLevel); // הכפתור השני שהוספנו
        RatingBar ratingBar = dialog.findViewById(R.id.dialogRatingBar);

        // התאמת התוכן למקרה של "נסה שוב"
        tvTitle.setText("כמעט הצלחת!");
        tvTitle.setTextColor(Color.parseColor("#333333")); // צבע כהה כי הרקע לבן

        tvMessage.setText("קיבלת " + (int)score + " נקודות.\nכדי לקבל את ה-V צריך לפחות 80.\nרוצה לנסות שוב?");
        tvMessage.setTextColor(Color.parseColor("#666666"));

        // שינוי האייקון למשהו פחות "חגיגי" מגביע (אולי מדליה או הקוסם)
        ivStatus.setImageResource(R.drawable.ic_medal);

        // עדכון הכוכבים לפי הציון שהשיג
        if (ratingBar != null) {
            ratingBar.setRating((float) (score / 20));
        }

        // הגדרת הכפתור המרכזי - לנסות שוב
        btnAction.setText("אני רוצה לנסות שוב!");
        btnAction.setOnClickListener(v -> {
            dialog.dismiss();
            // איפוס נתונים והרצה מחדש
            currentIndex = 0;
            attempts = 0;
            Collections.shuffle(allQuestions);
            showCurrentQuestion();
        });

        // הגדרת הכפתור המשני - לצאת לתפריט
        if (btnStay != null) {
            btnStay.setVisibility(View.VISIBLE);
            btnStay.setText("אולי אחר כך");
            btnStay.setOnClickListener(v -> {
                dialog.dismiss();
                finish(); // חוזר למסך בחירת הנושאים
            });
        }

        dialog.setCancelable(false);
        dialog.show();
    }

    private void showSuccessDialog(double score) {
        final android.app.Dialog dialog = new android.app.Dialog(this);
        dialog.setContentView(R.layout.dialog_test_result);

        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
        }

        TextView tvTitle = dialog.findViewById(R.id.tvDialogTitle);
        TextView tvMessage = dialog.findViewById(R.id.tvDialogMessage);
        ImageView ivStatus = dialog.findViewById(R.id.ivStatusIcon);
        RatingBar ratingBar = dialog.findViewById(R.id.dialogRatingBar);
        Button btnAction = dialog.findViewById(R.id.btnDialogAction);
        Button btnStay = dialog.findViewById(R.id.btnStayAtCurrentLevel);

        tvTitle.setText("וואו! אלוף!");
        tvTitle.setTextColor(Color.parseColor("#4CAF50"));
        tvMessage.setText("סיימת את התרגול בהצלחה!\nעכשיו מופיע לך V ירוק בתפריט.");
        tvMessage.setTextColor(Color.parseColor("#333333"));
        ivStatus.setImageResource(R.drawable.ic_trophy);

        if (ratingBar != null) ratingBar.setRating((float) (score / 20));
        if (btnStay != null) btnStay.setVisibility(View.GONE); // מחביאים את הכפתור המיותר

        btnAction.setText("חזור לתפריט");
        btnAction.setOnClickListener(v -> {
            dialog.dismiss();
            finish();
        });

        dialog.setCancelable(false);
        dialog.show();
    }

    private void markSubjectAsCompletedInFirebase() {
        String parentId = SharedPreferencesUtil.getUser(this).getId();
        DatabaseReference ref = FirebaseDatabase.getInstance().getReference("users")
                .child(parentId)
                .child("childrenList")
                .child(currentChild.getId())
                .child("completedSubjects");

        // שמירת הנושא כהושלם (למשל animals: true)
        ref.child(subject).setValue(true).addOnSuccessListener(aVoid -> {
            // עדכון אחוז התקדמות ל-100%
            DatabaseService.getInstance().updateDetailedProgress(
                    parentId, currentChild.getId(), currentChild.getAgeGroup(),
                    subject, 0, 0, 100, currentIndex
            );
        });
    }

    @Override
    protected void onDestroy() {
        if (tts != null) { tts.stop(); tts.shutdown(); }
        super.onDestroy();
    }
}