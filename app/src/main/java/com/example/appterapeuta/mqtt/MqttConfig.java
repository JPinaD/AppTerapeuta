package com.example.appterapeuta.mqtt;

/**
 * Constantes de configuración del broker MQTT.
 * El broker corre en el móvil hotspot (IP asignada por el hotspot Android: normalmente 192.168.43.1).
 * Ajusta BROKER_IP si tu hotspot usa otro rango.
 */
public class MqttConfig {

    public static final String BROKER_IP   = "10.38.229.66";
    public static final int    BROKER_PORT = 1883;
    public static final String CLIENT_ID_PREFIX = "terapeuta-";

    // QoS 1: entrega garantizada al menos una vez (suficiente para comandos de sesión)
    public static final int QOS = 1;

    private MqttConfig() {}
}
