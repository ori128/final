package com.ori.afinal;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.firebase.auth.FirebaseAuth;
import com.ori.afinal.Services.DatabaseService;
import com.ori.afinal.model.User;

import java.util.Objects;

public class RegisterUser extends AppCompatActivity implements View.OnClickListener {

    private static final String TAG = "Register";

    EditText etFname, etLname, etMail, etPhone, etPassword;
    String fName, lName, email, phone, password;

    Button btnSubmit;
    TextView tvRegisterLogin;

    private DatabaseService databaseService;
    private FirebaseAuth mAuth;

    public static final String MyPREFERENCES = "MyPrefs";
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

        sharedpreferences = getSharedPreferences(MyPREFERENCES, Context.MODE_PRIVATE);

        databaseService = DatabaseService.getInstance();
        mAuth = FirebaseAuth.getInstance();

        etFname = findViewById(R.id.et_register_first_name);
        etLname = findViewById(R.id.et_register_last_name);
        etMail = findViewById(R.id.et_register_email);
        etPhone = findViewById(R.id.et_register_phone);
        etPassword = findViewById(R.id.et_register_password);

        btnSubmit = findViewById(R.id.btn_register_register);
        tvRegisterLogin = findViewById(R.id.tv_register_login);

        btnSubmit.setOnClickListener(this);

        // 👉 מעבר למסך התחברות
        tvRegisterLogin.setOnClickListener(v -> {
            Intent intent = new Intent(RegisterUser.this, Login.class);
            startActivity(intent);
            finish();
        });
    }

    @Override
    public void onClick(View v) {
        if (v.getId() == btnSubmit.getId()) {
            Log.d(TAG, "Register button clicked");

            fName = etFname.getText().toString().trim();
            lName = etLname.getText().toString().trim();
            email = etMail.getText().toString().trim();
            phone = etPhone.getText().toString().trim();
            password = etPassword.getText().toString().trim();

            registerUser(fName, lName, phone, email, password);
        }
    }

    private void registerUser(String fname, String lname, String phone, String email, String password) {
        Log.d(TAG, "registerUser: Registering user");

        mAuth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener(task -> {
                    if (!task.isSuccessful()) {
                        Log.e(TAG, "Auth failed", task.getException());
                        Toast.makeText(this, "Authentication failed", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    String uid = Objects.requireNonNull(mAuth.getCurrentUser()).getUid();
                    boolean isAdmin = false;

                    User user = new User(uid, fname, lname, phone, email, password, isAdmin);
                    createUserInDatabase(user);
                });
    }

    private void createUserInDatabase(User user) {
        databaseService.createNewUser2(user, new DatabaseService.DatabaseCallback<Void>() {
            @Override
            public void onCompleted(Void object) {
                Log.d(TAG, "User created successfully");

                SharedPreferences.Editor editor = sharedpreferences.edit();
                editor.putString("email", email);
                editor.putString("password", password);
                editor.apply();

                Intent intent = new Intent(RegisterUser.this, MainActivity.class);
                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                startActivity(intent);
            }

            @Override
            public void onFailed(Exception e) {
                Log.e(TAG, "Failed to create user", e);
                Toast.makeText(RegisterUser.this, "Failed to register user", Toast.LENGTH_SHORT).show();
            }
        });
    }
}
