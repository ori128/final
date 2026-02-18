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

        rvEvents.setLayoutManager(new LinearLayoutManager(this));
        eventAdapter = new EventAdapter();
        rvEvents.setAdapter(eventAdapter);

        fabAddEvent.setOnClickListener(v -> {
            Intent intent = new Intent(HomePage.this, AddEvent.class);
            startActivity(intent);
        });
    }

    private void loadUserData() {
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
        databaseService.getEventList(new DatabaseService.DatabaseCallback<List<Event>>() {
            @Override
            public void onCompleted(List<Event> events) {
                if (events == null) return;

                List<Event> myEvents = new ArrayList<>();
                int totalDuration = 0;

                for (Event event : events) {
                    myEvents.add(event);
                    totalDuration += 1;
                }

                eventAdapter.setEvents(myEvents);
                tvStatsCount.setText(String.valueOf(myEvents.size()));
                tvStatsDuration.setText(totalDuration + "h");
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