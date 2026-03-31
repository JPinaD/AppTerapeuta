package com.example.appterapeuta.orchestration;

import com.example.appterapeuta.mqtt.MqttManager;
import com.example.appterapeuta.mqtt.MqttTopics;
import com.example.appterapeuta.mqtt.message.RobotCommand;
import com.example.appterapeuta.mqtt.message.RobotEvent;
import com.example.appterapeuta.mqtt.message.RobotStatus;
import com.example.appterapeuta.mqtt.message.SessionControlMessage;
import com.google.gson.Gson;

import java.util.List;

/**
 * Coordina la sesión terapéutica: envía comandos a robots y recibe su estado/eventos.
 * Es el punto central de control de AppTerapeuta durante una sesión activa.
 */
public class SessionOrchestrator {

    private final MqttManager mqtt;
    private final Gson gson = new Gson();

    // Callbacks para notificar a la UI (se asignan desde el ViewModel)
    public interface StatusListener {
        void onStatusReceived(RobotStatus status);
    }
    public interface EventListener {
        void onEventReceived(RobotEvent event);
    }

    private StatusListener statusListener;
    private EventListener eventListener;

    public SessionOrchestrator() {
        this.mqtt = MqttManager.getInstance();
    }

    public void setStatusListener(StatusListener l) { this.statusListener = l; }
    public void setEventListener(EventListener l)   { this.eventListener = l; }

    /** Suscribe a estado y eventos de todos los robots. Llamar al iniciar sesión. */
    public void startListening() {
        mqtt.subscribe(MqttTopics.ALL_STATUS, payload -> {
            RobotStatus status = gson.fromJson(payload, RobotStatus.class);
            if (statusListener != null) statusListener.onStatusReceived(status);
        });
        mqtt.subscribe(MqttTopics.ALL_EVENTS, payload -> {
            RobotEvent event = gson.fromJson(payload, RobotEvent.class);
            if (eventListener != null) eventListener.onEventReceived(event);
        });
    }

    /** Envía un comando a un robot concreto. */
    public void sendCommand(String robotId, RobotCommand command) {
        mqtt.publish(MqttTopics.command(robotId), gson.toJson(command));
    }

    /** Envía el mismo comando a varios robots. */
    public void broadcastCommand(List<String> robotIds, RobotCommand command) {
        String json = gson.toJson(command);
        for (String id : robotIds) {
            mqtt.publish(MqttTopics.command(id), json);
        }
    }

    /** Publica inicio/fin de sesión global. */
    public void publishSessionControl(String type, String sessionId) {
        SessionControlMessage msg = new SessionControlMessage(type, sessionId);
        mqtt.publish(MqttTopics.SESSION_CONTROL, gson.toJson(msg));
    }

    /** Parada de emergencia: envía EMERGENCY_STOP a todos los robots. */
    public void emergencyStop(List<String> robotIds) {
        broadcastCommand(robotIds, new RobotCommand("EMERGENCY_STOP"));
    }
}
