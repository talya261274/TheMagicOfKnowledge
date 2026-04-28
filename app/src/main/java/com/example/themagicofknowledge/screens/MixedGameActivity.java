package com.example.themagicofknowledge.screens;

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
import androidx.appcompat.app.AppCompatActivity;

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
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.Query;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class MixedGameActivity extends AppCompatActivity {

    private static final String TAG = "MixedGameActivity";

    private final String[] hebrewLetters = {
            "ו", "ה", "ד", "ג", "ב", "א", "DEL",
            "מ", "ל", "כ", "י", "ט", "ח", "ז",
            "ר", "ק", "צ", "פ", "ע", "ס", "נ",
            "ץ", "ף", "ן", "ם", "ך", "ת", "ש",
    };
    private List<UnifiedQuestion> allQuestions = new ArrayList<>();
    private int currentIndex = 0;
    private int attempts = 0;
    private int cardSize;
    private String subject;
    private boolean isProcessingAnswer = false;
    private UserChild currentChild;
    private TextToSpeech tts;
    
    private ProgressBar globalProgress;
    private TextView tvQuestionTitle;
    private View containerSelection, containerMatching, containerSentence, containerMemory;
    
    private MaterialButton btnPlayAudio;
    private ImageView ivQuestionImage;
    private final MaterialButton[] selectionButtons = new MaterialButton[4];
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

        initViews();
        setupTTS();
        loadAllData();
    }

    private void initViews() {
        globalProgress = findViewById(R.id.globalProgress);
        tvQuestionTitle = findViewById(R.id.tvMixedQuestionTitle);

        containerSelection = findViewById(R.id.containerSelection);
        containerMatching = findViewById(R.id.containerMatching);
        containerSentence = findViewById(R.id.containerSentence);
        containerMemory = findViewById(R.id.containerMemory);

        btnPlayAudio = findViewById(R.id.btnMixedPlayAudio);
        ivQuestionImage = findViewById(R.id.ivMixedImage);

        selectionButtons[0] = findViewById(R.id.btnAns1);
        selectionButtons[1] = findViewById(R.id.btnAns2);
        selectionButtons[2] = findViewById(R.id.btnAns3);
        selectionButtons[3] = findViewById(R.id.btnAns4);

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
        DatabaseReference rootRef = FirebaseDatabase.getInstance().getReference("Games");
        tasksCompleted = 0;
        allQuestions.clear();

        if (level.equals("3_4") || level.equals("5_6")) {
            tasksToLoad = 3;
            loadCategory(rootRef.child("audioRecognition").child("level_" + level).child(subject),
                    UnifiedQuestion.Type.AUDIO, Question.class);
            loadCategory(rootRef.child("matching").child("level_" + level).child(subject).child("pairs"),
                    UnifiedQuestion.Type.MATCHING, Pair.class);
            loadCategory(rootRef.child("memoryGame").child("level_" + level).child(subject),
                    UnifiedQuestion.Type.MEMORY, DataSnapshot.class);
        } else if (level.equals("7_8")) {
            tasksToLoad = 3;
            loadCategory(rootRef.child("imageRecognition").child("level_" + level).child(subject),
                    UnifiedQuestion.Type.IMAGE, Question.class);
            loadCategory(rootRef.child("sentenceCompletion").child("level_" + level).child(subject),
                    UnifiedQuestion.Type.SENTENCE, SentenceQuestion.class);
            loadCategory(rootRef.child("memoryGame").child("level_" + level).child(subject),
                    UnifiedQuestion.Type.MEMORY, DataSnapshot.class);
        } else {
            tasksToLoad = 1;
            loadCategory(rootRef.child("audioRecognition").child("level_3_4").child(subject),
                    UnifiedQuestion.Type.AUDIO, Question.class);
        }
    }

    private void loadCategory(Query query, UnifiedQuestion.Type type, Class<?> modelClass) {
        query.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
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
                        if (!pairs.isEmpty()) {
                            int chunkSize = 4;
                            for (int i = 0; i < pairs.size(); i += chunkSize) {
                                int end = Math.min(i + chunkSize, pairs.size());
                                allQuestions.add(new UnifiedQuestion(type, new ArrayList<>(pairs.subList(i, end))));
                            }
                        }
                    } else {
                        for (DataSnapshot ds : snapshot.getChildren()) {
                            Object data = ds.getValue(modelClass);
                            if (data != null) allQuestions.add(new UnifiedQuestion(type, data));
                        }
                    }
                } catch (Exception e) {
                    Log.e(TAG, "Error parsing " + type + " data: " + e.getMessage());
                }
                checkIfLoadingFinished();
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.e(TAG, "Database error loading " + type + ": " + error.getMessage());
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

    private void checkSavedProgress() {
        if (SharedPreferencesUtil.getUser(this) == null) {
            showCurrentQuestion();
            return;
        }
        String pId = SharedPreferencesUtil.getUser(this).getId();
        DatabaseReference ref = FirebaseDatabase.getInstance().getReference("users")
                .child(pId).child("childrenList").child(currentChild.getId())
                .child("progress").child(currentChild.getAgeGroup()).child(subject);

        ref.child("lastQuestionIndex").addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (snapshot.exists()) {
                    Integer savedIdx = snapshot.getValue(Integer.class);
                    if (savedIdx != null && savedIdx > 0 && savedIdx < allQuestions.size()) {
                        showContinueDialog(savedIdx);
                        return;
                    }
                }
                showCurrentQuestion();
            }
            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                showCurrentQuestion();
            }
        });
    }

    private void showContinueDialog(int savedIdx) {
        new AlertDialog.Builder(this)
                .setTitle("להמשיך מאיפה שעצרנו?")
                .setMessage("נראה שכבר התחלת לתרגל נושא זה.")
                .setPositiveButton("כן", (d, w) -> {
                    currentIndex = savedIdx;
                    showCurrentQuestion();
                })
                .setNegativeButton("מהתחלה", (d, w) -> {
                    currentIndex = 0;
                    showCurrentQuestion();
                })
                .setCancelable(false).show();
    }

    private void showCurrentQuestion() {
        isProcessingAnswer = false;
        if (currentIndex >= allQuestions.size()) {
            finishGame();
            return;
        }

        hideAllLayouts();
        updateProgressInFirebase();

        UnifiedQuestion uq = allQuestions.get(currentIndex);
        Log.d(TAG, "Showing question " + currentIndex + " type: " + uq.getType());
        try {
            switch (uq.getType()) {
                case AUDIO: displayAudioQuestion((Question) uq.getData()); break;
                case IMAGE: displayImageQuestion((Question) uq.getData()); break;
                case MATCHING: displayMatchingQuestion((List<Pair>) uq.getData()); break;
                case SENTENCE: displaySentenceQuestion((SentenceQuestion) uq.getData()); break;
                case MEMORY: displayMemoryGame((List<DataSnapshot>) uq.getData()); break;
            }
        } catch (Exception e) {
            Log.e(TAG, "Error displaying question: " + e.getMessage());
            currentIndex++;
            showCurrentQuestion();
        }
    }

    private void displayAudioQuestion(Question q) {
        containerSelection.setVisibility(View.VISIBLE);
        resetSelectionButtonColors();
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

        int resId = getResources().getIdentifier(sq.getHintImage(), "drawable", getPackageName());
        ivHint.setImageResource(resId != 0 ? resId : R.drawable.wizard_placeholder);
        tvSentence.setText(sq.getSentence());
        etAnswer.setText("");
        etAnswer.setBackgroundTintList(null);

        KeyboardAdapter adapter = new KeyboardAdapter(this, hebrewLetters, letter -> {
            if (letter.equals("DEL")) {
                String str = etAnswer.getText().toString();
                if (!str.isEmpty()) etAnswer.setText(str.substring(0, str.length() - 1));
            } else {
                etAnswer.append(letter);
            }
        });
        keyboardGrid.setAdapter(adapter);

        btnCheck.setOnClickListener(v -> {
            if (isProcessingAnswer) return;
            String userAns = etAnswer.getText().toString().trim();
            if (userAns.equalsIgnoreCase(sq.getCorrectAnswer())) {
                isProcessingAnswer = true;
                etAnswer.setBackgroundTintList(ColorStateList.valueOf(Color.GREEN));
                handleCorrect();
            } else {
                handleWrong(etAnswer);
            }
        });
    }

    private void displayMemoryGame(List<DataSnapshot> items) {
        containerMemory.setVisibility(View.VISIBLE);
        tvQuestionTitle.setText("משחק הזיכרון - מצאו את הזוגות!");
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
                if (checkAllMemoryMatched()) handleCorrect();
            } else {
                firstMemorySelected.setFlipped(false);
                secondMemorySelected.setFlipped(false);
                attempts++;
            }
            firstMemorySelected = null;
            secondMemorySelected = null;
            memoryAdapter.notifyDataSetChanged();
            isMemoryBusy = false;
        }, 800);
    }

    private boolean checkAllMemoryMatched() {
        for (MemoryCard c : memoryCards) if (!c.isMatched()) return false;
        return true;
    }

    private void displayMatchingQuestion(List<Pair> pairs) {
        containerMatching.setVisibility(View.VISIBLE);
        tvQuestionTitle.setText("התאימו את הזוגות!");
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
        MaterialButton btn = (MaterialButton) getLayoutInflater().inflate(R.layout.item_matching_button, column, false);
        btn.setTag(id);
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
                btn.setOnClickListener(v -> playTTS(id));
            } else {
                btn.setText(content);
                btn.setIconResource(0);
            }
        }
        btn.setOnLongClickListener(v -> {
            View.DragShadowBuilder shadowBuilder = new View.DragShadowBuilder(v);
            v.startDragAndDrop(null, shadowBuilder, v, 0);
            v.setVisibility(View.INVISIBLE);
            return true;
        });
        btn.setOnDragListener((v, event) -> {
            switch (event.getAction()) {
                case DragEvent.ACTION_DRAG_STARTED: return true;
                case DragEvent.ACTION_DRAG_ENTERED: v.setBackgroundTintList(ColorStateList.valueOf(Color.parseColor("#FFE082"))); return true;
                case DragEvent.ACTION_DRAG_EXITED: v.setBackgroundTintList(ColorStateList.valueOf(Color.WHITE)); return true;
                case DragEvent.ACTION_DROP:
                    View draggedView = (View) event.getLocalState();
                    if (draggedView != null && draggedView.getParent() != v.getParent() && draggedView.getTag().equals(v.getTag())) {
                        v.setVisibility(View.INVISIBLE);
                        draggedView.setVisibility(View.INVISIBLE);
                        matchesFound++;
                        if (matchesFound == totalPairs) handleCorrect();
                    } else {
                        handleWrong(v);
                        if (draggedView != null) draggedView.setVisibility(View.VISIBLE);
                    }
                    return true;
                case DragEvent.ACTION_DRAG_ENDED: if (!event.getResult()) btn.setVisibility(View.VISIBLE); return true;
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
            int resId = getResources().getIdentifier(item, "drawable", getPackageName());
            if (resId != 0) {
                selectionButtons[i].setIconResource(resId);
                selectionButtons[i].setIconTint(null);
                selectionButtons[i].setText("");
                selectionButtons[i].setIconSize(220);
                selectionButtons[i].setIconGravity(MaterialButton.ICON_GRAVITY_TEXT_START);
            } else {
                selectionButtons[i].setIconResource(0);
                selectionButtons[i].setText(item);
            }
            selectionButtons[i].setOnClickListener(v -> {
                if (isProcessingAnswer) return;
                isProcessingAnswer = true;
                if (index == q.getCorrectAnswerIndex()) {
                    handleCorrect();
                } else {
                    isProcessingAnswer = false;
                    handleWrong(selectionButtons[index]);
                }
            });
        }
    }

    private void handleCorrect() {
        currentIndex++;
        new Handler(Looper.getMainLooper()).postDelayed(this::showCurrentQuestion, 1000);
    }

    private void handleWrong(View view) {
        attempts++;
        final ColorStateList originalTint = view.getBackgroundTintList();
        view.setBackgroundTintList(ColorStateList.valueOf(Color.RED));
        view.animate().translationX(20).setDuration(50).withEndAction(() ->
                view.animate().translationX(-20).setDuration(50).withEndAction(() ->
                        view.animate().translationX(0).setDuration(50).start()
                ).start()
        ).start();
        new Handler(Looper.getMainLooper()).postDelayed(() -> {
            boolean restored = false;
            for (int i = 0; i < selectionButtons.length; i++) {
                if (view == selectionButtons[i]) {
                    view.setBackgroundTintList(ColorStateList.valueOf(Color.parseColor(buttonColors[i])));
                    restored = true;
                    break;
                }
            }
            if (!restored) view.setBackgroundTintList(originalTint);
        }, 1000);
    }

    private void updateProgressInFirebase() {
        if (allQuestions.isEmpty()) return;
        int percent = 15 + (int) (((double) currentIndex / allQuestions.size()) * 85);
        if (SharedPreferencesUtil.getUser(this) == null) return;
        String pId = SharedPreferencesUtil.getUser(this).getId();
        DatabaseService.getInstance().updateDetailedProgress(pId, currentChild.getId(), currentChild.getAgeGroup(), subject, 0, 0, percent, currentIndex);
        globalProgress.setProgress(percent);
    }

    private void resetSelectionButtonColors() {
        for (int i = 0; i < selectionButtons.length; i++) {
            if (selectionButtons[i] != null) {
                selectionButtons[i].setBackgroundTintList(ColorStateList.valueOf(Color.parseColor(buttonColors[i])));
                selectionButtons[i].setTextColor(Color.WHITE);
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
        double score = ((double) allQuestions.size() / (allQuestions.size() + attempts)) * 100;
        if (score >= 80) {
            markSubjectAsCompletedInFirebase();
            showSuccessDialog(score);
        } else {
            showTryAgainDialog(score);
        }
    }

    private void showTryAgainDialog(double score) {
        final android.app.Dialog dialog = new android.app.Dialog(this);
        dialog.setContentView(R.layout.dialog_test_result);
        if (dialog.getWindow() != null) dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
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
                Collections.shuffle(allQuestions);
                showCurrentQuestion();
            });
        }
        if (btnStay != null) {
            btnStay.setVisibility(View.VISIBLE);
            btnStay.setText("אולי אחר כך");
            btnStay.setOnClickListener(v -> { dialog.dismiss(); finish(); });
        }
        dialog.setCancelable(false);
        dialog.show();
    }

    private void showSuccessDialog(double score) {
        final android.app.Dialog dialog = new android.app.Dialog(this);
        dialog.setContentView(R.layout.dialog_test_result);
        if (dialog.getWindow() != null) dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
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
        DatabaseReference ref = FirebaseDatabase.getInstance().getReference("users").child(parentId).child("childrenList").child(currentChild.getId()).child("completedSubjects");
        ref.child(subject).setValue(true).addOnSuccessListener(aVoid -> {
            DatabaseService.getInstance().updateDetailedProgress(parentId, currentChild.getId(), currentChild.getAgeGroup(), subject, 0, 0, 100, currentIndex);
        });
    }

    @Override
    protected void onDestroy() {
        if (tts != null) { tts.stop(); tts.shutdown(); }
        super.onDestroy();
    }
}
