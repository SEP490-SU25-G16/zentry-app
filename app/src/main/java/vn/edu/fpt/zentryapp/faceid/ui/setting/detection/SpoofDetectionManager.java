package vn.edu.fpt.zentryapp.faceid.ui.setting.detection;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Rect;
import android.util.Log;

import vn.edu.fpt.zentryapp.faceid.data.service.FaceSpoofDetector;
import vn.edu.fpt.zentryapp.faceid.data.service.FaceIdConfig;

public class SpoofDetectionManager {
    private static final String TAG = "SpoofDetectionManager";

    private final FaceIdConfig.AntiSpoofConfig config;
    private final FaceSpoofDetector detector;

    // Counters
    private int realStreak = 0;
    private int suspicionScore = 0;
    private static final int SUSPICION_THRESHOLD = 14; // was 10 - reduce false positives
    private static final int LIVENESS_CHALLENGE_SUSPICION_THRESHOLD = 6; // was 5

    // Liveness Challenge
    private boolean livenessChallengeActive = false;
    private int framesSinceChallengeStarted = 0;
    private static final int LIVENESS_CHALLENGE_DURATION = 120; // ~4 seconds for more user-friendly challenge
    private boolean blinkDetectedInChallenge = false;

    // Frame history for temporal analysis
    private static final int FRAME_HISTORY_SIZE = 20; // give more temporal context
    private final java.util.Queue<FrameData> frameHistory = new java.util.LinkedList<>();

    private android.graphics.RectF ovalBoundary;

    public static class SpoofDetectionResult {
        public final boolean isSpoof;
        public final float confidence;
        public final ConfidenceLevel confidenceLevel;
        public final String explanation;
        public final boolean shouldProceed;
        public final boolean triggerLivenessChallenge;

        public SpoofDetectionResult(boolean isSpoof, float confidence,
                                    ConfidenceLevel confidenceLevel, String explanation,
                                    boolean shouldProceed, boolean triggerLivenessChallenge) {
            this.isSpoof = isSpoof;
            this.confidence = confidence;
            this.confidenceLevel = confidenceLevel;
            this.explanation = explanation;
            this.shouldProceed = shouldProceed;
            this.triggerLivenessChallenge = triggerLivenessChallenge;
        }
    }

    private static class FrameData {
        final float confidence;
        final boolean isSpoof;

        FrameData(float confidence, boolean isSpoof) {
            this.confidence = confidence;
            this.isSpoof = isSpoof;
        }
    }

    public enum ConfidenceLevel {
        HIGH, MEDIUM, LOW, VERY_LOW
    }

    public interface SpoofDetectionCallback {
        void onResult(SpoofDetectionResult result);
    }

    public SpoofDetectionManager(FaceSpoofDetector detector, Context context) {
        this.detector = detector;
        this.config = new FaceIdConfig(context).getConfig().antiSpoofConfig;
    }

    public void setOvalBoundary(android.graphics.RectF ovalRect) {
        this.ovalBoundary = ovalRect;
    }

    public void analyzeFrame(Bitmap bitmap, Rect faceRect, SpoofDetectionCallback callback) {
        detector.detectSpoofAsync(bitmap, faceRect, ovalBoundary, rawResult -> {
            if (livenessChallengeActive) {
                framesSinceChallengeStarted++;
                if (checkForBlink(rawResult)) {
                    blinkDetectedInChallenge = true;
                }
                if (framesSinceChallengeStarted >= LIVENESS_CHALLENGE_DURATION) {
                    livenessChallengeActive = false;
                }
            }

            SpoofDetectionResult enhancedResult = enhanceDetectionResult(rawResult);
            callback.onResult(enhancedResult);
        });
    }

    private SpoofDetectionResult enhanceDetectionResult(FaceSpoofDetector.SpoofResult rawResult) {
        updateFrameHistory(rawResult.getScore(), rawResult.isSpoof());
        return makeSecureDecision(rawResult.isSpoof(), rawResult.getScore());
    }

    private SpoofDetectionResult makeSecureDecision(boolean rawIsSpoof, float rawConfidence) {
        ConfidenceLevel level = getConfidenceLevel(rawConfidence);

        if (livenessChallengeActive) {
            return handleLivenessChallenge(rawConfidence, level);
        }

        updateSuspicionScore(rawIsSpoof, rawConfidence);

        if (suspicionScore >= SUSPICION_THRESHOLD) {
            return new SpoofDetectionResult(true, rawConfidence, level, "High suspicion of spoofing.", false, false);
        }

        if (suspicionScore >= LIVENESS_CHALLENGE_SUSPICION_THRESHOLD) {
            startLivenessChallenge();
            return new SpoofDetectionResult(false, rawConfidence, level, "Suspicious activity detected. Please blink.", false, true);
        }

        if (!rawIsSpoof && rawConfidence > config.highConfidenceThreshold) {
            realStreak++;
            if (realStreak >= config.minRealFaceFrames) {
                return new SpoofDetectionResult(false, rawConfidence, level, "Real face detected.", true, false);
            }
        } else {
            realStreak = 0;
        }

        return new SpoofDetectionResult(false, rawConfidence, level, "Hold steady.", false, false);
    }

