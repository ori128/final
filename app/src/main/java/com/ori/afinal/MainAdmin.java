package com.ori.afinal;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;

public class MainAdmin extends AppCompatActivity {

    private Button btnLogout;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main_admin);

        btnLogout = findViewById(R.id.btn_logout_admin);

        // מאזין ללחיצה על כפתור ההתנתקות
        btnLogout.setOnClickListener(v -> {
            FirebaseAuth.getInstance().signOut(); // ניתוק מהפיירבייס
            Intent intent = new Intent(MainAdmin.this, Login.class);
            // מנקה את היסטוריית המסכים כדי שלא יוכל לחזור אחורה עם כפתור החזור בטלפון
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
            finish();
        });
    }
}