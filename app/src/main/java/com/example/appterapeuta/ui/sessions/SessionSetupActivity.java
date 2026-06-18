package com.example.appterapeuta.ui.sessions;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.appterapeuta.AppTerapeutaApp;
import com.example.appterapeuta.R;
import com.example.appterapeuta.data.local.entity.StudentProfileEntity;
import com.example.appterapeuta.data.model.ActivityContentItem;
import com.example.appterapeuta.data.model.ConnectionState;
import com.example.appterapeuta.data.model.RobotConnection;
import com.example.appterapeuta.data.model.SocialScenario;
import com.example.appterapeuta.data.repository.ActivityContentRepository;
import com.example.appterapeuta.util.AppConstants;
import com.example.appterapeuta.viewmodel.RobotViewModel;
import com.example.appterapeuta.viewmodel.SessionViewModel;
import com.example.appterapeuta.viewmodel.StudentProfileViewModel;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class SessionSetupActivity extends AppCompatActivity {

    // Actividades disponibles: id, etiqueta
    private static final String[] ACTIVITY_IDS = {
            AppConstants.ACTIVITY_PICTOGRAM,
            AppConstants.ACTIVITY_EMOTION,
            AppConstants.ACTIVITY_SOCIAL,
            AppConstants.ACTIVITY_SEQUENCE,
            AppConstants.ACTIVITY_CALM,
            AppConstants.ACTIVITY_TURNS
    };
    private static final String[] ACTIVITY_LABELS = {
            "Pictogramas",
            "Reconocimiento Emocional",
            "Escenarios Sociales",
            "Secuencias Visuales",
            "Momento Calma",
            "Turnos Sociales (multi-robot)"
    };

    private RobotViewModel robotViewModel;
    private SessionViewModel sessionViewModel;
    private StudentProfileViewModel profileViewModel;
    private ActivityContentRepository contentRepository;

    private SessionRobotAdapter robotAdapter;
    private List<StudentProfileEntity> allProfiles = new ArrayList<>();
    private Map<String, RobotConnection> connections = new HashMap<>();

    // Selección de contenido
    private List<String> selectedItemIds = new ArrayList<>();
    private List<SocialScenario> selectedScenarios = new ArrayList<>();
    private int selectedSequenceLength = 2;

    private Spinner spinnerActivity;
    private TextView tvContentSummary;
    private Button btnSelectContent;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_session_setup);

        robotViewModel   = ((AppTerapeutaApp) getApplication()).getRobotViewModel();
        sessionViewModel = ((AppTerapeutaApp) getApplication()).getSessionViewModel();
        profileViewModel = new ViewModelProvider(this).get(StudentProfileViewModel.class);
        contentRepository = new ActivityContentRepository();

        RecyclerView rvRobots = findViewById(R.id.rvRobots);
        rvRobots.setLayoutManager(new LinearLayoutManager(this));
        robotAdapter = new SessionRobotAdapter();
        rvRobots.setAdapter(robotAdapter);

        spinnerActivity  = findViewById(R.id.spinnerActivity);
        tvContentSummary = findViewById(R.id.tvContentSummary);
        btnSelectContent = findViewById(R.id.btnSelectContent);

        ArrayAdapter<String> actAdapter = new ArrayAdapter<>(this,
                android.R.layout.simple_spinner_item, ACTIVITY_LABELS);
        actAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerActivity.setAdapter(actAdapter);

        spinnerActivity.setOnItemSelectedListener(new android.widget.AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(android.widget.AdapterView<?> parent, View view, int pos, long id) {
                onActivitySelected(pos);
            }
            @Override
            public void onNothingSelected(android.widget.AdapterView<?> parent) {}
        });

        btnSelectContent.setOnClickListener(v -> showContentSelectionDialog());

        profileViewModel.profiles.observe(this, profiles -> {
            allProfiles.clear();
            if (profiles != null) allProfiles.addAll(profiles);
            robotAdapter.setProfiles(allProfiles);
        });

        robotViewModel.getDiscoveredRobots().observe(this, robots ->
                robotAdapter.setRobots(robots, connections));

        robotViewModel.getRobotConnections().observe(this, conns -> {
            connections = conns != null ? conns : new HashMap<>();
            robotAdapter.setConnections(connections);
            updateTurnsAvailability();
        });

        findViewById(R.id.btnStartSession).setOnClickListener(v -> startSession());
        findViewById(R.id.back_button).setOnClickListener(v -> finish());
    }

    @Override
    protected void onStart() {
        super.onStart();
        robotViewModel.startDiscovery();
    }

    @Override
    protected void onStop() {
        super.onStop();
        robotViewModel.stopDiscovery();
    }

    private void onActivitySelected(int position) {
        // Resetear selección de contenido al cambiar actividad
        selectedItemIds.clear();
        selectedScenarios.clear();
        selectedSequenceLength = 2;

        String activityId = ACTIVITY_IDS[position];
        boolean hasContent = activityHasContent(activityId);
        btnSelectContent.setVisibility(hasContent ? View.VISIBLE : View.GONE);
        tvContentSummary.setVisibility(hasContent ? View.VISIBLE : View.GONE);
        tvContentSummary.setText("");

        updateTurnsAvailability();
    }

    private boolean activityHasContent(String activityId) {
        return AppConstants.ACTIVITY_EMOTION.equals(activityId)
                || AppConstants.ACTIVITY_SOCIAL.equals(activityId)
                || AppConstants.ACTIVITY_TURNS.equals(activityId)
                || AppConstants.ACTIVITY_SEQUENCE.equals(activityId);
    }

    private void updateTurnsAvailability() {
        int pos = spinnerActivity.getSelectedItemPosition();
        if (pos < 0 || pos >= ACTIVITY_IDS.length) return;
        if (!AppConstants.ACTIVITY_TURNS.equals(ACTIVITY_IDS[pos])) return;

        int connectedCount = countConnectedRobots();
        Button btnStart = findViewById(R.id.btnStartSession);
        if (connectedCount < 2) {
            btnStart.setEnabled(false);
            btnStart.setText("Turnos requiere ≥ 2 robots");
        } else {
            btnStart.setEnabled(true);
            btnStart.setText("▶  INICIAR SESIÓN");
        }
    }

    private int countConnectedRobots() {
        int count = 0;
        for (RobotConnection conn : connections.values()) {
            if (conn.state == ConnectionState.CONNECTED) count++;
        }
        return count;
    }

    private void showContentSelectionDialog() {
        int pos = spinnerActivity.getSelectedItemPosition();
        if (pos < 0 || pos >= ACTIVITY_IDS.length) return;
        String activityId = ACTIVITY_IDS[pos];

        if (AppConstants.ACTIVITY_EMOTION.equals(activityId)
                || AppConstants.ACTIVITY_TURNS.equals(activityId)) {
            showItemSelectionDialog(activityId);
        } else if (AppConstants.ACTIVITY_SOCIAL.equals(activityId)) {
            showScenarioSelectionDialog();
        } else if (AppConstants.ACTIVITY_SEQUENCE.equals(activityId)) {
            showSequenceLengthDialog();
        }
    }

    private void showItemSelectionDialog(String activityId) {
        List<ActivityContentItem> items = contentRepository.getItemsForActivity(activityId);
        if (items.isEmpty()) return;

        String[] labels = new String[items.size()];
        boolean[] checked = new boolean[items.size()];
        for (int i = 0; i < items.size(); i++) {
            labels[i] = items.get(i).label;
            checked[i] = selectedItemIds.contains(items.get(i).id);
        }

        new AlertDialog.Builder(this)
                .setTitle("Selecciona ítems")
                .setMultiChoiceItems(labels, checked, (dialog, which, isChecked) -> {
                    String id = items.get(which).id;
                    if (isChecked) { if (!selectedItemIds.contains(id)) selectedItemIds.add(id); }
                    else selectedItemIds.remove(id);
                })
                .setPositiveButton("Aceptar", (d, w) ->
                        tvContentSummary.setText(selectedItemIds.size() + " ítem(s) seleccionado(s)"))
                .setNegativeButton("Cancelar", null)
                .show();
    }

    private void showScenarioSelectionDialog() {
        List<SocialScenario> scenarios = contentRepository.getSocialScenarios();
        if (scenarios.isEmpty()) return;

        String[] labels = new String[scenarios.size()];
        boolean[] checked = new boolean[scenarios.size()];
        List<String> selectedIds = new ArrayList<>();
        for (SocialScenario s : selectedScenarios) selectedIds.add(s.id);

        for (int i = 0; i < scenarios.size(); i++) {
            labels[i] = scenarios.get(i).description;
            checked[i] = selectedIds.contains(scenarios.get(i).id);
        }

        new AlertDialog.Builder(this)
                .setTitle("Selecciona escenarios")
                .setMultiChoiceItems(labels, checked, (dialog, which, isChecked) -> {
                    SocialScenario s = scenarios.get(which);
                    if (isChecked) { if (!selectedIds.contains(s.id)) { selectedIds.add(s.id); selectedScenarios.add(s); } }
                    else { selectedIds.remove(s.id); selectedScenarios.removeIf(sc -> sc.id.equals(s.id)); }
                })
                .setPositiveButton("Aceptar", (d, w) ->
                        tvContentSummary.setText(selectedScenarios.size() + " escenario(s) seleccionado(s)"))
                .setNegativeButton("Cancelar", null)
                .show();
    }

    private void showSequenceLengthDialog() {
        new AlertDialog.Builder(this)
                .setTitle("Longitud de secuencia")
                .setItems(new String[]{"2 elementos", "3 elementos"}, (d, which) -> {
                    selectedSequenceLength = which + 2;
                    tvContentSummary.setText("Longitud: " + selectedSequenceLength);
                })
                .show();
    }

    private void startSession() {
        List<String> selectedRobotIds = robotAdapter.getSelectedRobotIds();
        if (selectedRobotIds.isEmpty()) {
            Toast.makeText(this, "Selecciona al menos un robot conectado", Toast.LENGTH_SHORT).show();
            return;
        }

        List<String> connected = new ArrayList<>();
        for (String id : selectedRobotIds) {
            RobotConnection conn = connections.get(id);
            if (conn != null && conn.state == ConnectionState.CONNECTED) {
                connected.add(id);
            } else {
                Toast.makeText(this, "Robot " + id + " no conectado, se excluye", Toast.LENGTH_SHORT).show();
            }
        }
        if (connected.isEmpty()) {
            Toast.makeText(this, "Ningún robot seleccionado está conectado", Toast.LENGTH_SHORT).show();
            return;
        }

        int pos = spinnerActivity.getSelectedItemPosition();
        String activityId = (pos >= 0 && pos < ACTIVITY_IDS.length)
                ? ACTIVITY_IDS[pos] : AppConstants.ACTIVITY_PICTOGRAM;

        // Validar contenido requerido
        if (AppConstants.ACTIVITY_EMOTION.equals(activityId) && selectedItemIds.isEmpty()) {
            Toast.makeText(this, "Selecciona al menos una emoción", Toast.LENGTH_SHORT).show();
            return;
        }
        if (AppConstants.ACTIVITY_SOCIAL.equals(activityId) && selectedScenarios.isEmpty()) {
            Toast.makeText(this, "Selecciona al menos un escenario social", Toast.LENGTH_SHORT).show();
            return;
        }
        if (AppConstants.ACTIVITY_TURNS.equals(activityId)) {
            if (connected.size() < 2) {
                Toast.makeText(this, "Turnos Sociales requiere al menos 2 robots conectados", Toast.LENGTH_SHORT).show();
                return;
            }
            if (selectedItemIds.isEmpty()) {
                Toast.makeText(this, "Selecciona al menos un pictograma de turno", Toast.LENGTH_SHORT).show();
                return;
            }
        }

        Map<String, String> robotToProfile = robotAdapter.getRobotToProfileId(connected);
        sessionViewModel.prepareSession(connected, robotToProfile, activityId,
                selectedItemIds, selectedScenarios, selectedSequenceLength);
        sessionViewModel.startSession(robotViewModel, allProfiles);

        startActivity(new Intent(this, SessionLiveActivity.class));
    }
}
