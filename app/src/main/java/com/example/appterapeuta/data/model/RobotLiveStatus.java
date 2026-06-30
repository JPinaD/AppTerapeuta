package com.example.appterapeuta.data.model;

public class RobotLiveStatus {
    public final String robotId;
    public boolean online;
    public Integer batteryPct;
    public String activityId;
    public Integer progressPct;
    public String assignedStudentName;
    /** true cuando el robot ha reportado una inclinación > 45° (vuelco detectado). */
    public boolean tilted;

    public RobotLiveStatus(String robotId) {
        this.robotId = robotId;
        this.online = false;
        this.tilted = false;
    }
}
