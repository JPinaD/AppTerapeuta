package com.example.appterapeuta.ui.sessions;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.CheckBox;
import android.widget.Spinner;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.appterapeuta.R;
import com.example.appterapeuta.data.local.entity.StudentProfileEntity;
import com.example.appterapeuta.data.model.ConnectionState;
import com.example.appterapeuta.data.model.DiscoveredRobot;
import com.example.appterapeuta.data.model.RobotConnection;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

class SessionRobotAdapter extends RecyclerView.Adapter<SessionRobotAdapter.VH> {

    private List<DiscoveredRobot> robots = new ArrayList<>();
    private Map<String, RobotConnection> connections = new HashMap<>();
    private List<StudentProfileEntity> profiles = new ArrayList<>();

    // robotId → posición seleccionada en el spinner de alumnos
    private final Map<String, Integer> selectedProfilePos = new HashMap<>();

    void setRobots(List<DiscoveredRobot> robots, Map<String, RobotConnection> connections) {
        this.robots = robots != null ? robots : new ArrayList<>();
        this.connections = connections != null ? connections : new HashMap<>();
        notifyDataSetChanged();
    }

    void setConnections(Map<String, RobotConnection> connections) {
        this.connections = connections != null ? connections : new HashMap<>();
        notifyDataSetChanged();
    }

    void setProfiles(List<StudentProfileEntity> profiles) {
        this.profiles = profiles != null ? profiles : new ArrayList<>();
        notifyDataSetChanged();
    }

    private final List<String> checkedIds = new ArrayList<>();

    List<String> getSelectedRobotIds() {
        return new ArrayList<>(checkedIds);
    }

    Map<String, String> getRobotToProfileId(List<String> robotIds) {
        Map<String, String> result = new HashMap<>();
        for (String id : robotIds) {
            Integer pos = selectedProfilePos.get(id);
            if (pos != null && pos > 0 && pos - 1 < profiles.size()) {
                result.put(id, profiles.get(pos - 1).id);
            }
        }
        return result;
    }

    @NonNull
    @Override
    public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_session_robot, parent, false);
        return new VH(v);
    }

    @Override
    public void onBindViewHolder(@NonNull VH holder, int position) {
        DiscoveredRobot robot = robots.get(position);
        RobotConnection conn = connections.get(robot.robotId);
        boolean connected = conn != null && conn.state == ConnectionState.CONNECTED;

        holder.tvName.setText(robot.serviceName);
        holder.tvState.setText(connected ? "Conectado" : "No conectado");
        holder.cbRobot.setEnabled(connected);

        holder.cbRobot.setOnCheckedChangeListener(null);
        holder.cbRobot.setChecked(checkedIds.contains(robot.robotId));
        holder.cbRobot.setOnCheckedChangeListener((btn, checked) -> {
            if (checked) {
                if (!checkedIds.contains(robot.robotId)) checkedIds.add(robot.robotId);
                holder.layoutStudentPicker.setVisibility(View.VISIBLE);
            } else {
                checkedIds.remove(robot.robotId);
                holder.layoutStudentPicker.setVisibility(View.GONE);
            }
        });

        // Spinner de alumnos
        List<String> names = new ArrayList<>();
        names.add("Sin alumno");
        for (StudentProfileEntity p : profiles) names.add(p.name);
        ArrayAdapter<String> adapter = new ArrayAdapter<>(holder.itemView.getContext(),
                android.R.layout.simple_spinner_item, names);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        holder.spinnerStudent.setAdapter(adapter);

        Integer savedPos = selectedProfilePos.get(robot.robotId);
        holder.spinnerStudent.setSelection(savedPos != null ? savedPos : 0);
        holder.spinnerStudent.setOnItemSelectedListener(new android.widget.AdapterView.OnItemSelectedListener() {
            @Override public void onItemSelected(android.widget.AdapterView<?> p, View v, int pos, long id) {
                selectedProfilePos.put(robot.robotId, pos);
            }
            @Override public void onNothingSelected(android.widget.AdapterView<?> p) {}
        });

        holder.layoutStudentPicker.setVisibility(
                checkedIds.contains(robot.robotId) ? View.VISIBLE : View.GONE);
    }

    @Override
    public int getItemCount() { return robots.size(); }

    static class VH extends RecyclerView.ViewHolder {
        CheckBox cbRobot;
        TextView tvName, tvState;
        View layoutStudentPicker;
        Spinner spinnerStudent;

        VH(@NonNull View v) {
            super(v);
            cbRobot            = v.findViewById(R.id.cbRobot);
            tvName             = v.findViewById(R.id.tvRobotName);
            tvState            = v.findViewById(R.id.tvRobotState);
            layoutStudentPicker = v.findViewById(R.id.layoutStudentPicker);
            spinnerStudent     = v.findViewById(R.id.spinnerStudent);
        }
    }
}
