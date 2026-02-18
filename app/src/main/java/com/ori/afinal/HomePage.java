package com.ori.afinal;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.firebase.auth.FirebaseAuth;
import com.ori.afinal.Services.DatabaseService;
import com.ori.afinal.adapter.EventAdapter;
import com.ori.afinal.model.Event;
import com.ori.afinal.model.User;

import java.util.ArrayList;
import java.util.List;

public class HomePage extends AppCompatActivity {

    private static final String TAG = "HomePage";

    private TextView tvGreeting, tvStatsCount, tvStatsDuration;
    private RecyclerView rvEvents;
    private FloatingActionButton fabAddEvent;

    private EventAdapter eventAdapter;
    private DatabaseService databaseService;
    private FirebaseAuth mAuth;
    private String currentUserId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_home_page);

        // טיפול ב-Padding של המערכת (סטטוס בר וכו')
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        // אתחול שירותים
        databaseService = DatabaseService.getInstance();
        mAuth = FirebaseAuth.getInstance();

        if (mAuth.getCurrentUser() != null) {
            currentUserId = mAuth.getCurrentUser().getUid();
        } else {
            // אם אין משתמש מחובר, חזור למסך התחברות
            finish();
            return;
        }

        initViews();
        loadUserData();
        loadEventsData();
    }

    private void initViews() {
        tvGreeting = findViewById(R.id.tv_greeting);
        tvStatsCount = findViewById(R.id.tv_stats_count);
        tvStatsDuration = findViewById(R.id.tv_stats_duration);
        fabAddEvent = findViewById(R.id.fab_add_event);
        rvEvents = findViewById(R.id.rv_events);

        // הגדרת הרשימה
        rvEvents.setLayoutManager(new LinearLayoutManager(this));
        eventAdapter = new EventAdapter();
        rvEvents.setAdapter(eventAdapter);

        // כפתור הוספת פגישה (הכפתור העגול למטה)
        fabAddEvent.setOnClickListener(v -> {
            Intent intent = new Intent(HomePage.this, AddEvent.class);
            startActivity(intent);
        });
    }

    private void loadUserData() {
        // טעינת שם המשתמש לכותרת
        databaseService.getUser(currentUserId, new DatabaseService.DatabaseCallback<User>() {
            @Override
            public void onCompleted(User user) {
                if (user != null && user.getFname() != null) {
                    tvGreeting.setText("שלום, " + user.getFname());
                }
            }

            @Override
            public void onFailed(Exception e) {
                Log.e(TAG, "Failed to load user", e);
            }
        });
    }

    private void loadEventsData() {
        // טעינת כל האירועים מהדאטהבייס
        databaseService.getEventList(new DatabaseService.DatabaseCallback<List<Event>>() {
            @Override
            public void onCompleted(List<Event> events) {
                if (events == null) return;

                // יצירת רשימה מקומית
                List<Event> myEvents = new ArrayList<>();
                int totalDuration = 0; // מונה שעות

                for (Event event : events) {
                    // כאן הוספתי את האירוע לרשימה
                    // בעתיד נוכל לסנן שרק אירועים שאני מוזמן אליהם יופיעו
                    myEvents.add(event);

                    // חישוב דמה: מוסיף שעה אחת על כל פגישה לסטטיסטיקה
                    totalDuration += 1;
                }

                // עדכון ה-UI עם הרשימה החדשה
                eventAdapter.setEvents(myEvents);

                // עדכון המספרים בקוביות למעלה
                tvStatsCount.setText(String.valueOf(myEvents.size()));
                tvStatsDuration.setText(totalDuration + "h");
            }

            @Override
            public void onFailed(Exception e) {
                Log.e(TAG, "Failed to load events", e);
                Toast.makeText(HomePage.this, "שגיאה בטעינת פגישות", Toast.LENGTH_SHORT).show();
            }
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        // רענון הרשימה כשחוזרים ממסך יצירת פגישה
        loadEventsData();
    }
}