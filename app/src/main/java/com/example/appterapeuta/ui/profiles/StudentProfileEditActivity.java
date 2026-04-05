package com.example.appterapeuta.ui.profiles;

import android.os.Bundle;
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

    // Paleta de colores comunes que pueden excluirse
    private static final String[][] COLOR_OPTIONS = {
            {"Verde suave",   "#C8E6C9"},
            {"Verde",         "#4CAF50"},
            {"Rojo",          "#F44336"},
            {"Azul",          "#2196F3"},
            {"Amarillo",      "#FFEB3B"},
            {"Naranja",       "#FF9800"},
            {"Morado",        "#9C27B0"},
            {"Rosa",          "#E91E63"},
    };

    // Sonidos disponibles en AppRobot res/raw (nombre del recurso)
    private static final String[] SOUND_OPTIONS = {
            "Ninguno",
            "sound_birds",
            "sound_ocean",
            "sound_rain",
    };

    private TextInputEditText etName;
    private ChipGroup chipGroupColors;
    private Spinner spinnerSound;
    private StudentProfileViewModel viewModel;
    private String editingId = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_student_profile_edit);

        etName         = findViewById(R.id.etName);
        chipGroupColors = findViewById(R.id.chipGroupColors);
        spinnerSound   = findViewById(R.id.spinnerSound);
        TextView tvTitle = findViewById(R.id.tvFormTitle);

        viewModel = new ViewModelProvider(this).get(StudentProfileViewModel.class);

        // Construir chips de colores
        for (String[] color : COLOR_OPTIONS) {
            Chip chip = new Chip(this);
            chip.setText(color[0]);
            chip.setTag(color[1]);
            chip.setCheckable(true);
            chipGroupColors.addView(chip);
        }

        // Spinner de sonidos
        ArrayAdapter<String> soundAdapter = new ArrayAdapter<>(
                this, android.R.layout.simple_spinner_item, SOUND_OPTIONS);
        soundAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerSound.setAdapter(soundAdapter);

        editingId = getIntent().getStringExtra(EXTRA_STUDENT_ID);
        if (editingId != null) {
            tvTitle.setText("Editar alumno");
            // Cargar datos del perfil existente
            viewModel.profiles.observe(this, profiles -> {
                for (StudentProfileEntity p : profiles) {
                    if (p.id.equals(editingId)) {
                        etName.setText(p.name);
                        populateColors(p.excludedColors);
                        populateSound(p.backgroundSoundResName);
                        break;
                    }
                }
            });
        } else {
            tvTitle.setText("Nuevo alumno");
        }

        Button btnSave = findViewById(R.id.btnSave);
        btnSave.setOnClickListener(v -> save());

        findViewById(R.id.back_button).setOnClickListener(v -> finish());
    }

    private void save() {
        String name = etName.getText() != null ? etName.getText().toString().trim() : "";
        if (name.isEmpty()) {
            Toast.makeText(this, "El nombre no puede estar vacío", Toast.LENGTH_SHORT).show();
            return;
        }

        String excludedColors = buildExcludedColorsJson();
        String sound = spinnerSound.getSelectedItemPosition() == 0
                ? null
                : SOUND_OPTIONS[spinnerSound.getSelectedItemPosition()];

        if (editingId != null) {
            viewModel.update(new StudentProfileEntity(editingId, name, excludedColors, sound));
        } else {
            viewModel.insert(new StudentProfileEntity(UUID.randomUUID().toString(), name, excludedColors, sound));
        }
        finish();
    }

    private String buildExcludedColorsJson() {
        List<String> selected = new ArrayList<>();
        for (int i = 0; i < chipGroupColors.getChildCount(); i++) {
            Chip chip = (Chip) chipGroupColors.getChildAt(i);
            if (chip.isChecked()) selected.add((String) chip.getTag());
        }
        return new JSONArray(selected).toString();
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

    private void populateSound(String resName) {
        if (resName == null) return;
        for (int i = 0; i < SOUND_OPTIONS.length; i++) {
            if (SOUND_OPTIONS[i].equals(resName)) {
                spinnerSound.setSelection(i);
                return;
            }
        }
    }
}
