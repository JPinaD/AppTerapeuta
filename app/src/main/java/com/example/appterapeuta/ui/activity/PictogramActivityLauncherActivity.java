package com.example.appterapeuta.ui.activity;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;

import com.example.appterapeuta.R;
import com.example.appterapeuta.data.model.ConnectionState;
import com.example.appterapeuta.data.model.RobotConnection;
import com.example.appterapeuta.util.AppConstants;
import com.example.appterapeuta.viewmodel.RobotViewModel;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.Map;

/**
 * Pantalla del terapeuta para lanzar la actividad de pictogramas en un robot
 * y hacer seguimiento de la selección del alumno.
 */
public class PictogramActivityLauncherActivity extends AppCompatActivity {

    public static final String EXTRA_ROBOT_ID   = "robot_id";
    public static final String EXTRA_ROBOT_NAME = "robot_name";

    private static final String[] PICTOGRAM_IDS = {"pic_agua", "pic_jugar", "pic_comer", "pic_ayuda"};
    private static final String FEEDBACK_TEXT   = "¡Muy bien!";

    private RobotViewModel viewModel;
    private String robotId;

    private TextView tvSelectedPictogram;
    private TextView tvSelectionLabel;
    private Button btnSendFeedback;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_pictogram_launcher);

        robotId = getIntent().getStringExtra(EXTRA_ROBOT_ID);
        String robotName = getIntent().getStringExtra(EXTRA_ROBOT_NAME);

        TextView tvRobotName = findViewById(R.id.tvRobotName);
        tvRobotName.setText("Robot: " + robotName);

        tvSelectionLabel     = findViewById(R.id.tvSelectionLabel);
        tvSelectedPictogram  = findViewById(R.id.tvSelectedPictogram);
        btnSendFeedback      = findViewById(R.id.btnSendFeedback);

        viewModel = new ViewModelProvider(this).get(RobotViewModel.class);

        findViewById(R.id.back_button).setOnClickListener(v -> finish());

        findViewById(R.id.btnLaunchActivity).setOnClickListener(v -> launchActivity());

        btnSendFeedback.setOnClickListener(v -> sendFeedback());

        viewModel.getActivityEvents().observe(this, event -> {
            if (event == null || !robotId.equals(event.robotId)) return;
            if (AppConstants.MSG_PICTOGRAM_SELECTED.equals(event.type)) {
                showSelection(event.payload);
            }
        });
    }

    private void launchActivity() {
        Map<String, RobotConnection> connections = viewModel.getRobotConnections().getValue();
        RobotConnection conn = connections != null ? connections.get(robotId) : null;
        if (conn == null || conn.state != ConnectionState.CONNECTED) {
            Toast.makeText(this, "El robot no está conectado", Toast.LENGTH_SHORT).show();
            return;
        }
        try {
            JSONArray pics = new JSONArray();
            for (String id : PICTOGRAM_IDS) pics.put(id);
            JSONObject payload = new JSONObject();
            payload.put("activityId", "pictogram_v1");
            payload.put("pictograms", pics);

            JSONObject msg = new JSONObject();
            msg.put("type", AppConstants.MSG_ACTIVITY_START);
            msg.put("payload", payload.toString());

            viewModel.sendMessage(robotId, msg.toString());
        } catch (JSONException e) {
            Toast.makeText(this, "Error al construir mensaje", Toast.LENGTH_SHORT).show();
        }
    }

    private void showSelection(String payload) {
        String label = payload;
        try {
            JSONObject obj = new JSONObject(payload);
            label = obj.optString("pictogramId", payload);
        } catch (JSONException ignored) {}

        tvSelectionLabel.setVisibility(View.VISIBLE);
        tvSelectedPictogram.setText(label);
        tvSelectedPictogram.setVisibility(View.VISIBLE);
        btnSendFeedback.setVisibility(View.VISIBLE);
    }

    private void sendFeedback() {
        try {
            JSONObject payload = new JSONObject();
            payload.put("text", FEEDBACK_TEXT);

            JSONObject msg = new JSONObject();
            msg.put("type", AppConstants.MSG_ROBOT_FEEDBACK);
            msg.put("payload", payload.toString());

            viewModel.sendMessage(robotId, msg.toString());
        } catch (JSONException e) {
            Toast.makeText(this, "Error al enviar feedback", Toast.LENGTH_SHORT).show();
        }
    }
}
