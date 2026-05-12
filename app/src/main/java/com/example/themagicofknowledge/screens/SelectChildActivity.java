package com.example.themagicofknowledge.screens;

import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
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

        currentParent = SharedPreferencesUtil.getUser(this);

        // הגנה: אם אין הורה מחובר, חזרה למסך הכניסה
        if (currentParent == null) {
            Intent intent = new Intent(this, LandingActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
            finish();
            return;
        }

        rvChildren = findViewById(R.id.rvChildren);
        childrenList = new ArrayList<>();
        
        if (currentParent.getChildrenList() != null) {
            childrenList.addAll(currentParent.getChildrenListAsList());
            childrenList.sort((a, b) -> Integer.compare(a.getAge(), b.getAge()));
        }

        updateLayoutManager();

        adapter = new ChildAdapter(childrenList,
                child -> handleChildClick(child),
                child -> showDeleteConfirmationDialog(child)
        );
        rvChildren.setAdapter(adapter);
        rvChildren.setLayoutDirection(View.LAYOUT_DIRECTION_RTL);

        loadChildrenFromDB();
        updateEmptyState();

        findViewById(R.id.btnAddFirstChild).setOnClickListener(v -> showAddChildDialog());
        ((FloatingActionButton) findViewById(R.id.fabAddChild)).setOnClickListener(v -> showAddChildDialog());
    }

    private void updateLayoutManager() {
        if (childrenList.size() == 1) {
            GridLayoutManager lm = new GridLayoutManager(this, 2);
            lm.setSpanSizeLookup(new GridLayoutManager.SpanSizeLookup() {
                @Override
                public int getSpanSize(int position) {
                    return 2; // ילד יחיד - תופס שתי עמודות = מרכז
                }
            });
            rvChildren.setLayoutManager(lm);
        } else {
            rvChildren.setLayoutManager(new GridLayoutManager(this, 2));
        }
    }

    /**
     * ⭐⭐⭐ פונקציה חדשה - טיפול בלחיצה על ילד קיים ⭐⭐⭐
     * בודקת אם הילד כבר עשה מבדק. אם כן - ישר ל-Main, אם לא - ל-PlacementTest.
     */
    private void handleChildClick(UserChild child) {
        SharedPreferencesUtil.saveCurrentChild(this, child);
        SharedPreferencesUtil.saveCurrentChildId(this, child.getId());

        if (child.getLastPlacementScore() != null) {
            // ✅ עשה מבדק - ישר ל-MainActivity
            Intent intent = new Intent(this, MainActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
        } else {
            showPlacementTestIntroDialog(child); // ← העבר את child
        }
    }

    private void loadChildrenFromDB() {
        DatabaseService.getInstance().getUser(currentParent.getId(), new DatabaseService.DatabaseCallback<UserParent>() {
            @Override
            public void onCompleted(UserParent userParentServer) {
                if (userParentServer != null) {
                    childrenList.clear();
                    childrenList.addAll(userParentServer.getChildrenListAsList());
                    childrenList.sort((a, b) -> Integer.compare(a.getAge(), b.getAge()));
                    currentParent.setChildrenList(userParentServer.getChildrenList());
                    SharedPreferencesUtil.saveUser(SelectChildActivity.this, currentParent);
                    updateLayoutManager(); // ← עדכן layout לפי מספר הילדים
                    adapter.notifyDataSetChanged();
                }
                updateEmptyState();
            }

            @Override
            public void onFailed(Exception e) {
                Toast.makeText(SelectChildActivity.this, "שגיאה בטעינה", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void showAddChildDialog() {
        final android.app.Dialog dialog = new android.app.Dialog(this);
        dialog.setContentView(R.layout.dialog_add_child_custom);

        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
        }

        EditText etName = dialog.findViewById(R.id.etDialogChildName);
        EditText etAge = dialog.findViewById(R.id.etDialogChildAge);
        TextView tvError = dialog.findViewById(R.id.tvDialogError);
        Button btnAdd = dialog.findViewById(R.id.btnDialogAdd);

        btnAdd.setOnClickListener(v -> {
            String name = etName.getText().toString().trim();
            String ageStr = etAge.getText().toString().trim();

            // וולידציה
            if (name.isEmpty() || ageStr.isEmpty()) {
                tvError.setText("נא למלא את כל השדות");
                tvError.setVisibility(View.VISIBLE);
                return;
            }

            int age;
            try {
                age = Integer.parseInt(ageStr);
            } catch (NumberFormatException e) {
                tvError.setText("הגיל חייב להיות מספר");
                tvError.setVisibility(View.VISIBLE);
                return;
            }

            if (age < 3 || age > 8) {
                tvError.setText("הגיל חייב להיות בין 3 ל-8");
                tvError.setVisibility(View.VISIBLE);
                return;
            }

            // יצירת ילד חדש
            String childId = DatabaseService.getInstance().generateChildId(currentParent.id);
            UserChild newChild = new UserChild(childId, currentParent.getId(), name, age);

            DatabaseService.getInstance().updateUser(currentParent.id, userParentServer -> {
                if (userParentServer != null) {
                    userParentServer.getChildrenList().put(newChild.getId(), newChild);
                }
                return userParentServer;
            }, new DatabaseService.DatabaseCallback<UserParent>() {
                @Override
                public void onCompleted(UserParent userParentServer) {
                    dialog.dismiss();

                    SharedPreferencesUtil.saveUser(SelectChildActivity.this, userParentServer);

                    // מעבר למסך בחירת האוואטר
                    Intent intent = new Intent(SelectChildActivity.this, AvatarSelectionActivity.class);
                    intent.putExtra("childId", newChild.getId());
                    startActivity(intent);
                }

                @Override
                public void onFailed(Exception e) {
                    tvError.setText("שגיאה בשמירה: " + e.getMessage());
                    tvError.setVisibility(View.VISIBLE);
                }
            });
        });

        dialog.show();
    }

    private void updateEmptyState() {
        View emptyStateLayout = findViewById(R.id.emptyStateLayout);
        View childrenCard = findViewById(R.id.childrenCard);

        if (childrenList.isEmpty()) {
            emptyStateLayout.setVisibility(View.VISIBLE);
            childrenCard.setVisibility(View.GONE);
        } else {
            emptyStateLayout.setVisibility(View.GONE);
            childrenCard.setVisibility(View.VISIBLE);
        }
    }

    private void showPlacementTestIntroDialog(UserChild child) {
        SharedPreferences prefs = getSharedPreferences("placement_prefs", MODE_PRIVATE);
        String level = child.getAgeGroup();
        boolean hasSavedProgress = prefs.contains("placement_index_" + level)
                && prefs.getInt("placement_index_" + level, 0) > 0;

        final android.app.Dialog dialog = new android.app.Dialog(this);
        dialog.setContentView(R.layout.dialog_placement_intro);
        dialog.setCancelable(false);

        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
        }

        // ===== שינוי הטקסטים לפי מצב =====
        TextView tvTitle = dialog.findViewById(R.id.tvDialogTitle);
        TextView tvMessage = dialog.findViewById(R.id.tvDialogMessage);
        MaterialButton btnStart = dialog.findViewById(R.id.btnStartTest);

        if (hasSavedProgress) {
            if (tvTitle != null) tvTitle.setText("ברוכים השבים!");
            if (tvMessage != null) tvMessage.setText("השארת את המבדק באמצע, בואו נמשיך מאיפה שעצרנו! 💪");
            btnStart.setText("המשך מבדק ▶");
        }
        // אם אין progress - הטקסטים הברירת מחדל מה-XML נשארים

        dialog.findViewById(R.id.btnStartTest).setOnClickListener(v -> {
            dialog.dismiss();
            Intent intent = new Intent(this, PlacementTestActivity.class);
            intent.putExtra("isNewChild", true);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
        });

        dialog.findViewById(R.id.btnLater).setOnClickListener(v -> {
            dialog.dismiss();
        });

        dialog.show();
    }

    private void showDeleteConfirmationDialog(UserChild child) {
        showCustomDialog(
                "מחיקת קוסם",
                "האם את בטוחה שברצונך למחוק את " + child.getName() + "? כל ההתקדמות תימחק.",
                "מחק לצמיתות",
                Color.parseColor("#FF5252"),
                () -> {
                    String pId = currentParent.getId();
                    String cId = child.getId();

                    DatabaseService.getInstance().deleteChild(pId, cId, new DatabaseService.DatabaseCallback<Void>() {
                        @Override
                        public void onCompleted(Void object) {
                            Toast.makeText(SelectChildActivity.this, "הילד/ה נמחק/ה בהצלחה", Toast.LENGTH_SHORT).show();
                            loadChildrenFromDB();
                        }

                        @Override
                        public void onFailed(Exception e) {
                            Toast.makeText(SelectChildActivity.this, "שגיאה במחיקה", Toast.LENGTH_SHORT).show();
                        }
                    });
                }
        );
    }
}