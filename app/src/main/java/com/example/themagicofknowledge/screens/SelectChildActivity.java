package com.example.themagicofknowledge.screens;

import android.app.AlertDialog;
import android.content.Intent;
import android.os.Bundle;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.themagicofknowledge.R;
import com.example.themagicofknowledge.models.UserChild;
import com.example.themagicofknowledge.models.UserParent;
import com.example.themagicofknowledge.utils.SharedPreferencesUtil;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

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
        DatabaseReference mDatabase = FirebaseDatabase.getInstance().getReference("Users").child(currentParent.getId());
        mDatabase.get().addOnCompleteListener(task -> {
            if (task.isSuccessful() && task.getResult().exists()) {
                currentParent = task.getResult().getValue(UserParent.class);

                // עדכון בזיכרון המקומי
                SharedPreferencesUtil.saveUser(this, currentParent);

                // עדכון הרשימה על המסך
                if (currentParent.getChildrenList() != null) {
                    adapter = new ChildAdapter(currentParent.getChildrenList(), child -> {
                        SharedPreferencesUtil.saveCurrentChild(this, child);
                        startActivity(new Intent(this, Total.class));
                    });
                    rvChildren.setAdapter(adapter);
                }
            }
        });

        FloatingActionButton fabAddChild = findViewById(R.id.fabAddChild);
        fabAddChild.setOnClickListener(v -> showAddChildDialog());
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

                // יצירת ילד חדש והוספה לרשימה
                String childId = String.valueOf(System.currentTimeMillis());
                UserChild newChild = new UserChild(childId, currentParent.getId(), name, age);
                currentParent.getChildrenList().add(newChild);

                // שמירה בזיכרון המקומי
                SharedPreferencesUtil.saveUser(this, currentParent);

                // שמירה בענן
                DatabaseReference mDatabase = FirebaseDatabase.getInstance().getReference("Users");
                mDatabase.child(currentParent.getId())
                        .setValue(currentParent)
                        .addOnSuccessListener(aVoid -> Toast.makeText(this, "הילד נשמר בהצלחה!", Toast.LENGTH_SHORT).show())
                        .addOnFailureListener(e -> Toast.makeText(this, "שגיאה בשמירה: " + e.getMessage(), Toast.LENGTH_SHORT).show());

                // עדכון הרשימה במסך
                adapter.notifyDataSetChanged();

                dialog.dismiss();
            });
        });

        dialog.show();
    }
}
