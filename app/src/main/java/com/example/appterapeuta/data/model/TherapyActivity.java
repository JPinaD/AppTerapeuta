package com.example.appterapeuta.data.model;

public class TherapyActivity {
    public String id;
    public String name;
    public String description;
    public int difficulty;

    public TherapyActivity(String id, String name, String description, int difficulty) {
        this.id = id;
        this.name = name;
        this.description = description;
        this.difficulty = difficulty;
    }
}
