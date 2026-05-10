package com.example.themagicofknowledge.screens;

import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.themagicofknowledge.R;
import com.example.themagicofknowledge.adapter.ChildAdapter;
import com.example.themagicofknowledge.models.UserChild;
import com.example.themagicofknowledge.models.UserParent;
import com.example.themagicofknowledge.services.DatabaseService;
import com.example.themagicofknowledge.utils.SharedPreferencesUtil;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

import java.util.ArrayList;
import java.util.List;

public class SelectChildActivity extends BaseActivity {

    private static final String TAG = "SelectChildActivity";

    // ===== רכיבי UI =====
    private RecyclerView rvChildren;
    private LinearLayout emptyStateLayout;
    private FloatingActionButton fabAddChild;
    private MaterialButton btnAddFirstChild;

    // ===== נתונים =====
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

        // ===== הגנה - בדיקת משתמש מחובר =====
        currentParent = SharedPreferencesUtil.getUser(this);
        if (currentParent == null) {
            Log.e(TAG, "No user logged in. Finishing activity.");
            Toast.makeText(this, "שגיאה: אין משתמש מחובר", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        initViews();
        setupRecyclerView();
        setupButtons();
        loadChildrenFromDB();
    }


    // ===== חיבור רכיבי UI =====
    private void initViews() {
        rvChildren = findViewById(R.id.rvChildren);
        emptyStateLayout = findViewById(R.id.emptyStateLayout);
        fabAddChild = findViewById(R.id.fabAddChild);
        btnAddFirstChild = findViewById(R.id.btnAddFirstChild);
    }


    // ===== הגדרת ה-RecyclerView =====
    private void setupRecyclerView() {
        childrenList = new ArrayList<>();

        // טעינה ראשונית מ-SharedPreferences (כדי שיופיע מהר)
        if (currentParent.getChildrenList() != null) {
            childrenList.addAll(currentParent.getChildrenListAsList());
        }

        // הגדרת רשת של 2 עמודות
        rvChildren.setLayoutManager(new GridLayoutManager(this, 2));

        // יצירת ה-Adapter עם 2 callbacks: לחיצה ומחיקה
        adapter = new ChildAdapter(
                childrenList,
                this::onChildSelected,           // לחיצה רגילה
                this::showDeleteConfirmationDialog  // לחיצה ארוכה למחיקה
        );
        rvChildren.setAdapter(adapter);

        // עדכון UI לפי המצב הראשוני
        updateEmptyState();
    }


    // ===== הגדרת כפתורי הוספה =====
    private void setupButtons() {
        // שני הכפתורים מובילים לאותה פעולה - הוספת ילד
        View.OnClickListener addChildListener = v -> showAddChildDialog();

        fabAddChild.setOnClickListener(addChildListener);
        btnAddFirstChild.setOnClickListener(addChildListener);
    }


    // ===== החלפה בין מצב ריק למצב עם ילדים =====
    private void updateEmptyState() {
        if (childrenList.isEmpty()) {
            // אין ילדים - מציגים מצב ריק
            emptyStateLayout.setVisibility(View.VISIBLE);
            rvChildren.setVisibility(View.GONE);
            fabAddChild.setVisibility(View.GONE);
        } else {
            // יש ילדים - מציגים את הרשימה
            emptyStateLayout.setVisibility(View.GONE);
            rvChildren.setVisibility(View.VISIBLE);
            fabAddChild.setVisibility(View.VISIBLE);
        }
    }


    // ===== טעינת ילדים מ-Firebase =====
    private void loadChildrenFromDB() {
        DatabaseService.getInstance().getUser(currentParent.getId(),
                new DatabaseService.DatabaseCallback<UserParent>() {
                    @Override
                    public void onCompleted(UserParent userFromServer) {
                        if (userFromServer == null) {
                            Log.w(TAG, "User not found in DB");
                            updateEmptyState();
                            return;
                        }

                        // עדכון הרשימה המקומית
                        childrenList.clear();
                        childrenList.addAll(userFromServer.getChildrenListAsList());

                        // עדכון האובייקט המקומי וה-SharedPreferences
                        currentParent.setChildrenList(userFromServer.getChildrenList());
                        SharedPreferencesUtil.saveUser(SelectChildActivity.this, currentParent);

                        // עדכון התצוגה
                        adapter.notifyDataSetChanged();
                        updateEmptyState();
                    }

                    @Override
                    public void onFailed(Exception e) {
                        Log.e(TAG, "Failed to load children", e);
                        Toast.makeText(SelectChildActivity.this, "שגיאה בטעינה", Toast.LENGTH_SHORT).show();
                        updateEmptyState();
                    }
                });
    }


    // ===== טיפול בלחיצה על ילד =====
    private void onChildSelected(UserChild child) {
        // שמירת הילד הפעיל
        SharedPreferencesUtil.saveCurrentChild(this, child);
        SharedPreferencesUtil.saveCurrentChildId(this, child.getId());

        // מעבר למסך הראשי
        Intent intent = new Intent(this, MainActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
    }


    // ===== דיאלוג הוספת ילד חדש =====
    private void showAddChildDialog() {
        final android.app.Dialog dialog = new android.app.Dialog(this);
        dialog.setContentView(R.layout.dialog_add_child_custom);

        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
        }

        // חיבור רכיבי הדיאלוג
        EditText etName = dialog.findViewById(R.id.etDialogChildName);
        EditText etAge = dialog.findViewById(R.id.etDialogChildAge);
        TextView tvError = dialog.findViewById(R.id.tvDialogError);
        Button btnAdd = dialog.findViewById(R.id.btnDialogAdd);

        btnAdd.setOnClickListener(v -> handleAddChild(dialog, etName, etAge, tvError));

        dialog.show();
    }


    // ===== טיפול בהוספת ילד =====
    private void handleAddChild(android.app.Dialog dialog, EditText etName,
                                EditText etAge, TextView tvError) {
        String name = etName.getText().toString().trim();
        String ageStr = etAge.getText().toString().trim();

        // וולידציה - שדות ריקים
        if (name.isEmpty() || ageStr.isEmpty()) {
            tvError.setText("נא למלא את כל השדות");
            tvError.setVisibility(View.VISIBLE);
            return;
        }

        // וולידציה - גיל תקין
        int age;
        try {
            age = Integer.parseInt(ageStr);
        } catch (NumberFormatException e) {
            tvError.setText("גיל לא תקין");
            tvError.setVisibility(View.VISIBLE);
            return;
        }

        if (age < 3 || age > 8) {
            tvError.setText("הגיל חייב להיות בין 3 ל-8");
            tvError.setVisibility(View.VISIBLE);
            return;
        }

        // יצירת אובייקט ילד חדש
        String childId = DatabaseService.getInstance().generateChildId(currentParent.getId());
        UserChild newChild = new UserChild(childId, currentParent.getId(), name, age);

        // שמירה ב-Firebase
        saveNewChildToFirebase(newChild, dialog, tvError);
    }


    // ===== שמירת הילד החדש ב-Firebase =====
    private void saveNewChildToFirebase(UserChild newChild, android.app.Dialog dialog,
                                        TextView tvError) {
        DatabaseService.getInstance().updateUser(
                currentParent.getId(),
                userFromServer -> {
                    if (userFromServer != null) {
                        userFromServer.getChildrenList().put(newChild.getId(), newChild);
                    }
                    return userFromServer;
                },
                new DatabaseService.DatabaseCallback<UserParent>() {
                    @Override
                    public void onCompleted(UserParent userFromServer) {
                        dialog.dismiss();

                        // עדכון SharedPreferences
                        SharedPreferencesUtil.saveUser(SelectChildActivity.this, userFromServer);

                        // מעבר למסך בחירת אווטאר
                        Intent intent = new Intent(SelectChildActivity.this, AvatarSelectionActivity.class);
                        intent.putExtra("childId", newChild.getId());
                        startActivity(intent);
                    }

                    @Override
                    public void onFailed(Exception e) {
                        Log.e(TAG, "Failed to save new child", e);
                        tvError.setText("שגיאה בשמירה: " + e.getMessage());
                        tvError.setVisibility(View.VISIBLE);
                    }
                }
        );
    }


    // ===== דיאלוג אישור מחיקה =====
    private void showDeleteConfirmationDialog(UserChild child) {
        showCustomDialog(
                "מחיקת קוסם",
                "האם את בטוחה שברצונך למחוק את " + child.getName() + "? כל ההתקדמות תימחק.",
                "מחק לצמיתות",
                Color.parseColor("#FF5252"),  // אדום להתראה
                () -> deleteChildFromDB(child)
        );
    }


    // ===== מחיקת ילד מ-Firebase =====
    private void deleteChildFromDB(UserChild child) {
        DatabaseService.getInstance().deleteChild(
                currentParent.getId(),
                child.getId(),
                new DatabaseService.DatabaseCallback<Void>() {
                    @Override
                    public void onCompleted(Void object) {
                        Toast.makeText(SelectChildActivity.this,
                                "הילד/ה נמחק/ה בהצלחה", Toast.LENGTH_SHORT).show();
                        loadChildrenFromDB();
                    }

                    @Override
                    public void onFailed(Exception e) {
                        Log.e(TAG, "Failed to delete child", e);
                        Toast.makeText(SelectChildActivity.this,
                                "שגיאה במחיקה", Toast.LENGTH_SHORT).show();
                    }
                }
        );
    }


    // ===== רענון הרשימה כשחוזרים למסך =====
    @Override
    protected void onResume() {
        super.onResume();
        // טעינה מחדש כשחוזרים מ-AvatarSelectionActivity
        if (currentParent != null) {
            loadChildrenFromDB();
        }
    }
}