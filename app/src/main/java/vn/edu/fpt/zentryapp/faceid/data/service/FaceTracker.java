package vn.edu.fpt.zentryapp.faceid.data.service;

import android.graphics.Rect;
import android.util.Log;

import java.util.LinkedList;
import java.util.Queue;

import lombok.Getter;

/**
 * Utility class for tracking face stability across multiple frames
 */
public class FaceTracker {
    private static final String TAG = "FaceTracker";
    
    // Constants
    private static final int DEFAULT_REQUIRED_STABLE_FRAMES = 20; // ~0.7 seconds at 30fps
    private static final float MAX_MOVEMENT_RATIO = 0.05f; // 5% of face width/height
    
    // State
    private final Queue<Rect> recentFaces;
    private final int requiredStableFrames;
    private int stableFrameCount = 0;
    /**
     * -- GETTER --
     *  Get the last stable face
     */
    @Getter
    private Rect lastStableFace = null;
    
    /**
     * Interface for face stability callbacks
     */
    public interface FaceStabilityCallback {
        void onFaceStabilizing(float progress);
        void onFaceStable(Rect stableFaceRect);
        void onFaceUnstable();
    }
    
    public FaceTracker() {
        this(DEFAULT_REQUIRED_STABLE_FRAMES);
    }
    
    public FaceTracker(int requiredStableFrames) {
        this.requiredStableFrames = requiredStableFrames;
        this.recentFaces = new LinkedList<>();
    }
    
    /**
     * Track a new face detection
     * @param faceRect Bounding box of detected face
     * @param callback Callback for stability updates
     * @return true if face is stable, false otherwise
     */
    public boolean trackFace(Rect faceRect, FaceStabilityCallback callback) {
        if (faceRect == null) {
            reset();
            callback.onFaceUnstable();
            return false;
        }
        
        // Add to recent faces queue
        recentFaces.add(new Rect(faceRect));
        if (recentFaces.size() > requiredStableFrames) {
            recentFaces.poll();
        }
        
        // Check if we have enough frames
        if (recentFaces.size() < requiredStableFrames) {
            stableFrameCount = 0;
            callback.onFaceStabilizing((float) recentFaces.size() / requiredStableFrames);
            return false;
        }
        
        // Check if face is stable
        if (isFaceStable()) {
            stableFrameCount++;
            
            // Calculate progress
            float progress = (float) stableFrameCount / requiredStableFrames;
            progress = Math.min(progress, 1.0f);
            
            if (stableFrameCount >= requiredStableFrames) {
                // Face is stable for required number of frames
                lastStableFace = new Rect(faceRect);
                callback.onFaceStable(lastStableFace);
                return true;
            } else {
                // Face is stabilizing
                callback.onFaceStabilizing(progress);
                return false;
            }
        } else {
            // Face is not stable
            stableFrameCount = 0;
            callback.onFaceUnstable();
            return false;
        }
    }
    
    /**
     * Check if the face is stable across recent frames
     */
    private boolean isFaceStable() {
        if (recentFaces.size() < 2) {
            return false;
        }
        
        // Get first face as reference
        Rect reference = recentFaces.peek();
        if (reference == null) {
            return false;
        }
        
        // Calculate maximum allowed movement
        int maxMovementX = (int) (reference.width() * MAX_MOVEMENT_RATIO);
        int maxMovementY = (int) (reference.height() * MAX_MOVEMENT_RATIO);
        
        // Check movement against all recent faces
        for (Rect face : recentFaces) {
            int deltaX = Math.abs(face.centerX() - reference.centerX());
            int deltaY = Math.abs(face.centerY() - reference.centerY());
            int deltaWidth = Math.abs(face.width() - reference.width());
            int deltaHeight = Math.abs(face.height() - reference.height());
            
            if (deltaX > maxMovementX || deltaY > maxMovementY || 
                    deltaWidth > maxMovementX || deltaHeight > maxMovementY) {
                return false;
            }
        }
        
        return true;
    }
    
    /**
     * Reset the tracker
     */
    public void reset() {
        recentFaces.clear();
        stableFrameCount = 0;
        lastStableFace = null;
    }

} 