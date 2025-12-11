package com.ori.afinal;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.firebase.auth.FirebaseAuth;
import com.ori.afinal.Services.DatabaseService;
import com.ori.afinal.model.User;

public class RegisterUser extends AppCompatActivity implements View.OnClickListener {

    private static final String TAG = "Register";
    EditText etFname, etLname, etMail, etPhone, etPassword;
    String fName, lName, email, phone, password;
    Button btnSubmit;
    private DatabaseService databaseService;
    private FirebaseAuth mAuth;
    public static final String MyPREFERENCES = "MyPrefs" ;


    SharedPreferences sharedpreferences;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_register_user);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;

        });


        sharedpreferences=getSharedPreferences(MyPREFERENCES, Context.MODE_PRIVATE);

        databaseService=DatabaseService.getInstance();
        mAuth = FirebaseAuth.getInstance();
        etFname = findViewById(R.id.et_register_first_name);
        etLname = findViewById(R.id.et_register_last_name);
        etMail = findViewById(R.id.et_register_email);
        etPhone = findViewById(R.id.et_register_phone);
        etPassword = findViewById(R.id.et_register_password);
        btnSubmit = findViewById(R.id.btn_register_register);
        btnSubmit.setOnClickListener(this);
    }


    @Override
    public void onClick(View v) {
        if (v.getId() == btnSubmit.getId()) {
            Log.d(TAG, "onClick: Register button clicked");

            /// get the input from the user
            fName = etFname.getText().toString();
            lName = etLname.getText().toString();
            email = etMail.getText().toString();
            phone = etPhone.getText().toString();
            password = etPassword.getText().toString();


            /// Validate input
            Log.d(TAG, "onClick: Registering user...");

            /// Register user
            registerUser(fName, lName, phone, email, password);
        }
    }

    /// Register the user
    private void registerUser(String fname, String lname, String phone, String email, String password) {
        Log.d(TAG, "registerUser: Registering user...");


        User user = new User("", fname, lname, email, phone, password, false);

        createUserInDatabase(user);
    }


    private void createUserInDatabase(User user) {
        databaseService.createNewUser(user, new DatabaseService.DatabaseCallback<String>() {
            @Override
            public void onCompleted(String uid) {
                Log.d(TAG, "createUserInDatabase: User created successfully");
                /// save the user to shared preferences
                user.setId(uid);
                Log.d(TAG, "createUserInDatabase: Redirecting to MainActivity");
                /// Redirect to MainActivity and clear back stack to prevent user from going back to register screen

                SharedPreferences.Editor editor = sharedpreferences.edit();

                editor.putString("email", email);
                editor.putString("password", password);

                editor.commit();

                Intent intent = new Intent(RegisterUser.this, MainActivity.class);
                /// clear the back stack (clear history) and start the MainActivity
                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                startActivity(intent);
            }

            @Override
            public void onFailed(Exception e) {
                Log.e(TAG, "createUserInDatabase: Failed to create user", e);
                /// show error message to user
                Toast.makeText(RegisterUser.this, "Failed to register user", Toast.LENGTH_SHORT).show();
                /// sign out the user if failed to register

            }
        });
    }
}