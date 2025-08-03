package vn.edu.fpt.zentryapp.student.ui.setting.detection;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Rect;
import android.util.Log;

import vn.edu.fpt.zentryapp.student.data.service.FaceSpoofDetector;
import vn.edu.fpt.zentryapp.student.data.service.FaceIdConfig;

public class SpoofDetectionManager {
    private static final String TAG = "SpoofDetectionManager";

    // 🔧 NEW: Configuration-based thresholds
    private final FaceIdConfig.AntiSpoofConfig config;

    private final FaceSpoofDetector detector;

    // Counters
    private int spoofStreak = 0;
    private int realStreak = 0;
    private int spoofWarningCount = 0;
    private int lowConfidenceCount = 0;
    
    // 🆕 NEW REAL FACE TRACKING
    private int realFaceRecoveryCount = 0;
    private float lastRealFaceConfidence = 0.0f;
    private boolean hasOvalBoundary = false;

    // Frame history for temporal analysis
    private static final int FRAME_HISTORY_SIZE = 8;
    private final java.util.Queue<FrameData> frameHistory = new java.util.LinkedList<>();

    // Last result for comparison
    private SpoofDetectionResult lastResult;
    
    // Oval boundaries for validation
    private android.graphics.RectF ovalBoundary;

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
    
    /**
     * Frame data for temporal analysis
     */
    private static class FrameData {
        final float confidence;
        final boolean isSpoof;
        final long timestamp;
        
        FrameData(float confidence, boolean isSpoof) {
            this.confidence = confidence;
            this.isSpoof = isSpoof;
            this.timestamp = System.currentTimeMillis();
        }
    }

    public enum ConfidenceLevel {
        HIGH,    // > 85%
        MEDIUM,  // 70% - 85%
        LOW,     // 50% - 70%
        VERY_LOW // < 50%
    }

    public interface SpoofDetectionCallback {
        void onResult(SpoofDetectionResult result);
    }

    public SpoofDetectionManager(FaceSpoofDetector detector, Context context) {
        this.detector = detector;
        this.config = new FaceIdConfig(context).getConfig().antiSpoofConfig;
    }
    
    public SpoofDetectionManager(FaceSpoofDetector detector, FaceIdConfig.AntiSpoofConfig config) {
        this.detector = detector;
        this.config = config;
    }
    
    /**
     * Set oval boundary for face validation
     */
    public void setOvalBoundary(android.graphics.RectF ovalRect) {
        this.ovalBoundary = ovalRect;
        this.hasOvalBoundary = (ovalRect != null);
        Log.d(TAG, "Oval boundary set: " + ovalRect + ", hasOvalBoundary: " + hasOvalBoundary);
    }

    /**
     * Analyze spoof detection result with enhanced security logic
     */
    public void analyzeFrame(Bitmap bitmap, Rect faceRect, SpoofDetectionCallback callback) {
        // Use oval boundary for validation if available
        detector.detectSpoofAsync(bitmap, faceRect, ovalBoundary, rawResult -> {
            SpoofDetectionResult enhancedResult = enhanceDetectionResult(rawResult);

            Log.d(TAG, String.format("🔍 Detection: %s (conf: %.2f, level: %s) - %s, proceed=%b",
                    enhancedResult.isSpoof ? "SPOOF" : "REAL",
                    enhancedResult.confidence,
                    enhancedResult.confidenceLevel,
                    enhancedResult.explanation,
                    enhancedResult.shouldProceed));

            callback.onResult(enhancedResult);
        });
    }

    /**
     * Legacy method for backward compatibility
     */
    public void analyzeFrame(Bitmap bitmap, Rect faceRect, android.graphics.RectF ovalRect, SpoofDetectionCallback callback) {
        setOvalBoundary(ovalRect);
        analyzeFrame(bitmap, faceRect, callback);
    }

