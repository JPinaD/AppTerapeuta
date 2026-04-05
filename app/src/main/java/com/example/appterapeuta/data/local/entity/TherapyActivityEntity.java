package com.example.appterapeuta.data.local.entity;

import androidx.room.Entity;
import androidx.room.PrimaryKey;
import androidx.annotation.NonNull;

@Entity(tableName = "therapy_activities")
public class TherapyActivityEntity {

    @PrimaryKey
    @NonNull
    public String id;
    public String name;
    public String description;
    public int difficulty;

    public TherapyActivityEntity(@NonNull String id, String name, String description, int difficulty) {
        this.id = id;
        this.name = name;
        this.description = description;
        this.difficulty = difficulty;
    }
}
