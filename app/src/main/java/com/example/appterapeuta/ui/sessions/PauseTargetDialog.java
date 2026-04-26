package com.example.appterapeuta.ui.sessions;

import android.app.Dialog;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.CheckBox;
import android.widget.LinearLayout;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.DialogFragment;

import java.util.ArrayList;
import java.util.List;

/**
 * Diálogo para seleccionar qué robots pausar.
 * Devuelve la lista de robotIds seleccionados al Activity vía OnRobotsPausedListener.
 */
public class PauseTargetDialog extends DialogFragment {

    public interface OnRobotsPausedListener {
        void onPause(List<String> robotIds);
    }

    private static final String ARG_ROBOT_IDS = "robot_ids";

    public static PauseTargetDialog newInstance(List<String> robotIds) {
        PauseTargetDialog d = new PauseTargetDialog();
        Bundle args = new Bundle();
        args.putStringArrayList(ARG_ROBOT_IDS, new ArrayList<>(robotIds));
        d.setArguments(args);
        return d;
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        List<String> robotIds = getArguments() != null
                ? getArguments().getStringArrayList(ARG_ROBOT_IDS)
                : new ArrayList<>();
        if (robotIds == null) robotIds = new ArrayList<>();

        LinearLayout layout = new LinearLayout(requireContext());
        layout.setOrientation(LinearLayout.VERTICAL);
        int pad = (int) (16 * getResources().getDisplayMetrics().density);
        layout.setPadding(pad, pad, pad, 0);

        List<CheckBox> checkBoxes = new ArrayList<>();
        for (String id : robotIds) {
            CheckBox cb = new CheckBox(requireContext());
            cb.setText(id);
            cb.setChecked(true);
            layout.addView(cb);
            checkBoxes.add(cb);
        }

        final List<String> finalRobotIds = robotIds;
        return new AlertDialog.Builder(requireContext())
                .setTitle("⏸ Parada de emergencia")
                .setView(layout)
                .setPositiveButton("Pausar seleccionados", (d, w) -> {
                    List<String> selected = new ArrayList<>();
                    for (int i = 0; i < checkBoxes.size(); i++) {
                        if (checkBoxes.get(i).isChecked()) selected.add(finalRobotIds.get(i));
                    }
                    notifyListener(selected);
                })
                .setNeutralButton("Pausar todos", (d, w) -> notifyListener(finalRobotIds))
                .setNegativeButton("Cancelar", null)
                .create();
    }

    private void notifyListener(List<String> ids) {
        if (ids.isEmpty()) return;
        if (getActivity() instanceof OnRobotsPausedListener) {
            ((OnRobotsPausedListener) getActivity()).onPause(ids);
        }
    }
}
