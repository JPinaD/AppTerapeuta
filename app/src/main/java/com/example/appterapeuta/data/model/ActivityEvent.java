package com.example.appterapeuta.data.model;

/** Evento de actividad recibido desde un robot (ej. PICTOGRAM_SELECTED). */
public class ActivityEvent {
    public final String robotId;
    public final String type;
    public final String payload;

    public ActivityEvent(String robotId, String type, String payload) {
        this.robotId = robotId;
        this.type = type;
        this.payload = payload;
    }
}
