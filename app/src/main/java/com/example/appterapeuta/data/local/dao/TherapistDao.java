package com.example.appterapeuta.data.local.dao;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.Update;

import com.example.appterapeuta.data.local.entity.TherapistEntity;

import java.util.List;

@Dao
public interface TherapistDao {
    @Query("SELECT * FROM therapists WHERE username = :username LIMIT 1")
    TherapistEntity getByUsername(String username);

    @Query("SELECT * FROM therapists ORDER BY displayName ASC")
    LiveData<List<TherapistEntity>> getAll();

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    void insert(TherapistEntity therapist);

    @Update
    void update(TherapistEntity therapist);

    @Delete
    void delete(TherapistEntity therapist);

    @Query("UPDATE therapists SET passwordHash = :hash WHERE username = :username")
    void updatePassword(String username, String hash);
}
