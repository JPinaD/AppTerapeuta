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
        if (AppConstants.MSG_COMMUNICATOR_SEQUENCE.equals(event.type)) newState = RobotSessionState.IN_ACTIVITY;
        if (AppConstants.MSG_STUDENT_PICTOGRAM_RESPONSE.equals(event.type)) newState = RobotSessionState.IN_ACTIVITY;
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
                if (profile.jointAttentionLevel != null) {
                    profileJson.put("jointAttentionLevel", profile.jointAttentionLevel);
                }
                payload.put("studentProfile", profileJson);
            }

            // Include activityContent based on activity type
            JSONObject activityContent = buildActivityContent(session.activityId);
            if (activityContent != null) {
                payload.put("activityContent", activityContent);
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


    /**
     * Builds the activityContent JSON for each activity type.
     * Includes scenarios for Social, steps/items for Emotion and Sequence.
     */
    private JSONObject buildActivityContent(String activityId) throws JSONException {
        if (activityId == null) return null;
        switch (activityId) {
            case "activity_social":
                return buildSocialContent();
            case "activity_emotion":
                return buildEmotionContent();
            case "activity_sequence":
                return buildSequenceContent();
            case "activity_communicator":
                // Communicator doesn't need activityContent — catalog is hardcoded in app
                return null;
            default:
                return null;
        }
    }


    private JSONObject buildEmotionContent() throws JSONException {
        JSONObject content = new JSONObject();
        content.put("steps", 5);
        JSONArray items = new JSONArray();
        items.put("emotion_happy");
        items.put("emotion_sad");
        items.put("emotion_angry");
        items.put("emotion_surprised");
        items.put("emotion_scared");
        items.put("emotion_disgusted");
        items.put("emotion_calm");
        items.put("emotion_shy");
        items.put("emotion_bored");
        items.put("emotion_tired");
        items.put("emotion_excited");
        items.put("emotion_terror");
        content.put("items", items);
        return content;
    }


    private JSONObject buildSequenceContent() throws JSONException {
        JSONObject content = new JSONObject();
        content.put("steps", 3);
        content.put("sequenceLength", 3);
        return content;
    }


    private JSONObject buildSocialContent() throws JSONException {
        JSONObject content = new JSONObject();
        content.put("steps", 5);
        JSONArray scenarios = new JSONArray();

        scenarios.put(buildScenario("s1",
                "Te saludan en el pasillo",
                "Le ignoro", "Le saludo",
                "Se siente ignorado", "¡Bien! Saludar es bonito", "B"));
        scenarios.put(buildScenario("s2",
                "Quieres un juguete que tiene otro niño",
                "Se lo quito", "Le pido que me lo deje",
                "Se enfada y llora", "¡Genial! Pedir está bien", "B"));
        scenarios.put(buildScenario("s3",
                "Tu amigo se cae y llora",
                "Le pregunto si está bien", "Me voy a jugar",
                "¡Bien! Ayudar es importante", "Se queda solo y triste", "A"));
        scenarios.put(buildScenario("s4",
                "Alguien se cuela en la fila",
                "Le empujo", "Le digo que la fila es atrás",
                "Se enfada contigo", "¡Bien! Hablar es mejor", "B"));
        scenarios.put(buildScenario("s5",
                "La profesora pregunta algo en clase",
                "Grito la respuesta", "Levanto la mano y espero",
                "Los demás se asustan", "¡Muy bien! Esperar el turno", "B"));
        scenarios.put(buildScenario("s6",
                "Un compañero te enseña su dibujo",
                "Le digo que es feo", "Le digo que me gusta",
                "Se pone triste", "¡Bien! Decir cosas bonitas", "B"));
        scenarios.put(buildScenario("s7",
                "Pierdes en un juego",
                "Tiro las piezas enfadado", "Digo: no pasa nada",
                "No quieren jugar contigo", "¡Genial! Saber perder", "B"));
        scenarios.put(buildScenario("s8",
                "Te ofrecen una galleta",
                "La cojo sin decir nada", "Digo: gracias",
                "Se siente ignorada", "¡Bien! Dar las gracias", "B"));
        scenarios.put(buildScenario("s9",
                "Un compañero juega solo y aburrido",
                "Le invito a jugar", "Paso de largo",
                "¡Bien! Incluir a los demás", "Sigue solo y triste", "A"));
        scenarios.put(buildScenario("s10",
                "Es hora de recoger",
                "Sigo jugando", "Recojo mis cosas",
                "Te lo tienen que repetir", "¡Muy bien! Seguir las normas", "B"));
        scenarios.put(buildScenario("s11",
                "Un niño nuevo llega a clase",
                "Me presento", "No le digo nada",
                "¡Genial! Presentarse es amable", "Se siente solo", "A"));
        scenarios.put(buildScenario("s12",
                "Alguien te interrumpe mientras hablas",
                "Espero a que termine", "Le grito que se calle",
                "¡Bien! Tener paciencia", "Se asusta", "A"));
        scenarios.put(buildScenario("s13",
                "Necesitas ir al baño en clase",
                "Me voy sin decir nada", "Levanto la mano",
                "No saben dónde estás", "¡Bien! Pedir permiso", "B"));
        scenarios.put(buildScenario("s14",
                "Te piden un lápiz prestado",
                "Digo que no", "Se lo presto",
                "Se siente rechazado", "¡Genial! Compartir es bonito", "B"));
        scenarios.put(buildScenario("s15",
                "Haces algo mal sin querer",
                "Digo: lo siento", "Digo: no es culpa mía",
                "¡Bien! Pedir disculpas", "La profesora se entristece", "A"));
        scenarios.put(buildScenario("s16",
                "Ves a dos niños peleándose",
                "Aviso a un profesor", "Me quedo mirando",
                "¡Bien! Avisar a un adulto", "La pelea sigue", "A"));
        scenarios.put(buildScenario("s17",
                "Un amigo te cuenta un secreto",
                "Se lo cuento a todos", "Lo guardo para mí",
                "Ya no confía en ti", "¡Genial! Guardar secretos", "B"));
        scenarios.put(buildScenario("s18",
                "Recibes un regalo que no te gusta",
                "Digo: gracias, qué bonito", "Digo: no me gusta",
                "¡Bien! Agradecer es educado", "Se pone triste", "A"));

        content.put("scenarios", scenarios);
        return content;
    }


    private JSONObject buildScenario(String id, String description,
                                     String optionA, String optionB,
                                     String outcomeA, String outcomeB,
                                     String correctOption) throws JSONException {
        JSONObject s = new JSONObject();
        s.put("id", id);
        s.put("description", description);
        s.put("optionA", optionA);
        s.put("optionB", optionB);
        s.put("outcomeA", outcomeA);
        s.put("outcomeB", outcomeB);
        s.put("correctOption", correctOption);
        return s;
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
