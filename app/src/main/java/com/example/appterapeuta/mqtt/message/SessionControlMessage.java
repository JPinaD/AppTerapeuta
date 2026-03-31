package com.example.appterapeuta.mqtt.message;

/**
 * Mensaje de control de sesión global publicado en "session/control".
 * Todos los robots suscritos lo reciben simultáneamente.
 *
 * Tipos (campo "type"):
 *   SESSION_START  → inicio de sesión (sessionId)
 *   SESSION_END    → fin de sesión
 */
public class SessionControlMessage {

    public String type;
    public String sessionId;
    public long timestamp;

    public SessionControlMessage(String type, String sessionId) {
        this.type = type;
        this.sessionId = sessionId;
        this.timestamp = System.currentTimeMillis();
    }
}
