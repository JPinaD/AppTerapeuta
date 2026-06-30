package com.example.appterapeuta.viewmodel;

import android.app.Application;
import android.os.Looper;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.example.appterapeuta.data.local.entity.RobotConfigEntity;
import com.example.appterapeuta.data.model.ActivityEvent;
import com.example.appterapeuta.data.model.ConnectionState;
import com.example.appterapeuta.data.model.RobotLiveStatus;
import com.example.appterapeuta.data.repository.RobotConfigRepository;
import com.example.appterapeuta.util.AppConstants;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ControlCenterViewModel extends AndroidViewModel {

    private static final String TAG = "ControlCenterViewModel";

    private final RobotConfigRepository repository;
    public final LiveData<List<RobotConfigEntity>> configuredRobots;
    private final MutableLiveData<Map<String, RobotLiveStatus>> liveStatuses =
            new MutableLiveData<>(new HashMap<>());

    public ControlCenterViewModel(@NonNull Application application) {
        super(application);
        repository = new RobotConfigRepository(application);
        configuredRobots = repository.getAll();
    }

    public LiveData<Map<String, RobotLiveStatus>> getLiveStatuses() { return liveStatuses; }

    public void addRobot(RobotConfigEntity robot) {
        repository.insert(robot);
    }

    public void deleteRobot(RobotConfigEntity robot) {
        repository.delete(robot);
        Map<String, RobotLiveStatus> current = new HashMap<>(safeStatuses());
        current.remove(robot.robotId);
        updateStatuses(current);
    }

    /** Llamar cuando cambia el estado de conexión TCP de un robot. */
    public void onConnectionStateChanged(String robotId, ConnectionState state) {
        Map<String, RobotLiveStatus> current = new HashMap<>(safeStatuses());
        RobotLiveStatus status = current.computeIfAbsent(robotId, RobotLiveStatus::new);
        status.online = (state == ConnectionState.CONNECTED);
        if (!status.online) {
            status.batteryPct = null;
            status.activityId = null;
            status.progressPct = null;
            status.tilted = false;
        }
        updateStatuses(current);
    }

    /** Procesa eventos entrantes — filtra ROBOT_STATUS y TILT_ALERT y actualiza liveStatuses. */
    public void onActivityEvent(ActivityEvent event) {
        if (event == null) return;

        // Manejar alerta de vuelco
        if (AppConstants.MSG_TILT_ALERT.equals(event.type)) {
            handleTiltAlert(event);
            return;
        }

        if (!AppConstants.MSG_ROBOT_STATUS.equals(event.type)) return;
        if (event.payload == null) return;
        try {
            JSONObject p = new JSONObject(event.payload);
            Map<String, RobotLiveStatus> current = new HashMap<>(safeStatuses());
            RobotLiveStatus status = current.computeIfAbsent(event.robotId, RobotLiveStatus::new);
            status.online = true;
            // Un ROBOT_STATUS normal indica que el robot está nivelado — limpiar alerta
            status.tilted = false;
            if (p.has("batteryPct") && !p.isNull("batteryPct"))
                status.batteryPct = p.getInt("batteryPct");
            if (p.has("activityId") && !p.isNull("activityId"))
                status.activityId = p.getString("activityId");
            else status.activityId = null;
            if (p.has("progressPct") && !p.isNull("progressPct"))
                status.progressPct = p.getInt("progressPct");
            else status.progressPct = null;
            updateStatuses(current);
        } catch (JSONException e) {
            Log.w(TAG, "Error parseando ROBOT_STATUS: " + event.payload);
        }
    }

    public void setAssignedStudent(String robotId, String studentName) {
        Map<String, RobotLiveStatus> current = new HashMap<>(safeStatuses());
        RobotLiveStatus status = current.computeIfAbsent(robotId, RobotLiveStatus::new);
        status.assignedStudentName = studentName;
        updateStatuses(current);
    }

    /**
     * Maneja la recepción de TILT_ALERT: marca el robot como inclinado.
     * El flag se limpiará al recibir el siguiente ROBOT_STATUS normal.
     */
    private void handleTiltAlert(ActivityEvent event) {
        String robotId = event.robotId;
        Log.w(TAG, "TILT_ALERT recibido de robot: " + robotId);
        Map<String, RobotLiveStatus> current = new HashMap<>(safeStatuses());
        RobotLiveStatus status = current.computeIfAbsent(robotId, RobotLiveStatus::new);
        status.tilted = true;
        updateStatuses(current);
    }

    /**
     * Actualiza liveStatuses de forma thread-safe.
     * Usa setValue si estamos en main thread (inmediato, sin pérdida),
     * postValue si estamos en hilo de fondo (safety net).
     */
    private void updateStatuses(Map<String, RobotLiveStatus> newStatuses) {
        if (Looper.myLooper() == Looper.getMainLooper()) {
            liveStatuses.setValue(newStatuses);
        } else {
            liveStatuses.postValue(newStatuses);
        }
    }

    private Map<String, RobotLiveStatus> safeStatuses() {
        Map<String, RobotLiveStatus> m = liveStatuses.getValue();
        return m != null ? m : new HashMap<>();
    }
}
