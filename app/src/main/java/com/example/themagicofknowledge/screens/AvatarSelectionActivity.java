package com.example.themagicofknowledge.screens;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.FrameLayout;
import android.widget.GridView;
import android.widget.ImageView;
import android.widget.Toast;

import com.example.themagicofknowledge.R;
import com.example.themagicofknowledge.models.UserChild;
import com.example.themagicofknowledge.models.UserParent;
import com.example.themagicofknowledge.utils.SharedPreferencesUtil;
import com.google.android.material.button.MaterialButton;
import com.google.firebase.database.FirebaseDatabase;

import java.util.ArrayList;
import java.util.List;

public class AvatarSelectionActivity extends BaseActivity {

    private GridView gvAvatars;
    private MaterialButton btnConfirmAvatar;
    private List<String> avatarNames;
    private String childId;
    private UserParent currentParent;

    // ⭐ חדש - שומר את הבחירה הנוכחית
    private int selectedPosition = -1;
    private AvatarAdapter adapter;

    @Override
    protected boolean hasSideMenu() {
        return false;
    }

    @Override
    protected boolean showToolbar() {
        return false;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_avatar_selection);

        childId = getIntent().getStringExtra("childId");
        currentParent = SharedPreferencesUtil.getUser(this);

        gvAvatars = findViewById(R.id.gvAvatars);
        btnConfirmAvatar = findViewById(R.id.btnConfirmAvatar);
        MaterialButton btnBack = findViewById(R.id.btnBackAvatar);

        initAvatarList();

        adapter = new AvatarAdapter();
        gvAvatars.setAdapter(adapter);

        // ⭐ לחיצה על תמונה - רק מסמנת אותה
        gvAvatars.setOnItemClickListener((parent, view, position, id) -> {
            selectedPosition = position;
            adapter.notifyDataSetChanged();  // מרענן את התצוגה כדי להציג את הבחירה

            // הופך את כפתור האישור לפעיל
            btnConfirmAvatar.setEnabled(true);
            btnConfirmAvatar.setAlpha(1.0f);
        });

        // ⭐ כפתור אישור - שומר את הבחירה
        btnConfirmAvatar.setOnClickListener(v -> {
            if (selectedPosition >= 0) {
                String selectedAvatar = avatarNames.get(selectedPosition);
                updateChildAvatarInFirebase(selectedAvatar);
            } else {
                Toast.makeText(this, "אנא בחרו תמונה תחילה", Toast.LENGTH_SHORT).show();
            }
        });

        // ⭐ כפתור חזרה
        if (btnBack != null) {
            btnBack.setOnClickListener(v -> finish());
        }

        // התחלה - הכפתור לא פעיל
        btnConfirmAvatar.setAlpha(0.5f);
    }

    private void initAvatarList() {
        avatarNames = new ArrayList<>();
        avatarNames.add("avatar_1");
        avatarNames.add("avatar_2");
        avatarNames.add("avatar_3");
        avatarNames.add("avatar_4");
        avatarNames.add("avatar_5");
        avatarNames.add("avatar_6");
        avatarNames.add("avatar_7");
        avatarNames.add("avatar_8");
        avatarNames.add("avatar_9");
    }

    private void updateChildAvatarInFirebase(String avatarName) {
        String path = "users/" + currentParent.getId() + "/childrenList/" + childId + "/avatar";

        FirebaseDatabase.getInstance().getReference(path).setValue(avatarName)
                .addOnSuccessListener(aVoid -> {
                    UserParent parent = SharedPreferencesUtil.getUser(AvatarSelectionActivity.this);
                    if (parent != null && parent.getChildrenList() != null) {
                        UserChild childInParent = parent.getChildrenList().get(childId);

                        if (childInParent != null) {
                            childInParent.setAvatar(avatarName);
                            SharedPreferencesUtil.saveUser(AvatarSelectionActivity.this, parent);
                            SharedPreferencesUtil.saveCurrentChild(AvatarSelectionActivity.this, childInParent);
                        }
                    }

                    Intent intent = new Intent(AvatarSelectionActivity.this, PlacementTestActivity.class);
                    intent.putExtra("isNewChild", true);
                    intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                    startActivity(intent);
                    finish();

                    Toast.makeText(AvatarSelectionActivity.this, "בחירה נהדרת! עכשיו נבדוק את הרמה שלך 🎯", Toast.LENGTH_SHORT).show();
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(AvatarSelectionActivity.this, "שגיאה בשמירה: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }

    private class AvatarAdapter extends BaseAdapter {
        @Override
        public int getCount() {
            return avatarNames.size();
        }

        @Override
        public Object getItem(int position) {
            return avatarNames.get(position);
        }

        @Override
        public long getItemId(int position) {
            return position;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            if (convertView == null) {
                convertView = getLayoutInflater().inflate(R.layout.item_avatar, parent, false);
            }

            FrameLayout container = convertView.findViewById(R.id.avatarContainer);
            ImageView imageView = convertView.findViewById(R.id.ivAvatarItem);

            String name = avatarNames.get(position);
            int resId = getResources().getIdentifier(name, "drawable", getPackageName());

            if (resId != 0) {
                imageView.setImageResource(resId);
            }

            // ⭐ מסמן את התמונה הנבחרת
            if (container != null) {
                if (position == selectedPosition) {
                    container.setBackgroundResource(R.drawable.selected_avatar_border);
                } else {
                    container.setBackgroundResource(R.drawable.default_avatar_background);
                }
            }

            return convertView;
        }
    }
}