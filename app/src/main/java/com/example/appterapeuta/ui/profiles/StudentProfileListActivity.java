package com.example.appterapeuta.ui.profiles;

import android.os.Bundle;
import android.widget.Button;

import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.appterapeuta.R;
import com.example.appterapeuta.viewmodel.StudentProfileViewModel;

public class StudentProfileListActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_student_profile_list);

        RecyclerView recyclerView = findViewById(R.id.student_list);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        StudentProfileAdapter adapter = new StudentProfileAdapter();
        recyclerView.setAdapter(adapter);

        StudentProfileViewModel viewModel = new ViewModelProvider(this).get(StudentProfileViewModel.class);
        viewModel.profiles.observe(this, adapter::setStudents);

        Button backButton = findViewById(R.id.back_button);
        backButton.setOnClickListener(v -> finish());
    }
}
