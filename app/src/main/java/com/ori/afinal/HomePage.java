package com.ori.afinal;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.ori.afinal.model.Event;

public class HomePage extends AppCompatActivity implements View.OnClickListener {
    private Button btnaddevent;
    private Button btneventcontrol;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_home_page);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });
        initViews();
    }

    public void initViews() {
        btnaddevent = findViewById(R.id.btnaddevent);
        btnaddevent.setOnClickListener(this);
        btneventcontrol = findViewById(R.id.btneventcontrol);
        btneventcontrol.setOnClickListener(this);

    }

    @Override
    public void onClick(View view) {

        if (view.getId() == btnaddevent.getId()) {
            Intent go = new Intent(HomePage.this, AddEvent.class);
            startActivity(go);
        }
        if (view.getId() == btneventcontrol.getId()) {
            Intent go = new Intent(HomePage.this, EventControl.class);
            startActivity(go);
        }
    }
}
