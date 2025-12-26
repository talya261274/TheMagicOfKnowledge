package com.example.themagicofknowledge.screens;

import android.content.Intent;
import android.os.Bundle;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;

import com.example.themagicofknowledge.R;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.*;

public class UserProfileActivity extends AppCompatActivity {

    private TextView tvUserDisplayName, tvUserDisplayId;
    private TextInputEditText etFirstName, etLastName, etEmail, etPhone, etBirthDate;
    private MaterialButton btnLogout;

    private FirebaseAuth auth;
    private DatabaseReference usersRef;
    private String uid;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_user_profile);

        auth = FirebaseAuth.getInstance();
        uid = auth.getCurrentUser().getUid();
        usersRef = FirebaseDatabase.getInstance().getReference("users").child(uid);

        initViews();
        loadUserData();
        setupLogoutButton();
    }

    private void initViews() {
        tvUserDisplayName = findViewById(R.id.tv_user_display_name);
        tvUserDisplayId   = findViewById(R.id.tv_user_display_id);

        etFirstName = findViewById(R.id.et_user_first_name);
        etLastName = findViewById(R.id.et_user_last_name);
        etEmail = findViewById(R.id.et_user_email);
        etPhone = findViewById(R.id.et_user_phone);
        etBirthDate = findViewById(R.id.et_user_birth_date);

        btnLogout = findViewById(R.id.btn_sign_out);
    }

    private void loadUserData() {
        usersRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot snapshot) {
                if (!snapshot.exists()) {
                    Toast.makeText(UserProfileActivity.this, "User data not found", Toast.LENGTH_SHORT).show();
                    return;
                }

                String userName  = snapshot.child("userName").getValue(String.class);
                String id        = snapshot.child("id").getValue(String.class);
                String firstName = snapshot.child("firstName").getValue(String.class);
                String lastName  = snapshot.child("lastName").getValue(String.class);
                String email     = snapshot.child("email").getValue(String.class);
                String phone     = snapshot.child("phone").getValue(String.class);
                String birthDate = snapshot.child("birthDate").getValue(String.class);

                // מציג בטופ
                tvUserDisplayName.setText(userName);
                tvUserDisplayId.setText(id);

                // מציג בשדות
                etFirstName.setText(firstName);
                etLastName.setText(lastName);
                etEmail.setText(email);
                etPhone.setText(phone);
                etBirthDate.setText(birthDate);
            }

            @Override
            public void onCancelled(DatabaseError error) {
                Toast.makeText(UserProfileActivity.this, "Failed to load data", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void setupLogoutButton() {
        btnLogout.setOnClickListener(v -> {
            auth.signOut();
            Toast.makeText(UserProfileActivity.this, "Signed out", Toast.LENGTH_SHORT).show();
            startActivity(new Intent(UserProfileActivity.this, LoginActivity.class));
            finish();
        });
    }
}
