package com.example.appterapeuta.network;

import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import com.example.appterapeuta.data.model.ConnectionState;
import com.example.appterapeuta.data.model.DiscoveredRobot;
import com.example.appterapeuta.data.model.RobotConnection;
import com.example.appterapeuta.util.AppConstants;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

/**
 * Gestiona conexiones TCP simultáneas a N robots, indexadas por robotId.
 * Incluye heartbeat PING cada 5s para detectar conexiones muertas.
 * Thread-safe: usa ConcurrentHashMap para acceso desde NSD, network y main threads.
 */
public class MultiRobotConnectionManager {

    public interface OnConnectionStateChangedListener {
        void onStateChanged(String robotId, ConnectionState state);
    }

    public interface OnMessageListener {
        void onMessage(String robotId, String message);
    }

    private static final String TAG = "MultiRobotConnMgr";
    private static final long RECONNECT_DELAY_MS = 3000;
    private static final long PING_INTERVAL_MS = 5000;

    // ConcurrentHashMap para acceso thread-safe desde NSD, reader y main threads
    private final ConcurrentHashMap<String, RobotConnection> connections = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, TcpClient> clients = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, DiscoveredRobot> robotInfos = new ConcurrentHashMap<>();
    private volatile OnConnectionStateChangedListener stateListener;
    private volatile OnMessageListener messageListener;

    private final ScheduledExecutorService pingScheduler = Executors.newSingleThreadScheduledExecutor();
    private ScheduledFuture<?> pingTask;
    private final Handler mainHandler = new Handler(Looper.getMainLooper());

    public void setListener(OnConnectionStateChangedListener listener) {
        this.stateListener = listener;
    }

    public void setMessageListener(OnMessageListener listener) {
        this.messageListener = listener;
    }

    public void connect(DiscoveredRobot robot) {
        String robotId = robot.robotId;

        // Si ya estamos conectados o conectando a este robot, no hacer nada
        RobotConnection existingConn = connections.get(robotId);
        if (existingConn != null) {
            if (existingConn.state == ConnectionState.CONNECTING) {
                Log.d(TAG, "Robot ya conectando, ignorando: " + robotId);
                return;
            }
            if (existingConn.state == ConnectionState.CONNECTED) {
                TcpClient existingClient = clients.get(robotId);
                if (existingClient != null && existingClient.isConnected()) {
                    Log.d(TAG, "Robot ya conectado, ignorando: " + robotId);
                    return;
                }
            }
        }

        // Cancelar cliente anterior si existe
        TcpClient existing = clients.remove(robotId);
        if (existing != null) existing.disconnect();

        RobotConnection conn = connections.get(robotId);
        if (conn == null) {
            conn = new RobotConnection(robotId, robot.host, robot.port);
            connections.put(robotId, conn);
        } else {
            conn.host = robot.host;
            conn.port = robot.port;
        }
        robotInfos.put(robotId, robot);

        updateState(robotId, ConnectionState.CONNECTING);

        final RobotConnection finalConn = conn;
        TcpClient client = new TcpClient(robot.host, robot.port, new TcpClient.ConnectionListener() {
            @Override
            public void onConnected() {
                updateState(finalConn.robotId, ConnectionState.CONNECTED);
                startPingIfNeeded();
            }

            @Override
            public void onMessage(String message) {
                // Filtrar PONGs silenciosamente
                if (AppConstants.MSG_PONG.equals(message.trim())) return;
                if (messageListener != null) messageListener.onMessage(finalConn.robotId, message);
            }

            @Override
            public void onDisconnected() {
                updateState(finalConn.robotId, ConnectionState.DISCONNECTED);
                scheduleReconnect(finalConn.robotId);
            }

            @Override
            public void onError(Exception e) {
                Log.w(TAG, "Error conexión " + finalConn.robotId + ": " + e.getMessage());
                updateState(finalConn.robotId, ConnectionState.DISCONNECTED);
                scheduleReconnect(finalConn.robotId);
            }
        });

        clients.put(robotId, client);
        client.connect();
    }

    public void send(String robotId, String message) {
        TcpClient client = clients.get(robotId);
        if (client == null) {
            Log.w(TAG, "send(): robot no encontrado: " + robotId);
            return;
        }
        RobotConnection conn = connections.get(robotId);
        if (conn == null || conn.state != ConnectionState.CONNECTED) {
            Log.w(TAG, "send(): robot no conectado: " + robotId);
            return;
        }
        client.send(message);
    }

    public void disconnect(String robotId) {
        TcpClient client = clients.remove(robotId);
        if (client != null) client.disconnect();
        connections.remove(robotId);
        robotInfos.remove(robotId);
        updateState(robotId, ConnectionState.DISCONNECTED);
        stopPingIfEmpty();
    }

    public void disconnectAll() {
        for (TcpClient client : clients.values()) {
            if (client != null) client.disconnect();
        }
        clients.clear();
        for (String robotId : connections.keySet()) {
            updateState(robotId, ConnectionState.DISCONNECTED);
        }
        connections.clear();
        robotInfos.clear();
        stopPing();
    }

    public Map<String, RobotConnection> getConnections() {
        return new HashMap<>(connections);
    }

    private void scheduleReconnect(String robotId) {
        mainHandler.postDelayed(() -> {
            // Solo reconectar si el robot sigue registrado y no está conectado
            if (!connections.containsKey(robotId)) return;
            RobotConnection conn = connections.get(robotId);
            if (conn != null && conn.state == ConnectionState.CONNECTED) return;

            DiscoveredRobot info = robotInfos.get(robotId);
            if (info != null) {
                Log.d(TAG, "Reintentando conexión a " + robotId);
                connect(info);
            }
        }, RECONNECT_DELAY_MS);
    }

    private void startPingIfNeeded() {
        if (pingTask != null && !pingTask.isCancelled()) return;
        pingTask = pingScheduler.scheduleAtFixedRate(this::sendPingToAll,
                PING_INTERVAL_MS, PING_INTERVAL_MS, TimeUnit.MILLISECONDS);
    }

    private void stopPingIfEmpty() {
        if (clients.isEmpty()) stopPing();
    }

    private void stopPing() {
        if (pingTask != null) {
            pingTask.cancel(false);
            pingTask = null;
        }
    }

    private void sendPingToAll() {
        for (Map.Entry<String, TcpClient> entry : clients.entrySet()) {
            TcpClient client = entry.getValue();
            if (client != null && client.isConnected()) {
                client.send(AppConstants.MSG_PING);
            }
        }
    }

    private void updateState(String robotId, ConnectionState state) {
        RobotConnection conn = connections.get(robotId);
        if (conn != null) conn.state = state;
        if (stateListener != null) stateListener.onStateChanged(robotId, state);
    }
}
