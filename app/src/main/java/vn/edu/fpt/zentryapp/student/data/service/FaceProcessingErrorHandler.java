package vn.edu.fpt.zentryapp.student.data.service;

import android.content.Context;
import android.util.Log;
import android.widget.Toast;

/**
 * Handler for face processing errors
 * Provides user-friendly error messages and graceful degradation
 */
public class FaceProcessingErrorHandler {
    private static final String TAG = "FaceProcessingErrorHandler";
    
    private final Context context;
    private final ErrorRecoveryStrategy recoveryStrategy;
    
    public FaceProcessingErrorHandler(Context context) {
        this(context, new DefaultErrorRecoveryStrategy());
    }
    
    public FaceProcessingErrorHandler(Context context, ErrorRecoveryStrategy recoveryStrategy) {
        this.context = context.getApplicationContext();
        this.recoveryStrategy = recoveryStrategy;
    }
    
    /**
     * Handle model initialization errors
     */
    public void handleModelInitializationError(Exception e, String modelName) {
        Log.e(TAG, "Model initialization error for " + modelName + ": " + e.getMessage(), e);
        
        String userMessage = getUserFriendlyMessage(FaceProcessingError.MODEL_INITIALIZATION_FAILED);
        showUserMessage(userMessage);
        
        recoveryStrategy.onModelInitializationFailed(modelName, e);
    }
    
    /**
     * Handle face detection errors
     */
    public void handleFaceDetectionError(Exception e) {
        Log.e(TAG, "Face detection error: " + e.getMessage(), e);
        
        String userMessage = getUserFriendlyMessage(FaceProcessingError.FACE_DETECTION_FAILED);
        showUserMessage(userMessage);
        
        recoveryStrategy.onFaceDetectionFailed(e);
    }
    
    /**
     * Handle spoof detection errors
     */
    public void handleSpoofDetectionError(Exception e) {
        Log.e(TAG, "Spoof detection error: " + e.getMessage(), e);
        
        String userMessage = getUserFriendlyMessage(FaceProcessingError.SPOOF_DETECTION_FAILED);
        showUserMessage(userMessage);
        
        recoveryStrategy.onSpoofDetectionFailed(e);
    }
    
    /**
     * Handle network errors
     */
    public void handleNetworkError(Exception e, String operation) {
        Log.e(TAG, "Network error during " + operation + ": " + e.getMessage(), e);
        
        String userMessage = getUserFriendlyMessage(FaceProcessingError.NETWORK_ERROR);
        showUserMessage(userMessage);
        
        recoveryStrategy.onNetworkError(operation, e);
    }
    
    /**
     * Handle timeout errors
     */
    public void handleTimeoutError(String operation, long timeoutMs) {
        Log.w(TAG, "Timeout error during " + operation + " after " + timeoutMs + "ms");
        
        String userMessage = getUserFriendlyMessage(FaceProcessingError.TIMEOUT_ERROR);
        showUserMessage(userMessage);
        
        recoveryStrategy.onTimeoutError(operation, timeoutMs);
    }
    
    /**
     * Handle low confidence errors
     */
    public void handleLowConfidenceError(float confidence, float threshold) {
        Log.w(TAG, "Low confidence error: " + confidence + " < " + threshold);
        
        String userMessage = getUserFriendlyMessage(FaceProcessingError.LOW_CONFIDENCE);
        showUserMessage(userMessage);
        
        recoveryStrategy.onLowConfidenceError(confidence, threshold);
    }
    
    /**
     * Handle oval validation errors
     */
    public void handleOvalValidationError(String reason) {
        Log.w(TAG, "Oval validation error: " + reason);
        
        String userMessage = getUserFriendlyMessage(FaceProcessingError.OVAL_VALIDATION_FAILED);
        showUserMessage(userMessage);
        
        recoveryStrategy.onOvalValidationFailed(reason);
    }
    
    /**
     * Handle general processing errors
     */
    public void handleGeneralError(Exception e, String operation) {
        Log.e(TAG, "General error during " + operation + ": " + e.getMessage(), e);
        
        String userMessage = getUserFriendlyMessage(FaceProcessingError.GENERAL_ERROR);
        showUserMessage(userMessage);
        
        recoveryStrategy.onGeneralError(operation, e);
    }
    
