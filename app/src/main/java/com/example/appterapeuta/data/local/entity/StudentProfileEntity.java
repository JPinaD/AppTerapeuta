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
    public String avatar;
    public String educationalNeeds;

    public StudentProfileEntity(@NonNull String id, String name, String avatar, String educationalNeeds) {
        this.id = id;
        this.name = name;
        this.avatar = avatar;
        this.educationalNeeds = educationalNeeds;
    }
}
