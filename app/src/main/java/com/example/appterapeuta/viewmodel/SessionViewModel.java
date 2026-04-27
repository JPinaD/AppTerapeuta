package com.example.appterapeuta.viewmodel;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.example.appterapeuta.data.local.entity.SessionRecordEntity;
import com.example.appterapeuta.data.local.entity.StudentProfileEntity;
import com.example.appterapeuta.data.model.ActivityEvent;
import com.example.appterapeuta.data.model.RobotSessionState;
import com.example.appterapeuta.data.model.RobotSessionStatus;
import com.example.appterapeuta.data.model.Session;
import com.example.appterapeuta.data.model.SessionState;
import com.example.appterapeuta.data.repository.SessionRecordRepository;
import com.example.appterapeuta.data.repository.StudentProfileRepository;
import com.example.appterapeuta.util.AppConstants;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class SessionViewModel extends AndroidViewModel {

    private static final String[] DEFAULT_PICTOGRAMS = {"pic_agua", "pic_jugar", "pic_comer", "pic_ayuda"};

    private final MutableLiveData<Session> currentSession = new MutableLiveData<>();
    private final MutableLiveData<Map<String, RobotSessionStatus>> robotStatuses =
            new MutableLiveData<>(new HashMap<>());

    private final StudentProfileRepository profileRepository;
    private final SessionRecordRepository sessionRecordRepository;
    private List<StudentProfileEntity> cachedProfiles = new ArrayList<>();
    private long sessionStartTimestamp = 0;

    public SessionViewModel(@NonNull Application application) {
        super(application);
        profileRepository      = new StudentProfileRepository(application);
        sessionRecordRepository = new SessionRecordRepository(application);
    }

    public LiveData<Session> getCurrentSession()                          { return currentSession; }
    public LiveData<Map<String, RobotSessionStatus>> getRobotStatuses()  { return robotStatuses; }

    public void prepareSession(List<String> robotIds,
                               Map<String, String> robotToProfileId,
                               String activityId) {
        String sessionId = UUID.randomUUID().toString();
        Session session = new Session(sessionId, robotIds, robotToProfileId, activityId);
        currentSession.setValue(session);

        Map<String, RobotSessionStatus> statuses = new HashMap<>();
        for (String id : robotIds) statuses.put(id, new RobotSessionStatus(id, RobotSessionState.WAITING));
        robotStatuses.setValue(statuses);
    }

    public void startSession(RobotViewModel robotViewModel,
                             List<StudentProfileEntity> allProfiles) {
        Session session = currentSession.getValue();
        if (session == null) return;

        cachedProfiles = allProfiles != null ? allProfiles : new ArrayList<>();
        sessionStartTimestamp = System.currentTimeMillis();

        session.state = SessionState.ACTIVE;
        currentSession.setValue(session);

        for (String robotId : session.participatingRobotIds) {
            String profileId = session.robotToStudentProfileId.get(robotId);
            StudentProfileEntity profile = findProfile(cachedProfiles, profileId);
            String msg = buildSessionStartMessage(session, profile);
            robotViewModel.sendMessage(robotId, msg);
        }
    }

    public void endSession(RobotViewModel robotViewModel) {
        Session session = currentSession.getValue();
        if (session == null) return;
        session.state = SessionState.ENDED;
        currentSession.setValue(session);

        try {
            JSONObject payload = new JSONObject();
            payload.put("sessionId", session.sessionId);
            JSONObject msg = new JSONObject();
            msg.put("type", AppConstants.MSG_SESSION_END);
            msg.put("payload", payload.toString());
            String msgStr = msg.toString();
            for (String robotId : session.participatingRobotIds) {
                robotViewModel.sendMessage(robotId, msgStr);
            }
        } catch (JSONException e) {
            android.util.Log.e("SessionViewModel", "Error construyendo SESSION_END", e);
        }

        persistSessionRecord(session);
    }

    public void pauseSession(RobotViewModel robotViewModel, List<String> robotIds) {
        Session session = currentSession.getValue();
        if (session == null || robotIds == null || robotIds.isEmpty()) return;
        try {
            JSONArray ids = new JSONArray();
            for (String id : robotIds) ids.put(id);
            JSONObject payload = new JSONObject();
            payload.put("sessionId", session.sessionId);
            payload.put("robotIds", ids);
            JSONObject msg = new JSONObject();
            msg.put("type", AppConstants.MSG_SESSION_PAUSE);
            msg.put("payload", payload.toString());
            String msgStr = msg.toString();
            for (String robotId : robotIds) robotViewModel.sendMessage(robotId, msgStr);
        } catch (JSONException e) {
            android.util.Log.e("SessionViewModel", "Error construyendo SESSION_PAUSE", e);
        }
        Map<String, RobotSessionStatus> current = new HashMap<>(safeStatuses());
        for (String robotId : robotIds) {
            RobotSessionStatus s = current.get(robotId);
            if (s != null) s.state = RobotSessionState.PAUSED;
        }
        robotStatuses.postValue(current);
    }

    public void resumeSession(RobotViewModel robotViewModel, List<String> robotIds) {
        Session session = currentSession.getValue();
        if (session == null || robotIds == null || robotIds.isEmpty()) return;
        try {
            JSONArray ids = new JSONArray();
            for (String id : robotIds) ids.put(id);
            JSONObject payload = new JSONObject();
            payload.put("sessionId", session.sessionId);
            payload.put("robotIds", ids);
            JSONObject msg = new JSONObject();
            msg.put("type", AppConstants.MSG_SESSION_RESUME);
            msg.put("payload", payload.toString());
            String msgStr = msg.toString();
            for (String robotId : robotIds) robotViewModel.sendMessage(robotId, msgStr);
        } catch (JSONException e) {
            android.util.Log.e("SessionViewModel", "Error construyendo SESSION_RESUME", e);
        }
    }

    public List<String> getPausedRobotIds() {
        List<String> paused = new ArrayList<>();
        for (RobotSessionStatus s : safeStatuses().values()) {
            if (s.state == RobotSessionState.PAUSED) paused.add(s.robotId);
        }
        return paused;
    }

    public void handleIncomingEvent(ActivityEvent event) {
        if (event == null) return;
        RobotSessionState newState = null;
        if (AppConstants.MSG_SESSION_READY.equals(event.type))      newState = RobotSessionState.READY;
        if (AppConstants.MSG_SESSION_ENDED.equals(event.type))      newState = RobotSessionState.ENDED;
        if (AppConstants.MSG_PICTOGRAM_SELECTED.equals(event.type)) newState = RobotSessionState.IN_ACTIVITY;
        if (AppConstants.MSG_SESSION_PAUSED.equals(event.type))     newState = RobotSessionState.PAUSED;
        if (AppConstants.MSG_SESSION_RESUMED.equals(event.type))    newState = RobotSessionState.IN_ACTIVITY;
        if (newState == null) return;

        Map<String, RobotSessionStatus> current = new HashMap<>(safeStatuses());
        RobotSessionStatus status = current.get(event.robotId);
        if (status != null) {
            status.state = newState;
            robotStatuses.postValue(current);
        }
    }

    public String getStudentName(String profileId) {
        StudentProfileEntity p = findProfile(cachedProfiles, profileId);
        return p != null ? p.name : null;
    }

    /** Envia un mensaje de feedback a un robot concreto durante la sesion. */
    public void sendFeedback(RobotViewModel robotViewModel, String robotId, String message) {
        try {
            JSONObject payload = new JSONObject();
            payload.put("text", message);
            JSONObject msg = new JSONObject();
            msg.put("type", AppConstants.MSG_ROBOT_FEEDBACK);
            msg.put("payload", payload.toString());
            robotViewModel.sendMessage(robotId, msg.toString());
        } catch (JSONException e) {
            android.util.Log.e("SessionViewModel", "Error construyendo ROBOT_FEEDBACK", e);
        }
    }

    // --- privado ---

    private void persistSessionRecord(Session session) {
        try {
            JSONArray robotIds = new JSONArray();
            for (String id : session.participatingRobotIds) robotIds.put(id);

            JSONObject robotToStudent = new JSONObject();
            for (Map.Entry<String, String> e : session.robotToStudentProfileId.entrySet()) {
                robotToStudent.put(e.getKey(), e.getValue());
            }

            SessionRecordEntity record = new SessionRecordEntity(
                    session.sessionId,
                    sessionStartTimestamp,
                    System.currentTimeMillis(),
                    robotIds.toString(),
                    robotToStudent.toString()
            );
            sessionRecordRepository.saveSession(record);
        } catch (JSONException e) {
            android.util.Log.e("SessionViewModel", "Error persistiendo SessionRecord", e);
        }
    }

    private String buildSessionStartMessage(Session session, StudentProfileEntity profile) {
        try {
            JSONArray pics = new JSONArray();
            for (String p : DEFAULT_PICTOGRAMS) pics.put(p);

            JSONObject payload = new JSONObject();
            payload.put("sessionId", session.sessionId);
            payload.put("activityId", session.activityId);
            payload.put("pictograms", pics);

            if (profile != null) {
                JSONObject profileJson = new JSONObject();
                profileJson.put("id", profile.id);
                profileJson.put("name", profile.name);
                profileJson.put("excludedColors",
                        profile.excludedColors != null ? new JSONArray(profile.excludedColors) : new JSONArray());
                if (profile.backgroundSoundResName != null) {
                    profileJson.put("backgroundSoundResName", profile.backgroundSoundResName);
                }
                payload.put("studentProfile", profileJson);
            }

            JSONObject msg = new JSONObject();
            msg.put("type", AppConstants.MSG_SESSION_START);
            msg.put("payload", payload.toString());
            return msg.toString();
        } catch (JSONException e) {
            android.util.Log.e("SessionViewModel", "Error construyendo SESSION_START", e);
            return "";
        }
    }

    private StudentProfileEntity findProfile(List<StudentProfileEntity> profiles, String profileId) {
        if (profiles == null || profileId == null) return null;
        for (StudentProfileEntity p : profiles) {
            if (p.id.equals(profileId)) return p;
        }
        return null;
    }

    private Map<String, RobotSessionStatus> safeStatuses() {
        Map<String, RobotSessionStatus> m = robotStatuses.getValue();
        return m != null ? m : new HashMap<>();
    }
}
