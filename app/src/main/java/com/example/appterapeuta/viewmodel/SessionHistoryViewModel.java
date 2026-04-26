package com.example.appterapeuta.viewmodel;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.example.appterapeuta.data.local.entity.ActivityResultEntity;
import com.example.appterapeuta.data.local.entity.AlumnResultEntity;
import com.example.appterapeuta.data.local.entity.IncidentEntity;
import com.example.appterapeuta.data.local.entity.SessionRecordEntity;
import com.example.appterapeuta.data.repository.ActivityResultRepository;
import com.example.appterapeuta.data.repository.IncidentRepository;
import com.example.appterapeuta.data.repository.SessionRecordRepository;

import java.util.List;
import java.util.concurrent.Executors;

public class SessionHistoryViewModel extends AndroidViewModel {

    public static class SessionDetail {
        public SessionRecordEntity session;
        public List<ActivityResultEntity> activities;
        public List<AlumnResultEntity> alumnResults;
        public List<IncidentEntity> incidents;
    }

    private final SessionRecordRepository sessionRepo;
    private final ActivityResultRepository activityRepo;
    private final IncidentRepository incidentRepo;

    public final LiveData<List<SessionRecordEntity>> sessions;
    private final MutableLiveData<SessionDetail> detail = new MutableLiveData<>();

    public SessionHistoryViewModel(@NonNull Application application) {
        super(application);
        sessionRepo  = new SessionRecordRepository(application);
        activityRepo = new ActivityResultRepository(application);
        incidentRepo = new IncidentRepository(application);
        sessions = sessionRepo.getAllSessions();
    }

    public LiveData<SessionDetail> getDetail() { return detail; }

    public void loadDetail(String sessionId) {
        Executors.newSingleThreadExecutor().execute(() -> {
            SessionDetail d = new SessionDetail();
            d.session    = sessionRepo.getSessionDetail(sessionId);
            d.activities = activityRepo.getResultsForSession(sessionId);
            d.incidents  = incidentRepo.getIncidentsForSession(sessionId);
            // Cargar AlumnResults para todas las actividades (se usa la primera actividad si solo hay una)
            if (d.activities != null && !d.activities.isEmpty()) {
                d.alumnResults = activityRepo.getAlumnResultsForActivity(d.activities.get(0).id);
            }
            detail.postValue(d);
        });
    }
}
