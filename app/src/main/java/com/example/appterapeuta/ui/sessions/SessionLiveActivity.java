package com.example.appterapeuta.ui.sessions;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.appterapeuta.AppTerapeutaApp;
import com.example.appterapeuta.R;
import com.example.appterapeuta.data.local.entity.IncidentEntity;
import com.example.appterapeuta.data.model.RobotSessionState;
import com.example.appterapeuta.data.model.RobotSessionStatus;
import com.example.appterapeuta.data.repository.IncidentRepository;
import com.example.appterapeuta.util.AppConstants;
import com.example.appterapeuta.viewmodel.ControlCenterViewModel;
import com.example.appterapeuta.viewmodel.RobotViewModel;
import com.example.appterapeuta.viewmodel.SessionViewModel;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class SessionLiveActivity extends AppCompatActivity
        implements PauseTargetDialog.OnRobotsPausedListener,
                   IncidentReasonDialog.OnIncidentConfirmedListener {

    private SessionViewModel sessionViewModel;
    private RobotViewModel robotViewModel;
    private IncidentRepository incidentRepository;
    private LinearLayout layoutPauseControls;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_session_live);

        robotViewModel                                = ((AppTerapeutaApp) getApplication()).getRobotViewModel();
        sessionViewModel                              = ((AppTerapeutaApp) getApplication()).getSessionViewModel();
        ControlCenterViewModel controlCenterViewModel = ((AppTerapeutaApp) getApplication()).getControlCenterViewModel();
        incidentRepository = new IncidentRepository(this);

        RecyclerView rv = findViewById(R.id.rvRobotStatuses);
        rv.setLayoutManager(new LinearLayoutManager(this));
        SessionStatusAdapter adapter = new SessionStatusAdapter();
        rv.setAdapter(adapter);

        sessionViewModel.getRobotStatuses().observe(this, statuses -> {
            if (statuses == null) return;
            List<RobotSessionStatus> list = new ArrayList<>(statuses.values());
            adapter.setStatuses(list);
            updatePauseControls(statuses);
        });

        controlCenterViewModel.getLiveStatuses().observe(this, adapter::setLiveStatuses);

        robotViewModel.getActivityEvents().observe(this, event -> {
            if (event == null) return;
            if (AppConstants.MSG_TURN_DONE.equals(event.type)) {
                sessionViewModel.handleTurnDone(robotViewModel, event.robotId);
            } else {
                sessionViewModel.handleIncomingEvent(event);
            }
        });

        layoutPauseControls = findViewById(R.id.layoutPauseControls);

        // Momento Calma
        findViewById(R.id.btnActivateCalm).setOnClickListener(v ->
                sessionViewModel.activateCalm(robotViewModel));

        // Parada de emergencia
        findViewById(R.id.btnEmergencyStop).setOnClickListener(v -> {
            List<String> robotIds = getParticipatingRobotIds();
            if (robotIds.isEmpty()) return;
            PauseTargetDialog.newInstance(robotIds)
                    .show(getSupportFragmentManager(), "pause_dialog");
        });

        // Reanudar
        findViewById(R.id.btnResume).setOnClickListener(v -> {
            List<String> paused = sessionViewModel.getPausedRobotIds();
            if (!paused.isEmpty()) sessionViewModel.resumeSession(robotViewModel, paused);
        });

        // Suspender desde pausa
        findViewById(R.id.btnSuspend).setOnClickListener(v -> {
            List<String> paused = sessionViewModel.getPausedRobotIds();
            if (!paused.isEmpty()) {
                IncidentReasonDialog.newInstance(String.join(", ", paused))
                        .show(getSupportFragmentManager(), "incident_dialog");
            } else {
                sessionViewModel.endSession(robotViewModel);
                finish();
            }
        });

        // Volver al panel sin finalizar sesión
        findViewById(R.id.btnBackToPanel).setOnClickListener(v -> {
            Intent intent = new Intent(this, com.example.appterapeuta.ui.dashboard.MainDashboardActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
            startActivity(intent);
        });

        // Volver a configuración
        findViewById(R.id.btnBackToSetup).setOnClickListener(v -> {
            Intent setupIntent = new Intent(this, SessionSetupActivity.class);
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

    @Override
    public void onRobotsPause(List<String> robotIds) {
        sessionViewModel.pauseSession(robotViewModel, robotIds);
    }

    @Override
    public void onIncidentConfirmed(String robotId, String reason) {
        if (reason != null && !reason.isEmpty()) {
            com.example.appterapeuta.data.model.Session session =
                    sessionViewModel.getCurrentSession().getValue();
            if (session != null) {
                for (String rid : sessionViewModel.getPausedRobotIds()) {
                    String studentId = session.robotToStudentProfileId.get(rid);
                    incidentRepository.saveIncident(new IncidentEntity(
                            session.sessionId, rid,
                            studentId != null ? studentId : "",
                            System.currentTimeMillis(), reason));
                }
            }
        }
        sessionViewModel.endSession(robotViewModel);
        finish();
    }

    private void updatePauseControls(Map<String, RobotSessionStatus> statuses) {
        boolean anyPaused = false;
        for (RobotSessionStatus s : statuses.values()) {
            if (s.state == RobotSessionState.PAUSED) { anyPaused = true; break; }
        }
        layoutPauseControls.setVisibility(anyPaused ? View.VISIBLE : View.GONE);
    }

    private List<String> getParticipatingRobotIds() {
        Map<String, RobotSessionStatus> statuses = sessionViewModel.getRobotStatuses().getValue();
        if (statuses == null) return new ArrayList<>();
        return new ArrayList<>(statuses.keySet());
    }

}
