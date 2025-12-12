package com.example.themagicofknowledge.screens;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.example.themagicofknowledge.R;
import com.example.themagicofknowledge.models.UserParent;
import com.example.themagicofknowledge.services.DatabaseService;
import com.example.themagicofknowledge.utils.SharedPreferencesUtil;
import com.example.themagicofknowledge.utils.Validator;

public class UpdateDetailsActivity extends BaseActivity implements View.OnClickListener {

    private static final String TAG = "UserProfileActivity";

    private EditText etUserFirstName, etUserLastName, etUserEmail, etUserPhone, etUserBirthDate, etUserUserName, etUserPassword;
    private Button btnUpdateProfile;
    UserParent currentUser;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_update_details);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        Button btnGoBack = findViewById(R.id.goBackBtn3);
        btnGoBack.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(UpdateDetailsActivity.this, LandingActivity.class);
                startActivity(intent);
            }
        });


        currentUser = SharedPreferencesUtil.getUser(this);
        assert currentUser != null;

        // Initialize the EditText fields
        etUserFirstName = findViewById(R.id.etFirstName);
        etUserLastName = findViewById(R.id.etLastName);
        etUserEmail = findViewById(R.id.etEmail);
        etUserPhone = findViewById(R.id.etPhone);
        etUserBirthDate = findViewById(R.id.etBirthDate);
        etUserUserName = findViewById(R.id.etUsername);
        etUserPassword = findViewById(R.id.etPassword);

        btnUpdateProfile = findViewById(R.id.btn_edit_profile);
        btnUpdateProfile.setOnClickListener(this);


        showUserProfile();
    }

    private void showUserProfile() {
        String id = SharedPreferencesUtil.getUserId(this);
        // Get the user data from database
        assert id != null;
        databaseService.getUser(id, new DatabaseService.DatabaseCallback<UserParent>() {
            @Override
            public void onCompleted(UserParent user) {
                currentUser = user;
                // Set the user data to the EditText fields
                etUserFirstName.setText(user.getFirstName());
                etUserLastName.setText(user.getLastName());
                etUserEmail.setText(user.getEmail());
                etUserPhone.setText(user.getPhone());
                etUserBirthDate.setText(user.getBirthDate());
                etUserUserName.setText(user.getUserName());
                etUserPassword.setText(user.getPassword());

            }

            @Override
            public void onFailed(Exception e) {
                Log.e(TAG, "Error getting user profile", e);
            }
        });
    }

    private void updateUserProfile() {
        if (currentUser == null) {
            Log.e(TAG, "User not found");
            Toast.makeText(this, "User not found", Toast.LENGTH_SHORT).show();
            return;
        }
        // Get the updated user data from the EditText fields
        String firstName = etUserFirstName.getText().toString();
        String lastName = etUserLastName.getText().toString();
        String phone = etUserPhone.getText().toString();
        String email = etUserEmail.getText().toString();
        String birthDate = etUserBirthDate.getText().toString();
        String userName = etUserUserName.getText().toString();
        String password = etUserPassword.getText().toString();


        if (!isValid(firstName, lastName, phone, email, birthDate, userName,password)) {
            Log.e(TAG, "Invalid input");
            return;
        }

        // Update the user object
        currentUser.setFirstName(firstName);
        currentUser.setLastName(lastName);
        currentUser.setPhone(phone);
        currentUser.setEmail(email);
        currentUser.setBirthDate(birthDate);
        currentUser.setUserName(userName);
        currentUser.setPassword(password);

        // Update the user data in the authentication
        Log.d(TAG, "Updating user profile");
        Log.d(TAG, "Selected user UID: " + currentUser.getId());
        Log.d(TAG, "User email: " + currentUser.getEmail());
        Log.d(TAG, "User password: " + currentUser.getPassword());


        updateUserInDatabase(currentUser);
    }

    private void updateUserInDatabase(UserParent user) {
        Log.d(TAG, "Updating user in database: " + user.getId());
        databaseService.updateUser(user, new DatabaseService.DatabaseCallback<Void>() {
            @Override
            public void onCompleted(Void result) {
                Log.d(TAG, "User profile updated successfully");
                Toast.makeText(UpdateDetailsActivity.this, "הפרופיל עודכן בהצלחה", Toast.LENGTH_SHORT).show();
                showUserProfile(); // Refresh the profile view
            }

            @Override
            public void onFailed(Exception e) {
                Log.e(TAG, "Error updating user profile", e);
                Toast.makeText(UpdateDetailsActivity.this, "עדכון הפרופיל נכשל", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private boolean isValid(String firstName, String lastName, String phone, String email, String birthDate, String userName, String password) {
        if (!Validator.isNameValid(firstName)) {
            etUserFirstName.setError("First name is required");
            etUserFirstName.requestFocus();
            return false;
        }
        if (!Validator.isNameValid(lastName)) {
            etUserLastName.setError("Last name is required");
            etUserLastName.requestFocus();
            return false;
        }
        if (!Validator.isPhoneValid(phone)) {
            etUserPhone.setError("Phone number is required");
            etUserPhone.requestFocus();
            return false;
        }
        if (!Validator.isEmailValid(email)) {
            etUserEmail.setError("Email is required");
            etUserEmail.requestFocus();
            return false;
        }
        if (!Validator.isBirthDateValid(birthDate)) {
            etUserBirthDate.setError("Birth Date is required");
            etUserBirthDate.requestFocus();
            return false;
        }
        if (!Validator.isUserNameValid(userName)) {
            etUserUserName.setError("User Name is required");
            etUserUserName.requestFocus();
            return false;
        }
        if (!Validator.isPasswordValid(password)) {
            etUserPassword.setError("Password is required");
            etUserPassword.requestFocus();
            return false;
        }
        return true;
    }

    @Override
    public void onClick(View view) {
        if (view.getId() == R.id.btn_edit_profile) {
            updateUserProfile();
        }
    }
}
