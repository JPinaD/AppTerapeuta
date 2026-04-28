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
import com.example.appterapeuta.data.model.SocialScenario;
import com.example.appterapeuta.data.repository.SessionRecordRepository;
import com.example.appterapeuta.data.repository.StudentProfileRepository;
import com.example.appterapeuta.util.AppConstants;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Collections;
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

    // Contenido de actividad seleccionado
    private List<String> activityItemIds = new ArrayList<>();
    private List<SocialScenario> socialScenarios = new ArrayList<>();
    private int sequenceLength = 2;

    // Estado de turnos
    private List<String> turnOrder = new ArrayList<>();
    private int currentTurnIndex = 0;

    public SessionViewModel(@NonNull Application application) {
        super(application);
        profileRepository      = new StudentProfileRepository(application);
        sessionRecordRepository = new SessionRecordRepository(application);
    }

    public LiveData<Session> getCurrentSession()                          { return currentSession; }
    public LiveData<Map<String, RobotSessionStatus>> getRobotStatuses()  { return robotStatuses; }

    public void prepareSession(List<String> robotIds,
                               Map<String, String> robotToProfileId,
                               String activityId,
                               List<String> itemIds,
                               List<SocialScenario> scenarios,
                               int seqLength) {
        String sessionId = UUID.randomUUID().toString();
        Session session = new Session(sessionId, robotIds, robotToProfileId, activityId);
        currentSession.setValue(session);

        activityItemIds  = itemIds != null ? new ArrayList<>(itemIds) : new ArrayList<>();
        socialScenarios  = scenarios != null ? new ArrayList<>(scenarios) : new ArrayList<>();
        sequenceLength   = seqLength > 0 ? seqLength : 2;

        Map<String, RobotSessionStatus> statuses = new HashMap<>();
        for (String id : robotIds) statuses.put(id, new RobotSessionStatus(id, RobotSessionState.WAITING));
        robotStatuses.setValue(statuses);
    }

    /** Sobrecarga de compatibilidad para llamadas sin contenido (pictogram). */
    public void prepareSession(List<String> robotIds,
                               Map<String, String> robotToProfileId,
                               String activityId) {
        prepareSession(robotIds, robotToProfileId, activityId, null, null, 2);
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

        // Iniciar coordinación de turnos si aplica
        if (AppConstants.ACTIVITY_TURNS.equals(session.activityId)) {
            startTurns(robotViewModel, session);
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

        if (AppConstants.MSG_TURN_DONE.equals(event.type)) {
            // Buscar el RobotViewModel desde el contexto de la sesión activa
            // Se delega a handleTurnDone con el robotId del evento
            return; // Se maneja en SessionLiveActivity con acceso al robotViewModel
        }

        RobotSessionState newState = null;
        if (AppConstants.MSG_SESSION_READY.equals(event.type))      newState = RobotSessionState.READY;
        if (AppConstants.MSG_SESSION_ENDED.equals(event.type))      newState = RobotSessionState.ENDED;
        if (AppConstants.MSG_PICTOGRAM_SELECTED.equals(event.type)) newState = RobotSessionState.IN_ACTIVITY;
        if (AppConstants.MSG_ACTIVITY_RESULT.equals(event.type))    newState = RobotSessionState.IN_ACTIVITY;
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

    /** Gestiona TURN_DONE: avanza al siguiente robot, salta desconectados. */
    public void handleTurnDone(RobotViewModel robotViewModel, String robotId) {
        Session session = currentSession.getValue();
        if (session == null || turnOrder.isEmpty()) return;

        // Avanzar al siguiente robot en el orden
        currentTurnIndex = (currentTurnIndex + 1) % turnOrder.size();

        // Saltar robots desconectados (máximo una vuelta completa)
        int attempts = 0;
        while (attempts < turnOrder.size()) {
            String nextRobotId = turnOrder.get(currentTurnIndex);
            if (isRobotConnected(nextRobotId)) {
                sendTurnSignals(robotViewModel, session.sessionId, nextRobotId);
                return;
            }
            currentTurnIndex = (currentTurnIndex + 1) % turnOrder.size();
            attempts++;
        }
        android.util.Log.w("SessionViewModel", "Todos los robots desconectados en activity_turns");
    }

    /** Activa activity_calm en todos los robots de la sesión activa. */
    public void activateCalm(RobotViewModel robotViewModel) {
        Session session = currentSession.getValue();
        if (session == null) return;
        try {
            JSONObject payload = new JSONObject();
            payload.put("sessionId", session.sessionId);
            payload.put("activityId", AppConstants.ACTIVITY_CALM);
            JSONObject msg = new JSONObject();
            msg.put("type", AppConstants.MSG_SESSION_START);
            msg.put("payload", payload.toString());
            String msgStr = msg.toString();
            for (String robotId : session.participatingRobotIds) {
                robotViewModel.sendMessage(robotId, msgStr);
            }
        } catch (JSONException e) {
            android.util.Log.e("SessionViewModel", "Error construyendo activateCalm", e);
        }
    }

    public String getStudentName(String profileId) {
        StudentProfileEntity p = findProfile(cachedProfiles, profileId);
        return p != null ? p.name : null;
    }

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

    // --- Turnos ---

    private void startTurns(RobotViewModel robotViewModel, Session session) {
        turnOrder = new ArrayList<>(session.participatingRobotIds);
        Collections.shuffle(turnOrder);
        currentTurnIndex = 0;
        if (!turnOrder.isEmpty()) {
            sendTurnSignals(robotViewModel, session.sessionId, turnOrder.get(0));
        }
    }

    private void sendTurnSignals(RobotViewModel robotViewModel, String sessionId, String activeRobotId) {
        for (String robotId : turnOrder) {
            boolean active = robotId.equals(activeRobotId);
            try {
                JSONObject payload = new JSONObject();
                payload.put("sessionId", sessionId);
                payload.put("active", active);
                JSONObject msg = new JSONObject();
                msg.put("type", AppConstants.MSG_TURN_SIGNAL);
                msg.put("payload", payload.toString());
                robotViewModel.sendMessage(robotId, msg.toString());
            } catch (JSONException e) {
                android.util.Log.e("SessionViewModel", "Error construyendo TURN_SIGNAL", e);
            }
        }
    }

    private boolean isRobotConnected(String robotId) {
        RobotSessionStatus status = safeStatuses().get(robotId);
        return status != null && status.state != RobotSessionState.ENDED;
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
            JSONObject payload = new JSONObject();
            payload.put("sessionId", session.sessionId);
            payload.put("activityId", session.activityId);

            // Pictogramas (para activity_pictogram)
            if (AppConstants.ACTIVITY_PICTOGRAM.equals(session.activityId)
                    || AppConstants.ACTIVITY_PICTOGRAM_LEGACY.equals(session.activityId)) {
                JSONArray pics = new JSONArray();
                for (String p : DEFAULT_PICTOGRAMS) pics.put(p);
                payload.put("pictograms", pics);
            }

            // activityContent según tipo
            JSONObject content = buildActivityContent(session.activityId);
            if (content != null) payload.put("activityContent", content);

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

    private JSONObject buildActivityContent(String activityId) throws JSONException {
        switch (activityId) {
            case AppConstants.ACTIVITY_EMOTION:
            case AppConstants.ACTIVITY_TURNS: {
                if (activityItemIds.isEmpty()) return null;
                JSONObject content = new JSONObject();
                JSONArray items = new JSONArray();
                for (String id : activityItemIds) items.put(id);
                content.put("items", items);
                return content;
            }
            case AppConstants.ACTIVITY_SOCIAL: {
                if (socialScenarios.isEmpty()) return null;
                JSONObject content = new JSONObject();
                JSONArray scenarios = new JSONArray();
                for (SocialScenario s : socialScenarios) {
                    JSONObject sObj = new JSONObject();
                    sObj.put("id", s.id);
                    sObj.put("description", s.description);
                    sObj.put("optionA", s.optionA);
                    sObj.put("optionB", s.optionB);
                    sObj.put("outcomeA", s.outcomeA);
                    sObj.put("outcomeB", s.outcomeB);
                    scenarios.put(sObj);
                }
                content.put("scenarios", scenarios);
                return content;
            }
            case AppConstants.ACTIVITY_SEQUENCE: {
                JSONObject content = new JSONObject();
                content.put("sequenceLength", sequenceLength);
                return content;
            }
            default:
                return null;
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
