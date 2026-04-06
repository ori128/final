package com.ori.afinal;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

// הנה השורה שהייתה חסרה ופותרת את הכל!
import com.ori.afinal.adapter.AdminEventAdapter;
import com.ori.afinal.model.Event;

import java.util.ArrayList;
import java.util.List;

public class MainAdmin extends AppCompatActivity {

    private Button btnLogout;
    private RecyclerView rvEvents;
    private AdminEventAdapter adapter;
    private List<Event> eventList;
    private DatabaseReference eventsRef;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main_admin);

        btnLogout = findViewById(R.id.btn_logout_admin);
        rvEvents = findViewById(R.id.rv_admin_events);

        rvEvents.setLayoutManager(new LinearLayoutManager(this));
        eventList = new ArrayList<>();

        // עכשיו האדפטר מוכר, והמערכת יודעת ש-event הוא מסוג Event
        adapter = new AdminEventAdapter(eventList, event -> showDeleteDialog(event));
        rvEvents.setAdapter(adapter);

        eventsRef = FirebaseDatabase.getInstance().getReference("Events");

        loadAllEvents();

        btnLogout.setOnClickListener(v -> {
            FirebaseAuth.getInstance().signOut();
            Intent intent = new Intent(MainAdmin.this, Login.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
            finish();
        });
    }

    private void loadAllEvents() {
        eventsRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                eventList.clear();
                for (DataSnapshot dataSnapshot : snapshot.getChildren()) {
                    Event event = dataSnapshot.getValue(Event.class);
                    if (event != null) {
                        eventList.add(event);
                    }
                }
                adapter.setEventList(eventList);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(MainAdmin.this, "שגיאה בטעינת פגישות: " + error.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void showDeleteDialog(Event event) {
        new AlertDialog.Builder(this)
                .setTitle("מחיקת פגישה מנהלתית")
                .setMessage("האם אתה בטוח שברצונך למחוק את הפגישה '" + event.getTitle() + "'?\nפעולה זו תמחק את הפגישה לכל המשתמשים ולא ניתן לבטל אותה.")
                .setPositiveButton("מחק לצמיתות", (dialog, which) -> deleteEvent(event))
                .setNegativeButton("ביטול", null)
                .show();
    }

    private void deleteEvent(Event event) {
        // החזרנו את זה ל getId() כמו שכתבת במקור
        if (event.getId() != null) {
            eventsRef.child(event.getId()).removeValue().addOnCompleteListener(task -> {
                if (task.isSuccessful()) {
                    Toast.makeText(MainAdmin.this, "הפגישה נמחקה בהצלחה", Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(MainAdmin.this, "שגיאה במחיקת הפגישה", Toast.LENGTH_SHORT).show();
                }
            });
        } else {
            Toast.makeText(MainAdmin.this, "שגיאה: לא נמצא מזהה לפגישה זו", Toast.LENGTH_SHORT).show();
        }
    }
}