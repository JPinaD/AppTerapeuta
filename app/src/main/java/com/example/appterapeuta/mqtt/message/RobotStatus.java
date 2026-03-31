package com.example.appterapeuta.mqtt.message;

/**
 * Estado periódico que publica AppRobot hacia AppTerapeuta.
 *
 * Campos:
 *   robotId          → identificador del robot
 *   batteryLevel     → porcentaje de batería (0-100)
 *   activityProgress → porcentaje de actividad completado (0-100)
 *   currentActivity  → id de la actividad en curso (null si ninguna)
 *   isConnectedToHw  → si la AppRobot tiene conexión Bluetooth con el hardware
 *   timestamp        → momento del envío
 */
public class RobotStatus {

    public String robotId;
    public int batteryLevel;
    public int activityProgress;
    public String currentActivity;
    public boolean isConnectedToHw;
    public long timestamp;
}
