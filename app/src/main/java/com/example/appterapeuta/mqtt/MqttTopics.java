package com.example.appterapeuta.mqtt;

/**
 * Centraliza todos los topics MQTT del sistema.
 *
 * Estructura de topics:
 *   robots/{robotId}/command   → AppTerapeuta publica, AppRobot suscribe
 *   robots/{robotId}/status    → AppRobot publica, AppTerapeuta suscribe
 *   robots/{robotId}/event     → AppRobot publica (eventos de sensor/actividad)
 *   session/control            → AppTerapeuta publica (inicio/parada de sesión global)
 */
public class MqttTopics {

    public static final String BASE = "robots/";
    public static final String SESSION_CONTROL = "session/control";

    public static String command(String robotId) {
        return BASE + robotId + "/command";
    }

    public static String status(String robotId) {
        return BASE + robotId + "/status";
    }

    public static String event(String robotId) {
        return BASE + robotId + "/event";
    }

    // Suscripción wildcard para escuchar todos los robots a la vez
    public static final String ALL_STATUS  = BASE + "+/status";
    public static final String ALL_EVENTS  = BASE + "+/event";

    private MqttTopics() {}
}
