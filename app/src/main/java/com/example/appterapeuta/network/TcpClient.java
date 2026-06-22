package com.example.appterapeuta.network;

import android.util.Log;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Cliente TCP para conectarse a un robot descubierto por NSD.
 * Incluye timeout de conexión, timeout de lectura (SO_TIMEOUT) y heartbeat.
 */
public class TcpClient {

    public interface ConnectionListener {
        void onConnected();
        void onMessage(String message);
        void onDisconnected();
        void onError(Exception e);
    }

    private static final String TAG = "TcpClient";
    private static final int CONNECT_TIMEOUT_MS = 5000;
    private static final int READ_TIMEOUT_MS = 16000; // >3x heartbeat interval (5s)

    private final String host;
    private final int port;
    private final ConnectionListener listener;
    private final ExecutorService readExecutor  = Executors.newSingleThreadExecutor();
    private final ExecutorService writeExecutor = Executors.newSingleThreadExecutor();

    private Socket socket;
    private PrintWriter out;
    private volatile boolean connected = false;
    private volatile boolean intentionalDisconnect = false;

    public TcpClient(String host, int port, ConnectionListener listener) {
        this.host = host;
        this.port = port;
        this.listener = listener;
    }

    public void connect() {
        readExecutor.execute(() -> {
            try {
                socket = new Socket();
                socket.connect(new InetSocketAddress(host, port), CONNECT_TIMEOUT_MS);
                socket.setSoTimeout(READ_TIMEOUT_MS);
                socket.setKeepAlive(true);
                out = new PrintWriter(socket.getOutputStream(), true);
                connected = true;
                listener.onConnected();

                BufferedReader in = new BufferedReader(
                        new InputStreamReader(socket.getInputStream()));
                String line;
                while ((line = in.readLine()) != null) {
                    listener.onMessage(line);
                }
                // readLine returned null = peer closed cleanly
                connected = false;
                if (!intentionalDisconnect) listener.onDisconnected();
            } catch (java.net.SocketTimeoutException e) {
                // SO_TIMEOUT fired — no data received in READ_TIMEOUT_MS
                connected = false;
                if (!intentionalDisconnect) listener.onDisconnected();
            } catch (IOException e) {
                connected = false;
                if (!intentionalDisconnect) listener.onError(e);
            } finally {
                close();
            }
        });
    }

    public void send(String message) {
        if (out != null && connected) {
            writeExecutor.execute(() -> out.println(message));
        }
    }

    public boolean isConnected() {
        return connected;
    }

    public void disconnect() {
        intentionalDisconnect = true;
        connected = false;
        close();
        readExecutor.shutdownNow();
        writeExecutor.shutdownNow();
    }

    private void close() {
        try {
            if (socket != null) socket.close();
        } catch (IOException e) {
            Log.w(TAG, "Error cerrando socket", e);
        }
    }
}
