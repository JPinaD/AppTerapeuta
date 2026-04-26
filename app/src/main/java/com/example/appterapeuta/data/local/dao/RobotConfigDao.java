package com.example.appterapeuta.data.local.dao;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;

import com.example.appterapeuta.data.local.entity.RobotConfigEntity;

import java.util.List;

@Dao
public interface RobotConfigDao {

    @Query("SELECT * FROM robot_configs ORDER BY name ASC")
    LiveData<List<RobotConfigEntity>> getAll();

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insert(RobotConfigEntity robot);

    @Delete
    void delete(RobotConfigEntity robot);
}
