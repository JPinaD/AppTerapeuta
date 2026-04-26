package com.example.appterapeuta.ui.dashboard;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.appterapeuta.AppTerapeutaApp;
import com.example.appterapeuta.R;
import com.example.appterapeuta.data.local.entity.RobotConfigEntity;
import com.example.appterapeuta.data.model.SessionState;
import com.example.appterapeuta.ui.activities.ActivityCatalogActivity;
import com.example.appterapeuta.ui.profiles.StudentProfileListActivity;
import com.example.appterapeuta.ui.sessions.SessionLiveActivity;
import com.example.appterapeuta.ui.sessions.SessionSetupActivity;
import com.example.appterapeuta.viewmodel.ControlCenterViewModel;
import com.example.appterapeuta.viewmodel.SessionViewModel;
import com.google.android.material.card.MaterialCardView;

import android.view.View;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Toast;

import com.example.appterapeuta.util.AppConstants;

public class MainDashboardActivity extends AppCompatActivity {

    private ControlCenterViewModel controlCenterViewModel;
    private SessionViewModel sessionViewModel;
    private RobotDashboardAdapter robotAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main_dashboard);

        controlCenterViewModel = ((AppTerapeutaApp) getApplication()).getControlCenterViewModel();
        sessionViewModel       = ((AppTerapeutaApp) getApplication()).getSessionViewModel();

        // RecyclerView de robots
        RecyclerView rvRobots = findViewById(R.id.rvRobots);
        rvRobots.setLayoutManager(new LinearLayoutManager(this));
        robotAdapter = new RobotDashboardAdapter(robot -> showDeleteRobotDialog(robot));
        rvRobots.setAdapter(robotAdapter);

        // Observar robots configurados + estados en vivo
        controlCenterViewModel.configuredRobots.observe(this, robots ->
                robotAdapter.setData(robots, controlCenterViewModel.getLiveStatuses().getValue()));
        controlCenterViewModel.getLiveStatuses().observe(this, statuses ->
                robotAdapter.setData(controlCenterViewModel.configuredRobots.getValue(), statuses));

        // Badge sesión activa
        MaterialCardView cardActiveSession = findViewById(R.id.cardActiveSession);
        sessionViewModel.getCurrentSession().observe(this, session -> {
            boolean active = session != null && session.state == SessionState.ACTIVE;
            cardActiveSession.setVisibility(active ? View.VISIBLE : View.GONE);
        });
        cardActiveSession.setOnClickListener(v ->
                startActivity(new Intent(this, SessionLiveActivity.class)));

        // Navegación
        findViewById(R.id.card_student).setOnClickListener(v ->
                startActivity(new Intent(this, StudentProfileListActivity.class)));
        findViewById(R.id.card_session).setOnClickListener(v ->
                startActivity(new Intent(this, SessionSetupActivity.class)));
        findViewById(R.id.card_activity_catalog).setOnClickListener(v ->
                startActivity(new Intent(this, ActivityCatalogActivity.class)));

        // Añadir robot
        findViewById(R.id.btnAddRobot).setOnClickListener(v -> showAddRobotDialog());
    }

    private void showAddRobotDialog() {
        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(48, 16, 48, 0);

        EditText etName = new EditText(this);
        etName.setHint("Nombre del robot (ej. Robot-1)");
        layout.addView(etName);

        EditText etId = new EditText(this);
        etId.setHint("ID del robot (ej. Robot-1)");
        layout.addView(etId);

        new AlertDialog.Builder(this)
                .setTitle("Añadir robot")
                .setView(layout)
                .setPositiveButton("Añadir", (d, w) -> {
                    String name = etName.getText().toString().trim();
                    String id   = etId.getText().toString().trim();
                    if (name.isEmpty() || id.isEmpty()) {
                        Toast.makeText(this, "Nombre e ID son obligatorios", Toast.LENGTH_SHORT).show();
                        return;
                    }
                    controlCenterViewModel.addRobot(
                            new RobotConfigEntity(id, name, null, AppConstants.NSD_DEFAULT_PORT));
                })
                .setNegativeButton("Cancelar", null)
                .show();
    }

    private void showDeleteRobotDialog(RobotConfigEntity robot) {
        new AlertDialog.Builder(this)
                .setTitle("Eliminar robot")
                .setMessage("¿Eliminar " + robot.name + "?")
                .setPositiveButton("Eliminar", (d, w) -> controlCenterViewModel.deleteRobot(robot))
                .setNegativeButton("Cancelar", null)
                .show();
    }
}
