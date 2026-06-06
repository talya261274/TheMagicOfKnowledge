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

public class MixedGameActivity extends BaseActivity {

    private static final String TAG = "MixedGameActivity";
    private final String[] hebrewLetters = {
            "ו", "ה", "ד", "ג", "ב", "א", "DEL",
            "מ", "ל", "כ", "י", "ט", "ח", "ז",
            "ר", "ק", "צ", "פ", "ע", "ס", "נ",
            "ץ", "ף", "ן", "ם", "ך", "ת", "ש",
    };
    private EditText etAnswer;

    // ===== נתוני המשחק =====
    private List<UnifiedQuestion> allQuestions = new ArrayList<>();
    private int currentIndex = 0;
    private int attempts = 0;
    private int cardSize;
    private String subject;
    private boolean isProcessingAnswer = false;

    // ⭐ חדש - מעקב אחר זמן המשחק
    private long gameStartTime;

    private UserChild currentChild;
    private TextToSpeech tts;

    // ===== רכיבי UI =====
    private ProgressBar globalProgress;
    private TextView tvQuestionTitle;
    private TextView tvProgressText;
    private View containerSelection, containerMatching, containerSentence, containerMemory;

    // ===== רכיבים לשאלות בחירה =====
    private LinearLayout btnPlayAudio;
    private ImageView ivQuestionImage;
    private View imageCardContainer;
    private final ImageView[] selectionButtons = new ImageView[4];
    private final TextView[] selectionTextViews = new TextView[4];
    private final String[] buttonColors = {"#FF9800", "#4CAF50", "#2196F3", "#E91E63"};

    // ===== רכיבים למשחק ההתאמה =====
    private LinearLayout leftColumn, rightColumn;
    private int matchesFound = 0;
    private int totalPairs;

    // ===== ניהול טעינת הנתונים =====
    private int tasksToLoad = 3;
    private int tasksCompleted = 0;

