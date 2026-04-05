package com.example.appterapeuta.ui.robots;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.appterapeuta.R;
import com.example.appterapeuta.data.model.DiscoveredRobot;
import com.example.appterapeuta.data.model.RobotConnection;
import com.example.appterapeuta.viewmodel.RobotViewModel;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import com.example.appterapeuta.ui.activity.PictogramActivityLauncherActivity;

public class RobotListActivity extends AppCompatActivity {

    private RobotViewModel viewModel;
    private RobotAdapter adapter;
    private final List<DiscoveredRobot> robotList = new ArrayList<>();
    private Map<String, RobotConnection> currentConnections;
    private TextView emptyView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_robot_list);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        emptyView = findViewById(R.id.empty_view);

        RecyclerView recyclerView = findViewById(R.id.robot_list);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        adapter = new RobotAdapter(robotList, this::onRobotSelected, () -> currentConnections);
        recyclerView.setAdapter(adapter);

        viewModel = new ViewModelProvider(this).get(RobotViewModel.class);
        viewModel.getDiscoveredRobots().observe(this, this::updateList);
        viewModel.getRobotConnections().observe(this, connections -> {
            currentConnections = connections;
            adapter.notifyDataSetChanged();
        });

        Button backButton = findViewById(R.id.back_button);
        backButton.setOnClickListener(v -> finish());
    }

    @Override
    protected void onStart() {
        super.onStart();
        viewModel.startDiscovery();
    }

    @Override
    protected void onStop() {
        super.onStop();
        viewModel.stopDiscovery();
    }

    private void updateList(List<DiscoveredRobot> robots) {
        robotList.clear();
        robotList.addAll(robots);
        adapter.notifyDataSetChanged();
        emptyView.setVisibility(robots.isEmpty() ? View.VISIBLE : View.GONE);
    }

    private void onRobotSelected(DiscoveredRobot robot) {
        RobotConnection conn = currentConnections != null ? currentConnections.get(robot.robotId) : null;
        if (conn != null && conn.state == com.example.appterapeuta.data.model.ConnectionState.CONNECTED) {
            Intent intent = new Intent(this, PictogramActivityLauncherActivity.class);
            intent.putExtra(PictogramActivityLauncherActivity.EXTRA_ROBOT_ID, robot.robotId);
            intent.putExtra(PictogramActivityLauncherActivity.EXTRA_ROBOT_NAME, robot.serviceName);
            startActivity(intent);
        } else {
            viewModel.connect(robot);
        }
    }
}
