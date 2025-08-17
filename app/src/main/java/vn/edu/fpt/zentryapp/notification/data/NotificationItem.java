package vn.edu.fpt.zentryapp.notification.data;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;
import android.util.Log;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class NotificationItem implements Serializable {
    private String id;
    private String title;
    private String message;
    private String timestamp;
    private boolean isRead;
    private boolean isSeen; // Đánh dấu thông báo đã được seen (xem qua) chưa

    // Optional raw data for deep link or action parsing
    private String rawData;
    
    // ✅ NEW: Expiration timestamp for Face ID verification requests
    private String expiresAt;


    // ✅ NEW: Check if notification is expired
    public boolean isExpired() {
        if (expiresAt == null || expiresAt.isEmpty()) {
            android.util.Log.d("NotificationItem", "🔍 No expiration set, treating as not expired");
            return false; // No expiration set, treat as not expired
        }
        
        try {
            // Parse ISO 8601 timestamp with microseconds support
            // Format: "2025-08-17T06:18:56.4318807Z" or "2024-01-01T12:00:00Z"
            SimpleDateFormat sdf;
            String formatUsed;
            
            if (expiresAt.contains(".")) {
                // Has microseconds/milliseconds
                sdf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSSSSS'Z'", Locale.US);
                formatUsed = "yyyy-MM-dd'T'HH:mm:ss.SSSSSSS'Z'";
            } else {
                // No microseconds/milliseconds
                sdf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US);
                formatUsed = "yyyy-MM-dd'T'HH:mm:ss'Z'";
            }
            
            android.util.Log.d("NotificationItem", "🔍 Using format: " + formatUsed + " for timestamp: " + expiresAt);
            sdf.setTimeZone(TimeZone.getTimeZone("UTC"));
            Date expirationDate = sdf.parse(expiresAt);
            
            if (expirationDate == null) {
                android.util.Log.w("NotificationItem", "⚠️ Failed to parse expiration date: " + expiresAt);
                return false; // Treat as not expired if parsing fails
            }
            
            long currentTime = System.currentTimeMillis();
            long expirationTime = expirationDate.getTime();
            
            // Add 5-minute buffer for network delays and processing time
            long bufferTime = 5 * 60 * 1000; // 5 minutes in milliseconds
            
            boolean isExpired = currentTime > (expirationTime + bufferTime);
            
            android.util.Log.d("NotificationItem", "🔍 Expiration check: " + expiresAt);
            android.util.Log.d("NotificationItem", "🔍 Current time: " + new Date(currentTime));
            android.util.Log.d("NotificationItem", "🔍 Expiration time: " + expirationDate);
            android.util.Log.d("NotificationItem", "🔍 Buffer time: " + (bufferTime / 1000) + "s");
            android.util.Log.d("NotificationItem", "🔍 Is expired: " + isExpired);
            
            return isExpired;
            
        } catch (ParseException e) {
            android.util.Log.w("NotificationItem", "⚠️ Primary format failed, trying fallback parsing: " + expiresAt);
            
            // Fallback: Try different timestamp formats
            try {
                SimpleDateFormat fallbackSdf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.US);
                fallbackSdf.setTimeZone(TimeZone.getTimeZone("UTC"));
                Date expirationDate = fallbackSdf.parse(expiresAt);
                
                if (expirationDate != null) {
                    long currentTime = System.currentTimeMillis();
                    long expirationTime = expirationDate.getTime();
                    long bufferTime = 5 * 60 * 1000; // 5 minutes
                    
                    boolean isExpired = currentTime > (expirationTime + bufferTime);
                    android.util.Log.d("NotificationItem", "✅ Fallback parsing successful, isExpired: " + isExpired);
                    return isExpired;
                }
            } catch (ParseException fallbackException) {
                android.util.Log.e("NotificationItem", "❌ Fallback parsing also failed: " + expiresAt, fallbackException);
            }
            
            return false; // Treat as not expired if all parsing fails
        }
    }
}
