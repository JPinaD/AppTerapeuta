package com.example.appterapeuta.ui.therapists;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.appterapeuta.R;
import com.example.appterapeuta.data.local.entity.TherapistEntity;

import java.util.ArrayList;
import java.util.List;

class TherapistAdapter extends RecyclerView.Adapter<TherapistAdapter.VH> {

    interface Listener {
        void onChangePassword(TherapistEntity t);
        void onDelete(TherapistEntity t);
    }

    private List<TherapistEntity> items = new ArrayList<>();
    private final Listener listener;

    TherapistAdapter(Listener listener) { this.listener = listener; }

    void setItems(List<TherapistEntity> list) {
        items = list != null ? list : new ArrayList<>();
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_therapist, parent, false);
        return new VH(v);
    }

    @Override
    public void onBindViewHolder(@NonNull VH h, int position) {
        TherapistEntity t = items.get(position);
        h.tvName.setText(t.displayName);
        h.tvUsername.setText("@" + t.username);
        h.btnChangePassword.setOnClickListener(v -> listener.onChangePassword(t));
        h.btnDelete.setOnClickListener(v -> listener.onDelete(t));
    }

    @Override
    public int getItemCount() { return items.size(); }

    static class VH extends RecyclerView.ViewHolder {
        TextView tvName, tvUsername;
        Button btnChangePassword, btnDelete;
        VH(@NonNull View v) {
            super(v);
            tvName           = v.findViewById(R.id.tvDisplayName);
            tvUsername       = v.findViewById(R.id.tvUsername);
            btnChangePassword = v.findViewById(R.id.btnChangePassword);
            btnDelete        = v.findViewById(R.id.btnDelete);
        }
    }
}
