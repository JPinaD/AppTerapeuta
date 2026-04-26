package com.example.appterapeuta.ui.history;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.appterapeuta.R;
import com.example.appterapeuta.data.local.entity.SessionRecordEntity;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

class SessionHistoryAdapter extends RecyclerView.Adapter<SessionHistoryAdapter.VH> {

    interface OnSessionClickListener {
        void onClick(String sessionId);
    }

    private List<SessionRecordEntity> sessions = new ArrayList<>();
    private final OnSessionClickListener listener;
    private static final SimpleDateFormat SDF = new SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault());

    SessionHistoryAdapter(OnSessionClickListener listener) {
        this.listener = listener;
    }

    void setSessions(List<SessionRecordEntity> list) {
        sessions = list != null ? list : new ArrayList<>();
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_session_history, parent, false);
        return new VH(v);
    }

    @Override
    public void onBindViewHolder(@NonNull VH h, int position) {
        SessionRecordEntity s = sessions.get(position);
        h.tvDate.setText(SDF.format(new Date(s.startTimestamp)));
        long durationMin = (s.endTimestamp - s.startTimestamp) / 60000;
        h.tvInfo.setText("Duración: " + durationMin + " min");
        h.itemView.setOnClickListener(v -> listener.onClick(s.sessionId));
    }

    @Override
    public int getItemCount() { return sessions.size(); }

    static class VH extends RecyclerView.ViewHolder {
        TextView tvDate, tvInfo;
        VH(@NonNull View v) {
            super(v);
            tvDate = v.findViewById(R.id.tvSessionDate);
            tvInfo = v.findViewById(R.id.tvSessionInfo);
        }
    }
}
