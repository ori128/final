package com.ori.afinal;

import android.content.Intent;
import android.os.Build;
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

    // כאן אני מגדיר את כל המשתנים של התצוגה (UI) כדי שאוכל לעבוד איתם בקוד
    private TextView tvGreeting, tvDate, tvDay;
    private TextView tvStatsCount, tvStatsDuration, tvNotificationBadgeCount;
    private TextView tvListHeader, tvEmptyText;
    private View cvNotificationBadge;
    private RecyclerView rvEvents, rvTemplates;
    private SearchView svEvents;

    // כפתורי הניווט והפעולות במסך
    private View btnProfile, btnLogout, btnAdmin;
    private ImageButton navUpcoming, navHistory, navProgress, navNotifications, navAdd;

    // משתנים עבור מדדי ההתקדמות (Progress) והפגישה הנוכחית (Live)
    private CircularProgressIndicator progressMeetings, progressHours;
    private View cvLiveMeeting;
    private TextView tvLiveTitle, tvLiveTime;
    private View llEmptyState;

    // אובייקטים לניהול המידע והתקשורת מול מסד הנתונים
    private EventAdapter eventAdapter;
    private DatabaseService databaseService;
    private FirebaseAuth mAuth;
    private String currentUserId;

    // רשימה שתשמור את כל הפגישות כדי שאוכל לסנן אותן אחר כך
    private List<Event> fullEventsList = new ArrayList<>();
    private boolean isShowingHistory = false;

    // יעדים שהגדרתי למשתמש עבור ה-Progress Bars
    private final int GOAL_MEETINGS = 5;
    private final double GOAL_HOURS = 10.0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge  .enable(this);
        setContentView(R.layout.activity_home_page);

        // סידור שוליים כדי שהאפליקציה תיראה טוב על כל המסך (Edge to Edge)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        // אתחול החיבור ל-Firebase ולשירות מסד הנתונים שבניתי
        databaseService = DatabaseService.getInstance();
        mAuth = FirebaseAuth.getInstance();

        // אני בודק אם יש משתמש מחובר. אם כן, אני שומר את ה-ID שלו. אם לא, אני סוגר את המסך.
        if (mAuth.getCurrentUser() != null) {
            currentUserId = mAuth.getCurrentUser().getUid();
        } else {
            finish();
            return;
        }

        // קורא לפונקציות שמסדרות את כל המסך ומושכות נתונים
        initViews();
        setupDateTime();
        setupBottomNavigation();
        loadTemplates();
        loadUserData();
        loadEventsData();
        loadNotificationsCount();
    }

    // פונקציה שמרכזת את כל הקישורים בין קובץ העיצוב (XML) לבין הקוד
    private void initViews() {
        tvGreeting = findViewById(R.id.tv_greeting);
        tvDate = findViewById(R.id.tv_date);
        tvDay = findViewById(R.id.tv_day);
        btnProfile = findViewById(R.id.btn_profile);
        btnLogout = findViewById(R.id.btn_logout);
        btnAdmin = findViewById(R.id.btn_admin);

        tvStatsCount = findViewById(R.id.tv_stats_count);
        tvStatsDuration = findViewById(R.id.tv_stats_duration);
        rvEvents = findViewById(R.id.rv_events);
        rvTemplates = findViewById(R.id.rv_templates);
        cvNotificationBadge = findViewById(R.id.cv_notification_badge);
        tvNotificationBadgeCount = findViewById(R.id.tv_notification_badge_count);
        svEvents = findViewById(R.id.sv_events);
        tvListHeader = findViewById(R.id.tv_list_header);
        tvEmptyText = findViewById(R.id.tv_empty_text);

        navUpcoming = findViewById(R.id.nav_upcoming);
        navHistory = findViewById(R.id.nav_history);
        navProgress = findViewById(R.id.nav_progress);
        navNotifications = findViewById(R.id.nav_notifications);
        navAdd = findViewById(R.id.nav_add);

        progressMeetings = findViewById(R.id.progress_meetings);
        progressHours = findViewById(R.id.progress_hours);
        cvLiveMeeting = findViewById(R.id.cv_live_meeting);
        tvLiveTitle = findViewById(R.id.tv_live_title);
        tvLiveTime = findViewById(R.id.tv_live_time);
        llEmptyState = findViewById(R.id.ll_empty_state);

        // הגדרת פונט לאנימציה (Lottie) של המצב הריק (כשאין פגישות)
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

        // הגדרת ה-RecyclerView שמציג את רשימת הפגישות
        if (rvEvents != null) {
            rvEvents.setLayoutManager(new LinearLayoutManager(this));
            rvEvents.setNestedScrollingEnabled(false);
            eventAdapter = new EventAdapter();
            eventAdapter.setCurrentUserId(currentUserId);
            rvEvents.setAdapter(eventAdapter);
        }

        // הוספתי מאזין לשורת החיפוש כדי לסנן את הפגישות בזמן אמת כשמקלידים
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

        // הגדרת כפתורי הפעולות העליונים
        if (btnLogout != null) {
            btnLogout.setOnClickListener(v -> showLogoutDialog());
        }
        if (btnProfile != null) {
            btnProfile.setOnClickListener(v -> {
                Intent intent = new Intent(HomePage.this, ProfileActivity.class);
                startActivity(intent);
            });
        }

        // כפתור מנהל - יוצג רק למי שיש הרשאה, בלחיצה מעביר לעמוד הניהול שבניתי
        if (btnAdmin != null) {
            btnAdmin.setOnClickListener(v -> {
                Intent intent = new Intent(HomePage.this, AdminActivity.class);
                startActivity(intent);
            });
        }
    }

    // פונקציה קטנה שמציגה את התאריך והיום הנוכחיים בראש המסך
    private void setupDateTime() {
        SimpleDateFormat sdfDay = new SimpleDateFormat("EEEE", new Locale("he", "IL"));
        SimpleDateFormat sdfDate = new SimpleDateFormat("d MMMM", new Locale("he", "IL"));
        if (tvDay != null) tvDay.setText(sdfDay.format(new Date()));
        if (tvDate != null) tvDate.setText(sdfDate.format(new Date()));
    }

    // הגדרת התפריט התחתון (Bottom Navigation) ומעבר בין המסכים
    private void setupBottomNavigation() {
        navUpcoming.setOnClickListener(v -> {
            String currentQuery = svEvents != null ? svEvents.getQuery().toString() : "";
            filterEvents(currentQuery);
        });

        navHistory.setOnClickListener(v -> {
            Intent intent = new Intent(HomePage.this, HistoryActivity.class);
            startActivity(intent);
        });

        navProgress.setOnClickListener(v -> {
            Intent intent = new Intent(HomePage.this, ProgressActivity.class);
            startActivity(intent);
        });

        navNotifications.setOnClickListener(v -> {
            Intent intent = new Intent(HomePage.this, NotificationsActivity.class);
            startActivity(intent);
        });

        // כפתור הוספת פגישה חדשה
        navAdd.setOnClickListener(v -> {
            Intent intent = new Intent(HomePage.this, AddEvent.class);
            startActivity(intent);
        });
    }

    // יצרתי רשימה של תבניות פגישה מוכנות מראש (כמו קפה זריז) כדי לחסוך זמן למשתמש
    private void loadTemplates() {
        List<MeetingTemplate> templates = new ArrayList<>();
        templates.add(new MeetingTemplate("קפה זריז ☕", 15, "15 דקות"));
        templates.add(new MeetingTemplate("ישיבת צוות 👥", 60, "שעה"));
        templates.add(new MeetingTemplate("סיעור מוחות 💡", 45, "45 דקות"));
        templates.add(new MeetingTemplate("ארוחת צהריים 🍔", 60, "שעה"));

        // כשהמשתמש לוחץ על תבנית, אני שולח אותו לעמוד הוספת פגישה עם הנתונים כבר מלאים
        TemplateAdapter templateAdapter = new TemplateAdapter(templates, template -> {
            Intent intent = new Intent(HomePage.this, AddEvent.class);
            intent.putExtra("TEMPLATE_TITLE", template.getTitle());
            intent.putExtra("TEMPLATE_DURATION", template.getDurationMinutes());
            startActivity(intent);
        });
        rvTemplates.setAdapter(templateAdapter);
    }

    // הלוגיקה של הסינון: בודקת אם להציג פגישות עבר/עתיד ואם הטקסט תואם לחיפוש
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

        // אני רץ על כל הפגישות כדי לבדוק מי מתאימה לסינון
        for (Event event : fullEventsList) {
            boolean isPastEvent = false;
            try {
                if (event.getDateTime() != null) {
                    Date startDate = sdfFull.parse(event.getDateTime());
                    if (startDate != null) {
                        // חישוב מתי הפגישה מסתיימת כדי לדעת אם היא נחשבת "פגישת עבר"
                        long endMillis = startDate.getTime() + (long) (event.getParticipationHours() * 60 * 60 * 1000);
                        if (currentTime > endMillis) isPastEvent = true;
                    }
                }
            } catch (Exception e) { e.printStackTrace(); }

            // דילוג על פגישות לא רלוונטיות (למשל, מציג עתיד אבל הפגישה בעבר)
            if (isShowingHistory && !isPastEvent) continue;
            if (!isShowingHistory && isPastEvent) continue;

            // בדיקה אם טקסט החיפוש נמצא בכותרת או במיקום הפגישה
            boolean isMatch = text.isEmpty();
            if (!isMatch) {
                if (event.getTitle() != null && event.getTitle().toLowerCase().contains(text.toLowerCase())) isMatch = true;
                if (event.getLocation() != null && event.getLocation().toLowerCase().contains(text.toLowerCase())) isMatch = true;
            }
            if (isMatch) filteredList.add(event);
        }

        // עדכון התצוגה - אם אין פגישות נציג אנימציה (Empty State), אחרת נציג את הרשימה
        if (filteredList.isEmpty()) {
            rvEvents.setVisibility(View.GONE);
            llEmptyState.setVisibility(View.VISIBLE);
        } else {
            rvEvents.setVisibility(View.VISIBLE);
            llEmptyState.setVisibility(View.GONE);
        }
        if (eventAdapter != null) eventAdapter.setEvents(filteredList);
    }

    // יצרתי דיאלוג התנתקות כדי לוודא שהמשתמש לא יתנתק בטעות בלחיצה שגויה
    private void showLogoutDialog() {
        if (isFinishing() || isDestroyed()) return;
        new androidx.appcompat.app.AlertDialog.Builder(this)
                .setTitle("התנתקות")
                .setMessage("האם אתה בטוח שברצונך להתנתק מהמערכת?")
                .setPositiveButton("כן, התנתק", (dialog, which) -> {
                    mAuth.signOut();
                    Intent intent = new Intent(HomePage.this, MainActivity.class);
                    intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                    startActivity(intent);
                    finish();
                }).setNegativeButton("ביטול", null).show();
    }

    // פונקציה ששואבת את פרטי המשתמש מהפיירבייס, מציגה את שמו ובודקת אם הוא מנהל
    private void loadUserData() {
        databaseService.getUser(currentUserId, new DatabaseService.DatabaseCallback<User>() {
            @Override
            public void onCompleted(User user) {
                if (isFinishing() || isDestroyed()) return;
                if (user != null) {
                    if (user.getFname() != null && tvGreeting != null) {
                        tvGreeting.setText("היי, " + user.getFname());
                    }

                    // הלוגיקה של המנהלים - אם משתנה ה-admin קיים ומוגדר כ-true, הוא יראה את הכפתור
                    if (btnAdmin != null) {
                        if (Boolean.TRUE.equals(user.getAdmin())) {
                            btnAdmin.setVisibility(View.VISIBLE);
                        } else {
                            btnAdmin.setVisibility(View.GONE);
                        }
                    }
                }
            }
            @Override
            public void onFailed(Exception e) { Log.e(TAG, "Failed to load user", e); }
        });
    }

    // פונקציה מרכזית שמושכת את הפגישות של המשתמש מהמסד
    private void loadEventsData() {
        databaseService.getUserEvents(currentUserId, new DatabaseService.DatabaseCallback<List<Event>>() {
            @Override
            public void onCompleted(List<Event> events) {
                if (isFinishing() || isDestroyed()) return;
                fullEventsList.clear();
                double totalDuration = 0;
                int completedMeetingsCount = 0;

                if (events != null) {
                    long currentTime = System.currentTimeMillis();
                    SimpleDateFormat sdfFull = new SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault());
                    SimpleDateFormat sdfTime = new SimpleDateFormat("HH:mm", Locale.getDefault());
                    Event liveEvent = null;
                    long liveEventEndTimeMillis = 0;

                    // מעבר על כל הפגישות כדי לחשב נתונים לסטטיסטיקה ולמצוא פגישה פעילה (Live)
                    for (Event event : events) {
                        fullEventsList.add(event);
                        try {
                            if (event.getDateTime() != null) {
                                Date startDate = sdfFull.parse(event.getDateTime());
                                if (startDate != null) {
                                    long startMillis = startDate.getTime();
                                    long endMillis = startMillis + (long) (event.getParticipationHours() * 60 * 60 * 1000);

                                    // מזהה אם יש עכשיו פגישה שמתרחשת באותו הרגע
                                    if (liveEvent == null && currentTime >= startMillis && currentTime <= endMillis) {
                                        liveEvent = event;
                                        liveEventEndTimeMillis = endMillis;
                                    }

                                    // חישוב סך שעות הפגישות שהמשתמש קבע
                                    if (currentTime >= startMillis) totalDuration += event.getParticipationHours();
                                    if (currentTime > endMillis) completedMeetingsCount++;
                                }
                            }
                        } catch (Exception e) { e.printStackTrace(); }
                    }

                    // אם יש פגישה פעילה עכשיו, אני מציג את החלונית שלה למעלה
                    if (liveEvent != null) {
                        cvLiveMeeting.setVisibility(View.VISIBLE);
                        tvLiveTitle.setText(liveEvent.getTitle());
                        tvLiveTime.setText("מסתיים ב- " + sdfTime.format(new Date(liveEventEndTimeMillis)));
                    } else {
                        cvLiveMeeting.setVisibility(View.GONE);
                    }
                }

                // מפעיל את הסינון כדי להציג את הרשימה המעודכנת
                String currentQuery = svEvents != null ? svEvents.getQuery().toString() : "";
                filterEvents(currentQuery);

                // מעדכן את המספרים בסטטיסטיקות (כמה פגישות וכמה שעות)
                if (tvStatsCount != null) tvStatsCount.setText(String.valueOf(fullEventsList.size()));
                if (tvStatsDuration != null) {
                    String durationText = (totalDuration == (long) totalDuration) ?
                            String.format(Locale.getDefault(), "%dh", (long) totalDuration) :
                            String.format(Locale.getDefault(), "%.1fh", totalDuration);
                    tvStatsDuration.setText(durationText);
                }

                // חישוב האחוזים בשביל ה-Progress Bars העגולים
                int currentMeetingsCount = fullEventsList.size();
                int meetingsProgressPercentage = (int) Math.min(((double) currentMeetingsCount / GOAL_MEETINGS) * 100, 100);
                int hoursProgressPercentage = (int) Math.min((totalDuration / GOAL_HOURS) * 100, 100);

                // עדכון התצוגה של המדדים בצורה תואמת גרסאות
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

    // פונקציה שבודקת אם יש למשתמש התראות שממתינות, ואם כן מציגה בועה אדומה עם המספר
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

    // כשהמסך חוזר לפוקוס (למשל אחרי שחזרנו מעמוד אחר), אני מרענן את כל הנתונים
    @Override
    protected void onResume() {
        super.onResume();
        loadEventsData();
        loadNotificationsCount();
        loadUserData();
    }
}