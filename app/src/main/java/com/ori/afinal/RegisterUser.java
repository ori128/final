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

    // כאן אני מגדיר את כל השדות שהמשתמש צריך למלא כדי להירשם
    private EditText etFname, etLname, etPhone, etEmail, etPassword;
    private Button btnRegister;
    private TextView tvLogin;
    private ImageButton btnBackMain;

    // אובייקטים לשמירת המידע בשרת ולאימות (הרשמה מול פיירבייס)
    private DatabaseService databaseService;
    private FirebaseAuth mAuth; // הוספנו את מערכת ההזדהות של פיירבייס

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_register_user);

        // סידור השוליים של המסך
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        // אתחול החיבורים לפיירבייס וקריאה לפונקציית הגדרת המסך
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

        // לחיצה על הרשמה מפעילה את הפונקציה המרכזית של המסך
        btnRegister.setOnClickListener(v -> registerUser());

        // אם המשתמש נזכר שכבר יש לו חשבון, הוא עובר לעמוד ההתחברות
        tvLogin.setOnClickListener(v -> {
            Intent intent = new Intent(RegisterUser.this, Login.class);
            startActivity(intent);
            finish();
        });

        btnBackMain.setOnClickListener(v -> {
            finish();
        });
    }

    // הפונקציה שבודקת את הנתונים ויוצרת את המשתמש
    private void registerUser() {
        // אני שואב את כל מה שהמשתמש הקליד ומוריד רווחים מיותרים עם trim()
        String fname = etFname.getText().toString().trim();
        String lname = etLname.getText().toString().trim();
        String phone = etPhone.getText().toString().trim();
        String email = etEmail.getText().toString().trim();
        String password = etPassword.getText().toString().trim();

        // 1. בדיקה (ולידציה) שכל השדות באמת מלאים ולא הושארו ריקים
        if (TextUtils.isEmpty(fname) || TextUtils.isEmpty(lname) ||
                TextUtils.isEmpty(phone) || TextUtils.isEmpty(email) || TextUtils.isEmpty(password)) {
            Toast.makeText(this, "אנא מלא את כל השדות", Toast.LENGTH_SHORT).show();
            return;
        }

        // 2. בדיקת תקינות כתובת אימייל (שהיא באמת בפורמט של אימייל, עם @ וכו')
        if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            etEmail.setError("כתובת אימייל לא תקינה");
            etEmail.requestFocus(); // מקפיץ את הסמן חזרה לשדה האימייל
            return;
        }

        // 3. בדיקת אורך סיסמה (מערכת Firebase דורשת מינימום 6 תווים, אז אני חוסם את זה מראש)
        if (password.length() < 6) {
            etPassword.setError("הסיסמה חייבת להכיל לפחות 6 תווים");
            etPassword.requestFocus();
            return;
        }

        // 4. בדיקת תקינות מספר טלפון (בישראל זה 10 ספרות, או 9 בטלפון נייח)
        if (phone.length() < 9 || phone.length() > 10 || !phone.matches("[0-9]+")) {
            etPhone.setError("מספר טלפון לא תקין");
            etPhone.requestFocus();
            return;
        }

        // השבתת הכפתור כדי למנוע לחיצות כפולות שעלולות לייצר שתי קריאות לשרת
        btnRegister.setEnabled(false);

        // 5. זה השלב הקריטי: קודם כל אני יוצר את המשתמש ב-Firebase Auth (מערכת האימות)
        mAuth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener(this, task -> {
                    if (task.isSuccessful()) {
                        // המשתמש נוצר בהצלחה במערכת ההזדהות. עכשיו אשלוף את ה-ID הייחודי שפיירבייס נתן לו
                        FirebaseUser firebaseUser = mAuth.getCurrentUser();
                        if (firebaseUser != null) {
                            String uid = firebaseUser.getUid();

                            // אני יוצר אובייקט מסוג User (מודל שבניתי) שמכיל את כל הפרטים הנוספים שלו
                            // פרמטר ה-false בסוף אומר שכברירת מחדל, משתמש חדש הוא לא Admin.
                            User user = new User(uid, fname, lname, phone, email, password, false);

                            // שלב אחרון: שומרים את האובייקט המלא במסד הנתונים שלי (Realtime Database)
                            databaseService.saveUser(user, new DatabaseService.DatabaseCallback<Void>() {
                                @Override
                                public void onCompleted(Void object) {
                                    Toast.makeText(RegisterUser.this, "נרשמת בהצלחה!", Toast.LENGTH_SHORT).show();
                                    // אחרי שהכל נשמר, אני מעביר אותו ישר למסך הבית!
                                    startActivity(new Intent(RegisterUser.this, HomePage.class));
                                    finish();
                                }

                                @Override
                                public void onFailed(Exception e) {
                                    // אם השמירה למסד הנתונים נכשלה מאיזושהי סיבה, אשחרר את הכפתור ואציג שגיאה
                                    btnRegister.setEnabled(true);
                                    Toast.makeText(RegisterUser.this, "שגיאה בשמירת נתונים: " + e.getMessage(), Toast.LENGTH_LONG).show();
                                }
                            });
                        }
                    } else {
                        // אם יצירת היוזר ב-Auth נכשלה (למשל, האימייל כבר רשום במערכת)
                        btnRegister.setEnabled(true);
                        Toast.makeText(RegisterUser.this, "שגיאה בהרשמה: " + task.getException().getMessage(), Toast.LENGTH_LONG).show();
                    }
                });
    }
}