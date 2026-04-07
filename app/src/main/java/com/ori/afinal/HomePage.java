package com.ori.afinal;

import android.content.Intent;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.SearchView;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.progressindicator.CircularProgressIndicator;
import com.google.firebase.auth.FirebaseAuth;
import com.ori.afinal.Services.DatabaseService;
import com.ori.afinal.adapter.EventAdapter;
import com.ori.afinal.adapter.TemplateAdapter;
import com.ori.afinal.model.Event;
import com.ori.afinal.model.MeetingTemplate;
import com.ori.afinal.model.User;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class HomePage extends AppCompatActivity {

    private static final String TAG = "HomePage";

    private TextView tvGreeting, tvStatsCount, tvStatsDuration, tvNotificationBadgeCount;
    private TextView tvListHeader, tvEmptyText;
    private View cvNotificationBadge;
    private RecyclerView rvEvents, rvTemplates;
    private FloatingActionButton fabAddEvent;
    private ImageButton btnLogout, btnNotifications;
    private SearchView svEvents;

    // כפתורי הניווט המרחף
    private ImageButton navUpcoming, navHistory, navProfile;

    private CircularProgressIndicator progressMeetings, progressHours;

    private View cvLiveMeeting;
    private TextView tvLiveTitle, tvLiveTime;
    private View llEmptyState;

    private EventAdapter eventAdapter;
    private DatabaseService databaseService;
    private FirebaseAuth mAuth;
    private String currentUserId;

    private List<Event> fullEventsList = new ArrayList<>();

    // משתנה ששומר איזה מסך אנחנו מראים כרגע (עתיד או עבר)
    private boolean isShowingHistory = false;

    private final int GOAL_MEETINGS = 5;
    private final double GOAL_HOURS = 10.0;

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
        setupBottomNavigation();
        loadTemplates();
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
        rvTemplates = findViewById(R.id.rv_templates);
        btnLogout = findViewById(R.id.btn_logout);
        btnNotifications = findViewById(R.id.btn_notifications);
        cvNotificationBadge = findViewById(R.id.cv_notification_badge);
        tvNotificationBadgeCount = findViewById(R.id.tv_notification_badge_count);
        svEvents = findViewById(R.id.sv_events);

        tvListHeader = findViewById(R.id.tv_list_header);
        tvEmptyText = findViewById(R.id.tv_empty_text);

        navUpcoming = findViewById(R.id.nav_upcoming);
        navHistory = findViewById(R.id.nav_history);
        navProfile = findViewById(R.id.nav_profile);

        progressMeetings = findViewById(R.id.progress_meetings);
        progressHours = findViewById(R.id.progress_hours);

        cvLiveMeeting = findViewById(R.id.cv_live_meeting);
        tvLiveTitle = findViewById(R.id.tv_live_title);
        tvLiveTime = findViewById(R.id.tv_live_time);

        llEmptyState = findViewById(R.id.ll_empty_state);

        com.airbnb.lottie.LottieAnimationView lottieEmpty = findViewById(R.id.lottie_empty);
        if (lottieEmpty != null) {
            lottieEmpty.setFontAssetDelegate(new com.airbnb.lottie.FontAssetDelegate() {
                @Override
                public android.graphics.Typeface fetchFont(String fontFamily) {
                    return android.graphics.Typeface.DEFAULT_BOLD;
                }
            });
        }

        if (cvNotificationBadge != null) cvNotificationBadge.setVisibility(View.GONE);

        if (rvEvents != null) {
            rvEvents.setLayoutManager(new LinearLayoutManager(this));
            rvEvents.setNestedScrollingEnabled(false);
            eventAdapter = new EventAdapter();
            eventAdapter.setCurrentUserId(currentUserId);
            rvEvents.setAdapter(eventAdapter);
        }

        if (svEvents != null) {
            svEvents.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
                @Override
                public boolean onQueryTextSubmit(String query) { return false; }

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

    // הפונקציה שמטפלת בלחיצות על תפריט הניווט התחתון
    private void setupBottomNavigation() {
        navUpcoming.setOnClickListener(v -> {
            isShowingHistory = false;
            // צובע את הבית בכחול, ואת השאר באפור
            navUpcoming.setColorFilter(Color.parseColor("#3B82F6"));
            navHistory.setColorFilter(Color.parseColor("#6B7280"));
            navProfile.setColorFilter(Color.parseColor("#6B7280"));

            tvListHeader.setText("הפגישות הקרובות");
            tvEmptyText.setText("אין לך פגישות קרובות...\nזה הזמן לנוח! ☕");

            // מרענן את הרשימה
            String currentQuery = svEvents != null ? svEvents.getQuery().toString() : "";
            filterEvents(currentQuery);
        });

        navHistory.setOnClickListener(v -> {
            isShowingHistory = true;
            // צובע את ההיסטוריה בכחול, ואת השאר באפור
            navHistory.setColorFilter(Color.parseColor("#3B82F6"));
            navUpcoming.setColorFilter(Color.parseColor("#6B7280"));
            navProfile.setColorFilter(Color.parseColor("#6B7280"));

            tvListHeader.setText("היסטוריית פגישות");
            tvEmptyText.setText("היסטוריית הפגישות שלך ריקה...");

            // מרענן את הרשימה
            String currentQuery = svEvents != null ? svEvents.getQuery().toString() : "";
            filterEvents(currentQuery);
        });

        navProfile.setOnClickListener(v -> {
            // כרגע רק מציג הודעה, בהמשך ניצור לזה מסך!
            Toast.makeText(HomePage.this, "פרופיל אישי יעלה בקרוב!", Toast.LENGTH_SHORT).show();
        });
    }

    private void loadTemplates() {
        List<MeetingTemplate> templates = new ArrayList<>();
        templates.add(new MeetingTemplate("קפה זריז ☕", 15, "15 דקות"));
        templates.add(new MeetingTemplate("ישיבת צוות 👥", 60, "שעה"));
        templates.add(new MeetingTemplate("סיעור מוחות 💡", 45, "45 דקות"));
        templates.add(new MeetingTemplate("ארוחת צהריים 🍔", 60, "שעה"));

        TemplateAdapter templateAdapter = new TemplateAdapter(templates, template -> {
            Intent intent = new Intent(HomePage.this, AddEvent.class);
            intent.putExtra("TEMPLATE_TITLE", template.getTitle());
            intent.putExtra("TEMPLATE_DURATION", template.getDurationMinutes());
            startActivity(intent);
        });

        rvTemplates.setAdapter(templateAdapter);
    }

    // עדכנו את מנגנון הסינון כדי שידע להפריד בין פגישות עבר ועתיד
    private void filterEvents(String text) {
        if (fullEventsList == null || fullEventsList.isEmpty()) {
            rvEvents.setVisibility(View.GONE);
            llEmptyState.setVisibility(View.VISIBLE);
            if (eventAdapter != null) eventAdapter.setEvents(new ArrayList<>());
            return;
        }

        long currentTime = System.currentTimeMillis();
        SimpleDateFormat sdfFull = new SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault());
        List<Event> filteredList = new ArrayList<>();

        for (Event event : fullEventsList) {
            boolean isPastEvent = false;

            // בודקים אם הפגישה הסתיימה
            try {
                if (event.getDateTime() != null) {
                    Date startDate = sdfFull.parse(event.getDateTime());
                    if (startDate != null) {
                        long startMillis = startDate.getTime();
                        long endMillis = startMillis + (long) (event.getParticipationHours() * 60 * 60 * 1000);
                        if (currentTime > endMillis) {
                            isPastEvent = true;
                        }
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            }

            // מסננים לפי מה שהמשתמש בחר בתפריט התחתון
            if (isShowingHistory && !isPastEvent) continue; // אנחנו בהיסטוריה אבל הפגישה בעתיד - דלג
            if (!isShowingHistory && isPastEvent) continue; // אנחנו בבית אבל הפגישה בעבר - דלג

            // מסננים לפי החיפוש (אם יש)
            boolean isMatch = false;
            if (text.isEmpty()) {
                isMatch = true;
            } else {
                if (event.getTitle() != null && event.getTitle().toLowerCase().contains(text.toLowerCase())) isMatch = true;
                if (event.getLocation() != null && event.getLocation().toLowerCase().contains(text.toLowerCase())) isMatch = true;
            }

            if (isMatch) {
                filteredList.add(event);
            }
        }

        if (filteredList.isEmpty()) {
            rvEvents.setVisibility(View.GONE);
            llEmptyState.setVisibility(View.VISIBLE);
        } else {
            rvEvents.setVisibility(View.VISIBLE);
            llEmptyState.setVisibility(View.GONE);
        }

        if (eventAdapter != null) eventAdapter.setEvents(filteredList);
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
            public void onFailed(Exception e) { Log.e(TAG, "Failed to load user", e); }
        });
    }

    private void loadEventsData() {
        databaseService.getUserEvents(currentUserId, new DatabaseService.DatabaseCallback<List<Event>>() {
            @Override
            public void onCompleted(List<Event> events) {
                if (isFinishing() || isDestroyed()) return;

                fullEventsList.clear();
                double totalDuration = 0;

                if (events != null) {
                    long currentTime = System.currentTimeMillis();
                    SimpleDateFormat sdfFull = new SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault());
                    SimpleDateFormat sdfTime = new SimpleDateFormat("HH:mm", Locale.getDefault());

                    Event liveEvent = null;
                    long liveEventEndTimeMillis = 0;

                    for (Event event : events) {
                        fullEventsList.add(event);

                        try {
                            if (event.getDateTime() != null) {
                                Date startDate = sdfFull.parse(event.getDateTime());
                                if (startDate != null) {
                                    long startMillis = startDate.getTime();
                                    long endMillis = startMillis + (long) (event.getParticipationHours() * 60 * 60 * 1000);

                                    // מזהה פגישת לייב לעכשיו
                                    if (liveEvent == null && currentTime >= startMillis && currentTime <= endMillis) {
                                        liveEvent = event;
                                        liveEventEndTimeMillis = endMillis;
                                    }

                                    // מחשב שעות התקדמות רק על פגישות שכבר הסתיימו או קורות עכשיו (הגיוני, לא?)
                                    if (currentTime >= startMillis) {
                                        totalDuration += event.getParticipationHours();
                                    }
                                }
                            }
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }

                    if (liveEvent != null) {
                        cvLiveMeeting.setVisibility(View.VISIBLE);
                        tvLiveTitle.setText(liveEvent.getTitle());
                        tvLiveTime.setText("מסתיים ב- " + sdfTime.format(new Date(liveEventEndTimeMillis)));
                    } else {
                        cvLiveMeeting.setVisibility(View.GONE);
                    }
                }

                String currentQuery = svEvents != null ? svEvents.getQuery().toString() : "";
                filterEvents(currentQuery);

                if (tvStatsCount != null) tvStatsCount.setText(String.valueOf(fullEventsList.size()));
                if (tvStatsDuration != null) {
                    String durationText = (totalDuration == (long) totalDuration) ?
                            String.format(Locale.getDefault(), "%dh", (long) totalDuration) :
                            String.format(Locale.getDefault(), "%.1fh", totalDuration);
                    tvStatsDuration.setText(durationText);
                }

                int currentMeetingsCount = fullEventsList.size();
                int meetingsProgressPercentage = (int) Math.min(((double) currentMeetingsCount / GOAL_MEETINGS) * 100, 100);
                int hoursProgressPercentage = (int) Math.min((totalDuration / GOAL_HOURS) * 100, 100);

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                    if (progressMeetings != null) progressMeetings.setProgress(meetingsProgressPercentage, true);
                    if (progressHours != null) progressHours.setProgress(hoursProgressPercentage, true);
                } else {
                    if (progressMeetings != null) progressMeetings.setProgress(meetingsProgressPercentage);
                    if (progressHours != null) progressHours.setProgress(hoursProgressPercentage);
                }
            }
            @Override
            public void onFailed(Exception e) { Log.e(TAG, "Failed to load events", e); }
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
                if (cvNotificationBadge != null) cvNotificationBadge.setVisibility(View.GONE);
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