package vn.edu.fpt.zentryapp.faceid.data.service;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.PointF;
import android.graphics.Rect;
import android.util.Log;

import java.util.Arrays;
import java.util.List;
import java.util.Random;
import java.util.ArrayList;
import java.util.Collections;
import java.util.concurrent.atomic.AtomicBoolean;

import vn.edu.fpt.zentryapp.faceid.data.service.FaceIdServiceManager;
import vn.edu.fpt.zentryapp.faceid.util.VibrationHelper;

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
    private HeadPoseEstimation headPoseEstimation; // NEW: Head pose for LEFT/RIGHT detection using MediaPipe landmarks
    private final VibrationHelper vibrationHelper; // NEW: Haptic feedback for step completion
    
    // Ownership flags to prevent double-close when instances are shared from FaceIdService
    private final boolean ownsLandmarkExtractor;
    private final boolean ownsGazeEstimator;
    
    // State flags
    private boolean blinkDetected = false;
    private boolean gazeVerified = false;
    private boolean livenessVerified = false;
    
    // Processing flags
    private final AtomicBoolean isProcessing = new AtomicBoolean(false);
    
    // Challenge type
    public enum ChallengeType {
        BLINK_ONLY,     // Only require blinking
        GAZE_ONLY,      // Only require gaze movement (LEFT/RIGHT/CENTER)
        BLINK_AND_GAZE, // Require both blinking and gaze
        RANDOM          // Randomly select challenges
    }
    
    // Head direction for challenges
    public enum HeadDirection {
        LEFT,      // Head turned left
        RIGHT,     // Head turned right  
        CENTER     // Head facing forward + looking at camera
    }
    
    private ChallengeType challengeType = ChallengeType.GAZE_ONLY; // Default to gaze only
    private HeadDirection currentChallenge = HeadDirection.RIGHT; // Will be set randomly
    private HeadDirection detectedDirection = HeadDirection.CENTER;
    
    // 3-Step Challenge System
    private List<HeadDirection> challengeSequence = new ArrayList<>(); // Random sequence of 3 directions
    private int currentStep = 0; // Current step in the sequence (0-2)
    private boolean threeStepChallengeActive = false;
    private Random random = new Random();
    
    /**
     * Callback for face ID enhancement events
     */
    public interface FaceIdEnhancerCallback {
        void onStateChanged(AuthState newState);
        void onBlinkDetected();
        void onGazeDirectionChanged(float x, float y);
        void onGazeDirectionCompleted(String direction);
        void onChallengeGenerated(String challengeText);
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
        
        // Initialize HeadPoseEstimation for LEFT/RIGHT detection
        // Assume 640x480 default, will be updated when first landmarks arrive
        headPoseEstimation = new HeadPoseEstimation(640, 480);
        
        // Initialize VibrationHelper for haptic feedback
        vibrationHelper = new VibrationHelper(context);
        
        Log.d(TAG, "Face ID enhancer initialized");

        // Generate initial challenge
        generateNewChallenge();
    }
    
    /**
     * Generate a new head direction challenge - now uses 3-step challenge
     */
    private void generateNewChallenge() {
        // Always start with 3-step challenge for better liveness detection
        startThreeStepChallenge();
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
        Log.i(TAG, "Resetting FaceIdEnhancer state");
        
        blinkDetected = false;
        gazeVerified = false;
        livenessVerified = false;
        
        // Reset 3-step challenge state
        challengeSequence.clear();
        currentStep = 0;
        threeStepChallengeActive = false;
        
        currentState = AuthState.WAITING;
        
        // Reset components
        blinkDetector.reset();
        
        // Generate new random challenge
        generateNewChallenge();
        
        isProcessing.set(false);
        
        Log.i(TAG, "FaceIdEnhancer reset completed");
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
     * Initialize a 3-step liveness challenge with random directions
     */
    public void startThreeStepChallenge() {
        // Reset challenge state
        challengeSequence.clear();
        currentStep = 0;
        threeStepChallengeActive = true;
        gazeVerified = false;
        
        // Create list of all directions
        List<HeadDirection> allDirections = new ArrayList<>();
        allDirections.add(HeadDirection.LEFT);
        allDirections.add(HeadDirection.RIGHT);
        allDirections.add(HeadDirection.CENTER);
        
        // Shuffle to create random sequence
        Collections.shuffle(allDirections, random);
        challengeSequence.addAll(allDirections);
        
        // Set first challenge
        currentChallenge = challengeSequence.get(0);
        
        Log.i(TAG, "Started 3-step challenge: " + challengeSequence);
        Log.i(TAG, "First challenge: " + currentChallenge);
        
        // Notify UI with first challenge
        if (callback != null) {
            String challengeText = getDirectionText(currentChallenge);
            String instruction = "Step 1/3: " + challengeText;
            Log.i(TAG, "Sending challenge instruction to UI: " + instruction);
            callback.onChallengeGenerated(instruction);
        } else {
            Log.w(TAG, "Callback is null - cannot send challenge instruction to UI");
        }
    }
    
    /**
     * Get user-friendly text for direction
     */
    private String getDirectionText(HeadDirection direction) {
        switch (direction) {
            case LEFT: return "Turn your head LEFT";
            case RIGHT: return "Turn your head RIGHT";
            case CENTER: return "Look straight at the camera";
            default: return "Unknown direction";
        }
    }
    
    /**
     * Process step completion in 3-step challenge
     */
    private void processThreeStepChallenge(HeadDirection detectedDir) {
        if (!threeStepChallengeActive || challengeSequence.isEmpty()) {
            return;
        }
        
        // Check if current step is completed
        if (detectedDir == currentChallenge) {
            currentStep++;
            Log.i(TAG, String.format("Step %d/3 completed: %s", currentStep, currentChallenge));
            
            // Vibrate to indicate step completion
            vibrationHelper.vibrateStepCompleted();
            
            if (currentStep >= 3) {
                // All 3 steps completed!
                threeStepChallengeActive = false;
                gazeVerified = true;
                
                Log.i(TAG, "3-step liveness challenge COMPLETED!");
                
                // Special vibration for challenge completion
                vibrationHelper.vibrateChallengeCompleted();
                
                if (callback != null) {
                    callback.onGazeDirectionCompleted("ALL_STEPS_COMPLETED");
                    callback.onChallengeGenerated("Completed! You passed the liveness challenge");
                }
                
                checkVerificationComplete();
            } else {
                // Move to next step
                currentChallenge = challengeSequence.get(currentStep);
                
                if (callback != null) {
                    String challengeText = getDirectionText(currentChallenge);
                    callback.onChallengeGenerated(String.format("Step %d/3: %s", currentStep + 1, challengeText));
                }
                
                Log.i(TAG, String.format("Moving to step %d/3: %s", currentStep + 1, currentChallenge));
            }
        }
    }
    
    /**
     * Reset challenge state and restart the 3-step challenge
     * Call this when UI gets stuck or needs to restart
     */
    public void resetAndRestartChallenge() {
        Log.i(TAG, "Resetting and restarting 3-step challenge");
        
        // Reset all state
        challengeSequence.clear();
        currentStep = 0;
        threeStepChallengeActive = false;
        gazeVerified = false;
        blinkDetected = false;
        livenessVerified = false;
        
        // Update state
        updateState(AuthState.WAITING);
        
        // Start new challenge
        startThreeStepChallenge();
    }
    
    /**
     * Force show current challenge instruction
     * Call this when UI needs to display the current challenge
     */
    public void showCurrentChallenge() {
        if (threeStepChallengeActive && !challengeSequence.isEmpty() && currentStep < challengeSequence.size()) {
            String challengeText = getDirectionText(currentChallenge);
            String instruction = String.format("Step %d/3: %s", currentStep + 1, challengeText);
            
            Log.i(TAG, "Showing current challenge: " + instruction);
            
            if (callback != null) {
                callback.onChallengeGenerated(instruction);
            }
        } else {
            Log.w(TAG, "No active challenge to show");
            // If no active challenge, start a new one
            resetAndRestartChallenge();
        }
    }
    
    /**
     * Get current challenge status for debugging
     */
    public String getCurrentChallengeStatus() {
        if (!threeStepChallengeActive) {
            return "No active challenge";
        }
        
        return String.format("Challenge active: Step %d/3, Current: %s, Sequence: %s", 
                           currentStep + 1, 
                           currentChallenge, 
                           challengeSequence);
    }
    
    /**
     * Public method to manually start/restart challenge
     * UI can call this when face is detected or when stuck
     */
    public void startLivenessChallenge() {
        Log.i(TAG, "Starting liveness challenge (public method)");
        startThreeStepChallenge();
    }
    
    /**
     * Check if challenge is currently active
     */
    public boolean isChallengeActive() {
        return threeStepChallengeActive;
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
        
        // NEW APPROACH: Use HeadPoseEstimation for LEFT/RIGHT, GazeEstimator for CENTER
        if (!gazeVerified && challengeType == ChallengeType.GAZE_ONLY) {
            processHeadPoseChallenge();
        }
        
        isProcessing.set(false);
    }
    
    /**
     * Process head pose challenge using PnP algorithm
     */
    private void processHeadPoseChallenge() {
        // Get facial landmarks for head pose estimation
        if (landmarkExtractor == null) {
            Log.w(TAG, "Landmark extractor not available");
            return;
        }
        
        List<PointF> landmarks = landmarkExtractor.getAllFaceLandmarks();
        
        if (landmarks != null && landmarks.size() >= 468) {
            // Estimate head pose using PnP algorithm
            boolean success = headPoseEstimation.estimateHeadPose(landmarks);
            
            if (success) {
                String detectedDir = headPoseEstimation.getHeadDirection();
                
                // Map string to enum
                HeadDirection detected = HeadDirection.valueOf(detectedDir);
                detectedDirection = detected;
                
                Log.d(TAG, String.format("HEAD POSE: yaw=%.1f°, pitch=%.1f°, roll=%.1f° → %s (challenge: %s)",
                      headPoseEstimation.getYaw(), headPoseEstimation.getPitch(), headPoseEstimation.getRoll(),
                      detected, currentChallenge));
                
                // Use 3-step challenge if active
                if (threeStepChallengeActive) {
                    processThreeStepChallenge(detected);
                } else {
                    // Original single-step logic
                    if (detected == currentChallenge) {
                        if (currentChallenge == HeadDirection.CENTER) {
                            // For CENTER, also verify gaze is looking at camera
                            verifyCenterWithGaze();
                        } else {
                            // For LEFT/RIGHT, head pose is sufficient
                            Log.i(TAG, "Head direction challenge COMPLETED: " + currentChallenge);
                            gazeVerified = true;
                            
                            // Vibrate for single-step completion
                            vibrationHelper.vibrateChallengeCompleted();
                            
                            if (callback != null) {
                                callback.onGazeDirectionCompleted(currentChallenge.toString());
                            }
                            
                            checkVerificationComplete();
                        }
                    } else {
                        Log.d(TAG, String.format("Challenge not complete: detected=%s, required=%s", detected, currentChallenge));
                    }
                }
                
            } else {
                Log.w(TAG, "Head pose estimation failed");
            }
        } else {
            Log.w(TAG, "Insufficient landmarks for head pose estimation: " + 
                  (landmarks != null ? landmarks.size() : "null"));
        }
    }
    
    /**
     * Verify CENTER challenge by combining head pose + gaze detection
     */
    private void verifyCenterWithGaze() {
        // Check if head is facing forward
        if (headPoseEstimation.isFacingForward()) {
            // Use gaze estimator to verify looking at camera
            float[] headPose = landmarkExtractor.getHeadEulerAngles();
            Bitmap leftEyeRegion = landmarkExtractor.getLeftEyeRegion();
            Bitmap rightEyeRegion = landmarkExtractor.getRightEyeRegion();
            
            if (leftEyeRegion != null && rightEyeRegion != null) {
                Log.d(TAG, "Verifying CENTER challenge with gaze detection...");
                gazeEstimator.estimateGaze(leftEyeRegion, rightEyeRegion, headPose);
                // Result will be handled in onGazeUpdate callback
            } else {
                Log.w(TAG, "Eye regions not available for CENTER verification");
                // For 3-step challenge, accept CENTER based on head pose only
                if (threeStepChallengeActive) {
                    processThreeStepChallenge(HeadDirection.CENTER);
                }
            }
        } else {
            Log.d(TAG, "Head not facing forward for CENTER challenge");
        }
    }
    
    //------------------------------------------------------------------------------
    // EyeBlinkDetector.BlinkDetectionCallback Implementation
    //------------------------------------------------------------------------------
    
    @Override
    public void onBlink(boolean isLeftEye, boolean isRightEye) {
        if (!blinkDetected) {
            blinkDetected = true;
            updateState(AuthState.BLINK_VERIFIED);
            
            // Vibrate for blink detection
            vibrationHelper.vibrateStepCompleted();
            
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
        // This callback is now only used for CENTER verification (looking at camera)
        Log.d(TAG, String.format("Gaze callback: isLookingAtScreen=%b (for CENTER verification)", isLookingAtScreen));
        
        if (currentChallenge == HeadDirection.CENTER && isLookingAtScreen) {
            Log.i(TAG, "CENTER challenge COMPLETED: Head facing forward + looking at camera");
            
            // Use 3-step challenge if active, otherwise use original logic
            if (threeStepChallengeActive) {
                processThreeStepChallenge(HeadDirection.CENTER);
            } else {
                gazeVerified = true;
                
                // Vibrate for single-step CENTER completion
                vibrationHelper.vibrateChallengeCompleted();
                
                if (callback != null) {
                    callback.onGazeDirectionCompleted("CENTER");
                }
                
                checkVerificationComplete();
            }
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
            // Cancel any ongoing vibration
            if (vibrationHelper != null) {
                vibrationHelper.cancelVibration();
            }
        } catch (Exception e) {
            Log.w(TAG, "Error cancelling vibration", e);
        }
        
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
