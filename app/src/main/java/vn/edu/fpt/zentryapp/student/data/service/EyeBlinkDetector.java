package vn.edu.fpt.zentryapp.student.data.service;

import android.graphics.PointF;
import android.util.Log;

import java.util.ArrayList;
import java.util.List;

import lombok.Getter;

/**
 * Detector for eye blinks using the Eye Aspect Ratio (EAR) algorithm
 * Based on the paper: "Real-Time Eye Blink Detection using Facial Landmarks"
 * by Soukupová and Čech (2016)
 * 
 * OPTIMIZED FOR REAL DATA PERFORMANCE
 */
public class EyeBlinkDetector {
    private static final String TAG = "EyeBlinkDetector";
    
    // Constants for blink detection - OPTIMIZED FOR REAL DATA
    private static final float EAR_THRESHOLD = 0.25f;      // Standard threshold for real data
    private static final int BLINK_FRAME_THRESHOLD = 1;    // Fast detection for real data
    private static final int MAX_BLINK_DURATION = 15;      // Maximum blink duration
    
    // State tracking
    private boolean isBlinking = false;
    private int blinkFrameCount = 0;
    private int totalBlinks = 0;
    
    // History for eye aspect ratios
    private final List<Float> leftEyeEARHistory = new ArrayList<>();
    private final List<Float> rightEyeEARHistory = new ArrayList<>();
    private static final int HISTORY_SIZE = 10;

    /**
     * -- GETTER --
     *  Get the current normal EAR value
     */
    // For calculating average EAR in normal state
    @Getter
    private float normalEAR = 0.3f;
    private int calibrationFrames = 0;
    private static final int CALIBRATION_FRAMES_NEEDED = 15; // Fast calibration for real data
    private boolean isCalibrated = false;
    
    // For detecting intentional blinks (multiple blinks in sequence)
    private long lastBlinkTimestamp = 0;
    private static final long INTENTIONAL_BLINK_INTERVAL_MS = 1000; // Maximum time between intentional blinks
    private int consecutiveBlinkCount = 0;
    private static final int INTENTIONAL_BLINK_COUNT = 2; // Number of blinks to consider intentional
    
    // Debug logging
    private boolean debugMode = true;
    
    /**
     * Callback for blink detection events
     */
    public interface BlinkDetectionCallback {
        void onBlink(boolean isLeftEye, boolean isRightEye);
        void onIntentionalBlink(int blinkCount);
    }
    
    private BlinkDetectionCallback callback;
    
    /**
     * Create a new blink detector optimized for real data
     * @param callback The callback for blink detection events
     */
    public EyeBlinkDetector(BlinkDetectionCallback callback) {
        this.callback = callback;
        Log.d(TAG, "Blink detector initialized for real data performance");
    }
    
