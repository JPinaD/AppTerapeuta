package com.example.appterapeuta.data.repository;

import android.content.Context;

import androidx.lifecycle.LiveData;

import com.example.appterapeuta.data.local.db.AppDatabase;
import com.example.appterapeuta.data.local.entity.TherapistEntity;
import com.example.appterapeuta.util.HashUtils;

import java.util.List;
import java.util.concurrent.Executors;

public class TherapistRepository {

    /** Usuario root con acceso maestro. Contraseña: root1234 */
    public static final String ROOT_USERNAME = "root";
    private static final String ROOT_PASSWORD = "root1234";

    private final AppDatabase db;

    public TherapistRepository(Context context) {
        db = AppDatabase.getInstance(context);
    }

    /** Devuelve true si las credenciales son correctas (incluye root). Llamar en hilo de fondo. */
    public boolean login(String username, String password) {
        if (ROOT_USERNAME.equals(username)) {
            return ROOT_PASSWORD.equals(password);
        }
        TherapistEntity therapist = db.therapistDao().getByUsername(username);
        if (therapist == null) return false;
        return therapist.passwordHash.equals(HashUtils.sha256(password));
    }

    /** Devuelve true si el usuario es root. */
    public boolean isRoot(String username) {
        return ROOT_USERNAME.equals(username);
    }

    public LiveData<List<TherapistEntity>> getAll() {
        return db.therapistDao().getAll();
    }

    public void insert(String username, String displayName, String password) {
        TherapistEntity t = new TherapistEntity(username, HashUtils.sha256(password), displayName);
        Executors.newSingleThreadExecutor().execute(() -> db.therapistDao().insert(t));
    }

    public void changePassword(String username, String newPassword) {
        Executors.newSingleThreadExecutor().execute(() ->
                db.therapistDao().updatePassword(username, HashUtils.sha256(newPassword)));
    }

    public void delete(TherapistEntity therapist) {
        Executors.newSingleThreadExecutor().execute(() -> db.therapistDao().delete(therapist));
    }
}
