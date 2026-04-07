package com.example.themagicofknowledge.screens;

import android.app.AlertDialog;
import android.content.Intent;
import android.os.Bundle;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.themagicofknowledge.R;
import com.example.themagicofknowledge.models.UserChild;
import com.example.themagicofknowledge.models.UserParent;
import com.example.themagicofknowledge.services.DatabaseService;
import com.example.themagicofknowledge.utils.SharedPreferencesUtil;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

import java.util.ArrayList;
import java.util.List;
import java.util.function.UnaryOperator;

public class SelectChildActivity extends BaseActivity {

    private RecyclerView rvChildren;
    private ChildAdapter adapter;
    private UserParent currentParent;
    private List<UserChild> childrenList;

    @Override
    protected boolean hasSideMenu() {
        return true;
    }

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

        loadChildrenFromDB();

        FloatingActionButton fabAddChild = findViewById(R.id.fabAddChild);
        fabAddChild.setOnClickListener(v -> showAddChildDialog());
    }

    private void loadChildrenFromDB() {
        DatabaseService.getInstance().getUser(currentParent.getId(), new DatabaseService.DatabaseCallback<UserParent>() {
            @Override
            public void onCompleted(UserParent userParentServer) {
                childrenList.clear();
                childrenList.addAll(userParentServer.childrenList);

                currentParent.setChildrenList(new ArrayList<>(childrenList));
                SharedPreferencesUtil.saveUser(SelectChildActivity.this, currentParent);

                // הודעה לאדפטר שהנתונים השתנו - בלי ליצור אותו מחדש
                adapter.notifyDataSetChanged();
            }

            @Override
            public void onFailed(Exception e) {

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


                String childId = DatabaseService.getInstance().generateChildId(currentParent.id);

                UserChild newChild = new UserChild(childId, currentParent.getId(), name, age);

                DatabaseService.getInstance().updateUser(currentParent.id, new UnaryOperator<UserParent>() {
                    @Override
                    public UserParent apply(UserParent userParentServer) {
                        if (userParentServer != null) {
                            userParentServer.childrenList.add(newChild);
                        }
                        return userParentServer;
                    }
                }, new DatabaseService.DatabaseCallback<UserParent>() {
                    @Override
                    public void onCompleted(UserParent userParentServer) {
                        Toast.makeText(SelectChildActivity.this, "הקוסם הקטן נוסף!", Toast.LENGTH_SHORT).show();
                        dialog.dismiss();
                        childrenList.clear();
                        childrenList.addAll(userParentServer.childrenList);
                        adapter.notifyDataSetChanged();
                    }

                    @Override
                    public void onFailed(Exception e) {
                        tvError.setText("שגיאה בשמירה: " + e.getMessage());
                    }
                });
            });
        });

        dialog.show();
    }
}
