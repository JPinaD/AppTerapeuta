package com.example.appterapeuta.data.local.dao;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.Query;

import com.example.appterapeuta.data.local.entity.IncidentEntity;

import java.util.List;

@Dao
public interface IncidentDao {
    @Insert
    void insert(IncidentEntity incident);

    @Query("SELECT * FROM incidents WHERE sessionId = :sessionId ORDER BY timestamp ASC")
    List<IncidentEntity> getBySession(String sessionId);
}
