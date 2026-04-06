package com.ori.afinal;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.SearchView; // שים לב ל-Import הזה
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
import java.util.Locale;

public class HomePage extends AppCompatActivity {

    private static final String TAG = "HomePage";

    private TextView tvGreeting, tvStatsCount, tvStatsDuration, tvNotificationBadgeCount;
    private View cvNotificationBadge;
    private RecyclerView rvEvents;
    private FloatingActionButton fabAddEvent;
    private ImageButton btnLogout, btnNotifications;
    private SearchView svEvents; // משתנה לשורת החיפוש

    private EventAdapter eventAdapter;
    private DatabaseService databaseService;
    private FirebaseAuth mAuth;
    private String currentUserId;

    // רשימה שתשמור תמיד את כל הפגישות (כדי שנוכל לסנן מתוכה)
    private List<Event> fullEventsList = new ArrayList<>();

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
        loadNotificationsCount();
    }

    private void initViews() {
        tvGreeting = findViewById(R.id.tv_greeting);
        tvStatsCount = findViewById(R.id.tv_stats_count);
        tvStatsDuration = findViewById(R.id.tv_stats_duration);
        fabAddEvent = findViewById(R.id.fab_add_event);
        rvEvents = findViewById(R.id.rv_events);
        btnLogout = findViewById(R.id.btn_logout);
        btnNotifications = findViewById(R.id.btn_notifications);
        cvNotificationBadge = findViewById(R.id.cv_notification_badge);
        tvNotificationBadgeCount = findViewById(R.id.tv_notification_badge_count);
        svEvents = findViewById(R.id.sv_events); // אתחול החיפוש

        if (cvNotificationBadge != null) {
            cvNotificationBadge.setVisibility(View.GONE);
        }

        if (rvEvents != null) {
            rvEvents.setLayoutManager(new LinearLayoutManager(this));
            eventAdapter = new EventAdapter();
            eventAdapter.setCurrentUserId(currentUserId);
            rvEvents.setAdapter(eventAdapter);
        }

        // הגדרת מאזין לשורת החיפוש - מופעל בכל פעם שמוסיפים או מוחקים אות
        if (svEvents != null) {
            svEvents.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
                @Override
                public boolean onQueryTextSubmit(String query) {
                    return false;
                }

                @Override
                public boolean onQueryTextChange(String newText) {
                    filterEvents(newText);
                    return true;
                }
            });
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

        if (btnNotifications != null) {
            btnNotifications.setOnClickListener(v -> {
                Intent intent = new Intent(HomePage.this, NotificationsActivity.class);
                startActivity(intent);
            });
        }
    }

    private void filterEvents(String text) {
        // אם אין פגישות בכלל, אין מה לסנן
        if (fullEventsList == null || fullEventsList.isEmpty()) {
            return;
        }

        List<Event> filteredList = new ArrayList<>();

        for (Event event : fullEventsList) {
            boolean isMatch = false;

            // בדיקה האם כותרת הפגישה מכילה את טקסט החיפוש
            if (event.getTitle() != null && event.getTitle().toLowerCase().contains(text.toLowerCase())) {
                isMatch = true;
            }

            // בדיקה האם מיקום הפגישה מכיל את טקסט החיפוש
            if (event.getLocation() != null && event.getLocation().toLowerCase().contains(text.toLowerCase())) {
                isMatch = true;
            }

            if (isMatch) {
                filteredList.add(event);
            }
        }

        // עדכון הרשימה שמוצגת למשתמש
        if (eventAdapter != null) {
            eventAdapter.setEvents(filteredList);
        }
    }

    private void showLogoutDialog() {
        if (isFinishing() || isDestroyed()) return;

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
                if (isFinishing() || isDestroyed()) return;

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
                if (isFinishing() || isDestroyed()) return;

                if (events == null) return;

                fullEventsList.clear(); // מנקים את הרשימה המלאה הישנה
                double totalDuration = 0;

                for (Event event : events) {
                    fullEventsList.add(event); // שומרים את הפגישה ברשימה המלאה
                    totalDuration += event.getParticipationHours();
                }

                // הפעלת החיפוש מחדש במקרה שהמשתמש השאיר טקסט בתיבה
                String currentQuery = svEvents != null ? svEvents.getQuery().toString() : "";
                filterEvents(currentQuery);

                if (tvStatsCount != null) {
                    tvStatsCount.setText(String.valueOf(fullEventsList.size()));
                }

                if (tvStatsDuration != null) {
                    String durationText;
                    if (totalDuration == (long) totalDuration) {
                        durationText = String.format(Locale.getDefault(), "%dh", (long) totalDuration);
                    } else {
                        durationText = String.format(Locale.getDefault(), "%.1fh", totalDuration);
                    }
                    tvStatsDuration.setText(durationText);
                }
            }

            @Override
            public void onFailed(Exception e) {
                Log.e(TAG, "Failed to load events", e);
            }
        });
    }

    private void loadNotificationsCount() {
        databaseService.getUserNotifications(currentUserId, new DatabaseService.DatabaseCallback<List<Event>>() {
            @Override
            public void onCompleted(List<Event> pendingEvents) {
                if (isFinishing() || isDestroyed()) return;

                if (pendingEvents != null && !pendingEvents.isEmpty()) {
                    cvNotificationBadge.setVisibility(View.VISIBLE);
                    tvNotificationBadgeCount.setText(String.valueOf(pendingEvents.size()));
                } else {
                    cvNotificationBadge.setVisibility(View.GONE);
                }
            }

            @Override
            public void onFailed(Exception e) {
                Log.e(TAG, "Failed to load notifications count", e);
                if (cvNotificationBadge != null) {
                    cvNotificationBadge.setVisibility(View.GONE);
                }
            }
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadEventsData();
        loadNotificationsCount();
    }
}