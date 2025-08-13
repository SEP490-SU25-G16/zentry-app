package vn.edu.fpt.zentryapp.notification.push;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.util.Log;


import org.json.JSONObject;

import vn.edu.fpt.zentryapp.auth.client.AuthManager;

import androidx.annotation.NonNull;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import com.google.firebase.messaging.FirebaseMessagingService;
import com.google.firebase.messaging.RemoteMessage;

/**
 * Realtime push entry-point. On push arrival, we refresh notifications via API.
 */
@SuppressLint("MissingFirebaseInstanceTokenRefresh")
public class FcmMessagingService extends FirebaseMessagingService {
    private static final String TAG = "FcmMessagingService";

    @Override
    public void onMessageReceived(@NonNull RemoteMessage remoteMessage) {
        try {
            // Refresh notifications immediately
            String userId = AuthManager.getInstance(getApplicationContext()).getCurrentUserId();
            if (userId != null) {
                // Fire-and-forget: start a lightweight service to refresh via API
                NotificationRefreshService.enqueueWork(getApplicationContext(), userId);
                // Notify UI to reload if it's visible
                Intent i = new Intent("vn.edu.fpt.zentryapp.NOTIFICATIONS_UPDATED");
                LocalBroadcastManager.getInstance(getApplicationContext()).sendBroadcast(i);
            }

            // If push includes deeplink and app is foreground, optionally open it
            if (remoteMessage.getData() != null && !remoteMessage.getData().isEmpty()) {
                String dataJson = remoteMessage.getData().get("Data");
                if (dataJson != null) {
                    JSONObject json = new JSONObject(dataJson);
                    String type = json.optString("type", "");
                    String deeplink = json.optString("deeplink", "");
                    if ("FACE_VERIFICATION_REQUEST".equalsIgnoreCase(type) && !deeplink.isEmpty()) {
                        // Post a local broadcast or notification click intent can be configured to open deeplink.
                        // Here we just log; UI click will open deeplink.
                        Log.d(TAG, "Received face verification request deeplink: " + deeplink);
                    }
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "Error handling FCM message", e);
        }
    }
}


