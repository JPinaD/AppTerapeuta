package com.example.appterapeuta.ui.sessions;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.appterapeuta.AppTerapeutaApp;
import com.example.appterapeuta.R;
import com.example.appterapeuta.data.model.RobotSessionStatus;
import com.example.appterapeuta.viewmodel.ControlCenterViewModel;
import com.example.appterapeuta.viewmodel.RobotViewModel;
import com.example.appterapeuta.viewmodel.SessionViewModel;

import java.util.ArrayList;
import java.util.List;

public class SessionLiveActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_session_live);

        RobotViewModel robotViewModel             = ((AppTerapeutaApp) getApplication()).getRobotViewModel();
        SessionViewModel sessionViewModel         = ((AppTerapeutaApp) getApplication()).getSessionViewModel();
        ControlCenterViewModel controlCenterViewModel = ((AppTerapeutaApp) getApplication()).getControlCenterViewModel();

        RecyclerView rv = findViewById(R.id.rvRobotStatuses);
        rv.setLayoutManager(new LinearLayoutManager(this));
        SessionStatusAdapter adapter = new SessionStatusAdapter();
        rv.setAdapter(adapter);

        sessionViewModel.getRobotStatuses().observe(this, statuses -> {
            if (statuses == null) return;
            List<RobotSessionStatus> list = new ArrayList<>(statuses.values());
            adapter.setStatuses(list);
        });

        controlCenterViewModel.getLiveStatuses().observe(this, adapter::setLiveStatuses);

        robotViewModel.getActivityEvents().observe(this, event ->
                sessionViewModel.handleIncomingEvent(event));

        // Volver al panel sin finalizar la sesión
        Button btnBack = findViewById(R.id.btnBackToPanel);
        btnBack.setOnClickListener(v -> {
            // Volver al panel principal (limpiar el back stack hasta MainDashboardActivity)
            Intent intent = new Intent(this, com.example.appterapeuta.ui.dashboard.MainDashboardActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
            startActivity(intent);
        });

        // Volver a la pantalla de configuración de sesión
        Button btnBackToSetup = findViewById(R.id.btnBackToSetup);
        btnBackToSetup.setOnClickListener(v -> {
            Intent setupIntent = new Intent(this, com.example.appterapeuta.ui.sessions.SessionSetupActivity.class);
            setupIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
            startActivity(setupIntent);
        });

        // Finalizar sesión
        Button btnEnd = findViewById(R.id.btnEndSession);
        btnEnd.setOnClickListener(v -> {
            btnEnd.setEnabled(false);
            sessionViewModel.endSession(robotViewModel);
            btnEnd.postDelayed(this::finish, 500);
        });
    }
}
