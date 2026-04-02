package com.example.appterapeuta.network;

import android.content.Context;
import android.net.nsd.NsdManager;
import android.net.nsd.NsdServiceInfo;
import android.util.Log;

import com.example.appterapeuta.data.model.DiscoveredRobot;
import com.example.appterapeuta.util.AppConstants;

public class NsdDiscoveryManager {

    public interface OnRobotDiscoveredListener {
        void onRobotFound(DiscoveredRobot robot);
        void onRobotLost(String serviceName);
    }

    private static final String TAG = "NsdDiscoveryManager";

    private final NsdManager nsdManager;
    private NsdManager.DiscoveryListener discoveryListener;
    private OnRobotDiscoveredListener listener;

    // NsdManager no permite resoluciones concurrentes; usamos flag de guardia
    private boolean resolving = false;

    public NsdDiscoveryManager(Context context) {
        nsdManager = (NsdManager) context.getSystemService(Context.NSD_SERVICE);
    }

    public void startDiscovery(OnRobotDiscoveredListener listener) {
        this.listener = listener;

        discoveryListener = new NsdManager.DiscoveryListener() {
            @Override public void onStartDiscoveryFailed(String serviceType, int errorCode) {
                Log.e(TAG, "startDiscovery fallido: " + errorCode);
            }
            @Override public void onStopDiscoveryFailed(String serviceType, int errorCode) {
                Log.e(TAG, "stopDiscovery fallido: " + errorCode);
            }
            @Override public void onDiscoveryStarted(String serviceType) {
                Log.d(TAG, "Búsqueda NSD iniciada");
            }
            @Override public void onDiscoveryStopped(String serviceType) {
                Log.d(TAG, "Búsqueda NSD detenida");
            }
            @Override public void onServiceFound(NsdServiceInfo serviceInfo) {
                resolveService(serviceInfo);
            }
            @Override public void onServiceLost(NsdServiceInfo serviceInfo) {
                if (NsdDiscoveryManager.this.listener != null) {
                    NsdDiscoveryManager.this.listener.onRobotLost(serviceInfo.getServiceName());
                }
            }
        };

        nsdManager.discoverServices(AppConstants.NSD_SERVICE_TYPE, NsdManager.PROTOCOL_DNS_SD, discoveryListener);
    }

    public void stopDiscovery() {
        if (discoveryListener != null) {
            try {
                nsdManager.stopServiceDiscovery(discoveryListener);
            } catch (IllegalArgumentException e) {
                Log.w(TAG, "stopDiscovery sin búsqueda activa");
            }
            discoveryListener = null;
        }
        listener = null;
    }

    private void resolveService(NsdServiceInfo serviceInfo) {
        if (resolving) {
            // NsdManager no soporta resoluciones concurrentes; descartamos y esperamos al próximo evento
            Log.d(TAG, "Resolución en curso, se omite: " + serviceInfo.getServiceName());
            return;
        }
        resolving = true;

        nsdManager.resolveService(serviceInfo, new NsdManager.ResolveListener() {
            @Override public void onResolveFailed(NsdServiceInfo info, int errorCode) {
                Log.e(TAG, "Resolución fallida para " + info.getServiceName() + ": " + errorCode);
                resolving = false;
            }
            @Override public void onServiceResolved(NsdServiceInfo info) {
                resolving = false;
                if (listener == null) return;
                String host = info.getHost().getHostAddress();
                DiscoveredRobot robot = new DiscoveredRobot(info.getServiceName(), host, info.getPort());
                listener.onRobotFound(robot);
            }
        });
    }
}
