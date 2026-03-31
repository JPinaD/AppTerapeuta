package com.example.appterapeuta.mqtt.message;

/**
 * Evento puntual que publica AppRobot (sensor, interacción del alumno, incidencia).
 *
 * Tipos de evento (campo "type"):
 *   OBSTACLE_DETECTED   → sensor de ultrasonidos detectó obstáculo
 *   LINE_LOST           → sensor de línea perdió la línea
 *   ACTIVITY_COMPLETED  → alumno completó la actividad
 *   ANSWER_GIVEN        → alumno respondió (correct: true/false)
 *   INCIDENT            → incidencia genérica (description)
 */
public class RobotEvent {

    public String robotId;
    public String type;
    public boolean correct;       // para ANSWER_GIVEN
    public String description;    // para INCIDENT
    public long timestamp;
}
