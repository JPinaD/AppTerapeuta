package com.example.appterapeuta.ui.profiles;

import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;

import com.example.appterapeuta.R;
import com.example.appterapeuta.data.local.entity.StudentProfileEntity;
import com.example.appterapeuta.viewmodel.StudentProfileViewModel;
import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;
import com.google.android.material.textfield.TextInputEditText;

import org.json.JSONArray;
import org.json.JSONException;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class StudentProfileEditActivity extends AppCompatActivity {

    public static final String EXTRA_STUDENT_ID = "student_id";

    private static final String[][] COLOR_OPTIONS = {
            {"Verde suave", "#C8E6C9"}, {"Verde", "#4CAF50"}, {"Rojo", "#F44336"},
            {"Azul", "#2196F3"}, {"Amarillo", "#FFEB3B"}, {"Naranja", "#FF9800"},
            {"Morado", "#9C27B0"}, {"Rosa", "#E91E63"},
    };

    private static final String[] SOUND_OPTIONS = {"Ninguno", "sound_birds", "sound_ocean", "sound_rain"};

    private static final String[] COMMUNICATION_OPTIONS = {
            "—", "No verbal", "Comunicación emergente",
            "Comunicación funcional con apoyo", "Comunicación verbal funcional", "Otros (especificar)"};
    private static final String[] SENSORY_OPTIONS = {
            "—", "Sin alteraciones aparentes", "Hipersensibilidad (evita estímulos)",
            "Hiposensibilidad (busca estímulos)", "Perfil mixto", "Otros (especificar)"};
    private static final String[] ATTENTION_OPTIONS = {
            "—", "Atención muy reducida (< 5 min)", "Atención reducida (5-10 min)",
            "Atención moderada (10-20 min)", "Atención sostenida (> 20 min)", "Otros (especificar)"};
    private static final String[] MOTOR_OPTIONS = {
            "—", "Sin dificultades aparentes", "Dificultades en motricidad fina",
            "Dificultades en motricidad gruesa", "Dificultades en ambas", "Otros (especificar)"};
    private static final String[] SOCIOEMOTIONAL_OPTIONS = {
            "—", "Alta reactividad emocional", "Dificultad en reconocimiento emocional",
            "Tendencia al aislamiento", "Conductas de búsqueda de interacción",
            "Perfil mixto", "Otros (especificar)"};

    private TextInputEditText etName, etCommunicationOther, etSensoryOther,
            etAttentionOther, etMotorOther, etSocioemotionalOther, etClinicalNotes, etSafePlaceUri;
    private ChipGroup chipGroupColors;
    private Spinner spinnerSound, spinnerCommunication, spinnerSensory,
            spinnerAttention, spinnerMotor, spinnerSocioemotional;
    private StudentProfileViewModel viewModel;
    private String editingId = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_student_profile_edit);

        bindViews();
        viewModel = new ViewModelProvider(this).get(StudentProfileViewModel.class);

        setupSpinner(spinnerSound, SOUND_OPTIONS, null);
        setupSpinner(spinnerCommunication, COMMUNICATION_OPTIONS, etCommunicationOther);
        setupSpinner(spinnerSensory, SENSORY_OPTIONS, etSensoryOther);
        setupSpinner(spinnerAttention, ATTENTION_OPTIONS, etAttentionOther);
        setupSpinner(spinnerMotor, MOTOR_OPTIONS, etMotorOther);
        setupSpinner(spinnerSocioemotional, SOCIOEMOTIONAL_OPTIONS, etSocioemotionalOther);

        for (String[] color : COLOR_OPTIONS) {
            Chip chip = new Chip(this);
            chip.setText(color[0]);
            chip.setTag(color[1]);
            chip.setCheckable(true);
            chipGroupColors.addView(chip);
        }

        editingId = getIntent().getStringExtra(EXTRA_STUDENT_ID);
        TextView tvTitle = findViewById(R.id.tvFormTitle);
        if (editingId != null) {
            tvTitle.setText("Editar alumno");
            viewModel.profiles.observe(this, profiles -> {
                for (StudentProfileEntity p : profiles) {
                    if (p.id.equals(editingId)) { populate(p); break; }
                }
            });
        } else {
            tvTitle.setText("Nuevo alumno");
        }

        findViewById(R.id.btnSave).setOnClickListener(v -> save());
        findViewById(R.id.back_button).setOnClickListener(v -> finish());
        findViewById(R.id.btnSelectSafePlace).setOnClickListener(v ->
                Toast.makeText(this, "Próximamente", Toast.LENGTH_SHORT).show());
    }

    private void bindViews() {
        etName                  = findViewById(R.id.etName);
        chipGroupColors         = findViewById(R.id.chipGroupColors);
        spinnerSound            = findViewById(R.id.spinnerSound);
        spinnerCommunication    = findViewById(R.id.spinnerCommunication);
        etCommunicationOther    = findViewById(R.id.etCommunicationOther);
        spinnerSensory          = findViewById(R.id.spinnerSensory);
        etSensoryOther          = findViewById(R.id.etSensoryOther);
        spinnerAttention        = findViewById(R.id.spinnerAttention);
        etAttentionOther        = findViewById(R.id.etAttentionOther);
        spinnerMotor            = findViewById(R.id.spinnerMotor);
        etMotorOther            = findViewById(R.id.etMotorOther);
        spinnerSocioemotional   = findViewById(R.id.spinnerSocioemotional);
        etSocioemotionalOther   = findViewById(R.id.etSocioemotionalOther);
        etClinicalNotes         = findViewById(R.id.etClinicalNotes);
        etSafePlaceUri          = findViewById(R.id.etSafePlaceUri);
    }

    private void setupSpinner(Spinner spinner, String[] options, TextInputEditText otherField) {
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this,
                android.R.layout.simple_spinner_item, options);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinner.setAdapter(adapter);
        if (otherField != null) {
            spinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
                @Override
                public void onItemSelected(AdapterView<?> p, View v, int pos, long id) {
                    boolean isOther = options[pos].startsWith("Otros");
                    otherField.setVisibility(isOther ? View.VISIBLE : View.GONE);
                }
                @Override public void onNothingSelected(AdapterView<?> p) {}
            });
        }
    }

    private void save() {
        String name = etName.getText() != null ? etName.getText().toString().trim() : "";
        if (name.isEmpty()) {
            Toast.makeText(this, "El nombre no puede estar vacío", Toast.LENGTH_SHORT).show();
            return;
        }

        String id = editingId != null ? editingId : UUID.randomUUID().toString();
        String excludedColors = buildExcludedColorsJson();
        String sound = spinnerSound.getSelectedItemPosition() == 0 ? null
                : SOUND_OPTIONS[spinnerSound.getSelectedItemPosition()];

        StudentProfileEntity p = new StudentProfileEntity(id, name, excludedColors, sound);
        p.communicationLevel         = spinnerValue(spinnerCommunication, COMMUNICATION_OPTIONS);
        p.communicationLevelOther    = otherText(spinnerCommunication, COMMUNICATION_OPTIONS, etCommunicationOther);
        p.sensorySensitivity         = spinnerValue(spinnerSensory, SENSORY_OPTIONS);
        p.sensorySensitivityOther    = otherText(spinnerSensory, SENSORY_OPTIONS, etSensoryOther);
        p.attentionLevel             = spinnerValue(spinnerAttention, ATTENTION_OPTIONS);
        p.attentionLevelOther        = otherText(spinnerAttention, ATTENTION_OPTIONS, etAttentionOther);
        p.motorSkills                = spinnerValue(spinnerMotor, MOTOR_OPTIONS);
        p.motorSkillsOther           = otherText(spinnerMotor, MOTOR_OPTIONS, etMotorOther);
        p.socioemotionalProfile      = spinnerValue(spinnerSocioemotional, SOCIOEMOTIONAL_OPTIONS);
        p.socioemotionalProfileOther = otherText(spinnerSocioemotional, SOCIOEMOTIONAL_OPTIONS, etSocioemotionalOther);
        p.clinicalNotes              = text(etClinicalNotes);
        p.safePlaceUri               = text(etSafePlaceUri);

        if (editingId != null) viewModel.update(p);
        else viewModel.insert(p);
        finish();
    }

    /** Devuelve el valor seleccionado, o null si es "—". */
    private String spinnerValue(Spinner spinner, String[] options) {
        int pos = spinner.getSelectedItemPosition();
        return (pos == 0) ? null : options[pos];
    }

    /** Devuelve el texto libre solo si se seleccionó "Otros". */
    private String otherText(Spinner spinner, String[] options, TextInputEditText et) {
        int pos = spinner.getSelectedItemPosition();
        if (!options[pos].startsWith("Otros")) return null;
        String t = et.getText() != null ? et.getText().toString().trim() : "";
        return t.isEmpty() ? null : t;
    }

    private String text(TextInputEditText et) {
        String t = et.getText() != null ? et.getText().toString().trim() : "";
        return t.isEmpty() ? null : t;
    }

    private String buildExcludedColorsJson() {
        List<String> selected = new ArrayList<>();
        for (int i = 0; i < chipGroupColors.getChildCount(); i++) {
            Chip chip = (Chip) chipGroupColors.getChildAt(i);
            if (chip.isChecked()) selected.add((String) chip.getTag());
        }
        return new JSONArray(selected).toString();
    }

    private void populate(StudentProfileEntity p) {
        etName.setText(p.name);
        populateColors(p.excludedColors);
        populateSpinner(spinnerSound, SOUND_OPTIONS, p.backgroundSoundResName);
        populateSpinnerWithOther(spinnerCommunication, COMMUNICATION_OPTIONS, etCommunicationOther,
                p.communicationLevel, p.communicationLevelOther);
        populateSpinnerWithOther(spinnerSensory, SENSORY_OPTIONS, etSensoryOther,
                p.sensorySensitivity, p.sensorySensitivityOther);
        populateSpinnerWithOther(spinnerAttention, ATTENTION_OPTIONS, etAttentionOther,
                p.attentionLevel, p.attentionLevelOther);
        populateSpinnerWithOther(spinnerMotor, MOTOR_OPTIONS, etMotorOther,
                p.motorSkills, p.motorSkillsOther);
        populateSpinnerWithOther(spinnerSocioemotional, SOCIOEMOTIONAL_OPTIONS, etSocioemotionalOther,
                p.socioemotionalProfile, p.socioemotionalProfileOther);
        if (p.clinicalNotes != null) etClinicalNotes.setText(p.clinicalNotes);
        if (p.safePlaceUri != null) etSafePlaceUri.setText(p.safePlaceUri);
    }

    private void populateColors(String json) {
        if (json == null || json.isEmpty()) return;
        try {
            JSONArray arr = new JSONArray(json);
            List<String> excluded = new ArrayList<>();
            for (int i = 0; i < arr.length(); i++) excluded.add(arr.getString(i).toUpperCase());
            for (int i = 0; i < chipGroupColors.getChildCount(); i++) {
                Chip chip = (Chip) chipGroupColors.getChildAt(i);
                chip.setChecked(excluded.contains(((String) chip.getTag()).toUpperCase()));
            }
        } catch (JSONException ignored) {}
    }

    private void populateSpinner(Spinner spinner, String[] options, String value) {
        if (value == null) return;
        for (int i = 0; i < options.length; i++) {
            if (options[i].equals(value)) { spinner.setSelection(i); return; }
        }
    }

    private void populateSpinnerWithOther(Spinner spinner, String[] options,
                                          TextInputEditText otherField, String value, String other) {
        if (value == null) return;
        for (int i = 0; i < options.length; i++) {
            if (options[i].equals(value)) {
                spinner.setSelection(i);
                if (options[i].startsWith("Otros") && other != null) {
                    otherField.setVisibility(View.VISIBLE);
                    otherField.setText(other);
                }
                return;
            }
        }
    }
}
