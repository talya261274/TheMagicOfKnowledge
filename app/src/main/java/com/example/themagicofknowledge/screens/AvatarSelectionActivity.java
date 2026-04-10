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
        DatabaseService.getInstance().updateUser(currentParent.getId(), userParentServer -> {
            if (userParentServer != null && userParentServer.getChildrenList() != null) {
                UserChild child = userParentServer.getChildrenList().get(childId);
                if (child != null) {
                    child.setAvatar(avatarName);
                }
            }
            return userParentServer;
        }, new DatabaseService.DatabaseCallback<UserParent>() {
            @Override
            public void onCompleted(UserParent userParentServer) {
                // 1. שמירת המשתמש המעודכן בזיכרון המקומי (SharedPreferences)
                SharedPreferencesUtil.saveUser(AvatarSelectionActivity.this, userParentServer);

                // 2. הגדרת הילד הזה כ"ילד הנוכחי" שנכנס לאפליקציה
                UserChild updatedChild = userParentServer.getChildrenList().get(childId);
                if (updatedChild != null) {
                    SharedPreferencesUtil.saveCurrentChild(AvatarSelectionActivity.this, updatedChild);
                }

                // 3. מעבר ישיר למסך הראשי (MainActivity)
                Intent intent = new Intent(AvatarSelectionActivity.this, MainActivity.class);

                // דואג שהמשתמש לא יוכל לחזור אחורה למסך בחירת האוואטר
                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);

                startActivity(intent);
                finish();

                Toast.makeText(AvatarSelectionActivity.this, ",בחירה נהדרת! בואו נתחיל", Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onFailed(Exception e) {
                Toast.makeText(AvatarSelectionActivity.this, "שגיאה בשמירה: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            }
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