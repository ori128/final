package com.ori.afinal;

import android.app.DatePickerDialog;
import android.app.TimePickerDialog;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.firebase.auth.FirebaseAuth;
import com.ori.afinal.Services.DatabaseService;
import com.ori.afinal.model.Event;
import com.ori.afinal.model.User;

import java.util.ArrayList;
import java.util.Calendar;

public class AddEvent extends AppCompatActivity {

    private EditText editTextTitle, editTextDescription, editTextLocation, editTextMaxParticipants;
    private EditText editTextTime, editTextDate;
    private Spinner editTextType;
    private Button buttonSubmit;

    private DatabaseService databaseService;
    private static final String TAG = "AddEvent";
    private Event event;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_add_event);




        databaseService = DatabaseService.getInstance();

        // Views
        editTextTitle = findViewById(R.id.editTextTitle);
        editTextDescription = findViewById(R.id.editTextDescription);
        editTextLocation = findViewById(R.id.editTextLocation);
        editTextMaxParticipants = findViewById(R.id.editTextMaxParticipants);
        editTextTime = findViewById(R.id.editTextTime);
        editTextDate = findViewById(R.id.editTextDate);
        editTextType = findViewById(R.id.editTextType);
        buttonSubmit = findViewById(R.id.buttonSubmit);

        setupDatePicker();
        setupTimePicker();
        setupTypeSpinner();

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets bar = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(bar.left, bar.top, bar.right, bar.bottom);
            return insets;
        });

        buttonSubmit.setOnClickListener(v -> createEvent());
    }

    // --- DATE PICKER ---
    private void setupDatePicker() {
        editTextDate.setOnClickListener(v -> {
            Calendar c = Calendar.getInstance();
            int year = c.get(Calendar.YEAR);
            int month = c.get(Calendar.MONTH);
            int day = c.get(Calendar.DAY_OF_MONTH);

            DatePickerDialog dpd = new DatePickerDialog(this, (view, y, m, d) -> {
                editTextDate.setText(String.format("%02d/%02d/%04d", d, m + 1, y));
            }, year, month, day);
            dpd.show();
        });
    }

    // --- TIME PICKER (חצאי שעות) ---
    private void setupTimePicker() {
        editTextTime.setOnClickListener(v -> {
            Calendar c = Calendar.getInstance();
            int hour = c.get(Calendar.HOUR_OF_DAY);
            int minute = 0;

            TimePickerDialog tpd = new TimePickerDialog(this, (view, h, m) -> {
                editTextTime.setText(String.format("%02d:%02d", h, m));
            }, hour, minute, true);

            tpd.setOnShowListener(dialog -> {
                try {
                    android.widget.NumberPicker minutePicker = tpd.findViewById(
                            getResources().getIdentifier("android:id/minute", null, null)
                    );
                    if (minutePicker != null) {
                        minutePicker.setMinValue(0);
                        minutePicker.setMaxValue(1);
                        minutePicker.setDisplayedValues(new String[]{"00", "30"});
                    }
                } catch (Exception ignored) {}
            });

            tpd.show();
        });
    }

    // --- TYPE SPINNER ---
    private void setupTypeSpinner() {
        ArrayList<String> types = new ArrayList<>();
        types.add("יום הולדת");
        types.add("יום נישואים");
        types.add("חתונה");
        types.add("על האש");
        types.add("מסיבה");
        types.add("דייט");
        types.add("מפגש חברים");
        types.add("אירוע משפחתי");
        types.add("אירוע עבודה");

        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, types);
        editTextType.setAdapter(adapter);
    }

    // --- CREATE EVENT ---
    private void createEvent() {
        String title = editTextTitle.getText().toString().trim();
        String description = editTextDescription.getText().toString().trim();
        String type = editTextType.getSelectedItem().toString();
        String location = editTextLocation.getText().toString().trim();
        String maxParticipantsStr = editTextMaxParticipants.getText().toString().trim();
        String date = editTextDate.getText().toString().trim();
        String time = editTextTime.getText().toString().trim();

        if (title.isEmpty() || description.isEmpty() || location.isEmpty() ||
                maxParticipantsStr.isEmpty() || date.isEmpty() || time.isEmpty()) {
            Toast.makeText(this, "All fields are required", Toast.LENGTH_SHORT).show();
            return;
        }

        int maxParticipants;
        try {
            maxParticipants = Integer.parseInt(maxParticipantsStr);
        } catch (NumberFormatException e) {
            Toast.makeText(this, "Max participants must be a number", Toast.LENGTH_SHORT).show();
            return;
        }

        String dateTime = date + " " + time;





         FirebaseAuth mAuth= FirebaseAuth.getInstance();

        String userid = mAuth.getUid();
        databaseService.getUser(userid, new DatabaseService.DatabaseCallback<User>() {
            @Override
            public void onCompleted(User user) {
                Log.d(TAG, "Login success, user: " + user.getId());
                // Generate new ID and create Event

                user=new User()
                String id = databaseService.generateEventId();

                 event = new Event(id, title, description, dateTime, type, location, "open",maxParticipants, user);


                Log.d(TAG,event.toString());
                saveEventInDataBase(event);


                }

            @Override
            public void onFailed(Exception e) {

                Toast.makeText(AddEvent.this, "Failed to get user: " + e.getMessage(), Toast.LENGTH_LONG).show();
              return;
            }
        });






        // Save to Firebase

    }

    private void saveEventInDataBase(Event event) {

        databaseService.createNewEvent(event, new DatabaseService.DatabaseCallback<Void>() {
            @Override
            public void onCompleted(Void object) {
                Toast.makeText(AddEvent.this, "Event saved successfully!", Toast.LENGTH_LONG).show();

                Log.d(TAG,event.toString());
            }

            @Override
            public void onFailed(Exception e) {
                Toast.makeText(AddEvent.this, "Failed to save event: " + e.getMessage(), Toast.LENGTH_LONG).show();
            }
        });
    }
}