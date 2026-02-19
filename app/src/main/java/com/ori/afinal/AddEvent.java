package com.ori.afinal;

import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.EditText;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.datepicker.MaterialDatePicker;
import com.google.android.material.textfield.TextInputEditText;
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

    private EditText etTitle, etLocation, etDescription;
    private TextInputEditText etDatePicker, etStartTime, etEndTime;
    private RadioGroup radioGroupType;
    private Button btnSaveEvent, btnBack, btnAddParticipants;
    private TextView tvParticipantsList;

    private DatabaseService databaseService;
    private FirebaseAuth mAuth;

    private Calendar selectedDate;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_event);

        databaseService = DatabaseService.getInstance();
        mAuth = FirebaseAuth.getInstance();

        // קישור הרכיבים בדיוק לפי ה-IDs של קובץ ה-XML החדש שלך
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

        // מאזינים ללחיצות כפתורים
        btnSaveEvent.setOnClickListener(v -> createEvent());

        btnBack.setOnClickListener(v -> finish()); // כפתור ביטול חזר אחורה

        btnAddParticipants.setOnClickListener(v -> {
            Toast.makeText(this, "הוספת משתתפים תתווסף בהמשך", Toast.LENGTH_SHORT).show();
        });
    }

    private void setupPickers() {
        // בחירת תאריך
        etDatePicker.setOnClickListener(v -> {
            MaterialDatePicker<Long> datePicker = MaterialDatePicker.Builder.datePicker()
                    .setTitleText("בחר תאריך")
                    .setSelection(MaterialDatePicker.todayInUtcMilliseconds())
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
                    .setHour(12)
                    .setMinute(0)
                    .setTitleText("בחר שעת התחלה")
                    .build();

            timePicker.show(getSupportFragmentManager(), "START_TIME_PICKER");

            timePicker.addOnPositiveButtonClickListener(t -> {
                String time = String.format(Locale.getDefault(), "%02d:%02d", timePicker.getHour(), timePicker.getMinute());
                etStartTime.setText(time);
            });
        });

        // בחירת שעת סיום
        etEndTime.setOnClickListener(v -> {
            MaterialTimePicker timePicker = new MaterialTimePicker.Builder()
                    .setTimeFormat(TimeFormat.CLOCK_24H)
                    .setHour(13)
                    .setMinute(0)
                    .setTitleText("בחר שעת סיום")
                    .build();

            timePicker.show(getSupportFragmentManager(), "END_TIME_PICKER");

            timePicker.addOnPositiveButtonClickListener(t -> {
                String time = String.format(Locale.getDefault(), "%02d:%02d", timePicker.getHour(), timePicker.getMinute());
                etEndTime.setText(time);
            });
        });
    }

    private void createEvent() {
        String title = etTitle.getText().toString().trim();
        String description = etDescription.getText().toString().trim();
        String date = etDatePicker.getText().toString().trim();
        String startTime = etStartTime.getText().toString().trim();
        String location = etLocation.getText().toString().trim();

        // סוג האירוע מה- RadioGroup
        int selectedId = radioGroupType.getCheckedRadioButtonId();
        if (selectedId == -1) {
            Toast.makeText(this, "אנא בחר סוג פגישה", Toast.LENGTH_SHORT).show();
            return;
        }
        RadioButton selectedRadio = findViewById(selectedId);
        String type = selectedRadio.getText().toString();

        if (title.isEmpty() || date.isEmpty() || startTime.isEmpty() || type.isEmpty()) {
            Toast.makeText(this, "אנא מלא את כל השדות החיוניים (כותרת, תאריך ושעת התחלה)", Toast.LENGTH_SHORT).show();
            return;
        }

        // חיבור של התאריך והשעה למחרוזת אחת שתתאים למסד הנתונים כפי שהגדרנו במודל
        String dateTime = date + " " + startTime;

        if (mAuth.getCurrentUser() == null) {
            Toast.makeText(this, "משתמש לא מחובר", Toast.LENGTH_SHORT).show();
            return;
        }

        String uid = mAuth.getCurrentUser().getUid();
        User admin = new User();
        admin.setUid(uid);

        // יצירת האירוע עם כל השדות שהגדרנו במחלקה Event
        // הערה: הגדרנו 0 בתור מספר המשתתפים המקסימלי זמנית, כיוון שהשדה לא קיים בעיצוב הנוכחי
        Event event = new Event(
                databaseService.generateEventId(),
                title,
                description,
                dateTime,
                type,
                location,
                0,
                admin
        );

        databaseService.createNewEvent(event, new DatabaseService.DatabaseCallback<Void>() {
            @Override
            public void onCompleted(Void object) {
                Log.d(TAG, "Event created successfully");
                Toast.makeText(AddEvent.this, "הפגישה נוצרה בהצלחה", Toast.LENGTH_SHORT).show();
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