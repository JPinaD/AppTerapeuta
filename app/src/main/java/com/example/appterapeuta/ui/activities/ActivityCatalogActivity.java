package com.example.appterapeuta.ui.activities;

import android.os.Bundle;
import android.widget.Button;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.appterapeuta.R;
import com.example.appterapeuta.data.model.TherapyActivity;
import com.example.appterapeuta.util.MockDataProvider;

import java.util.List;

public class ActivityCatalogActivity extends AppCompatActivity {

    private RecyclerView rvActivities;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_catalog);

        rvActivities = findViewById(R.id.rvActivities);

        List<TherapyActivity> activities = MockDataProvider.getMockActivities();

        rvActivities.setLayoutManager(new LinearLayoutManager(this));
        rvActivities.setAdapter(new ActivityCatalogAdapter(activities));

        Button backButton = findViewById(R.id.back_button);
        backButton.setOnClickListener(v -> finish());
    }
}