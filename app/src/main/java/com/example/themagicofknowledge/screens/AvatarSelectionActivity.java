package com.example.themagicofknowledge.screens;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.GridView;
import android.widget.ImageView;
import android.widget.Toast;

import com.example.themagicofknowledge.R;
import com.example.themagicofknowledge.models.UserChild;
import com.example.themagicofknowledge.models.UserParent;
import com.example.themagicofknowledge.services.DatabaseService;
import com.example.themagicofknowledge.utils.SharedPreferencesUtil;
import com.google.firebase.database.FirebaseDatabase;

import java.util.ArrayList;
import java.util.List;

public class AvatarSelectionActivity extends BaseActivity {

    private GridView gvAvatars;
    private List<String> avatarNames;
    private String childId;
    private UserParent currentParent;

    @Override
    protected boolean hasSideMenu() {
        return true;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_avatar_selection);

        // קבלת ה-ID של הילד מה-Intent
        childId = getIntent().getStringExtra("childId");
        currentParent = SharedPreferencesUtil.getUser(this);

        gvAvatars = findViewById(R.id.gvAvatars);
        initAvatarList();

        AvatarAdapter adapter = new AvatarAdapter();
        gvAvatars.setAdapter(adapter);

        gvAvatars.setOnItemClickListener((parent, view, position, id) -> {
            String selectedAvatar = avatarNames.get(position);
            updateChildAvatarInFirebase(selectedAvatar);
        });
    }

    private void initAvatarList() {
        avatarNames = new ArrayList<>();
        // וודאי ששמות אלו תואמים בדיוק לקבצים ב-res/drawable
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
        // נתיב ישיר לשדה האוואטר של הילד הספציפי ב-Firebase
        String path = "users/" + currentParent.getId() + "/childrenList/" + childId + "/avatar";

        // שימוש ב-FirebaseDatabase ישירות כדי לעדכן רק את השדה הזה
        FirebaseDatabase.getInstance().getReference(path).setValue(avatarName)
                .addOnSuccessListener(aVoid -> {
                    // 1. קודם כל, מעדכנים את אובייקט ההורה בזיכרון (זה כבר עשית וזה מצוין)
                    UserParent parent = SharedPreferencesUtil.getUser(AvatarSelectionActivity.this);
                    if (parent != null && parent.getChildrenList() != null) {
                        UserChild childInParent = parent.getChildrenList().get(childId);

                        if (childInParent != null) {
                            // מעדכנים לו את האוואטר
                            childInParent.setAvatar(avatarName);

                            // שומרים את ההורה המעודכן
                            SharedPreferencesUtil.saveUser(AvatarSelectionActivity.this, parent);

                            // *** השורה הקריטית: הופכים את הילד החדש לילד הפעיל! ***
                            SharedPreferencesUtil.saveCurrentChild(AvatarSelectionActivity.this, childInParent);
                        }
                    }

                    // 2. מעבר למסך הראשי
                    Intent intent = new Intent(AvatarSelectionActivity.this, MainActivity.class);
                    intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                    startActivity(intent);
                    finish();

                    Toast.makeText(AvatarSelectionActivity.this, "בחירה נהדרת! בואו נתחיל", Toast.LENGTH_SHORT).show();
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(AvatarSelectionActivity.this, "שגיאה בשמירה: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }

    // אדפטר פנימי להצגת האוואטרים ב-GridView
    private class AvatarAdapter extends BaseAdapter {
        @Override
        public int getCount() { return avatarNames.size(); }
        @Override
        public Object getItem(int position) { return avatarNames.get(position); }
        @Override
        public long getItemId(int position) { return position; }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            if (convertView == null) {
                // טעינת ה-Layout של האייטם (זה עם העיגול והמסגרת)
                convertView = getLayoutInflater().inflate(R.layout.item_avatar, parent, false);
            }

            ImageView imageView = convertView.findViewById(R.id.ivAvatarItem);
            String name = avatarNames.get(position);
            int resId = getResources().getIdentifier(name, "drawable", getPackageName());

            if (resId != 0) {
                imageView.setImageResource(resId);
            }

            return convertView;
        }
    }
}