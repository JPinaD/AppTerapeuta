package com.example.appterapeuta.ui.history;

import android.os.Bundle;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;

import com.example.appterapeuta.R;
import com.example.appterapeuta.data.local.entity.ActivityResultEntity;
import com.example.appterapeuta.data.local.entity.AlumnResultEntity;
import com.example.appterapeuta.data.local.entity.IncidentEntity;
import com.example.appterapeuta.data.local.entity.SessionRecordEntity;
import com.example.appterapeuta.viewmodel.SessionHistoryViewModel;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class SessionResultDetailActivity extends AppCompatActivity {

    private static final SimpleDateFormat SDF = new SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault());

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_session_result_detail);

        String sessionId = getIntent().getStringExtra("session_id");
        if (sessionId == null) { finish(); return; }

        SessionHistoryViewModel vm = new ViewModelProvider(this).get(SessionHistoryViewModel.class);
        vm.getDetail().observe(this, this::bindDetail);
        vm.loadDetail(sessionId);

        findViewById(R.id.back_button).setOnClickListener(v -> finish());
    }

    private void bindDetail(SessionHistoryViewModel.SessionDetail detail) {
        if (detail == null || detail.session == null) return;

        SessionRecordEntity s = detail.session;
        long durationMin = (s.endTimestamp - s.startTimestamp) / 60000;
        TextView tvHeader = findViewById(R.id.tvSessionHeader);
        tvHeader.setText(
                "Inicio: " + SDF.format(new Date(s.startTimestamp)) + "\n" +
                "Fin:    " + SDF.format(new Date(s.endTimestamp)) + "\n" +
                "Duración: " + durationMin + " min\n" +
                "Robots: " + (s.robotIdsJson != null ? s.robotIdsJson : "—")
        );

        // Actividades
        LinearLayout layoutActivities = findViewById(R.id.layoutActivities);
        layoutActivities.removeAllViews();
        if (detail.activities != null) {
            for (ActivityResultEntity ar : detail.activities) {
                addActivitySection(layoutActivities, ar, detail.alumnResults);
            }
        }

        // Incidencias
        LinearLayout layoutIncidents = findViewById(R.id.layoutIncidents);
        TextView tvIncidentsHeader = findViewById(R.id.tvIncidentsHeader);
        layoutIncidents.removeAllViews();
        if (detail.incidents != null && !detail.incidents.isEmpty()) {
            tvIncidentsHeader.setVisibility(View.VISIBLE);
            for (IncidentEntity inc : detail.incidents) {
                TextView tv = makeTextView(
                        "Robot: " + inc.robotId + " | " +
                        SDF.format(new Date(inc.timestamp)) + "\n" +
                        "Motivo: " + (inc.reason != null ? inc.reason : "—")
                );
                layoutIncidents.addView(tv);
            }
        }
    }

    private void addActivitySection(LinearLayout parent, ActivityResultEntity ar,
                                    List<AlumnResultEntity> alumnResults) {
        // Cabecera actividad (expandible al pulsar)
        TextView tvActivity = makeTextView("▶ Actividad: " + ar.activityId);
        tvActivity.setTextColor(getColor(R.color.hud_accent_cyan));

        LinearLayout layoutAlumns = new LinearLayout(this);
        layoutAlumns.setOrientation(LinearLayout.VERTICAL);
        layoutAlumns.setVisibility(View.GONE);

        if (alumnResults != null) {
            for (AlumnResultEntity alumn : alumnResults) {
                if (alumn.activityResultId == ar.id) {
                    layoutAlumns.addView(makeTextView(
                            "  " + alumn.studentName +
                            " | Intentos: " + alumn.attempts +
                            " | Aciertos: " + alumn.successes +
                            " | T.medio: " + alumn.avgResponseTimeMs + "ms" +
                            (alumn.finalPictogramId != null ? " | Final: " + alumn.finalPictogramId : "")
                    ));
                }
            }
        }

        tvActivity.setOnClickListener(v ->
                layoutAlumns.setVisibility(
                        layoutAlumns.getVisibility() == View.GONE ? View.VISIBLE : View.GONE));

        parent.addView(tvActivity);
        parent.addView(layoutAlumns);
    }

    private TextView makeTextView(String text) {
        TextView tv = new TextView(this);
        tv.setText(text);
        tv.setTextColor(getColor(R.color.hud_text_secondary));
        tv.setTypeface(android.graphics.Typeface.MONOSPACE);
        tv.setTextSize(12f);
        int pad = (int) (6 * getResources().getDisplayMetrics().density);
        tv.setPadding(0, pad, 0, pad);
        return tv;
    }
}
