package com.example.appterapeuta.viewmodel;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.example.appterapeuta.data.repository.TherapistRepository;

import java.util.concurrent.Executors;

public class LoginViewModel extends AndroidViewModel {

    public enum LoginResult { SUCCESS, SUCCESS_ROOT, WRONG_CREDENTIALS }

    private final MutableLiveData<LoginResult> loginResult = new MutableLiveData<>();
    private final TherapistRepository repository;

    public LoginViewModel(@NonNull Application application) {
        super(application);
        repository = new TherapistRepository(application);
    }

    public LiveData<LoginResult> getLoginResult() { return loginResult; }

    public void login(String username, String password) {
        if (username.isEmpty() || password.isEmpty()) {
            loginResult.setValue(LoginResult.WRONG_CREDENTIALS);
            return;
        }
        Executors.newSingleThreadExecutor().execute(() -> {
            boolean ok = repository.login(username, password);
            if (!ok) { loginResult.postValue(LoginResult.WRONG_CREDENTIALS); return; }
            loginResult.postValue(repository.isRoot(username)
                    ? LoginResult.SUCCESS_ROOT : LoginResult.SUCCESS);
        });
    }
}
