package com.example.appterapeuta.ui.dashboard;

import android.annotation.SuppressLint;
import android.os.Bundle;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import android.content.Intent;
import android.widget.Button;

import com.example.appterapeuta.ui.activities.ActivityCatalogActivity;
import com.example.appterapeuta.ui.profiles.StudentProfileListActivity;
import com.example.appterapeuta.ui.robots.RobotListActivity;
import com.example.appterapeuta.ui.sessions.SessionSetupActivity;
import com.google.android.material.card.MaterialCardView;

import com.example.appterapeuta.R;

public class MainDashboardActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main_dashboard);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        MaterialCardView cardRobot = findViewById(R.id.card_robot);
        MaterialCardView cardStudent = findViewById(R.id.card_student);
        MaterialCardView cardSession = findViewById(R.id.card_session);
        MaterialCardView cardActivityCatalog = findViewById(R.id.card_activity_catalog);

        cardRobot.setOnClickListener(v -> startActivity(new Intent(this, RobotListActivity.class)));
        cardStudent.setOnClickListener(v -> startActivity(new Intent(this, StudentProfileListActivity.class)));
        cardSession.setOnClickListener(v -> startActivity(new Intent(this, SessionSetupActivity.class)));
        cardActivityCatalog.setOnClickListener(v -> startActivity(new Intent(this, ActivityCatalogActivity.class)));

        Button backButton = findViewById(R.id.back_button);
        backButton.setOnClickListener(v -> finish());
    }
}