    /**
     * Enhance raw detection result with streak tracking and security improvements
     */
    private SpoofDetectionResult enhanceDetectionResult(FaceSpoofDetector.SpoofResult rawResult) {
        boolean isSpoof = rawResult.isSpoof();
        float confidence = rawResult.getScore();

        // Store frame data for temporal analysis
        updateFrameHistory(confidence, isSpoof);

        // Determine confidence level
        ConfidenceLevel level = getConfidenceLevel(confidence);

        // 🔧 ENHANCED MULTI-LAYER SECURITY LOGIC
        SpoofDetectionResult result = makeSecureDecision(isSpoof, confidence, level);

        // Update streaks
        updateStreaks(result);

        // Store last result
        lastResult = result;

        return result;
    }

    /**
     * Enhanced decision logic with better real face detection
     */
    private SpoofDetectionResult makeSecureDecision(boolean rawIsSpoof, float rawConfidence, ConfidenceLevel level) {
        String explanation;
        boolean finalIsSpoof;
        boolean shouldProceed = false;

        // Check for replay attack patterns
        boolean possibleReplayAttack = checkForReplayAttack(rawIsSpoof, rawConfidence);

        // Check for abnormal confidence patterns
        boolean suspiciousConfidencePattern = checkForAbnormalConfidencePattern();
        
        // 🆕 NEW: Check for natural movement patterns (real face indicator)
        boolean hasNaturalMovement = checkForNaturalMovement(rawConfidence);
        
        // 🆕 NEW: Check for oval boundary compliance (security indicator)
        boolean isOvalCompliant = hasOvalBoundary && ovalBoundary != null;
        
        // Debug logging
        Log.d(TAG, String.format("🔍 Decision: rawIsSpoof=%b, confidence=%.4f, level=%s, realStreak=%d, replayAttack=%b, suspiciousPattern=%b, naturalMovement=%b, ovalCompliant=%b",
                rawIsSpoof, rawConfidence, level, realStreak, possibleReplayAttack, suspiciousConfidencePattern, hasNaturalMovement, isOvalCompliant));
        
        // 🔧 NEW: Use configurable thresholds
        // 🟢 HIGH CONFIDENCE CASES - More lenient for real faces
        if (level == ConfidenceLevel.HIGH) {
            if (!rawIsSpoof) {
                // High confidence real face - trust the model more
                finalIsSpoof = false;
                // More lenient - only need 2 consecutive confirmations for high confidence
                shouldProceed = realStreak >= config.minRealFaceFrames;
                explanation = "High confidence real face detected" + 
                    (shouldProceed ? "" : " - need " + (config.minRealFaceFrames - realStreak) + " more frames");
            } else {
                // High confidence spoof
                finalIsSpoof = true;
                shouldProceed = false;
                explanation = "High confidence spoof detected";
            }
        }
        // 🟡 MEDIUM CONFIDENCE CASES - Much more lenient for real faces
        else if (level == ConfidenceLevel.MEDIUM) {
            if (!rawIsSpoof && !possibleReplayAttack && !suspiciousConfidencePattern) {
                // Medium confidence real with no suspicious indicators
                finalIsSpoof = false;
                // 🆕 IMPROVED: More lenient for real faces with natural movement
                if (hasNaturalMovement && isOvalCompliant) {
                    shouldProceed = realStreak >= 1; // Only need 1 frame if natural movement detected
                    explanation = "Medium confidence real face with natural movement - proceed";
                } else {
                    shouldProceed = realStreak >= config.minRealFaceFrames; // Need configurable frames otherwise
                    explanation = "Medium confidence real face - hold steady" +
                        (shouldProceed ? "" : " - need " + (config.minRealFaceFrames - realStreak) + " more frames");
                }
            } else if (rawIsSpoof && !possibleReplayAttack && !suspiciousConfidencePattern) {
                // Medium confidence spoof but no suspicious patterns
                finalIsSpoof = true;
                shouldProceed = false;
                explanation = "Medium confidence spoof detected";
            } else {
                // Any suspicious indicators with medium confidence leads to spoof
                finalIsSpoof = true;
                shouldProceed = false;
                if (possibleReplayAttack) {
                    explanation = "Possible replay attack detected with medium confidence";
                } else if (suspiciousConfidencePattern) {
                    explanation = "Suspicious pattern detected - please try again";
                } else {
                    explanation = "Medium confidence spoof detected";
                }
            }
        }
        // 🔴 LOW AND VERY LOW CONFIDENCE CASES - More lenient with recovery
        else {
            lowConfidenceCount++;

            // 🆕 IMPROVED: Better recovery logic for real faces
            if (lowConfidenceCount > 10 && !rawIsSpoof && !possibleReplayAttack && !suspiciousConfidencePattern) {
                // More lenient - only need configurable consecutive confirmations for low confidence
                finalIsSpoof = false;
                shouldProceed = realStreak >= config.minRealFaceFrames;
                explanation = "Low confidence detection - please improve lighting or camera position";
            } else if (hasNaturalMovement && isOvalCompliant && !rawIsSpoof) {
                // 🆕 NEW: Allow real face with natural movement even at low confidence
                finalIsSpoof = false;
                shouldProceed = realStreak >= config.minRealFaceFrames;
                explanation = "Real face detected with natural movement";
            } else {
                // Default to spoof for low confidence
                finalIsSpoof = true;
                shouldProceed = false;
                if (possibleReplayAttack || suspiciousConfidencePattern) {
                    explanation = "Suspicious pattern detected with low confidence";
                } else {
                    explanation = "Low confidence detection, please improve lighting or camera position";
                }
            }
        }

        Log.d(TAG, String.format("🎯 Final decision: isSpoof=%b, shouldProceed=%b, explanation=%s",
                finalIsSpoof, shouldProceed, explanation));

        return new SpoofDetectionResult(finalIsSpoof, rawConfidence, level, explanation, shouldProceed);
    }
    
