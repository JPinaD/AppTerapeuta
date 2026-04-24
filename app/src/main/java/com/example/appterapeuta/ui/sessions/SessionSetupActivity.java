package com.example.appterapeuta.ui.sessions;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.appterapeuta.AppTerapeutaApp;
import com.example.appterapeuta.R;
import com.example.appterapeuta.data.local.entity.StudentProfileEntity;
import com.example.appterapeuta.data.model.ConnectionState;
import com.example.appterapeuta.data.model.DiscoveredRobot;
import com.example.appterapeuta.data.model.RobotConnection;
import com.example.appterapeuta.viewmodel.RobotViewModel;
import com.example.appterapeuta.viewmodel.SessionViewModel;
import com.example.appterapeuta.viewmodel.StudentProfileViewModel;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class SessionSetupActivity extends AppCompatActivity {

    private RobotViewModel robotViewModel;
    private SessionViewModel sessionViewModel;
    private StudentProfileViewModel profileViewModel;

    private SessionRobotAdapter robotAdapter;
    private List<StudentProfileEntity> allProfiles = new ArrayList<>();
    private Map<String, RobotConnection> connections = new HashMap<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_session_setup);

        robotViewModel   = ((AppTerapeutaApp) getApplication()).getRobotViewModel();
        sessionViewModel = ((AppTerapeutaApp) getApplication()).getSessionViewModel();
        profileViewModel = new ViewModelProvider(this).get(StudentProfileViewModel.class);

        RecyclerView rvRobots = findViewById(R.id.rvRobots);
        rvRobots.setLayoutManager(new LinearLayoutManager(this));
        robotAdapter = new SessionRobotAdapter();
        rvRobots.setAdapter(robotAdapter);

        // Spinner de actividad (solo pictogram_v1 por ahora)
        Spinner spinnerActivity = findViewById(R.id.spinnerActivity);
        ArrayAdapter<String> actAdapter = new ArrayAdapter<>(this,
                android.R.layout.simple_spinner_item, new String[]{"Pictogramas (v1)"});
        actAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerActivity.setAdapter(actAdapter);

        profileViewModel.profiles.observe(this, profiles -> {
            allProfiles.clear();
            if (profiles != null) allProfiles.addAll(profiles);
            robotAdapter.setProfiles(allProfiles);
        });

        robotViewModel.getDiscoveredRobots().observe(this, robots ->
                robotAdapter.setRobots(robots, connections));

        robotViewModel.getRobotConnections().observe(this, conns -> {
            connections = conns != null ? conns : new HashMap<>();
            robotAdapter.setConnections(connections);
        });

        findViewById(R.id.btnStartSession).setOnClickListener(v -> startSession());
        findViewById(R.id.back_button).setOnClickListener(v -> finish());
    }

    @Override
    protected void onStart() {
        super.onStart();
        robotViewModel.startDiscovery();
    }

    @Override
    protected void onStop() {
        super.onStop();
        robotViewModel.stopDiscovery();
    }

    private void startSession() {
        List<String> selectedRobotIds = robotAdapter.getSelectedRobotIds();
        if (selectedRobotIds.isEmpty()) {
            Toast.makeText(this, "Selecciona al menos un robot conectado", Toast.LENGTH_SHORT).show();
            return;
        }

        // Verificar que todos los seleccionados están conectados
        List<String> connected = new ArrayList<>();
        for (String id : selectedRobotIds) {
            RobotConnection conn = connections.get(id);
            if (conn != null && conn.state == ConnectionState.CONNECTED) {
                connected.add(id);
            } else {
                Toast.makeText(this, "Robot " + id + " no conectado, se excluye", Toast.LENGTH_SHORT).show();
            }
        }
        if (connected.isEmpty()) {
            Toast.makeText(this, "Ningún robot seleccionado está conectado", Toast.LENGTH_SHORT).show();
            return;
        }

        Map<String, String> robotToProfile = robotAdapter.getRobotToProfileId(connected);
        sessionViewModel.prepareSession(connected, robotToProfile, "pictogram_v1");
        sessionViewModel.startSession(robotViewModel, allProfiles);

        startActivity(new Intent(this, SessionLiveActivity.class));
    }
}
