package vn.edu.fpt.zentryapp.faceid.ui.common;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Rect;
import android.graphics.RectF;
import android.util.Log;

import vn.edu.fpt.zentryapp.faceid.data.service.FaceSpoofDetector;
import vn.edu.fpt.zentryapp.faceid.ui.setting.detection.SpoofDetectionManager;

/**
 * Controller for handling spoof detection logic
 */
public class FaceIdSpoofDetectionController {
    private static final String TAG = "SpoofDetectionController";
    
    private final Context context;
    private FaceSpoofDetector faceSpoofDetector;
    private SpoofDetectionManager spoofDetectionManager;
    private RectF ovalBoundary;
    
    public FaceIdSpoofDetectionController(Context context) {
        this.context = context;
    }
    
    /**
     * Initialize with face spoof detector and oval boundary
     */
    public void initialize(FaceSpoofDetector faceSpoofDetector, RectF ovalBoundary) {
        this.faceSpoofDetector = faceSpoofDetector;
        this.ovalBoundary = ovalBoundary;
        
        if (faceSpoofDetector != null) {
            spoofDetectionManager = new SpoofDetectionManager(faceSpoofDetector, context);
            if (ovalBoundary != null) {
                spoofDetectionManager.setOvalBoundary(ovalBoundary);
            }
        }
    }
    
    /**
     * Set the oval boundary for face validation
     */
    public void setOvalBoundary(RectF ovalBoundary) {
        this.ovalBoundary = ovalBoundary;
        if (spoofDetectionManager != null) {
            spoofDetectionManager.setOvalBoundary(ovalBoundary);
        }
    }
    
    /**
     * Analyze frame for spoof detection
     */
    public void analyzeFrame(Bitmap bitmap, Rect faceRect, SpoofResultCallback callback) {
        if (spoofDetectionManager == null) {
            Log.w(TAG, "SpoofDetectionManager not initialized");
            SpoofDetectionResult result = new SpoofDetectionResult(false, false, 
                    "SpoofDetectionManager not initialized", false);
            callback.onSpoofResult(result);
            return;
        }
        
        spoofDetectionManager.analyzeFrame(bitmap, faceRect, result -> {
            callback.onSpoofResult(convertResult(result));
        });
    }
    
    /**
     * Convert SpoofDetectionManager result to our internal format
     */
    private SpoofDetectionResult convertResult(SpoofDetectionManager.SpoofDetectionResult managerResult) {
        return new SpoofDetectionResult(
                managerResult.isSpoof,
                managerResult.shouldProceed,
                managerResult.explanation,
                managerResult.triggerLivenessChallenge
        );
    }
    
    /**
     * Result class for spoof detection
     */
    public static class SpoofDetectionResult {
        public final boolean isSpoof;
        public final boolean shouldProceed;
        public final String explanation;
        public final boolean triggerLivenessChallenge;
        
        public SpoofDetectionResult(boolean isSpoof, boolean shouldProceed, String explanation,
                                    boolean triggerLivenessChallenge) {
            this.isSpoof = isSpoof;
            this.shouldProceed = shouldProceed;
            this.explanation = explanation;
            this.triggerLivenessChallenge = triggerLivenessChallenge;
        }
    }
    
    /**
     * Callback interface for spoof detection results
     */
    public interface SpoofResultCallback {
        void onSpoofResult(SpoofDetectionResult result);
    }
}
