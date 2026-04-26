package com.example.appterapeuta.data.repository;

import android.content.Context;

import androidx.lifecycle.LiveData;

import com.example.appterapeuta.data.local.db.AppDatabase;
import com.example.appterapeuta.data.local.entity.RobotConfigEntity;

import java.util.List;
import java.util.concurrent.Executors;

public class RobotConfigRepository {

    private final AppDatabase db;

    public RobotConfigRepository(Context context) {
        db = AppDatabase.getInstance(context);
    }

    public LiveData<List<RobotConfigEntity>> getAll() {
        return db.robotConfigDao().getAll();
    }

    public void insert(RobotConfigEntity robot) {
        Executors.newSingleThreadExecutor().execute(() -> db.robotConfigDao().insert(robot));
    }

    public void delete(RobotConfigEntity robot) {
        Executors.newSingleThreadExecutor().execute(() -> db.robotConfigDao().delete(robot));
    }
}
