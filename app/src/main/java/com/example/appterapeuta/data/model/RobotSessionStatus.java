package com.example.appterapeuta.data.model;

public class RobotSessionStatus {
    public final String robotId;
    public RobotSessionState state;

    public RobotSessionStatus(String robotId, RobotSessionState state) {
        this.robotId = robotId;
        this.state = state;
    }
}
