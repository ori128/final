package com.ori.afinal;

import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.EditText;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.datepicker.MaterialDatePicker;
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

public class AddEvent extends AppCompatActivity {

    private EditText etTitle, etDescription, etDateTime, etLocation, etMaxParticipants;
    private RadioGroup radioGroupType;
    private Button btnCreateEvent, btnInviteUsers;
    private TextView tvSelectedCount;

    private DatabaseService databaseService;
    private FirebaseAuth mAuth;
    private Calendar selectedDateTime;

    private List<User> allUsersFromDb = new ArrayList<>();
    private List<String> selectedUserIds = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_event);

        databaseService = DatabaseService.getInstance();
        mAuth = FirebaseAuth.getInstance();

        initViews();
        setupDateTimePicker();

        btnInviteUsers.setOnClickListener(v -> showInviteUsersDialog());
        btnCreateEvent.setOnClickListener(v -> createEvent());
    }

    private void initViews() {
        etTitle = findViewById(R.id.et_event_title);
        etDescription = findViewById(R.id.et_event_description);
        etDateTime = findViewById(R.id.et_event_date_time);
        etLocation = findViewById(R.id.et_event_location);
        etMaxParticipants = findViewById(R.id.et_event_max_participants);
        radioGroupType = findViewById(R.id.radio_group_event_type);
        btnCreateEvent = findViewById(R.id.btn_create_event);
        btnInviteUsers = findViewById(R.id.btn_invite_users);
        tvSelectedCount = findViewById(R.id.tv_selected_users_count);
    }

    private void showInviteUsersDialog() {
        databaseService.getUserList(new DatabaseService.DatabaseCallback<List<User>>() {
            @Override
            public void onCompleted(List<User> users) {
                if (users == null) return;

                String currentUid = mAuth.getCurrentUser().getUid();
                allUsersFromDb.clear();

                // סינון המשתמש הנוכחי בעזרת getId()
                for (User u : users) {
                    if (u.getId() != null && !u.getId().equals(currentUid)) {
                        allUsersFromDb.add(u);
                    }
                }

                String[] userNames = new String[allUsersFromDb.size()];
                boolean[] checkedItems = new boolean[allUsersFromDb.size()];

                for (int i = 0; i < allUsersFromDb.size(); i++) {
                    User u = allUsersFromDb.get(i);
                    userNames[i] = (u.getFullName() != null && !u.getFullName().isEmpty()) ? u.getFullName() : u.getEmail();
                    checkedItems[i] = selectedUserIds.contains(u.getId());
                }

                new AlertDialog.Builder(AddEvent.this)
                        .setTitle("בחר משתמשים להזמנה")
                        .setMultiChoiceItems(userNames, checkedItems, (dialog, which, isChecked) -> {
                            String userId = allUsersFromDb.get(which).getId();
                            if (isChecked) {
                                if (!selectedUserIds.contains(userId)) selectedUserIds.add(userId);
                            } else {
                                selectedUserIds.remove(userId);
                            }
                        })
                        .setPositiveButton("אישור", (dialog, which) -> {
                            tvSelectedCount.setText("נבחרו " + selectedUserIds.size() + " מוזמנים");
                        })
                        .show();
            }

            @Override
            public void onFailed(Exception e) {
                Toast.makeText(AddEvent.this, "שגיאה בטעינת רשימה", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void setupDateTimePicker() {
        selectedDateTime = Calendar.getInstance();
        etDateTime.setOnClickListener(v -> {
            MaterialDatePicker<Long> datePicker = MaterialDatePicker.Builder.datePicker().build();
            datePicker.show(getSupportFragmentManager(), "DATE");
            datePicker.addOnPositiveButtonClickListener(selection -> {
                selectedDateTime.setTimeInMillis(selection);
                MaterialTimePicker timePicker = new MaterialTimePicker.Builder().setTimeFormat(TimeFormat.CLOCK_24H).build();
                timePicker.show(getSupportFragmentManager(), "TIME");
                timePicker.addOnPositiveButtonClickListener(t -> {
                    selectedDateTime.set(Calendar.HOUR_OF_DAY, timePicker.getHour());
                    selectedDateTime.set(Calendar.MINUTE, timePicker.getMinute());
                    etDateTime.setText(new SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()).format(selectedDateTime.getTime()));
                });
            });
        });
    }

    private void createEvent() {
        String title = etTitle.getText().toString().trim();
        String maxStr = etMaxParticipants.getText().toString().trim();

        if (title.isEmpty() || maxStr.isEmpty()) {
            Toast.makeText(this, "מלא שדות חובה", Toast.LENGTH_SHORT).show();
            return;
        }

        User admin = new User();
        admin.setUid(mAuth.getCurrentUser().getUid());

        Event event = new Event(
                databaseService.generateEventId(),
                title,
                etDescription.getText().toString(),
                etDateTime.getText().toString(),
                "Meeting",
                etLocation.getText().toString(),
                Integer.parseInt(maxStr),
                admin
        );

        // הוספת המוזמנים לאירוע
        event.setInvitedUsers(new ArrayList<>(selectedUserIds));

        databaseService.createNewEvent(event, new DatabaseService.DatabaseCallback<Void>() {
            @Override
            public void onCompleted(Void object) {
                Toast.makeText(AddEvent.this, "אירוע נוצר בהצלחה!", Toast.LENGTH_SHORT).show();
                finish();
            }
            @Override
            public void onFailed(Exception e) {
                Toast.makeText(AddEvent.this, "שגיאה בשמירה", Toast.LENGTH_SHORT).show();
            }
        });
    }
}