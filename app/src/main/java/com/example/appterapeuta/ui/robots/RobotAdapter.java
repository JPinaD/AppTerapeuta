package com.example.appterapeuta.ui.robots;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.appterapeuta.R;
import com.example.appterapeuta.data.model.ConnectionState;
import com.example.appterapeuta.data.model.DiscoveredRobot;
import com.example.appterapeuta.data.model.RobotConnection;

import java.util.List;
import java.util.Map;

public class RobotAdapter extends RecyclerView.Adapter<RobotAdapter.RobotViewHolder> {

    public interface OnRobotClickListener {
        void onRobotClick(DiscoveredRobot robot);
    }

    public interface ConnectionsProvider {
        Map<String, RobotConnection> getConnections();
    }

    private final List<DiscoveredRobot> robots;
    private final OnRobotClickListener clickListener;
    private final ConnectionsProvider connectionsProvider;

    public RobotAdapter(List<DiscoveredRobot> robots, OnRobotClickListener clickListener,
                        ConnectionsProvider connectionsProvider) {
        this.robots = robots;
        this.clickListener = clickListener;
        this.connectionsProvider = connectionsProvider;
    }

    @NonNull
    @Override
    public RobotViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_robot, parent, false);
        return new RobotViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull RobotViewHolder holder, int position) {
        DiscoveredRobot robot = robots.get(position);
        holder.robotName.setText(robot.serviceName);
        holder.robotBattery.setText(robot.host + ":" + robot.port);

        Map<String, RobotConnection> connections = connectionsProvider.getConnections();
        RobotConnection conn = connections != null ? connections.get(robot.robotId) : null;
        holder.robotStatus.setText(stateLabel(conn));

        holder.itemView.setOnClickListener(v -> clickListener.onRobotClick(robot));
    }

    @Override
    public int getItemCount() {
        return robots.size();
    }

    private String stateLabel(RobotConnection conn) {
        if (conn == null) return "Detectado";
        switch (conn.state) {
            case CONNECTING:    return "Conectando\u2026";
            case CONNECTED:     return "Conectado";
            case ERROR:         return "Error";
            case DISCONNECTED:
            default:            return "Desconectado";
        }
    }

    public static class RobotViewHolder extends RecyclerView.ViewHolder {
        TextView robotName, robotStatus, robotBattery;

        RobotViewHolder(View itemView) {
            super(itemView);
            robotName    = itemView.findViewById(R.id.robot_name);
            robotStatus  = itemView.findViewById(R.id.robot_status);
            robotBattery = itemView.findViewById(R.id.robot_battery);
        }
    }
}
