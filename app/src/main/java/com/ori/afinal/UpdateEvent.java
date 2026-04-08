package com.ori.afinal;

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
import java.util.List;
import java.util.Locale;

public class UpdateEvent extends AppCompatActivity {

    private EditText etTitle, etLocation, etDescription;
    private TextInputEditText etDate, etTime, etEndTime;
    private RadioGroup rgMeetingType;
    private RadioButton rbPhysical, rbOnline;
    private Button btnSaveUpdates, btnCancel, btnUpdateParticipants;
    private TextView tvParticipantsList;

    private DatabaseService databaseService;
    private Event currentEvent;
    private String eventId;
    private String currentUserId;

    private List<User> allUsers = new ArrayList<>();
    private List<String> selectedUserIds = new ArrayList<>();
    private boolean[] checkedUsersArray;

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
        setupPickers();
        loadAllUsers();
        loadEventData();
    }

    private void initViews() {
        etTitle = findViewById(R.id.et_update_title);
        etLocation = findViewById(R.id.et_update_location);
        etDescription = findViewById(R.id.et_update_description);
        etDate = findViewById(R.id.et_update_date);
        etTime = findViewById(R.id.et_update_time);
        etEndTime = findViewById(R.id.et_update_end_time);

        rgMeetingType = findViewById(R.id.rg_update_meeting_type);
        rbPhysical = findViewById(R.id.rb_update_physical);
        rbOnline = findViewById(R.id.rb_update_online);

        btnUpdateParticipants = findViewById(R.id.btn_update_participants);
        tvParticipantsList = findViewById(R.id.tv_update_participants_list);
        btnSaveUpdates = findViewById(R.id.btn_save_updates);
        btnCancel = findViewById(R.id.btn_cancel_update);

        btnCancel.setOnClickListener(v -> finish());
        btnSaveUpdates.setOnClickListener(v -> saveEventUpdates());
        btnUpdateParticipants.setOnClickListener(v -> showParticipantsDialog());

        rgMeetingType.setOnCheckedChangeListener((group, checkedId) -> {
            if (checkedId == R.id.rb_update_online) {
                etLocation.setText("Online");
                etLocation.setEnabled(false);
            } else {
                etLocation.setEnabled(true);
            }
        });
    }

    // הגדרת בוחר התאריך והשעונים בעזרת Material Design
    private void setupPickers() {
        // בחירת תאריך מודרנית
        etDate.setOnClickListener(v -> {
            MaterialDatePicker<Long> datePicker = MaterialDatePicker.Builder.datePicker()
                    .setTitleText("בחר תאריך")
                    .setSelection(MaterialDatePicker.todayInUtcMilliseconds())
                    .build();

            datePicker.show(getSupportFragmentManager(), "DATE_PICKER");
            datePicker.addOnPositiveButtonClickListener(selection -> {
                Calendar selectedDate = Calendar.getInstance();
                selectedDate.setTimeInMillis(selection);
                SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
                etDate.setText(sdf.format(selectedDate.getTime()));
            });
        });

        // שעון מודרני לשעת התחלה
        etTime.setOnClickListener(v -> showMaterialTimePicker(etTime, "שעת התחלה", 12, 0));

        // שעון מודרני לשעת סיום
        etEndTime.setOnClickListener(v -> showMaterialTimePicker(etEndTime, "שעת סיום", 13, 0));
    }

    // פונקציית עזר להצגת השעון המודרני
    private void showMaterialTimePicker(TextInputEditText targetEditText, String title, int defaultHour, int defaultMinute) {
        MaterialTimePicker timePicker = new MaterialTimePicker.Builder()
                .setTimeFormat(TimeFormat.CLOCK_24H)
                .setHour(defaultHour)
                .setMinute(defaultMinute)
                .setTitleText(title)
                .build();

        timePicker.show(getSupportFragmentManager(), title);
        timePicker.addOnPositiveButtonClickListener(t -> {
            targetEditText.setText(String.format(Locale.getDefault(), "%02d:%02d", timePicker.getHour(), timePicker.getMinute()));
        });
    }

    private void loadAllUsers() {
        databaseService.getAllUsers(new DatabaseService.DatabaseCallback<List<User>>() {
            @Override
            public void onCompleted(List<User> users) {
                if (users != null) {
                    allUsers.clear();
                    for (User u : users) {
                        if (currentUserId != null && !u.getId().equals(currentUserId)) {
                            allUsers.add(u);
                        }
                    }
                    checkedUsersArray = new boolean[allUsers.size()];
                }
            }
            @Override public void onFailed(Exception e) {}
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

                    if (event.getDateTime() != null && event.getDateTime().contains(" ")) {
                        String[] dt = event.getDateTime().split(" ");
                        if (dt.length >= 2) {
                            etDate.setText(dt[0]);
                            etTime.setText(dt[1]);
                        }
                    }

                    if (event.getEndTime() != null) {
                        etEndTime.setText(event.getEndTime());
                    }

                    if ("פגישה מקוונת (Online)".equals(event.getType()) || "Online".equalsIgnoreCase(event.getType())) {
                        rbOnline.setChecked(true);
                    } else {
                        rbPhysical.setChecked(true);
                    }

                    if (event.getInvitedParticipantIds() != null) selectedUserIds.addAll(event.getInvitedParticipantIds());
                    if (event.getParticipantIds() != null) {
                        for(String id : event.getParticipantIds()) {
                            if(event.getEventAdmin() != null && !id.equals(event.getEventAdmin().getId())) {
                                selectedUserIds.add(id);
                            }
                        }
                    }
                    updateParticipantsText();
                }
            }
            @Override public void onFailed(Exception e) {}
        });
    }

    private void showParticipantsDialog() {
        if (allUsers.isEmpty()) {
            Toast.makeText(this, "אין משתמשים במערכת", Toast.LENGTH_SHORT).show();
            return;
        }

        String[] names = new String[allUsers.size()];
        for (int i = 0; i < allUsers.size(); i++) {
            names[i] = allUsers.get(i).getFname() != null ? allUsers.get(i).getFname() : "משתמש";
            checkedUsersArray[i] = selectedUserIds.contains(allUsers.get(i).getId());
        }

        new AlertDialog.Builder(this)
                .setTitle("בחר משתתפים לפגישה")
                .setMultiChoiceItems(names, checkedUsersArray, (dialog, which, isChecked) -> {
                    String uid = allUsers.get(which).getId();
                    if (isChecked) {
                        if (!selectedUserIds.contains(uid)) selectedUserIds.add(uid);
                    } else {
                        selectedUserIds.remove(uid);
                    }
                })
                .setPositiveButton("אישור", (dialog, which) -> updateParticipantsText())
                .setNegativeButton("ביטול", null)
                .show();
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
        String startTime = etTime.getText().toString().trim();
        String endTime = etEndTime.getText().toString().trim();
        String type = rbPhysical.isChecked() ? "פגישה פיזית" : "פגישה מקוונת (Online)";

        if (title.isEmpty() || date.isEmpty() || startTime.isEmpty() || endTime.isEmpty()) {
            Toast.makeText(this, "נא למלא את כל שדות החובה כולל שעות", Toast.LENGTH_SHORT).show();
            return;
        }

        currentEvent.setTitle(title);
        currentEvent.setLocation(location);
        currentEvent.setDescription(description);
        currentEvent.setType(type);
        currentEvent.setDateTime(date + " " + startTime);
        currentEvent.setEndTime(endTime);
        currentEvent.setInvitedParticipantIds(selectedUserIds);

        databaseService.updateEvent(currentEvent, new DatabaseService.DatabaseCallback<Void>() {
            @Override
            public void onCompleted(Void object) {
                Toast.makeText(UpdateEvent.this, "הפגישה עודכנה בהצלחה!", Toast.LENGTH_SHORT).show();
                finish();
            }
            @Override public void onFailed(Exception e) {
                Toast.makeText(UpdateEvent.this, "שגיאה בשמירת הנתונים", Toast.LENGTH_SHORT).show();
            }
        });
    }
}