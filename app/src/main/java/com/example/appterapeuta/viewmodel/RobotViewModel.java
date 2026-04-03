package com.example.appterapeuta.viewmodel;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.example.appterapeuta.data.model.ConnectionState;
import com.example.appterapeuta.data.model.DiscoveredRobot;
import com.example.appterapeuta.data.model.RobotConnection;
import com.example.appterapeuta.network.MultiRobotConnectionManager;
import com.example.appterapeuta.network.NsdDiscoveryManager;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class RobotViewModel extends AndroidViewModel {

    private final MutableLiveData<List<DiscoveredRobot>> discoveredRobots = new MutableLiveData<>(new ArrayList<>());
    private final MutableLiveData<Map<String, RobotConnection>> robotConnections = new MutableLiveData<>(new HashMap<>());

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
    }

    public LiveData<List<DiscoveredRobot>> getDiscoveredRobots() { return discoveredRobots; }
    public LiveData<Map<String, RobotConnection>> getRobotConnections() { return robotConnections; }

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