    /**
     * Get user-friendly error message
     */
    public String getUserFriendlyMessage(FaceProcessingError error) {
        switch (error) {
            case MODEL_INITIALIZATION_FAILED:
                return "Face detection system is initializing. Please wait a moment and try again.";
                
            case FACE_DETECTION_FAILED:
                return "Unable to detect your face. Please ensure your face is clearly visible and try again.";
                
            case SPOOF_DETECTION_FAILED:
                return "Face verification system is temporarily unavailable. Please try again in a moment.";
                
            case NETWORK_ERROR:
                return "Network connection issue. Please check your internet connection and try again.";
                
            case TIMEOUT_ERROR:
                return "Operation is taking longer than expected. Please try again.";
                
            case LOW_CONFIDENCE:
                return "Face detection is unclear. Please improve lighting and position your face properly.";
                
            case OVAL_VALIDATION_FAILED:
                return "Please position your face within the oval guide for better detection.";
                
            case GENERAL_ERROR:
                return "An unexpected error occurred. Please try again.";
                
            default:
                return "Please try again.";
        }
    }
    
    /**
     * Show user message (can be overridden for custom UI)
     */
    protected void showUserMessage(String message) {
        // Default implementation shows Toast
        // Can be overridden to show custom UI
        Toast.makeText(context, message, Toast.LENGTH_SHORT).show();
    }
    
    /**
     * Check if error is recoverable
     */
    public boolean isRecoverableError(FaceProcessingError error) {
        switch (error) {
            case NETWORK_ERROR:
            case TIMEOUT_ERROR:
            case LOW_CONFIDENCE:
            case OVAL_VALIDATION_FAILED:
                return true;
                
            case MODEL_INITIALIZATION_FAILED:
            case FACE_DETECTION_FAILED:
            case SPOOF_DETECTION_FAILED:
            case GENERAL_ERROR:
                return false;
                
            default:
                return false;
        }
    }
    
    /**
     * Get recovery suggestion for error
     */
    public String getRecoverySuggestion(FaceProcessingError error) {
        switch (error) {
            case NETWORK_ERROR:
                return "Check your internet connection and try again.";
                
            case TIMEOUT_ERROR:
                return "Try again in a few moments.";
                
            case LOW_CONFIDENCE:
                return "Improve lighting and position your face clearly.";
                
            case OVAL_VALIDATION_FAILED:
                return "Position your face within the oval guide.";
                
            default:
                return "Please try again.";
        }
    }
    
    /**
     * Error types
     */
    public enum FaceProcessingError {
        MODEL_INITIALIZATION_FAILED,
        FACE_DETECTION_FAILED,
        SPOOF_DETECTION_FAILED,
        NETWORK_ERROR,
        TIMEOUT_ERROR,
        LOW_CONFIDENCE,
        OVAL_VALIDATION_FAILED,
        GENERAL_ERROR
    }
    
    /**
     * Error recovery strategy interface
     */
    public interface ErrorRecoveryStrategy {
        void onModelInitializationFailed(String modelName, Exception e);
        void onFaceDetectionFailed(Exception e);
        void onSpoofDetectionFailed(Exception e);
        void onNetworkError(String operation, Exception e);
        void onTimeoutError(String operation, long timeoutMs);
        void onLowConfidenceError(float confidence, float threshold);
        void onOvalValidationFailed(String reason);
        void onGeneralError(String operation, Exception e);
    }
    
    /**
     * Default error recovery strategy
     */
    public static class DefaultErrorRecoveryStrategy implements ErrorRecoveryStrategy {
        @Override
        public void onModelInitializationFailed(String modelName, Exception e) {
            Log.w(TAG, "Default recovery: Model " + modelName + " failed to initialize");
        }
        
        @Override
        public void onFaceDetectionFailed(Exception e) {
            Log.w(TAG, "Default recovery: Face detection failed");
        }
        
        @Override
        public void onSpoofDetectionFailed(Exception e) {
            Log.w(TAG, "Default recovery: Spoof detection failed");
        }
        
        @Override
        public void onNetworkError(String operation, Exception e) {
            Log.w(TAG, "Default recovery: Network error during " + operation);
        }
        
        @Override
        public void onTimeoutError(String operation, long timeoutMs) {
            Log.w(TAG, "Default recovery: Timeout during " + operation);
        }
        
        @Override
        public void onLowConfidenceError(float confidence, float threshold) {
            Log.w(TAG, "Default recovery: Low confidence " + confidence + " < " + threshold);
        }
        
        @Override
        public void onOvalValidationFailed(String reason) {
            Log.w(TAG, "Default recovery: Oval validation failed - " + reason);
        }
        
        @Override
        public void onGeneralError(String operation, Exception e) {
            Log.w(TAG, "Default recovery: General error during " + operation);
        }
    }
} 