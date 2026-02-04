package com.example.themagicofknowledge.screens;

import android.app.AlertDialog;
import android.content.Intent;
import android.os.Bundle;
import android.widget.EditText;
import android.widget.LinearLayout;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.example.themagicofknowledge.R;
import com.example.themagicofknowledge.models.UserChild;
import com.example.themagicofknowledge.models.UserParent;
import com.example.themagicofknowledge.utils.SharedPreferencesUtil;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
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

        // שליפת ההורה מהזכרון
        currentParent = SharedPreferencesUtil.getUser(this);

        // אם אין רשימה, ניצור אחת ריקה כדי שלא יקרוס
        if (currentParent.childrenList == null) {
            currentParent.childrenList = new ArrayList<>();
        }

        currentParent.getChildrenList().add(new UserChild("1", currentParent.getId(), "יוסי הקטן", 4));

        // הגדרת הרשימה
        rvChildren.setLayoutManager(new LinearLayoutManager(this));
        adapter = new ChildAdapter(currentParent.childrenList, child -> {
            // כאן יקרה המעבר למשחק
            SharedPreferencesUtil.saveCurrentChild(this, child);
            startActivity(new Intent(this, Total.class));
        });
        rvChildren.setAdapter(adapter);

        FloatingActionButton fabAddChild = findViewById(R.id.fabAddChild);
        fabAddChild.setOnClickListener(v -> showAddChildDialog());
    }

    private void showAddChildDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("הוספת ילד חדש");

        // יצירת עיצוב פשוט לחלון דרך הקוד
        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(40, 20, 40, 20);

        final EditText etName = new EditText(this);
        etName.setHint("שם הילד");
        layout.addView(etName);

        final EditText etAge = new EditText(this);
        etAge.setHint("גיל");
        etAge.setInputType(android.text.InputType.TYPE_CLASS_NUMBER);
        layout.addView(etAge);

        builder.setView(layout);

        builder.setPositiveButton("הוסף", (dialog, which) -> {
            String name = etName.getText().toString();
            String ageStr = etAge.getText().toString();

            if (!name.isEmpty() && !ageStr.isEmpty()) {
                int age = Integer.parseInt(ageStr);

                // יצירת אובייקט ילד חדש
                String childId = String.valueOf(System.currentTimeMillis()); // ID זמני
                UserChild newChild = new UserChild(childId, currentParent.getId(), name, age);

                // הוספה לרשימה ועדכון
                currentParent.getChildrenList().add(newChild);

                // חשוב: שמירת ההורה המעודכן בזיכרון!
                SharedPreferencesUtil.saveUser(this, currentParent);

                // עדכון הרשימה במסך
                adapter.notifyDataSetChanged();
            }
        });

        builder.setNegativeButton("ביטול", null);
        builder.show();
    }

}