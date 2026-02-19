package com.ori.afinal;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.widget.ImageButton;
import android.widget.TextView;

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
    private ImageButton btnLogout, btnNotifications; // הוספנו את כפתור ההתראות

    private EventAdapter eventAdapter;
    private DatabaseService databaseService;
    private FirebaseAuth mAuth;
    private String currentUserId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_home_page);

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        databaseService = DatabaseService.getInstance();
        mAuth = FirebaseAuth.getInstance();

        if (mAuth.getCurrentUser() != null) {
            currentUserId = mAuth.getCurrentUser().getUid();
        } else {
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
        btnLogout = findViewById(R.id.btn_logout);
        btnNotifications = findViewById(R.id.btn_notifications); // קישור לעיצוב

        if (rvEvents != null) {
            rvEvents.setLayoutManager(new LinearLayoutManager(this));
            eventAdapter = new EventAdapter();
            rvEvents.setAdapter(eventAdapter);
        }

        if (fabAddEvent != null) {
            fabAddEvent.setOnClickListener(v -> {
                Intent intent = new Intent(HomePage.this, AddEvent.class);
                startActivity(intent);
            });
        }

        if (btnLogout != null) {
            btnLogout.setOnClickListener(v -> showLogoutDialog());
        }

        // לחיצה על כפתור הפעמון
        if (btnNotifications != null) {
            btnNotifications.setOnClickListener(v -> {
                Intent intent = new Intent(HomePage.this, NotificationsActivity.class);
                startActivity(intent);
            });
        }
    }

    private void showLogoutDialog() {
        new androidx.appcompat.app.AlertDialog.Builder(this)
                .setTitle("התנתקות")
                .setMessage("האם אתה בטוח שברצונך להתנתק?")
                .setPositiveButton("כן, התנתק", (dialog, which) -> {
                    mAuth.signOut();
                    Intent intent = new Intent(HomePage.this, MainActivity.class);
                    intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                    startActivity(intent);
                    finish();
                })
                .setNegativeButton("ביטול", null)
                .show();
    }

    private void loadUserData() {
        databaseService.getUser(currentUserId, new DatabaseService.DatabaseCallback<User>() {
            @Override
            public void onCompleted(User user) {
                if (user != null && user.getFname() != null && tvGreeting != null) {
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
        databaseService.getUserEvents(currentUserId, new DatabaseService.DatabaseCallback<List<Event>>() {
            @Override
            public void onCompleted(List<Event> events) {
                if (events == null) return;

                List<Event> myEvents = new ArrayList<>();
                int totalDuration = 0;

                for (Event event : events) {
                    myEvents.add(event);
                    totalDuration += 1;
                }

                if (eventAdapter != null) {
                    eventAdapter.setEvents(myEvents);
                }

                if (tvStatsCount != null) {
                    tvStatsCount.setText(String.valueOf(myEvents.size()));
                }
                if (tvStatsDuration != null) {
                    tvStatsDuration.setText(totalDuration + "h");
                }
            }

            @Override
            public void onFailed(Exception e) {
                Log.e(TAG, "Failed to load events", e);
            }
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadEventsData();
    }
}