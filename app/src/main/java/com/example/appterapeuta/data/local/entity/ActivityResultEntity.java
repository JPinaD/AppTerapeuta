package com.example.appterapeuta.data.local.entity;

import androidx.room.Entity;
import androidx.room.ForeignKey;
import androidx.room.Index;
import androidx.room.PrimaryKey;

@Entity(
    tableName = "activity_results",
    foreignKeys = @ForeignKey(
        entity = SessionRecordEntity.class,
        parentColumns = "sessionId",
        childColumns = "sessionId",
        onDelete = ForeignKey.CASCADE
    ),
    indices = @Index("sessionId")
)
public class ActivityResultEntity {
    @PrimaryKey(autoGenerate = true)
    public long id;
    public String sessionId;
    public String activityId;

    public ActivityResultEntity(String sessionId, String activityId) {
        this.sessionId = sessionId;
        this.activityId = activityId;
    }
}
