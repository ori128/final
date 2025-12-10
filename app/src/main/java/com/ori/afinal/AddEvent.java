package com.ori.afinal;

import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.ori.afinal.model.Event;
import com.ori.afinal.utils.ImageUtil; // <--- תוודא שזה קיים

import java.util.UUID;

public class AddEvent extends AppCompatActivity {

    private ImageView postImageView;
    private EditText editTextTitle, editTextDescription, editTextDateTime,
            editTextType, editTextLocation, editTextStatus, editTextMaxParticipants;
    private Button buttonSubmit;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_add_event);

        // Initialize views
        postImageView = findViewById(R.id.postImageView);
        editTextTitle = findViewById(R.id.editTextTitle);
        editTextDescription = findViewById(R.id.editTextDescription);
        editTextDateTime = findViewById(R.id.editTextDateTime);
        editTextType = findViewById(R.id.editTextType);
        editTextLocation = findViewById(R.id.editTextLocation);
        editTextStatus = findViewById(R.id.editTextStatus);
        editTextMaxParticipants = findViewById(R.id.editTextMaxParticipants);

        buttonSubmit = findViewById(R.id.buttonSubmit);

        // Fix padding for Edge-to-Edge
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets bar = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(bar.left, bar.top, bar.right, bar.bottom);
            return insets;
        });

        // Request permissions if using camera/storage
        ImageUtil.requestPermission(this);

        buttonSubmit.setOnClickListener(v -> createEvent());
    }

    private void createEvent() {

        String id = UUID.randomUUID().toString();
        String title = editTextTitle.getText().toString().trim();
        String description = editTextDescription.getText().toString().trim();
        String dateTime = editTextDateTime.getText().toString().trim();
        String type = editTextType.getText().toString().trim();
        String location = editTextLocation.getText().toString().trim();
        String status = editTextStatus.getText().toString().trim();
        String maxParticipantsStr = editTextMaxParticipants.getText().toString().trim();

        if (title.isEmpty() || description.isEmpty() || dateTime.isEmpty() ||
                type.isEmpty() || location.isEmpty() || status.isEmpty() || maxParticipantsStr.isEmpty()) {

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

        // Convert ImageView to Base64
        String base64Image = ImageUtil.convertTo64Base(postImageView);

        // Create Event
        Event event = new Event(
                id,
                title,
                description,
                dateTime,
                type,
                base64Image,
                location,
                status,
                maxParticipants
        );

        Toast.makeText(this, "Event Created:\n" + event.toString(), Toast.LENGTH_LONG).show();
    }
}
