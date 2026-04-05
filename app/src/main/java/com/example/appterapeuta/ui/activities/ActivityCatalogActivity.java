package com.example.appterapeuta.ui.activities;

import android.os.Bundle;
import android.widget.Button;

import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.appterapeuta.R;
import com.example.appterapeuta.viewmodel.ActivityCatalogViewModel;

public class ActivityCatalogActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_catalog);

        RecyclerView rvActivities = findViewById(R.id.rvActivities);
        rvActivities.setLayoutManager(new LinearLayoutManager(this));
        ActivityCatalogAdapter adapter = new ActivityCatalogAdapter();
        rvActivities.setAdapter(adapter);

        ActivityCatalogViewModel viewModel = new ViewModelProvider(this).get(ActivityCatalogViewModel.class);
        viewModel.activities.observe(this, adapter::setActivities);

        Button backButton = findViewById(R.id.back_button);
        backButton.setOnClickListener(v -> finish());
    }
}
