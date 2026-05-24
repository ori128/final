package com.ori.afinal;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.datepicker.CalendarConstraints;
import com.google.android.material.datepicker.DateValidatorPointForward;
import com.google.android.material.datepicker.MaterialDatePicker;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.timepicker.MaterialTimePicker;
import com.google.android.material.timepicker.TimeFormat;
import com.google.firebase.auth.FirebaseAuth;
import com.ori.afinal.Services.DatabaseService;
import com.ori.afinal.model.Event;
import com.ori.afinal.model.User;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class AddEvent extends AppCompatActivity {

    private static final String TAG = "AddEvent";

    // כאן אני מגדיר את כל השדות והכפתורים שיש לי בטופס יצירת הפגישה
    private EditText etTitle, etLocation, etDescription;
    private TextInputEditText etDatePicker, etStartTime, etEndTime;
    private RadioGroup radioGroupType;
    private Button btnSaveEvent, btnBack, btnAddParticipants;
    private TextView tvParticipantsList;

    // כפתורי הניווט התחתון (Bottom Navigation) והתראות
    private ImageButton navUpcoming, navHistory, navProgress, navNotifications, navAdd;
    private View cvNotificationBadge;
    private TextView tvNotificationBadgeCount;

    // אובייקטים להתחברות מול מסד הנתונים שלי ומערכת האימות של Firebase
    private DatabaseService databaseService;
    private FirebaseAuth mAuth;

    // משתנים לשמירת התאריך שנבחר ורשימת המזהים (IDs) של האנשים שהזמנתי לפגישה
    private Calendar selectedDate;
    private List<String> selectedParticipantIds = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_event);

        // פה אני מוודא שיש לי הרשאה לשלוח התראות למשתמש (רלוונטי לאנדרואיד 13 ומעלה)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (checkSelfPermission(android.Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                requestPermissions(new String[]{android.Manifest.permission.POST_NOTIFICATIONS}, 101);
            }
        }

        // אתחול החיבור לשרת (Firebase)
        databaseService = DatabaseService.getInstance();
        mAuth = FirebaseAuth.getInstance();

        // קישור בין כל המשתנים שהגדרתי למעלה לבין קובץ העיצוב (XML) של המסך
        etTitle = findViewById(R.id.et_title);
        etLocation = findViewById(R.id.et_location);
        etDescription = findViewById(R.id.et_description);

        etDatePicker = findViewById(R.id.et_date_picker);
        etStartTime = findViewById(R.id.et_start_time);
        etEndTime = findViewById(R.id.et_end_time);

        radioGroupType = findViewById(R.id.rg_meeting_type);

        btnSaveEvent = findViewById(R.id.btn_save_event);
        btnBack = findViewById(R.id.btn_back);
        btnAddParticipants = findViewById(R.id.btn_add_participants);
        tvParticipantsList = findViewById(R.id.tv_participants_list);

        navUpcoming = findViewById(R.id.nav_upcoming);
        navHistory = findViewById(R.id.nav_history);
        navProgress = findViewById(R.id.nav_progress);
        navNotifications = findViewById(R.id.nav_notifications);
        navAdd = findViewById(R.id.nav_add);
        cvNotificationBadge = findViewById(R.id.cv_notification_badge);
        tvNotificationBadgeCount = findViewById(R.id.tv_notification_badge_count);

        if (cvNotificationBadge != null) cvNotificationBadge.setVisibility(View.GONE);

        selectedDate = Calendar.getInstance();

        // קורא לפונקציות העזר שיצרתי שמגדירות את תפקוד המסך
        setupPickers();
        setupRadioGroupListener();
        setupBottomNavigation();
        handleTemplateData();

        // לחיצה על "שמור פגישה" תפעיל את הפונקציה המרכזית שיוצרת את הפגישה
        btnSaveEvent.setOnClickListener(v -> createEvent());

        // לחיצה על חזור פשוט סוגרת את המסך הנוכחי ומחזירה אחורה
        btnBack.setOnClickListener(v -> finish());

        // הלוגיקה של כפתור "הוסף משתתפים" - פותח חלונית (Dialog) לבחירת האנשים לפגישה
        btnAddParticipants.setOnClickListener(v -> {
            // אני קורא לכל המשתמשים מהדאטה-בייס כדי להציג אותם ברשימה
            databaseService.getAllUsers(new DatabaseService.DatabaseCallback<List<User>>() {
                @Override
                public void onCompleted(List<User> users) {
                    if (users == null || users.isEmpty()) {
                        Toast.makeText(AddEvent.this, "אין משתמשים במערכת", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    // אני מייצר רשימה חדשה שמוציאה אותי (המשתמש המחובר) כדי שלא אזמין את עצמי
                    List<User> otherUsers = new ArrayList<>();
                    String currentUserId = mAuth.getCurrentUser().getUid();
                    for (User u : users) {
                        if (u.getId() != null && !u.getId().equals(currentUserId)) {
                            otherUsers.add(u);
                        }
                    }

                    // הכנת המערכים הדרושים לחלונית הבחירה (שמות + מי סומן ומי לא)
                    String[] userNames = new String[otherUsers.size()];
                    boolean[] checkedItems = new boolean[otherUsers.size()];

                    for (int i = 0; i < otherUsers.size(); i++) {
                        userNames[i] = otherUsers.get(i).getFname() != null ? otherUsers.get(i).getFname() : "משתמש";
                        // מסמן מראש אנשים שכבר נבחרו בלחיצה קודמת
                        checkedItems[i] = selectedParticipantIds.contains(otherUsers.get(i).getId());
                    }

                    // יצירת והצגת הדיאלוג עם תיבות הסימון
                    new AlertDialog.Builder(AddEvent.this)
                            .setTitle("בחר משתתפים להזמנה")
                            .setMultiChoiceItems(userNames, checkedItems, (dialog, which, isChecked) -> {
                                String selectedId = otherUsers.get(which).getId();
                                if (isChecked) {
                                    if (!selectedParticipantIds.contains(selectedId)) selectedParticipantIds.add(selectedId);
                                } else {
                                    selectedParticipantIds.remove(selectedId);
                                }
                            })
                            .setPositiveButton("אישור", (dialog, which) -> {
                                // כשהמשתמש מאשר, אני מעדכן את הטקסט במסך שיציג כמה אנשים הוזמנו
                                tvParticipantsList.setText("נבחרו " + selectedParticipantIds.size() + " מוזמנים");
                            })
                            .setNegativeButton("ביטול", null)
                            .show();
                }

                @Override
                public void onFailed(Exception e) {
                    Toast.makeText(AddEvent.this, "שגיאה בטעינת משתמשים", Toast.LENGTH_SHORT).show();
                }
            });
        });
    }

    // הפונקציה הזו מטפלת במצב שבו הגענו למסך הזה מלחיצה על "תבנית" (כמו קפה זריז במסך הבית)
    private void handleTemplateData() {
        Intent intent = getIntent();
        if (intent != null && intent.hasExtra("TEMPLATE_TITLE")) {
            String title = intent.getStringExtra("TEMPLATE_TITLE");
            int durationMinutes = intent.getIntExtra("TEMPLATE_DURATION", 0);

            // ממלא את הכותרת אוטומטית
            etTitle.setText(title);

            // ממלא את הזמנים אוטומטית בהתאם לאורך התבנית
            if (durationMinutes > 0) {
                selectedDate = Calendar.getInstance();
                SimpleDateFormat sdfDate = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
                etDatePicker.setText(sdfDate.format(selectedDate.getTime()));

                // הפגישה תתחיל בעוד 5 דקות מעכשיו (כברירת מחדל)
                Calendar startCal = Calendar.getInstance();
                startCal.add(Calendar.MINUTE, 5);
                SimpleDateFormat sdfTime = new SimpleDateFormat("HH:mm", Locale.getDefault());
                etStartTime.setText(sdfTime.format(startCal.getTime()));

                // מוסיף את אורך הפגישה לשעת ההתחלה כדי לחשב שעת סיום
                Calendar endCal = (Calendar) startCal.clone();
                endCal.add(Calendar.MINUTE, durationMinutes);
                etEndTime.setText(sdfTime.format(endCal.getTime()));
            }
        }
    }

    // פונקציה שמאזינה לבחירת סוג הפגישה (פיזית/אונליין)
    private void setupRadioGroupListener() {
        radioGroupType.setOnCheckedChangeListener((group, checkedId) -> {
            if (checkedId == R.id.rb_online) {
                // אם בחר אונליין - ממלאים אוטומטית וחוסמים את כתיבת המיקום
                etLocation.setText("Online");
                etLocation.setEnabled(false);
                etLocation.setError(null);
            } else {
                // אם בחר פיזי - מרוקנים ופותחים את שדה המיקום לכתיבה
                if (etLocation.getText().toString().equals("Online")) {
                    etLocation.setText("");
                }
                etLocation.setEnabled(true);
            }
        });
    }

    // פונקציה שמגדירה את הפופ-אפים של בחירת תאריך ושעות (Material Design)
    private void setupPickers() {
        // בחירת תאריך
        etDatePicker.setOnClickListener(v -> {
            // הגבלתי כאן את התאריכים כך שאי אפשר יהיה לקבוע פגישה ביום שכבר עבר (PointForward.now)
            CalendarConstraints.Builder constraintsBuilder = new CalendarConstraints.Builder()
                    .setValidator(DateValidatorPointForward.now());

            MaterialDatePicker<Long> datePicker = MaterialDatePicker.Builder.datePicker()
                    .setTitleText("בחר תאריך")
                    .setSelection(MaterialDatePicker.todayInUtcMilliseconds())
                    .setCalendarConstraints(constraintsBuilder.build())
                    .build();

            datePicker.show(getSupportFragmentManager(), "DATE_PICKER");
            datePicker.addOnPositiveButtonClickListener(selection -> {
                selectedDate.setTimeInMillis(selection);
                SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
                etDatePicker.setText(sdf.format(selectedDate.getTime()));
            });
        });

        // בחירת שעת התחלה
        etStartTime.setOnClickListener(v -> {
            MaterialTimePicker timePicker = new MaterialTimePicker.Builder()
                    .setTimeFormat(TimeFormat.CLOCK_24H)
                    .setHour(12).setMinute(0).setTitleText("שעת התחלה").build();
            timePicker.show(getSupportFragmentManager(), "START_TIME");
            timePicker.addOnPositiveButtonClickListener(t -> {
                etStartTime.setText(String.format(Locale.getDefault(), "%02d:%02d", timePicker.getHour(), timePicker.getMinute()));
            });
        });

        // בחירת שעת סיום
        etEndTime.setOnClickListener(v -> {
            MaterialTimePicker timePicker = new MaterialTimePicker.Builder()
                    .setTimeFormat(TimeFormat.CLOCK_24H)
                    .setHour(13).setMinute(0).setTitleText("שעת סיום").build();
            timePicker.show(getSupportFragmentManager(), "END_TIME");
            timePicker.addOnPositiveButtonClickListener(t -> {
                etEndTime.setText(String.format(Locale.getDefault(), "%02d:%02d", timePicker.getHour(), timePicker.getMinute()));
            });
        });
    }

    // הניווט הכללי של האפליקציה בתפריט התחתון
    private void setupBottomNavigation() {
        navUpcoming.setOnClickListener(v -> {
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

        navProgress.setOnClickListener(v -> {
            Intent intent = new Intent(this, ProgressActivity.class);
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
    }

    // זו הפונקציה המרכזית שרצה כשלוחצים "שמור". היא עושה ולידציות (בדיקות) ושומרת במסד הנתונים
    private void createEvent() {
        // אני שואב את כל מה שהמשתמש הקליד
        String title = etTitle.getText().toString().trim();
        String description = etDescription.getText().toString().trim();
        String date = etDatePicker.getText().toString().trim();
        String startTime = etStartTime.getText().toString().trim();
        String endTime = etEndTime.getText().toString().trim();
        String location = etLocation.getText().toString().trim();

        // בדיקה 1: האם שמו כותרת?
        if (TextUtils.isEmpty(title)) {
            etTitle.setError("חובה להזין כותרת לפגישה");
            etTitle.requestFocus();
            return;
        }

        // בדיקה 2: האם בחרו סוג פגישה (פיזית/אונליין)?
        int selectedId = radioGroupType.getCheckedRadioButtonId();
        if (selectedId == -1) {
            Toast.makeText(this, "אנא בחר סוג פגישה", Toast.LENGTH_SHORT).show();
            return;
        }

        // בדיקה 3: אם הפגישה פיזית - האם הזינו מיקום?
        if (selectedId == R.id.rb_physical && TextUtils.isEmpty(location)) {
            etLocation.setError("חובה להזין מיקום עבור פגישה פיזית");
            etLocation.requestFocus();
            return;
        }

        // בדיקה 4: האם הזינו את כל הזמנים?
        if (TextUtils.isEmpty(date) || TextUtils.isEmpty(startTime) || TextUtils.isEmpty(endTime)) {
            Toast.makeText(this, "חובה לבחור תאריך, שעת התחלה ושעת סיום", Toast.LENGTH_SHORT).show();
            return;
        }

        // בדיקה 5: האם הזמינו לפחות מישהו אחד לפגישה?
        if (selectedParticipantIds.isEmpty()) {
            Toast.makeText(this, "חובה להזמין לפחות משתתף אחד", Toast.LENGTH_LONG).show();
            return;
        }

        String type = selectedId == R.id.rb_online ? "פגישה מקוונת (Online)" : "פגישה פיזית";
        if (selectedId == R.id.rb_online) location = "Online";

        double calculatedHours = 0;
        long meetingStartTimeMillis = 0;

        // שלב בדיקות הזמנים הלוגיות (זמן תקין)
        try {
            SimpleDateFormat sdfFull = new SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault());
            Date currentDateTime = new Date();
            Date fullStartObj = sdfFull.parse(date + " " + startTime);
            Date fullEndObj = sdfFull.parse(date + " " + endTime);

            // מוודא שלא מנסים לקבוע פגישה להיום בשעה שכבר עברה
            if (fullStartObj != null && fullStartObj.before(currentDateTime)) {
                Toast.makeText(this, "לא ניתן לקבוע פגישה לשעה שכבר עברה היום", Toast.LENGTH_LONG).show();
                return;
            }

            // מוודא ששעת הסיום הגיונית ומאוחרת משעת ההתחלה
            if (fullStartObj != null && fullEndObj != null && !fullEndObj.after(fullStartObj)) {
                Toast.makeText(this, "שעת הסיום חייבת להיות מאוחרת משעת ההתחלה", Toast.LENGTH_LONG).show();
                return;
            }

            if (fullStartObj != null) {
                meetingStartTimeMillis = fullStartObj.getTime();
            }

            // פה אני מחשב את משך הפגישה בשעות (יכול להיות מספר עשרוני)
            long diffInMillis = fullEndObj.getTime() - fullStartObj.getTime();
            calculatedHours = diffInMillis / (1000.0 * 60 * 60);
            calculatedHours = Math.round(calculatedHours * 100.0) / 100.0;

        } catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(this, "שגיאה בחישוב התאריכים", Toast.LENGTH_SHORT).show();
            return;
        }

        if (mAuth.getCurrentUser() == null) return;
        String uid = mAuth.getCurrentUser().getUid();
        User admin = new User();
        admin.setId(uid);

        String dateTime = date + " " + startTime;

        // התיקון הקריטי: אנחנו משאירים את ה-ID ריק ("").
        // פונקציית saveEvent של ה-DatabaseService תייצר לו ID אוטומטית בפיירבייס!
        Event event = new Event(
                "", title, description, dateTime, type, location, calculatedHours, admin
        );

        // מוודא שאני לא בטעות ברשימת ה"מוזמנים" ומכניס את כל מי שבחרתי
        if (selectedParticipantIds.contains(uid)) selectedParticipantIds.remove(uid);
        event.setInvitedParticipantIds(selectedParticipantIds);

        // אני אוטומטית נכנס לרשימת המשתתפים ש"אישרו" (כי אני יצרתי את הפגישה)
        List<String> acceptedList = new ArrayList<>();
        acceptedList.add(uid);
        event.setParticipantIds(acceptedList);

        final long finalMeetingStartTime = meetingStartTimeMillis;

        // התיקון השני: שימוש ב-saveEvent הרשמי לשמירה בשרת
        databaseService.saveEvent(event, new DatabaseService.DatabaseCallback<Void>() {
            @Override
            public void onCompleted(Void object) {
                // אם השמירה עברה בהצלחה - אני מתזמן את ההתראה וסוגר את המסך
                scheduleNotification(event, finalMeetingStartTime);
                Toast.makeText(AddEvent.this, "הפגישה נוצרה וההזמנות נשלחו", Toast.LENGTH_SHORT).show();
                finish();
            }

            @Override
            public void onFailed(Exception e) {
                Toast.makeText(AddEvent.this, "שגיאה ביצירת הפגישה: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    // פונקציה שמפעילה את שעון המערכת כדי להקפיץ "פוש" למכשיר
    private void scheduleNotification(Event event, long meetingTimeInMillis) {
        // מחשב בדיוק 15 דקות לפני הפגישה כדי להקפיץ את ההתראה
        long alarmTime = meetingTimeInMillis - (15 * 60 * 1000);

        // אם זמן ההתראה כבר עבר (למשל פגישה לעוד 5 דקות), אין טעם לתזמן
        if (alarmTime < System.currentTimeMillis()) {
            return;
        }

        // שימוש ב-AlarmManager של אנדרואיד כדי להפעיל את ה-BroadcastReceiver שיצרתי
        AlarmManager alarmManager = (AlarmManager) getSystemService(Context.ALARM_SERVICE);
        Intent intent = new Intent(this, MeetingReminderReceiver.class);
        intent.putExtra("EVENT_TITLE", event.getTitle());
        intent.putExtra("EVENT_ID", event.getId());

        PendingIntent pendingIntent = PendingIntent.getBroadcast(
                this,
                event.getId().hashCode(),
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );

        // הגדרה שתעבוד תקין גם בגרסאות אנדרואיד חדשות (דורש אישור לתזמון מדויק)
        if (alarmManager != null) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                if (alarmManager.canScheduleExactAlarms()) {
                    alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, alarmTime, pendingIntent);
                }
            } else {
                alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, alarmTime, pendingIntent);
            }
        }
    }
}