    /**
     * Process eye landmarks to detect blinks (REAL DATA ONLY)
     * 
     * @param leftEyePoints The 6 key points for the left eye
     * @param rightEyePoints The 6 key points for the right eye
     * @param leftEyeOpenProbability ML Kit's probability of left eye being open
     * @param rightEyeOpenProbability ML Kit's probability of right eye being open
     * @return True if a blink was detected in this frame
     */
    public boolean detectBlink(
            List<PointF> leftEyePoints, 
            List<PointF> rightEyePoints,
            float leftEyeOpenProbability,
            float rightEyeOpenProbability) {
        
        // Check if we have valid eye points
        if (leftEyePoints.size() < 6 || rightEyePoints.size() < 6) {
            if (debugMode) {
                Log.w(TAG, "Insufficient eye points for EAR calculation. Left: " + leftEyePoints.size() + ", Right: " + rightEyePoints.size());
            }
            return false;
        }
        
        // Calculate Eye Aspect Ratio (EAR) for both eyes
        float leftEAR = calculateEAR(leftEyePoints);
        float rightEAR = calculateEAR(rightEyePoints);
        
        // Average EAR of both eyes
        float avgEAR = (leftEAR + rightEAR) / 2.0f;
        
        // Update history
        updateEARHistory(leftEAR, rightEAR);
        
        // Debug logging for real data
        if (debugMode) {
            Log.d(TAG, String.format("REAL DATA EAR - Left: %.3f, Right: %.3f, Avg: %.3f, LeftProb: %.3f, RightProb: %.3f", 
                leftEAR, rightEAR, avgEAR, leftEyeOpenProbability, rightEyeOpenProbability));
        }
        
        // Calibrate if needed
        if (!isCalibrated) {
            calibrateEAR(avgEAR);
            if (debugMode) {
                Log.d(TAG, "Calibrating EAR for real data. Frames: " + calibrationFrames + "/" + CALIBRATION_FRAMES_NEEDED);
            }
            return false;
        }
        
        // Use optimized threshold for real data
        float absoluteThreshold = 0.25f; // Standard threshold for real data
        float dynamicThreshold = Math.min(normalEAR * 0.65f, absoluteThreshold);
        
        if (debugMode) {
            Log.d(TAG, String.format("REAL DATA Thresholds - Absolute: %.3f, Dynamic: %.3f, NormalEAR: %.3f", 
                absoluteThreshold, dynamicThreshold, normalEAR));
        }
        
        // Detect blink state using real data thresholds
        boolean leftEyeClosed = leftEAR < absoluteThreshold;
        boolean rightEyeClosed = rightEAR < absoluteThreshold;
        boolean bothEyesClosed = leftEyeClosed && rightEyeClosed;
        
        // Use the ML Kit's probability as additional evidence
        boolean mlKitDetectedBlink = leftEyeOpenProbability < 0.4f && rightEyeOpenProbability < 0.4f;
        
        // Enhanced blink detection logic for real data
        boolean strongBlinkEvidence = bothEyesClosed || 
                                    (leftEyeClosed && rightEyeClosed) ||
                                    (avgEAR < absoluteThreshold * 0.8f) ||
                                    mlKitDetectedBlink;
        
        if (debugMode && (leftEyeClosed || rightEyeClosed || strongBlinkEvidence)) {
            Log.d(TAG, String.format("REAL DATA Blink detected - Left: %s, Right: %s, MLKit: %s, Strong: %s, AvgEAR: %.3f", 
                leftEyeClosed, rightEyeClosed, mlKitDetectedBlink, strongBlinkEvidence, avgEAR));
        }
        
        // Update blink state machine
        return updateBlinkState(leftEyeClosed, rightEyeClosed, strongBlinkEvidence);
    }
    
    /**
     * Calculate the Eye Aspect Ratio (EAR) for real data
     * EAR = (||p2-p6|| + ||p3-p5||) / (2 * ||p1-p4||)
     * Where p1-p6 are the 6 landmarks of the eye
     */
    private float calculateEAR(List<PointF> eyePoints) {
        if (eyePoints.size() < 6) {
            return 1.0f; // Default to open eye
        }
        
        // Calculate vertical distances (average of two pairs)
        float verticalDist1 = distance(eyePoints.get(1), eyePoints.get(5)); // Top left to bottom left
        float verticalDist2 = distance(eyePoints.get(2), eyePoints.get(4)); // Top right to bottom right
        
        // Calculate horizontal distance (eye width)
        float horizontalDist = distance(eyePoints.get(0), eyePoints.get(3)); // Left corner to right corner
        
        // Avoid division by zero
        if (horizontalDist == 0) {
            return 1.0f;
        }
        
        // Calculate EAR for real data
        float ear = (verticalDist1 + verticalDist2) / (2 * horizontalDist);
        
        // Validate EAR for real data (should be between 0.0 and 1.0)
        if (ear < 0.0f) {
            ear = 0.0f;
            if (debugMode) {
                Log.d(TAG, "EAR value too low (" + ear + "), setting to 0.0f");
            }
        } else if (ear > 1.0f) {
            ear = 1.0f;
            if (debugMode) {
                Log.d(TAG, "EAR value too high (" + ear + "), setting to 1.0f");
            }
        }
        
        if (debugMode) {
            Log.d(TAG, String.format("REAL DATA EAR calculation - V1: %.3f, V2: %.3f, H: %.3f, EAR: %.3f", 
                verticalDist1, verticalDist2, horizontalDist, ear));
        }
        
        return ear;
    }
    
    /**
     * Calculate Euclidean distance between two points
     */
    private float distance(PointF p1, PointF p2) {
        float dx = p1.x - p2.x;
        float dy = p1.y - p2.y;
        return (float) Math.sqrt(dx * dx + dy * dy);
    }
    