    /**
     * Check for replay attack patterns using temporal analysis
     */
    private boolean checkForReplayAttack(boolean currentIsSpoof, float currentConfidence) {
        if (frameHistory.size() < 4) {
            return false; // Not enough data
        }
        
        // Check for rapid classification flips (indicative of 2D replay)
        int classificationFlips = 0;
        boolean lastWasSpoof = false;
        boolean firstItem = true;
        
        for (FrameData frame : frameHistory) {
            if (firstItem) {
                lastWasSpoof = frame.isSpoof;
                firstItem = false;
                continue;
            }
            
            if (frame.isSpoof != lastWasSpoof) {
                classificationFlips++;
            }
            lastWasSpoof = frame.isSpoof;
        }
        
        // More lenient - only detect if there are many rapid flips
        boolean suspiciousFlips = classificationFlips >= 4; // Increased from 3
        
        // Check for suspiciously stable confidence (indicative of replay)
        float confidenceVariance = calculateConfidenceVariance();
        boolean suspiciouslyStableConfidence = confidenceVariance < 0.005f; // More lenient - was 0.003f
        
        // Check for unrealistic confidence patterns
        boolean unrealisticConfidence = false;
        if (frameHistory.size() >= 6) {
            int highConfidenceCount = 0;
            int lowConfidenceCount = 0;
            
            for (FrameData frame : frameHistory) {
                if (frame.confidence > 0.95f) {
                    highConfidenceCount++;
                } else if (frame.confidence < 0.05f) {
                    lowConfidenceCount++;
                }
            }
            
            // More lenient - only detect if there are extreme patterns
            unrealisticConfidence = (highConfidenceCount >= 5 && lowConfidenceCount == 0) ||
                                  (lowConfidenceCount >= 5 && highConfidenceCount == 0);
        }
        
        boolean isReplayAttack = suspiciousFlips || (suspiciouslyStableConfidence && unrealisticConfidence);
        
        Log.d(TAG, String.format("📊 REPLAY CHECK: flips=%d, confVar=%.6f, stable=%b, unrealistic=%b, result=%b",
                classificationFlips, confidenceVariance, suspiciouslyStableConfidence, unrealisticConfidence, isReplayAttack));
        
        return isReplayAttack;
    }
    
