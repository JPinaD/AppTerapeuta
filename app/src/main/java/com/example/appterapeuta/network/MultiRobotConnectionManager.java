package com.example.appterapeuta.network;

import com.example.appterapeuta.data.model.ConnectionState;
import com.example.appterapeuta.data.model.DiscoveredRobot;
import com.example.appterapeuta.data.model.RobotConnection;
import com.example.appterapeuta.util.AppConstants;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * Gestiona conexiones TCP simultáneas a N robots, indexadas por robotId.
 * No tiene límite fijo de conexiones.
 */
public class MultiRobotConnectionManager {

    public interface OnConnectionStateChangedListener {
        void onStateChanged(String robotId, ConnectionState state);
    }

    private final Map<String, RobotConnection> connections = new HashMap<>();
    private final Map<String, TcpClient> clients = new HashMap<>();
    private OnConnectionStateChangedListener listener;

    public void setListener(OnConnectionStateChangedListener listener) {
        this.listener = listener;
    }

    public void connect(DiscoveredRobot robot) {
        String robotId = robot.robotId;

        RobotConnection conn = connections.get(robotId);
        if (conn == null) {
            conn = new RobotConnection(robotId, robot.host, robot.port);
            connections.put(robotId, conn);
        }

        TcpClient existing = clients.get(robotId);
        if (existing != null) existing.disconnect();

        updateState(robotId, ConnectionState.CONNECTING);

        final RobotConnection finalConn = conn;
        TcpClient client = new TcpClient(robot.host, robot.port, new TcpClient.ConnectionListener() {
            @Override public void onConnected() {
                updateState(finalConn.robotId, ConnectionState.CONNECTED);
            }
            @Override public void onMessage(String message) {
                // Mensajes manejados por capas superiores si es necesario
            }
            @Override public void onDisconnected() {
                updateState(finalConn.robotId, ConnectionState.DISCONNECTED);
            }
            @Override public void onError(Exception e) {
                updateState(finalConn.robotId, ConnectionState.ERROR);
            }
        });

        clients.put(robotId, client);
        client.connect();
    }

    public void disconnect(String robotId) {
        TcpClient client = clients.remove(robotId);
        if (client != null) client.disconnect();
        updateState(robotId, ConnectionState.DISCONNECTED);
    }

    public void disconnectAll() {
        for (String robotId : clients.keySet()) {
            TcpClient client = clients.get(robotId);
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
        if (listener != null) listener.onStateChanged(robotId, state);
    }
}
