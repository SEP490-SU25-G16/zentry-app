package vn.edu.fpt.zentryapp.faceid.data.service;

import android.graphics.PointF;
import android.util.Log;

import java.util.List;
import java.util.ArrayList;

/**
 * Head pose estimation using only MediaPipe facial landmarks
 * No OpenCV dependency - uses geometric calculations for head orientation
 * Provides LEFT/RIGHT/CENTER detection for face authentication challenges
 */
public class HeadPoseEstimation {
    private static final String TAG = "HeadPoseEstimation";
    
    // MediaPipe 468-point face mesh landmark indices for key facial features
    private static final int NOSE_TIP = 1;        // Nose tip
    private static final int CHIN = 152;          // Chin center
    private static final int LEFT_EYE_LEFT = 33;  // Left eye left corner  
    private static final int RIGHT_EYE_RIGHT = 362; // Right eye right corner
    private static final int LEFT_EYE_CENTER = 159; // Left eye center
    private static final int RIGHT_EYE_CENTER = 386; // Right eye center
    private static final int LEFT_MOUTH = 61;     // Left mouth corner
    private static final int RIGHT_MOUTH = 291;   // Right mouth corner
    
    // Head direction thresholds (based on geometric ratios)
    private static final double YAW_THRESHOLD = 0.15;   // Ratio threshold for LEFT/RIGHT
    private static final double SYMMETRY_THRESHOLD = 0.1; // Facial symmetry threshold
    
    // Smoothing parameters for stable detection
    private static final double SMOOTH_ALPHA = 0.3;
    private double emaAlpha = SMOOTH_ALPHA; // Exponential moving average alpha for legacy compatibility
    private double smoothedYawRatio = 0.0;
    private double smoothedSymmetryRatio = 0.0;
    
    // Image dimensions for landmark normalization
    private final int imageWidth;
    private final int imageHeight;
    
    // Current head orientation metrics
    private double currentYawRatio = 0.0;      // Left/right asymmetry ratio
    private double currentSymmetryRatio = 0.0;  // Facial symmetry ratio
    private String currentDirection = "CENTER";
    
    // Frame confidence tracking
    private int consecutiveFrames = 0;
    private String lastStableDirection = "CENTER";
    private static final int STABILITY_FRAMES = 3; // Frames needed for stable detection
    
    /**
     * Initialize head pose estimation with image dimensions
     * @param imageWidth Camera image width in pixels
     * @param imageHeight Camera image height in pixels
     */
    public HeadPoseEstimation(int imageWidth, int imageHeight) {
        this.imageWidth = imageWidth;
        this.imageHeight = imageHeight;
        
        Log.d(TAG, String.format("HeadPoseEstimation initialized: %dx%d (MediaPipe landmarks only)", 
                imageWidth, imageHeight));
    }
    /**
     * Estimate head pose from facial landmarks using geometric analysis
     * @param landmarks List of all face landmarks from MediaPipe (468 points)  
     * @return true if pose was estimated successfully
     */
    public boolean estimateHeadPose(List<PointF> landmarks) {
        if (landmarks == null || landmarks.size() < 468) {
            Log.w(TAG, "Insufficient landmarks for head pose estimation: " + 
                  (landmarks != null ? landmarks.size() : "null"));
            return false;
        }
        
        try {
            // Get key landmark points
            PointF noseTip = landmarks.get(NOSE_TIP);
            PointF chin = landmarks.get(CHIN);
            PointF leftEyeLeft = landmarks.get(LEFT_EYE_LEFT);
            PointF rightEyeRight = landmarks.get(RIGHT_EYE_RIGHT);
            PointF leftEyeCenter = landmarks.get(LEFT_EYE_CENTER);
            PointF rightEyeCenter = landmarks.get(RIGHT_EYE_CENTER);
            PointF leftMouth = landmarks.get(LEFT_MOUTH);
            PointF rightMouth = landmarks.get(RIGHT_MOUTH);
            
            // Calculate head direction using geometric ratios
            calculateHeadDirection(noseTip, chin, leftEyeLeft, rightEyeRight, 
                                 leftEyeCenter, rightEyeCenter, leftMouth, rightMouth);
            
            return true;
            
        } catch (Exception e) {
            Log.e(TAG, "Error in head pose estimation", e);
            return false;
        }
    }
    
