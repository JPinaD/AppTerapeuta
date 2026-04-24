package com.example.appterapeuta;

import android.app.Application;

import androidx.lifecycle.ViewModelProvider;
import androidx.lifecycle.ViewModelStore;
import androidx.lifecycle.ViewModelStoreOwner;

import com.example.appterapeuta.viewmodel.RobotViewModel;
import com.example.appterapeuta.viewmodel.SessionViewModel;

/**
 * Application que expone ViewModels compartidos entre Activities.
 */
public class AppTerapeutaApp extends Application {

    private ViewModelStore viewModelStore;
    private RobotViewModel robotViewModel;
    private SessionViewModel sessionViewModel;

    @Override
    public void onCreate() {
        super.onCreate();
        viewModelStore = new ViewModelStore();
        ViewModelStoreOwner owner = () -> viewModelStore;
        ViewModelProvider.AndroidViewModelFactory factory =
                ViewModelProvider.AndroidViewModelFactory.getInstance(this);
        ViewModelProvider provider = new ViewModelProvider(owner, factory);
        robotViewModel   = provider.get(RobotViewModel.class);
        sessionViewModel = provider.get(SessionViewModel.class);
    }

    public RobotViewModel getRobotViewModel()     { return robotViewModel; }
    public SessionViewModel getSessionViewModel() { return sessionViewModel; }
}
