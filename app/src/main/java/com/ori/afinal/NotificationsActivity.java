package com.ori.afinal;

import android.os.Bundle;
import android.view.View;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.auth.FirebaseAuth;
import com.ori.afinal.Services.DatabaseService;
import com.ori.afinal.adapter.NotificationAdapter;
import com.ori.afinal.model.Notification;

import java.util.Collections;
import java.util.List;

public class NotificationsActivity extends AppCompatActivity {

    private RecyclerView rvNotifications;
    private TextView tvEmptyNotifications;
    private ImageButton btnBack;

    private DatabaseService databaseService;
    private FirebaseAuth mAuth;
    private NotificationAdapter notificationAdapter;
    private String currentUserId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_notifications);

        databaseService = DatabaseService.getInstance();
        mAuth = FirebaseAuth.getInstance();

        if (mAuth.getCurrentUser() != null) {
            currentUserId = mAuth.getCurrentUser().getUid();
        } else {
            finish();
            return;
        }

        rvNotifications = findViewById(R.id.rv_notifications);
        tvEmptyNotifications = findViewById(R.id.tv_empty_notifications);
        btnBack = findViewById(R.id.btn_back);

        btnBack.setOnClickListener(v -> finish());

        rvNotifications.setLayoutManager(new LinearLayoutManager(this));

        // יצירת האדפטר
        notificationAdapter = new NotificationAdapter(() -> loadNotifications());
        rvNotifications.setAdapter(notificationAdapter);

        loadNotifications();
    }

    private void loadNotifications() {
        databaseService.getSmartNotifications(currentUserId, new DatabaseService.DatabaseCallback<List<Notification>>() {
            @Override
            public void onCompleted(List<Notification> notifications) {
                if (notifications == null || notifications.isEmpty()) {
                    tvEmptyNotifications.setVisibility(View.VISIBLE);
                    rvNotifications.setVisibility(View.GONE);
                } else {
                    tvEmptyNotifications.setVisibility(View.GONE);
                    rvNotifications.setVisibility(View.VISIBLE);

                    // נמיין את ההתראות כך שהחדשות ביותר יהיו למעלה
                    Collections.sort(notifications, (n1, n2) -> Long.compare(n2.getTimestamp(), n1.getTimestamp()));

                    notificationAdapter.setNotifications(notifications);
                }
            }

            @Override
            public void onFailed(Exception e) {
                Toast.makeText(NotificationsActivity.this, "שגיאה בטעינת התראות", Toast.LENGTH_SHORT).show();
            }
        });
    }
}