package com.example.appterapeuta.data.model;

public class DiscoveredRobot {
    public final String serviceName;
    public final String host;
    public final int port;
    /** Puede ser null si el anuncio NSD no incluye el atributo robotId (fallback a serviceName). */
    public final String robotId;

    public DiscoveredRobot(String serviceName, String host, int port, String robotId) {
        this.serviceName = serviceName;
        this.host = host;
        this.port = port;
        this.robotId = robotId != null ? robotId : serviceName;
    }
}
