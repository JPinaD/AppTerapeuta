package com.example.appterapeuta.viewmodel;

import android.app.Application;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.example.appterapeuta.data.model.ActivityEvent;
import com.example.appterapeuta.data.model.ConnectionState;
import com.example.appterapeuta.data.model.DiscoveredRobot;
import com.example.appterapeuta.data.model.RobotConnection;
import com.example.appterapeuta.network.MultiRobotConnectionManager;
import com.example.appterapeuta.network.NsdDiscoveryManager;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class RobotViewModel extends AndroidViewModel {

    private static final String TAG = "RobotViewModel";

    private final MutableLiveData<List<DiscoveredRobot>> discoveredRobots = new MutableLiveData<>(new ArrayList<>());
    private final MutableLiveData<Map<String, RobotConnection>> robotConnections = new MutableLiveData<>(new HashMap<>());
    private final MutableLiveData<ActivityEvent> activityEvents = new MutableLiveData<>();

    // Listener adicional para telemetría (no pasa por LiveData para evitar pérdida de eventos)
    private volatile java.util.function.BiConsumer<String, String> telemetryListener;

    private final NsdDiscoveryManager nsdDiscoveryManager;
    private final MultiRobotConnectionManager connectionManager;

    public RobotViewModel(@NonNull Application application) {
        super(application);
        nsdDiscoveryManager = new NsdDiscoveryManager(application);
        connectionManager = new MultiRobotConnectionManager();
        connectionManager.setListener((robotId, state) -> {
            Map<String, RobotConnection> current = new HashMap<>(connectionManager.getConnections());
            robotConnections.postValue(current);
        });
        connectionManager.setMessageListener((robotId, message) -> {
            try {
                JSONObject obj = new JSONObject(message);
                String type = obj.getString("type");
                String payload = obj.optString("payload", null);
                ActivityEvent event = new ActivityEvent(robotId, type, payload);
                activityEvents.postValue(event);
                // Notificar telemetría directamente sin pasar por LiveData
                if (telemetryListener != null) telemetryListener.accept(robotId, message);
            } catch (JSONException e) {
                Log.w(TAG, "Mensaje entrante no parseable: " + message);
            }
        });
    }

    public void setTelemetryListener(java.util.function.BiConsumer<String, String> listener) {
        this.telemetryListener = listener;
    }

    public LiveData<List<DiscoveredRobot>> getDiscoveredRobots() { return discoveredRobots; }
    public LiveData<Map<String, RobotConnection>> getRobotConnections() { return robotConnections; }
    public LiveData<ActivityEvent> getActivityEvents() { return activityEvents; }

    public void startDiscovery() {
        nsdDiscoveryManager.startDiscovery(new NsdDiscoveryManager.OnRobotDiscoveredListener() {
            @Override
            public void onRobotFound(DiscoveredRobot robot) {
                List<DiscoveredRobot> current = new ArrayList<>(safeList());
                for (int i = 0; i < current.size(); i++) {
                    if (current.get(i).robotId.equals(robot.robotId)) {
                        current.set(i, robot);
                        discoveredRobots.postValue(current);
                        return;
                    }
                }
                current.add(robot);
                discoveredRobots.postValue(current);
                connectionManager.connect(robot); // conectar automáticamente al descubrir
            }

            @Override
            public void onRobotLost(String serviceName) {
                List<DiscoveredRobot> current = new ArrayList<>(safeList());
                current.removeIf(r -> r.serviceName.equals(serviceName));
                discoveredRobots.postValue(current);
            }
        });
    }

    public void stopDiscovery() {
        nsdDiscoveryManager.stopDiscovery();
    }

    public void connect(DiscoveredRobot robot) {
        connectionManager.connect(robot);
    }

    public void disconnect(String robotId) {
        connectionManager.disconnect(robotId);
    }

    public void sendMessage(String robotId, String message) {
        connectionManager.send(robotId, message);
    }

    private List<DiscoveredRobot> safeList() {
        List<DiscoveredRobot> list = discoveredRobots.getValue();
        return list != null ? list : new ArrayList<>();
    }

    @Override
    protected void onCleared() {
        super.onCleared();
        nsdDiscoveryManager.stopDiscovery();
        connectionManager.disconnectAll();
    }
}
