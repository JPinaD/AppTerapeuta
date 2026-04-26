package com.example.appterapeuta.data.repository;

import android.content.Context;
import android.util.Log;

import com.example.appterapeuta.data.local.db.AppDatabase;
import com.example.appterapeuta.data.local.entity.IncidentEntity;

import java.util.List;
import java.util.concurrent.Executors;

public class IncidentRepository {

    private static final String TAG = "IncidentRepository";
    private final AppDatabase db;

    public IncidentRepository(Context context) {
        db = AppDatabase.getInstance(context);
    }

    public void saveIncident(IncidentEntity incident) {
        Executors.newSingleThreadExecutor().execute(() -> {
            try {
                db.incidentDao().insert(incident);
            } catch (Exception e) {
                Log.e(TAG, "Error guardando incidencia", e);
            }
        });
    }

    public List<IncidentEntity> getIncidentsForSession(String sessionId) {
        return db.incidentDao().getBySession(sessionId);
    }
}
