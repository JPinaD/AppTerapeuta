package com.example.appterapeuta.ui.activities;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.appterapeuta.R;
import com.example.appterapeuta.data.local.entity.TherapyActivityEntity;

import java.util.ArrayList;
import java.util.List;

public class ActivityCatalogAdapter extends RecyclerView.Adapter<ActivityCatalogAdapter.ActivityViewHolder> {

    private final List<TherapyActivityEntity> activities = new ArrayList<>();

    public void setActivities(List<TherapyActivityEntity> list) {
        activities.clear();
        if (list != null) activities.addAll(list);
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ActivityViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_activity_catalog, parent, false);
        return new ActivityViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ActivityViewHolder holder, int position) {
        TherapyActivityEntity activity = activities.get(position);
        holder.tvActivityName.setText(activity.name);
        holder.tvActivityDescription.setText(activity.description);
        holder.tvActivityDifficulty.setText("Dificultad: " + activity.difficulty);
    }

    @Override
    public int getItemCount() {
        return activities.size();
    }

    static class ActivityViewHolder extends RecyclerView.ViewHolder {
        TextView tvActivityName, tvActivityDescription, tvActivityDifficulty;

        ActivityViewHolder(@NonNull View itemView) {
            super(itemView);
            tvActivityName        = itemView.findViewById(R.id.tvActivityName);
            tvActivityDescription = itemView.findViewById(R.id.tvActivityDescription);
            tvActivityDifficulty  = itemView.findViewById(R.id.tvActivityDifficulty);
        }
    }
}
