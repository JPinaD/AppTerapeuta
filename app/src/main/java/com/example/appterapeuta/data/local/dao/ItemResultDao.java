package com.example.appterapeuta.data.local.dao;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.Query;

import com.example.appterapeuta.data.local.entity.ItemResultEntity;

import java.util.List;

@Dao
public interface ItemResultDao {

    @Insert
    void insert(ItemResultEntity result);

    /** Get all item results for a session. */
    @Query("SELECT * FROM item_results WHERE sessionId = :sessionId ORDER BY timestamp ASC")
    List<ItemResultEntity> getBySession(String sessionId);

    /** Get item results for a session filtered by activityId. */
    @Query("SELECT * FROM item_results WHERE sessionId = :sessionId AND activityId = :activityId ORDER BY timestamp ASC")
    List<ItemResultEntity> getBySessionAndActivity(String sessionId, String activityId);

    /** Get item results for a specific student in a session. */
    @Query("SELECT * FROM item_results WHERE sessionId = :sessionId AND studentId = :studentId ORDER BY timestamp ASC")
    List<ItemResultEntity> getBySessionAndStudent(String sessionId, String studentId);
}
