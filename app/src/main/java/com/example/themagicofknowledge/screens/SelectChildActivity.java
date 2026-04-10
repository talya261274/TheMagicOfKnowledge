package com.example.themagicofknowledge.screens;

import android.app.AlertDialog;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.recyclerview.widget.GridLayoutManager;
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
            childrenList.addAll(currentParent.getChildrenListAsList());
        }

        // הגדרת RecyclerView פעם אחת בלבד
        rvChildren.setLayoutManager(new GridLayoutManager(this, 2));
        adapter = new ChildAdapter(childrenList,
                child -> {
                    // 1. שמירת הילד הנבחר בזיכרון המקומי
                    SharedPreferencesUtil.saveCurrentChild(this, child);

                    // 2. מעבר למסך הראשי (MainActivity)
                    Intent intent = new Intent(this, MainActivity.class);
                    // השורה הזו מוודא שלא יהיה אפשר לחזור אחורה לבחירת ילד עם כפתור ה-Back
                    intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                    startActivity(intent);
                },
                child -> {
                    showDeleteConfirmationDialog(child);
                }
        );
        rvChildren.setAdapter(adapter);

        loadChildrenFromDB();

        FloatingActionButton fabAddChild = findViewById(R.id.fabAddChild);
        fabAddChild.setOnClickListener(v -> showAddChildDialog());
    }

    private void loadChildrenFromDB() {
        DatabaseService.getInstance().getUser(currentParent.getId(), new DatabaseService.DatabaseCallback<UserParent>() {
            @Override
            public void onCompleted(UserParent userParentServer) {
                if (userParentServer != null) {
                    childrenList.clear();
                    // הופכים את המפה לרשימה עבור האדפטר
                    childrenList.addAll(userParentServer.getChildrenListAsList());

                    // עדכון האובייקט המקומי וה-SharedPreferences
                    currentParent.setChildrenList(userParentServer.getChildrenList());
                    SharedPreferencesUtil.saveUser(SelectChildActivity.this, currentParent);

                    adapter.notifyDataSetChanged();
                }
            }

            @Override
            public void onFailed(Exception e) {
                Toast.makeText(SelectChildActivity.this, "שגיאה בטעינה", Toast.LENGTH_SHORT).show();
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
                            // ב-Map משתמשים ב-put. המפתח הוא ה-ID והערך הוא אובייקט הילד
                            userParentServer.getChildrenList().put(newChild.getId(), newChild);
                        }
                        return userParentServer;
                    }
                }, new DatabaseService.DatabaseCallback<UserParent>() {
                    @Override
                    public void onCompleted(UserParent userParentServer) {
                        //  סגירת הדיאלוג של ההורה
                        dialog.dismiss();

                        //  עדכון המשתמש המקומי (חשוב כדי שה-ID יהיה מסונכרן)
                        SharedPreferencesUtil.saveUser(SelectChildActivity.this, userParentServer);

                        //  מעבר למסך בחירת האוואטר עבור הילד החדש
                        Intent intent = new Intent(SelectChildActivity.this, AvatarSelectionActivity.class);
                        intent.putExtra("childId", newChild.getId()); // אנחנו שולחים את ה-ID של הילד החדש שיצרנו הרגע
                        startActivity(intent);

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
    private void deleteChild(UserChild child) {
        new AlertDialog.Builder(this)
                .setTitle("מחיקת ילד/ה")
                .setMessage("האם את בטוחה שברצונך למחוק את " + child.getName() + "? כל ההתקדמות שלו תימחק.")
                .setPositiveButton("מחק", (dialog, which) -> {
                    DatabaseService.getInstance().deleteChild(currentParent.getId(), child.getId(), new DatabaseService.DatabaseCallback<Void>() {
                        @Override
                        public void onCompleted(Void object) {
                            Toast.makeText(SelectChildActivity.this, "הילד/ה נמחק/ה בהצלחה", Toast.LENGTH_SHORT).show();
                            // עדכון הרשימה המקומית
                            loadChildrenFromDB();
                        }

                        @Override
                        public void onFailed(Exception e) {
                            Toast.makeText(SelectChildActivity.this, "שגיאה במחיקה: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                        }
                    });
                })
                .setNegativeButton("ביטול", null)
                .show();
    }

    private void showDeleteConfirmationDialog(UserChild child) {

        Log.d("DeleteCheck", "Child Name: " + child.getName() + ", Child ID: " + child.getId());

        new AlertDialog.Builder(this)
                .setTitle("מחיקת ילד")
                .setMessage("האם את בטוחה שברצונך למחוק את " + child.getName() + "?")
                .setPositiveButton("מחק", (dialog, which) -> {
                    String pId = currentParent.getId();
                    String cId = child.getId();

                    DatabaseService.getInstance().deleteChild(pId, cId, new DatabaseService.DatabaseCallback<Void>() {
                        @Override
                        public void onCompleted(Void object) {
                            Toast.makeText(SelectChildActivity.this, "הילד נמחק", Toast.LENGTH_SHORT).show();
                            loadChildrenFromDB(); // רענון הרשימה מהשרת
                        }

                        @Override
                        public void onFailed(Exception e) {
                            Toast.makeText(SelectChildActivity.this, "שגיאה במחיקה", Toast.LENGTH_SHORT).show();
                        }
                    });
                })
                .setNegativeButton("ביטול", null)
                .show();
    }
}
