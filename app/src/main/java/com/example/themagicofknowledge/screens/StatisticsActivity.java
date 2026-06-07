package com.example.themagicofknowledge.screens;

import android.graphics.Color;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.cardview.widget.CardView;

import com.example.themagicofknowledge.R;
import com.example.themagicofknowledge.models.UserChild;
import com.example.themagicofknowledge.models.UserParent;
import com.example.themagicofknowledge.services.DatabaseService;
import com.example.themagicofknowledge.utils.SharedPreferencesUtil;
import com.google.firebase.database.DataSnapshot;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class StatisticsActivity extends BaseActivity {

    private static final String[] SUBJECTS = {"animals", "colors", "numbers", "letters", "shapes", "bodyparts"};
    private static final String[] SUBJECT_NAMES = {"חיות", "צבעים", "מספרים", "אותיות", "צורות", "חלקי גוף"};
    private static final int[] SUBJECT_COLORS = {
            Color.parseColor("#378ADD"),
            Color.parseColor("#1D9E75"),
            Color.parseColor("#EF9F27"),
            Color.parseColor("#D4537E"),
            Color.parseColor("#534AB7"),
            Color.parseColor("#D85A30")
    };

    private TextView tvTotalChildren, tvAvgCompletion, tvLevelUpCount, tvCompletedAll;
    private TextView tv34, tv56, tv78;
    private View bar34, bar56, bar78;
    private LinearLayout subjectBarsContainer, difficultyContainer;
    private ProgressBar statsLoader;
    private LinearLayout statsContent;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        UserParent currentUser = SharedPreferencesUtil.getUser(this);
        if (currentUser == null || !currentUser.isAdmin()) {
            Toast.makeText(this, "אין הרשאה", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        setContentView(R.layout.activity_statistics);
        initViews();
        loadStatistics();
    }

    private void initViews() {
        tvTotalChildren = findViewById(R.id.tvTotalChildren);
        tvAvgCompletion = findViewById(R.id.tvAvgCompletion);
        tvLevelUpCount = findViewById(R.id.tvLevelUpCount);
        tvCompletedAll = findViewById(R.id.tvCompletedAll);
        tv34 = findViewById(R.id.tv34);
        tv56 = findViewById(R.id.tv56);
        tv78 = findViewById(R.id.tv78);
        bar34 = findViewById(R.id.bar34);
        bar56 = findViewById(R.id.bar56);
        bar78 = findViewById(R.id.bar78);
        subjectBarsContainer = findViewById(R.id.subjectBarsContainer);
        difficultyContainer = findViewById(R.id.difficultyContainer);
        statsLoader = findViewById(R.id.statsLoader);
        statsContent = findViewById(R.id.statsContent);
    }

    private void loadStatistics() {
        DatabaseService.getInstance().getUserList(new DatabaseService.DatabaseCallback<List<UserParent>>() {
            @Override
            public void onCompleted(List<UserParent> users) {
                processData(users);
            }

            @Override
            public void onFailed(Exception e) {
                runOnUiThread(() -> Toast.makeText(StatisticsActivity.this, "שגיאה בטעינה", Toast.LENGTH_SHORT).show());
            }
        });
    }

    private void processData(List<UserParent> users) {
        int totalChildren = 0;
        int count34 = 0, count56 = 0, count78 = 0;
        int levelUpCount = 0;
        int completedAllCount = 0;

        // מפות לצבירת נתוני נושאים
        Map<String, Integer> subjectCompletedCount = new HashMap<>();
        Map<String, Integer> subjectTotalCount = new HashMap<>();
        Map<String, Long> subjectAttempts = new HashMap<>();

        for (String s : SUBJECTS) {
            subjectCompletedCount.put(s, 0);
            subjectTotalCount.put(s, 0);
            subjectAttempts.put(s, 0L);
        }

        int totalProgressPercent = 0;
        int progressCount = 0;

        for (UserParent parent : users) {
            if (parent == null || parent.isAdmin()) continue;
            if (parent.getChildrenList() == null) continue;

            for (UserChild child : parent.getChildrenList().values()) {
                if (child == null) continue;
                totalChildren++;

                // ספירה לפי רמה
                String ag = child.getAgeGroup();
                if ("3-4".equals(ag)) count34++;
                else if ("5-6".equals(ag)) count56++;
                else if ("7-8".equals(ag)) count78++;

                // בדיקה אם עלה רמה (startingLevel שונה מ-ageGroup)
                // נוסיף את זה בשלב הבא דרך Firebase

                // עיבוד התקדמות
                if (child.getProgress() != null) {
                    Object progressObj = child.getProgress().get(ag);
                    if (progressObj instanceof Map) {
                        Map<?, ?> progressMap = (Map<?, ?>) progressObj;
                        int completedSubjects = 0;

                        for (String subject : SUBJECTS) {
                            Object subjectObj = progressMap.get(subject);
                            if (subjectObj instanceof Map) {
                                Map<?, ?> subjectData = (Map<?, ?>) subjectObj;

                                subjectTotalCount.put(subject, subjectTotalCount.get(subject) + 1);

                                Boolean completed = (Boolean) subjectData.get("completed");
                                if (Boolean.TRUE.equals(completed)) {
                                    subjectCompletedCount.put(subject, subjectCompletedCount.get(subject) + 1);
                                    completedSubjects++;
                                }

                                Object percentObj = subjectData.get("progressPercent");
                                if (percentObj instanceof Long) {
                                    totalProgressPercent += ((Long) percentObj).intValue();
                                    progressCount++;
                                } else if (percentObj instanceof Integer) {
                                    totalProgressPercent += (Integer) percentObj;
                                    progressCount++;
                                }

                                Object attObj = subjectData.get("attempts");
                                if (attObj instanceof Long) {
                                    subjectAttempts.put(subject, subjectAttempts.get(subject) + (Long) attObj);
                                }
                            }
                        }

                        if (completedSubjects >= 6) completedAllCount++;
                    }
                }
            }
        }

        // חישוב עלו רמה - נטען ישירות מ-Firebase
        final int finalCount34 = count34;
        final int finalCount56 = count56;
        final int finalCount78 = count78;
        final int finalTotalChildren = totalChildren;
        final int finalCompletedAllCount = completedAllCount;
        final int avgCompletion = progressCount > 0 ? totalProgressPercent / progressCount : 0;
        final Map<String, Integer> finalCompletedCount = new HashMap<>(subjectCompletedCount);
        final Map<String, Integer> finalTotalCount = new HashMap<>(subjectTotalCount);
        final Map<String, Long> finalAttempts = new HashMap<>(subjectAttempts);

        // ספירת עלו רמה
        countLevelUps(levelUpsFinal -> {
            runOnUiThread(() -> updateUI(
                    finalTotalChildren, avgCompletion, levelUpsFinal, finalCompletedAllCount,
                    finalCount34, finalCount56, finalCount78,
                    finalCompletedCount, finalTotalCount, finalAttempts
            ));
        });
    }

    interface LevelUpsCallback {
        void onResult(int count);
    }

    private void countLevelUps(LevelUpsCallback callback) {
        DatabaseService.getInstance().loadAllUsersData(new DatabaseService.DatabaseCallback<DataSnapshot>() {
            @Override
            public void onCompleted(DataSnapshot snapshot) {
                int count = 0;
                for (DataSnapshot userSnap : snapshot.getChildren()) {
                    DataSnapshot childrenSnap = userSnap.child("childrenList");
                    for (DataSnapshot childSnap : childrenSnap.getChildren()) {
                        String startingLevel = childSnap.child("startingLevel").getValue(String.class);
                        String currentLevel = childSnap.child("ageGroup").getValue(String.class);
                        if (startingLevel != null && currentLevel != null && !startingLevel.equals(currentLevel)) {
                            count++;
                        }
                    }
                }
                callback.onResult(count);
            }
            @Override
            public void onFailed(Exception e) {
                callback.onResult(0);
            }
        });
    }

    private void updateUI(int totalChildren, int avgCompletion, int levelUps, int completedAll,
                          int count34, int count56, int count78,
                          Map<String, Integer> completedCount, Map<String, Integer> totalCount,
                          Map<String, Long> attempts) {

        statsLoader.setVisibility(View.GONE);
        statsContent.setVisibility(View.VISIBLE);

        // מדדים ראשיים
        tvTotalChildren.setText(String.valueOf(totalChildren));
        tvAvgCompletion.setText(avgCompletion + "%");
        tvLevelUpCount.setText(String.valueOf(levelUps));
        tvCompletedAll.setText(String.valueOf(completedAll));

        // פסי רמה
        tv34.setText(String.valueOf(count34));
        tv56.setText(String.valueOf(count56));
        tv78.setText(String.valueOf(count78));

        int maxLevel = Math.max(Math.max(count34, count56), count78);
        if (maxLevel > 0) {
            setBarWidth(bar34, count34, maxLevel);
            setBarWidth(bar56, count56, maxLevel);
            setBarWidth(bar78, count78, maxLevel);
        }

        // פסי נושאים
        subjectBarsContainer.removeAllViews();
        difficultyContainer.removeAllViews();

        // מיון לפי אחוז השלמה לנושאים קשים
        int[] completionPercents = new int[SUBJECTS.length];
        for (int i = 0; i < SUBJECTS.length; i++) {
            String s = SUBJECTS[i];
            int total = totalCount.getOrDefault(s, 0);
            int completed = completedCount.getOrDefault(s, 0);
            completionPercents[i] = total > 0 ? (completed * 100 / total) : 0;
        }

        // הצגת פסי נושאים
        for (int i = 0; i < SUBJECTS.length; i++) {
            addSubjectBar(SUBJECT_NAMES[i], completionPercents[i], SUBJECT_COLORS[i]);
        }

        // טבלת קושי - מיון מהקשה לקל
        Integer[] indices = new Integer[SUBJECTS.length];
        for (int i = 0; i < indices.length; i++) indices[i] = i;
        java.util.Arrays.sort(indices, (a, b) -> completionPercents[a] - completionPercents[b]);

        for (int rank = 0; rank < indices.length; rank++) {
            int i = indices[rank];
            long att = attempts.getOrDefault(SUBJECTS[i], 0L);
            int total = totalCount.getOrDefault(SUBJECTS[i], 0);
            double avgAtt = total > 0 ? (double) att / total : 0;
            addDifficultyRow(rank + 1, SUBJECT_NAMES[i], completionPercents[i], avgAtt);
        }
    }

    private void setBarWidth(View bar, int value, int max) {
        bar.post(() -> {
            ViewGroup parent = (ViewGroup) bar.getParent();
            int totalWidth = parent.getWidth();
            int newWidth = max > 0 ? (int) ((float) value / max * totalWidth) : 0;
            ViewGroup.LayoutParams params = bar.getLayoutParams();
            params.width = newWidth;
            bar.setLayoutParams(params);
        });
    }

    private void addSubjectBar(String name, int percent, int color) {
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(android.view.Gravity.CENTER_VERTICAL);
        LinearLayout.LayoutParams rowParams = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        rowParams.setMargins(0, 0, 0, dpToPx(10));
        row.setLayoutParams(rowParams);

        // תווית
        TextView label = new TextView(this);
        label.setText(name);
        label.setTextSize(13);
        label.setTextColor(Color.parseColor("#5C6166"));
        LinearLayout.LayoutParams labelParams = new LinearLayout.LayoutParams(dpToPx(70), ViewGroup.LayoutParams.WRAP_CONTENT);
        label.setLayoutParams(labelParams);
        row.addView(label);

        // רקע אפור
        CardView bgCard = new CardView(this);
        LinearLayout.LayoutParams bgParams = new LinearLayout.LayoutParams(0, dpToPx(10), 1f);
        bgParams.setMargins(dpToPx(8), 0, dpToPx(8), 0);
        bgCard.setLayoutParams(bgParams);
        bgCard.setCardBackgroundColor(Color.parseColor("#E0E0E0"));
        bgCard.setRadius(dpToPx(5));
        bgCard.setCardElevation(0);

        // פס צבעוני
        View fill = new View(this);
        fill.setBackgroundColor(color);
        ViewGroup.LayoutParams fillParams = new ViewGroup.LayoutParams(
                (int) (percent / 100f * 1000), ViewGroup.LayoutParams.MATCH_PARENT);
        fill.setLayoutParams(fillParams);
        bgCard.addView(fill);

        // עדכון רוחב אחרי layout
        bgCard.post(() -> {
            int totalWidth = bgCard.getWidth();
            ViewGroup.LayoutParams p = fill.getLayoutParams();
            p.width = (int) (percent / 100f * totalWidth);
            fill.setLayoutParams(p);
        });

        row.addView(bgCard);

        // אחוז
        TextView pct = new TextView(this);
        pct.setText(percent + "%");
        pct.setTextSize(13);
        pct.setTypeface(null, android.graphics.Typeface.BOLD);
        pct.setTextColor(color);
        LinearLayout.LayoutParams pctParams = new LinearLayout.LayoutParams(dpToPx(40), ViewGroup.LayoutParams.WRAP_CONTENT);
        pct.setLayoutParams(pctParams);
        row.addView(pct);

        subjectBarsContainer.addView(row);
    }

    private void addDifficultyRow(int rank, String name, int percent, double avgAttempts) {
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(android.view.Gravity.CENTER_VERTICAL);
        LinearLayout.LayoutParams rowParams = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, dpToPx(40));
        rowParams.setMargins(0, 0, 0, dpToPx(4));
        row.setLayoutParams(rowParams);
        row.setBackgroundColor(rank % 2 == 0 ? Color.parseColor("#F8F9FB") : Color.TRANSPARENT);
        row.setPadding(dpToPx(8), 0, dpToPx(8), 0);

        // מיקום
        TextView rankTv = new TextView(this);
        rankTv.setText(String.valueOf(rank));
        rankTv.setTextSize(13);
        rankTv.setTextColor(Color.parseColor("#9E9E9E"));
        rankTv.setGravity(Gravity.CENTER);
        LinearLayout.LayoutParams rankParams = new LinearLayout.LayoutParams(dpToPx(24), ViewGroup.LayoutParams.WRAP_CONTENT);
        rankTv.setLayoutParams(rankParams);
        row.addView(rankTv);

        // שם
        TextView nameTv = new TextView(this);
        nameTv.setText(name);
        nameTv.setTextSize(13);
        nameTv.setTextColor(Color.parseColor("#1E5F8B"));
        LinearLayout.LayoutParams nameParams = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
        nameParams.setMarginStart(dpToPx(8));
        nameTv.setLayoutParams(nameParams);
        row.addView(nameTv);

        // אחוז השלמה
        TextView pctTv = new TextView(this);
        pctTv.setText(percent + "%");
        pctTv.setTextSize(13);
        pctTv.setTypeface(null, android.graphics.Typeface.BOLD);
        int pctColor = percent >= 70 ? Color.parseColor("#3B6D11") :
                percent >= 40 ? Color.parseColor("#854F0B") : Color.parseColor("#A32D2D");
        pctTv.setTextColor(pctColor);
        LinearLayout.LayoutParams pctParams = new LinearLayout.LayoutParams(dpToPx(50), ViewGroup.LayoutParams.WRAP_CONTENT);
        pctTv.setLayoutParams(pctParams);
        pctTv.setGravity(Gravity.CENTER);
        row.addView(pctTv);

        // ממוצע ניסיונות
        TextView attTv = new TextView(this);
        attTv.setText(String.format("%.1f ניס'", avgAttempts));
        attTv.setTextSize(12);
        attTv.setTextColor(Color.parseColor("#9E9E9E"));
        LinearLayout.LayoutParams attParams = new LinearLayout.LayoutParams(dpToPx(60), ViewGroup.LayoutParams.WRAP_CONTENT);
        attTv.setLayoutParams(attParams);
        attTv.setGravity(Gravity.END);
        row.addView(attTv);

        difficultyContainer.addView(row);
    }

    private int dpToPx(int dp) {
        float density = getResources().getDisplayMetrics().density;
        return Math.round(dp * density);
    }
}