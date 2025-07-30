package vn.edu.fpt.zentryapp.student.ui.setting.state;

import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import java.util.concurrent.atomic.AtomicReference;

/**
 * Thread-safe State Manager cho Face Registration
 * Đảm bảo state transitions nhất quán và tránh race conditions
 */
public class FaceRegistrationStateManager {
    private static final String TAG = "FaceRegStateManager";
    
    private final AtomicReference<FaceRegistrationState> currentState = 
            new AtomicReference<>(FaceRegistrationState.INITIALIZING);
    
    private final Handler mainHandler = new Handler(Looper.getMainLooper());
    private StateChangeListener listener;
    
    // Timeouts
    private Runnable detectionTimeoutRunnable;
    private Runnable registrationTimeoutRunnable;
    private static final long DETECTION_TIMEOUT_MS = 30000; // 30 seconds
    private static final long REGISTRATION_TIMEOUT_MS = 15000; // 15 seconds
    
    public interface StateChangeListener {
        void onStateChanged(FaceRegistrationState newState, String message);
    }
    
    public void setStateChangeListener(StateChangeListener listener) {
        this.listener = listener;
    }
    
    /**
     * Thread-safe state transition
     */
    public boolean transitionTo(FaceRegistrationState newState, String customMessage) {
        FaceRegistrationState oldState = currentState.get();
        
        // Validate transition
        if (!isValidTransition(oldState, newState)) {
            Log.w(TAG, "Invalid transition from " + oldState + " to " + newState);
            return false;
        }
        
        // Perform atomic state change
        if (currentState.compareAndSet(oldState, newState)) {
            Log.d(TAG, "State transition: " + oldState + " → " + newState);
            
            // Cancel previous timeouts
            cancelTimeouts();
            
            // Set new timeouts if needed
            scheduleTimeouts(newState);
            
            // Notify listener on main thread
            String message = customMessage != null ? customMessage : newState.getDefaultMessage();
            mainHandler.post(() -> {
                if (listener != null) {
                    listener.onStateChanged(newState, message);
                }
            });
            
            return true;
        }
        
        return false;
    }
    
    /**
     * Get current state
     */
    public FaceRegistrationState getCurrentState() {
        return currentState.get();
    }
    
    /**
     * Kiểm tra transition có hợp lệ không
     */
    private boolean isValidTransition(FaceRegistrationState from, FaceRegistrationState to) {
        // Final states không thể transition sang state khác
        if (from.isFinalState() && to != from) {
            return false;
        }
        
        // Một số transition logic cụ thể
        switch (from) {
            case INITIALIZING:
                return to == FaceRegistrationState.READY || to.isErrorState();
                
            case PROCESSING:
                return to == FaceRegistrationState.SUCCESS || to.isErrorState();
                
            case CAPTURING:
                return to == FaceRegistrationState.PROCESSING || to.isErrorState();
                
            default:
                return true; // Allow most transitions by default
        }
    }
    
    /**
     * Schedule timeouts cho states cần thiết
     */
    private void scheduleTimeouts(FaceRegistrationState state) {
        switch (state) {
            case READY:
            case NO_FACE:
            case FACE_DETECTED:
                // Timeout nếu không detect được face sau 30s
                detectionTimeoutRunnable = () -> {
                    transitionTo(FaceRegistrationState.TIMEOUT_DETECTION, 
                        "Face detection timeout. Please try again.");
                };
                mainHandler.postDelayed(detectionTimeoutRunnable, DETECTION_TIMEOUT_MS);
                break;
                
            case PROCESSING:
                // Timeout nếu registration quá lâu
                registrationTimeoutRunnable = () -> {
                    transitionTo(FaceRegistrationState.TIMEOUT_REGISTRATION, 
                        "Registration timeout. Please try again.");
                };
                mainHandler.postDelayed(registrationTimeoutRunnable, REGISTRATION_TIMEOUT_MS);
                break;
        }
    }
    
    /**
     * Cancel all pending timeouts
     */
    private void cancelTimeouts() {
        if (detectionTimeoutRunnable != null) {
            mainHandler.removeCallbacks(detectionTimeoutRunnable);
            detectionTimeoutRunnable = null;
        }
        if (registrationTimeoutRunnable != null) {
            mainHandler.removeCallbacks(registrationTimeoutRunnable);
            registrationTimeoutRunnable = null;
        }
    }
    
    /**
     * Reset state manager
     */
    public void reset() {
        cancelTimeouts();
        currentState.set(FaceRegistrationState.INITIALIZING);
        Log.d(TAG, "State manager reset");
    }
    
    /**
     * Cleanup resources
     */
    public void cleanup() {
        cancelTimeouts();
        listener = null;
        Log.d(TAG, "State manager cleaned up");
    }
}