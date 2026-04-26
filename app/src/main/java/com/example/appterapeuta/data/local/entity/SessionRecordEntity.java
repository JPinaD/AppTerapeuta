package com.example.appterapeuta.data.local.entity;

import androidx.annotation.NonNull;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "session_records")
public class SessionRecordEntity {
    @PrimaryKey
    @NonNull
    public String sessionId;
    public long startTimestamp;
    public long endTimestamp;
    /** JSON array de robotIds participantes */
    public String robotIdsJson;
    /** JSON object robotId → studentId */
    public String robotToStudentJson;

    public SessionRecordEntity(@NonNull String sessionId, long startTimestamp, long endTimestamp,
                               String robotIdsJson, String robotToStudentJson) {
        this.sessionId = sessionId;
        this.startTimestamp = startTimestamp;
        this.endTimestamp = endTimestamp;
        this.robotIdsJson = robotIdsJson;
        this.robotToStudentJson = robotToStudentJson;
    }
}
