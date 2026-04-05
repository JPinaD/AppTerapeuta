package com.example.appterapeuta.data.local.entity;

import androidx.room.Entity;
import androidx.room.PrimaryKey;
import androidx.annotation.NonNull;

@Entity(tableName = "student_profiles")
public class StudentProfileEntity {

    @PrimaryKey
    @NonNull
    public String id;
    public String name;
    // JSON array de strings hexadecimales, ej: ["#C8E6C9","#4CAF50"]
    public String excludedColors;
    // Nombre del recurso raw en AppRobot, ej: "sound_birds". Null si no aplica.
    public String backgroundSoundResName;

    public StudentProfileEntity(@NonNull String id, String name,
                                String excludedColors, String backgroundSoundResName) {
        this.id = id;
        this.name = name;
        this.excludedColors = excludedColors;
        this.backgroundSoundResName = backgroundSoundResName;
    }
}
