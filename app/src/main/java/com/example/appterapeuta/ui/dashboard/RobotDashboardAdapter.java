package com.example.appterapeuta.ui.dashboard;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.appterapeuta.R;
import com.example.appterapeuta.data.local.entity.RobotConfigEntity;
import com.example.appterapeuta.data.model.RobotLiveStatus;
import com.google.android.material.card.MaterialCardView;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

class RobotDashboardAdapter extends RecyclerView.Adapter<RobotDashboardAdapter.VH> {

    interface OnRobotLongClickListener {
        void onLongClick(RobotConfigEntity robot);
    }

    private List<RobotConfigEntity> robots = new ArrayList<>();
    private Map<String, RobotLiveStatus> statuses;
    private final OnRobotLongClickListener longClickListener;

    RobotDashboardAdapter(OnRobotLongClickListener longClickListener) {
        this.longClickListener = longClickListener;
    }

    void setData(List<RobotConfigEntity> robots, Map<String, RobotLiveStatus> statuses) {
        this.robots = robots != null ? robots : new ArrayList<>();
        this.statuses = statuses;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_robot_dashboard, parent, false);
        return new VH(v);
    }

    @Override
    public void onBindViewHolder(@NonNull VH h, int position) {
        RobotConfigEntity robot = robots.get(position);
        RobotLiveStatus status = statuses != null ? statuses.get(robot.robotId) : null;
        boolean online = status != null && status.online;

        h.tvName.setText(robot.name);

        int bgColor = h.itemView.getContext().getColor(
                online ? R.color.hud_card_bg : R.color.hud_offline_bg);
        int borderColor = h.itemView.getContext().getColor(
                online ? R.color.hud_online_border : R.color.hud_offline_border);
        h.card.setCardBackgroundColor(bgColor);
        h.card.setStrokeColor(borderColor);

        if (online) {
            h.tvOnline.setText("● ONLINE");
            h.tvOnline.setTextColor(h.itemView.getContext().getColor(R.color.hud_success));
            h.tvDetails.setVisibility(View.VISIBLE);
            String bat = status.batteryPct != null ? status.batteryPct + "%" : "—";
            String act = status.activityId != null ? status.activityId : "Sin actividad";
            String prog = status.progressPct != null ? status.progressPct + "%" : "—";
            String student = status.assignedStudentName != null ? status.assignedStudentName : "Sin asignar";
            h.tvDetails.setText("BAT: " + bat + "  |  ACT: " + act + "  |  PROG: " + prog + "\nALUMNO: " + student);
        } else {
            h.tvOnline.setText("○ OFFLINE");
            h.tvOnline.setTextColor(h.itemView.getContext().getColor(R.color.hud_text_secondary));
            h.tvDetails.setVisibility(View.GONE);
        }

        h.itemView.setOnLongClickListener(v -> {
            longClickListener.onLongClick(robot);
            return true;
        });
    }

    @Override
    public int getItemCount() { return robots.size(); }

    static class VH extends RecyclerView.ViewHolder {
        MaterialCardView card;
        TextView tvName, tvOnline, tvDetails;
        VH(@NonNull View v) {
            super(v);
            card      = v.findViewById(R.id.cardRobot);
            tvName    = v.findViewById(R.id.tvRobotName);
            tvOnline  = v.findViewById(R.id.tvRobotOnline);
            tvDetails = v.findViewById(R.id.tvRobotDetails);
        }
    }
}
