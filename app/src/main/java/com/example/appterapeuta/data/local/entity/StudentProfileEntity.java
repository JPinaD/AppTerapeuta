package com.example.appterapeuta.data.local.entity;

import androidx.annotation.NonNull;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "student_profiles")
public class StudentProfileEntity {

    @PrimaryKey
    @NonNull
    public String id;
    public String name;
    /** JSON array de strings hexadecimales, ej: ["#C8E6C9","#4CAF50"] */
    public String excludedColors;
    /** Nombre del recurso raw en AppRobot, ej: "sound_birds". Null si no aplica. */
    public String backgroundSoundResName;

    // --- Características clínicas TEA ---
    public String communicationLevel;
    public String communicationLevelOther;
    public String sensorySensitivity;
    public String sensorySensitivityOther;
    public String attentionLevel;
    public String attentionLevelOther;
    public String motorSkills;
    public String motorSkillsOther;
    public String socioemotionalProfile;
    public String socioemotionalProfileOther;
    public String clinicalNotes;
    /** Referencia multimedia para el Lugar Seguro. Sin uso funcional en esta versión. */
    public String safePlaceUri;

    public StudentProfileEntity(@NonNull String id, String name,
                                String excludedColors, String backgroundSoundResName) {
        this.id = id;
        this.name = name;
        this.excludedColors = excludedColors;
        this.backgroundSoundResName = backgroundSoundResName;
    }
}
