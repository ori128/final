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

    // הגדרת משתני התצוגה של כל השדות שניתן לערוך במסך הזה
    private EditText etTitle, etLocation, etDescription;
    private TextInputEditText etDate, etTime, etEndTime;
    private RadioGroup rgMeetingType;
    private RadioButton rbPhysical, rbOnline;
    private Button btnSaveUpdates, btnCancel, btnUpdateParticipants;
    private TextView tvParticipantsList;

    // אובייקטים לתקשורת עם מסד הנתונים ושמירת נתוני הפגישה שאנחנו עורכים
    private DatabaseService databaseService;
    private Event currentEvent; // שומר את הפגישה המקורית כדי שנוכל לדרוס אותה עם הנתונים החדשים
    private String eventId;
    private String currentUserId;

    // רשימות ומשתנים לניהול המשתתפים (כדי שנוכל להוסיף או להוריד אנשים בעריכה)
    private List<User> allUsers = new ArrayList<>();
    private List<String> selectedUserIds = new ArrayList<>();
    private boolean[] checkedUsersArray;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_update_event);

        // סידור שוליים לתצוגת מסך מלא ונקייה יותר
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        // אתחול החיבור למסד הנתונים ומשיכת ה-ID של המשתמש המחובר כרגע
        databaseService = DatabaseService.getInstance();
        if (FirebaseAuth.getInstance().getCurrentUser() != null) {
            currentUserId = FirebaseAuth.getInstance().getCurrentUser().getUid();
        }

        // אני מושך את ה-ID של הפגישה שהועבר אליי מהמסך הקודם (Intent Extra)
        // כדי לדעת איזו פגישה בדיוק אני צריך לערוך עכשיו
        eventId = getIntent().getStringExtra("EVENT_ID");

        // בדיקת בטיחות - אם בטעות הגעתי למסך בלי ID של פגישה, אני חוסם את הגישה וזורק החוצה
        if (eventId == null) {
            Toast.makeText(this, "שגיאה בטעינת פגישה לעריכה", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        // קריאה לפונקציות שמאתחלות את המסך ומביאות את הנתונים הקיימים
        initViews();
        setupPickers();
        loadAllUsers();
        loadEventData();
    }

    // פונקציה שמקשרת בין משתני הקוד לאלמנטים העיצוביים ב-XML
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

        // הגדרת מאזיני לחיצות (Listeners) לכפתורים השונים
        btnCancel.setOnClickListener(v -> finish()); // כפתור ביטול פשוט סוגר את המסך
        btnSaveUpdates.setOnClickListener(v -> saveEventUpdates());
        btnUpdateParticipants.setOnClickListener(v -> showParticipantsDialog());

        // מאזין שבודק אם שינינו את סוג הפגישה - ואם העברנו לאונליין הוא חוסם את שדה המיקום
        rgMeetingType.setOnCheckedChangeListener((group, checkedId) -> {
            if (checkedId == R.id.rb_update_online) {
                etLocation.setText("Online");
                etLocation.setEnabled(false);
            } else {
                etLocation.setEnabled(true);
            }
        });
    }

    // הגדרת בוחר התאריך והשעונים בעזרת רכיבי Material Design של גוגל למראה מודרני
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

        // שעון מודרני לשעת התחלה (נעזר בפונקציית עזר שכתבתי למטה)
        etTime.setOnClickListener(v -> showMaterialTimePicker(etTime, "שעת התחלה", 12, 0));

        // שעון מודרני לשעת סיום
        etEndTime.setOnClickListener(v -> showMaterialTimePicker(etEndTime, "שעת סיום", 13, 0));
    }

    // פונקציית עזר שיצרתי כדי למנוע כתיבת קוד כפולה של השעון המודרני
    private void showMaterialTimePicker(TextInputEditText targetEditText, String title, int defaultHour, int defaultMinute) {
        MaterialTimePicker timePicker = new MaterialTimePicker.Builder()
                .setTimeFormat(TimeFormat.CLOCK_24H)
                .setHour(defaultHour)
                .setMinute(defaultMinute)
                .setTitleText(title)
                .build();

        timePicker.show(getSupportFragmentManager(), title);
        // ברגע שהמשתמש לוחץ אישור בשעון, אני ממלא את השדה הרלוונטי (התחלה או סיום)
        timePicker.addOnPositiveButtonClickListener(t -> {
            targetEditText.setText(String.format(Locale.getDefault(), "%02d:%02d", timePicker.getHour(), timePicker.getMinute()));
        });
    }

    // מושך את כל המשתמשים מהמסד כדי שאוכל להציג אותם בחלונית הוספת/הסרת משתתפים
    private void loadAllUsers() {
        databaseService.getAllUsers(new DatabaseService.DatabaseCallback<List<User>>() {
            @Override
            public void onCompleted(List<User> users) {
                if (users != null) {
                    allUsers.clear();
                    // מסנן החוצה את עצמי, כדי שלא אוסיף את עצמי פעמיים
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

    // הפונקציה הכי חשובה כאן: מביאה את פרטי הפגישה הנוכחיים וממלאת את כל השדות במסך
    // ככה המשתמש רואה בדיוק מה הוא עורך ולא מתחיל מטופס ריק!
    private void loadEventData() {
        databaseService.getEvent(eventId, new DatabaseService.DatabaseCallback<Event>() {
            @Override
            public void onCompleted(Event event) {
                if (event != null) {
                    currentEvent = event; // אני שומר את האובייקט כדי לדרוס אותו אח"כ
                    etTitle.setText(event.getTitle());
                    etLocation.setText(event.getLocation());
                    etDescription.setText(event.getDescription());

                    // פיצול התאריך והשעה כי הם שמורים כמחרוזת אחת בדאטה-בייס (לדוגמה "2024-05-12 14:30")
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

                    // סימון אוטומטי של הרדיו באטון (פיזית/אונליין) בהתאם למה שהיה שמור
                    if ("פגישה מקוונת (Online)".equals(event.getType()) || "Online".equalsIgnoreCase(event.getType())) {
                        rbOnline.setChecked(true);
                    } else {
                        rbPhysical.setChecked(true);
                    }

                    // שחזור של מי כבר הוזמן כדי שאוכל לסמן אותם ב-V בחלון בחירת משתתפים
                    if (event.getInvitedParticipantIds() != null) selectedUserIds.addAll(event.getInvitedParticipantIds());
                    if (event.getParticipantIds() != null) {
                        for(String id : event.getParticipantIds()) {
                            // מוודא שאני לא מוסיף את היוצר של הפגישה לרשימה הזו
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

    // פותח חלונית (Dialog) לבחירת האנשים. מסמן אוטומטית אנשים שכבר היו מוזמנים.
    private void showParticipantsDialog() {
        if (allUsers.isEmpty()) {
            Toast.makeText(this, "אין משתמשים במערכת", Toast.LENGTH_SHORT).show();
            return;
        }

        String[] names = new String[allUsers.size()];
        for (int i = 0; i < allUsers.size(); i++) {
            names[i] = allUsers.get(i).getFname() != null ? allUsers.get(i).getFname() : "משתמש";
            // אם היוזר כבר נמצא ברשימה - תיבת הסימון תהיה מסומנת מראש
            checkedUsersArray[i] = selectedUserIds.contains(allUsers.get(i).getId());
        }

        new AlertDialog.Builder(this)
                .setTitle("בחר משתתפים לפגישה")
                .setMultiChoiceItems(names, checkedUsersArray, (dialog, which, isChecked) -> {
                    String uid = allUsers.get(which).getId();
                    if (isChecked) {
                        if (!selectedUserIds.contains(uid)) selectedUserIds.add(uid);
                    } else {
                        selectedUserIds.remove(uid); // אם המשתמש הוריד את ה-V, אני מסיר את ההזמנה שלו
                    }
                })
                .setPositiveButton("אישור", (dialog, which) -> updateParticipantsText())
                .setNegativeButton("ביטול", null)
                .show();
    }

    // פונקציית עזר לעדכון הטקסט שמציג כמה אנשים הוזמנו סך הכל
    private void updateParticipantsText() {
        tvParticipantsList.setText("נבחרו " + selectedUserIds.size() + " משתתפים");
    }

    // פונקציית השמירה! פועלת כשלוחצים על הכפתור ודורסת את הנתונים הישנים עם מה שכתבנו עכשיו
    private void saveEventUpdates() {
        if (currentEvent == null) return;

        // אוסף את כל הטקסטים החדשים
        String title = etTitle.getText().toString().trim();
        String location = etLocation.getText().toString().trim();
        String description = etDescription.getText().toString().trim();
        String date = etDate.getText().toString().trim();
        String startTime = etTime.getText().toString().trim();
        String endTime = etEndTime.getText().toString().trim();
        String type = rbPhysical.isChecked() ? "פגישה פיזית" : "פגישה מקוונת (Online)";

        // מבצע ולידציה בסיסית שאין שדות ריקים
        if (title.isEmpty() || date.isEmpty() || startTime.isEmpty() || endTime.isEmpty()) {
            Toast.makeText(this, "נא למלא את כל שדות החובה כולל שעות", Toast.LENGTH_SHORT).show();
            return;
        }

        // מעדכן את האובייקט הקיים עם הנתונים החדשים שלנו
        currentEvent.setTitle(title);
        currentEvent.setLocation(location);
        currentEvent.setDescription(description);
        currentEvent.setType(type);
        currentEvent.setDateTime(date + " " + startTime);
        currentEvent.setEndTime(endTime);
        currentEvent.setInvitedParticipantIds(selectedUserIds);

        // שולח למסד הנתונים את הפגישה המעודכנת
        databaseService.updateEvent(currentEvent, new DatabaseService.DatabaseCallback<Void>() {
            @Override
            public void onCompleted(Void object) {
                Toast.makeText(UpdateEvent.this, "הפגישה עודכנה בהצלחה!", Toast.LENGTH_SHORT).show();
                finish(); // סוגר את המסך וחוזר אחורה
            }
            @Override public void onFailed(Exception e) {
                Toast.makeText(UpdateEvent.this, "שגיאה בשמירת הנתונים", Toast.LENGTH_SHORT).show();
            }
        });
    }
}