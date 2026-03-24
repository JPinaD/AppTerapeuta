package com.example.appterapeuta.ui.activities;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.appterapeuta.R;
import com.example.appterapeuta.data.model.TherapyActivity;

import java.util.List;

public class ActivityCatalogAdapter extends RecyclerView.Adapter<ActivityCatalogAdapter.ActivityViewHolder> {

    private final List<TherapyActivity> activities;

    public ActivityCatalogAdapter(List<TherapyActivity> activities) {
        this.activities = activities;
    }

    @NonNull
    @Override
    public ActivityViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_activity_catalog, parent, false);
        return new ActivityViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ActivityViewHolder holder, int position) {
        TherapyActivity activity = activities.get(position);

        holder.tvActivityName.setText(activity.name);
        holder.tvActivityDescription.setText(activity.description);
        holder.tvActivityDifficulty.setText("Dificultad: " + activity.difficulty);

        holder.itemView.setOnClickListener(v ->
                Toast.makeText(v.getContext(), "Actividad: " + activity.name, Toast.LENGTH_SHORT).show()
        );
    }

    @Override
    public int getItemCount() {
        return activities.size();
    }

    static class ActivityViewHolder extends RecyclerView.ViewHolder {
        TextView tvActivityName, tvActivityDescription, tvActivityDifficulty;

        public ActivityViewHolder(@NonNull View itemView) {
            super(itemView);
            tvActivityName = itemView.findViewById(R.id.tvActivityName);
            tvActivityDescription = itemView.findViewById(R.id.tvActivityDescription);
            tvActivityDifficulty = itemView.findViewById(R.id.tvActivityDifficulty);
        }
    }
}