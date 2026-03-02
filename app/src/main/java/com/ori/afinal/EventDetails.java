package com.ori.afinal;

import android.content.Intent;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.ori.afinal.Services.DatabaseService;
import com.ori.afinal.model.Event;
import com.ori.afinal.model.User;

import java.util.List;

public class EventDetails extends AppCompatActivity {

    private TextView tvTitle, tvType, tvDateTime, tvLocation, tvDescription;
    private LinearLayout llParticipants;
    private Button btnBackHome, btnEditEvent;
    private DatabaseService databaseService;
    private String currentUserId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_event_details);

        databaseService = DatabaseService.getInstance();
        if (FirebaseAuth.getInstance().getCurrentUser() != null) {
            currentUserId = FirebaseAuth.getInstance().getCurrentUser().getUid();
        }

        // קישור משתנים לעיצוב
        tvTitle = findViewById(R.id.tv_detail_title);
        tvType = findViewById(R.id.tv_detail_type);
        tvDateTime = findViewById(R.id.tv_detail_datetime);
        tvLocation = findViewById(R.id.tv_detail_location);
        tvDescription = findViewById(R.id.tv_detail_description);
        llParticipants = findViewById(R.id.ll_detail_participants);
        btnBackHome = findViewById(R.id.btn_back_home);
        btnEditEvent = findViewById(R.id.btn_edit_event);

        btnBackHome.setOnClickListener(v -> finish());

        String eventId = getIntent().getStringExtra("EVENT_ID");

        if (eventId != null) {
            loadEventData(eventId);

            // לחיצה על כפתור העריכה
            btnEditEvent.setOnClickListener(v -> {
                Intent intent = new Intent(EventDetails.this, UpdateEvent.class);
                intent.putExtra("EVENT_ID", eventId);
                startActivity(intent);
            });

        } else {
            Toast.makeText(this, "שגיאה בטעינת הפגישה", Toast.LENGTH_SHORT).show();
            finish();
        }
    }

    private void loadEventData(String eventId) {
        databaseService.getEvent(eventId, new DatabaseService.DatabaseCallback<Event>() {
            @Override
            public void onCompleted(Event event) {
                if (event != null) {
                    tvTitle.setText(event.getTitle() != null ? event.getTitle() : "ללא כותרת");
                    tvType.setText(event.getType() != null ? event.getType() : "סוג לא ידוע");
                    tvDateTime.setText(event.getDateTime() != null ? event.getDateTime() : "לא צוין זמן");
                    tvLocation.setText(event.getLocation() != null ? event.getLocation() : "לא צוין מיקום");

                    if (event.getDescription() != null && !event.getDescription().isEmpty()) {
                        tvDescription.setText(event.getDescription());
                    } else {
                        tvDescription.setText("אין תיאור לפגישה זו.");
                    }

                    // בדיקה: האם המשתמש הנוכחי הוא מנהל הפגישה? אם כן, הראה את כפתור העריכה
                    if (event.getEventAdmin() != null && event.getEventAdmin().getId().equals(currentUserId)) {
                        btnEditEvent.setVisibility(View.VISIBLE);
                    } else {
                        btnEditEvent.setVisibility(View.GONE);
                    }

                    // טעינת משתתפים עם כל הרשימות
                    loadParticipantsNames(event);
                }
            }

            @Override
            public void onFailed(Exception e) {
                Toast.makeText(EventDetails.this, "שגיאה בטעינת נתונים", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void loadParticipantsNames(Event event) {
        llParticipants.removeAllViews();

        databaseService.getUserList(new DatabaseService.DatabaseCallback<List<User>>() {
            @Override
            public void onCompleted(List<User> users) {
                if (users == null) return;

                boolean hasParticipants = false;
                String adminId = event.getEventAdmin() != null ? event.getEventAdmin().getId() : "";

                for (User user : users) {
                    String status = "";
                    String textColor = "";
                    String bgColor = "";
                    boolean isInvolved = false;

                    // בדיקה מה הסטטוס של המשתמש ביחס לפגישה
                    if (user.getId().equals(adminId)) {
                        status = "מנהל";
                        textColor = "#EA580C";
                        bgColor = "#FFEDD5";
                        isInvolved = true;
                    }
                    else if (event.getParticipantIds() != null && event.getParticipantIds().contains(user.getId())) {
                        status = "אישר/ה הגעה";
                        textColor = "#16A34A";
                        bgColor = "#DCFCE7";
                        isInvolved = true;
                    }
                    else if (event.getDeclinedParticipantIds() != null && event.getDeclinedParticipantIds().contains(user.getId())) {
                        status = "דחה/תה";
                        textColor = "#DC2626";
                        bgColor = "#FEE2E2";
                        isInvolved = true;
                    }
                    else if (event.getInvitedParticipantIds() != null && event.getInvitedParticipantIds().contains(user.getId())) {
                        status = "טרם אישר/ה";
                        textColor = "#CA8A04";
                        bgColor = "#FEF9C3";
                        isInvolved = true;
                    }

                    if (isInvolved) {
                        hasParticipants = true;
                        String name = user.getFname() != null ? user.getFname() : "משתמש";
                        addParticipantView(name, status, textColor, bgColor);
                    }
                }

                if (!hasParticipants) {
                    TextView tvEmpty = new TextView(EventDetails.this);
                    tvEmpty.setText("אין מוזמנים לפגישה זו.");
                    llParticipants.addView(tvEmpty);
                }
            }

            @Override
            public void onFailed(Exception e) {
                TextView tvError = new TextView(EventDetails.this);
                tvError.setText("שגיאה בטעינת המשתתפים");
                llParticipants.addView(tvError);
            }
        });
    }

    private void addParticipantView(String name, String status, String textColor, String bgColor) {
        LinearLayout rowLayout = new LinearLayout(this);
        rowLayout.setOrientation(LinearLayout.HORIZONTAL);
        rowLayout.setLayoutParams(new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT));
        rowLayout.setPadding(0, 8, 0, 8);
        rowLayout.setGravity(Gravity.CENTER_VERTICAL);

        TextView tvName = new TextView(this);
        tvName.setText("• " + name);
        tvName.setTextSize(16);
        tvName.setTextColor(Color.parseColor("#1F2937"));
        LinearLayout.LayoutParams nameParams = new LinearLayout.LayoutParams(
                0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f);
        tvName.setLayoutParams(nameParams);

        TextView tvStatus = new TextView(this);
        tvStatus.setText(status);
        tvStatus.setTextSize(12);
        tvStatus.setTextColor(Color.parseColor(textColor));
        tvStatus.setPadding(24, 8, 24, 8);

        GradientDrawable shape = new GradientDrawable();
        shape.setShape(GradientDrawable.RECTANGLE);
        shape.setCornerRadius(30f);
        shape.setColor(Color.parseColor(bgColor));
        tvStatus.setBackground(shape);

        rowLayout.addView(tvName);
        rowLayout.addView(tvStatus);

        llParticipants.addView(rowLayout);
    }

    @Override
    protected void onResume() {
        super.onResume();
        // רענון הנתונים אם המשתמש חזר מעמוד עריכה
        String eventId = getIntent().getStringExtra("EVENT_ID");
        if (eventId != null) {
            loadEventData(eventId);
        }
    }
}