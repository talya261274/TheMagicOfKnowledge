package com.example.themagicofknowledge.screens;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.Button;
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
import com.google.android.material.textfield.TextInputLayout;

import java.util.ArrayList;

/// Activity for registering the user
/// This activity is used to register the user
/// It contains fields for the user to enter their information
/// It also contains a button to register the user
/// When the user is registered, they are redirected to the main activity
public class RegisterActivity extends BaseActivity implements View.OnClickListener {

    private static final String TAG = "RegisterActivity";

    private TextInputLayout etEmail, etPassword, etFName, etLName, etPhone, etBDate, etUName, etCPassword;
    private Button btnRegister;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        /// set the layout for the activity
        setContentView(R.layout.activity_register);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.register), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });
        Button btnGoBack = findViewById(R.id.goBackBtn2);
        btnGoBack.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(RegisterActivity.this, LandingActivity.class);
                startActivity(intent);
            }
        });

        /// get the views
        etFName = findViewById(R.id.et_register_first_name);
        etLName = findViewById(R.id.et_register_last_name);
        etEmail = findViewById(R.id.et_register_email);
        etPhone = findViewById(R.id.et_register_phone);
        etBDate = findViewById(R.id.et_register_birth_date);
        etUName = findViewById(R.id.et_register_user_name);
        etPassword = findViewById(R.id.et_register_password);
        etCPassword = findViewById(R.id.et_register_confirm_password);
        btnRegister = findViewById(R.id.btn_register_register);

        /// set the click listener
        btnRegister.setOnClickListener(this);
    }

    @Override
    public void onClick(View v) {
        if (v.getId() == btnRegister.getId()) {
            Log.d(TAG, "onClick: Register button clicked");

            /// get the input from the user
            String fName = etFName.getEditText().getText().toString();
            String lName = etLName.getEditText().getText().toString();
            String email = etEmail.getEditText().getText().toString();
            String phone = etPhone.getEditText().getText().toString();
            String BDate = etBDate.getEditText().getText().toString();
            String UName = etUName.getEditText().getText().toString();
            String password = etPassword.getEditText().getText().toString();
            String CPassword = etCPassword.getEditText().getText().toString();



            /// log the input
            Log.d(TAG, "onClick: First Name: " + fName);
            Log.d(TAG, "onClick: Last Name: " + lName);
            Log.d(TAG, "onClick: Email: " + email);
            Log.d(TAG, "onClick: Phone: " + phone);
            Log.d(TAG, "onClick: Birth Date: " + BDate);
            Log.d(TAG, "onClick: User Name: " + UName);
            Log.d(TAG, "onClick: Password: " + password);
            Log.d(TAG, "onClick: Confirm Password: " + CPassword);



            /// Validate input
            Log.d(TAG, "onClick: Validating input...");
            if (!checkInput(fName, lName,  email, phone, BDate, UName, password, CPassword)) {
                /// stop if input is invalid
                return;
            }

            Log.d(TAG, "onClick: Registering user...");

            /// Register user
            registerUser(fName, lName,  email, phone, BDate, UName, password, CPassword);
        }
    }

    /// Check if the input is valid
    /// @return true if the input is valid, false otherwise
    /// @see Validator
    private boolean checkInput(String fName, String lName, String email, String phone, String BDate,
                               String UName, String password, String CPassword) {

         if (!Validator.isNameValid(fName)) {
            etFName.setError("שם פרטי חייב להכיל לפחות 3 אותיות");
            etFName.requestFocus();
            Log.i(TAG, "שם פרטי חייב להכיל לפחות 3 אותיות");
            return false;
        }

        if (!Validator.isNameValid(lName)) {
            etLName.setError("שם משפחה חייב להכיל לפחות 3 אותיות");
            etLName.requestFocus();
            Log.i(TAG, "שם משפחה חייב להכיל לפחות 3 אותיות");
            return false;
        }

        if (!Validator.isEmailValid(email)) {
            etEmail.setError("כתובת אימייל לא חוקית");
            Log.i(TAG, "כתובת אימייל לא חוקית");
            etEmail.requestFocus();
            return false;
        }

        if (!Validator.isPhoneValid(phone)) {
            etPhone.setError("מספר טלפון חייב להכיל לפחות 10 ספרות");
            Log.i(TAG, "מספר טלפון חייב להכיל לפחות 10 ספרות");
            etPhone.requestFocus();
            return false;
        }

        if (!Validator.isUserNameValid(UName)) {
            etUName.setError("שם משתמש חייב להכיל לפחות 3 תווים, אותיות ומספרים בלבד");
            etUName.requestFocus();
            Log.i(TAG, "שם משתמש חייב להכיל לפחות 3 תווים, אותיות ומספרים בלבד");

            return false;
        }

        if (!Validator.isBirthDateValid(BDate)) {
            etBDate.setError("תאריך לידה לא חוקי או בעתיד");
            etBDate.requestFocus();
            Log.i(TAG, "תאריך לידה לא חוקי או בעתיד");
            return false;
        }

        if (!Validator.isPasswordValid(password)) {
            etPassword.setError("סיסמה חייבת להכיל לפחות 6 תווים, אותיות גדולות, קטנות ומספרים");
            etPassword.requestFocus();
            Log.i(TAG, "סיסמה חייבת להכיל לפחות 6 תווים, אותיות גדולות, קטנות ומספרים");
            return false;
        }

        if (!password.equals(CPassword)) {
            etCPassword.setError("הסיסמאות אינן תואמות");
            etCPassword.requestFocus();
            Log.i(TAG, "הסיסמאות אינן תואמות");
            return false;
        }

        return true;
    }

    /// Register the user
    private void registerUser(String fName, String lName, String email, String phone, String BDate,
                              String UName, String password,String CPassword) {
        Log.d(TAG, "registerUser: Registering user...");

        String uid = databaseService.generateUserId();

        /// create a new user object
        UserParent user = new UserParent(uid, fName, lName, email, phone, BDate, UName, password, false, new ArrayList<>());

        databaseService.checkIfEmailExists(email, new DatabaseService.DatabaseCallback<>() {
            @Override
            public void onCompleted(Boolean exists) {
                if (exists) {
                    Log.e(TAG, "onCompleted: Email already exists");
                    /// show error message to user
                    Toast.makeText(RegisterActivity.this, "Email already exists", Toast.LENGTH_SHORT).show();
                } else {
                    /// proceed to create the user
                    createUserInDatabase(user);
                }
            }

            @Override
            public void onFailed(Exception e) {
                Log.e(TAG, "onFailed: Failed to check if email exists", e);
                /// show error message to user
                Toast.makeText(RegisterActivity.this, "Failed to register user", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void createUserInDatabase(UserParent user) {
        databaseService.createNewUser(user, new DatabaseService.DatabaseCallback<Void>() {
            @Override
            public void onCompleted(Void object) {
                Log.d(TAG, "createUserInDatabase: User created successfully");
                /// save the user to shared preferences
                SharedPreferencesUtil.saveUser(RegisterActivity.this, user);
                Log.d(TAG, "createUserInDatabase: Redirecting to MainActivity");
                /// Redirect to MainActivity and clear back stack to prevent user from going back to register screen
                Intent mainIntent = new Intent(RegisterActivity.this, MainActivity.class);
                /// clear the back stack (clear history) and start the MainActivity
                mainIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                startActivity(mainIntent);
            }

            @Override
            public void onFailed(Exception e) {
                Log.e(TAG, "createUserInDatabase: Failed to create user", e);
                /// show error message to user
                Toast.makeText(RegisterActivity.this, "Failed to register user", Toast.LENGTH_SHORT).show();
                /// sign out the user if failed to register
                SharedPreferencesUtil.signOutUser(RegisterActivity.this);
            }
        });
    }
}