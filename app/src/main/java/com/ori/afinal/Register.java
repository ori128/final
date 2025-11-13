package com.ori.afinal;

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


import com.ori.afinal.model.User;
import com.ori.afinal.Services.DatabaseService;
/*
import com.example.testapp.R;
import com.example.testapp.utils.SharedPreferencesUtil;
import com.example.testapp.utils.Validator;

 */

/// Activity for registering the user
/// This activity is used to register the user
/// It contains fields for the user to enter their information
/// It also contains a button to register the user
/// When the user is registered, they are redirected to the main activity
public class RegisterActivity extends BaseActivity implements View.OnClickListener {

    private static final String TAG = "RegisterActivity";

    private EditText etEmail, etPassword, etFName, etLName, etPhone;
    private Button btnRegister;
    private TextView tvLogin;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        /// set the layout for the activity
        setContentView(R.layout.activity_register);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        /// get the views
        etEmail = findViewById(R.id.et_register_email);
        etPassword = findViewById(R.id.et_register_password);
        etFName = findViewById(R.id.et_register_first_name);
        etLName = findViewById(R.id.et_register_last_name);
        etPhone = findViewById(R.id.et_register_phone);
        btnRegister = findViewById(R.id.btn_register_register);
        tvLogin = findViewById(R.id.tv_register_login);

        /// set the click listener
        btnRegister.setOnClickListener(this);
        tvLogin.setOnClickListener(this);
    }

    @Override
    public void onClick(View v) {
        if (v.getId() == btnRegister.getId()) {
            Log.d(TAG, "onClick: Register button clicked");

            /// get the input from the user
            String email = etEmail.getText().toString();
            String password = etPassword.getText().toString();
            String fName = etFName.getText().toString();
            String lName = etLName.getText().toString();
            String phone = etPhone.getText().toString();


            /// Validate input
            Log.d(TAG, "onClick: Validating input...");
            if (!checkInput(email, password, fName, lName, phone)) {
                /// stop if input is invalid
                return;
            }

            Log.d(TAG, "onClick: Registering user...");

            /// Register user
            registerUser(fName, lName, phone, email, password);
        } else if (v.getId() == tvLogin.getId()) {
            /// Navigate back to Login Activity
            finish();
        }
    }






    /// Register the user
    private void registerUser(String fname, String lname, String phone, String email, String password) {
        Log.d(TAG, "registerUser: Registering user...");

        String uid = databaseService.generateUserId();

        /// create a new user object
        User user = new User(uid, fname, lname, phone,email, password, false);

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

    private void createUserInDatabase(User user) {
        databaseService.createNewUser(user, new DatabaseService.DatabaseCallback<Void>() {
            @Override
            public void onCompleted(Void object) {
                Log.d(TAG, "createUserInDatabase: User created successfully");
                /// save the user to shared preferences

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

            }
        });
    }
}
