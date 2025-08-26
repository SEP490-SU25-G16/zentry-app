package vn.edu.fpt.zentryapp.faceid.util;

import android.content.Context;
import android.os.Build;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.util.Log;

/**
 * Helper class for haptic feedback during face authentication challenges
 * Provides different vibration patterns for various events
 */
public class VibrationHelper {
    private static final String TAG = "VibrationHelper";
    
    private final Vibrator vibrator;
    private final boolean isVibratorAvailable;
    
    // Vibration patterns (in milliseconds)
    private static final long STEP_COMPLETED_DURATION = 100; // Short buzz for step completion
    private static final long CHALLENGE_COMPLETED_DURATION = 200; // Longer buzz for full challenge completion
    private static final long ERROR_PATTERN[] = {0, 50, 100, 50}; // Double buzz for errors
    
    public VibrationHelper(Context context) {
        vibrator = (Vibrator) context.getSystemService(Context.VIBRATOR_SERVICE);
        isVibratorAvailable = vibrator != null && vibrator.hasVibrator();
        
        if (!isVibratorAvailable) {
            Log.w(TAG, "Vibrator not available on this device");
        } else {
            Log.d(TAG, "VibrationHelper initialized successfully");
        }
    }
    
    /**
     * Vibrate when a step is completed successfully
     */
    public void vibrateStepCompleted() {
        if (!isVibratorAvailable) return;
        
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                // Android 8.0+ - Use VibrationEffect
                VibrationEffect effect = VibrationEffect.createOneShot(
                    STEP_COMPLETED_DURATION, 
                    VibrationEffect.DEFAULT_AMPLITUDE
                );
                vibrator.vibrate(effect);
            } else {
                // Older versions - deprecated method but still works
                vibrator.vibrate(STEP_COMPLETED_DURATION);
            }
            
            Log.d(TAG, "Step completed vibration triggered");
            
        } catch (Exception e) {
            Log.e(TAG, "Error triggering step completed vibration", e);
        }
    }
    
    /**
     * Vibrate when entire challenge is completed successfully
     */
    public void vibrateChallengeCompleted() {
        if (!isVibratorAvailable) return;
        
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                // Longer, stronger vibration for challenge completion
                VibrationEffect effect = VibrationEffect.createOneShot(
                    CHALLENGE_COMPLETED_DURATION, 
                    VibrationEffect.EFFECT_HEAVY_CLICK
                );
                vibrator.vibrate(effect);
            } else {
                vibrator.vibrate(CHALLENGE_COMPLETED_DURATION);
            }
            
            Log.d(TAG, "Challenge completed vibration triggered");
            
        } catch (Exception e) {
            Log.e(TAG, "Error triggering challenge completed vibration", e);
        }
    }
    
    /**
     * Vibrate with error pattern when something goes wrong
     */
    public void vibrateError() {
        if (!isVibratorAvailable) return;
        
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                // Create pattern: wait 0ms, vibrate 50ms, wait 100ms, vibrate 50ms
                VibrationEffect effect = VibrationEffect.createWaveform(
                    ERROR_PATTERN, 
                    -1 // Don't repeat
                );
                vibrator.vibrate(effect);
            } else {
                vibrator.vibrate(ERROR_PATTERN, -1);
            }
            
            Log.d(TAG, "Error vibration triggered");
            
        } catch (Exception e) {
            Log.e(TAG, "Error triggering error vibration", e);
        }
    }
    
    /**
     * Light vibration for UI feedback (button presses, etc.)
     */
    public void vibrateLight() {
        if (!isVibratorAvailable) return;
        
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                VibrationEffect effect = VibrationEffect.createOneShot(
                    50, // Very short
                    VibrationEffect.EFFECT_TICK
                );
                vibrator.vibrate(effect);
            } else {
                vibrator.vibrate(50);
            }
            
            Log.d(TAG, "Light vibration triggered");
            
        } catch (Exception e) {
            Log.e(TAG, "Error triggering light vibration", e);
        }
    }
    
    /**
     * Cancel any ongoing vibration
     */
    public void cancelVibration() {
        if (!isVibratorAvailable) return;
        
        try {
            vibrator.cancel();
            Log.d(TAG, "Vibration cancelled");
        } catch (Exception e) {
            Log.e(TAG, "Error cancelling vibration", e);
        }
    }
    
    /**
     * Check if vibration is available on this device
     */
    public boolean isVibrationAvailable() {
        return isVibratorAvailable;
    }
}