    private SpoofDetectionResult handleLivenessChallenge(float rawConfidence, ConfidenceLevel level) {
        if (blinkDetectedInChallenge) {
            livenessChallengeActive = false;
            suspicionScore = 0; // Reset suspicion after successful liveness check
            return new SpoofDetectionResult(false, rawConfidence, level, "Liveness confirmed.", true, false);
        }
        if (framesSinceChallengeStarted >= LIVENESS_CHALLENGE_DURATION) {
            livenessChallengeActive = false;
            return new SpoofDetectionResult(true, rawConfidence, level, "Liveness check failed.", false, false);
        }
            return new SpoofDetectionResult(false, rawConfidence, level, "Blink your eyes.", false, true);
    }

    private void updateSuspicionScore(boolean rawIsSpoof, float rawConfidence) {
        if (rawIsSpoof) {
            suspicionScore += 2;
        } else if (rawConfidence < (config.lowConfidenceThreshold - 0.05f)) { // be less sensitive
            suspicionScore++;
        } else {
            suspicionScore = Math.max(0, suspicionScore - 1); // Decrease suspicion for real faces
        }

        if (checkForAbnormalConfidencePattern()) {
            suspicionScore += 3;
        }
    }

    private boolean checkForAbnormalConfidencePattern() {
        if (frameHistory.size() < FRAME_HISTORY_SIZE) {
            return false;
        }
        // Simplified check for abnormal patterns
        float confidenceVariance = calculateConfidenceVariance();
        return confidenceVariance < 0.0005f || confidenceVariance > 0.15f; // widen bounds
    }

    private float calculateConfidenceVariance() {
        if (frameHistory.size() < 2) {
            return 0.0f;
        }
        float sum = 0;
        float sumSq = 0;
        for (FrameData frame : frameHistory) {
            sum += frame.confidence;
            sumSq += frame.confidence * frame.confidence;
        }
        float mean = sum / frameHistory.size();
        return (sumSq / frameHistory.size()) - (mean * mean);
    }

    private void updateFrameHistory(float confidence, boolean isSpoof) {
        frameHistory.add(new FrameData(confidence, isSpoof));
        if (frameHistory.size() > FRAME_HISTORY_SIZE) {
            frameHistory.poll();
        }
    }

    private ConfidenceLevel getConfidenceLevel(float confidence) {
        if (confidence >= config.highConfidenceThreshold) return ConfidenceLevel.HIGH;
        if (confidence >= config.mediumConfidenceThreshold) return ConfidenceLevel.MEDIUM;
        if (confidence >= config.lowConfidenceThreshold) return ConfidenceLevel.LOW;
        return ConfidenceLevel.VERY_LOW;
    }

    public void reset() {
        realStreak = 0;
        suspicionScore = 0;
        livenessChallengeActive = false;
        frameHistory.clear();
    }

    /**
     * Reset only the liveness challenge related state, keeping historical frames if needed
     */
    public void resetLivenessState() {
        livenessChallengeActive = false;
        framesSinceChallengeStarted = 0;
        blinkDetectedInChallenge = false;
    }

    /**
     * Mark liveness challenge as successful and clear suspicion
     */
    public void markLivenessSuccess() {
        livenessChallengeActive = false;
        framesSinceChallengeStarted = 0;
        blinkDetectedInChallenge = false;
        suspicionScore = 0;
    }

    private void startLivenessChallenge() {
        if (!livenessChallengeActive) {
            livenessChallengeActive = true;
            framesSinceChallengeStarted = 0;
            blinkDetectedInChallenge = false;
        }
    }

    private boolean checkForBlink(FaceSpoofDetector.SpoofResult rawResult) {
        if (frameHistory.size() < 3) return false;
        FrameData[] frames = frameHistory.toArray(new FrameData[0]);
        FrameData currentFrame = new FrameData(rawResult.getScore(), rawResult.isSpoof());
        FrameData lastFrame = frames[frames.length - 1];
        FrameData preLastFrame = frames[frames.length - 2];
        return !currentFrame.isSpoof && !lastFrame.isSpoof && !preLastFrame.isSpoof &&
               currentFrame.confidence > 0.6 && preLastFrame.confidence > 0.6 &&
               lastFrame.confidence < 0.3;
    }
}