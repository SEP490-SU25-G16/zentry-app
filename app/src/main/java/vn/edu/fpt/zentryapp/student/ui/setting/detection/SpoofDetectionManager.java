package vn.edu.fpt.zentryapp.student.ui.setting.detection;

import android.graphics.Bitmap;
import android.graphics.Rect;
import android.util.Log;

import vn.edu.fpt.zentryapp.student.data.service.FaceSpoofDetector;

/**
 * Wrapper cho Spoof Detection với improved logic và consistent thresholds
 * Fix các vấn đề về confidence inconsistency
 */
public class SpoofDetectionManager {
    private static final String TAG = "SpoofDetectionManager";
    
    // 🔧 UNIFIED CONFIDENCE THRESHOLDS
    private static final float HIGH_CONFIDENCE_THRESHOLD = 0.75f;    // Chắc chắn
    private static final float MEDIUM_CONFIDENCE_THRESHOLD = 0.55f;  // Khá chắc
    private static final float LOW_CONFIDENCE_THRESHOLD = 0.35f;     // Không chắc
    
    private static final float REAL_SPOOF_RATIO_STRICT = 1.8f;  // Real phải cao hơn Spoof 80%
    private static final float REAL_SPOOF_RATIO_LENIENT = 1.3f; // Real phải cao hơn Spoof 30%
    
    // Streak counting for stability
    private static final int SPOOF_CONFIRMATION_LIMIT = 3;
    private static final int REAL_CONFIRMATION_LIMIT = 2;
    private static final int MAX_SPOOF_WARNINGS = 5; // Sau 5 lần warning, allow user continue
    
    private final FaceSpoofDetector detector;
    
    // Counters
    private int spoofStreak = 0;
    private int realStreak = 0;
    private int spoofWarningCount = 0;
    private int lowConfidenceCount = 0;
    
    // Last result for comparison
    private SpoofDetectionResult lastResult;
    
    public static class SpoofDetectionResult {
        public final boolean isSpoof;
        public final float confidence;
        public final ConfidenceLevel confidenceLevel;
        public final String explanation;
        public final boolean shouldProceed; // Có nên tiếp tục không
        
        public SpoofDetectionResult(boolean isSpoof, float confidence, 
                                  ConfidenceLevel confidenceLevel, String explanation, 
                                  boolean shouldProceed) {
            this.isSpoof = isSpoof;
            this.confidence = confidence;
            this.confidenceLevel = confidenceLevel;
            this.explanation = explanation;
            this.shouldProceed = shouldProceed;
        }
    }
    
    public enum ConfidenceLevel {
        HIGH,    // > 75%
        MEDIUM,  // 55% - 75% 
        LOW,     // 35% - 55%
        VERY_LOW // < 35%
    }
    
    public interface SpoofDetectionCallback {
        void onResult(SpoofDetectionResult result);
    }
    
    public SpoofDetectionManager(FaceSpoofDetector detector) {
        this.detector = detector;
    }
    
    /**
     * Analyze spoof detection result với improved logic
     */
    public void analyzeFrame(Bitmap bitmap, Rect faceRect, SpoofDetectionCallback callback) {
        detector.detectSpoofAsync(bitmap, faceRect, rawResult -> {
            SpoofDetectionResult enhancedResult = enhanceDetectionResult(rawResult);
            
            Log.d(TAG, String.format("🔍 Detection: %s (conf: %.2f, level: %s) - %s", 
                enhancedResult.isSpoof ? "SPOOF" : "REAL",
                enhancedResult.confidence,
                enhancedResult.confidenceLevel,
                enhancedResult.explanation));
            
            callback.onResult(enhancedResult);
        });
    }
    
    /**
     * Enhance raw detection result với streak tracking và logical improvements
     */
    private SpoofDetectionResult enhanceDetectionResult(FaceSpoofDetector.SpoofResult rawResult) {
        boolean isSpoof = rawResult.isSpoof();
        float confidence = rawResult.getScore();
        
        // Determine confidence level
        ConfidenceLevel level = getConfidenceLevel(confidence);
        
        // 🔧 IMPROVED LOGIC: Multi-criteria decision
        SpoofDetectionResult result = makeSmartDecision(isSpoof, confidence, level);
        
        // Update streaks
        updateStreaks(result);
        
        // Store last result
        lastResult = result;
        
        return result;
    }
    
