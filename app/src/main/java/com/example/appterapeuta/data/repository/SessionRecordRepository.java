package com.example.appterapeuta.data.repository;

import android.content.Context;

import androidx.lifecycle.LiveData;

import com.example.appterapeuta.data.local.db.AppDatabase;
import com.example.appterapeuta.data.local.entity.SessionRecordEntity;

import java.util.List;
import java.util.concurrent.Executors;

public class SessionRecordRepository {

    private final AppDatabase db;

    public SessionRecordRepository(Context context) {
        db = AppDatabase.getInstance(context);
    }

    public void saveSession(SessionRecordEntity record) {
        Executors.newSingleThreadExecutor().execute(() -> db.sessionRecordDao().insert(record));
    }

    public LiveData<List<SessionRecordEntity>> getAllSessions() {
        return db.sessionRecordDao().getAll();
    }

    public SessionRecordEntity getSessionDetail(String sessionId) {
        return db.sessionRecordDao().getById(sessionId);
    }
}
