package com.example.appterapeuta.data.local.dao;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;

import com.example.appterapeuta.data.local.entity.StudentProfileEntity;

import java.util.List;

@Dao
public interface StudentProfileDao {

    @Query("SELECT * FROM student_profiles ORDER BY name ASC")
    LiveData<List<StudentProfileEntity>> getAll();

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insert(StudentProfileEntity profile);

    @Query("DELETE FROM student_profiles WHERE id = :id")
    void deleteById(String id);
}
