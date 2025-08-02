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

    // 🔧 BALANCED ANTI-SPOOFING THRESHOLDS
    private static final float HIGH_CONFIDENCE_THRESHOLD = 0.75f;    // High but achievable threshold
    private static final float MEDIUM_CONFIDENCE_THRESHOLD = 0.55f;  // More reasonable medium threshold
    private static final float LOW_CONFIDENCE_THRESHOLD = 0.35f;     // Lower threshold for poor lighting

    private static final float REAL_SPOOF_RATIO_STRICT = 1.7f;  // Real must be 70% higher than Spoof
    private static final float REAL_SPOOF_RATIO_LENIENT = 1.3f; // Real must be 30% higher than Spoof

    // Streak counting with reasonable requirements
    private static final int SPOOF_CONFIRMATION_LIMIT = 2;     // Need 2 frames to confirm spoof
    private static final int REAL_CONFIRMATION_LIMIT = 2;      // Only need 2 frames to confirm real
    private static final int MAX_SPOOF_WARNINGS = 3;           // Allow proceeding after 3 warnings
    private static final int CONSECUTIVE_FRAME_REQUIREMENT = 2; // Only need 2 consistent frames

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

        // Check for possible replay attack patterns
        boolean possibleReplayAttack = false;
        if (lastResult != null) {
            // Check for rapid classification flips (indicative of 2D replay)
            // This is now less sensitive and requires more evidence
            boolean classificationFlipping =
                    (lastResult.isSpoof != rawIsSpoof) &&
                            (spoofStreak <= 1 && realStreak <= 1) &&
                            lowConfidenceCount > 5 &&                  // Require more low confidence frames
                            Math.abs(lastResult.confidence - rawConfidence) < 0.1f; // And with similar confidence

            // Look for suspiciously stable confidence (static images)
            // Real faces show more variation than printed images - this is now much less aggressive
            float confidenceDelta = Math.abs(lastResult.confidence - rawConfidence);
            boolean suspiciouslyStableConfidence =
                    confidenceDelta < 0.005f &&   // Make this much stricter - almost no change
                            realStreak > 8 &&            // Require many more frames of stability
                            level != ConfidenceLevel.HIGH; // Don't flag high confidence real faces

            // Disable medium confidence plateauing entirely
            boolean mediumConfidenceStuck = false;

            // Make replay attack detection much less aggressive
            possibleReplayAttack = (classificationFlipping && spoofStreak > 0) ||
                    (suspiciouslyStableConfidence && spoofStreak > 2);

            if (possibleReplayAttack) {
                Log.d(TAG, "⚠️ REPLAY ATTACK WARNING: flip=" + classificationFlipping +
                        ", stableConf=" + suspiciouslyStableConfidence +
                        ", mediumStuck=" + mediumConfidenceStuck);
            }
        }

        // 🟢 HIGH CONFIDENCE CASES
        if (level == ConfidenceLevel.HIGH) {
            if (!rawIsSpoof && !possibleReplayAttack) {
                // High confidence real face, no replay attack indicators
                finalIsSpoof = false;
                // Need several consecutive confirmations
                shouldProceed = realStreak >= REAL_CONFIRMATION_LIMIT;
                explanation = "High confidence real face detected";
            } else {
                // High confidence spoof or replay attack indicators
                finalIsSpoof = true;
                shouldProceed = false;
                explanation = possibleReplayAttack ?
                        "Possible replay attack detected - please use real face" :
                        "High confidence spoof detected";
            }
        }
        // 🟡 MEDIUM CONFIDENCE CASES - MORE LENIENT FOR REAL FACES
        else if (level == ConfidenceLevel.MEDIUM) {
            // For medium confidence, default to real face unless clear spoof indicators
            if (!rawIsSpoof || (rawConfidence > MEDIUM_CONFIDENCE_THRESHOLD * 0.9 && !possibleReplayAttack)) {
                // Medium confidence real with no replay indicators or borderline cases
                finalIsSpoof = false;

                // Less strict streak requirement for medium confidence real faces
                shouldProceed = realStreak >= REAL_CONFIRMATION_LIMIT;
                explanation = "Medium confidence real face - hold steady";
            } else if (rawIsSpoof && rawConfidence > MEDIUM_CONFIDENCE_THRESHOLD * 1.1 && possibleReplayAttack) {
                // Only flag as spoof if both the detector says spoof AND we have replay attack indicators
                // AND the confidence is well above the threshold
                finalIsSpoof = true;
                shouldProceed = false;
                explanation = "Medium confidence spoof detected with replay patterns";
            } else {
                // For borderline cases, favor real face but with warning
                finalIsSpoof = false;
                shouldProceed = realStreak >= REAL_CONFIRMATION_LIMIT + 1; // Still require more confirmation
                explanation = "Borderline detection - try to improve lighting and stay still";
            }
        }
        // 🔴 LOW CONFIDENCE CASES - MORE LENIENT
        else {
            // Low confidence - try to be more permissive while maintaining security
            lowConfidenceCount++;

            if (lowConfidenceCount > 10 && !rawIsSpoof) {
                // Allow proceeding sooner if not flagged as spoof even in poor conditions
                // This greatly helps users in poor lighting conditions
                finalIsSpoof = false;
                shouldProceed = realStreak >= 1;
                explanation = "Low confidence but likely real face - proceeding with caution";
            } else if (lowConfidenceCount > 15) {
                // After many attempts, default to allowing with warning
                finalIsSpoof = false;
                shouldProceed = true;
                explanation = "Low confidence - proceeding due to persistent poor conditions";
            } else if (!rawIsSpoof && lowConfidenceCount > 5) {
                // If not flagged as spoof and we've seen several frames
                finalIsSpoof = false;
                shouldProceed = false; // Still don't proceed yet
                explanation = "Low confidence but likely real - please improve lighting or position";
            } else {
                // Only for initial low confidence frames with spoof indication
                finalIsSpoof = true;
                shouldProceed = false;
                explanation = "Low confidence detection, please improve lighting or camera position";
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