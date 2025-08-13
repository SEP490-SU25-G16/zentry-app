package vn.edu.fpt.zentryapp.faceid.ui.components;

import android.util.Log;
import android.util.Size;

/**
 * Helper class for device-specific optimizations.
 * This class provides methods to optimize camera and face detection based on device model.
 */
public class DeviceSpecificOptimizer {
    private static final String TAG = "DeviceSpecificOptimizer";
    
    /**
     * Get optimal resolution based on device model
     * Different devices perform better with different resolutions
     */
    public static Size getOptimalResolutionForDevice() {
        String manufacturer = android.os.Build.MANUFACTURER.toLowerCase();
        String model = android.os.Build.MODEL.toLowerCase();
        
        // Default resolution for most devices
        Size defaultResolution = new Size(640, 480);
        
        // Lower resolution for lower-end devices
        Size lowResolution = new Size(320, 240);
        
        // Higher resolution for high-end devices
        Size highResolution = new Size(1280, 720);
        
        Log.d(TAG, "Selecting resolution for " + manufacturer + " " + model);
        
        // Samsung devices
        if (manufacturer.contains("samsung")) {
            if (model.contains("a10") || model.contains("a20") || model.contains("a30")) {
                Log.d(TAG, "Using low resolution for Samsung A series");
                return lowResolution;
            } else if (model.contains("s10") || model.contains("s20") || model.contains("note")) {
                Log.d(TAG, "Using high resolution for Samsung flagship");
                return highResolution;
            }
        }
        
        // Xiaomi devices - tend to need lower resolution
        if (manufacturer.contains("xiaomi") || manufacturer.contains("redmi")) {
            Log.d(TAG, "Using low resolution for Xiaomi device");
            return lowResolution;
        }
        
        // Oppo devices
        if (manufacturer.contains("oppo") || manufacturer.contains("realme")) {
            Log.d(TAG, "Using standard resolution for Oppo device");
            return defaultResolution;
        }
        
        // LG devices
        if (manufacturer.contains("lg")) {
            if (model.contains("g7") || model.contains("thinq")) {
                Log.d(TAG, "Using standard resolution for LG G7 ThinQ");
                return defaultResolution;
            }
        }
        
        Log.d(TAG, "Using default resolution for unrecognized device");
        return defaultResolution;
    }
    
    /**
     * Determine if this device should use emergency fallback conversion first
     */
    public static boolean shouldUseEmergencyFallbackFirst() {
        String manufacturer = android.os.Build.MANUFACTURER.toLowerCase();
        String model = android.os.Build.MODEL.toLowerCase();
        
        // Xiaomi devices often have issues with standard YUV conversion
        if (manufacturer.contains("xiaomi") || manufacturer.contains("redmi")) {
            Log.d(TAG, "Device-specific optimization: Using emergency fallback for Xiaomi device");
            return true;
        }
        
        // Oppo devices may also benefit from emergency fallback
        if (manufacturer.contains("oppo") || manufacturer.contains("realme")) {
            Log.d(TAG, "Device-specific optimization: Using emergency fallback for Oppo device");
            return true;
        }
        
        // Some LG models have custom camera implementations
        if (manufacturer.contains("lg") && model.contains("thinq")) {
            Log.d(TAG, "Device-specific optimization: Using emergency fallback for LG device");
            return true;
        }
        
        return false;
    }
    
    /**
     * Get optimal face detection confidence threshold based on device
     */
    public static float getOptimalDetectionConfidence() {
        String manufacturer = android.os.Build.MANUFACTURER.toLowerCase();
        
        // Lower threshold for devices with challenging camera conditions
        if (manufacturer.contains("xiaomi") || 
            manufacturer.contains("oppo") || 
            manufacturer.contains("realme") ||
            manufacturer.contains("lg")) {
            return 0.3f;  // Lower threshold for these devices
        }
        
        return 0.5f;  // Standard threshold for most devices
    }
    
    /**
     * Get device information string for logging
     */
    public static String getDeviceInfoString() {
        return "Device: " + android.os.Build.MANUFACTURER + " " + 
               android.os.Build.MODEL + ", Android " + android.os.Build.VERSION.RELEASE;
    }
}
