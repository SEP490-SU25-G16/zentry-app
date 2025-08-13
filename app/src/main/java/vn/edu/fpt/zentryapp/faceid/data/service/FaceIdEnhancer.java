package vn.edu.fpt.zentryapp.faceid.data.service;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.PointF;
import android.graphics.Rect;
import android.util.Log;

import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

import vn.edu.fpt.zentryapp.faceid.data.service.FaceIdServiceManager;

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
    // Ownership flags to prevent double-close when instances are shared from FaceIdService
    private final boolean ownsLandmarkExtractor;
    private final boolean ownsGazeEstimator;
    
    // State flags
    private boolean blinkDetected = false;
    private boolean gazeVerified = false;
    private boolean livenessVerified = false;
    
    // Robust, prompt-driven gaze challenge
    public enum Direction { LEFT, RIGHT, UP, DOWN }
    private java.util.List<Direction> requiredDirections = new java.util.ArrayList<>();
    private int currentDirectionIndex = 0;
    private int directionStableFrames = 0;
    private static final int DIRECTION_STABLE_FRAMES_REQUIRED = 7; // reduced for responsiveness
    private static final float GAZE_THRESHOLD = 0.35f; // relaxed to improve detection at larger yaw
    
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
        // New: notify when a gaze direction step is completed
        void onGazeStepVerified(Direction direction, int stepIndex, int totalSteps);
        // New: continually prompt required gaze direction to the UI
        void onGazePrompt(Direction required, int stepIndex, int totalSteps);
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
            ownsLandmarkExtractor = false;
            Log.d(TAG, "Using preloaded MediaPipeFaceLandmarkExtractor from FaceIdService");
        } else {
            // Fallback: create new instance if not available
            landmarkExtractor = new MediaPipeFaceLandmarkExtractor(context);
            ownsLandmarkExtractor = true;
            Log.d(TAG, "Created new MediaPipeFaceLandmarkExtractor (fallback)");
        }
        
        blinkDetector = new EyeBlinkDetector(this);
        
        // Get GazeEstimator from FaceIdService if available, otherwise create new instance
        if (faceIdService != null && faceIdService.getGazeEstimator() != null) {
            gazeEstimator = faceIdService.getGazeEstimator();
            gazeEstimator.setCallback(this); // Set callback for the shared instance
            ownsGazeEstimator = false;
            Log.d(TAG, "Using GazeEstimator from FaceIdService");
        } else {
            // Fallback: create new instance if not available
            gazeEstimator = new GazeEstimator(context, this);
            ownsGazeEstimator = true;
            Log.d(TAG, "Created new GazeEstimator (fallback)");
        }
        
        Log.d(TAG, "Face ID enhancer initialized");

        // Default gaze challenge pattern: look RIGHT then LEFT
        this.requiredDirections.clear();
        this.requiredDirections.add(Direction.RIGHT);
        this.requiredDirections.add(Direction.LEFT);
        this.currentDirectionIndex = 0;
        this.directionStableFrames = 0;
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
        // Reset gaze sequence state
        currentDirectionIndex = 0;
        directionStableFrames = 0;
        
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
        if (gazeVerified) return;
        if (currentDirectionIndex >= requiredDirections.size()) {
            gazeVerified = true;
            updateState(AuthState.GAZE_VERIFIED);
            Log.d(TAG, "Gaze verification complete (sequence)");
            checkVerificationComplete();
        }
    }

    /**
     * Get the currently required gaze direction in the challenge sequence.
     * Returns null if the sequence is complete or not configured.
     */
    private Direction getCurrentRequiredDirection() {
        if (requiredDirections == null || requiredDirections.isEmpty()) {
            return null;
        }
        if (currentDirectionIndex < 0 || currentDirectionIndex >= requiredDirections.size()) {
            return null;
        }
        return requiredDirections.get(currentDirectionIndex);
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
        // Determine current required direction
        Direction currentRequired = getCurrentRequiredDirection();

        // If no current required (sequence done), just finalize
        if (currentRequired == null) {
            checkGazeVerification();
            return;
        }

        // Prompt UI about current required direction on every update (idempotent)
        if (callback != null) {
            callback.onGazePrompt(currentRequired, currentDirectionIndex, requiredDirections.size());
        }

        // GazeEstimator already outputs mirrored coordinates when front camera; use as-is
        float adjX = x;
        float adjY = y;

        if (callback != null) {
            callback.onGazeDirectionChanged(adjX, adjY);
        }

        boolean meetsDirection = false;
        switch (currentRequired) {
            case LEFT:
                meetsDirection = (adjX < -GAZE_THRESHOLD);
                break;
            case RIGHT:
                meetsDirection = (adjX > GAZE_THRESHOLD);
                break;
            case UP:
                meetsDirection = (adjY < -GAZE_THRESHOLD);
                break;
            case DOWN:
                meetsDirection = (adjY > GAZE_THRESHOLD);
                break;
        }

        if (meetsDirection) {
            directionStableFrames++;
        } else {
            // Reset stability counter if user deviates from required direction
            directionStableFrames = 0;
        }

        if (directionStableFrames >= DIRECTION_STABLE_FRAMES_REQUIRED) {
            // Mark this direction as completed and advance to next
            if (callback != null) {
                callback.onGazeStepVerified(currentRequired, currentDirectionIndex, requiredDirections.size());
            }
            currentDirectionIndex++;
            directionStableFrames = 0;
            Log.d(TAG, "Gaze direction completed: " + currentRequired);
            checkGazeVerification();
        }
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
        try {
            if (ownsLandmarkExtractor && landmarkExtractor != null) {
                landmarkExtractor.close();
            }
        } catch (Exception e) {
            Log.w(TAG, "Error closing landmark extractor", e);
        }
        try {
            if (ownsGazeEstimator && gazeEstimator != null) {
                gazeEstimator.close();
            }
        } catch (Exception e) {
            Log.w(TAG, "Error closing gaze estimator", e);
        }
    }
}
