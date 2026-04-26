package com.example.appterapeuta.data.local.dao;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.Query;

import com.example.appterapeuta.data.local.entity.AlumnResultEntity;

import java.util.List;

@Dao
public interface AlumnResultDao {
    @Insert
    void insert(AlumnResultEntity result);

    @Query("SELECT * FROM alumn_results WHERE activityResultId = :activityResultId")
    List<AlumnResultEntity> getByActivityResult(long activityResultId);
}
