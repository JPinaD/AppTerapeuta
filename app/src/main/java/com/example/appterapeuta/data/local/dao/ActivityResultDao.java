package com.example.appterapeuta.data.local.dao;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.Query;

import com.example.appterapeuta.data.local.entity.ActivityResultEntity;

import java.util.List;

@Dao
public interface ActivityResultDao {
    @Insert
    long insert(ActivityResultEntity result);

    @Query("SELECT * FROM activity_results WHERE sessionId = :sessionId")
    List<ActivityResultEntity> getBySession(String sessionId);
}
