package com.example.appterapeuta.viewmodel;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;

import com.example.appterapeuta.data.local.entity.StudentProfileEntity;
import com.example.appterapeuta.data.repository.StudentProfileRepository;

import java.util.List;

public class StudentProfileViewModel extends AndroidViewModel {

    private final StudentProfileRepository repository;
    public final LiveData<List<StudentProfileEntity>> profiles;

    public StudentProfileViewModel(@NonNull Application application) {
        super(application);
        repository = new StudentProfileRepository(application);
        profiles = repository.getAll();
    }

    public void insert(StudentProfileEntity profile) {
        repository.insert(profile);
    }

    public void update(StudentProfileEntity profile) {
        repository.update(profile);
    }

    public void deleteById(String id) {
        repository.deleteById(id);
    }
}
