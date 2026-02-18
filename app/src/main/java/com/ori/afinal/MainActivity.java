package com.ori.afinal;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

public class MainActivity extends AppCompatActivity {

    private Button btnLogin, btnRegister, btnAbout;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        initViews();
    }

    private void initViews() {
        btnLogin = findViewById(R.id.btn_login_nav);
        btnRegister = findViewById(R.id.btn_register_nav);
        btnAbout = findViewById(R.id.btn_about_nav);

        // מעבר למסך התחברות
        btnLogin.setOnClickListener(v -> {
            startActivity(new Intent(MainActivity.this, Login.class));
        });

        // מעבר למסך הרשמה
        btnRegister.setOnClickListener(v -> {
            startActivity(new Intent(MainActivity.this, RegisterUser.class));
        });

        // פתיחת חלונית אודות
        btnAbout.setOnClickListener(v -> {
            new AlertDialog.Builder(this)
                    .setTitle("אודות האפליקציה")
                    .setMessage("אפליקציה לניהול פגישות חכם.\n\nפותחה על ידי: אורי\nגרסה: 1.0\nשנה: 2026")
                    .setPositiveButton("סגור", null)
                    .setIcon(android.R.drawable.ic_dialog_info)
                    .show();
        });
    }
}