package com.example.appterapeuta.ui.activity;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;

import com.example.appterapeuta.R;
import com.example.appterapeuta.data.local.entity.StudentProfileEntity;
import com.example.appterapeuta.data.model.ConnectionState;
import com.example.appterapeuta.data.model.RobotConnection;
import com.example.appterapeuta.util.AppConstants;
import com.example.appterapeuta.viewmodel.RobotViewModel;
import com.example.appterapeuta.viewmodel.StudentProfileViewModel;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class PictogramActivityLauncherActivity extends AppCompatActivity {

    public static final String EXTRA_ROBOT_ID   = "robot_id";
    public static final String EXTRA_ROBOT_NAME = "robot_name";

    private static final String[] PICTOGRAM_IDS = {"pic_agua", "pic_jugar", "pic_comer", "pic_ayuda"};
    private static final String FEEDBACK_TEXT   = "¡Muy bien!";

    private RobotViewModel robotViewModel;
    private String robotId;

    private Spinner spinnerStudent;
    private List<StudentProfileEntity> profileList = new ArrayList<>();

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

        tvSelectionLabel    = findViewById(R.id.tvSelectionLabel);
        tvSelectedPictogram = findViewById(R.id.tvSelectedPictogram);
        btnSendFeedback     = findViewById(R.id.btnSendFeedback);
        spinnerStudent      = findViewById(R.id.spinnerStudent);

        robotViewModel = new ViewModelProvider(this).get(RobotViewModel.class);

        // Observar lista de perfiles para poblar el Spinner
        StudentProfileViewModel profileViewModel =
                new ViewModelProvider(this).get(StudentProfileViewModel.class);
        profileViewModel.profiles.observe(this, profiles -> {
            profileList.clear();
            if (profiles != null) profileList.addAll(profiles);

            List<String> names = new ArrayList<>();
            names.add("Sin alumno");
            for (StudentProfileEntity p : profileList) names.add(p.name);

            ArrayAdapter<String> adapter = new ArrayAdapter<>(
                    this, android.R.layout.simple_spinner_item, names);
            adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
            spinnerStudent.setAdapter(adapter);
        });

        findViewById(R.id.back_button).setOnClickListener(v -> finish());
        findViewById(R.id.btnLaunchActivity).setOnClickListener(v -> launchActivity());
        btnSendFeedback.setOnClickListener(v -> sendFeedback());

        robotViewModel.getActivityEvents().observe(this, event -> {
            if (event == null || !robotId.equals(event.robotId)) return;
            if (AppConstants.MSG_PICTOGRAM_SELECTED.equals(event.type)) {
                showSelection(event.payload);
            }
        });
    }

    private void launchActivity() {
        Map<String, RobotConnection> connections = robotViewModel.getRobotConnections().getValue();
        RobotConnection conn = connections != null ? connections.get(robotId) : null;
        if (conn == null || conn.state != ConnectionState.CONNECTED) {
            Toast.makeText(this, "El robot no está conectado", Toast.LENGTH_SHORT).show();
            return;
        }
        try {
            JSONArray pics = new JSONArray();
            for (String id : PICTOGRAM_IDS) pics.put(id);

            JSONObject payload = new JSONObject();
            payload.put("activityId", AppConstants.ACTIVITY_PICTOGRAM);
            payload.put("pictograms", pics);

            // Incluir perfil del alumno si hay uno seleccionado
            int selectedPos = spinnerStudent.getSelectedItemPosition();
            if (selectedPos > 0) {
                StudentProfileEntity profile = profileList.get(selectedPos - 1);
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
            msg.put("type", AppConstants.MSG_ACTIVITY_START);
            msg.put("payload", payload.toString());

            robotViewModel.sendMessage(robotId, msg.toString());
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

            robotViewModel.sendMessage(robotId, msg.toString());
        } catch (JSONException e) {
            Toast.makeText(this, "Error al enviar feedback", Toast.LENGTH_SHORT).show();
        }
    }
}
