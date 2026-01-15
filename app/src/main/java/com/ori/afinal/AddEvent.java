package com.ori.afinal;

import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.EditText;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.datepicker.CalendarConstraints;
import com.google.android.material.datepicker.MaterialDatePicker;
import com.google.android.material.timepicker.MaterialTimePicker;
import com.google.android.material.timepicker.TimeFormat;
import com.google.firebase.auth.FirebaseAuth;
import com.ori.afinal.Services.DatabaseService;
import com.ori.afinal.model.Event;
import com.ori.afinal.model.User;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Locale;

public class AddEvent extends AppCompatActivity {

    private static final String TAG = "AddEvent";

    private EditText etTitle, etDescription, etDateTime, etLocation, etMaxParticipants;
    private RadioGroup radioGroupType;
    private Button btnCreateEvent;

    private DatabaseService databaseService;
    private FirebaseAuth mAuth;

    private Calendar selectedDateTime;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_event);

        databaseService = DatabaseService.getInstance();
        mAuth = FirebaseAuth.getInstance();

        etTitle = findViewById(R.id.et_event_title);
        etDescription = findViewById(R.id.et_event_description);
        etDateTime = findViewById(R.id.et_event_date_time);
        etLocation = findViewById(R.id.et_event_location);
        etMaxParticipants = findViewById(R.id.et_event_max_participants);
        radioGroupType = findViewById(R.id.radio_group_event_type);
        btnCreateEvent = findViewById(R.id.btn_create_event);

        setupDateTimePicker();
        btnCreateEvent.setOnClickListener(v -> createEvent());
    }

    private void setupDateTimePicker() {
        selectedDateTime = Calendar.getInstance();

        etDateTime.setOnClickListener(v -> {
            // Material Date Picker
            MaterialDatePicker<Long> datePicker = MaterialDatePicker.Builder.datePicker()
                    .setTitleText("בחר תאריך")
                    .setSelection(MaterialDatePicker.todayInUtcMilliseconds())
                    .build();

            datePicker.show(getSupportFragmentManager(), "DATE_PICKER");

            datePicker.addOnPositiveButtonClickListener(selection -> {
                selectedDateTime.setTimeInMillis(selection);

                // Material Time Picker
                MaterialTimePicker timePicker = new MaterialTimePicker.Builder()
                        .setTimeFormat(TimeFormat.CLOCK_24H)
                        .setHour(selectedDateTime.get(Calendar.HOUR_OF_DAY))
                        .setMinute(selectedDateTime.get(Calendar.MINUTE))
                        .setTitleText("בחר שעה")
                        .build();

                timePicker.show(getSupportFragmentManager(), "TIME_PICKER");

                timePicker.addOnPositiveButtonClickListener(t -> {
                    selectedDateTime.set(Calendar.HOUR_OF_DAY, timePicker.getHour());
                    selectedDateTime.set(Calendar.MINUTE, timePicker.getMinute());

                    SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault());
                    etDateTime.setText(sdf.format(selectedDateTime.getTime()));
                });
            });
        });
    }

    private void createEvent() {
        String title = etTitle.getText().toString().trim();
        String description = etDescription.getText().toString().trim();
        String dateTime = etDateTime.getText().toString().trim();
        String location = etLocation.getText().toString().trim();
        String maxParticipantsStr = etMaxParticipants.getText().toString().trim();

        // סוג האירוע מה- RadioGroup
        int selectedId = radioGroupType.getCheckedRadioButtonId();
        if (selectedId == -1) {
            Toast.makeText(this, "אנא בחר סוג אירוע", Toast.LENGTH_SHORT).show();
            return;
        }
        RadioButton selectedRadio = findViewById(selectedId);
        String type = selectedRadio.getText().toString();

        if (title.isEmpty() || dateTime.isEmpty() || type.isEmpty() || maxParticipantsStr.isEmpty()) {
            Toast.makeText(this, "אנא מלא את כל השדות החיוניים", Toast.LENGTH_SHORT).show();
            return;
        }

        int maxParticipants;
        try {
            maxParticipants = Integer.parseInt(maxParticipantsStr);
        } catch (NumberFormatException e) {
            Toast.makeText(this, "מספר משתתפים לא תקין", Toast.LENGTH_SHORT).show();
            return;
        }

        if (mAuth.getCurrentUser() == null) {
            Toast.makeText(this, "משתמש לא מחובר", Toast.LENGTH_SHORT).show();
            return;
        }

        String uid = mAuth.getCurrentUser().getUid();
        User admin = new User();
        admin.setUid(uid);

        Event event = new Event(
                databaseService.generateEventId(),
                title,
                description,
                dateTime,
                type,
                location,
                maxParticipants,
                admin
        );

        databaseService.createNewEvent(event, new DatabaseService.DatabaseCallback<Void>() {
            @Override
            public void onCompleted(Void object) {
                Log.d(TAG, "Event created successfully");
                Toast.makeText(AddEvent.this, "האירוע נוצר בהצלחה", Toast.LENGTH_SHORT).show();
                finish();
            }

            @Override
            public void onFailed(Exception e) {
                Log.e(TAG, "Failed to create event", e);
                Toast.makeText(AddEvent.this, "שגיאה ביצירת האירוע", Toast.LENGTH_SHORT).show();
            }
        });
    }
}
