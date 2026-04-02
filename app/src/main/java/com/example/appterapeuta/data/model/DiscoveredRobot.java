package com.example.appterapeuta.data.model;

public class DiscoveredRobot {
    public final String serviceName;
    public final String host;
    public final int port;

    public DiscoveredRobot(String serviceName, String host, int port) {
        this.serviceName = serviceName;
        this.host = host;
        this.port = port;
    }
}
