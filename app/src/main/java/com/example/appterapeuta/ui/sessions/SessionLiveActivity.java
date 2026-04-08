package com.example.appterapeuta.ui.sessions;

import android.os.Bundle;
import android.widget.Button;

import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.appterapeuta.R;
import com.example.appterapeuta.data.model.RobotSessionStatus;
import com.example.appterapeuta.viewmodel.RobotViewModel;
import com.example.appterapeuta.viewmodel.SessionViewModel;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class SessionLiveActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_session_live);

        RobotViewModel robotViewModel     = new ViewModelProvider(this).get(RobotViewModel.class);
        SessionViewModel sessionViewModel = new ViewModelProvider(this).get(SessionViewModel.class);

        RecyclerView rv = findViewById(R.id.rvRobotStatuses);
        rv.setLayoutManager(new LinearLayoutManager(this));
        SessionStatusAdapter adapter = new SessionStatusAdapter();
        rv.setAdapter(adapter);

        sessionViewModel.getRobotStatuses().observe(this, statuses -> {
            if (statuses == null) return;
            List<RobotSessionStatus> list = new ArrayList<>(statuses.values());
            adapter.setStatuses(list);
        });

        // Actualizar estados al recibir eventos de red
        robotViewModel.getActivityEvents().observe(this, event ->
                sessionViewModel.handleIncomingEvent(event));

        Button btnEnd = findViewById(R.id.btnEndSession);
        btnEnd.setOnClickListener(v -> {
            sessionViewModel.endSession(robotViewModel);
            finish();
        });
    }
}
