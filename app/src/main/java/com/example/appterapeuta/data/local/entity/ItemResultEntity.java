package com.example.appterapeuta.data.local.entity;

import androidx.room.Entity;
import androidx.room.ForeignKey;
import androidx.room.Index;
import androidx.room.PrimaryKey;

/**
 * Stores individual item-level results within a session.
 * Each row represents a single attempt by a student (e.g., one emotion recognition round).
 * Used for per-item analysis (emotion grouping, scenario breakdown, etc.).
 */
@Entity(
    tableName = "item_results",
    foreignKeys = @ForeignKey(
        entity = SessionRecordEntity.class,
        parentColumns = "sessionId",
        childColumns = "sessionId",
        onDelete = ForeignKey.CASCADE
    ),
    indices = {
        @Index("sessionId"),
        @Index("studentId")
    }
)
public class ItemResultEntity {
    @PrimaryKey(autoGenerate = true)
    public long id;
    public String sessionId;
    public String activityId;
    public String studentId;
    /** The item being tested (e.g., "emotion_happy", "s1") */
    public String itemId;
    /** Whether the student answered correctly */
    public boolean correct;
    /** Timestamp of this result */
    public long timestamp;

    public ItemResultEntity(String sessionId, String activityId, String studentId,
                            String itemId, boolean correct, long timestamp) {
        this.sessionId = sessionId;
        this.activityId = activityId;
        this.studentId = studentId;
        this.itemId = itemId;
        this.correct = correct;
        this.timestamp = timestamp;
    }
}
