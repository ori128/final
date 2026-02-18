package com.ori.afinal;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
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

public class RegisterUser extends AppCompatActivity {

    private EditText etFname, etLname, etPhone, etEmail, etPassword;
    private Button btnRegister;
    private TextView tvLogin;
    private ImageButton btnBackMain; // הכפתור החדש

    private DatabaseService databaseService;

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

        databaseService = DatabaseService.getInstance();
        initViews();
    }

    private void initViews() {
        etFname = findViewById(R.id.et_fname);
        etLname = findViewById(R.id.et_lname);
        etPhone = findViewById(R.id.et_phone);
        etEmail = findViewById(R.id.et_email);
        etPassword = findViewById(R.id.et_password);
        btnRegister = findViewById(R.id.btn_register);
        tvLogin = findViewById(R.id.tv_login);
        btnBackMain = findViewById(R.id.btn_back_main);

        btnRegister.setOnClickListener(v -> registerUser());

        // תיקון הקישור: מעבר ישיר למסך התחברות
        tvLogin.setOnClickListener(v -> {
            Intent intent = new Intent(RegisterUser.this, Login.class);
            startActivity(intent);
            finish(); // סוגר את ההרשמה
        });

        // חזרה למסך הראשי
        btnBackMain.setOnClickListener(v -> {
            finish();
        });
    }

    private void registerUser() {
        String fname = etFname.getText().toString();
        String lname = etLname.getText().toString();
        String phone = etPhone.getText().toString();
        String email = etEmail.getText().toString();
        String password = etPassword.getText().toString();

        if (TextUtils.isEmpty(fname) || TextUtils.isEmpty(lname) ||
                TextUtils.isEmpty(email) || TextUtils.isEmpty(password)) {
            Toast.makeText(this, "אנא מלא את כל השדות", Toast.LENGTH_SHORT).show();
            return;
        }

        User user = new User("", fname, lname, phone, email, password);

        databaseService.createNewUser(user, new DatabaseService.DatabaseCallback<String>() {
            @Override
            public void onCompleted(String uid) {
                Toast.makeText(RegisterUser.this, "נרשמת בהצלחה!", Toast.LENGTH_SHORT).show();
                startActivity(new Intent(RegisterUser.this, HomePage.class));
                finish();
            }

            @Override
            public void onFailed(Exception e) {
                Toast.makeText(RegisterUser.this, "שגיאה בהרשמה: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }
}