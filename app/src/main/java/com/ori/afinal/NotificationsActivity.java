package com.ori.afinal;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.auth.FirebaseAuth;
import com.ori.afinal.Services.DatabaseService;
import com.ori.afinal.adapter.EventAdapter;
import com.ori.afinal.model.Event;

import java.util.ArrayList;
import java.util.List;

public class NotificationsActivity extends AppCompatActivity {

    private static final String TAG = "NotificationsActivity";

    private RecyclerView rvNotifications;
    private TextView tvNoNotifications;
    private EventAdapter adapter;
    private DatabaseService databaseService;
    private FirebaseAuth mAuth;
    private String currentUserId;

    // כפתורי הניווט התחתון
    private ImageButton navUpcoming, navHistory;
    private View cvNotificationBadge;
    private TextView tvNotificationBadgeCount;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_notifications);

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

        rvNotifications = findViewById(R.id.rv_notifications);
        tvNoNotifications = findViewById(R.id.tv_no_notifications);

        navUpcoming = findViewById(R.id.nav_upcoming);
        navHistory = findViewById(R.id.nav_history);
        cvNotificationBadge = findViewById(R.id.cv_notification_badge);
        tvNotificationBadgeCount = findViewById(R.id.tv_notification_badge_count);

        rvNotifications.setLayoutManager(new LinearLayoutManager(this));
        adapter = new EventAdapter();
        adapter.setCurrentUserId(currentUserId);
        rvNotifications.setAdapter(adapter);

        setupBottomNavigation();
        loadNotifications();
    }

    private void setupBottomNavigation() {
        navUpcoming.setOnClickListener(v -> {
            // חוזר למסך הבית (פגישות קרובות) וסוגר את מסך ההתראות
            Intent intent = new Intent(this, HomePage.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
            startActivity(intent);
            finish();
        });

        navHistory.setOnClickListener(v -> {
            // חוזר למסך הבית אבל שולח פקודה לעבור ישירות להיסטוריה
            Intent intent = new Intent(this, HomePage.class);
            intent.putExtra("SHOW_HISTORY", true);
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
            startActivity(intent);
            finish();
        });
    }

    private void loadNotifications() {
        // אנחנו קוראים לאותה פונקציה בדיוק שעושה את המספר לבועה!
        databaseService.getUserNotifications(currentUserId, new DatabaseService.DatabaseCallback<List<Event>>() {
            @Override
            public void onCompleted(List<Event> pendingEvents) {
                if (isFinishing() || isDestroyed()) return;

                if (pendingEvents != null && !pendingEvents.isEmpty()) {
                    rvNotifications.setVisibility(View.VISIBLE);
                    tvNoNotifications.setVisibility(View.GONE);
                    adapter.setEvents(pendingEvents);

                    // מציג את הבועה למטה גם במסך הזה
                    cvNotificationBadge.setVisibility(View.VISIBLE);
                    tvNotificationBadgeCount.setText(String.valueOf(pendingEvents.size()));
                } else {
                    rvNotifications.setVisibility(View.GONE);
                    tvNoNotifications.setVisibility(View.VISIBLE);
                    adapter.setEvents(new ArrayList<>());

                    cvNotificationBadge.setVisibility(View.GONE);
                }
            }

            @Override
            public void onFailed(Exception e) {
                Log.e(TAG, "Failed to load notifications", e);
                tvNoNotifications.setVisibility(View.VISIBLE);
                cvNotificationBadge.setVisibility(View.GONE);
            }
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadNotifications(); // מרענן אם הוא מאשר פגישה
    }
}