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
                "Un compañero te saluda en el pasillo.",
                "Le ignoro y sigo andando", "Le saludo de vuelta",
                "Tu compañero se siente ignorado.", "¡Bien! Es agradable saludar.", "B"));
        scenarios.put(buildScenario("s2",
                "Quieres jugar con un juguete que tiene otro niño.",
                "Se lo quito de las manos", "Le pregunto si puedo jugar",
                "El otro niño se enfada y llora.", "¡Genial! Pedir las cosas está bien.", "B"));
        scenarios.put(buildScenario("s3",
                "Tu amigo se ha caído y está llorando.",
                "Le pregunto si está bien", "Me voy corriendo a jugar",
                "¡Bien! Ayudar a los demás es importante.", "Tu amigo se queda solo y triste.", "A"));
        scenarios.put(buildScenario("s4",
                "Estás en la fila y alguien se cuela delante de ti.",
                "Le empujo para que se quite", "Le digo que la fila empieza atrás",
                "El otro niño se enfada contigo.", "¡Bien! Hablar es mejor que empujar.", "B"));
        scenarios.put(buildScenario("s5",
                "La profesora hace una pregunta en clase.",
                "Grito la respuesta sin levantar la mano", "Levanto la mano y espero mi turno",
                "Los demás se asustan del grito.", "¡Muy bien! Esperar el turno es importante.", "B"));
        scenarios.put(buildScenario("s6",
                "Un compañero te enseña su dibujo.",
                "Le digo que es muy feo", "Le digo que me gusta",
                "Tu compañero se pone triste.", "¡Bien! Decir cosas bonitas alegra a los demás.", "B"));
        scenarios.put(buildScenario("s7",
                "Pierdes en un juego.",
                "Tiro las piezas al suelo enfadado", "Digo: no pasa nada, buen juego",
                "Los demás no quieren jugar contigo.", "¡Genial! Saber perder es muy valiente.", "B"));
        scenarios.put(buildScenario("s8",
                "Alguien te ofrece una galleta.",
                "La cojo sin decir nada", "Digo: gracias, qué amable",
                "La otra persona se siente ignorada.", "¡Bien! Dar las gracias es bonito.", "B"));
        scenarios.put(buildScenario("s9",
                "Tu compañero está jugando solo y parece aburrido.",
                "Le pregunto si quiere jugar conmigo", "Paso de largo sin hablarle",
                "¡Bien! Incluir a los demás es genial.", "Tu compañero sigue solo y triste.", "A"));
        scenarios.put(buildScenario("s10",
                "La profesora dice que es hora de recoger.",
                "Sigo jugando como si nada", "Recojo mis cosas",
                "La profesora tiene que repetírtelo.", "¡Muy bien! Seguir las normas ayuda a todos.", "B"));
        scenarios.put(buildScenario("s11",
                "Un niño nuevo llega a clase y no conoce a nadie.",
                "Me acerco y me presento", "Le miro pero no le digo nada",
                "¡Genial! Presentarse es muy amable.", "El niño nuevo se siente solo.", "A"));
        scenarios.put(buildScenario("s12",
                "Estás hablando y alguien te interrumpe.",
                "Espero a que termine y sigo hablando", "Le grito que se calle",
                "¡Bien! Tener paciencia es importante.", "La otra persona se asusta.", "A"));
        scenarios.put(buildScenario("s13",
                "Quieres ir al baño durante la clase.",
                "Me levanto y me voy sin decir nada", "Levanto la mano y pido permiso",
                "La profesora no sabe dónde estás.", "¡Bien! Pedir permiso es lo correcto.", "B"));
        scenarios.put(buildScenario("s14",
                "Tu compañero te pide prestado un lápiz.",
                "Le digo que no sin motivo", "Se lo presto con cuidado",
                "Tu compañero se siente rechazado.", "¡Genial! Compartir es muy bonito.", "B"));
        scenarios.put(buildScenario("s15",
                "Has hecho algo mal sin querer y la profesora te lo dice.",
                "Digo: lo siento, no lo haré más", "Me enfado y digo que no es culpa mía",
                "¡Bien! Pedir disculpas es de valientes.", "La profesora se pone triste.", "A"));
        scenarios.put(buildScenario("s16",
                "Estás en el patio y ves que dos niños se pelean.",
                "Voy a avisar a un profesor", "Me quedo mirando sin hacer nada",
                "¡Bien! Avisar a un adulto es lo correcto.", "La pelea continúa y alguien se puede hacer daño.", "A"));
        scenarios.put(buildScenario("s17",
                "Un amigo te cuenta un secreto.",
                "Se lo cuento a todos", "Lo guardo y no se lo digo a nadie",
                "Tu amigo se enfada y ya no confía en ti.", "¡Genial! Guardar secretos es ser buen amigo.", "B"));
        scenarios.put(buildScenario("s18",
                "Es tu cumpleaños y recibes un regalo que no te gusta mucho.",
                "Digo: gracias, qué bonito", "Digo: esto no me gusta, quiero otro",
                "¡Bien! Agradecer los regalos es educado.", "La persona que te lo dio se pone triste.", "A"));

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
