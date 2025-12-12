package com.example.themagicofknowledge.screens;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.example.themagicofknowledge.R;
import com.example.themagicofknowledge.models.UserParent;
import com.example.themagicofknowledge.services.DatabaseService;
import com.example.themagicofknowledge.utils.SharedPreferencesUtil;
import com.example.themagicofknowledge.utils.Validator;

/// Activity for logging in the user
/// This activity is used to log in the user
/// It contains fields for the user to enter their user name and password
/// It also contains a button to log in the user
/// When the user is logged in, they are redirected to the main activity
public class LoginActivity extends BaseActivity implements View.OnClickListener {

    private static final String TAG = "LoginActivity";

    private EditText etUName, etPassword;
    private Button btnLogin;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        /// set the layout for the activity
        setContentView(R.layout.activity_login);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.login), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        Button btnGoBack = findViewById(R.id.goBackBtn);
        btnGoBack.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(LoginActivity.this, LandingActivity.class);
                startActivity(intent);
            }
        });

        TextView tvForgotPassword = findViewById(R.id.tvForgotPassword);

        tvForgotPassword.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(LoginActivity.this, ForgotPasswordActivity.class);
                startActivity(intent);
            }
        });

        /// get the views
        etUName = findViewById(R.id.et_login_user_name);
        etPassword = findViewById(R.id.et_login_password);
        btnLogin = findViewById(R.id.btn_login_login);

        /// set the click listener
        btnLogin.setOnClickListener(this);
    }

    @Override
    public void onClick(View v) {
        if (v.getId() == btnLogin.getId()) {
            Log.d(TAG, "onClick: Login button clicked");

            /// get the user name and password entered by the user
            String UName = etUName.getText().toString()+"";
            String password = etPassword.getText().toString()+"";

            /// log the user name and password
            Log.d(TAG, "onClick: User Name: " + UName);
            Log.d(TAG, "onClick: Password: " + password);

            Log.d(TAG, "onClick: Validating input...");
            /// Validate input
            if (!checkInput(UName, password)) {
                /// stop if input is invalid
                return;
            }

            Log.d(TAG, "onClick: Logging in user...");

            /// Login user
            loginUser(UName, password);
        }
    }

    /// Method to check if the input is valid
    /// It checks if the yser name and password are valid
    /// @see Validator#isUserNameValid(String) Valid(String)
    /// @see Validator#isPasswordValid(String)
    private boolean checkInput(String UName, String password) {
        if (!Validator.isUserNameValid(UName)) {
            Log.e(TAG, "checkInput: Invalid user name");
            /// show error message to user
            etUName.setError("Invalid user name");
            /// set focus to user name field
            etUName.requestFocus();
            return false;
        }

        if (!Validator.isPasswordValid(password)) {
            Log.e(TAG, "checkInput: Invalid password");
            /// show error message to user
            etPassword.setError("Password must be at least 6 characters long");
            /// set focus to password field
            etPassword.requestFocus();
            return false;
        }

        return true;
    }

    private void loginUser(String UName, String password) {
        databaseService.getUserByUsernameAndPassword(UName, password, new DatabaseService.DatabaseCallback<UserParent>() {
            /// Callback method called when the operation is completed
            /// @param user the user object that is logged in
            @Override
            public void onCompleted(UserParent user) {
                if (user == null) {
                    /// Show error message to user
                    etPassword.setError("Invalid user name or password");
                    etPassword.requestFocus();
                    return;
                }
                Log.d(TAG, "onCompleted: User logged in: " + user);
                /// save the user data to shared preferences
                SharedPreferencesUtil.saveUser(LoginActivity.this, user);
                /// Redirect to main activity and clear back stack to prevent user from going back to login screen
                Intent mainIntent = new Intent(LoginActivity.this, UsersListActivity.class);
                /// Clear the back stack (clear history) and start the MainActivity
                mainIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                startActivity(mainIntent);
            }

            @Override
            public void onFailed(Exception e) {
                Log.e(TAG, "onFailed: Failed to retrieve user data", e);
                /// Show error message to user
                etPassword.setError("Invalid user name or password");
                etPassword.requestFocus();
                /// Sign out the user if failed to retrieve user data
                /// This is to prevent the user from being logged in again
                SharedPreferencesUtil.signOutUser(LoginActivity.this);
            }
        });
    }
}