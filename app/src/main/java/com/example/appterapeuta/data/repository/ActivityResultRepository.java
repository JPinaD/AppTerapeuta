package com.example.appterapeuta.data.repository;

import android.content.Context;
import android.util.Log;

import com.example.appterapeuta.data.local.db.AppDatabase;
import com.example.appterapeuta.data.local.entity.ActivityResultEntity;
import com.example.appterapeuta.data.local.entity.AlumnResultEntity;

import java.util.List;
import java.util.concurrent.Executors;

public class ActivityResultRepository {

    private static final String TAG = "ActivityResultRepository";
    private final AppDatabase db;

    public ActivityResultRepository(Context context) {
        db = AppDatabase.getInstance(context);
    }

    /** Inserta ActivityResult y sus AlumnResults en una sola operación de fondo. */
    public void saveActivityResult(ActivityResultEntity result, List<AlumnResultEntity> alumnResults) {
        Executors.newSingleThreadExecutor().execute(() -> {
            try {
                long activityResultId = db.activityResultDao().insert(result);
                for (AlumnResultEntity ar : alumnResults) {
                    ar.activityResultId = activityResultId;
                    db.alumnResultDao().insert(ar);
                }
            } catch (Exception e) {
                Log.e(TAG, "Error guardando ActivityResult", e);
            }
        });
    }

    public List<ActivityResultEntity> getResultsForSession(String sessionId) {
        return db.activityResultDao().getBySession(sessionId);
    }

    public List<AlumnResultEntity> getAlumnResultsForActivity(long activityResultId) {
        return db.alumnResultDao().getByActivityResult(activityResultId);
    }
}
