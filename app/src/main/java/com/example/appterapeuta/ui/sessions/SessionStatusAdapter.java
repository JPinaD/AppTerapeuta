package com.example.appterapeuta.ui.sessions;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.appterapeuta.R;
import com.example.appterapeuta.data.model.RobotLiveStatus;
import com.example.appterapeuta.data.model.RobotSessionState;
import com.example.appterapeuta.data.model.RobotSessionStatus;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

class SessionStatusAdapter extends RecyclerView.Adapter<SessionStatusAdapter.VH> {

    interface OnFeedbackClickListener {
        void onFeedbackClick(String robotId);
    }

    private List<RobotSessionStatus> statuses = new ArrayList<>();
    private Map<String, RobotLiveStatus> liveStatuses;
    private OnFeedbackClickListener feedbackListener;

    void setStatuses(List<RobotSessionStatus> list) {
        statuses = list != null ? list : new ArrayList<>();
        notifyDataSetChanged();
    }

    void setLiveStatuses(Map<String, RobotLiveStatus> live) {
        liveStatuses = live;
        notifyDataSetChanged();
    }

    void setFeedbackListener(OnFeedbackClickListener listener) {
        this.feedbackListener = listener;
    }

    @NonNull
    @Override
    public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_session_status, parent, false);
        return new VH(v);
    }

    @Override
    public void onBindViewHolder(@NonNull VH holder, int position) {
        RobotSessionStatus s = statuses.get(position);
        holder.tvName.setText(s.robotId);
        holder.tvState.setText(stateLabel(s.state));

        RobotLiveStatus live = liveStatuses != null ? liveStatuses.get(s.robotId) : null;
        if (live != null) {
            String bat  = live.batteryPct  != null ? live.batteryPct  + "%" : "—";
            String act  = live.activityId  != null ? live.activityId       : "—";
            String prog = live.progressPct != null ? live.progressPct + "%" : "—";
            holder.tvTelemetry.setText("BAT: " + bat + "  |  ACT: " + act + "  |  PROG: " + prog);
        } else {
            holder.tvTelemetry.setText("");
        }

        holder.btnFeedback.setOnClickListener(v -> {
            if (feedbackListener != null) feedbackListener.onFeedbackClick(s.robotId);
        });
    }

    @Override
    public int getItemCount() { return statuses.size(); }

    private String stateLabel(RobotSessionState state) {
        switch (state) {
            case WAITING:     return "⏳ Esperando";
            case READY:       return "✅ Listo";
            case IN_ACTIVITY: return "▶ En actividad";
            case PAUSED:      return "⏸ Pausado";
            case ENDED:       return "🏁 Finalizado";
            case ERROR:       return "❌ Error";
            default:          return state.name();
        }
    }

    static class VH extends RecyclerView.ViewHolder {
        TextView tvName, tvState, tvTelemetry;
        Button btnFeedback;
        VH(@NonNull View v) {
            super(v);
            tvName      = v.findViewById(R.id.tvRobotName);
            tvState     = v.findViewById(R.id.tvSessionState);
            tvTelemetry = v.findViewById(R.id.tvTelemetry);
            btnFeedback = v.findViewById(R.id.btnFeedback);
        }
    }
}
