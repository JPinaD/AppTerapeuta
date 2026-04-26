package com.example.appterapeuta.data.local.entity;

import androidx.annotation.NonNull;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "robot_configs")
public class RobotConfigEntity {

    @PrimaryKey
    @NonNull
    public String robotId;
    public String name;
    public String lastKnownHost;
    public int lastKnownPort;

    public RobotConfigEntity(@NonNull String robotId, String name,
                              String lastKnownHost, int lastKnownPort) {
        this.robotId = robotId;
        this.name = name;
        this.lastKnownHost = lastKnownHost;
        this.lastKnownPort = lastKnownPort;
    }
}
