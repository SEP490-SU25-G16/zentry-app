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
    
    // ✅ NEW: Constant for session end notification text
    private static final String SESSION_END_TEXT = "Tiết học đã kết thúc sớm";
    
    // ✅ NEW: Flag to prevent duplicate BLE service stops
    private static volatile boolean isBLEServiceStopped = false;
    
    // ✅ NEW: Method to reset BLE service stopped flag (called when new session starts)
    public static void resetBLEServiceStoppedFlag() {
        isBLEServiceStopped = false;
        Log.d(TAG, "🔄 Reset BLE service stopped flag - ready for new session");
    }
    
    // ✅ NEW: Method to check if BLE service is already stopped
    public static boolean isBLEServiceStopped() {
        return isBLEServiceStopped;
    }

    @Override
    public void onMessageReceived(@NonNull RemoteMessage remoteMessage) {
        try {
            Log.d(TAG, "📱 FCM message received: " + remoteMessage.getMessageId());
            Log.d(TAG, "📱 FCM data: " + remoteMessage.getData());
            
            // ✅ NEW: Declare variables in wider scope to avoid scope issues
            String notificationType = "";
            String notificationTitle = "";
            String notificationBody = "";
            String deeplink = "";
            String action = "";
            
            // Refresh notifications immediately
            String userId = AuthManager.getInstance(getApplicationContext()).getCurrentUserId();
            if (userId != null) {
                Log.d(TAG, "👤 User ID found: " + userId);
                
                // Fire-and-forget: start a lightweight service to refresh via API
               // NotificationRefreshService.enqueueWork(getApplicationContext(), userId);
                
                // 🔧 IMPROVED: Send detailed broadcast with notification info
                Intent broadcastIntent = new Intent("vn.edu.fpt.zentryapp.NOTIFICATIONS_UPDATED");
                broadcastIntent.putExtra("userId", userId);
                broadcastIntent.putExtra("timestamp", System.currentTimeMillis());
                
                // Add notification details if available
                if (remoteMessage.getData() != null && !remoteMessage.getData().isEmpty()) {
                    String dataJson = remoteMessage.getData().get("Data");
                    Log.d(TAG, "onMessageReceived: " + dataJson);
                    if (dataJson != null) {
                        try {
                            JSONObject json = new JSONObject(dataJson);
                            notificationType = json.optString("type", "");
                            notificationTitle = json.optString("title", "");
                            notificationBody = json.optString("body", "");
                            
                            broadcastIntent.putExtra("notificationType", notificationType);
                            broadcastIntent.putExtra("notificationTitle", notificationTitle);
                            broadcastIntent.putExtra("notificationBody", notificationBody);
                            
                            Log.d(TAG, "📢 Broadcasting notification update: " + notificationType + " - " + notificationTitle);
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

            // ✅ SỬA: Tách riêng logic xử lý từng loại notification
            if (remoteMessage.getData() != null && !remoteMessage.getData().isEmpty()) {
                String dataJson = remoteMessage.getData().get("Data");
                if (dataJson != null) {
                    try {
                        JSONObject json = new JSONObject(dataJson);
                        notificationType = json.optString("type", "");
                        deeplink = json.optString("deeplink", "");

                        // Handle Face ID verification
                        if ("FACE_VERIFICATION_REQUEST".equalsIgnoreCase(notificationType) && !deeplink.isEmpty()) {
                            Log.d(TAG, "🎭 Received face verification request deeplink: " + deeplink);
                        }

                    } catch (Exception e) {
                        Log.e(TAG, "Error parsing notification data for extras", e);
                    }
                }
            }

            // ✅ SỬA: Kiểm tra session end RIÊNG BIỆT, không trong else if
            // Check for session end in title OR body
            boolean isSessionEnd = false;
            if (notificationTitle != null && notificationTitle.contains(SESSION_END_TEXT)) {
                isSessionEnd = true;
                Log.d(TAG, "🔍 Session end detected in TITLE: " + notificationTitle);
            }
            if (notificationBody != null && notificationBody.contains(SESSION_END_TEXT)) {
                isSessionEnd = true;
                Log.d(TAG, "🔍 Session end detected in BODY: " + notificationBody);
            }

            if (isSessionEnd && !isBLEServiceStopped) {
                try {
                    Intent stopBle = new Intent(getApplicationContext(), vn.edu.fpt.zentryapp.service.BLEAttendanceService.class);
                    stopBle.setAction("STOP_ATTENDANCE");
                    getApplicationContext().startService(stopBle);

                    isBLEServiceStopped = true;

                    Log.d(TAG, "✅ FCM: Sent STOP_ATTENDANCE intent to BLE service");
                    Log.d(TAG, "🛡️ FCM: BLE service stop flag set to prevent duplicates");

                } catch (Exception e) {
                    Log.e(TAG, "❌ FCM: Error sending STOP_ATTENDANCE intent to BLE service", e);
                }
            } else if (isSessionEnd) {
                Log.d(TAG, "ℹ️ FCM: Session end detected but BLE service already stopped");
            }
        } catch (Exception e) {
            Log.e(TAG, "❌ Error handling FCM message", e);
        }
    }
}


