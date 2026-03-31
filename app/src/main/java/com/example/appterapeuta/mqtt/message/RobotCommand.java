package com.example.appterapeuta.mqtt.message;

/**
 * Comando enviado por AppTerapeuta a un robot concreto.
 * Se serializa a JSON con Gson antes de publicar.
 *
 * Tipos de comando (campo "type"):
 *   START_ACTIVITY  → iniciar actividad (activityId, profileId)
 *   STOP_ACTIVITY   → detener actividad en curso
 *   EMERGENCY_STOP  → parada de emergencia
 *   SEND_MESSAGE    → mostrar mensaje en pantalla del robot (messageText)
 *   PREDEFINED_ACTION → acción predefinida del robot (actionId: "greet", "go_to_a", etc.)
 */
public class RobotCommand {

    public String type;
    public String activityId;
    public String profileId;
    public String messageText;
    public String actionId;
    public long timestamp;

    public RobotCommand(String type) {
        this.type = type;
        this.timestamp = System.currentTimeMillis();
    }
}
