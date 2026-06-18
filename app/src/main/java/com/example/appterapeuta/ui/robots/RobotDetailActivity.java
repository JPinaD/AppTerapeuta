package com.example.appterapeuta.ui.robots;

import android.os.Bundle;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.example.appterapeuta.AppTerapeutaApp;
import com.example.appterapeuta.R;
import com.example.appterapeuta.data.model.RobotLiveStatus;

import java.util.Map;

public class RobotDetailActivity extends AppCompatActivity {

    public static final String EXTRA_ROBOT_ID   = "robot_id";
    public static final String EXTRA_ROBOT_NAME = "robot_name";
    public static final String EXTRA_ROBOT_HOST = "robot_host";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_robot_detail);

        String robotId   = getIntent().getStringExtra(EXTRA_ROBOT_ID);
        String robotName = getIntent().getStringExtra(EXTRA_ROBOT_NAME);
        String robotHost = getIntent().getStringExtra(EXTRA_ROBOT_HOST);

        ((TextView) findViewById(R.id.tvRobotName)).setText(robotName != null ? robotName : "—");
        ((TextView) findViewById(R.id.tvRobotId)).setText("ID: " + (robotId != null ? robotId : "—"));
        ((TextView) findViewById(R.id.tvRobotHost)).setText("Host: " + (robotHost != null ? robotHost : "—"));

        Map<String, RobotLiveStatus> statuses =
                ((AppTerapeutaApp) getApplication()).getControlCenterViewModel()
                        .getLiveStatuses().getValue();
        RobotLiveStatus live = statuses != null && robotId != null ? statuses.get(robotId) : null;
        TextView tvStatus = findViewById(R.id.tvRobotStatus);
        if (live != null && live.online) {
            String bat  = live.batteryPct  != null ? live.batteryPct  + "%" : "—";
            String act  = live.activityId  != null ? live.activityId       : "Sin actividad";
            String prog = live.progressPct != null ? live.progressPct + "%" : "—";
            tvStatus.setText("● ONLINE\nBAT: " + bat + "  |  ACT: " + act + "  |  PROG: " + prog);
            tvStatus.setTextColor(getColor(R.color.hud_success));
        } else {
            tvStatus.setText("○ OFFLINE");
            tvStatus.setTextColor(getColor(R.color.hud_text_secondary));
        }

        findViewById(R.id.back_button).setOnClickListener(v -> finish());
    }
}
