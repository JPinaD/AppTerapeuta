package com.example.appterapeuta.viewmodel;

import android.app.Application;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

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
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class SessionViewModel extends AndroidViewModel {

    private static final String TAG = "SessionViewModel";
    private static final String[] DEFAULT_PICTOGRAMS = {"pic_agua", "pic_jugar", "pic_comer", "pic_ayuda"};

    /** Pool de 12 emociones ARASAAC reutilizado para Turnos Sociales cooperativos. */
    private static final String[] EMOTION_POOL = {
            "emotion_happy", "emotion_sad", "emotion_angry", "emotion_surprised",
            "emotion_scared", "emotion_disgusted", "emotion_calm", "emotion_shy",
            "emotion_bored", "emotion_tired", "emotion_excited", "emotion_terror"
    };

    private final MutableLiveData<Session> currentSession = new MutableLiveData<>();
    private final MutableLiveData<Map<String, RobotSessionStatus>> robotStatuses =
            new MutableLiveData<>(new HashMap<>());

    private final StudentProfileRepository profileRepository;
    private final SessionRecordRepository sessionRecordRepository;
    private List<StudentProfileEntity> cachedProfiles = new ArrayList<>();
    private long sessionStartTimestamp = 0;

    // --- Turns coordination state ---
    private RobotViewModel turnsRobotViewModel;
    private final Handler turnsHandler = new Handler(Looper.getMainLooper());
    private int turnsCurrentRound = 0;
    private int turnsTotalRounds = 5;
    private List<TurnsRoundData> turnsRounds;
    /** Tracks which robots have responded in the current round: robotId -> selectedOption */
    private final Map<String, String> turnsResponses = new HashMap<>();
    /** Delay before sending the next round after correct result (ms). */
    private static final long TURNS_NEXT_ROUND_DELAY_CORRECT_MS = 3500;
    /** Delay before sending the next round after wrong result (ms).
     *  Must be > WRONG_ADVANCE_DELAY_MS in TurnsViewModel (4000ms) to avoid race condition. */
    private static final long TURNS_NEXT_ROUND_DELAY_WRONG_MS = 5000;

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

        // If this is a Turns activity with 2+ robots, initialize turn coordination
        if ("activity_turns".equals(session.activityId) && session.participatingRobotIds.size() >= 2) {
            initTurnsCoordination(robotViewModel, session);
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
            Log.e(TAG, "Error construyendo SESSION_END", e);
        }

        // Clean up turns state
        turnsHandler.removeCallbacksAndMessages(null);
        turnsRobotViewModel = null;

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
            Log.e(TAG, "Error construyendo SESSION_PAUSE", e);
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
            Log.e(TAG, "Error construyendo SESSION_RESUME", e);
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

        // Handle TURN_DONE for cooperative turns coordination
        if (AppConstants.MSG_TURN_DONE.equals(event.type)) {
            // Update status
            Map<String, RobotSessionStatus> current = new HashMap<>(safeStatuses());
            RobotSessionStatus status = current.get(event.robotId);
            if (status != null) {
                status.state = RobotSessionState.IN_ACTIVITY;
                robotStatuses.postValue(current);
            }
            handleTurnDone(event);
            return;
        }

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

    // =========================================================================
    // TURNS COORDINATION (cooperative multi-robot emotion recognition)
    // =========================================================================

    /**
     * Initializes the turn coordination for a multi-robot turns session.
     * Generates N rounds of emotion data and sends the first round after a brief delay.
     */
    private void initTurnsCoordination(RobotViewModel robotViewModel, Session session) {
        this.turnsRobotViewModel = robotViewModel;
        this.turnsCurrentRound = 0;
        this.turnsTotalRounds = 5; // default, could be configurable
        this.turnsRounds = generateTurnsRounds(turnsTotalRounds);
        this.turnsResponses.clear();

        // Send the first round after robots have time to navigate to TurnsActivity
        turnsHandler.postDelayed(() -> sendTurnsRound(session), 2000);
    }

    /**
     * Generates N rounds of emotion recognition data.
     * Each round has a correct emotion and 4 shuffled options.
     */
    private List<TurnsRoundData> generateTurnsRounds(int count) {
        List<TurnsRoundData> rounds = new ArrayList<>();
        List<String> pool = new ArrayList<>();
        for (String e : EMOTION_POOL) pool.add(e);
        String previousEmotion = null;

        for (int i = 0; i < count; i++) {
            List<String> candidates = new ArrayList<>(pool);
            if (previousEmotion != null) candidates.remove(previousEmotion);
            Collections.shuffle(candidates);

            String correct = candidates.get(0);
            previousEmotion = correct;

            // Build 4 options: 1 correct + 3 distractors
            List<String> distractors = new ArrayList<>(pool);
            distractors.remove(correct);
            Collections.shuffle(distractors);

            List<String> options = new ArrayList<>();
            options.add(correct);
            for (int j = 0; j < 3 && j < distractors.size(); j++) {
                options.add(distractors.get(j));
            }
            Collections.shuffle(options);

            rounds.add(new TurnsRoundData(correct, options));
        }
        return rounds;
    }

    /**
     * Sends the current round data as TURN_SIGNAL to all participating robots.
     * Payload: {active: true, roundData: {emotionId, options[], correctOption, round, totalRounds}}
     */
    private void sendTurnsRound(Session session) {
        if (turnsRobotViewModel == null || session == null) return;
        if (turnsCurrentRound >= turnsTotalRounds || turnsCurrentRound >= turnsRounds.size()) return;

        turnsResponses.clear();
        TurnsRoundData roundData = turnsRounds.get(turnsCurrentRound);

        try {
            JSONObject roundDataJson = new JSONObject();
            roundDataJson.put("emotionId", roundData.correctEmotionId);
            JSONArray optionsArr = new JSONArray();
            for (String opt : roundData.options) optionsArr.put(opt);
            roundDataJson.put("options", optionsArr);
            roundDataJson.put("correctOption", roundData.correctEmotionId);
            roundDataJson.put("round", turnsCurrentRound + 1);
            roundDataJson.put("totalRounds", turnsTotalRounds);

            JSONObject payload = new JSONObject();
            payload.put("active", true);
            payload.put("roundData", roundDataJson);

            JSONObject msg = new JSONObject();
            msg.put("type", AppConstants.MSG_TURN_SIGNAL);
            msg.put("payload", payload.toString());
            String msgStr = msg.toString();

            for (String robotId : session.participatingRobotIds) {
                turnsRobotViewModel.sendMessage(robotId, msgStr);
            }
            Log.d(TAG, "Sent TURN_SIGNAL round " + (turnsCurrentRound + 1) + "/" + turnsTotalRounds);
        } catch (JSONException e) {
            Log.e(TAG, "Error construyendo TURN_SIGNAL", e);
        }
    }

    /**
     * Handles TURN_DONE from a robot.
     * Waits for all robots to respond, then evaluates and sends result.
     */
    private void handleTurnDone(ActivityEvent event) {
        Session session = currentSession.getValue();
        if (session == null || turnsRobotViewModel == null) return;
        if (!"activity_turns".equals(session.activityId)) return;

        // Parse the selected option from payload
        String selectedOption = null;
        if (event.payload != null) {
            try {
                JSONObject payloadObj = new JSONObject(event.payload);
                selectedOption = payloadObj.optString("selectedOption", null);
            } catch (JSONException ignored) {}
        }

        turnsResponses.put(event.robotId, selectedOption);
        Log.d(TAG, "TURN_DONE from " + event.robotId + " selected=" + selectedOption
                + " (" + turnsResponses.size() + "/" + session.participatingRobotIds.size() + ")");

        // Wait until ALL robots have responded
        if (turnsResponses.size() < session.participatingRobotIds.size()) return;

        // Evaluate: all must be correct
        TurnsRoundData roundData = turnsCurrentRound < turnsRounds.size()
                ? turnsRounds.get(turnsCurrentRound) : null;
        if (roundData == null) return;

        boolean allCorrect = true;
        for (String response : turnsResponses.values()) {
            if (!roundData.correctEmotionId.equals(response)) {
                allCorrect = false;
                break;
            }
        }

        turnsCurrentRound++;
        boolean isLastRound = turnsCurrentRound >= turnsTotalRounds;

        // Send result to all robots
        sendTurnsResult(session, allCorrect, isLastRound, roundData.correctEmotionId);

        // Schedule next round (if any). Use longer delay on wrong to let robots finish
        // their visual feedback (WRONG_ADVANCE_DELAY_MS = 4000ms in TurnsViewModel).
        if (!isLastRound) {
            long delay = allCorrect ? TURNS_NEXT_ROUND_DELAY_CORRECT_MS : TURNS_NEXT_ROUND_DELAY_WRONG_MS;
            turnsHandler.postDelayed(() -> sendTurnsRound(session), delay);
        }
    }

    /**
     * Sends the round result to all robots.
     * Payload: {active: false, result: "correct"/"wrong", correctOption: emotionId, lastRound: bool}
     */
    private void sendTurnsResult(Session session, boolean allCorrect, boolean lastRound, String correctOption) {
        if (turnsRobotViewModel == null) return;

        try {
            JSONObject payload = new JSONObject();
            payload.put("active", false);
            payload.put("result", allCorrect ? "correct" : "wrong");
            payload.put("correctOption", correctOption);
            payload.put("lastRound", lastRound);

            JSONObject msg = new JSONObject();
            msg.put("type", AppConstants.MSG_TURN_SIGNAL);
            msg.put("payload", payload.toString());
            String msgStr = msg.toString();

            for (String robotId : session.participatingRobotIds) {
                turnsRobotViewModel.sendMessage(robotId, msgStr);
            }
            Log.d(TAG, "Sent TURN_SIGNAL result=" + (allCorrect ? "correct" : "wrong")
                    + " lastRound=" + lastRound);
        } catch (JSONException e) {
            Log.e(TAG, "Error construyendo TURN_SIGNAL result", e);
        }
    }

    /** Data class for a single turns round. */
    private static class TurnsRoundData {
        final String correctEmotionId;
        final List<String> options; // 4 options (shuffled, one is correct)

        TurnsRoundData(String correctEmotionId, List<String> options) {
            this.correctEmotionId = correctEmotionId;
            this.options = options;
        }
    }

    // =========================================================================
    // PRIVATE helpers
    // =========================================================================

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
            Log.e(TAG, "Error persistiendo SessionRecord", e);
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
            Log.e(TAG, "Error construyendo SESSION_START", e);
            return "";
        }
    }


    /**
     * Builds the activityContent JSON for each activity type.
     * Includes scenarios for Social, steps/items for Emotion, Sequence and Turns.
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
            case "activity_turns":
                return buildTurnsContent();
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
        for (String e : EMOTION_POOL) items.put(e);
        content.put("items", items);
        return content;
    }


    /**
     * Builds activityContent for Turns Sociales.
     * Includes the same 12 emotions as Emociones and a steps count.
     * AppTerapeuta coordinates via TURN_SIGNAL (requires 2+ robots).
     */
    private JSONObject buildTurnsContent() throws JSONException {
        JSONObject content = new JSONObject();
        content.put("steps", 5);
        JSONArray items = new JSONArray();
        for (String e : EMOTION_POOL) items.put(e);
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
