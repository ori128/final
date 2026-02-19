package com.ori.afinal;

import android.content.DialogInterface;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
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
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class AddEvent extends AppCompatActivity {

    private EditText etTitle, etLocation, etDescription;
    private TextInputEditText etDatePicker, etStartTime, etEndTime;
    private RadioGroup rgMeetingType;
    private RadioButton rbPhysical, rbOnline;
    private Button btnSave, btnBack, btnAddParticipants;
    private TextView tvParticipantsList;

    private DatabaseService databaseService;
    private FirebaseAuth mAuth;


    private List<User> allUsers = new ArrayList<>();
    private List<String> selectedUserIds = new ArrayList<>();
    private boolean[] selectedUsersState; // מערך בוליאני לדיאלוג הבחירה

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_add_event);

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        databaseService = DatabaseService.getInstance();
        mAuth = FirebaseAuth.getInstance();

        initViews();
        setupPickers();
        loadUsersForSelection();
    }

    private void initViews() {
        etTitle = findViewById(R.id.et_title);
        etLocation = findViewById(R.id.et_location);
        etDescription = findViewById(R.id.et_description);

        etDatePicker = findViewById(R.id.et_date_picker);
        etStartTime = findViewById(R.id.et_start_time);
        etEndTime = findViewById(R.id.et_end_time);

        rgMeetingType = findViewById(R.id.rg_meeting_type);
        rbPhysical = findViewById(R.id.rb_physical);
        rbOnline = findViewById(R.id.rb_online);

        btnAddParticipants = findViewById(R.id.btn_add_participants);
        tvParticipantsList = findViewById(R.id.tv_participants_list);

        btnSave = findViewById(R.id.btn_save_event);
        btnBack = findViewById(R.id.btn_back);

        // לוגיקה לבחירת סוג פגישה
        rgMeetingType.setOnCheckedChangeListener((group, checkedId) -> {
            if (checkedId == R.id.rb_online) {
                etLocation.setText("Online Meeting");
                etLocation.setEnabled(false);
            } else {
                etLocation.setText("");
                etLocation.setEnabled(true);
                etLocation.setHint("הכנס מיקום");
            }
        });

        btnSave.setOnClickListener(v -> saveEvent());
        btnBack.setOnClickListener(v -> finish());

        // כפתור בחירת משתתפים
        btnAddParticipants.setOnClickListener(v -> showUserSelectionDialog());
    }

    private void setupPickers() {
        // בחירת תאריך - עיצוב מודרני
        etDatePicker.setOnClickListener(v -> {
            MaterialDatePicker<Long> datePicker = MaterialDatePicker.Builder.datePicker()
                    .setTitleText("בחר תאריך לפגישה")
                    .setSelection(MaterialDatePicker.todayInUtcMilliseconds())
                    .build();

            datePicker.addOnPositiveButtonClickListener(selection -> {
                SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());
                etDatePicker.setText(sdf.format(new Date(selection)));
            });

            datePicker.show(getSupportFragmentManager(), "DATE_PICKER");
        });

        // בחירת שעת התחלה - עיצוב שעון
        etStartTime.setOnClickListener(v -> showTimePicker(etStartTime));

        // בחירת שעת סיום
        etEndTime.setOnClickListener(v -> showTimePicker(etEndTime));
    }

    private void showTimePicker(EditText targetView) {
        MaterialTimePicker timePicker = new MaterialTimePicker.Builder()
                .setTimeFormat(TimeFormat.CLOCK_24H)
                .setHour(12)
                .setMinute(0)
                .setTitleText("בחר שעה")
                .build();

        timePicker.addOnPositiveButtonClickListener(v -> {
            String time = String.format(Locale.getDefault(), "%02d:%02d", timePicker.getHour(), timePicker.getMinute());
            targetView.setText(time);
        });

        timePicker.show(getSupportFragmentManager(), "TIME_PICKER");
    }

    private void loadUsersForSelection() {
        // טעינת כל המשתמשים מהדאטהבייס כדי להציג אותם בחלונית הבחירה
        databaseService.getUserList(new DatabaseService.DatabaseCallback<List<User>>() {
            @Override
            public void onCompleted(List<User> users) {
                allUsers.clear();
                // הוסף את כולם חוץ מהמשתמש הנוכחי (אני)
                String myId = mAuth.getCurrentUser() != null ? mAuth.getCurrentUser().getUid() : "";
                for (User u : users) {
                    if (!u.getId().equals(myId)) {
                        allUsers.add(u);
                    }
                }
                // אתחול המערך הבוליאני
                selectedUsersState = new boolean[allUsers.size()];
            }

            @Override
            public void onFailed(Exception e) {
                Toast.makeText(AddEvent.this, "שגיאה בטעינת משתמשים", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void showUserSelectionDialog() {
        if (allUsers.isEmpty()) {
            Toast.makeText(this, "אין משתמשים נוספים להזמנה", Toast.LENGTH_SHORT).show();
            return;
        }

        // הכנת רשימת השמות לתצוגה
        String[] userNames = new String[allUsers.size()];
        for (int i = 0; i < allUsers.size(); i++) {
            User u = allUsers.get(i);
            userNames[i] = u.getFname() + " " + u.getLname();
        }

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("בחר משתתפים");
        builder.setMultiChoiceItems(userNames, selectedUsersState, (dialog, which, isChecked) -> {
            selectedUsersState[which] = isChecked;
        });

        builder.setPositiveButton("אישור", (dialog, which) -> {
            selectedUserIds.clear();
            StringBuilder namesDisplay = new StringBuilder();

            for (int i = 0; i < allUsers.size(); i++) {
                if (selectedUsersState[i]) {
                    selectedUserIds.add(allUsers.get(i).getId());
                    if (namesDisplay.length() > 0) namesDisplay.append(", ");
                    namesDisplay.append(allUsers.get(i).getFname());
                }
            }

            if (selectedUserIds.isEmpty()) {
                tvParticipantsList.setText("אף משתתף לא נבחר");
            } else {
                tvParticipantsList.setText("מוזמנים: " + namesDisplay.toString());
            }
        });

        builder.setNegativeButton("ביטול", null);
        builder.show();
    }

    private void saveEvent() {
        String title = etTitle.getText().toString();
        String location = etLocation.getText().toString();
        String desc = etDescription.getText().toString();
        String date = etDatePicker.getText().toString();
        String startTime = etStartTime.getText().toString();
        String endTime = etEndTime.getText().toString();
        boolean isOnline = rbOnline.isChecked();

        if (TextUtils.isEmpty(title) || TextUtils.isEmpty(date) ||
                TextUtils.isEmpty(startTime) || TextUtils.isEmpty(endTime)) {
            Toast.makeText(this, "אנא מלא את כל שדות החובה", Toast.LENGTH_SHORT).show();
            return;
        }

        String eventId = databaseService.generateEventId();

        Event event = new Event(eventId, title, location, desc, date, startTime, endTime, isOnline);

        // הוספת המוזמנים
        event.setInvitedUserIds(selectedUserIds);

        // הוספת המשתמש שיצר את האירוע כמשתתף (אופציונלי, אבל מומלץ)
        if (mAuth.getCurrentUser() != null) {
            event.addInvitee(mAuth.getCurrentUser().getUid());
        }

        databaseService.createNewEvent(event, new DatabaseService.DatabaseCallback<Void>() {
            @Override
            public void onCompleted(Void object) {
                Toast.makeText(AddEvent.this, "הפגישה נוצרה בהצלחה!", Toast.LENGTH_SHORT).show();
                finish();
            }

            @Override
            public void onFailed(Exception e) {
                Toast.makeText(AddEvent.this, "שגיאה ביצירת פגישה", Toast.LENGTH_SHORT).show();
            }
        });
    }
}