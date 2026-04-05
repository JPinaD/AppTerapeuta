package com.example.appterapeuta.ui.profiles;

import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.appterapeuta.R;
import com.example.appterapeuta.data.local.entity.StudentProfileEntity;

import org.json.JSONArray;
import org.json.JSONException;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import android.view.ViewGroup;

public class StudentProfileAdapter extends RecyclerView.Adapter<StudentProfileAdapter.StudentViewHolder> {

    public interface OnDeleteListener { void onDelete(StudentProfileEntity profile); }

    // Mapa hex → nombre legible (mismo orden que StudentProfileEditActivity)
    private static final Map<String, String> COLOR_NAMES = new LinkedHashMap<>();
    static {
        COLOR_NAMES.put("#C8E6C9", "Verde suave");
        COLOR_NAMES.put("#4CAF50", "Verde");
        COLOR_NAMES.put("#F44336", "Rojo");
        COLOR_NAMES.put("#2196F3", "Azul");
        COLOR_NAMES.put("#FFEB3B", "Amarillo");
        COLOR_NAMES.put("#FF9800", "Naranja");
        COLOR_NAMES.put("#9C27B0", "Morado");
        COLOR_NAMES.put("#E91E63", "Rosa");
    }

    // Mapa resName → nombre legible
    private static final Map<String, String> SOUND_NAMES = new LinkedHashMap<>();
    static {
        SOUND_NAMES.put("sound_birds", "Pájaros");
        SOUND_NAMES.put("sound_ocean", "Océano");
        SOUND_NAMES.put("sound_rain",  "Lluvia");
    }

    private final List<StudentProfileEntity> students = new ArrayList<>();
    private OnDeleteListener deleteListener;
    private boolean editMode = false;

    public void setStudents(List<StudentProfileEntity> list) {
        students.clear();
        if (list != null) students.addAll(list);
        notifyDataSetChanged();
    }

    public void setOnDeleteListener(OnDeleteListener listener) {
        this.deleteListener = listener;
    }

    public void setEditMode(boolean enabled) {
        this.editMode = enabled;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public StudentViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_student_profile, parent, false);
        return new StudentViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull StudentViewHolder holder, int position) {
        StudentProfileEntity s = students.get(position);
        holder.tvName.setText(s.name);
        holder.tvColors.setText(resolveColorNames(s.excludedColors));
        holder.tvSound.setText(resolveSoundName(s.backgroundSoundResName));

        int visibility = editMode ? View.VISIBLE : View.GONE;
        holder.btnEdit.setVisibility(visibility);
        holder.btnDelete.setVisibility(visibility);

        holder.btnEdit.setOnClickListener(v -> {
            Intent intent = new Intent(v.getContext(), StudentProfileEditActivity.class);
            intent.putExtra(StudentProfileEditActivity.EXTRA_STUDENT_ID, s.id);
            v.getContext().startActivity(intent);
        });

        holder.btnDelete.setOnClickListener(v -> {
            if (deleteListener != null) deleteListener.onDelete(s);
        });
    }

    @Override
    public int getItemCount() { return students.size(); }

    private String resolveColorNames(String json) {
        if (json == null || json.equals("[]") || json.isEmpty()) return "Sin colores excluidos";
        try {
            JSONArray arr = new JSONArray(json);
            List<String> names = new ArrayList<>();
            for (int i = 0; i < arr.length(); i++) {
                String hex = arr.getString(i).toUpperCase();
                String name = COLOR_NAMES.get(hex);
                names.add(name != null ? name : hex);
            }
            return "Colores excluidos: " + String.join(", ", names);
        } catch (JSONException e) {
            return "Sin colores excluidos";
        }
    }

    private String resolveSoundName(String resName) {
        if (resName == null || resName.isEmpty()) return "Sin sonido de fondo";
        String name = SOUND_NAMES.get(resName);
        return "Sonido: " + (name != null ? name : resName);
    }

    static class StudentViewHolder extends RecyclerView.ViewHolder {
        TextView tvName, tvColors, tvSound;
        ImageButton btnEdit, btnDelete;

        StudentViewHolder(@NonNull View itemView) {
            super(itemView);
            tvName    = itemView.findViewById(R.id.student_name);
            tvColors  = itemView.findViewById(R.id.tvColors);
            tvSound   = itemView.findViewById(R.id.tvSound);
            btnEdit   = itemView.findViewById(R.id.btnEdit);
            btnDelete = itemView.findViewById(R.id.btnDelete);
        }
    }
}
