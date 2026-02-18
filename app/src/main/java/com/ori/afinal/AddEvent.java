package com.ori.afinal;

import android.os.Bundle;
import android.text.TextUtils;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.firebase.auth.FirebaseAuth;
import com.ori.afinal.Services.DatabaseService;
import com.ori.afinal.model.Event;

public class AddEvent extends AppCompatActivity {

    private EditText etTitle, etLocation, etDescription, etDate, etTime;
    private Button btnSave, btnBack;

    private DatabaseService databaseService;
    private FirebaseAuth mAuth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_add_event);

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        databaseService = DatabaseService.getInstance();
        mAuth = FirebaseAuth.getInstance();

        initViews();
    }

    private void initViews() {
        etTitle = findViewById(R.id.et_title);
        etLocation = findViewById(R.id.et_location);
        etDescription = findViewById(R.id.et_description);
        etDate = findViewById(R.id.et_date);
        etTime = findViewById(R.id.et_time);

        btnSave = findViewById(R.id.btn_save_event);
        btnBack = findViewById(R.id.btn_back);

        btnSave.setOnClickListener(v -> saveEvent());
        btnBack.setOnClickListener(v -> finish());
    }

    private void saveEvent() {
        String title = etTitle.getText().toString();
        String location = etLocation.getText().toString();
        String desc = etDescription.getText().toString();
        String date = etDate.getText().toString();
        String time = etTime.getText().toString();

        if (TextUtils.isEmpty(title) || TextUtils.isEmpty(date) || TextUtils.isEmpty(time)) {
            Toast.makeText(this, "אנא מלא את שדות החובה (כותרת, תאריך ושעה)", Toast.LENGTH_SHORT).show();
            return;
        }

        // יצירת מזהה ייחודי
        String eventId = databaseService.generateEventId();
        String userId = mAuth.getCurrentUser() != null ? mAuth.getCurrentUser().getUid() : "";

        // יצירת אובייקט אירוע
        // הערה: אני מניח שיש בנאי כזה ב-Event. אם אין, תצטרך לעדכן את המודל או להשתמש ב-Setters
        // שילוב תאריך ושעה למחרוזת אחת
        String dateTime = date + " " + time;

        Event event = new Event();
        event.setId(eventId);
        event.setTitle(title);
        event.setLocation(location);
        event.setDescription(desc);
        event.setDateTime(dateTime);
        event.setType("Meeting"); // ברירת מחדל
        // event.setOwnerId(userId); // אם יש שדה כזה במודל שלך

        databaseService.createNewEvent(event, new DatabaseService.DatabaseCallback<Void>() {
            @Override
            public void onCompleted(Void object) {
                Toast.makeText(AddEvent.this, "הפגישה נוצרה בהצלחה!", Toast.LENGTH_SHORT).show();
                finish(); // חוזר למסך הבית שיתעדכן אוטומטית
            }

            @Override
            public void onFailed(Exception e) {
                Toast.makeText(AddEvent.this, "שגיאה ביצירת פגישה", Toast.LENGTH_SHORT).show();
            }
        });
    }
}