package com.example.appterapeuta.viewmodel;

import android.app.Application;
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
        liveStatuses.postValue(current);
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
        }
        liveStatuses.postValue(current);
    }

    /** Procesa eventos entrantes — filtra ROBOT_STATUS y actualiza liveStatuses. */
    public void onActivityEvent(ActivityEvent event) {
        if (event == null) return;
        if (!AppConstants.MSG_ROBOT_STATUS.equals(event.type)) return;
        if (event.payload == null) return;
        try {
            JSONObject p = new JSONObject(event.payload);
            Map<String, RobotLiveStatus> current = new HashMap<>(safeStatuses());
            RobotLiveStatus status = current.computeIfAbsent(event.robotId, RobotLiveStatus::new);
            status.online = true;
            if (p.has("batteryPct") && !p.isNull("batteryPct"))
                status.batteryPct = p.getInt("batteryPct");
            if (p.has("activityId") && !p.isNull("activityId"))
                status.activityId = p.getString("activityId");
            else status.activityId = null;
            if (p.has("progressPct") && !p.isNull("progressPct"))
                status.progressPct = p.getInt("progressPct");
            else status.progressPct = null;
            liveStatuses.postValue(current);
        } catch (JSONException e) {
            Log.w(TAG, "Error parseando ROBOT_STATUS: " + event.payload);
        }
    }

    public void setAssignedStudent(String robotId, String studentName) {
        Map<String, RobotLiveStatus> current = new HashMap<>(safeStatuses());
        RobotLiveStatus status = current.computeIfAbsent(robotId, RobotLiveStatus::new);
        status.assignedStudentName = studentName;
        liveStatuses.postValue(current);
    }

    private Map<String, RobotLiveStatus> safeStatuses() {
        Map<String, RobotLiveStatus> m = liveStatuses.getValue();
        return m != null ? m : new HashMap<>();
    }
}
