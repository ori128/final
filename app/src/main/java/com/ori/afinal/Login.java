package com.ori.afinal;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.activity.EdgeToEdge;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.ori.afinal.Services.DatabaseService;
import com.ori.afinal.model.User;

public class Login extends AppCompatActivity implements View.OnClickListener {

    private static final String TAG = "LoginActivity";

    private EditText etEmail, etPassword;
    private Button btnLogin, btnBackMain;
    private TextView tvRegister;
    private DatabaseService databaseService;
    private FirebaseAuth mAuth;

    public static final String MyPREFERENCES = "MyPrefs" ;


    SharedPreferences sharedpreferences;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_login);

        // EdgeToEdge padding
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.loginLayout), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });



        // Find views
        etEmail = findViewById(R.id.etEmail);
        etPassword = findViewById(R.id.etPassword);
        btnLogin = findViewById(R.id.btnLogin);
        tvRegister = findViewById(R.id.noAccount);
        btnBackMain = findViewById(R.id.btnBackMain);



        sharedpreferences=getSharedPreferences(MyPREFERENCES, Context.MODE_PRIVATE);


         String email2=sharedpreferences.getString("email","");
         String pass2=sharedpreferences.getString("password","");
        etEmail.setText(email2);
        etPassword.setText(pass2);

        // Initialize Firebase + Database
        databaseService = DatabaseService.getInstance();
        mAuth = FirebaseAuth.getInstance();


        // Set listeners
        btnLogin.setOnClickListener(this);
        tvRegister.setOnClickListener(this);
        btnBackMain.setOnClickListener(v -> {
            Intent intent = new Intent(Login.this, MainActivity.class);
            startActivity(intent);
            finish();
        });
    }

    @Override
    public void onClick(View v) {
        int id = v.getId();

        if (id == btnLogin.getId()) {
            String email = etEmail.getText().toString().trim();
            String password = etPassword.getText().toString().trim();

            if (!checkInput(email, password)) return;

            // Login using Firebase Authentication
            mAuth.signInWithEmailAndPassword(email, password)
                    .addOnCompleteListener(task -> {
                        if (task.isSuccessful()) {
                            FirebaseUser firebaseUser = mAuth.getCurrentUser();
                            if (firebaseUser == null) {
                                etPassword.setError("שגיאה בכניסה, נסה שוב");
                                etPassword.requestFocus();
                                return;
                            }

                            String uid = firebaseUser.getUid();
                            databaseService.getUser(uid, new DatabaseService.DatabaseCallback<User>() {
                                @Override
                                public void onCompleted(User user) {
                                    if (user == null) {
                                        Log.e(TAG, "User data not found in database for UID: " + uid);
                                        etPassword.setError("נתוני משתמש לא נמצאו במסד הנתונים");
                                        etPassword.requestFocus();
                                        return;
                                    }
                                    
                                    Log.d(TAG, "Login success, user: " + user.getId());

                                    Intent homepageIntent = new Intent(Login.this, HomePage.class);
                                    homepageIntent.putExtra("USER_ID", user.getId());
                                    homepageIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                                    startActivity(homepageIntent);
                                }

                                @Override
                                public void onFailed(Exception e) {
                                    Log.e(TAG, "Failed to get user data", e);
                                    etPassword.setError("שגיאה בטעינת נתוני המשתמש");
                                    etPassword.requestFocus();
                                }
                            });

                        } else {
                            etPassword.setError("אימייל או סיסמה שגויים");
                            etPassword.requestFocus();
                            Log.e(TAG, "Login failed", task.getException());
                        }
                    });

        } else if (id == tvRegister.getId()) {
            Intent registerIntent = new Intent(Login.this, RegisterUser.class);
            startActivity(registerIntent);
        }
    }

    private boolean checkInput(String email, String password) {
        if (email.isEmpty() || !email.contains("@")) {
            etEmail.setError("נא להכניס אימייל תקין");
            etEmail.requestFocus();
            return false;
        }

        if (password.isEmpty() || password.length() < 6) {
            etPassword.setError("סיסמה חייבת להיות לפחות 6 תווים");
            etPassword.requestFocus();
            return false;
        }

        return true;
    }
}
