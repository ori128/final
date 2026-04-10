package com.ori.afinal;

import android.animation.ValueAnimator;
import android.content.Intent;
import android.graphics.Color;
import android.os.Build;
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

import com.applandeo.materialcalendarview.CalendarView;
import com.applandeo.materialcalendarview.EventDay;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.progressindicator.CircularProgressIndicator;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.ori.afinal.Services.DatabaseService;
import com.ori.afinal.adapter.EventAdapter;
import com.ori.afinal.model.Event;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.TimeUnit;

public class ProgressActivity extends AppCompatActivity {

    private static final String TAG = "ProgressActivity";

    private MaterialCardView btnTabStats, btnTabHistory;
    private TextView tvTabStats, tvTabHistory;
    private View scrollStats, llHistoryContainer;

    private CircularProgressIndicator progressMain;
    private TextView tvProgressPercent;
    private TextView tvStatCompleted, tvStatRegistration, tvStatLastDate, tvStatHours;

    private CalendarView calendarView;
    private RecyclerView rvRecentHistory;
    private View llEmptyHistory;
    private TextView tvSeeAllHistory;
    private EventAdapter recentAdapter;

    private ImageButton navHome, navHistory, navProgress, navNotifications, navAdd;
    private View cvNotificationBadge;
    private TextView tvNotificationBadgeCount;

