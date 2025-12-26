package com.example.themagicofknowledge.screens;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import com.example.themagicofknowledge.R;
import com.example.themagicofknowledge.models.UserParent;
import com.example.themagicofknowledge.utils.SharedPreferencesUtil;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.*;

public class UserProfileActivity extends AppCompatActivity {

    private TextInputEditText etFirstName, etLastName, etEmail, etPhone, etBirthDate;
    private TextView etUserName, etId;
    private MaterialButton btnSignOut;
    UserParent currentUser;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_user_profile);

        etUserName = findViewById(R.id.tv_user_display_name);
        etId = findViewById(R.id.tv_user_display_id);
        etFirstName = findViewById(R.id.et_user_first_name);
        etLastName = findViewById(R.id.et_user_last_name);
        etEmail = findViewById(R.id.et_user_email);
        etPhone = findViewById(R.id.et_user_phone);
        etBirthDate = findViewById(R.id.et_user_birth_date);
        btnSignOut = findViewById(R.id.btn_sign_out);

        currentUser = SharedPreferencesUtil.getUser(this);

        if (currentUser != null) {
            etUserName.setText(currentUser.getUserName());
            etId.setText(currentUser.getId());
            etFirstName.setText(currentUser.getFirstName());
            etLastName.setText(currentUser.getLastName());
            etEmail.setText(currentUser.getEmail());
            etPhone.setText(currentUser.getPhone());
            etBirthDate.setText(currentUser.getBirthDate());
        }

        btnSignOut.setOnClickListener(v -> {
            SharedPreferencesUtil.signOutUser(this);
            Intent intent = new Intent(UserProfileActivity.this, LandingActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
        });


        Toolbar toolbar = findViewById(R.id.toolbar_user_profile);

        toolbar.setNavigationOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                finish();
            }
        });
    }
}