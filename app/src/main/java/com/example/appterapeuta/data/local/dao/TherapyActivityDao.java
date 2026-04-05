package com.example.appterapeuta.data.local.dao;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;

import com.example.appterapeuta.data.local.entity.TherapyActivityEntity;

import java.util.List;

@Dao
public interface TherapyActivityDao {

    @Query("SELECT * FROM therapy_activities ORDER BY difficulty ASC, name ASC")
    LiveData<List<TherapyActivityEntity>> getAll();

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insert(TherapyActivityEntity activity);
}
