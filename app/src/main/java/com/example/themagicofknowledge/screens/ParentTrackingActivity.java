package com.example.themagicofknowledge.screens;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.HorizontalScrollView;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.themagicofknowledge.R;
import com.example.themagicofknowledge.adapter.LevelProgressAdapter;
import com.example.themagicofknowledge.models.LevelProgress;
import com.example.themagicofknowledge.models.SubjectStat;
import com.example.themagicofknowledge.models.UserChild;
import com.example.themagicofknowledge.models.UserParent;
import com.example.themagicofknowledge.models.UserRole;
import com.example.themagicofknowledge.utils.SharedPreferencesUtil;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class ParentTrackingActivity extends AppCompatActivity {

    private ImageView ivSelectedChildAvatar;
    private TextView tvSelectedChildName, tvSelectedChildAge, tvCompletedCount, tvCurrentLevel;
    private LinearLayout containerChildrenSelector;
    private RecyclerView rvLevels;

    private List<LevelProgress> levelsList = new ArrayList<>();
    private LevelProgressAdapter levelAdapter;
    private UserParent currentParent;

    // ⭐ סדר הרמות באפליקציה
    private static final String[] AGE_GROUPS = {"3-4", "5-6", "7-8"};
    // ⭐ כל הנושאים האפשריים
    private static final String[] ALL_SUBJECTS = {"animals", "colors", "numbers", "letters", "shapes", "bodyparts"};

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (UserRole.isChild(this)) {
            Toast.makeText(this, "מסך זה אינו זמין", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        setContentView(R.layout.activity_parent_tracking);
        initViews();

        currentParent = SharedPreferencesUtil.getUser(this);
        String selectedChildId = getIntent().getStringExtra("SELECTED_CHILD_ID");

        if (currentParent != null && currentParent.getChildrenList() != null) {
            setupChildrenSelector();
            if (selectedChildId == null || selectedChildId.isEmpty()) {
                selectedChildId = new ArrayList<>(currentParent.getChildrenList().keySet()).get(0);
            }
            loadStatsForChild(selectedChildId);
        }
    }

    private void initViews() {
        ivSelectedChildAvatar = findViewById(R.id.iv_selected_child_avatar);
        tvSelectedChildName = findViewById(R.id.tv_selected_child_name);
        tvSelectedChildAge = findViewById(R.id.tv_selected_child_age);
        tvCompletedCount = findViewById(R.id.tvCompletedCount);
        tvCurrentLevel = findViewById(R.id.tvCurrentLevel);
        containerChildrenSelector = findViewById(R.id.container_children_selector);
        rvLevels = findViewById(R.id.rvLevels);

        rvLevels.setLayoutManager(new LinearLayoutManager(this));
        levelAdapter = new LevelProgressAdapter(levelsList);
        rvLevels.setAdapter(levelAdapter);

        if (findViewById(R.id.goBackBtnTracking) != null) {
            findViewById(R.id.goBackBtnTracking).setOnClickListener(v -> finish());
        }

        LinearLayout headerExpandable = findViewById(R.id.headerExpandable);
        HorizontalScrollView expandableContent = findViewById(R.id.expandableContent);
        ImageView ivExpandArrow = findViewById(R.id.ivExpandArrow);

        headerExpandable.setOnClickListener(v -> {
            if (expandableContent.getVisibility() == View.VISIBLE) {
                expandableContent.setVisibility(View.GONE);
                ivExpandArrow.animate().rotation(0f).setDuration(200).start();
            } else {
                expandableContent.setVisibility(View.VISIBLE);
                ivExpandArrow.animate().rotation(180f).setDuration(200).start();
            }
        });
    }

    private void setupChildrenSelector() {
        containerChildrenSelector.removeAllViews();
        LayoutInflater inflater = LayoutInflater.from(this);

        for (UserChild child : currentParent.getChildrenList().values()) {
            View childView = inflater.inflate(R.layout.item_child_carousel, containerChildrenSelector, false);

            ImageView ivAvatar = childView.findViewById(R.id.iv_child_avatar);
            TextView tvName = childView.findViewById(R.id.tv_child_name);
            TextView tvAge = childView.findViewById(R.id.tv_child_age);

            tvName.setText(child.getName());
            tvAge.setText("גיל " + child.getAge());

            if (child.getAvatar() != null) {
                int resId = getResources().getIdentifier(child.getAvatar(), "drawable", getPackageName());
                ivAvatar.setImageResource(resId != 0 ? resId : R.drawable.logo);
            }

            childView.setOnClickListener(v -> loadStatsForChild(child.getId()));
            containerChildrenSelector.addView(childView);
        }
    }

    private void loadStatsForChild(String childId) {
        UserChild selectedChild = currentParent.getChildrenList().get(childId);
        if (selectedChild != null) {
            tvSelectedChildName.setText(selectedChild.getName());
            tvSelectedChildAge.setText("גיל: " + selectedChild.getAge());
            int resId = getResources().getIdentifier(selectedChild.getAvatar(), "drawable", getPackageName());
            ivSelectedChildAvatar.setImageResource(resId != 0 ? resId : R.drawable.logo);

            // ⭐ הצגת רמה נוכחית
            String currentAge = selectedChild.getAgeGroup();
            if (currentAge != null) {
                tvCurrentLevel.setText("🌟 רמה נוכחית: " + currentAge);
                tvCurrentLevel.setVisibility(View.VISIBLE);
            } else {
                tvCurrentLevel.setVisibility(View.GONE);
            }
        }

        DatabaseReference childRef = FirebaseDatabase.getInstance().getReference("users")
                .child(currentParent.getId()).child("childrenList").child(childId);

        childRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                processProgressData(snapshot, selectedChild);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.e("ParentTracking", error.getMessage());
            }
        });
    }

    /**
     * ⭐⭐⭐ פונקציה חדשה - מעבדת את הנתונים לרשימת רמות ⭐⭐⭐
     */
    private void processProgressData(DataSnapshot snapshot, UserChild selectedChild) {
        levelsList.clear();
        int totalCompletedSubjects = 0;

        // ⭐ מציאת הרמה הנוכחית של הילד
        String currentAgeGroup = selectedChild != null ? selectedChild.getAgeGroup() : null;
        int currentLevelIndex = -1;
        if (currentAgeGroup != null) {
            currentLevelIndex = Arrays.asList(AGE_GROUPS).indexOf(currentAgeGroup);
        }

        DataSnapshot progressSnapshot = snapshot.child("progress");

        // ⭐ עוברים על כל הרמות בסדר קבוע
        for (int i = 0; i < AGE_GROUPS.length; i++) {
            String ageGroup = AGE_GROUPS[i];
            LevelProgress level = new LevelProgress(ageGroup);

            // ⭐ קביעת מצב הרמה
            if (currentLevelIndex == -1) {
                // אין רמה נוכחית - נחשב הכל כנעול
                level.setLocked(true);
            } else if (i > currentLevelIndex) {
                // רמה עתידית - נעולה
                level.setLocked(true);
            } else if (i == currentLevelIndex) {
                // הרמה הנוכחית
                level.setCurrent(true);
            }

            // ⭐ טעינת הנושאים של הרמה
            List<SubjectStat> subjects = new ArrayList<>();
            int completedInLevel = 0;

            if (progressSnapshot.hasChild(ageGroup)) {
                DataSnapshot levelSnapshot = progressSnapshot.child(ageGroup);

                // עוברים על כל 6 הנושאים
                for (String subjectKey : ALL_SUBJECTS) {
                    boolean isCompleted = false;
                    int attempts = 0;
                    long timeSeconds = 0;
                    int progressPercent = 0;

                    if (levelSnapshot.hasChild(subjectKey)) {
                        DataSnapshot subjectData = levelSnapshot.child(subjectKey);

                        Boolean completedVal = subjectData.child("completed").getValue(Boolean.class);
                        isCompleted = completedVal != null && completedVal;

                        Integer attemptsVal = subjectData.child("attempts").getValue(Integer.class);
                        attempts = attemptsVal != null ? attemptsVal : 0;

                        Long timeVal = subjectData.child("timeSeconds").getValue(Long.class);
                        timeSeconds = timeVal != null ? timeVal : 0;

                        Integer percentVal = subjectData.child("progressPercent").getValue(Integer.class);
                        progressPercent = percentVal != null ? percentVal : 0;
                    }

                    if (isCompleted) completedInLevel++;

                    subjects.add(new SubjectStat(
                            translateSubject(subjectKey),
                            attempts,
                            timeSeconds,
                            isCompleted,
                            progressPercent
                    ));
                }
            } else {
                // אין נתונים - יוצרים רשימה ריקה של 6 נושאים
                for (String subjectKey : ALL_SUBJECTS) {
                    subjects.add(new SubjectStat(translateSubject(subjectKey), 0, 0, false, 0));
                }
            }

            level.setSubjects(subjects);

            // ⭐ אם כל 6 הנושאים הושלמו - הרמה הושלמה
            if (completedInLevel >= 6) {
                level.setCompleted(true);
            }

            // ⭐ פתיחה אוטומטית של הרמה הנוכחית
            if (level.isCurrent()) {
                level.setExpanded(true);
            }

            // ⭐ סופרים סך הכל הנושאים שהושלמו (רק ברמה הנוכחית!)
            if (level.isCurrent()) {
                totalCompletedSubjects = completedInLevel;
            }

            levelsList.add(level);
        }

        tvCompletedCount.setText(String.valueOf(totalCompletedSubjects));
        levelAdapter.notifyDataSetChanged();
    }

    private String translateSubject(String sub) {
        if (sub == null) return "";
        switch (sub.toLowerCase()) {
            case "animals":
                return "חיות";
            case "numbers":
                return "מספרים";
            case "colors":
                return "צבעים";
            case "letters":
                return "אותיות";
            case "shapes":
                return "צורות";
            case "bodyparts":
                return "חלקי גוף";
            default:
                return sub;
        }
    }
}