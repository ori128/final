package com.ori.afinal;

import android.content.Intent;
import android.content.res.ColorStateList;
import android.content.res.Configuration;
import android.graphics.Color;
import android.os.Bundle;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.android.material.button.MaterialButton;
import com.google.firebase.auth.FirebaseAuth;
import com.ori.afinal.Services.DatabaseService;
import com.ori.afinal.model.User;

public class ProfileActivity extends AppCompatActivity {

    private ImageButton btnBack;
    private TextView tvAvatarLetter, tvProfileName, tvProfileEmail;
    private MaterialButton btnEditProfile, btnToggleTheme;

    private DatabaseService databaseService;
    private String currentUserId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_profile);

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.header_bg), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(0, systemBars.top, 0, 0);
            return insets;
        });

        databaseService = DatabaseService.getInstance();
        if (FirebaseAuth.getInstance().getCurrentUser() != null) {
            currentUserId = FirebaseAuth.getInstance().getCurrentUser().getUid();
        } else {
            finish();
            return;
        }

        initViews();
        updateThemeButtonUI(); // קריאה לעדכון הכפתור לפי המצב הנוכחי
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadUserProfile();
    }

    private void initViews() {
        btnBack = findViewById(R.id.btn_back);
        tvAvatarLetter = findViewById(R.id.tv_avatar_letter);
        tvProfileName = findViewById(R.id.tv_profile_name);
        tvProfileEmail = findViewById(R.id.tv_profile_email);
        btnEditProfile = findViewById(R.id.btn_edit_profile);
        btnToggleTheme = findViewById(R.id.btn_toggle_theme);

        btnBack.setOnClickListener(v -> finish());

        btnEditProfile.setOnClickListener(v -> {
            Intent intent = new Intent(ProfileActivity.this, EditProfileActivity.class);
            startActivity(intent);
        });

        btnToggleTheme.setOnClickListener(v -> toggleNightMode());
    }

    private void loadUserProfile() {
        databaseService.getUser(currentUserId, new DatabaseService.DatabaseCallback<User>() {
            @Override
            public void onCompleted(User user) {
                if (user != null) {
                    String fname = user.getFname() != null ? user.getFname() : "";
                    String lname = user.getLname() != null ? user.getLname() : ""; // במידה ויש שם משפחה
                    String fullName = fname + " " + lname;
                    String email = user.getEmail() != null ? user.getEmail() : "";

                    tvProfileName.setText(fullName.trim());
                    tvProfileEmail.setText(email);

                    if (!fname.isEmpty()) {
                        tvAvatarLetter.setText(String.valueOf(fname.charAt(0)).toUpperCase());
                    }
                }
            }

            @Override
            public void onFailed(Exception e) {
                Toast.makeText(ProfileActivity.this, "שגיאה בטעינת נתונים", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void updateThemeButtonUI() {
        int currentNightMode = getResources().getConfiguration().uiMode & Configuration.UI_MODE_NIGHT_MASK;
        if (currentNightMode == Configuration.UI_MODE_NIGHT_YES) {
            // אנחנו במצב לילה -> הכפתור יהיה ירוק ויציע לעבור ליום
            btnToggleTheme.setText("עבור למצב רגיל (יום)");
            btnToggleTheme.setBackgroundTintList(ColorStateList.valueOf(Color.parseColor("#10B981"))); // ירוק
        } else {
            // אנחנו במצב יום -> הכפתור יהיה כהה ויציע לעבור ללילה
            btnToggleTheme.setText("עבור למצב לילה");
            btnToggleTheme.setBackgroundTintList(ColorStateList.valueOf(Color.parseColor("#1F2937"))); // כהה
        }
    }

    private void toggleNightMode() {
        int currentNightMode = getResources().getConfiguration().uiMode & Configuration.UI_MODE_NIGHT_MASK;
        if (currentNightMode == Configuration.UI_MODE_NIGHT_YES) {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);
        } else {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES);
        }
    }
}