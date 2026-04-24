package com.example.themagicofknowledge.screens;

import android.os.Bundle;
import android.util.Log;
import android.view.Gravity;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.themagicofknowledge.R;
import com.example.themagicofknowledge.adapter.SubjectProgressAdapter;
import com.example.themagicofknowledge.models.SubjectStat;
import com.example.themagicofknowledge.models.UserChild;
import com.example.themagicofknowledge.models.UserParent;
import com.example.themagicofknowledge.utils.SharedPreferencesUtil;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.List;

public class ParentTrackingActivity extends AppCompatActivity {

    private ImageView ivSelectedChildAvatar;
    private TextView tvSelectedChildName, tvSelectedChildAge, tvCompletedCount;
    private LinearLayout containerChildrenSelector;
    private RecyclerView rvSubjects;

    private List<SubjectStat> subjectsStats = new ArrayList<>();
    private SubjectProgressAdapter adapter;
    private UserParent currentParent;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_parent_tracking);

        initViews();

        currentParent = SharedPreferencesUtil.getUser(this);

        // שליפת ה-ID שנשלח מהפרופיל
        String selectedChildId = getIntent().getStringExtra("SELECTED_CHILD_ID");

        if (currentParent != null && currentParent.getChildrenList() != null) {
            setupChildrenSelector();

            // אם לא הגיע ID, נבחר את הילד הראשון כברירת מחדל
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
        containerChildrenSelector = findViewById(R.id.container_children_selector);
        rvSubjects = findViewById(R.id.rvSubjectsProgress);

        rvSubjects.setLayoutManager(new LinearLayoutManager(this));
        adapter = new SubjectProgressAdapter(subjectsStats);
        rvSubjects.setAdapter(adapter);

        // כפתור חזרה (רק אם הוספת אותו ב-XML)
        if (findViewById(R.id.goBackBtnTracking) != null) {
            findViewById(R.id.goBackBtnTracking).setOnClickListener(v -> finish());
        }
    }

    private void setupChildrenSelector() {
        containerChildrenSelector.removeAllViews();
        for (UserChild child : currentParent.getChildrenList().values()) {
            LinearLayout itemLayout = new LinearLayout(this);
            itemLayout.setOrientation(LinearLayout.VERTICAL);
            itemLayout.setPadding(25, 10, 25, 10);
            itemLayout.setGravity(Gravity.CENTER);

            ImageView avatar = new ImageView(this);
            avatar.setLayoutParams(new LinearLayout.LayoutParams(130, 130));

            int resId = getResources().getIdentifier(child.getAvatar(), "drawable", getPackageName());
            avatar.setImageResource(resId != 0 ? resId : R.drawable.logo);

            TextView name = new TextView(this);
            name.setText(child.getName());
            name.setGravity(Gravity.CENTER);

            itemLayout.addView(avatar);
            itemLayout.addView(name);
            itemLayout.setOnClickListener(v -> loadStatsForChild(child.getId()));

            containerChildrenSelector.addView(itemLayout);
        }
    }

    private void loadStatsForChild(String childId) {
        UserChild selectedChild = currentParent.getChildrenList().get(childId);
        if (selectedChild != null) {
            tvSelectedChildName.setText(selectedChild.getName());
            tvSelectedChildAge.setText("גיל: " + selectedChild.getAge());
            int resId = getResources().getIdentifier(selectedChild.getAvatar(), "drawable", getPackageName());
            ivSelectedChildAvatar.setImageResource(resId != 0 ? resId : R.drawable.logo);
        }

        DatabaseReference childRef = FirebaseDatabase.getInstance().getReference("users")
                .child(currentParent.getId()).child("childrenList").child(childId);

        childRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                subjectsStats.clear();
                List<String> finishedSubjects = new ArrayList<>();

                DataSnapshot completedSnapshot = snapshot.child("completedSubjects");
                for (DataSnapshot ds : completedSnapshot.getChildren()) {
                    if (Boolean.TRUE.equals(ds.getValue(Boolean.class)))
                        finishedSubjects.add(ds.getKey());
                }

                DataSnapshot progressSnapshot = snapshot.child("progress");
                if (progressSnapshot.exists()) {
                    for (DataSnapshot ageGroup : progressSnapshot.getChildren()) {
                        for (DataSnapshot ds : ageGroup.getChildren()) {
                            String rawName = ds.getKey();
                            subjectsStats.add(new SubjectStat(
                                    translateSubject(rawName),
                                    ds.child("attempts").getValue(Integer.class) != null ? ds.child("attempts").getValue(Integer.class) : 0,
                                    ds.child("timeSeconds").getValue(Long.class) != null ? ds.child("timeSeconds").getValue(Long.class) : 0,
                                    finishedSubjects.contains(rawName),
                                    ds.child("progressPercent").getValue(Integer.class) != null ? ds.child("progressPercent").getValue(Integer.class) : 0
                            ));
                        }
                    }
                }
                tvCompletedCount.setText(String.valueOf(finishedSubjects.size()));
                adapter.notifyDataSetChanged();
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.e("ParentTracking", error.getMessage());
            }
        });
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