    /**
     * Update the EAR history
     */
    private void updateEARHistory(float leftEAR, float rightEAR) {
        // Add to history
        leftEyeEARHistory.add(leftEAR);
        rightEyeEARHistory.add(rightEAR);
        
        // Keep history at fixed size
        if (leftEyeEARHistory.size() > HISTORY_SIZE) {
            leftEyeEARHistory.remove(0);
        }
        if (rightEyeEARHistory.size() > HISTORY_SIZE) {
            rightEyeEARHistory.remove(0);
        }
    }
    
    /**
     * Calibrate the normal EAR for this person
     */
    private void calibrateEAR(float currentEAR) {
        // Only use values that appear to be from open eyes
        if (currentEAR > 0.15f) {
            normalEAR = (normalEAR * calibrationFrames + currentEAR) / (calibrationFrames + 1);
            calibrationFrames++;
            
            if (calibrationFrames >= CALIBRATION_FRAMES_NEEDED) {
                isCalibrated = true;
                Log.d(TAG, "EAR calibration complete. Normal EAR: " + normalEAR);
            }
        }
    }
    
    /**
     * Update the blink state machine
     * 
     * @return True if a blink was detected and completed in this frame
     */
    private boolean updateBlinkState(boolean leftEyeClosed, boolean rightEyeClosed, boolean strongEvidence) {
        boolean blinkDetected = false;
        
        // Current eye state
        boolean currentlyBlinking = strongEvidence;
        
        if (debugMode) {
            Log.d(TAG, String.format("Blink state - Currently: %s, IsBlinking: %s, FrameCount: %d", 
                currentlyBlinking, isBlinking, blinkFrameCount));
        }
        
        if (!isBlinking && currentlyBlinking) {
            // Blink started
            isBlinking = true;
            blinkFrameCount = 1;
            if (debugMode) {
                Log.d(TAG, "Blink started");
            }
        } else if (isBlinking && currentlyBlinking) {
            // Blink continuing
            blinkFrameCount++;
            if (debugMode) {
                Log.d(TAG, "Blink continuing. Frame count: " + blinkFrameCount);
            }
        } else if (isBlinking && !currentlyBlinking) {
            // Blink ended - check if it was a valid blink
            if (blinkFrameCount >= BLINK_FRAME_THRESHOLD && blinkFrameCount <= MAX_BLINK_DURATION) {
                totalBlinks++;
                blinkDetected = true;
                
                if (debugMode) {
                    Log.d(TAG, "Valid blink detected! Frame count: " + blinkFrameCount + ", Total blinks: " + totalBlinks);
                }
                
                // Check for intentional blink pattern
                long now = System.currentTimeMillis();
                if (now - lastBlinkTimestamp < INTENTIONAL_BLINK_INTERVAL_MS) {
                    consecutiveBlinkCount++;
                    if (consecutiveBlinkCount >= INTENTIONAL_BLINK_COUNT) {
                        if (callback != null) {
                            callback.onIntentionalBlink(consecutiveBlinkCount);
                        }
                        Log.d(TAG, "Intentional blink detected with " + consecutiveBlinkCount + " blinks");
                        consecutiveBlinkCount = 0;
                    }
                } else {
                    consecutiveBlinkCount = 1;
                }
                lastBlinkTimestamp = now;
                
                Log.d(TAG, "Blink detected. Total blinks: " + totalBlinks);
                if (callback != null) {
                    callback.onBlink(leftEyeClosed, rightEyeClosed);
                }
            } else {
                if (debugMode) {
                    Log.d(TAG, "Invalid blink duration. Frame count: " + blinkFrameCount + 
                          " (min: " + BLINK_FRAME_THRESHOLD + ", max: " + MAX_BLINK_DURATION + ")");
                }
            }
            
            // Reset blink state
            isBlinking = false;
            blinkFrameCount = 0;
        }
        
        return blinkDetected;
    }
    
    /**
     * Check if blink detector is calibrated
     */
    public boolean isCalibrated() {
        return isCalibrated;
    }

    /**
     * Reset the detector state
     */
    public void reset() {
        isBlinking = false;
        blinkFrameCount = 0;
        totalBlinks = 0;
        leftEyeEARHistory.clear();
        rightEyeEARHistory.clear();
        consecutiveBlinkCount = 0;
        lastBlinkTimestamp = 0;
    }
}
