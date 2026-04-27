package com.example.appterapeuta;

/**
 * Singleton en memoria que guarda el estado de la sesión del terapeuta autenticado.
 * Se pierde al cerrar el proceso (comportamiento correcto: requiere login de nuevo).
 */
public class SessionManager {

    private static SessionManager instance;

    private String currentUsername;
    private boolean root;

    private SessionManager() {}

    public static SessionManager getInstance() {
        if (instance == null) instance = new SessionManager();
        return instance;
    }

    public void login(String username, boolean isRoot) {
        this.currentUsername = username;
        this.root = isRoot;
    }

    public void logout() {
        this.currentUsername = null;
        this.root = false;
    }

    public boolean isRoot() { return root; }

    public String getCurrentUsername() { return currentUsername; }
}
