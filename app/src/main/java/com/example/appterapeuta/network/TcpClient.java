package com.example.appterapeuta.network;

import android.util.Log;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Cliente TCP para conectarse a un robot descubierto por NSD.
 * Permite enviar mensajes y recibir respuestas (ej. PING/PONG).
 */
public class TcpClient {

    public interface ConnectionListener {
        void onConnected();
        void onMessage(String message);
        void onDisconnected();
        void onError(Exception e);
    }

    private static final String TAG = "TcpClient";

    private final String host;
    private final int port;
    private final ConnectionListener listener;
    private final ExecutorService executor = Executors.newSingleThreadExecutor();

    private Socket socket;
    private PrintWriter out;
    private volatile boolean connected = false;

    public TcpClient(String host, int port, ConnectionListener listener) {
        this.host = host;
        this.port = port;
        this.listener = listener;
    }

    public void connect() {
        executor.execute(() -> {
            try {
                socket = new Socket(host, port);
                out = new PrintWriter(socket.getOutputStream(), true);
                connected = true;
                listener.onConnected();

                BufferedReader in = new BufferedReader(
                        new InputStreamReader(socket.getInputStream()));
                String line;
                while ((line = in.readLine()) != null) {
                    final String msg = line;
                    listener.onMessage(msg);
                }
            } catch (IOException e) {
                if (connected) listener.onError(e);
                else listener.onError(e);
            } finally {
                connected = false;
                listener.onDisconnected();
                close();
            }
        });
    }

    public void send(String message) {
        if (out != null && connected) {
            executor.execute(() -> out.println(message));
        }
    }

    public void disconnect() {
        connected = false;
        close();
        executor.shutdownNow();
    }

    private void close() {
        try {
            if (socket != null) socket.close();
        } catch (IOException e) {
            Log.w(TAG, "Error cerrando socket", e);
        }
    }
}
