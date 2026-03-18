// RobotAdapter.java
package com.example.appterapeuta.ui.robots;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.appterapeuta.R;

import java.util.List;

public class RobotAdapter extends RecyclerView.Adapter<RobotAdapter.RobotViewHolder> {
    private final List<String> robots;

    public RobotAdapter(List<String> robots) {
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
        holder.robotName.setText(robots.get(position));
    }

    @Override
    public int getItemCount() {
        return robots.size();
    }

    static class RobotViewHolder extends RecyclerView.ViewHolder {
        TextView robotName;
        RobotViewHolder(View itemView) {
            super(itemView);
            robotName = itemView.findViewById(R.id.robot_name);
        }
    }
}

