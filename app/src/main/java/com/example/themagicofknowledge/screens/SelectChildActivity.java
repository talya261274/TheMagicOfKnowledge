package com.example.themagicofknowledge.screens;

import android.app.AlertDialog;
import android.content.Intent;
import android.os.Bundle;
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

public class SelectChildActivity extends AppCompatActivity {

    private RecyclerView rvChildren;
    private ChildAdapter adapter;
    private UserParent currentParent;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_select_child);

        rvChildren = findViewById(R.id.rvChildren);

        // שליפת ההורה מהזיכרון
        currentParent = SharedPreferencesUtil.getUser(this);

        // אם הרשימה ריקה, יוצרים רשימה ריקה
        if (currentParent.getChildrenList() == null) {
            currentParent.setChildrenList(new ArrayList<>());
        }

        // הגדרת RecyclerView
        rvChildren.setLayoutManager(new LinearLayoutManager(this));
        adapter = new ChildAdapter(currentParent.getChildrenList(), child -> {
            SharedPreferencesUtil.saveCurrentChild(this, child);
            startActivity(new Intent(this, Total.class));
        });
        rvChildren.setAdapter(adapter);

        // משיכת נתונים מעודכנים מהענן
        loadChildrenFromFirebase();

        FloatingActionButton fabAddChild = findViewById(R.id.fabAddChild);
        fabAddChild.setOnClickListener(v -> showAddChildDialog());
    }

    private void loadChildrenFromFirebase() {
        DatabaseReference mDatabase = FirebaseDatabase.getInstance()
                .getReference("Users")
                .child(currentParent.getId());

        mDatabase.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (snapshot.exists()) {
                    // טעינת פרטי ההורה
                    UserParent updatedParent = snapshot.getValue(UserParent.class);
                    if (updatedParent != null) {
                        currentParent.setFirstName(updatedParent.getFirstName());
                        currentParent.setLastName(updatedParent.getLastName());
                        currentParent.setEmail(updatedParent.getEmail());
                        currentParent.setPhone(updatedParent.getPhone());
                        currentParent.setBirthDate(updatedParent.getBirthDate());
                    }

                    // טעינת רשימת הילדים בנפרד
                    DataSnapshot childrenSnapshot = snapshot.child("childrenList");
                    if (childrenSnapshot.exists()) {
                        ArrayList<UserChild> children = new ArrayList<>();
                        for (DataSnapshot childSnapshot : childrenSnapshot.getChildren()) {
                            UserChild child = childSnapshot.getValue(UserChild.class);
                            if (child != null) {
                                children.add(child);
                            }
                        }
                        currentParent.setChildrenList(children);
                    } else {
                        currentParent.setChildrenList(new ArrayList<>());
                    }

                    // עדכון בזיכרון המקומי
                    SharedPreferencesUtil.saveUser(SelectChildActivity.this, currentParent);

                    // עדכון הרשימה על המסך
                    if (currentParent.getChildrenList() != null && !currentParent.getChildrenList().isEmpty()) {
                        adapter = new ChildAdapter(currentParent.getChildrenList(), child -> {
                            SharedPreferencesUtil.saveCurrentChild(SelectChildActivity.this, child);
                            startActivity(new Intent(SelectChildActivity.this, Total.class));
                        });
                        rvChildren.setAdapter(adapter);
                    }
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(SelectChildActivity.this,
                        "שגיאה בטעינת נתונים: " + error.getMessage(),
                        Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void showAddChildDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("הוספת ילד חדש");

        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(40, 20, 40, 20);

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
        builder.setPositiveButton("הוסף", null); // נבטל את ההגדרה הראשונית

        AlertDialog dialog = builder.create();
        dialog.setOnShowListener(dialogInterface -> {
            dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v -> {
                String name = etName.getText().toString().trim();
                String ageStr = etAge.getText().toString().trim();

                // בדיקות קלט
                if (name.isEmpty()) {
                    tvError.setText("אנא הזן שם לילד");
                    return;
                }
                if (ageStr.isEmpty()) {
                    tvError.setText("אנא הזן גיל לילד");
                    return;
                }

                int age;
                try {
                    age = Integer.parseInt(ageStr);
                } catch (NumberFormatException e) {
                    tvError.setText("גיל חייב להיות מספר");
                    return;
                }

                if (age < 3 || age > 8) {
                    tvError.setText("גיל הילד חייב להיות בין 3 ל-8");
                    return;
                }

                // יצירת רשימת ילדים אם היא null
                if (currentParent.getChildrenList() == null) {
                    currentParent.setChildrenList(new ArrayList<>());
                }

                // יצירת ילד חדש עם ID ייחודי
                String childId = String.valueOf(System.currentTimeMillis());
                UserChild newChild = new UserChild(childId, currentParent.getId(), name, age);

                // הוספה לרשימת הילדים
                currentParent.getChildrenList().add(newChild);

                // שמירה בענן - בשיטה המתוקנת
                DatabaseReference mDatabase = FirebaseDatabase.getInstance()
                        .getReference("Users")
                        .child(currentParent.getId())
                        .child("childrenList");

                // שמירת הילד החדש ברשימה
                mDatabase.child(childId).setValue(newChild.toMap())
                        .addOnSuccessListener(aVoid -> {
                            // עדכון גם את האובייקט הראשי
                            SharedPreferencesUtil.saveUser(SelectChildActivity.this, currentParent);

                            Toast.makeText(SelectChildActivity.this, "הילד נשמר בהצלחה!", Toast.LENGTH_SHORT).show();
                            adapter.notifyDataSetChanged(); // עדכון הרשימה
                            dialog.dismiss(); // סגירת הדיאלוג
                        })
                        .addOnFailureListener(e -> {
                            tvError.setText("שגיאה בשמירה בענן: " + e.getMessage());
                            // הסרת הילד מהרשימה המקומית אם השמירה נכשלה
                            currentParent.getChildrenList().remove(newChild);
                        });
            });
        });

        dialog.show();
    }
}
