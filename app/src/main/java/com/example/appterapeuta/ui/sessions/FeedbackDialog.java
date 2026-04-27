package com.example.appterapeuta.ui.sessions;

import android.app.Dialog;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.EditText;
import android.widget.RadioButton;
import android.widget.RadioGroup;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.DialogFragment;

import com.example.appterapeuta.R;

/**
 * Diálogo para enviar un mensaje de refuerzo positivo a un robot concreto.
 */
public class FeedbackDialog extends DialogFragment {

    public interface OnFeedbackSentListener {
        void onFeedbackSent(String robotId, String message);
    }

    private static final String ARG_ROBOT_ID = "robot_id";
    private static final String[] PRESET_MESSAGES = {
            "¡Muy bien!", "¡Genial!", "¡Sigue así!", "¡Casi!", "Inténtalo de nuevo"
    };

    public static FeedbackDialog newInstance(String robotId) {
        FeedbackDialog d = new FeedbackDialog();
        Bundle args = new Bundle();
        args.putString(ARG_ROBOT_ID, robotId);
        d.setArguments(args);
        return d;
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        String robotId = getArguments() != null ? getArguments().getString(ARG_ROBOT_ID, "") : "";

        View view = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_feedback, null);
        RadioGroup radioGroup = view.findViewById(R.id.radioGroupFeedback);
        EditText etCustom = view.findViewById(R.id.etCustomFeedback);

        // Añadir opciones predefinidas
        for (int i = 0; i < PRESET_MESSAGES.length; i++) {
            RadioButton rb = new RadioButton(requireContext());
            rb.setId(i);
            rb.setText(PRESET_MESSAGES[i]);
            rb.setTextSize(16f);
            int pad = (int) (8 * getResources().getDisplayMetrics().density);
            rb.setPadding(pad, pad, pad, pad);
            radioGroup.addView(rb);
        }
        radioGroup.check(0); // seleccionar el primero por defecto

        radioGroup.setOnCheckedChangeListener((group, checkedId) ->
                etCustom.setEnabled(checkedId < 0));

        return new AlertDialog.Builder(requireContext())
                .setTitle("Enviar feedback — " + robotId)
                .setView(view)
                .setPositiveButton("Enviar", (d, w) -> {
                    String message;
                    int checked = radioGroup.getCheckedRadioButtonId();
                    String custom = etCustom.getText().toString().trim();
                    if (!custom.isEmpty()) {
                        message = custom;
                    } else if (checked >= 0 && checked < PRESET_MESSAGES.length) {
                        message = PRESET_MESSAGES[checked];
                    } else {
                        return;
                    }
                    if (getActivity() instanceof OnFeedbackSentListener) {
                        ((OnFeedbackSentListener) getActivity()).onFeedbackSent(robotId, message);
                    }
                })
                .setNegativeButton("Cancelar", null)
                .create();
    }
}
