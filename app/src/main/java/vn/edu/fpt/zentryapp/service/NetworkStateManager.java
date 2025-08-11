package vn.edu.fpt.zentryapp.service;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkCapabilities;
import android.net.NetworkRequest;
import android.util.Log;

public class NetworkStateManager {
    private static final String TAG = "NetworkStateManager";

    private final Context context;
    private final ConnectivityManager connectivityManager;
    private NetworkCallback networkCallback;
    private boolean isNetworkAvailable = false;

    public interface NetworkStateListener {
        void onNetworkAvailable();
        void onNetworkLost();
    }

    private NetworkStateListener listener;

    public NetworkStateManager(Context context) {
        this.context = context;
        this.connectivityManager = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        checkInitialNetworkState();
    }

    private void checkInitialNetworkState() {
        Network activeNetwork = connectivityManager.getActiveNetwork();
        if (activeNetwork != null) {
            NetworkCapabilities capabilities = connectivityManager.getNetworkCapabilities(activeNetwork);
            isNetworkAvailable = capabilities != null &&
                    (capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) ||
                            capabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR));
        }

        Log.d(TAG, "Initial network state: " + (isNetworkAvailable ? "Available" : "Unavailable"));
    }

    public void startMonitoring(NetworkStateListener listener) {
        this.listener = listener;

        if (networkCallback != null) {
            stopMonitoring();
        }

        networkCallback = new NetworkCallback();

        try {
            connectivityManager.registerDefaultNetworkCallback(networkCallback);

            Log.d(TAG, "📶 Default network callback registered successfully");

        } catch (Exception e) {
            Log.e(TAG, "❌ Failed to register default network callback", e);
            networkCallback = null;
        }
    }

    public void stopMonitoring() {
        if (networkCallback != null) {
            connectivityManager.unregisterNetworkCallback(networkCallback);
            networkCallback = null;
            Log.d(TAG, "🚫 Stopped network monitoring");
        }
    }

    public boolean isNetworkAvailable() {
        return isNetworkAvailable;
    }

    private class NetworkCallback extends ConnectivityManager.NetworkCallback {
        @Override
        public void onAvailable(Network network) {
            Log.d(TAG, "🟢 Network became available");
            isNetworkAvailable = true;
            new android.os.Handler(android.os.Looper.getMainLooper()).post(() -> {
                if (listener != null) {
                    listener.onNetworkAvailable();
                }
            });
        }

        @Override
        public void onLost(Network network) {
            Log.d(TAG, "🔴 Network lost");
            isNetworkAvailable = false;
            new android.os.Handler(android.os.Looper.getMainLooper()).post(() -> {
                if (listener != null) {
                    listener.onNetworkLost();
                }
            });
        }
    }
}
