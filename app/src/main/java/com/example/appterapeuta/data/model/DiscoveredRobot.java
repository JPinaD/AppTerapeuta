package com.example.appterapeuta.data.model;

public class DiscoveredRobot {
    public final String serviceName;
    public final String host;
    public final int port;
    public final String robotId;

    public DiscoveredRobot(String serviceName, String host, int port, String robotId) {
        this.serviceName = serviceName;
        this.host = host;
        this.port = port;
        this.robotId = robotId != null ? robotId : serviceName;
    }
}
