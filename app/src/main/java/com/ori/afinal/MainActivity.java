package com.ori.afinal;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;

import androidx.appcompat.app.AppCompatActivity;

public class MainActivity extends AppCompatActivity implements View.OnClickListener {

    private Button btnRegister, btnLogin;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        btnRegister = findViewById(R.id.btnRegister);
        btnLogin = findViewById(R.id.btnLogin);
        btnLogin.setOnClickListener(this);
        btnRegister.setOnClickListener(this);
    }

    @Override
    public void onClick(View view) {
        if (view.getId()==btnLogin.getId())
        {
            Intent intent = new Intent(MainActivity.this,Login.class);
            startActivity(intent);
        }
        else if (view==btnRegister)
        {
            Intent intent = new Intent(MainActivity.this, RegisterUser.class);
            startActivity(intent);
        }
    }
}
