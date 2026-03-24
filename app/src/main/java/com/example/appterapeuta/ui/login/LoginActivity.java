package com.example.appterapeuta.ui.login;

import android.os.Bundle;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import android.content.Intent;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import com.example.appterapeuta.R;
import com.example.appterapeuta.ui.dashboard.MainDashboardActivity;
import com.example.appterapeuta.ui.splash.SplashActivity;

public class LoginActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_login);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

// LoginActivity.java
        Button loginButton = findViewById(R.id.login_button);
        EditText usernameEdit = findViewById(R.id.username);
        EditText passwordEdit = findViewById(R.id.password);

        loginButton.setOnClickListener(v -> {
            String username = usernameEdit.getText().toString();
            String password = passwordEdit.getText().toString();
            if (!username.isEmpty() && !password.isEmpty()) {
                Intent intent = new Intent(LoginActivity.this, MainDashboardActivity.class);
                startActivity(intent);
            } else {
                Toast.makeText(this, "Completa todos los campos", Toast.LENGTH_SHORT).show();
            }
        });
        Button backButton = findViewById(R.id.back_button);
        backButton.setOnClickListener(v -> {

            Intent intent = new Intent(LoginActivity.this, SplashActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
            startActivity(intent);
            finish();
        });

    }
}