    // ===== רכיבים למשחק הזיכרון =====
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

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_mixed_game);

        subject = getIntent().getStringExtra("subject");
        currentChild = SharedPreferencesUtil.getCurrentChild(this);

        if (currentChild == null || subject == null) {
            Log.e(TAG, "Child or subject is null. Finishing.");
            finish();
            return;
        }

        // ⭐ חדש - מתחילים למדוד זמן
        gameStartTime = System.currentTimeMillis();

        initViews();
        setupTTS();
        loadAllData();
    }

    private void initViews() {
        globalProgress = findViewById(R.id.globalProgress);
        tvProgressText = findViewById(R.id.tvProgressText);
        tvQuestionTitle = findViewById(R.id.tvMixedQuestionTitle);

        containerSelection = findViewById(R.id.containerSelection);
        containerMatching = findViewById(R.id.containerMatching);
        containerSentence = findViewById(R.id.containerSentence);
        containerMemory = findViewById(R.id.containerMemory);

        imageCardContainer = findViewById(R.id.imageCardContainer);

        btnPlayAudio = findViewById(R.id.btnMixedPlayAudio);
        findViewById(R.id.btnExit).setOnClickListener(v -> showExitDialog());

        ivQuestionImage = findViewById(R.id.ivMixedImage);

        selectionButtons[0] = findViewById(R.id.btnAns1);
        selectionButtons[1] = findViewById(R.id.btnAns2);
        selectionButtons[2] = findViewById(R.id.btnAns3);
        selectionButtons[3] = findViewById(R.id.btnAns4);

        selectionTextViews[0] = findViewById(R.id.tvAns1);
        selectionTextViews[1] = findViewById(R.id.tvAns2);
        selectionTextViews[2] = findViewById(R.id.tvAns3);
        selectionTextViews[3] = findViewById(R.id.tvAns4);

        leftColumn = findViewById(R.id.mixedLeftColumn);
        rightColumn = findViewById(R.id.mixedRightColumn);

        gvMemoryBoard = findViewById(R.id.gvMemoryBoard);

        DisplayMetrics metrics = getResources().getDisplayMetrics();
        int screenWidth = metrics.widthPixels - (int) (24 * metrics.density);
        cardSize = (screenWidth - (int) (24 * metrics.density)) / 3;
    }

    private void setupTTS() {
        tts = new TextToSpeech(this, status -> {
            if (status == TextToSpeech.SUCCESS) tts.setLanguage(new Locale("he"));
        });
    }

    private void loadAllData() {
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

    private void loadCategory(String path, UnifiedQuestion.Type type, Class<?> modelClass) {
        DatabaseService.getInstance().loadGameData(path, new DatabaseService.DatabaseCallback<DataSnapshot>() {
            @Override
            public void onCompleted(DataSnapshot snapshot) {
                try {
                    if (type == UnifiedQuestion.Type.MEMORY) {
                        List<DataSnapshot> allItems = new ArrayList<>();
                        for (DataSnapshot item : snapshot.getChildren()) {
                            allItems.add(item);
                        }
                        int chunkSize = 3;
                        for (int i = 0; i < allItems.size(); i += chunkSize) {
                            int end = Math.min(i + chunkSize, allItems.size());
                            allQuestions.add(new UnifiedQuestion(type, new ArrayList<>(allItems.subList(i, end))));
                        }

                    } else if (type == UnifiedQuestion.Type.MATCHING) {
                        List<Pair> pairs = new ArrayList<>();
                        for (DataSnapshot ds : snapshot.getChildren()) {
                            Pair p = ds.getValue(Pair.class);
                            if (p != null) pairs.add(p);
                        }
                        int chunkSize = 4;
                        for (int i = 0; i < pairs.size(); i += chunkSize) {
                            int end = Math.min(i + chunkSize, pairs.size());
                            allQuestions.add(new UnifiedQuestion(type, new ArrayList<>(pairs.subList(i, end))));
                        }

                    } else {
                        for (DataSnapshot ds : snapshot.getChildren()) {
                            Object data = ds.getValue(modelClass);
                            if (data != null) allQuestions.add(new UnifiedQuestion(type, data));
                        }
                    }
                } catch (Exception e) {
                    Log.e(TAG, "Error loading type=" + type + ": " + e.getMessage());
                }
                checkIfLoadingFinished();
            }

            @Override
            public void onFailed(Exception e) {
                Log.e(TAG, "❌ שגיאת Firebase ב-" + type + ": " + e.getMessage());
                checkIfLoadingFinished();
            }
        });
    }

    private void checkIfLoadingFinished() {
        tasksCompleted++;

        if (tasksCompleted >= tasksToLoad) {
            if (allQuestions.isEmpty()) {
                Log.w(TAG, "No questions loaded.");
                new AlertDialog.Builder(this)
                        .setTitle("הקוסם נח כרגע")
                        .setMessage("עוד אין שאלות בנושא הזה. בוא ננסה נושא אחר!")
                        .setPositiveButton("אוקיי", (d, w) -> finish())
                        .setCancelable(false)
                        .show();
                return;
            }

            allQuestions = interleaveQuestions(allQuestions);

            Log.d(TAG, "📋 סה\"כ שאלות: " + allQuestions.size());
            for (int i = 0; i < allQuestions.size(); i++) {
                Log.d(TAG, "   [" + i + "] " + allQuestions.get(i).getType()
                        + " - data: " + (allQuestions.get(i).getData() == null ? "NULL!" : allQuestions.get(i).getData().getClass().getSimpleName()));
            }
            checkSavedProgress();
        }
    }

    private List<UnifiedQuestion> interleaveQuestions(List<UnifiedQuestion> original) {
        Map<UnifiedQuestion.Type, List<UnifiedQuestion>> grouped = new HashMap<>();

        for (UnifiedQuestion.Type type : UnifiedQuestion.Type.values()) {
            grouped.put(type, new ArrayList<>());
        }

        for (UnifiedQuestion q : original) {
            List<UnifiedQuestion> list = grouped.get(q.getType());
            if (list != null) list.add(q);
        }

        for (List<UnifiedQuestion> list : grouped.values()) {
            Collections.shuffle(list);
        }

        List<UnifiedQuestion> interleaved = new ArrayList<>();
        boolean added;

        do {
            added = false;
            List<UnifiedQuestion.Type> types = new ArrayList<>(grouped.keySet());
            Collections.shuffle(types);

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

    private void saveProgressLocally() {
        if (currentIndex == 0) return;
        getSharedPreferences("game_progress", MODE_PRIVATE)
                .edit()
                .putInt(subject + "_index", currentIndex)
                .putString(subject + "_child", currentChild.getId())
                .apply();
    }

    private void clearProgressLocally() {
        getSharedPreferences("game_progress", MODE_PRIVATE)
                .edit()
                .remove(subject + "_index")
                .remove(subject + "_child")
                .apply();
    }

    private void checkSavedProgress() {
        SharedPreferences prefs = getSharedPreferences("game_progress", MODE_PRIVATE);
        int savedIdx = prefs.getInt(subject + "_index", 0);
        String savedChild = prefs.getString(subject + "_child", "");

        if (savedIdx > 0 && currentChild.getId().equals(savedChild)
                && savedIdx < allQuestions.size()) {
            showContinueDialog(savedIdx);
        } else {
            showCurrentQuestion();
        }
    }

    private void showContinueDialog(int savedIdx) {
        final android.app.Dialog dialog = new android.app.Dialog(this);
        dialog.setContentView(R.layout.dialog_continue_progress);
        if (dialog.getWindow() != null)
            dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);

        dialog.findViewById(R.id.btnContinue).setOnClickListener(v -> {
            dialog.dismiss();
            currentIndex = savedIdx;
            showCurrentQuestion();
        });

        dialog.findViewById(R.id.btnRestart).setOnClickListener(v -> {
            dialog.dismiss();
            clearProgressLocally();
            currentIndex = 0;
            showCurrentQuestion();
        });

        dialog.setCancelable(false);
        dialog.show();
    }

    private void showCurrentQuestion() {
        isProcessingAnswer = false;

        if (currentIndex >= allQuestions.size()) {
            finishGame();
            return;
        }

        hideAllLayouts();
        updateProgressBar();

        UnifiedQuestion uq = allQuestions.get(currentIndex);

        // הוסף בדיקה
        if (uq == null || uq.getData() == null || uq.getType() == null) {
            Log.e(TAG, "❌ uq is null or invalid at index " + currentIndex);
            currentIndex++;
            showCurrentQuestion();
            return;
        }

        Log.d(TAG, "Showing question " + currentIndex + " type: " + uq.getType());

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
            Log.e(TAG, "Error displaying question: " + e.getMessage());
            currentIndex++;
            showCurrentQuestion();
        }
    }

    private void updateProgressBar() {
        if (allQuestions.isEmpty()) return;
        int percent = 15 + (int) (((double) currentIndex / allQuestions.size()) * 85);
        globalProgress.setProgress(percent);
        if (tvProgressText != null) tvProgressText.setText(percent + "%");
    }

    private void displayAudioQuestion(Question q) {
        Log.d(TAG, "🔊 הצגת שאלת שמע");

        containerSelection.setVisibility(View.VISIBLE);
        resetSelectionButtonColors();
        btnPlayAudio.setVisibility(View.VISIBLE);

        // ⭐ מסתיר את כרטיס התמונה
        imageCardContainer.setVisibility(View.GONE);

        tvQuestionTitle.setText("האזינו לשאלה 🎧");

        playTTS(q.getQuestionText());
        btnPlayAudio.setOnClickListener(v -> playTTS(q.getQuestionText()));

        setupSelectionButtons(q);
    }

    private void displayImageQuestion(Question q) {
        Log.d(TAG, "🖼️ הצגת שאלת תמונה");
        Log.d(TAG, "   - mediaUrl: " + q.getMediaUrl());

        containerSelection.setVisibility(View.VISIBLE);
        resetSelectionButtonColors();
        btnPlayAudio.setVisibility(View.GONE);

        // ⭐ מציג את הכרטיס שמכיל את התמונה
        imageCardContainer.setVisibility(View.VISIBLE);
        ivQuestionImage.setVisibility(View.VISIBLE);

        tvQuestionTitle.setText(q.getQuestionText());

        int resId = getResources().getIdentifier(q.getMediaUrl(), "drawable", getPackageName());
        Log.d(TAG, "   - resId: " + resId + (resId == 0 ? " ❌ לא נמצא!" : " ✅"));

        ivQuestionImage.setImageResource(resId != 0 ? resId : R.drawable.wizard_placeholder1);

        setupSelectionButtons(q);
    }

    private void displaySentenceQuestion(SentenceQuestion sq) {
        containerSentence.setVisibility(View.VISIBLE);
        tvQuestionTitle.setText("השלימו את המשפט ✏️");

        ImageView ivHint = findViewById(R.id.ivMixedSentenceHint);
        TextView tvSentence = findViewById(R.id.tvMixedSentenceText);
        EditText etAnswer = findViewById(R.id.etMixedAnswer);
        GridView mixedKeyboard = findViewById(R.id.mixedKeyboard);
        mixedKeyboard.setNestedScrollingEnabled(false);
        Button btnCheck = findViewById(R.id.btnCheck);

        int resId = getResources().getIdentifier(sq.getHintImage(), "drawable", getPackageName());
        ivHint.setImageResource(resId != 0 ? resId : R.drawable.wizard_placeholder1);

        tvSentence.setText(sq.getSentence());
        etAnswer.setText("");
        etAnswer.setTextColor(Color.parseColor("#1E5F8B"));

        KeyboardAdapter adapter = new KeyboardAdapter(this, hebrewLetters, letter -> {
            if (letter.equals("DEL")) {
                String str = etAnswer.getText().toString();
                if (!str.isEmpty()) etAnswer.setText(str.substring(0, str.length() - 1));
            } else {
                etAnswer.append(letter);
            }
        });
        mixedKeyboard.setAdapter(adapter);

        btnCheck.setOnClickListener(v -> {
            if (isProcessingAnswer) return;

            String userAns = etAnswer.getText().toString().trim();

            if (userAns.equalsIgnoreCase(sq.getCorrectAnswer())) {
                isProcessingAnswer = true;
                shakeAndColorAnswer(etAnswer, true);
                new Handler(Looper.getMainLooper()).postDelayed(() -> handleCorrect(), 800);
            } else {
                shakeAndColorAnswer(etAnswer, false);
            }
        });
    }

    private void shakeAndColorAnswer(EditText etAnswer, boolean isCorrect) {
        int color = isCorrect ? Color.parseColor("#4CAF50") : Color.parseColor("#F44336");
        etAnswer.setTextColor(color);

        if (!isCorrect) {
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

            etAnswer.postDelayed(() -> {
                etAnswer.setTextColor(Color.parseColor("#1E5F8B"));
                etAnswer.setText("");
            }, 800);
        } else {
            etAnswer.postDelayed(() ->
                    etAnswer.setTextColor(Color.parseColor("#1E5F8B")), 600);
        }
    }

    private void displayMemoryGame(List<DataSnapshot> items) {

        containerMemory.setVisibility(View.VISIBLE);
        tvQuestionTitle.setText("מצאו את הזוגות 🎴");
        memoryCards.clear();

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
            if (selected.isMatched() || selected.isFlipped()) return;
            selected.setFlipped(true);
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

        new Handler(Looper.getMainLooper()).postDelayed(() -> {
            if (firstMemorySelected.getMatchId().equals(secondMemorySelected.getMatchId())) {
                firstMemorySelected.setMatched(true);
                secondMemorySelected.setMatched(true);
                firstMemorySelected.setFlipped(false);
                secondMemorySelected.setFlipped(false);

                firstMemorySelected = null;
                secondMemorySelected = null;
                memoryAdapter.notifyDataSetChanged();
                isMemoryBusy = false;

                if (checkAllMemoryMatched()) handleCorrect();
            } else {
                firstMemorySelected.setFlipped(false);
                secondMemorySelected.setFlipped(false);
                firstMemorySelected = null;
                secondMemorySelected = null;
                memoryAdapter.notifyDataSetChanged();
                isMemoryBusy = false;
            }
        }, 1000);
    }

    private boolean checkAllMemoryMatched() {
        for (MemoryCard c : memoryCards) {
            if (!c.isMatched()) return false;
        }
        return true;
    }

    private void displayMatchingQuestion(List<Pair> pairs) {
        containerMatching.setVisibility(View.VISIBLE);
        tvQuestionTitle.setText("התאימו את הזוגות 🔗");

        leftColumn.removeAllViews();
        rightColumn.removeAllViews();

        matchesFound = 0;
        totalPairs = pairs.size();

        List<Pair> leftSide = new ArrayList<>(pairs);
        List<Pair> rightSide = new ArrayList<>(pairs);
        Collections.shuffle(leftSide);
        Collections.shuffle(rightSide);

        for (Pair p : leftSide) setupMatchingView(p.getLeft(), p.getId(), leftColumn);
        for (Pair p : rightSide) setupMatchingView(p.getRight(), p.getId(), rightColumn);
    }

    private void setupMatchingView(String content, String id, LinearLayout column) {
        com.google.android.material.card.MaterialCardView card =
                (com.google.android.material.card.MaterialCardView) getLayoutInflater()
                        .inflate(R.layout.item_matching_button, column, false);

        card.setTag(id);

        TextView answerText = card.findViewById(R.id.answerText);
        ImageView answerImage = card.findViewById(R.id.answerImage);

        if (content != null) {
            if (content.startsWith("ss_")) {
                int resId = getResources().getIdentifier(content, "drawable", getPackageName());
                answerImage.setImageResource(resId != 0 ? resId : R.drawable.wizard_placeholder1);
                answerImage.setVisibility(View.VISIBLE);
                answerText.setVisibility(View.GONE);
            } else if (content.equalsIgnoreCase("audio") || content.equalsIgnoreCase("speaker")) {
                answerText.setText("🔊");
                answerText.setTextSize(32);
                answerImage.setVisibility(View.GONE);
                card.setOnClickListener(v -> playTTS(id));
            } else {
                answerText.setText(content);
                answerText.setVisibility(View.VISIBLE);
                answerImage.setVisibility(View.GONE);
            }
        }

        card.setOnLongClickListener(v -> {
            if (Boolean.TRUE.equals(v.getTag(R.id.tag_matched))) {
                return false;
            }
            View.DragShadowBuilder shadowBuilder = new View.DragShadowBuilder(v);
            v.startDragAndDrop(null, shadowBuilder, v, 0);
            v.setVisibility(View.INVISIBLE);
            return true;
        });

        card.setOnDragListener((v, event) -> {
            switch (event.getAction()) {
                case DragEvent.ACTION_DRAG_STARTED:
                    return true;

                case DragEvent.ACTION_DRAG_ENTERED:
                    if (!Boolean.TRUE.equals(v.getTag(R.id.tag_matched))) {
                        ((com.google.android.material.card.MaterialCardView) v)
                                .setCardBackgroundColor(Color.parseColor("#ffe4e1"));
                    }
                    return true;

                case DragEvent.ACTION_DRAG_EXITED:
                    if (!Boolean.TRUE.equals(v.getTag(R.id.tag_matched))) {
                        ((com.google.android.material.card.MaterialCardView) v)
                                .setCardBackgroundColor(Color.WHITE);
                    }
                    return true;

                case DragEvent.ACTION_DROP:
                    View draggedView = (View) event.getLocalState();
                    if (draggedView != null && draggedView.getParent() != v.getParent()
                            && draggedView.getTag().equals(v.getTag())
                            && !Boolean.TRUE.equals(v.getTag(R.id.tag_matched))) {
                        markCardAsMatched(v);
                        markCardAsMatched(draggedView);
                        matchesFound++;
                        if (matchesFound == totalPairs) handleCorrect();
                    } else {
                        if (!Boolean.TRUE.equals(v.getTag(R.id.tag_matched))) handleWrong(v);
                        if (draggedView != null) draggedView.setVisibility(View.VISIBLE);
                    }
                    return true;

                case DragEvent.ACTION_DRAG_ENDED:
                    if (!event.getResult()) card.setVisibility(View.VISIBLE);
                    return true;
            }
            return false;
        });

        card.setOnClickListener(v -> {
            if (Boolean.TRUE.equals(v.getTag(R.id.tag_matched))) return;
            ((com.google.android.material.card.MaterialCardView) v)
                    .setCardBackgroundColor(Color.parseColor("#B3E5FC"));
            new Handler(Looper.getMainLooper()).postDelayed(() ->
                    ((com.google.android.material.card.MaterialCardView) v)
                            .setCardBackgroundColor(Color.WHITE), 300);
        });
        column.addView(card);
    }

    private void markCardAsMatched(View view) {
        view.setTag(R.id.tag_matched, true);
        view.setVisibility(View.VISIBLE);
        view.setAlpha(1f);
        view.setEnabled(false);
        view.setClickable(false);
        view.setLongClickable(false);

        if (view instanceof com.google.android.material.card.MaterialCardView) {
            ((com.google.android.material.card.MaterialCardView) view)
                    .setCardBackgroundColor(Color.parseColor("#c8f5c8"));
        } else {
            view.setBackgroundTintList(ColorStateList.valueOf(Color.parseColor("#c8f5c8")));
        }

        view.animate().scaleX(0.95f).scaleY(0.95f).setDuration(200)
                .withEndAction(() -> view.animate().scaleX(1f).scaleY(1f).setDuration(200).start()).start();
    }

    private void setupSelectionButtons(Question q) {
        List<String> options = q.getOptions();
        if (options == null || options.size() < 4) return;

        for (int i = 0; i < 4; i++) {
            final int index = i;
            String item = options.get(i);
            int resId = getResources().getIdentifier(item, "drawable", getPackageName());

            if (resId != 0) {
                selectionButtons[i].setImageResource(resId);
                selectionButtons[i].setVisibility(View.VISIBLE);
                selectionTextViews[i].setVisibility(View.GONE);
            } else {
                selectionButtons[i].setVisibility(View.GONE);
                selectionTextViews[i].setText(item);
                selectionTextViews[i].setVisibility(View.VISIBLE);
            }

            View.OnClickListener listener = v -> {
                if (isProcessingAnswer) return;
                isProcessingAnswer = true;
                if (index == q.getCorrectAnswerIndex()) {
                    shakeAndColorCard(index, true);
                    new Handler(Looper.getMainLooper()).postDelayed(() -> handleCorrect(), 600);
                } else {
                    isProcessingAnswer = false;
                    shakeAndColorCard(index, false);
                }
            };

            selectionButtons[i].setOnClickListener(listener);
            selectionTextViews[i].setOnClickListener(listener);
        }
    }

    private void shakeAndColorCard(int index, boolean isCorrect) {
        int[] cardIds = {R.id.cardAns1, R.id.cardAns2, R.id.cardAns3, R.id.cardAns4};
        View card = findViewById(cardIds[index]);
        if (!(card instanceof com.google.android.material.card.MaterialCardView)) return;

        com.google.android.material.card.MaterialCardView cardView =
                (com.google.android.material.card.MaterialCardView) card;

        int feedbackColor = isCorrect ? Color.parseColor("#00ff00") : Color.parseColor("#F44336");
        cardView.setCardBackgroundColor(feedbackColor);

        if (!isCorrect) {
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

        // החזרת הצבע המקורי
        card.postDelayed(() ->
                        cardView.setCardBackgroundColor(Color.parseColor(buttonColors[index])),
                isCorrect ? 600 : 800
        );
    }

    private void handleCorrect() {
        currentIndex++;
        saveProgressLocally(); // מיידי!
        updateProgressInFirebase(); // לסטטיסטיקות
        new Handler(Looper.getMainLooper()).postDelayed(this::showCurrentQuestion, 1000);
    }

    private void handleWrong(View view) {
        attempts++;

        view.setBackgroundTintList(ColorStateList.valueOf(Color.RED));
        view.animate().translationX(20).setDuration(50).withEndAction(() ->
                view.animate().translationX(-20).setDuration(50).withEndAction(() ->
                        view.animate().translationX(0).setDuration(50).start()
                ).start()
        ).start();

        view.postDelayed(() -> {
            if (view instanceof EditText) {
                view.setBackgroundTintList(null);
            } else {
                view.setBackgroundTintList(ColorStateList.valueOf(Color.WHITE));
            }
        }, 800);
    }

    private void updateProgressInFirebase() {
        if (allQuestions.isEmpty() || currentIndex == 0) return;
        if (SharedPreferencesUtil.getUser(this) == null) return;

        int percent = 15 + (int) (((double) currentIndex / allQuestions.size()) * 85);
        String parentId = SharedPreferencesUtil.getUser(this).getId();
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
        gameStartTime = System.currentTimeMillis();
        attempts = 0;
    }

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

    private void hideAllLayouts() {
        containerSelection.setVisibility(View.GONE);
        containerMatching.setVisibility(View.GONE);
        containerSentence.setVisibility(View.GONE);
        containerMemory.setVisibility(View.GONE);
    }

    private void playTTS(String text) {
        if (tts != null) {
            tts.speak(text, TextToSpeech.QUEUE_FLUSH, null, null);
        }
    }

    private void finishGame() {
        //  עדכון אחרון של הזמן והטעויות לפני סיום
        updateProgressInFirebase();

        double score = ((double) allQuestions.size() / (allQuestions.size() + attempts)) * 100;

        if (score >= 80) {
            markSubjectAsCompletedInFirebase();
            showSuccessDialog(score);
        } else {
            showTryAgainDialog(score);
        }
    }

    private void showExitDialog() {
        showCustomDialog(
                "לצאת מהמשחק?",
                "אל דאגה! ההתקדמות שלך נשמרת ותוכל להמשיך בפעם הבאה 🌟",
                "יציאה",
                Color.parseColor("#FF9800"),
                () -> {
                    updateProgressInFirebase(); // שומר התקדמות
                    finish();
                }
        );
    }

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
        if (ratingBar != null) ratingBar.setRating((float) (score / 20));

        if (btnAction != null) {
            btnAction.setText("אני רוצה לנסות שוב!");
            btnAction.setOnClickListener(v -> {
                dialog.dismiss();
                currentIndex = 0;
                attempts = 0;
                gameStartTime = System.currentTimeMillis();  // ⭐ איפוס זמן
                Collections.shuffle(allQuestions);
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

        if (btnStay != null) btnStay.setVisibility(View.GONE);

        if (btnAction != null) {
            btnAction.setText("חזור לתפריט");
            btnAction.setOnClickListener(v -> { dialog.dismiss(); finish(); });
        }

        dialog.setCancelable(false);
        dialog.show();
    }

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

    @Override
    protected void onDestroy() {
        // הסר את updateProgressInFirebase מכאן!
        if (tts != null) {
            tts.stop();
            tts.shutdown();
        }
        super.onDestroy();
    }
}