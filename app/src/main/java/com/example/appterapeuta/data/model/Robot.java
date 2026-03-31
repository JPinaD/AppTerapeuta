package com.example.appterapeuta.data.model;

public class Robot {
    public String id;
    public String name;
    public boolean isConnected;
    public int batteryLevel;

    public Robot(String id, String name, boolean isConnected, int batteryLevel) {
        this.id = id;
        this.name = name;
        this.isConnected = isConnected;
        this.batteryLevel = batteryLevel;
    }
}
