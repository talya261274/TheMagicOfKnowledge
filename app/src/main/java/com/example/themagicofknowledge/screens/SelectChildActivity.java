package com.example.themagicofknowledge.screens;

import android.app.AlertDialog;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.themagicofknowledge.R;
import com.example.themagicofknowledge.models.UserChild;
import com.example.themagicofknowledge.models.UserParent;
import com.example.themagicofknowledge.utils.SharedPreferencesUtil;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.List;

public class SelectChildActivity extends AppCompatActivity {

    private RecyclerView rvChildren;
    private ChildAdapter adapter;
    private UserParent currentParent;
    private List<UserChild> childrenList; // רשימה שתנהל את הילדים במסך

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_select_child);

        rvChildren = findViewById(R.id.rvChildren);
        currentParent = SharedPreferencesUtil.getUser(this);

        // אתחול הרשימה
        childrenList = new ArrayList<>();
        if (currentParent.getChildrenList() != null) {
            childrenList.addAll(currentParent.getChildrenList());
        }

        // הגדרת RecyclerView פעם אחת בלבד
        rvChildren.setLayoutManager(new LinearLayoutManager(this));
        adapter = new ChildAdapter(childrenList, child -> {
            SharedPreferencesUtil.saveCurrentChild(this, child);
            // שימי לב: שיניתי ל-Total.class כפי שמופיע אצלך, וודאי שזה ה-Activity הנכון
            startActivity(new Intent(this, Total.class));
        });
        rvChildren.setAdapter(adapter);

        loadChildrenFromFirebase();

        FloatingActionButton fabAddChild = findViewById(R.id.fabAddChild);
        fabAddChild.setOnClickListener(v -> showAddChildDialog());
    }

    private void loadChildrenFromFirebase() {
        DatabaseReference mDatabase = FirebaseDatabase.getInstance()
                .getReference("users")
                .child(currentParent.getId());

        mDatabase.addValueEventListener(new ValueEventListener() { // שימוש ב-addValueEventListener לעדכון חי
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (snapshot.exists()) {
                    // טעינת רשימת הילדים בזהירות כדי למנוע את שגיאת ה-List/HashMap
                    DataSnapshot childrenSnapshot = snapshot.child("childrenList");
                    childrenList.clear(); // מנקים את הרשימה הנוכחית לפני טעינה

                    for (DataSnapshot childData : childrenSnapshot.getChildren()) {
                        UserChild child = childData.getValue(UserChild.class);
                        if (child != null) {
                            childrenList.add(child);
                        }
                    }

                    // עדכון המודל המקומי וה-SharedPreferences
                    currentParent.setChildrenList(new ArrayList<>(childrenList));
                    SharedPreferencesUtil.saveUser(SelectChildActivity.this, currentParent);

                    // הודעה לאדפטר שהנתונים השתנו - בלי ליצור אותו מחדש
                    adapter.notifyDataSetChanged();
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(SelectChildActivity.this, "שגיאה: " + error.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void showAddChildDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("הוספת ילד חדש");

        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(50, 20, 50, 10);

        final EditText etName = new EditText(this);
        etName.setHint("שם הילד");
        layout.addView(etName);

        final EditText etAge = new EditText(this);
        etAge.setHint("גיל (3–8)");
        etAge.setInputType(android.text.InputType.TYPE_CLASS_NUMBER);
        layout.addView(etAge);

        final TextView tvError = new TextView(this);
        tvError.setTextColor(getResources().getColor(android.R.color.holo_red_dark));
        layout.addView(tvError);

        builder.setView(layout);
        builder.setPositiveButton("הוסף", null);

        AlertDialog dialog = builder.create();
        dialog.setOnShowListener(dialogInterface -> {
            dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v -> {

                String name = etName.getText().toString().trim();
                String ageStr = etAge.getText().toString().trim();

                if (name.isEmpty() || ageStr.isEmpty()) {
                    tvError.setText("נא למלא את כל השדות");
                    return;
                }

                int age = Integer.parseInt(ageStr);
                if (age < 3 || age > 8) {
                    tvError.setText("הגיל חייב להיות בין 3 ל-8");
                    return;
                }

                DatabaseReference childrenRef = FirebaseDatabase.getInstance()
                        .getReference("users")
                        .child(currentParent.getId())
                        .child("childrenList");

                String childId = childrenRef.push().getKey();

                UserChild newChild = new UserChild(childId, currentParent.getId(), name, age);

                // 🔥 ה-LOG החשוב
                Log.d("FIREBASE_DEBUG", "Saving child: " + newChild.toString());
                Log.d("FIREBASE_DEBUG", "Path: users/" + currentParent.getId() + "/childrenList/" + childId);
                Log.d("FIREBASE_DEBUG", FirebaseDatabase.getInstance().getReference().toString());

                if (childId != null) {
                    childrenRef.child(childId).setValue(newChild)
                            .addOnSuccessListener(aVoid -> {
                                Log.d("FIREBASE_DEBUG", "Child saved successfully!");
                                Toast.makeText(SelectChildActivity.this, "הקוסם הקטן נוסף!", Toast.LENGTH_SHORT).show();
                                dialog.dismiss();
                            })
                            .addOnFailureListener(e -> {
                                Log.e("FIREBASE_DEBUG", "Save failed: " + e.getMessage());
                                tvError.setText("שגיאה בשמירה: " + e.getMessage());
                            });
                }
            });
        });

        dialog.show();
    }
}
