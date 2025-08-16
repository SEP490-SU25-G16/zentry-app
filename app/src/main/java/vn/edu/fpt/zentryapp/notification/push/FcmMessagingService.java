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
            Log.d(TAG, "📱 FCM message received: " + remoteMessage.getMessageId());
            Log.d(TAG, "📱 FCM data: " + remoteMessage.getData());
            
            // Refresh notifications immediately
            String userId = AuthManager.getInstance(getApplicationContext()).getCurrentUserId();
            if (userId != null) {
                Log.d(TAG, "👤 User ID found: " + userId);
                
                // Fire-and-forget: start a lightweight service to refresh via API
                NotificationRefreshService.enqueueWork(getApplicationContext(), userId);
                
                // 🔧 IMPROVED: Send detailed broadcast with notification info
                Intent broadcastIntent = new Intent("vn.edu.fpt.zentryapp.NOTIFICATIONS_UPDATED");
                broadcastIntent.putExtra("userId", userId);
                broadcastIntent.putExtra("timestamp", System.currentTimeMillis());
                
                // Add notification details if available
                if (remoteMessage.getData() != null && !remoteMessage.getData().isEmpty()) {
                    String dataJson = remoteMessage.getData().get("Data");
                    if (dataJson != null) {
                        try {
                            JSONObject json = new JSONObject(dataJson);
                            String type = json.optString("type", "");
                            String title = json.optString("title", "");
                            String body = json.optString("body", "");
                            
                            broadcastIntent.putExtra("notificationType", type);
                            broadcastIntent.putExtra("notificationTitle", title);
                            broadcastIntent.putExtra("notificationBody", body);
                            
                            Log.d(TAG, "📢 Broadcasting notification update: " + type + " - " + title);
                        } catch (Exception e) {
                            Log.e(TAG, "Error parsing notification data", e);
                        }
                    }
                }
                
                // 🔧 IMPROVED: Send broadcast with better error handling
                try {
                    androidx.localbroadcastmanager.content.LocalBroadcastManager localBroadcastManager = 
                        androidx.localbroadcastmanager.content.LocalBroadcastManager.getInstance(getApplicationContext());
                    
                    localBroadcastManager.sendBroadcast(broadcastIntent);
                    Log.d(TAG, "✅ Broadcast sent successfully");
                    
                    // 🔧 NEW: Verify broadcast was sent
                    Log.d(TAG, "🔍 Broadcast details:");
                    Log.d(TAG, "  - Action: " + broadcastIntent.getAction());
                    Log.d(TAG, "  - UserId: " + broadcastIntent.getStringExtra("userId"));
                    Log.d(TAG, "  - Type: " + broadcastIntent.getStringExtra("notificationType"));
                    Log.d(TAG, "  - Title: " + broadcastIntent.getStringExtra("notificationTitle"));
                    
                } catch (Exception e) {
                    Log.e(TAG, "❌ Failed to send broadcast", e);
                }
            } else {
                Log.w(TAG, "⚠️ No user ID found, cannot send broadcast");
            }

            // If push includes deeplink or server-side action, handle extras
            if (remoteMessage.getData() != null && !remoteMessage.getData().isEmpty()) {
                String dataJson = remoteMessage.getData().get("Data");
                if (dataJson != null) {
                    JSONObject json = new JSONObject(dataJson);
                    String type = json.optString("type", "");
                    String deeplink = json.optString("deeplink", "");
                    String action = json.optString("action", "");
                    if ("FACE_VERIFICATION_REQUEST".equalsIgnoreCase(type) && !deeplink.isEmpty()) {
                        // Post a local broadcast or notification click intent can be configured to open deeplink.
                        // Here we just log; UI click will open deeplink.
                        Log.d(TAG, "🎭 Received face verification request deeplink: " + deeplink);
                    } else if ("SESSION_ENDED".equalsIgnoreCase(type) || "END_SESSION".equalsIgnoreCase(action)) {
                        // Stop BLE attendance for students on session end notification
                        try {
                            Intent stopBle = new Intent(getApplicationContext(), vn.edu.fpt.zentryapp.service.BLEAttendanceService.class);
                            stopBle.setAction("STOP_ATTENDANCE");
                            getApplicationContext().stopService(stopBle);
                            Log.d(TAG, "Stopped BLEAttendanceService due to session end push");
                        } catch (Exception e) {
                            Log.e(TAG, "Error stopping BLE service on session end push", e);
                        }
                    }
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "❌ Error handling FCM message", e);
        }
    }
}


