package com.example.appterapeuta.data.model;

public class Robot {
    public String id;
    public String name;
    public String macAddress;
    public boolean isConnected;
    public int batteryLevel;

    public Robot(String id, String name, String macAddress, boolean isConnected, int batteryLevel) {
        this.id = id;
        this.name = name;
        this.macAddress = macAddress;
        this.isConnected = isConnected;
        this.batteryLevel = batteryLevel;
    }
}