    private DatabaseService databaseService;
    private FirebaseAuth mAuth;
    private String currentUserId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_progress);

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        mAuth = FirebaseAuth.getInstance();
        if (mAuth.getCurrentUser() != null) {
            currentUserId = mAuth.getCurrentUser().getUid();
            databaseService = DatabaseService.getInstance();
        } else {
            finish();
            return;
        }

        initViews();
        setupTopToggle();
        setupBottomNavigation();
        loadStatisticsAndHistory();
        loadNotificationsCount();

        loadUserAccountStats();
    }

    private void initViews() {
        btnTabStats = findViewById(R.id.btn_tab_stats);
        btnTabHistory = findViewById(R.id.btn_tab_history);
        tvTabStats = findViewById(R.id.tv_tab_stats);
        tvTabHistory = findViewById(R.id.tv_tab_history);
        scrollStats = findViewById(R.id.scroll_stats);
        llHistoryContainer = findViewById(R.id.ll_history_container);

        progressMain = findViewById(R.id.progress_main);
        tvProgressPercent = findViewById(R.id.tv_progress_percent);

        tvStatCompleted = findViewById(R.id.tv_stat_completed);
        tvStatRegistration = findViewById(R.id.tv_stat_registration);
        tvStatLastDate = findViewById(R.id.tv_stat_last_date);
        tvStatHours = findViewById(R.id.tv_stat_hours);

        calendarView = findViewById(R.id.calendarView);
        rvRecentHistory = findViewById(R.id.rv_recent_history);
        llEmptyHistory = findViewById(R.id.ll_empty_history);
        tvSeeAllHistory = findViewById(R.id.tv_see_all_history);

        com.airbnb.lottie.LottieAnimationView lottieEmpty = findViewById(R.id.lottie_empty_history);
        if (lottieEmpty != null) {
            lottieEmpty.setFontAssetDelegate(new com.airbnb.lottie.FontAssetDelegate() {
                @Override
                public android.graphics.Typeface fetchFont(String fontFamily) {
                    return android.graphics.Typeface.DEFAULT_BOLD;
                }
            });
        }

        rvRecentHistory.setLayoutManager(new LinearLayoutManager(this));
        recentAdapter = new EventAdapter();
        recentAdapter.setCurrentUserId(currentUserId);
        rvRecentHistory.setAdapter(recentAdapter);

        tvSeeAllHistory.setOnClickListener(v -> {
            Intent intent = new Intent(this, HistoryActivity.class);
            startActivity(intent);
        });

        navHome = findViewById(R.id.nav_upcoming);
        navHistory = findViewById(R.id.nav_history);
        navProgress = findViewById(R.id.nav_progress);
        navNotifications = findViewById(R.id.nav_notifications);
        navAdd = findViewById(R.id.nav_add);
        cvNotificationBadge = findViewById(R.id.cv_notification_badge);
        tvNotificationBadgeCount = findViewById(R.id.tv_notification_badge_count);
    }

    private void setupTopToggle() {
        btnTabStats.setOnClickListener(v -> {
            btnTabStats.setCardBackgroundColor(Color.parseColor("#3B82F6"));
            tvTabStats.setTextColor(Color.parseColor("#FFFFFF"));
            btnTabHistory.setCardBackgroundColor(Color.parseColor("#00000000"));
            tvTabHistory.setTextColor(Color.parseColor("#6B7280"));
            scrollStats.setVisibility(View.VISIBLE);
            llHistoryContainer.setVisibility(View.GONE);
            loadUserAccountStats();
        });

        btnTabHistory.setOnClickListener(v -> {
            btnTabHistory.setCardBackgroundColor(Color.parseColor("#3B82F6"));
            tvTabHistory.setTextColor(Color.parseColor("#FFFFFF"));
            btnTabStats.setCardBackgroundColor(Color.parseColor("#00000000"));
            tvTabStats.setTextColor(Color.parseColor("#6B7280"));
            llHistoryContainer.setVisibility(View.VISIBLE);
            scrollStats.setVisibility(View.GONE);
        });
    }

    private void setupBottomNavigation() {
        navHome.setOnClickListener(v -> {
            Intent intent = new Intent(this, HomePage.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
            startActivity(intent);
            finish();
        });
        navHistory.setOnClickListener(v -> {
            Intent intent = new Intent(this, HistoryActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
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

    private void loadUserAccountStats() {
        FirebaseUser user = mAuth.getCurrentUser();
        if (user != null && user.getMetadata() != null) {
            long creationTimestamp = user.getMetadata().getCreationTimestamp();

            SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());
            String registrationDateStr = sdf.format(new Date(creationTimestamp));
            if (tvStatRegistration != null) tvStatRegistration.setText(registrationDateStr);

            long currentTimestamp = System.currentTimeMillis();
            long diffInMillis = currentTimestamp - creationTimestamp;
            long daysActive = TimeUnit.MILLISECONDS.toDays(diffInMillis);

            if (daysActive <= 0) daysActive = 1;

            animateDaysCounter(0, (int) daysActive);
        }
    }

    private void animateDaysCounter(int start, int end) {
        if (tvProgressPercent == null || progressMain == null) return;
        ValueAnimator animator = ValueAnimator.ofInt(start, end);
        animator.setDuration(1500);
        animator.addUpdateListener(animation -> {
            int val = (int) animation.getAnimatedValue();
            tvProgressPercent.setText(String.valueOf(val));

            int percent = Math.min((int) (((double) val / 100.0) * 100), 100);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                progressMain.setProgress(percent, true);
            } else {
                progressMain.setProgress(percent);
            }
        });
        animator.start();
    }

    private void loadStatisticsAndHistory() {
        databaseService.getUserEvents(currentUserId, new DatabaseService.DatabaseCallback<List<Event>>() {
            @Override
            public void onCompleted(List<Event> events) {
                if (isFinishing() || isDestroyed()) return;

                if (events == null || events.isEmpty()) {
                    tvStatCompleted.setText("0");
                    tvStatLastDate.setText("אין");
                    tvStatHours.setText("0h");
                    rvRecentHistory.setVisibility(View.GONE);
                    llEmptyHistory.setVisibility(View.VISIBLE);
                    return;
                }

                int completedMeetings = 0;
                double totalHours = 0;
                long maxPastMillis = 0;
                long currentTime = System.currentTimeMillis();

                SimpleDateFormat sdfFull = new SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault());
                SimpleDateFormat sdfDisplay = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());

                List<Event> pastEvents = new ArrayList<>();
                List<EventDay> calendarEvents = new ArrayList<>();

                for (Event event : events) {
                    try {
                        if (event.getDateTime() != null) {
                            Date startDate = sdfFull.parse(event.getDateTime());
                            if (startDate != null) {

                                // הוספת נקודה כחולה ללוח השנה עבור *כל* פגישה
                                Calendar calendar = Calendar.getInstance();
                                calendar.setTime(startDate);
                                calendarEvents.add(new EventDay(calendar, R.drawable.calendar_dot));

                                // חישוב סטטיסטיקות והיסטוריה רק לפגישות עבר
                                long startMillis = startDate.getTime();
                                long endMillis = startMillis + (long) (event.getParticipationHours() * 60 * 60 * 1000);
                                if (currentTime > endMillis) {
                                    completedMeetings++;
                                    totalHours += event.getParticipationHours();
                                    if (endMillis > maxPastMillis) maxPastMillis = endMillis;
                                    pastEvents.add(event);
                                }
                            }
                        }
                    } catch (Exception e) { e.printStackTrace(); }
                }

                tvStatCompleted.setText(String.valueOf(completedMeetings));
                String hoursText = (totalHours == (long) totalHours) ? String.format(Locale.getDefault(), "%d", (long) totalHours) : String.format(Locale.getDefault(), "%.1f", totalHours);
                tvStatHours.setText(hoursText + "h");
                if (maxPastMillis > 0) tvStatLastDate.setText(sdfDisplay.format(new Date(maxPastMillis)));
                else tvStatLastDate.setText("אין");

                calendarView.setEvents(calendarEvents);

                if (pastEvents.isEmpty()) {
                    rvRecentHistory.setVisibility(View.GONE);
                    llEmptyHistory.setVisibility(View.VISIBLE);
                } else {
                    rvRecentHistory.setVisibility(View.VISIBLE);
                    llEmptyHistory.setVisibility(View.GONE);
                    Collections.sort(pastEvents, (e1, e2) -> {
                        try {
                            Date d1 = sdfFull.parse(e1.getDateTime());
                            Date d2 = sdfFull.parse(e2.getDateTime());
                            if (d1 != null && d2 != null) return d2.compareTo(d1);
                        } catch (Exception e) { e.printStackTrace(); }
                        return 0;
                    });
                    List<Event> recent5 = pastEvents.size() > 5 ? pastEvents.subList(0, 5) : pastEvents;
                    recentAdapter.setEvents(recent5);
                }
            }

            @Override
            public void onFailed(Exception e) { Log.e(TAG, "Failed to load stats", e); }
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
                if (cvNotificationBadge != null) cvNotificationBadge.setVisibility(View.GONE);
            }
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadStatisticsAndHistory();
        loadUserAccountStats();
    }
}