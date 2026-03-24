package com.example.appterapeuta.data.model;

public class StudentProfile {
    public String id;
    public String name;
    public String avatar;
    public String educationalNeeds;

    public StudentProfile(String id, String name, String avatar, String educationalNeeds) {
        this.id = id;
        this.name = name;
        this.avatar = avatar;
        this.educationalNeeds = educationalNeeds;
    }
}
