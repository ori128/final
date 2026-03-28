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

    private EditText etTitle, etLocation, etDescription;
    private TextInputEditText etDatePicker, etStartTime, etEndTime;
    private RadioGroup radioGroupType;
    private Button btnSaveEvent, btnBack, btnAddParticipants;
    private TextView tvParticipantsList;

    private DatabaseService databaseService;
    private FirebaseAuth mAuth;

    private Calendar selectedDate;

    // רשימה לשמירת המזהים של המשתתפים שנבחרו
    private List<String> selectedParticipantIds = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_event);

        databaseService = DatabaseService.getInstance();
        mAuth = FirebaseAuth.getInstance();

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

        selectedDate = Calendar.getInstance();

        setupPickers();
        setupRadioGroupListener();

        btnSaveEvent.setOnClickListener(v -> createEvent());
        btnBack.setOnClickListener(v -> finish());

        btnAddParticipants.setOnClickListener(v -> {
            databaseService.getUserList(new DatabaseService.DatabaseCallback<List<User>>() {
                @Override
                public void onCompleted(List<User> users) {
                    if (users == null || users.isEmpty()) {
                        Toast.makeText(AddEvent.this, "אין משתמשים במערכת", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    List<User> otherUsers = new ArrayList<>();
                    String currentUserId = mAuth.getCurrentUser().getUid();
                    for (User u : users) {
                        if (u.getId() != null && !u.getId().equals(currentUserId)) {
                            otherUsers.add(u);
                        }
                    }

                    String[] userNames = new String[otherUsers.size()];
                    boolean[] checkedItems = new boolean[otherUsers.size()];

                    for (int i = 0; i < otherUsers.size(); i++) {
                        userNames[i] = otherUsers.get(i).getFname() != null ? otherUsers.get(i).getFname() : "משתמש";
                        checkedItems[i] = selectedParticipantIds.contains(otherUsers.get(i).getId());
                    }

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

    private void setupRadioGroupListener() {
        radioGroupType.setOnCheckedChangeListener((group, checkedId) -> {
            if (checkedId == R.id.rb_online) {
                etLocation.setText("Online");
                etLocation.setEnabled(false);
            } else {
                if (etLocation.getText().toString().equals("Online")) {
                    etLocation.setText("");
                }
                etLocation.setEnabled(true);
            }
        });
    }

    private void setupPickers() {
        etDatePicker.setOnClickListener(v -> {
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

        etStartTime.setOnClickListener(v -> {
            MaterialTimePicker timePicker = new MaterialTimePicker.Builder()
                    .setTimeFormat(TimeFormat.CLOCK_24H)
                    .setHour(12).setMinute(0).setTitleText("שעת התחלה").build();
            timePicker.show(getSupportFragmentManager(), "START_TIME");
            timePicker.addOnPositiveButtonClickListener(t -> {
                etStartTime.setText(String.format(Locale.getDefault(), "%02d:%02d", timePicker.getHour(), timePicker.getMinute()));
            });
        });

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

    private void createEvent() {
        String title = etTitle.getText().toString().trim();
        String description = etDescription.getText().toString().trim();
        String date = etDatePicker.getText().toString().trim();
        String startTime = etStartTime.getText().toString().trim();
        String endTime = etEndTime.getText().toString().trim();
        String location = etLocation.getText().toString().trim();

        int selectedId = radioGroupType.getCheckedRadioButtonId();
        if (selectedId == -1) {
            Toast.makeText(this, "אנא בחר סוג פגישה", Toast.LENGTH_SHORT).show();
            return;
        }
        RadioButton selectedRadio = findViewById(selectedId);
        String type = selectedRadio.getText().toString();

        if (selectedId == R.id.rb_online) {
            location = "Online";
        }

        if (title.isEmpty() || date.isEmpty() || startTime.isEmpty() || endTime.isEmpty() || type.isEmpty()) {
            Toast.makeText(this, "אנא מלא את כל השדות החיוניים", Toast.LENGTH_SHORT).show();
            return;
        }

        double calculatedHours = 0;

        try {
            // חישוב מבוסס תאריך מלא כדי למנוע בעיות של השוואת זמנים בלי תאריך
            SimpleDateFormat sdfFull = new SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault());

            Date currentDateTime = new Date();
            Date fullStartObj = sdfFull.parse(date + " " + startTime);
            Date fullEndObj = sdfFull.parse(date + " " + endTime);

            // 1. האם שעת ההתחלה עברה?
            if (fullStartObj != null && fullStartObj.before(currentDateTime)) {
                Toast.makeText(this, "לא ניתן לקבוע פגישה לתאריך או שעה שעברו", Toast.LENGTH_SHORT).show();
                return;
            }

            // 2. האם שעת הסיום לפני שעת ההתחלה?
            if (fullStartObj != null && fullEndObj != null && !fullEndObj.after(fullStartObj)) {
                Toast.makeText(this, "שעת הסיום חייבת להיות מאוחרת משעת ההתחלה", Toast.LENGTH_SHORT).show();
                return;
            }

            // 3. חישוב השעות המדויק בהפרש אלפיות השנייה
            long diffInMillis = fullEndObj.getTime() - fullStartObj.getTime();
            calculatedHours = diffInMillis / (1000.0 * 60 * 60);

            // עיגול התוצאה ל-2 ספרות אחרי הנקודה (כדי למנוע תוצאות כמו 1.999999)
            calculatedHours = Math.round(calculatedHours * 100.0) / 100.0;

        } catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(this, "שגיאה בחישוב התאריכים", Toast.LENGTH_SHORT).show();
            return;
        }

        String dateTime = date + " " + startTime;

        if (mAuth.getCurrentUser() == null) {
            Toast.makeText(this, "משתמש לא מחובר", Toast.LENGTH_SHORT).show();
            return;
        }

        String uid = mAuth.getCurrentUser().getUid();
        User admin = new User();
        admin.setId(uid);

        Event event = new Event(
                databaseService.generateEventId(),
                title,
                description,
                dateTime,
                type,
                location,
                calculatedHours,
                admin
        );

        if (selectedParticipantIds.contains(uid)) {
            selectedParticipantIds.remove(uid);
        }

        event.setInvitedParticipantIds(selectedParticipantIds);

        List<String> acceptedList = new ArrayList<>();
        acceptedList.add(uid);
        event.setParticipantIds(acceptedList);

        databaseService.createNewEvent(event, new DatabaseService.DatabaseCallback<Void>() {
            @Override
            public void onCompleted(Void object) {
                Log.d(TAG, "Event created successfully");
                Toast.makeText(AddEvent.this, "הפגישה נוצרה וההזמנות נשלחו", Toast.LENGTH_SHORT).show();
                finish();
            }

            @Override
            public void onFailed(Exception e) {
                Log.e(TAG, "Failed to create event", e);
                Toast.makeText(AddEvent.this, "שגיאה ביצירת הפגישה", Toast.LENGTH_SHORT).show();
            }
        });
    }
}