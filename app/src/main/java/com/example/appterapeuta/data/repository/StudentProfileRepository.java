package com.example.appterapeuta.data.repository;

import android.content.Context;

import androidx.lifecycle.LiveData;

import com.example.appterapeuta.data.local.db.AppDatabase;
import com.example.appterapeuta.data.local.entity.StudentProfileEntity;

import java.util.List;
import java.util.concurrent.Executors;

public class StudentProfileRepository {

    private final AppDatabase db;

    public StudentProfileRepository(Context context) {
        db = AppDatabase.getInstance(context);
    }

    public LiveData<List<StudentProfileEntity>> getAll() {
        return db.studentProfileDao().getAll();
    }

    public void insert(StudentProfileEntity profile) {
        Executors.newSingleThreadExecutor().execute(() -> db.studentProfileDao().insert(profile));
    }

    public void update(StudentProfileEntity profile) {
        Executors.newSingleThreadExecutor().execute(() -> db.studentProfileDao().update(profile));
    }

    public void deleteById(String id) {
        Executors.newSingleThreadExecutor().execute(() -> db.studentProfileDao().deleteById(id));
    }
}