    /**
     * Check for abnormal confidence patterns that indicate spoofing attempts
     */
    private boolean checkForAbnormalConfidencePattern() {
        if (frameHistory.size() < 6) {
            return false; // Not enough data
        }
        
        // Check for "staircase" pattern (gradually increasing/decreasing confidence)
        // This can indicate algorithmic manipulation attempts
        boolean isStaircase = true;
        boolean increasing = true;
        float lastConfidence = -1;
        boolean firstItem = true;
        
        for (FrameData frame : frameHistory) {
            if (firstItem) {
                lastConfidence = frame.confidence;
                firstItem = false;
                continue;
            }
            
            if (lastConfidence != -1) {
                if (frame.confidence > lastConfidence) {
                    if (firstItem) {
                        increasing = true;
                        firstItem = false;
                    } else if (!increasing) {
                        isStaircase = false;
                        break;
                    }
                } else if (frame.confidence < lastConfidence) {
                    if (firstItem) {
                        increasing = false;
                        firstItem = false;
                    } else if (increasing) {
                        isStaircase = false;
                        break;
                    }
                }
            }
            lastConfidence = frame.confidence;
        }
        
        // Check for "plateau" pattern (very stable confidence)
        float confidenceVariance = calculateConfidenceVariance();
        boolean isPlateau = confidenceVariance < 0.002f; // More lenient - was 0.001f
        
        // Check for "oscillation" pattern (alternating high/low confidence)
        boolean isOscillating = false;
        if (frameHistory.size() >= 8) {
            int oscillationCount = 0;
            boolean lastWasHigh = false;

            
            for (FrameData frame : frameHistory) {
                boolean currentIsHigh = frame.confidence > 0.7f;
                
                if (!firstItem && currentIsHigh != lastWasHigh) {
                    oscillationCount++;
                }
                
                lastWasHigh = currentIsHigh;
                firstItem = false;
            }
            
            // More lenient - only detect if there are many oscillations
            isOscillating = oscillationCount >= 6; // Increased from 4
        }
        
        // Check for "spike" pattern (sudden extreme confidence changes)
        boolean hasSpikes = false;
        if (frameHistory.size() >= 4) {
            float maxChange = 0;
            
            for (FrameData frame : frameHistory) {
                if (lastConfidence != -1) {
                    float change = Math.abs(frame.confidence - lastConfidence);
                    maxChange = Math.max(maxChange, change);
                }
                lastConfidence = frame.confidence;
            }
            
            // More lenient - only detect if there are extreme spikes
            hasSpikes = maxChange > 0.8f; // Increased from 0.6f
        }
        
        boolean abnormalPattern = isStaircase || isPlateau || isOscillating || hasSpikes;
        
        Log.d(TAG, String.format("📊 PATTERN ANALYSIS: flips=%d, confVariance=%.6f, abnormal=%b",
                frameHistory.size(), confidenceVariance, abnormalPattern));
        
        return abnormalPattern;
    }
    
    /**
     * Detect cyclic patterns in confidence values
     */
    private boolean detectCyclicPattern() {
        if (frameHistory.size() < 8) {
            return false;
        }
        
        // Convert queue to array for easier indexing
        FrameData[] frames = frameHistory.toArray(new FrameData[0]);
        
        // Check for simple repetition pattern of length 2
        boolean twoFrameCycle = true;
        for (int i = 0; i < frames.length - 2; i += 2) {
            if (Math.abs(frames[i].confidence - frames[i+2].confidence) > 0.02f) {
                twoFrameCycle = false;
                break;
            }
        }
        
        // Check for simple repetition pattern of length 3
        boolean threeFrameCycle = true;
        if (frames.length >= 6) {
            for (int i = 0; i < frames.length - 3; i += 3) {
                if (Math.abs(frames[i].confidence - frames[i+3].confidence) > 0.02f) {
                    threeFrameCycle = false;
                    break;
                }
            }
        }
        
        return twoFrameCycle || threeFrameCycle;
    }
    
    /**
     * Calculate variance in confidence values
     */
    private float calculateConfidenceVariance() {
        if (frameHistory.size() < 2) {
            return 0.01f; // Default value if not enough data
        }
        
        float sum = 0;
        float sumSq = 0;
        int count = 0;
        
        for (FrameData frame : frameHistory) {
            sum += frame.confidence;
            sumSq += frame.confidence * frame.confidence;
            count++;
        }
        
        float mean = sum / count;
        float variance = (sumSq / count) - (mean * mean);
        
        return variance;
    }
    
