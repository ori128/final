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
import com.ori.afinal.adapter.NotificationAdapter;
import com.ori.afinal.model.Event;

import java.util.ArrayList;
import java.util.List;

public class NotificationsActivity extends AppCompatActivity {

    private static final String TAG = "NotificationsActivity";

    private RecyclerView rvNotifications;
    private View llEmptyState; // שונה מ-TextView ל-View כי זה עכשיו Layout
    private SearchView svNotifications;
    private ImageButton btnGoToTrash;

    private NotificationAdapter adapter; // שימוש באדפטר החדש!
    private DatabaseService databaseService;
    private FirebaseAuth mAuth;
    private String currentUserId;

    private List<Event> fullNotificationsList = new ArrayList<>();

    private ImageButton navUpcoming, navHistory, navProgress, navNotifications, navAdd;
    private View cvNotificationBadge;
    private TextView tvNotificationBadgeCount;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_notifications);

        // תיקון ה-Padding עבור ה-RecyclerView
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.rv_notifications), (v, insets) -> {
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
        loadNotifications();
    }

    private void initViews() {
        rvNotifications = findViewById(R.id.rv_notifications);
        llEmptyState = findViewById(R.id.ll_empty_state);
        svNotifications = findViewById(R.id.sv_notifications);
        btnGoToTrash = findViewById(R.id.btn_go_to_trash);

        navUpcoming = findViewById(R.id.nav_upcoming);
        navHistory = findViewById(R.id.nav_history);
        navProgress = findViewById(R.id.nav_progress);
        navNotifications = findViewById(R.id.nav_notifications);
        navAdd = findViewById(R.id.nav_add);

        cvNotificationBadge = findViewById(R.id.cv_notification_badge);
        tvNotificationBadgeCount = findViewById(R.id.tv_notification_badge_count);

        // הגדרת האדפטר החדש של ההתראות (isTrashMode = false)
        rvNotifications.setLayoutManager(new LinearLayoutManager(this));
        adapter = new NotificationAdapter(currentUserId, false);
        rvNotifications.setAdapter(adapter);

        // הגדרת החיפוש
        svNotifications.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) { return false; }

            @Override
            public boolean onQueryTextChange(String newText) {
                filterNotifications(newText);
                return true;
            }
        });

        btnGoToTrash.setOnClickListener(v -> {
            Intent intent = new Intent(this, TrashActivity.class);
            startActivity(intent);
        });

        // הגדרת הפנדה (למניעת קריסה)
        com.airbnb.lottie.LottieAnimationView lottiePanda = findViewById(R.id.lottie_panda);
        if (lottiePanda != null) {
            lottiePanda.setFontAssetDelegate(new com.airbnb.lottie.FontAssetDelegate() {
                @Override
                public android.graphics.Typeface fetchFont(String fontFamily) {
                    return android.graphics.Typeface.DEFAULT_BOLD;
                }
            });
        }
    }

    private void setupBottomNavigation() {
        navUpcoming.setOnClickListener(v -> {
            Intent intent = new Intent(this, HomePage.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
            startActivity(intent);
            finish();
        });

        navHistory.setOnClickListener(v -> {
            Intent intent = new Intent(this, HistoryActivity.class);
            startActivity(intent);
        });

        navProgress.setOnClickListener(v -> {
            Intent intent = new Intent(this, ProgressActivity.class);
            startActivity(intent);
        });

        navAdd.setOnClickListener(v -> {
            Intent intent = new Intent(this, AddEvent.class);
            startActivity(intent);
        });
    }

    private void loadNotifications() {
        databaseService.getUserNotifications(currentUserId, new DatabaseService.DatabaseCallback<List<Event>>() {
            @Override
            public void onCompleted(List<Event> pendingEvents) {
                if (isFinishing() || isDestroyed()) return;

                fullNotificationsList.clear();
                if (pendingEvents != null) {
                    fullNotificationsList.addAll(pendingEvents);
                }

                filterNotifications(svNotifications.getQuery().toString());
                updateBadge(fullNotificationsList.size());
            }

            @Override
            public void onFailed(Exception e) {
                Log.e(TAG, "Failed to load notifications", e);
                llEmptyState.setVisibility(View.VISIBLE);
            }
        });
    }

    private void filterNotifications(String text) {
        List<Event> filteredList = new ArrayList<>();
        for (Event event : fullNotificationsList) {
            if (text.isEmpty() || (event.getTitle() != null && event.getTitle().toLowerCase().contains(text.toLowerCase()))) {
                filteredList.add(event);
            }
        }

        if (filteredList.isEmpty()) {
            rvNotifications.setVisibility(View.GONE);
            llEmptyState.setVisibility(View.VISIBLE);
        } else {
            rvNotifications.setVisibility(View.VISIBLE);
            llEmptyState.setVisibility(View.GONE);
        }
        adapter.setEvents(filteredList);
    }

    private void updateBadge(int count) {
        if (count > 0) {
            cvNotificationBadge.setVisibility(View.VISIBLE);
            tvNotificationBadgeCount.setText(String.valueOf(count));
        } else {
            cvNotificationBadge.setVisibility(View.GONE);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadNotifications();
    }
}