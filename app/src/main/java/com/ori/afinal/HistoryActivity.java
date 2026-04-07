package com.ori.afinal;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.SearchView;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.auth.FirebaseAuth;
import com.ori.afinal.Services.DatabaseService;
import com.ori.afinal.adapter.EventAdapter;
import com.ori.afinal.model.Event;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class HistoryActivity extends AppCompatActivity {

    private static final String TAG = "HistoryActivity";

    private RecyclerView rvHistory;
    private SearchView svHistory;
    private View llEmptyState;
    private EventAdapter adapter;

    private ImageButton navUpcoming, navHistory, navProgress, navNotifications, navAdd;
    private View cvNotificationBadge;
    private TextView tvNotificationBadgeCount;

    private DatabaseService databaseService;
    private FirebaseAuth mAuth;
    private String currentUserId;

    private List<Event> pastEventsList = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_history);

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.rv_history), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom + 100);
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
        setupBottomNavigation();
        loadHistoryEvents();
        loadNotificationsCount();
    }

    private void initViews() {
        rvHistory = findViewById(R.id.rv_history);
        svHistory = findViewById(R.id.sv_history);
        llEmptyState = findViewById(R.id.ll_empty_state);

        navUpcoming = findViewById(R.id.nav_upcoming);
        navHistory = findViewById(R.id.nav_history);
        navProgress = findViewById(R.id.nav_progress);
        navNotifications = findViewById(R.id.nav_notifications);
        navAdd = findViewById(R.id.nav_add);
        cvNotificationBadge = findViewById(R.id.cv_notification_badge);
        tvNotificationBadgeCount = findViewById(R.id.tv_notification_badge_count);

        com.airbnb.lottie.LottieAnimationView lottieEmpty = findViewById(R.id.lottie_empty);
        if (lottieEmpty != null) {
            lottieEmpty.setFontAssetDelegate(new com.airbnb.lottie.FontAssetDelegate() {
                @Override
                public android.graphics.Typeface fetchFont(String fontFamily) {
                    return android.graphics.Typeface.DEFAULT_BOLD;
                }
            });
        }

        rvHistory.setLayoutManager(new LinearLayoutManager(this));
        adapter = new EventAdapter();
        adapter.setCurrentUserId(currentUserId);
        rvHistory.setAdapter(adapter);

        svHistory.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) { return false; }
            @Override
            public boolean onQueryTextChange(String newText) {
                filterEvents(newText);
                return true;
            }
        });
    }

    private void setupBottomNavigation() {
        navUpcoming.setOnClickListener(v -> {
            Intent intent = new Intent(this, HomePage.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
            startActivity(intent);
            finish();
        });

        navProgress.setOnClickListener(v -> {
            Intent intent = new Intent(this, ProgressActivity.class);
            startActivity(intent);
            finish();
        });

        navNotifications.setOnClickListener(v -> {
            Intent intent = new Intent(this, NotificationsActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
            startActivity(intent);
            finish();
        });

        navAdd.setOnClickListener(v -> {
            Intent intent = new Intent(this, AddEvent.class);
            startActivity(intent);
        });
    }

    private void loadHistoryEvents() {
        databaseService.getUserEvents(currentUserId, new DatabaseService.DatabaseCallback<List<Event>>() {
            @Override
            public void onCompleted(List<Event> events) {
                if (isFinishing() || isDestroyed()) return;
                pastEventsList.clear();

                if (events != null) {
                    long currentTime = System.currentTimeMillis();
                    SimpleDateFormat sdfFull = new SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault());

                    for (Event event : events) {
                        try {
                            if (event.getDateTime() != null) {
                                Date startDate = sdfFull.parse(event.getDateTime());
                                if (startDate != null) {
                                    long endMillis = startDate.getTime() + (long) (event.getParticipationHours() * 60 * 60 * 1000);
                                    if (currentTime > endMillis) {
                                        pastEventsList.add(event);
                                    }
                                }
                            }
                        } catch (Exception e) { e.printStackTrace(); }
                    }
                }

                String currentQuery = svHistory != null ? svHistory.getQuery().toString() : "";
                filterEvents(currentQuery);
            }
            @Override
            public void onFailed(Exception e) { Log.e(TAG, "Failed to load history", e); }
        });
    }

    private void filterEvents(String text) {
        if (pastEventsList == null || pastEventsList.isEmpty()) {
            rvHistory.setVisibility(View.GONE);
            llEmptyState.setVisibility(View.VISIBLE);
            if (adapter != null) adapter.setEvents(new ArrayList<>());
            return;
        }

        List<Event> filteredList = new ArrayList<>();
        for (Event event : pastEventsList) {
            boolean isMatch = text.isEmpty();
            if (!isMatch) {
                if (event.getTitle() != null && event.getTitle().toLowerCase().contains(text.toLowerCase())) isMatch = true;
                if (event.getLocation() != null && event.getLocation().toLowerCase().contains(text.toLowerCase())) isMatch = true;
            }
            if (isMatch) filteredList.add(event);
        }

        if (filteredList.isEmpty()) {
            rvHistory.setVisibility(View.GONE);
            llEmptyState.setVisibility(View.VISIBLE);
        } else {
            rvHistory.setVisibility(View.VISIBLE);
            llEmptyState.setVisibility(View.GONE);
        }
        if (adapter != null) adapter.setEvents(filteredList);
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
                if (cvNotificationBadge != null) cvNotificationBadge.setVisibility(View.GONE);
            }
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadHistoryEvents();
        loadNotificationsCount();
    }
}