package com.example.themagicofknowledge.screens;

import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Spinner;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.themagicofknowledge.R;
import com.example.themagicofknowledge.adapter.SubjectProgressAdapter;
import com.example.themagicofknowledge.models.SubjectStat;
import com.example.themagicofknowledge.models.UserChild;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.List;

public class ParentTrackingActivity extends AppCompatActivity {

    private Spinner spinnerChildren;
    private RecyclerView rvSubjects;
    private TextView tvCompletedCount;
    private List<UserChild> childrenList = new ArrayList<>();
    private List<SubjectStat> subjectsStats = new ArrayList<>();
    private SubjectProgressAdapter adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_parent_tracking);
        String childId = getIntent().getStringExtra("CHILD_ID");

        if (childId == null) {
            Log.e("Error", "childId is missing!");
            finish();
            return;
        }

        spinnerChildren = findViewById(R.id.spinnerChildren);
        rvSubjects = findViewById(R.id.rvSubjectsProgress);
        tvCompletedCount = findViewById(R.id.tvCompletedCount);

        rvSubjects.setLayoutManager(new LinearLayoutManager(this));
        adapter = new SubjectProgressAdapter(subjectsStats);
        rvSubjects.setAdapter(adapter);

        loadChildren();

        spinnerChildren.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                if (childrenList != null && !childrenList.isEmpty()) {
                    loadStatsForChild(childrenList.get(position).getId());
                }
            }
            @Override
            public void onNothingSelected(AdapterView<?> parent) {}
        });
    }

    private void loadChildren() {
        String parentId = FirebaseAuth.getInstance().getUid();

        if (parentId == null) {
            Log.e("ParentTracking", "User is not logged in!");
            return;
        }

        DatabaseReference ref = FirebaseDatabase.getInstance().getReference("Parents")
                .child(parentId).child("children");

        ref.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {

                subjectsStats.clear();
                if (!snapshot.exists()) {
                    // הילד עדיין לא שיחק באף משחק
                    tvCompletedCount.setText("0");
                    adapter.notifyDataSetChanged();
                    return;
                }

                childrenList.clear();
                List<String> names = new ArrayList<>();
                for (DataSnapshot ds : snapshot.getChildren()) {
                    UserChild child = ds.getValue(UserChild.class);
                    if (child != null) {
                        childrenList.add(child);
                        names.add(child.getName());
                    }
                }
                ArrayAdapter<String> spinnerAdapter = new ArrayAdapter<>(ParentTrackingActivity.this,
                        android.R.layout.simple_spinner_dropdown_item, names);
                spinnerChildren.setAdapter(spinnerAdapter);
            }
            @Override
            public void onCancelled(@NonNull DatabaseError error) {}
        });
    }

    private void loadStatsForChild(String childId) {
        String parentId = FirebaseAuth.getInstance().getUid();

        if (parentId == null || childId == null) {
            Log.e("ParentTracking", "parentId or childId is null");
            return;
        }

        DatabaseReference ref = FirebaseDatabase.getInstance().getReference("Parents")
                .child(parentId).child("children").child(childId).child("stats");

        ref.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                subjectsStats.clear();
                for (DataSnapshot ds : snapshot.getChildren()) {
                    Integer attemptsObj = ds.child("attempts").getValue(Integer.class);
                    int attempts = (attemptsObj != null) ? attemptsObj : 0;

                    Long timeObj = ds.child("timeInSeconds").getValue(Long.class);
                    long time = (timeObj != null) ? timeObj : 0L;
                    subjectsStats.add(new SubjectStat(ds.getKey(), attempts, time));
                }
                tvCompletedCount.setText(String.valueOf(subjectsStats.size()));
                adapter.notifyDataSetChanged();
            }
            @Override
            public void onCancelled(@NonNull DatabaseError error) {}
        });
    }
}