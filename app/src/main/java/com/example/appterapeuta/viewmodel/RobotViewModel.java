package com.example.appterapeuta.viewmodel;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.example.appterapeuta.data.model.DiscoveredRobot;
import com.example.appterapeuta.network.NsdDiscoveryManager;
import com.example.appterapeuta.network.TcpClient;
import com.example.appterapeuta.util.AppConstants;

import java.util.ArrayList;
import java.util.List;

public class RobotViewModel extends AndroidViewModel {

    public enum ConnectionState { IDLE, CONNECTING, CONNECTED, ERROR }

    private final MutableLiveData<List<DiscoveredRobot>> discoveredRobots = new MutableLiveData<>(new ArrayList<>());
    private final MutableLiveData<ConnectionState> connectionState = new MutableLiveData<>(ConnectionState.IDLE);
    private final MutableLiveData<String> lastPongReceived = new MutableLiveData<>();

    private final NsdDiscoveryManager nsdDiscoveryManager;
    private TcpClient tcpClient;

    public RobotViewModel(@NonNull Application application) {
        super(application);
        nsdDiscoveryManager = new NsdDiscoveryManager(application);
    }

    public LiveData<List<DiscoveredRobot>> getDiscoveredRobots() { return discoveredRobots; }
    public LiveData<ConnectionState> getConnectionState() { return connectionState; }
    public LiveData<String> getLastPongReceived() { return lastPongReceived; }

    public void startDiscovery() {
        nsdDiscoveryManager.startDiscovery(new NsdDiscoveryManager.OnRobotDiscoveredListener() {
            @Override
            public void onRobotFound(DiscoveredRobot robot) {
                List<DiscoveredRobot> current = new ArrayList<>(safeList());
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

    public void connectToRobot(DiscoveredRobot robot) {
        if (tcpClient != null) tcpClient.disconnect();
        connectionState.postValue(ConnectionState.CONNECTING);

        tcpClient = new TcpClient(robot.host, robot.port, new TcpClient.ConnectionListener() {
            @Override public void onConnected() {
                connectionState.postValue(ConnectionState.CONNECTED);
                sendPing();
            }
            @Override public void onMessage(String message) {
                if (AppConstants.MSG_PONG.equals(message)) {
                    lastPongReceived.postValue(message);
                }
            }
            @Override public void onDisconnected() {
                connectionState.postValue(ConnectionState.IDLE);
            }
            @Override public void onError(Exception e) {
                connectionState.postValue(ConnectionState.ERROR);
            }
        });
        tcpClient.connect();
    }

    public void sendPing() {
        if (tcpClient != null) tcpClient.send(AppConstants.MSG_PING);
    }

    public void disconnectRobot() {
        if (tcpClient != null) {
            tcpClient.disconnect();
            tcpClient = null;
        }
        connectionState.postValue(ConnectionState.IDLE);
    }

    private List<DiscoveredRobot> safeList() {
        List<DiscoveredRobot> list = discoveredRobots.getValue();
        return list != null ? list : new ArrayList<>();
    }

    @Override
    protected void onCleared() {
        super.onCleared();
        nsdDiscoveryManager.stopDiscovery();
        if (tcpClient != null) tcpClient.disconnect();
    }
}
