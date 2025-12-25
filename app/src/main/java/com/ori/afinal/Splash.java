package com.ori.afinal;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.widget.ProgressBar;

import androidx.appcompat.app.AppCompatActivity;

public class Splash extends AppCompatActivity {

    ProgressBar progressBar;
    int progress = 0;
    Handler handler = new Handler();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash);

        progressBar = findViewById(R.id.progressBar);

        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                progress++;
                progressBar.setProgress(progress);

                if (progress < 100) {
                    handler.postDelayed(this, 30);
                } else {
                    // מעבר לעמוד הראשי
                    startActivity(new Intent(Splash.this, MainActivity.class));
                    finish();
                }
            }
        }, 30);
    }
}
