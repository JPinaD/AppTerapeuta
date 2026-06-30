package com.example.appterapeuta.data.model;

import java.util.List;

public class StudentProfile {
    public String id;
    public String name;
    public List<String> excludedColors;
    public String backgroundSoundResName;
    public String calmType;

    public StudentProfile(String id, String name,
                          List<String> excludedColors, String backgroundSoundResName) {
        this.id = id;
        this.name = name;
        this.excludedColors = excludedColors;
        this.backgroundSoundResName = backgroundSoundResName;
    }

    public StudentProfile(String id, String name,
                          List<String> excludedColors, String backgroundSoundResName,
                          String calmType) {
        this(id, name, excludedColors, backgroundSoundResName);
        this.calmType = calmType;
    }
}
