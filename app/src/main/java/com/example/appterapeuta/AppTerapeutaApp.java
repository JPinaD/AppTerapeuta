package com.example.appterapeuta;

import android.app.Application;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import androidx.lifecycle.ViewModelProvider;
import androidx.lifecycle.ViewModelStore;
import androidx.lifecycle.ViewModelStoreOwner;

import com.example.appterapeuta.data.local.db.AppDatabase;
import com.example.appterapeuta.data.local.entity.ItemResultEntity;
import com.example.appterapeuta.data.model.RobotConnection;
import com.example.appterapeuta.data.model.Session;
import com.example.appterapeuta.data.model.SessionState;
import com.example.appterapeuta.util.AppConstants;
import com.example.appterapeuta.viewmodel.ControlCenterViewModel;
import com.example.appterapeuta.viewmodel.RobotViewModel;
import com.example.appterapeuta.viewmodel.SessionViewModel;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.Map;
import java.util.concurrent.Executors;

/**
 * Application que expone ViewModels compartidos entre Activities.
 */
public class AppTerapeutaApp extends Application {

    private static final String TAG = "AppTerapeutaApp";

    private ViewModelStore viewModelStore;
    private RobotViewModel robotViewModel;
    private SessionViewModel sessionViewModel;
    private ControlCenterViewModel controlCenterViewModel;
    private final Handler mainHandler = new Handler(Looper.getMainLooper());

    @Override
    public void onCreate() {
        super.onCreate();
        viewModelStore = new ViewModelStore();
        ViewModelStoreOwner owner = () -> viewModelStore;
        ViewModelProvider.AndroidViewModelFactory factory =
                ViewModelProvider.AndroidViewModelFactory.getInstance(this);
        ViewModelProvider provider = new ViewModelProvider(owner, factory);
        robotViewModel         = provider.get(RobotViewModel.class);
        sessionViewModel       = provider.get(SessionViewModel.class);
        controlCenterViewModel = provider.get(ControlCenterViewModel.class);

        // Actualizar alumno asignado cuando cambia la sesión
        sessionViewModel.getCurrentSession().observeForever(session -> {
            if (session == null || session.state != SessionState.ACTIVE) return;
            for (String robotId : session.participatingRobotIds) {
                String profileId = session.robotToStudentProfileId.get(robotId);
                String studentName = sessionViewModel.getStudentName(profileId);
                controlCenterViewModel.setAssignedStudent(robotId, studentName);
            }
        });

        // Telemetría: listener directo para no perder eventos por sobreescritura de LiveData
        robotViewModel.setTelemetryListener((robotId, message) -> {
            mainHandler.post(() -> {
                try {
                    JSONObject obj = new JSONObject(message);
                    String type = obj.getString("type");
                    String payload = obj.optString("payload", null);

                    // Dispatch to control center (handles ROBOT_STATUS, TILT_ALERT, etc.)
                    controlCenterViewModel.onActivityEvent(
                            new com.example.appterapeuta.data.model.ActivityEvent(robotId, type, payload));

                    // Persist ACTIVITY_RESULT to item_results for per-item analysis
                    if (AppConstants.MSG_ACTIVITY_RESULT.equals(type) && payload != null) {
                        persistItemResult(robotId, payload);
                    }
                } catch (JSONException ignored) {}
            });
        });

        robotViewModel.getRobotConnections().observeForever(connections -> {
            if (connections == null) return;
            for (Map.Entry<String, RobotConnection> e : connections.entrySet()) {
                controlCenterViewModel.onConnectionStateChanged(e.getKey(), e.getValue().state);
            }
        });
    }

    /**
     * Persists an individual ACTIVITY_RESULT to the item_results table.
     * Payload expected: {sessionId, correct, expected (itemId), selected, step, totalSteps}
     */
    private void persistItemResult(String robotId, String payload) {
        Executors.newSingleThreadExecutor().execute(() -> {
            try {
                JSONObject p = new JSONObject(payload);
                String sessionId = p.optString("sessionId", null);
                boolean correct = p.optBoolean("correct", false);
                // "expected" contains the item being tested (e.g., emotion ID, scenario ID)
                String itemId = p.optString("expected", p.optString("itemId", null));

                if (sessionId == null) return;

                // Resolve studentId from active session
                Session session = sessionViewModel.getCurrentSession().getValue();
                String studentId = null;
                String activityId = null;
                if (session != null && session.robotToStudentProfileId != null) {
                    studentId = session.robotToStudentProfileId.get(robotId);
                    activityId = session.activityId;
                }

                AppDatabase db = AppDatabase.getInstance(getApplicationContext());
                ItemResultEntity item = new ItemResultEntity(
                        sessionId, activityId, studentId, itemId, correct, System.currentTimeMillis());
                db.itemResultDao().insert(item);
            } catch (JSONException e) {
                Log.w(TAG, "Error persisting ACTIVITY_RESULT: " + e.getMessage());
            }
        });
    }

    public RobotViewModel getRobotViewModel()                 { return robotViewModel; }
    public SessionViewModel getSessionViewModel()             { return sessionViewModel; }
    public ControlCenterViewModel getControlCenterViewModel() { return controlCenterViewModel; }
}
