package com.ori.afinal;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

public class EventControl extends AppCompatActivity implements View.OnClickListener {

    private Button btnUpdate, btnChange, btnCancel, btnBack;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_event_control);
        
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        // אתחול הכפתורים
        btnUpdate = findViewById(R.id.btnUpdateEvent);
        btnChange = findViewById(R.id.btnChangeEvent);
        btnCancel = findViewById(R.id.btnCancelEvent);
        btnBack = findViewById(R.id.btnBack);

        // הגדרת מאזינים
        btnUpdate.setOnClickListener(this);
        btnChange.setOnClickListener(this);
        btnCancel.setOnClickListener(this);
        btnBack.setOnClickListener(this);
    }

    @Override
    public void onClick(View v) {
        int id = v.getId();

        if (id == R.id.btnUpdateEvent) {
            Intent intent = new Intent(this, UpdateEvent.class);
            startActivity(intent);
        } else if (id == R.id.btnChangeEvent) {
            // כאן תוכל להוסיף מעבר לעמוד שינוי מועד
            // Intent intent = new Intent(this, ChangeEventDateActivity.class);
            // startActivity(intent);
        } else if (id == R.id.btnCancelEvent) {
            // כאן תוכל להוסיף מעבר לעמוד ביטול
            // Intent intent = new Intent(this, CancelEventActivity.class);
            // startActivity(intent);
        } else if (id == R.id.btnBack) {
            finish(); // חוזר לעמוד הקודם
        }
    }
}
