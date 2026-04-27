package com.example.appterapeuta;

import org.json.JSONException;
import org.json.JSONObject;
import org.junit.Test;

import static org.junit.Assert.*;

/**
 * Tests unitarios para el parsing de mensajes TCP del protocolo.
 * Verifica que los mensajes se parsean correctamente y que los casos
 * límite no lanzan excepciones.
 */
public class MessageParsingTest {

    // --- Helpers de parsing (replica la lógica de WaitingSessionActivity/RobotViewModel) ---

    private static class ParsedMessage {
        final String type;
        final String payload;
        ParsedMessage(String type, String payload) {
            this.type = type;
            this.payload = payload;
        }
    }

    /** Parsea un mensaje JSON. Devuelve null si es inválido. */
    private ParsedMessage parse(String raw) {
        if (raw == null) return null;
        try {
            JSONObject obj = new JSONObject(raw);
            if (!obj.has("type")) return null;
            String type = obj.getString("type");
            String payload = obj.optString("payload", null);
            return new ParsedMessage(type, payload);
        } catch (JSONException e) {
            return null;
        }
    }

    @Test
    public void parseo_mensajeValido_conPayload() throws JSONException {
        JSONObject msg = new JSONObject();
        msg.put("type", "SESSION_START");
        JSONObject payload = new JSONObject();
        payload.put("sessionId", "abc-123");
        msg.put("payload", payload.toString());

        ParsedMessage result = parse(msg.toString());

        assertNotNull(result);
        assertEquals("SESSION_START", result.type);
        assertNotNull(result.payload);
        assertTrue(result.payload.contains("abc-123"));
    }

    @Test
    public void parseo_mensajeValido_sinPayload() throws JSONException {
        JSONObject msg = new JSONObject();
        msg.put("type", "PING");

        ParsedMessage result = parse(msg.toString());

        assertNotNull(result);
        assertEquals("PING", result.type);
        assertNull(result.payload);
    }

    @Test
    public void parseo_jsonMalformado_devuelveNull() {
        ParsedMessage result = parse("{esto no es json válido");

        assertNull(result);
    }

    @Test
    public void parseo_sinCampoType_devuelveNull() throws JSONException {
        JSONObject msg = new JSONObject();
        msg.put("payload", "algo");

        ParsedMessage result = parse(msg.toString());

        assertNull(result);
    }
}
