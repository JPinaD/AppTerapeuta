package com.example.appterapeuta.ui.sessions;

import android.app.Dialog;
import android.os.Bundle;
import android.widget.EditText;
import android.widget.LinearLayout;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.DialogFragment;

/**
 * Diálogo para introducir el motivo de una suspensión forzosa.
 * Si se confirma sin texto, la suspensión se ejecuta igualmente sin registrar incidencia.
 */
public class IncidentReasonDialog extends DialogFragment {

    public interface OnIncidentConfirmedListener {
        /** reason puede ser null o vacío si el terapeuta no introdujo texto. */
        void onIncidentConfirmed(String robotId, String reason);
    }

    private static final String ARG_ROBOT_ID = "robot_id";

    public static IncidentReasonDialog newInstance(String robotId) {
        IncidentReasonDialog d = new IncidentReasonDialog();
        Bundle args = new Bundle();
        args.putString(ARG_ROBOT_ID, robotId);
        d.setArguments(args);
        return d;
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        String robotId = getArguments() != null ? getArguments().getString(ARG_ROBOT_ID, "") : "";

        LinearLayout layout = new LinearLayout(requireContext());
        layout.setOrientation(LinearLayout.VERTICAL);
        int pad = (int) (16 * getResources().getDisplayMetrics().density);
        layout.setPadding(pad, pad, pad, 0);

        EditText etReason = new EditText(requireContext());
        etReason.setHint("Motivo de la suspensión (opcional)");
        etReason.setMinLines(2);
        layout.addView(etReason);

        return new AlertDialog.Builder(requireContext())
                .setTitle("⏹ Suspender robot: " + robotId)
                .setView(layout)
                .setPositiveButton("Confirmar suspensión", (d, w) -> {
                    String reason = etReason.getText().toString().trim();
                    notifyListener(robotId, reason.isEmpty() ? null : reason);
                })
                .setNegativeButton("Cancelar", null)
                .create();
    }

    private void notifyListener(String robotId, String reason) {
        if (getActivity() instanceof OnIncidentConfirmedListener) {
            ((OnIncidentConfirmedListener) getActivity()).onIncidentConfirmed(robotId, reason);
        }
    }
}
