package com.example.appterapeuta.ui.robots;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.appterapeuta.R;
import com.example.appterapeuta.data.model.DiscoveredRobot;

import java.util.List;

public class RobotAdapter extends RecyclerView.Adapter<RobotAdapter.RobotViewHolder> {

    public interface OnRobotClickListener {
        void onRobotClick(DiscoveredRobot robot);
    }

    private final List<DiscoveredRobot> robots;
    private final OnRobotClickListener clickListener;

    public RobotAdapter(List<DiscoveredRobot> robots, OnRobotClickListener clickListener) {
        this.robots = robots;
        this.clickListener = clickListener;
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
        holder.robotStatus.setText(R.string.robot_status_detected);
        holder.robotBattery.setText(robot.host + ":" + robot.port);
        holder.itemView.setOnClickListener(v -> clickListener.onRobotClick(robot));
    }

    @Override
    public int getItemCount() {
        return robots.size();
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
