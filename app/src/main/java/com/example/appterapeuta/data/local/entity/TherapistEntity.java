package com.example.appterapeuta.data.local.entity;

import androidx.annotation.NonNull;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "therapists")
public class TherapistEntity {
    @PrimaryKey
    @NonNull
    public String username;
    /** SHA-256 del password. No se guarda en claro (suficiente para TFG). */
    public String passwordHash;
    public String displayName;

    public TherapistEntity(@NonNull String username, String passwordHash, String displayName) {
        this.username = username;
        this.passwordHash = passwordHash;
        this.displayName = displayName;
    }
}
