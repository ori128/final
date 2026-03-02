package com.ori.afinal;

import android.app.DatePickerDialog;
import android.app.TimePickerDialog;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.auth.FirebaseAuth;
import com.ori.afinal.Services.DatabaseService;
import com.ori.afinal.model.Event;
import com.ori.afinal.model.Notification;
import com.ori.afinal.model.User;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;

public class UpdateEvent extends AppCompatActivity {

    private EditText etTitle, etLocation, etDescription;
    private RadioGroup rgMeetingType;
    private RadioButton rbPhysical, rbOnline;
    private TextInputEditText etDate, etTime;
    private Button btnSaveUpdates, btnCancel, btnUpdateParticipants;
    private TextView tvParticipantsList;

    private DatabaseService databaseService;
    private Event currentEvent;
    private String eventId;
    private String currentUserId;

    // רשימות לניהול המשתתפים
    private List<User> allUsers = new ArrayList<>();
    private List<String> selectedUserIds = new ArrayList<>(); // מי שבחרנו בסוף
    private boolean[] checkedUsersArray; // למעקב אחרי ה-Dialog

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_update_event);

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        databaseService = DatabaseService.getInstance();
        if (FirebaseAuth.getInstance().getCurrentUser() != null) {
            currentUserId = FirebaseAuth.getInstance().getCurrentUser().getUid();
        }

        eventId = getIntent().getStringExtra("EVENT_ID");

        if (eventId == null) {
            Toast.makeText(this, "שגיאה בטעינת פגישה לעריכה", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        initViews();
        setupDateAndTimePickers();
        loadAllUsersFromDB(); // טוען את המשתמשים ברקע
        loadEventData();
    }

    private void initViews() {
        etTitle = findViewById(R.id.et_update_title);
        etLocation = findViewById(R.id.et_update_location);
        etDescription = findViewById(R.id.et_update_description);
        rgMeetingType = findViewById(R.id.rg_update_meeting_type);
        rbPhysical = findViewById(R.id.rb_update_physical);
        rbOnline = findViewById(R.id.rb_update_online);
        etDate = findViewById(R.id.et_update_date);
        etTime = findViewById(R.id.et_update_time);
        btnUpdateParticipants = findViewById(R.id.btn_update_participants);
        tvParticipantsList = findViewById(R.id.tv_update_participants_list);
        btnSaveUpdates = findViewById(R.id.btn_save_updates);
        btnCancel = findViewById(R.id.btn_cancel_update);

        btnCancel.setOnClickListener(v -> finish());
        btnSaveUpdates.setOnClickListener(v -> saveEventUpdates());
        btnUpdateParticipants.setOnClickListener(v -> showParticipantsDialog());
    }

    private void setupDateAndTimePickers() {
        etDate.setOnClickListener(v -> {
            Calendar calendar = Calendar.getInstance();
            DatePickerDialog datePickerDialog = new DatePickerDialog(this,
                    (view, year, month, day) -> etDate.setText(String.format(Locale.getDefault(), "%02d/%02d/%04d", day, month + 1, year)),
                    calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH), calendar.get(Calendar.DAY_OF_MONTH));
            datePickerDialog.show();
        });

        etTime.setOnClickListener(v -> {
            Calendar calendar = Calendar.getInstance();
            TimePickerDialog timePickerDialog = new TimePickerDialog(this,
                    (view, hour, minute) -> etTime.setText(String.format(Locale.getDefault(), "%02d:%02d", hour, minute)),
                    calendar.get(Calendar.HOUR_OF_DAY), calendar.get(Calendar.MINUTE), true);
            timePickerDialog.show();
        });
    }

    // מושך את כל המשתמשים במערכת כדי שנוכל להציג אותם לבחירה
    private void loadAllUsersFromDB() {
        databaseService.getUserList(new DatabaseService.DatabaseCallback<List<User>>() {
            @Override
            public void onCompleted(List<User> users) {
                if (users != null) {
                    // נסנן את המנהל עצמו (כדי שלא יזמין את עצמו)
                    for (User u : users) {
                        if (currentUserId != null && !u.getId().equals(currentUserId)) {
                            allUsers.add(u);
                        }
                    }
                    checkedUsersArray = new boolean[allUsers.size()];
                }
            }
            @Override
            public void onFailed(Exception e) { }
        });
    }

    private void loadEventData() {
        databaseService.getEvent(eventId, new DatabaseService.DatabaseCallback<Event>() {
            @Override
            public void onCompleted(Event event) {
                if (event != null) {
                    currentEvent = event;

                    etTitle.setText(event.getTitle());
                    etLocation.setText(event.getLocation());
                    etDescription.setText(event.getDescription());

                    if ("פגישה מקוונת (Online)".equals(event.getType()) || "Online".equalsIgnoreCase(event.getType())) {
                        rbOnline.setChecked(true);
                    } else {
                        rbPhysical.setChecked(true);
                    }

                    if (event.getDateTime() != null) {
                        String[] parts = event.getDateTime().split(" ");
                        if (parts.length >= 2) {
                            etDate.setText(parts[0]);
                            etTime.setText(parts[1]);
                        } else {
                            etDate.setText(event.getDateTime());
                        }
                    }

                    // הרכבת רשימת המשתתפים שכבר שייכים לפגישה
                    if (event.getParticipantIds() != null) selectedUserIds.addAll(event.getParticipantIds());
                    if (event.getInvitedParticipantIds() != null) selectedUserIds.addAll(event.getInvitedParticipantIds());
                    if (event.getDeclinedParticipantIds() != null) selectedUserIds.addAll(event.getDeclinedParticipantIds());

                    updateParticipantsText();
                }
            }
            @Override
            public void onFailed(Exception e) {
                Toast.makeText(UpdateEvent.this, "נכשל בטעינת פרטי הפגישה", Toast.LENGTH_SHORT).show();
            }
        });
    }

    // פותח חלון בחירה מרובה (Checkboxes) לכל המשתמשים
    private void showParticipantsDialog() {
        if (allUsers.isEmpty()) {
            Toast.makeText(this, "אין משתמשים במערכת", Toast.LENGTH_SHORT).show();
            return;
        }

        String[] userNames = new String[allUsers.size()];
        for (int i = 0; i < allUsers.size(); i++) {
            userNames[i] = allUsers.get(i).getFname() != null ? allUsers.get(i).getFname() : "משתמש";
            checkedUsersArray[i] = selectedUserIds.contains(allUsers.get(i).getId());
        }

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("בחר משתתפים לפגישה");
        builder.setMultiChoiceItems(userNames, checkedUsersArray, (dialog, position, isChecked) -> {
            String uid = allUsers.get(position).getId();
            if (isChecked) {
                if (!selectedUserIds.contains(uid)) selectedUserIds.add(uid);
            } else {
                selectedUserIds.remove(uid);
            }
        });

        builder.setPositiveButton("אישור", (dialog, which) -> updateParticipantsText());
        builder.setNegativeButton("ביטול", null);
        builder.show();
    }

    private void updateParticipantsText() {
        tvParticipantsList.setText("נבחרו " + selectedUserIds.size() + " משתתפים");
    }

    private void saveEventUpdates() {
        if (currentEvent == null) return;

        String title = etTitle.getText().toString().trim();
        String location = etLocation.getText().toString().trim();
        String description = etDescription.getText().toString().trim();
        String date = etDate.getText().toString().trim();
        String time = etTime.getText().toString().trim();
        String type = rbPhysical.isChecked() ? "פגישה פיזית" : "פגישה מקוונת (Online)";

        if (title.isEmpty() || date.isEmpty() || time.isEmpty()) {
            Toast.makeText(this, "נא למלא לפחות כותרת, תאריך ושעה", Toast.LENGTH_SHORT).show();
            return;
        }

        // חישוב מי הוסר, מי התווסף ומי נשאר (לפני שמעדכנים את האובייקט)
        List<String> oldParticipants = new ArrayList<>();
        if (currentEvent.getParticipantIds() != null) oldParticipants.addAll(currentEvent.getParticipantIds());
        if (currentEvent.getInvitedParticipantIds() != null) oldParticipants.addAll(currentEvent.getInvitedParticipantIds());
        if (currentEvent.getDeclinedParticipantIds() != null) oldParticipants.addAll(currentEvent.getDeclinedParticipantIds());

        List<String> removedUsers = new ArrayList<>();
        List<String> addedUsers = new ArrayList<>();
        List<String> retainedUsers = new ArrayList<>();

        for (String uid : oldParticipants) {
            if (!selectedUserIds.contains(uid)) removedUsers.add(uid);
            else retainedUsers.add(uid);
        }
        for (String uid : selectedUserIds) {
            if (!oldParticipants.contains(uid)) addedUsers.add(uid);
        }

        // --- עדכון האובייקט currentEvent ---
        currentEvent.setTitle(title);
        currentEvent.setLocation(location);
        currentEvent.setDescription(description);
        currentEvent.setType(type);
        currentEvent.setDateTime(date + " " + time);

        // מסירים משתמשים שהמנהל מחק
        if (currentEvent.getParticipantIds() != null) currentEvent.getParticipantIds().removeAll(removedUsers);
        if (currentEvent.getInvitedParticipantIds() != null) currentEvent.getInvitedParticipantIds().removeAll(removedUsers);
        if (currentEvent.getDeclinedParticipantIds() != null) currentEvent.getDeclinedParticipantIds().removeAll(removedUsers);

        // מוסיפים משתמשים חדשים לרשימת "מוזמנים" (טרם ענו)
        if (currentEvent.getInvitedParticipantIds() == null) currentEvent.setInvitedParticipantIds(new ArrayList<>());
        currentEvent.getInvitedParticipantIds().addAll(addedUsers);

        // שומרים ל-Firebase
        databaseService.updateEvent(currentEvent, new DatabaseService.DatabaseCallback<Void>() {
            @Override
            public void onCompleted(Void object) {
                // שולחים התראות לכולם בהתאם למצב החדש שלהם!
                sendSmartNotifications(removedUsers, addedUsers, retainedUsers, currentEvent);

                Toast.makeText(UpdateEvent.this, "הפגישה עודכנה בהצלחה!", Toast.LENGTH_SHORT).show();
                finish();
            }

            @Override
            public void onFailed(Exception e) {
                Toast.makeText(UpdateEvent.this, "שגיאה בשמירת הנתונים", Toast.LENGTH_SHORT).show();
            }
        });
    }

    // מחלק את ההתראות לפי הקטגוריות
    private void sendSmartNotifications(List<String> removed, List<String> added, List<String> retained, Event event) {
        long timestamp = System.currentTimeMillis();

        // 1. התראות למי שהוסר מהפגישה
        for (String uid : removed) {
            Notification n = new Notification(databaseService.generateNotificationId(), uid,
                    "הוסרת מהפגישה",
                    "המנהל הסיר אותך מהפגישה: " + event.getTitle(),
                    "REMOVED", event.getId(), timestamp);
            databaseService.sendNotification(n, null);
        }

        // 2. התראות למי שהתווסף (הזמנה חדשה!)
        for (String uid : added) {
            Notification n = new Notification(databaseService.generateNotificationId(), uid,
                    "הזמנה חדשה",
                    "הוזמנת לפגישה: " + event.getTitle(),
                    "INVITE", event.getId(), timestamp);
            databaseService.sendNotification(n, null);
        }

        // 3. התראות למי שנשאר בפגישה (עדכון מידע רגיל)
        for (String uid : retained) {
            Notification n = new Notification(databaseService.generateNotificationId(), uid,
                    "פגישה עודכנה",
                    "פרטי הפגישה '" + event.getTitle() + "' שונו על ידי המנהל.",
                    "UPDATE", event.getId(), timestamp);
            databaseService.sendNotification(n, null);
        }
    }
}