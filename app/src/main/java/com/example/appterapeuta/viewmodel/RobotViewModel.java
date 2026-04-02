package com.example.appterapeuta.viewmodel;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.example.appterapeuta.data.model.DiscoveredRobot;
import com.example.appterapeuta.network.NsdDiscoveryManager;

import java.util.ArrayList;
import java.util.List;

public class RobotViewModel extends AndroidViewModel {

    private final MutableLiveData<List<DiscoveredRobot>> discoveredRobots = new MutableLiveData<>(new ArrayList<>());
    private final NsdDiscoveryManager nsdDiscoveryManager;

    public RobotViewModel(@NonNull Application application) {
        super(application);
        nsdDiscoveryManager = new NsdDiscoveryManager(application);
    }

    public LiveData<List<DiscoveredRobot>> getDiscoveredRobots() {
        return discoveredRobots;
    }

    public void startDiscovery() {
        nsdDiscoveryManager.startDiscovery(new NsdDiscoveryManager.OnRobotDiscoveredListener() {
            @Override
            public void onRobotFound(DiscoveredRobot robot) {
                List<DiscoveredRobot> current = new ArrayList<>(safeList());
                // Evitar duplicados por nombre de servicio
                for (int i = 0; i < current.size(); i++) {
                    if (current.get(i).serviceName.equals(robot.serviceName)) {
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

    private List<DiscoveredRobot> safeList() {
        List<DiscoveredRobot> list = discoveredRobots.getValue();
        return list != null ? list : new ArrayList<>();
    }

    @Override
    protected void onCleared() {
        super.onCleared();
        nsdDiscoveryManager.stopDiscovery();
    }
}
