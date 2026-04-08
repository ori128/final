package com.ori.afinal;

import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.textfield.TextInputEditText;
import com.ori.afinal.Services.DatabaseService;
import com.ori.afinal.model.User;

public class UpdateUser extends AppCompatActivity {

    private TextInputEditText etFname, etLname, etPhone, etPassword;
    private TextView tvEmail;
    private Button btnSave;
    private ImageButton btnBack;

    private DatabaseService databaseService;
    private User currentUserToEdit;
    private String targetUserId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_update_user);

        databaseService = DatabaseService.getInstance();
        targetUserId = getIntent().getStringExtra("USER_ID");

        if (targetUserId == null) {
            Toast.makeText(this, "שגיאה בטעינת משתמש", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        initViews();
        loadUserData();
    }

    private void initViews() {
        tvEmail = findViewById(R.id.tv_update_user_email);
        etFname = findViewById(R.id.et_update_user_fname);
        etLname = findViewById(R.id.et_update_user_lname);
        etPhone = findViewById(R.id.et_update_user_phone);
        etPassword = findViewById(R.id.et_update_user_password);

        btnSave = findViewById(R.id.btn_save_user_updates);
        btnBack = findViewById(R.id.btn_back_update_user);

        btnBack.setOnClickListener(v -> finish());
        btnSave.setOnClickListener(v -> saveUserUpdates());
    }

    private void loadUserData() {
        databaseService.getUser(targetUserId, new DatabaseService.DatabaseCallback<User>() {
            @Override
            public void onCompleted(User user) {
                if (user != null) {
                    currentUserToEdit = user;
                    tvEmail.setText(user.getEmail() != null ? user.getEmail() : "אין אימייל");
                    etFname.setText(user.getFname());
                    etLname.setText(user.getLname());
                    etPhone.setText(user.getPhone());
                    etPassword.setText(user.getPassword());
                }
            }

            @Override
            public void onFailed(Exception e) {
                Toast.makeText(UpdateUser.this, "נכשל בטעינת נתונים", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void saveUserUpdates() {
        if (currentUserToEdit == null) return;

        String newFname = etFname.getText().toString().trim();
        String newLname = etLname.getText().toString().trim();
        String newPhone = etPhone.getText().toString().trim();
        String newPassword = etPassword.getText().toString().trim();

        if (newFname.isEmpty() || newLname.isEmpty() || newPhone.isEmpty() || newPassword.isEmpty()) {
            Toast.makeText(this, "נא למלא את כל השדות", Toast.LENGTH_SHORT).show();
            return;
        }

        currentUserToEdit.setFname(newFname);
        currentUserToEdit.setLname(newLname);
        currentUserToEdit.setPhone(newPhone);
        currentUserToEdit.setPassword(newPassword);

        databaseService.updateUser(currentUserToEdit, new DatabaseService.DatabaseCallback<Void>() {
            @Override
            public void onCompleted(Void object) {
                Toast.makeText(UpdateUser.this, "המשתמש עודכן בהצלחה", Toast.LENGTH_SHORT).show();
                finish(); // חזרה לעמוד האדמין
            }

            @Override
            public void onFailed(Exception e) {
                Toast.makeText(UpdateUser.this, "שגיאה בעדכון", Toast.LENGTH_SHORT).show();
            }
        });
    }
}