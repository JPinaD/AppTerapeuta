// RobotAdapter.java
package com.example.appterapeuta.ui.robots;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.appterapeuta.R;
import com.example.appterapeuta.data.model.Robot;

import java.util.List;

public class RobotAdapter extends RecyclerView.Adapter<RobotAdapter.RobotViewHolder> {
    private final List<Robot> robots;

    public RobotAdapter(List<Robot> robots) {
        this.robots = robots;
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
        Robot robot = robots.get(position);
        holder.robotName.setText(robot.name);
        holder.robotMac.setText(robot.macAddress);
        holder.robotStatus.setText(robot.isConnected ? "Conectado" : "Desconectado");
        holder.robotBattery.setText(
            holder.itemView.getContext().getString(R.string.robot_battery, robot.batteryLevel)
        );
    }

    @Override
    public int getItemCount() {
        return robots.size();
    }

    public static class RobotViewHolder extends RecyclerView.ViewHolder {
        TextView robotName;
        TextView robotMac;
        TextView robotStatus;
        TextView robotBattery;
        RobotViewHolder(View itemView) {
            super(itemView);
            robotName = itemView.findViewById(R.id.robot_name);
            robotMac = itemView.findViewById(R.id.robot_mac);
            robotStatus = itemView.findViewById(R.id.robot_status);
            robotBattery = itemView.findViewById(R.id.robot_battery);
        }
    }
}
