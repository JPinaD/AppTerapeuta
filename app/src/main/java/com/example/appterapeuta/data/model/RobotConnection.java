package com.example.appterapeuta.data.model;

public class RobotConnection {
    public final String robotId;
    public final String host;
    public final int port;
    public ConnectionState state;

    public RobotConnection(String robotId, String host, int port) {
        this.robotId = robotId;
        this.host = host;
        this.port = port;
        this.state = ConnectionState.DISCONNECTED;
    }
}
