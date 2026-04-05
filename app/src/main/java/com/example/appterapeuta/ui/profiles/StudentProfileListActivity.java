package com.example.appterapeuta.ui.profiles;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.appterapeuta.R;
import com.example.appterapeuta.viewmodel.StudentProfileViewModel;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

public class StudentProfileListActivity extends AppCompatActivity {

    private StudentProfileAdapter adapter;
    private boolean editMode = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_student_profile_list);

        RecyclerView recyclerView = findViewById(R.id.student_list);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        adapter = new StudentProfileAdapter();
        recyclerView.setAdapter(adapter);

        StudentProfileViewModel viewModel = new ViewModelProvider(this).get(StudentProfileViewModel.class);
        viewModel.profiles.observe(this, adapter::setStudents);

        adapter.setOnDeleteListener(profile -> new AlertDialog.Builder(this)
                .setTitle("Eliminar perfil")
                .setMessage("¿Eliminar el perfil de " + profile.name + "?")
                .setPositiveButton("Eliminar", (d, w) -> {
                    viewModel.deleteById(profile.id);
                    Toast.makeText(this, "Perfil eliminado", Toast.LENGTH_SHORT).show();
                })
                .setNegativeButton("Cancelar", null)
                .show());

        findViewById(R.id.btnToggleEdit).setOnClickListener(v -> {
            editMode = !editMode;
            adapter.setEditMode(editMode);
        });

        FloatingActionButton fab = findViewById(R.id.fabAddStudent);
        fab.setOnClickListener(v ->
                startActivity(new Intent(this, StudentProfileEditActivity.class)));

        findViewById(R.id.back_button).setOnClickListener(v -> finish());
    }
}
