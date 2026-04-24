package com.example.appterapeuta.network;

import com.example.appterapeuta.data.model.ConnectionState;
import com.example.appterapeuta.data.model.DiscoveredRobot;
import com.example.appterapeuta.data.model.RobotConnection;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * Gestiona conexiones TCP simultáneas a N robots, indexadas por robotId.
 */
public class MultiRobotConnectionManager {

    public interface OnConnectionStateChangedListener {
        void onStateChanged(String robotId, ConnectionState state);
    }

    public interface OnMessageListener {
        void onMessage(String robotId, String message);
    }

    private final Map<String, RobotConnection> connections = new HashMap<>();
    private final Map<String, TcpClient> clients = new HashMap<>();
    private OnConnectionStateChangedListener stateListener;
    private OnMessageListener messageListener;

    public void setListener(OnConnectionStateChangedListener listener) {
        this.stateListener = listener;
    }

    public void setMessageListener(OnMessageListener listener) {
        this.messageListener = listener;
    }

    public void connect(DiscoveredRobot robot) {
        String robotId = robot.robotId;

        // Cancelar cliente anterior si existe
        TcpClient existing = clients.remove(robotId);
        if (existing != null) existing.disconnect();

        RobotConnection conn = connections.get(robotId);
        if (conn == null) {
            conn = new RobotConnection(robotId, robot.host, robot.port);
            connections.put(robotId, conn);
        }

        updateState(robotId, ConnectionState.CONNECTING);

        final RobotConnection finalConn = conn;
        final TcpClient[] clientRef = new TcpClient[1];
        TcpClient client = new TcpClient(robot.host, robot.port, new TcpClient.ConnectionListener() {
            @Override public void onConnected() {
                updateState(finalConn.robotId, ConnectionState.CONNECTED);
            }
            @Override public void onMessage(String message) {
                if (messageListener != null) messageListener.onMessage(finalConn.robotId, message);
            }
            @Override public void onDisconnected() {
                updateState(finalConn.robotId, ConnectionState.DISCONNECTED);
                new android.os.Handler(android.os.Looper.getMainLooper()).postDelayed(() -> {
                    if (clients.get(robot.robotId) == clientRef[0] &&
                            connections.containsKey(robot.robotId) &&
                            connections.get(robot.robotId).state != ConnectionState.CONNECTED) {
                        clients.remove(robot.robotId);
                        connect(robot);
                    }
                }, 2000);
            }
            @Override public void onError(Exception e) {
                updateState(finalConn.robotId, ConnectionState.ERROR);
                new android.os.Handler(android.os.Looper.getMainLooper()).postDelayed(() -> {
                    if (clients.get(robot.robotId) == clientRef[0] &&
                            connections.containsKey(robot.robotId) &&
                            connections.get(robot.robotId).state != ConnectionState.CONNECTED) {
                        clients.remove(robot.robotId);
                        connect(robot);
                    }
                }, 2000);
            }
        });
        clientRef[0] = client;

        clients.put(robotId, client);
        client.connect();
    }

    public void send(String robotId, String message) {
        TcpClient client = clients.get(robotId);
        if (client == null) {
            android.util.Log.w("MultiRobotConnMgr", "send(): robot no encontrado: " + robotId);
            return;
        }
        RobotConnection conn = connections.get(robotId);
        if (conn == null || conn.state != ConnectionState.CONNECTED) {
            android.util.Log.w("MultiRobotConnMgr", "send(): robot no conectado: " + robotId);
            return;
        }
        client.send(message);
    }

    public void disconnect(String robotId) {
        TcpClient client = clients.remove(robotId);
        if (client != null) client.disconnect();
        connections.remove(robotId); // eliminar para que la reconexión no se dispare
        updateState(robotId, ConnectionState.DISCONNECTED);
    }

    public void disconnectAll() {
        for (TcpClient client : clients.values()) {
            if (client != null) client.disconnect();
        }
        clients.clear();
        for (String robotId : connections.keySet()) {
            updateState(robotId, ConnectionState.DISCONNECTED);
        }
    }

    public Map<String, RobotConnection> getConnections() {
        return Collections.unmodifiableMap(connections);
    }

    private void updateState(String robotId, ConnectionState state) {
        RobotConnection conn = connections.get(robotId);
        if (conn != null) conn.state = state;
        if (stateListener != null) stateListener.onStateChanged(robotId, state);
    }
}
