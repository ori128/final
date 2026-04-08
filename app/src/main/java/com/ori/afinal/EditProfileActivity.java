package com.ori.afinal;

import android.app.AlertDialog;
import android.os.Bundle;
import android.text.TextUtils;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.EmailAuthProvider;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.ori.afinal.Services.DatabaseService;
import com.ori.afinal.model.User;

public class EditProfileActivity extends AppCompatActivity {

    private ImageButton btnBack, btnEmailInfo;
    private TextView tvEmailDisplay;
    private TextInputEditText etFname, etLname, etPhone, etPassword;
    private MaterialButton btnSave;

    private DatabaseService databaseService;
    private FirebaseAuth mAuth;
    private FirebaseUser currentFirebaseUser;
    private User currentUserObj;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_edit_profile);

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.rl_header), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(0, systemBars.top, 0, 0);
            return insets;
        });

        databaseService = DatabaseService.getInstance();
        mAuth = FirebaseAuth.getInstance();
        currentFirebaseUser = mAuth.getCurrentUser();

        if (currentFirebaseUser == null) {
            finish();
            return;
        }

        initViews();
        loadData();
    }

    private void initViews() {
        btnBack = findViewById(R.id.btn_back_edit);
        tvEmailDisplay = findViewById(R.id.tv_email_display);
        btnEmailInfo = findViewById(R.id.btn_email_info);

        etFname = findViewById(R.id.et_edit_fname);
        etLname = findViewById(R.id.et_edit_lname);
        etPhone = findViewById(R.id.et_edit_phone);
        etPassword = findViewById(R.id.et_edit_password);
        btnSave = findViewById(R.id.btn_save_changes);

        btnBack.setOnClickListener(v -> finish());

        // חלונית ההסבר על האימייל
        btnEmailInfo.setOnClickListener(v -> {
            new AlertDialog.Builder(EditProfileActivity.this)
                    .setTitle("שינוי אימייל")
                    .setMessage("לא ניתן לשנות את כתובת האימייל מטעמי אבטחה של המערכת.")
                    .setPositiveButton("הבנתי", null)
                    .show();
        });

        btnSave.setOnClickListener(v -> saveChanges());
    }

    private void loadData() {
        databaseService.getUser(currentFirebaseUser.getUid(), new DatabaseService.DatabaseCallback<User>() {
            @Override
            public void onCompleted(User user) {
                if (user != null) {
                    currentUserObj = user;
                    etFname.setText(user.getFname() != null ? user.getFname() : "");
                    etLname.setText(user.getLname() != null ? user.getLname() : "");
                    etPhone.setText(user.getPhone() != null ? user.getPhone() : "");
                    tvEmailDisplay.setText(user.getEmail() != null ? user.getEmail() : currentFirebaseUser.getEmail());
                    etPassword.setText(user.getPassword() != null ? user.getPassword() : "");
                }
            }

            @Override
            public void onFailed(Exception e) {
                Toast.makeText(EditProfileActivity.this, "שגיאה בטעינת נתונים", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void saveChanges() {
        if (currentUserObj == null) return;

        String newFname = etFname.getText().toString().trim();
        String newLname = etLname.getText().toString().trim();
        String newPhone = etPhone.getText().toString().trim();
        String newPassword = etPassword.getText().toString().trim();
        String existingEmail = tvEmailDisplay.getText().toString();

        if (TextUtils.isEmpty(newFname) || TextUtils.isEmpty(newPassword)) {
            Toast.makeText(this, "שם וסיסמה הם שדות חובה", Toast.LENGTH_SHORT).show();
            return;
        }

        btnSave.setEnabled(false);

        boolean passwordChanged = !newPassword.equals(currentUserObj.getPassword());

        if (passwordChanged) {
            String authEmail = currentFirebaseUser.getEmail();
            String currentPassword = currentUserObj.getPassword();

            if (authEmail != null && currentPassword != null && !currentPassword.isEmpty()) {
                AuthCredential credential = EmailAuthProvider.getCredential(authEmail, currentPassword);
                currentFirebaseUser.reauthenticate(credential).addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        currentFirebaseUser.updatePassword(newPassword).addOnCompleteListener(task2 -> {
                            if (task2.isSuccessful()) {
                                updateDatabase(newFname, newLname, newPhone, existingEmail, newPassword);
                            } else {
                                btnSave.setEnabled(true);
                                Toast.makeText(this, "שגיאה בעדכון הסיסמה.", Toast.LENGTH_LONG).show();
                            }
                        });
                    } else {
                        btnSave.setEnabled(true);
                        Toast.makeText(EditProfileActivity.this, "אימות נכשל. ודא שהסיסמה הקודמת נכונה.", Toast.LENGTH_LONG).show();
                    }
                });
            } else {
                btnSave.setEnabled(true);
                Toast.makeText(this, "חסרה סיסמה קיימת כדי לשנות סיסמה.", Toast.LENGTH_LONG).show();
            }
        } else {
            // רק שמות או טלפון השתנו
            updateDatabase(newFname, newLname, newPhone, existingEmail, newPassword);
        }
    }

    private void updateDatabase(String fname, String lname, String phone, String email, String password) {
        currentUserObj.setFname(fname);
        currentUserObj.setLname(lname);
        currentUserObj.setPhone(phone);
        currentUserObj.setEmail(email);
        currentUserObj.setPassword(password);

        // אם הוספת fullName למודל User בהודעות הקודמות, אפשר גם לעדכן אותו
        if (currentUserObj.getFullName() != null || true) {
            currentUserObj.setFullName(fname + " " + lname);
        }

        databaseService.updateUser(currentUserObj, new DatabaseService.DatabaseCallback<Void>() {
            @Override
            public void onCompleted(Void object) {
                Toast.makeText(EditProfileActivity.this, "הפרטים עודכנו בהצלחה!", Toast.LENGTH_SHORT).show();
                finish();
            }

            @Override
            public void onFailed(Exception e) {
                btnSave.setEnabled(true);
                Toast.makeText(EditProfileActivity.this, "שגיאה בשמירת הנתונים", Toast.LENGTH_SHORT).show();
            }
        });
    }
}