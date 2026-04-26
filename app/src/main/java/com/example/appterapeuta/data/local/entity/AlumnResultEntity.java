package com.example.appterapeuta.data.local.entity;

import androidx.room.Entity;
import androidx.room.ForeignKey;
import androidx.room.Index;
import androidx.room.PrimaryKey;

@Entity(
    tableName = "alumn_results",
    foreignKeys = @ForeignKey(
        entity = ActivityResultEntity.class,
        parentColumns = "id",
        childColumns = "activityResultId",
        onDelete = ForeignKey.CASCADE
    ),
    indices = @Index("activityResultId")
)
public class AlumnResultEntity {
    @PrimaryKey(autoGenerate = true)
    public long id;
    public long activityResultId;
    public String studentId;
    public String studentName;
    public int attempts;
    public int successes;
    public long avgResponseTimeMs;
    public String finalPictogramId;

    public AlumnResultEntity(long activityResultId, String studentId, String studentName,
                             int attempts, int successes, long avgResponseTimeMs,
                             String finalPictogramId) {
        this.activityResultId = activityResultId;
        this.studentId = studentId;
        this.studentName = studentName;
        this.attempts = attempts;
        this.successes = successes;
        this.avgResponseTimeMs = avgResponseTimeMs;
        this.finalPictogramId = finalPictogramId;
    }
}
