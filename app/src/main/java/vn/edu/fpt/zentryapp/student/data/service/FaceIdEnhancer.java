package vn.edu.fpt.zentryapp.student.data.service;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.PointF;
import android.graphics.Rect;
import android.util.Log;

import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

import vn.edu.fpt.zentryapp.student.data.service.FaceIdServiceManager;

/**
 * Enhances Face ID authentication by adding blink detection and gaze tracking
 * to prevent spoofing with photos or videos
 */
public class FaceIdEnhancer implements 
        MediaPipeFaceLandmarkExtractor.LandmarkExtractionCallback,
        EyeBlinkDetector.BlinkDetectionCallback,
        GazeEstimator.GazeCallback {
    
    private static final String TAG = "FaceIdEnhancer";
    
    // Authentication state
    public enum AuthState {
        WAITING,
        FACE_DETECTED,
        ANALYZING,
        BLINK_VERIFIED,
        GAZE_VERIFIED,
        VERIFIED,
        FAILED
    }


    private AuthState currentState = AuthState.WAITING;
    
    // Component instances
    private final MediaPipeFaceLandmarkExtractor landmarkExtractor;
    private final EyeBlinkDetector blinkDetector;
    private final GazeEstimator gazeEstimator;
    
    // State flags
    private boolean blinkDetected = false;
    private boolean gazeVerified = false;
    private boolean livenessVerified = false;
    
    // For checking if the user looked at different directions
    private boolean lookedLeft = false;
    private boolean lookedRight = false;
    private boolean lookedUp = false;
    private boolean lookedDown = false;
    
    // Processing flags
    private final AtomicBoolean isProcessing = new AtomicBoolean(false);
    
    // Challenge type
    public enum ChallengeType {
        BLINK_ONLY,     // Only require blinking
        GAZE_ONLY,      // Only require gaze movement
        BLINK_AND_GAZE, // Require both blinking and gaze
        RANDOM          // Randomly select challenges
    }
    
    private ChallengeType challengeType = ChallengeType.BLINK_AND_GAZE;
    
    /**
     * Callback for face ID enhancement events
     */
    public interface FaceIdEnhancerCallback {
        void onStateChanged(AuthState newState);
        void onBlinkDetected();
        void onGazeDirectionChanged(float x, float y);
        void onLivenessVerified(boolean isLive);
        void onVerificationComplete(boolean success);
    }
    
    private FaceIdEnhancerCallback callback;
    
    /**
     * Creates a new face ID enhancer
     * 
     * @param context Application context
     * @param callback Callback for face ID enhancement events
     */
    public FaceIdEnhancer(Context context, FaceIdEnhancerCallback callback) {
        this.callback = callback;
        
        // Initialize components - Use preloaded models from FaceIdService
        FaceIdService faceIdService = FaceIdServiceManager.getInstance().getService();
        
        // Get preloaded MediaPipeFaceLandmarkExtractor from FaceIdService
        if (faceIdService != null && faceIdService.getMediaPipeFaceLandmarkExtractor() != null) {
            landmarkExtractor = faceIdService.getMediaPipeFaceLandmarkExtractor();
            Log.d(TAG, "Using preloaded MediaPipeFaceLandmarkExtractor from FaceIdService");
        } else {
            // Fallback: create new instance if not available
            landmarkExtractor = new MediaPipeFaceLandmarkExtractor(context);
            Log.d(TAG, "Created new MediaPipeFaceLandmarkExtractor (fallback)");
        }
        
        blinkDetector = new EyeBlinkDetector(this);
        
        // Get GazeEstimator from FaceIdService if available, otherwise create new instance
        if (faceIdService != null && faceIdService.getGazeEstimator() != null) {
            gazeEstimator = faceIdService.getGazeEstimator();
            gazeEstimator.setCallback(this); // Set callback for the shared instance
            Log.d(TAG, "Using GazeEstimator from FaceIdService");
        } else {
            // Fallback: create new instance if not available
            gazeEstimator = new GazeEstimator(context, this);
            Log.d(TAG, "Created new GazeEstimator (fallback)");
        }
        
        Log.d(TAG, "Face ID enhancer initialized");
    }
    
    /**
     * Process a face frame for liveness detection
     * 
     * @param faceBitmap The face bitmap to process
     * @param faceRect The detected face rectangle
     */
    public void processFaceFrame(Bitmap faceBitmap, Rect faceRect) {
        // Skip if already processing a frame
        if (isProcessing.getAndSet(true)) {
            return;
        }
        
        // Skip if already verified
        if (currentState == AuthState.VERIFIED || currentState == AuthState.FAILED) {
            isProcessing.set(false);
            return;
        }
        
        // Check if landmarkExtractor is still active
        if (landmarkExtractor == null || !landmarkExtractor.isActive()) {
            Log.w(TAG, "LandmarkExtractor is not active, skipping frame processing");
            isProcessing.set(false);
            return;
        }
        
        // Update state if needed
        if (currentState == AuthState.WAITING) {
            updateState(AuthState.FACE_DETECTED);
        }
        
        // Extract facial landmarks
        landmarkExtractor.extractLandmarks(faceBitmap, faceRect, this);
    }
    
    /**
     * Set the challenge type for liveness verification
     */
    public void setChallengeType(ChallengeType type) {
        this.challengeType = type;
        Log.d(TAG, "Challenge type set to: " + type);
    }
    
    /**
     * Reset the enhancer state to start a new verification
     */
    public void reset() {
        blinkDetected = false;
        gazeVerified = false;
        livenessVerified = false;
        lookedLeft = false;
        lookedRight = false;
        lookedUp = false;
        lookedDown = false;
        
        currentState = AuthState.WAITING;
        
        // Reset components
        blinkDetector.reset();
        
        isProcessing.set(false);
        
        Log.d(TAG, "Face ID enhancer reset");
    }
    
    /**
     * Check if liveness verification is complete
     */
    public boolean isLivenessVerified() {
        return livenessVerified;
    }
    
    /**
     * Get the current authentication state
     */
    public AuthState getCurrentState() {
        return currentState;
    }
    
    /**
     * Update the current state and notify callback
     */
    private void updateState(AuthState newState) {
        currentState = newState;
        Log.d(TAG, "State changed to: " + newState);
        
        if (callback != null) {
            callback.onStateChanged(newState);
        }
    }
    
    /**
     * Check if all required verifications are complete based on challenge type
     */
    private void checkVerificationComplete() {
        if (livenessVerified) {
            return; // Already verified
        }
        
        boolean verified = false;
        
        switch (challengeType) {
            case BLINK_ONLY:
                verified = blinkDetected;
                break;
            case GAZE_ONLY:
                verified = gazeVerified;
                break;
            case BLINK_AND_GAZE:
                verified = blinkDetected && gazeVerified;
                break;
            case RANDOM:
                // Randomly select if we need blink, gaze, or both
                // This decision should be made once at the beginning and stored
                // For now, require both as in BLINK_AND_GAZE
                verified = blinkDetected && gazeVerified;
                break;
        }
        
        if (verified) {
            livenessVerified = true;
            updateState(AuthState.VERIFIED);
            
            if (callback != null) {
                callback.onLivenessVerified(true);
                callback.onVerificationComplete(true);
            }
            
            Log.d(TAG, "Liveness verification complete: SUCCESS");
        }
    }
    
    /**
     * Check if the user has looked in all required directions
     */
    private void checkGazeVerification() {
        boolean allDirectionsChecked = lookedLeft && lookedRight && 
                                      (lookedUp || lookedDown); // Only require one of up/down
        
        if (allDirectionsChecked && !gazeVerified) {
            gazeVerified = true;
            updateState(AuthState.GAZE_VERIFIED);
            Log.d(TAG, "Gaze verification complete");
            
            // Check if all verifications are complete
            checkVerificationComplete();
        }
    }

    //------------------------------------------------------------------------------
    // FaceLandmarkExtractor.LandmarkExtractionCallback Implementation
    //------------------------------------------------------------------------------
    
    @Override
    public void onLandmarksExtracted(boolean success) {
        if (!success) {
            Log.w(TAG, "Landmark extraction failed");
            isProcessing.set(false);
            return;
        }
        
        if (currentState == AuthState.FACE_DETECTED) {
            updateState(AuthState.ANALYZING);
        }
        
        // Process for blink detection
        List<PointF> leftEyePoints = landmarkExtractor.getLeftEyeEARPoints();
        List<PointF> rightEyePoints = landmarkExtractor.getRightEyeEARPoints();
        
        // Debug logging for eye points
        Log.d(TAG, "Eye points - Left: " + leftEyePoints.size() + ", Right: " + rightEyePoints.size());
        
        // Detect blinks - REMOVED blinkDetected check to allow multiple detections
        if (!leftEyePoints.isEmpty() && !rightEyePoints.isEmpty()) {
            boolean blinkDetected = blinkDetector.detectBlink(
                    leftEyePoints, 
                    rightEyePoints,
                    landmarkExtractor.getLeftEyeOpenProbability(),
                    landmarkExtractor.getRightEyeOpenProbability()
            );
            
            if (blinkDetected) {
                Log.d(TAG, "Blink detected in FaceIdEnhancer");
            }
        } else {
            Log.w(TAG, "No eye points available for blink detection");
        }
        
        // Process for gaze estimation
        if (!gazeVerified) {
            Bitmap leftEyeRegion = landmarkExtractor.getLeftEyeRegion();
            Bitmap rightEyeRegion = landmarkExtractor.getRightEyeRegion();
            float[] headPose = landmarkExtractor.getHeadEulerAngles();
            
            if (leftEyeRegion != null && rightEyeRegion != null) {
                gazeEstimator.estimateGaze(leftEyeRegion, rightEyeRegion, headPose);
            } else {
                Log.w(TAG, "Eye regions not available for gaze estimation");
            }
        }
        
        isProcessing.set(false);
    }
    
    //------------------------------------------------------------------------------
    // EyeBlinkDetector.BlinkDetectionCallback Implementation
    //------------------------------------------------------------------------------
    
    @Override
    public void onBlink(boolean isLeftEye, boolean isRightEye) {
        if (!blinkDetected) {
            blinkDetected = true;
            updateState(AuthState.BLINK_VERIFIED);
            
            if (callback != null) {
                callback.onBlinkDetected();
            }
            
            Log.d(TAG, "Blink verified");
            
            // Check if all verifications are complete
            checkVerificationComplete();
        }
    }
    
    @Override
    public void onIntentionalBlink(int blinkCount) {
        // Additional handling for intentional blinks (multiple blinks)
        Log.d(TAG, "Intentional blink detected: " + blinkCount + " blinks");
    }
    
    //------------------------------------------------------------------------------
    // GazeEstimator.GazeCallback Implementation
    //------------------------------------------------------------------------------
    
    @Override
    public void onGazeUpdate(float x, float y, boolean isLookingAtScreen) {
        // Notify about gaze direction change
        if (callback != null) {
            callback.onGazeDirectionChanged(x, y);
        }
        
        // Update gaze direction flags
        if (x < -0.3f) lookedLeft = true;
        if (x > 0.3f) lookedRight = true;
        if (y < -0.3f) lookedUp = true;
        if (y > 0.3f) lookedDown = true;
        
        // Check if gaze verification is complete
        checkGazeVerification();
    }
    
    @Override
    public void onLookingAway(boolean isLookingAway) {
        // Can be used for additional security checks
        if (isLookingAway) {
            Log.d(TAG, "User is looking away from screen");
        }
    }
    
    /**
     * Release resources
     */
    public void close() {
        landmarkExtractor.close();
        gazeEstimator.close();
    }
}
