package com.example.appterapeuta;

import android.app.Application;

import androidx.lifecycle.ViewModelProvider;
import androidx.lifecycle.ViewModelStore;
import androidx.lifecycle.ViewModelStoreOwner;

import com.example.appterapeuta.data.model.RobotConnection;
import com.example.appterapeuta.viewmodel.ControlCenterViewModel;
import com.example.appterapeuta.viewmodel.RobotViewModel;
import com.example.appterapeuta.viewmodel.SessionViewModel;

import java.util.Map;

/**
 * Application que expone ViewModels compartidos entre Activities.
 */
public class AppTerapeutaApp extends Application {

    private ViewModelStore viewModelStore;
    private RobotViewModel robotViewModel;
    private SessionViewModel sessionViewModel;
    private ControlCenterViewModel controlCenterViewModel;

    @Override
    public void onCreate() {
        super.onCreate();
        viewModelStore = new ViewModelStore();
        ViewModelStoreOwner owner = () -> viewModelStore;
        ViewModelProvider.AndroidViewModelFactory factory =
                ViewModelProvider.AndroidViewModelFactory.getInstance(this);
        ViewModelProvider provider = new ViewModelProvider(owner, factory);
        robotViewModel         = provider.get(RobotViewModel.class);
        sessionViewModel       = provider.get(SessionViewModel.class);
        controlCenterViewModel = provider.get(ControlCenterViewModel.class);

        // Actualizar alumno asignado cuando cambia la sesión
        sessionViewModel.getCurrentSession().observeForever(session -> {
            if (session == null || session.state != com.example.appterapeuta.data.model.SessionState.ACTIVE) return;
            for (String robotId : session.participatingRobotIds) {
                String profileId = session.robotToStudentProfileId.get(robotId);
                String studentName = sessionViewModel.getStudentName(profileId);
                controlCenterViewModel.setAssignedStudent(robotId, studentName);
            }
        });        // Telemetría: listener directo para no perder eventos por sobreescritura de LiveData
        robotViewModel.setTelemetryListener((robotId, message) -> {
            try {
                org.json.JSONObject obj = new org.json.JSONObject(message);
                String type = obj.getString("type");
                String payload = obj.optString("payload", null);
                controlCenterViewModel.onActivityEvent(
                        new com.example.appterapeuta.data.model.ActivityEvent(robotId, type, payload));
            } catch (org.json.JSONException ignored) {}
        });
        robotViewModel.getRobotConnections().observeForever(connections -> {
            if (connections == null) return;
            for (Map.Entry<String, RobotConnection> e : connections.entrySet()) {
                controlCenterViewModel.onConnectionStateChanged(e.getKey(), e.getValue().state);
            }
        });
    }

    public RobotViewModel getRobotViewModel()                 { return robotViewModel; }
    public SessionViewModel getSessionViewModel()             { return sessionViewModel; }
    public ControlCenterViewModel getControlCenterViewModel() { return controlCenterViewModel; }
}