    /**
     * Update frame history for temporal analysis
     */
    private void updateFrameHistory(float confidence, boolean isSpoof) {
        frameHistory.add(new FrameData(confidence, isSpoof));
        if (frameHistory.size() > FRAME_HISTORY_SIZE) {
            frameHistory.poll();
        }
    }

    /**
     * 🆕 NEW: Check for natural movement patterns (real face indicator)
     */
    private boolean checkForNaturalMovement(float currentConfidence) {
        if (frameHistory.size() < 3) {
            return false; // Not enough data
        }
        
        // Calculate confidence variance
        float variance = calculateConfidenceVariance();
        
        // 🔧 NEW: Use configurable threshold for natural movement
        boolean hasNaturalVariance = variance > 0.01f && variance < config.naturalMovementThreshold;
        
        // Check for gradual confidence changes (natural) vs sudden changes (replay)
        boolean hasGradualChanges = true;
        float lastConfidence = lastRealFaceConfidence;
        
        for (FrameData frame : frameHistory) {
            float confidenceDiff = Math.abs(frame.confidence - lastConfidence);
            if (confidenceDiff > 0.3f) { // Sudden large changes indicate replay
                hasGradualChanges = false;
                break;
            }
            lastConfidence = frame.confidence;
        }
        
        return hasNaturalVariance && hasGradualChanges;
    }

    /**
     * 🆕 IMPROVED: Update streak counters with better recovery logic
     */
    private void updateStreaks(SpoofDetectionResult result) {
        if (result.isSpoof) {
            spoofStreak++;
            realStreak = 0;
            realFaceRecoveryCount = 0; // Reset recovery

            if (result.confidenceLevel == ConfidenceLevel.HIGH || result.confidenceLevel == ConfidenceLevel.MEDIUM) {
                spoofWarningCount++;
            }
        } else {
            realStreak++;
            spoofStreak = 0;
            lastRealFaceConfidence = result.confidence;

            // 🆕 IMPROVED: Better recovery logic for real faces
            if (result.confidenceLevel == ConfidenceLevel.HIGH) {
                spoofWarningCount = Math.max(0, spoofWarningCount - 2); // Faster recovery
                lowConfidenceCount = Math.max(0, lowConfidenceCount - 3); // Much faster recovery
                realFaceRecoveryCount++;
            } else if (result.confidenceLevel == ConfidenceLevel.MEDIUM) {
                spoofWarningCount = Math.max(0, spoofWarningCount - 1); // Normal recovery
                lowConfidenceCount = Math.max(0, lowConfidenceCount - 2); // Faster recovery
                realFaceRecoveryCount++;
            } else {
                // Low confidence real face - slower recovery
                lowConfidenceCount = Math.max(0, lowConfidenceCount - 1);
                realFaceRecoveryCount++;
            }
        }

        Log.d(TAG, String.format("📊 Streaks: Real=%d, Spoof=%d, Warnings=%d, LowConf=%d, Recovery=%d",
                realStreak, spoofStreak, spoofWarningCount, lowConfidenceCount, realFaceRecoveryCount));
    }

    /**
     * Get confidence level from score
     */
    private ConfidenceLevel getConfidenceLevel(float confidence) {
        if (confidence >= config.highConfidenceThreshold) {
            return ConfidenceLevel.HIGH;
        } else if (confidence >= config.mediumConfidenceThreshold) {
            return ConfidenceLevel.MEDIUM;
        } else if (confidence >= config.lowConfidenceThreshold) {
            return ConfidenceLevel.LOW;
        } else {
            return ConfidenceLevel.VERY_LOW;
        }
    }

    /**
     * Reset all counters and security state
     */
    public void reset() {
        spoofStreak = 0;
        realStreak = 0;
        spoofWarningCount = 0;
        lowConfidenceCount = 0;
        // 🆕 NEW: Reset real face tracking
        realFaceRecoveryCount = 0;
        lastRealFaceConfidence = 0.0f;
        frameHistory.clear();
        lastResult = null;
        Log.d(TAG, "Detection manager reset");
    }

    /**
     * Get current streak info for debugging
     */
    public String getStreakInfo() {
        return String.format("Real: %d, Spoof: %d, Warnings: %d, Recovery: %d",
                realStreak, spoofStreak, spoofWarningCount, realFaceRecoveryCount);
    }
}