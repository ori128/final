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

public class Login extends AppCompatActivity {

    // כאן אני מגדיר את משתני התצוגה - שדות הקלט לכניסה והכפתורים
    private EditText etEmail, etPassword;
    private Button btnLogin;
    private TextView tvRegister;
    private ImageButton btnBackMain;

    // מופע של מערכת האימות של פיירבייס שאיתו אני אבצע את הלוגין
    private FirebaseAuth mAuth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_login);

        // סידור שוליים (Edge to Edge) כדי שהאפליקציה תיראה טוב על כל המסך
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        // אני מאתחל את שירות האימות של פיירבייס וקורא לפונקציה שמקשרת את כפתורי המסך
        mAuth = FirebaseAuth.getInstance();
        initViews();
    }

    // פונקציה שמקשרת בין קוד ה-Java לעיצוב ב-XML ומגדירה לחיצות
    private void initViews() {
        etEmail = findViewById(R.id.et_email);
        etPassword = findViewById(R.id.et_password);
        btnLogin = findViewById(R.id.btn_login);
        tvRegister = findViewById(R.id.tv_register);
        btnBackMain = findViewById(R.id.btn_back_main);

        // לחיצה על כפתור ההתחברות מפעילה את פונקציית הלוגין
        btnLogin.setOnClickListener(v -> loginUser());

        // אם למשתמש אין חשבון, הוא ילחץ פה ויעבור למסך ההרשמה
        tvRegister.setOnClickListener(v -> {
            Intent intent = new Intent(Login.this, RegisterUser.class);
            startActivity(intent);
            finish(); // סוגר את הלוגין כדי שלא יישאר סתם ברקע
        });

        // כפתור חזור פשוט סוגר את המסך ומחזיר למסך הראשי
        btnBackMain.setOnClickListener(v -> {
            finish();
        });
    }

    // הפונקציה המרכזית שעושה את ההתחברות בפועל
    private void loginUser() {
        String email = etEmail.getText().toString();
        String password = etPassword.getText().toString();

        // בדיקה בסיסית שהמשתמש בכלל הזין משהו ולא לחץ על הכפתור סתם
        if (TextUtils.isEmpty(email) || TextUtils.isEmpty(password)) {
            Toast.makeText(this, "אנא מלא את כל השדות", Toast.LENGTH_SHORT).show();
            return;
        }

        // פה קורה הקסם - אני שולח לפיירבייס בקשה לאמת את האימייל והסיסמה
        mAuth.signInWithEmailAndPassword(email, password).addOnCompleteListener(task -> {
            if (task.isSuccessful() && mAuth.getCurrentUser() != null) {
                // התחברות ל-Auth הצליחה!
                // כעת אני מושך את ה-ID של המשתמש שהתחבר
                String currentUserId = mAuth.getCurrentUser().getUid();

                // אני ניגש למסד הנתונים שלי (Realtime Database) ובודק את פרטי המשתמש
                // המטרה שלי פה היא לבדוק האם הוא משתמש רגיל או מנהל (Admin)
                DatabaseService.getInstance().getUser(currentUserId, new DatabaseService.DatabaseCallback<User>() {
                    @Override
                    public void onCompleted(User user) {
                        if (user != null && Boolean.TRUE.equals(user.getAdmin())) {
                            // המשתמש הוא מנהל - אני מעביר אותו לדף הבית (הכפתור המיוחד של מנהלים יופיע שם בזכות זה)
                            startActivity(new Intent(Login.this, HomePage.class));
                        } else {
                            // משתמש רגיל - עובר גם כן לדף הבית
                            startActivity(new Intent(Login.this, HomePage.class));
                        }
                        finish(); // סוגר את דף הלוגין לתמיד
                    }

                    @Override
                    public void onFailed(Exception e) {
                        Toast.makeText(Login.this, "שגיאה בשליפת נתוני משתמש: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    }
                });

            } else {
                // אם ההתחברות נכשלה (סיסמה שגויה למשל), נציג שגיאה למשתמש
                Toast.makeText(Login.this, "התחברות נכשלה: " + task.getException().getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }
}