    /**
     * Make smart decision based on multiple criteria
     */
    private SpoofDetectionResult makeSmartDecision(boolean rawIsSpoof, float rawConfidence, ConfidenceLevel level) {
        String explanation;
        boolean finalIsSpoof;
        boolean shouldProceed = false;
        
        // 🟢 HIGH CONFIDENCE CASES
        if (level == ConfidenceLevel.HIGH) {
            if (!rawIsSpoof) {
                // High confidence real face
                finalIsSpoof = false;
                shouldProceed = realStreak >= REAL_CONFIRMATION_LIMIT - 1; // Almost ready
                explanation = "High confidence real face detected";
            } else {
                // High confidence spoof
                finalIsSpoof = true;
                shouldProceed = false;
                explanation = "High confidence spoof detected";
            }
        }
        // 🟡 MEDIUM CONFIDENCE CASES 
        else if (level == ConfidenceLevel.MEDIUM) {
            if (!rawIsSpoof && rawConfidence > MEDIUM_CONFIDENCE_THRESHOLD) {
                // Medium confidence real
                finalIsSpoof = false;
                shouldProceed = realStreak >= REAL_CONFIRMATION_LIMIT;
                explanation = "Medium confidence real face";
            } else if (rawIsSpoof && rawConfidence > MEDIUM_CONFIDENCE_THRESHOLD) {
                // Medium confidence spoof
                finalIsSpoof = true;
                shouldProceed = spoofWarningCount >= MAX_SPOOF_WARNINGS; // Allow after too many warnings
                explanation = spoofWarningCount >= MAX_SPOOF_WARNINGS ? 
                    "Spoof detected but allowing due to repeated warnings" : 
                    "Medium confidence spoof detected";
            } else {
                // Unclear medium confidence
                finalIsSpoof = rawIsSpoof;
                shouldProceed = false;
                explanation = "Unclear detection, please adjust position";
            }
        }
        // 🔴 LOW CONFIDENCE CASES
        else {
            // Low confidence - be conservative
            lowConfidenceCount++;
            
            if (lowConfidenceCount > 10) {
                // Too many low confidence frames - might be lighting issue
                finalIsSpoof = false;
                shouldProceed = true; // Allow user to proceed
                explanation = "Low confidence - proceeding due to poor conditions";
            } else {
                finalIsSpoof = rawIsSpoof;
                shouldProceed = false;
                explanation = "Low confidence detection, improve lighting";
            }
        }
        
        return new SpoofDetectionResult(finalIsSpoof, rawConfidence, level, explanation, shouldProceed);
    }
    
    /**
     * Update streak counters
     */
    private void updateStreaks(SpoofDetectionResult result) {
        if (result.isSpoof) {
            spoofStreak++;
            realStreak = 0;
            
            if (result.confidenceLevel == ConfidenceLevel.HIGH || result.confidenceLevel == ConfidenceLevel.MEDIUM) {
                spoofWarningCount++;
            }
        } else {
            realStreak++;
            spoofStreak = 0;
            
            // Reset warning count on good real face detection
            if (result.confidenceLevel == ConfidenceLevel.HIGH) {
                spoofWarningCount = Math.max(0, spoofWarningCount - 1);
                lowConfidenceCount = 0;
            }
        }
        
        Log.d(TAG, String.format("📊 Streaks: Real=%d, Spoof=%d, Warnings=%d, LowConf=%d", 
            realStreak, spoofStreak, spoofWarningCount, lowConfidenceCount));
    }
    
    /**
     * Get confidence level from score
     */
    private ConfidenceLevel getConfidenceLevel(float confidence) {
        if (confidence >= HIGH_CONFIDENCE_THRESHOLD) {
            return ConfidenceLevel.HIGH;
        } else if (confidence >= MEDIUM_CONFIDENCE_THRESHOLD) {
            return ConfidenceLevel.MEDIUM;
        } else if (confidence >= LOW_CONFIDENCE_THRESHOLD) {
            return ConfidenceLevel.LOW;
        } else {
            return ConfidenceLevel.VERY_LOW;
        }
    }
    
    /**
     * Reset all counters
     */
    public void reset() {
        spoofStreak = 0;
        realStreak = 0;
        spoofWarningCount = 0;
        lowConfidenceCount = 0;
        lastResult = null;
        Log.d(TAG, "Detection manager reset");
    }
    
    /**
     * Get current streak info for debugging
     */
    public String getStreakInfo() {
        return String.format("Real: %d, Spoof: %d, Warnings: %d", 
            realStreak, spoofStreak, spoofWarningCount);
    }
}