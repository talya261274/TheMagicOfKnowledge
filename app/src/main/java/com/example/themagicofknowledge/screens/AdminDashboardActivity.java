package com.example.themagicofknowledge.screens;

import android.content.Intent;
import android.os.Bundle;
import android.widget.TextView;

import com.example.themagicofknowledge.R;
import com.example.themagicofknowledge.models.UserParent;
import com.example.themagicofknowledge.services.DatabaseService;
import com.example.themagicofknowledge.utils.SharedPreferencesUtil;
import com.google.android.material.button.MaterialButton;

import java.util.List;

public class AdminDashboardActivity extends BaseActivity {

    private TextView tvAdminName, tvTotalUsers, tvTotalChildren;
    private MaterialButton btnViewUsers;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_admin_dashboard);

        // הגנת הרשאות - רק מנהל יכול להיות פה
        UserParent currentUser = SharedPreferencesUtil.getUser(this);
        if (currentUser == null || !currentUser.isAdmin()) {
            Intent intent = new Intent(this, LandingActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
            finish();
            return;
        }

        initViews();
        tvAdminName.setText("שלום, " + currentUser.getFirstName() + "!");
        loadStatistics();

        btnViewUsers.setOnClickListener(v -> {
            Intent intent = new Intent(this, UsersListActivity.class);
            startActivity(intent);
        });
    }

    private void initViews() {
        tvAdminName = findViewById(R.id.tvAdminName);
        tvTotalUsers = findViewById(R.id.tvTotalUsers);
        tvTotalChildren = findViewById(R.id.tvTotalChildren);
        btnViewUsers = findViewById(R.id.btnViewUsers);
    }

    private void loadStatistics() {
        DatabaseService.getInstance().getUserList(new DatabaseService.DatabaseCallback<>() {
            @Override
            public void onCompleted(List<UserParent> users) {
                int totalUsers = 0;
                int totalChildren = 0;

                for (UserParent user : users) {
                    if (user != null && !user.isAdmin()) {
                        totalUsers++;
                        if (user.getChildrenList() != null) {
                            totalChildren += user.getChildrenList().size();
                        }
                    }
                }

                tvTotalUsers.setText(String.valueOf(totalUsers));
                tvTotalChildren.setText(String.valueOf(totalChildren));
            }

            @Override
            public void onFailed(Exception e) {
                tvTotalUsers.setText("?");
                tvTotalChildren.setText("?");
            }
        });
    }

    @Override
    protected boolean hasSideMenu() {
        return true;
    }
}