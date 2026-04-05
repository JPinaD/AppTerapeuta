package com.example.appterapeuta.data.repository;

import android.content.Context;

import androidx.lifecycle.LiveData;

import com.example.appterapeuta.data.local.db.AppDatabase;
import com.example.appterapeuta.data.local.entity.TherapyActivityEntity;

import java.util.List;
import java.util.concurrent.Executors;

public class TherapyActivityRepository {

    private final AppDatabase db;

    public TherapyActivityRepository(Context context) {
        db = AppDatabase.getInstance(context);
    }

    public LiveData<List<TherapyActivityEntity>> getAll() {
        return db.therapyActivityDao().getAll();
    }

    public void insert(TherapyActivityEntity activity) {
        Executors.newSingleThreadExecutor().execute(() -> db.therapyActivityDao().insert(activity));
    }
}
