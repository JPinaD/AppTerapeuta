package com.example.appterapeuta.viewmodel;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.example.appterapeuta.data.local.entity.StudentProfileEntity;
import com.example.appterapeuta.data.model.ActivityEvent;
import com.example.appterapeuta.data.model.RobotSessionState;
import com.example.appterapeuta.data.model.RobotSessionStatus;
import com.example.appterapeuta.data.model.Session;
import com.example.appterapeuta.data.model.SessionState;
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

    public SessionViewModel(@NonNull Application application) {
        super(application);
        profileRepository = new StudentProfileRepository(application);
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

    /** Envía SESSION_START a cada robot participante. */
    public void startSession(RobotViewModel robotViewModel,
                             List<StudentProfileEntity> allProfiles) {
        Session session = currentSession.getValue();
        if (session == null) return;
        session.state = SessionState.ACTIVE;
        currentSession.setValue(session);

        for (String robotId : session.participatingRobotIds) {
            String profileId = session.robotToStudentProfileId.get(robotId);
            StudentProfileEntity profile = findProfile(allProfiles, profileId);
            String msg = buildSessionStartMessage(session, profile);
            robotViewModel.sendMessage(robotId, msg);
        }
    }

    /** Envía SESSION_END a todos los robots participantes. */
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
    }

    /** Procesa eventos entrantes SESSION_READY / SESSION_ENDED. */
    public void handleIncomingEvent(ActivityEvent event) {
        if (event == null) return;
        RobotSessionState newState = null;
        if (AppConstants.MSG_SESSION_READY.equals(event.type))  newState = RobotSessionState.READY;
        if (AppConstants.MSG_SESSION_ENDED.equals(event.type))  newState = RobotSessionState.ENDED;
        if (AppConstants.MSG_PICTOGRAM_SELECTED.equals(event.type)) newState = RobotSessionState.IN_ACTIVITY;
        if (newState == null) return;

        Map<String, RobotSessionStatus> current = new HashMap<>(safeStatuses());
        RobotSessionStatus status = current.get(event.robotId);
        if (status != null) {
            status.state = newState;
            robotStatuses.postValue(current);
        }
    }

    // --- privado ---

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
