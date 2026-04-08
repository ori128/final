package com.ori.afinal;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Patterns;
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
import com.google.firebase.auth.FirebaseUser;
import com.ori.afinal.Services.DatabaseService;
import com.ori.afinal.model.User;

public class RegisterUser extends AppCompatActivity {

    private EditText etFname, etLname, etPhone, etEmail, etPassword;
    private Button btnRegister;
    private TextView tvLogin;
    private ImageButton btnBackMain;

    private DatabaseService databaseService;
    private FirebaseAuth mAuth; // הוספנו את מערכת ההזדהות של פיירבייס

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
        mAuth = FirebaseAuth.getInstance(); // אתחול מערכת ההזדהות
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

        tvLogin.setOnClickListener(v -> {
            Intent intent = new Intent(RegisterUser.this, Login.class);
            startActivity(intent);
            finish();
        });

        btnBackMain.setOnClickListener(v -> {
            finish();
        });
    }

    private void registerUser() {
        String fname = etFname.getText().toString().trim();
        String lname = etLname.getText().toString().trim();
        String phone = etPhone.getText().toString().trim();
        String email = etEmail.getText().toString().trim();
        String password = etPassword.getText().toString().trim();

        // 1. בדיקה שכל השדות מלאים
        if (TextUtils.isEmpty(fname) || TextUtils.isEmpty(lname) ||
                TextUtils.isEmpty(phone) || TextUtils.isEmpty(email) || TextUtils.isEmpty(password)) {
            Toast.makeText(this, "אנא מלא את כל השדות", Toast.LENGTH_SHORT).show();
            return;
        }

        // 2. בדיקת תקינות כתובת אימייל
        if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            etEmail.setError("כתובת אימייל לא תקינה");
            etEmail.requestFocus();
            return;
        }

        // 3. בדיקת אורך סיסמה (Firebase דורש לפחות 6 תווים)
        if (password.length() < 6) {
            etPassword.setError("הסיסמה חייבת להכיל לפחות 6 תווים");
            etPassword.requestFocus();
            return;
        }

        // 4. בדיקת תקינות מספר טלפון (בישראל זה 10 ספרות, או 9 נייח)
        if (phone.length() < 9 || phone.length() > 10 || !phone.matches("[0-9]+")) {
            etPhone.setError("מספר טלפון לא תקין");
            etPhone.requestFocus();
            return;
        }

        // השבתת הכפתור כדי למנוע לחיצות כפולות
        btnRegister.setEnabled(false);

        // 5. יצירת המשתמש ב-Firebase Auth קודם כל!
        mAuth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener(this, task -> {
                    if (task.isSuccessful()) {
                        // המשתמש נוצר בהצלחה במערכת ההזדהות, נשלוף את ה-ID שלו
                        FirebaseUser firebaseUser = mAuth.getCurrentUser();
                        if (firebaseUser != null) {
                            String uid = firebaseUser.getUid();

                            // ניצור את אובייקט המשתמש עם כל הפרטים למסד הנתונים
                            User user = new User(uid, fname, lname, phone, email, password, false);

                            // שומרים את הנתונים ב-Realtime Database
                            databaseService.saveUser(user, new DatabaseService.DatabaseCallback<Void>() {
                                @Override
                                public void onCompleted(Void object) {
                                    Toast.makeText(RegisterUser.this, "נרשמת בהצלחה!", Toast.LENGTH_SHORT).show();
                                    startActivity(new Intent(RegisterUser.this, HomePage.class));
                                    finish();
                                }

                                @Override
                                public void onFailed(Exception e) {
                                    btnRegister.setEnabled(true);
                                    Toast.makeText(RegisterUser.this, "שגיאה בשמירת נתונים: " + e.getMessage(), Toast.LENGTH_LONG).show();
                                }
                            });
                        }
                    } else {
                        // במקרה שהאימייל תפוס או שגיאה אחרת
                        btnRegister.setEnabled(true);
                        Toast.makeText(RegisterUser.this, "שגיאה בהרשמה: " + task.getException().getMessage(), Toast.LENGTH_LONG).show();
                    }
                });
    }
}