package com.example.appterapeuta.data.local.dao;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;

import com.example.appterapeuta.data.local.entity.SessionRecordEntity;

import java.util.List;

@Dao
public interface SessionRecordDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insert(SessionRecordEntity record);

    @Query("SELECT * FROM session_records ORDER BY startTimestamp DESC")
    LiveData<List<SessionRecordEntity>> getAll();

    @Query("SELECT * FROM session_records WHERE sessionId = :sessionId LIMIT 1")
    SessionRecordEntity getById(String sessionId);
}