    /**
     * Calculate head direction using facial landmark geometry
     */
    private void calculateHeadDirection(PointF noseTip, PointF chin, 
                                      PointF leftEyeLeft, PointF rightEyeRight,
                                      PointF leftEyeCenter, PointF rightEyeCenter,
                                      PointF leftMouth, PointF rightMouth) {
        
        // Method 1: Eye asymmetry analysis
        // When head turns left, left eye appears smaller/more profile
        // When head turns right, right eye appears smaller/more profile
        double leftEyeWidth = distance(leftEyeLeft, leftEyeCenter) * 2; // approximate
        double rightEyeWidth = distance(rightEyeCenter, rightEyeRight) * 2; // approximate
        double eyeAsymmetryRatio = (rightEyeWidth - leftEyeWidth) / (rightEyeWidth + leftEyeWidth);
        
        // Method 2: Face width analysis  
        // Distance from nose to left vs right face boundary
        double noseToLeftDistance = distance(noseTip, leftEyeLeft);
        double noseToRightDistance = distance(noseTip, rightEyeRight);
        double faceAsymmetryRatio = (noseToRightDistance - noseToLeftDistance) / 
                                  (noseToRightDistance + noseToLeftDistance);
        
        // Method 3: Mouth asymmetry
        double noseToLeftMouth = distance(noseTip, leftMouth);
        double noseToRightMouth = distance(noseTip, rightMouth);
        double mouthAsymmetryRatio = (noseToRightMouth - noseToLeftMouth) / 
                                   (noseToRightMouth + noseToLeftMouth);
        
        // Combine ratios for robust detection
        double combinedRatio = (eyeAsymmetryRatio + faceAsymmetryRatio + mouthAsymmetryRatio) / 3.0;
        
        // Apply smoothing
        currentYawRatio = SMOOTH_ALPHA * currentYawRatio + (1 - SMOOTH_ALPHA) * combinedRatio;
        
        // Determine direction based on combined ratio
        String detectedDirection;
        if (currentYawRatio > YAW_THRESHOLD) {
            detectedDirection = "LEFT";   // Head turned left (positive ratio means left eye narrower)
        } else if (currentYawRatio < -YAW_THRESHOLD) {
            detectedDirection = "RIGHT";  // Head turned right (negative ratio means right eye narrower)
        } else {
            detectedDirection = "CENTER"; // Head facing forward
        }
        
        // Apply stability filtering
        if (detectedDirection.equals(lastStableDirection)) {
            consecutiveFrames++;
        } else {
            consecutiveFrames = 1;
            lastStableDirection = detectedDirection;
        }
        
        // Update current direction only if stable
        if (consecutiveFrames >= STABILITY_FRAMES) {
            currentDirection = detectedDirection;
        }
        
        Log.d(TAG, String.format("Head direction: %s (ratio=%.3f, frames=%d)", 
              currentDirection, currentYawRatio, consecutiveFrames));
    }
    
    /**
     * Calculate Euclidean distance between two points
     */
    private double distance(PointF p1, PointF p2) {
        double dx = p1.x - p2.x;
        double dy = p1.y - p2.y;
        return Math.sqrt(dx * dx + dy * dy);
    }
    
    /**
     * Get the current head direction
     * @return "LEFT", "RIGHT", or "CENTER"
     */
    public String getHeadDirection() {
        return currentDirection;
    }
    
    /**
     * Check if head is facing forward (suitable for gaze detection)
     * @return true if head is facing forward
     */
    public boolean isFacingForward() {
        return "CENTER".equals(currentDirection);
    }
    
    /**
     * Get the current yaw ratio (for debugging)
     * Positive values indicate rightward head turn
     * Negative values indicate leftward head turn  
     * @return yaw asymmetry ratio
     */
    public double getYawRatio() {
        return currentYawRatio;
    }
    
    /**
     * Reset the pose estimation state
     */
    public void reset() {
        currentYawRatio = 0.0;
        currentSymmetryRatio = 0.0;
        smoothedYawRatio = 0.0;
        smoothedSymmetryRatio = 0.0;
        currentDirection = "CENTER";
        consecutiveFrames = 0;
        lastStableDirection = "CENTER";
        
        Log.d(TAG, "Head pose estimation reset");
    }
    
    // Legacy compatibility methods (return dummy values since we don't use Euler angles)
    public double getYaw() { return currentYawRatio * 45.0; } // Convert ratio to approximate degrees
    public double getPitch() { return 0.0; } // Not calculated in this simplified approach
    public double getRoll() { return 0.0; }  // Not calculated in this simplified approach
    
    /**
     * Set smoothing factor for EMA filtering
     */
    public void setSmoothingFactor(double alpha) {
        this.emaAlpha = Math.max(0.0, Math.min(1.0, alpha));
        Log.d(TAG, "Smoothing factor set to: " + this.emaAlpha);
    }
    
    /**
     * Reset smoothing state - stub method for compatibility
     */
    public void resetSmoothing() {
        Log.d(TAG, "Smoothing state reset");
    }
    
    /**
     * Log current head pose state for debugging
     */
    public void logCurrentState() {
        Log.i(TAG, "=== HEAD POSE STATE ===");
        Log.i(TAG, String.format("Current direction: %s", getHeadDirection()));
        Log.i(TAG, String.format("Facing forward: %b", isFacingForward()));
        Log.i(TAG, String.format("Yaw ratio: %.3f", currentYawRatio));
        Log.i(TAG, "======================");
    }
    
    /**
     * Release resources - stub method for compatibility 
     */
    public void release() {
        Log.d(TAG, "HeadPoseEstimation resources released");
    }
}
