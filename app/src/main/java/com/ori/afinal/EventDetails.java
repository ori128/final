package com.ori.afinal;

import android.os.Bundle;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.ori.afinal.Services.DatabaseService;
import com.ori.afinal.model.Event;
import com.ori.afinal.model.User;

import java.util.List;

public class EventDetails extends AppCompatActivity {

    private TextView tvTitle, tvType, tvDateTime, tvLocation, tvDescription, tvParticipants;
    private Button btnBackHome;
    private DatabaseService databaseService;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_event_details);

        databaseService = DatabaseService.getInstance();

        // קישור משתנים לעיצוב
        tvTitle = findViewById(R.id.tv_detail_title);
        tvType = findViewById(R.id.tv_detail_type);
        tvDateTime = findViewById(R.id.tv_detail_datetime);
        tvLocation = findViewById(R.id.tv_detail_location);
        tvDescription = findViewById(R.id.tv_detail_description);
        tvParticipants = findViewById(R.id.tv_detail_participants);
        btnBackHome = findViewById(R.id.btn_back_home);

        // כפתור חזרה לעמוד הבית
        btnBackHome.setOnClickListener(v -> finish());

        // קבלת מזהה הפגישה מהעמוד הקודם
        String eventId = getIntent().getStringExtra("EVENT_ID");

        if (eventId != null) {
            loadEventData(eventId);
        } else {
            Toast.makeText(this, "שגיאה בטעינת הפגישה", Toast.LENGTH_SHORT).show();
            finish();
        }
    }

    private void loadEventData(String eventId) {
        databaseService.getEvent(eventId, new DatabaseService.DatabaseCallback<Event>() {
            @Override
            public void onCompleted(Event event) {
                if (event != null) {
                    tvTitle.setText(event.getTitle() != null ? event.getTitle() : "ללא כותרת");
                    tvType.setText(event.getType() != null ? event.getType() : "סוג לא ידוע");
                    tvDateTime.setText(event.getDateTime() != null ? event.getDateTime() : "לא צוין זמן");
                    tvLocation.setText(event.getLocation() != null ? event.getLocation() : "לא צוין מיקום");

                    if (event.getDescription() != null && !event.getDescription().isEmpty()) {
                        tvDescription.setText(event.getDescription());
                    } else {
                        tvDescription.setText("אין תיאור לפגישה זו.");
                    }

                    loadParticipantsNames(event.getParticipantIds());
                }
            }

            @Override
            public void onFailed(Exception e) {
                Toast.makeText(EventDetails.this, "שגיאה בטעינת נתונים", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void loadParticipantsNames(List<String> participantIds) {
        if (participantIds == null || participantIds.isEmpty()) {
            tvParticipants.setText("אין משתתפים.");
            return;
        }

        databaseService.getUserList(new DatabaseService.DatabaseCallback<List<User>>() {
            @Override
            public void onCompleted(List<User> users) {
                if (users == null) return;

                StringBuilder namesBuilder = new StringBuilder();
                for (User user : users) {
                    if (participantIds.contains(user.getId())) {
                        String name = user.getFname() != null ? user.getFname() : "משתמש";
                        namesBuilder.append("• ").append(name).append("\n");
                    }
                }
                tvParticipants.setText(namesBuilder.toString().trim());
            }

            @Override
            public void onFailed(Exception e) {
                tvParticipants.setText("שגיאה בטעינת המשתתפים");
            }
        });
    }
}