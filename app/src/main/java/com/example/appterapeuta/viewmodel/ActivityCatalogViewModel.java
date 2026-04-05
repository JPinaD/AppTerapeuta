package com.example.appterapeuta.viewmodel;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;

import com.example.appterapeuta.data.local.entity.TherapyActivityEntity;
import com.example.appterapeuta.data.repository.TherapyActivityRepository;

import java.util.List;

public class ActivityCatalogViewModel extends AndroidViewModel {

    private final TherapyActivityRepository repository;
    public final LiveData<List<TherapyActivityEntity>> activities;

    public ActivityCatalogViewModel(@NonNull Application application) {
        super(application);
        repository = new TherapyActivityRepository(application);
        activities = repository.getAll();
    }

    public void insert(TherapyActivityEntity activity) {
        repository.insert(activity);
    }
}
