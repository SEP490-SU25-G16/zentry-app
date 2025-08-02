package vn.edu.fpt.zentryapp.student.ui.setting.detection;

import android.graphics.Bitmap;
import android.graphics.Rect;
import android.util.Log;

import vn.edu.fpt.zentryapp.student.data.service.FaceSpoofDetector;

public class SpoofDetectionManager {
    private static final String TAG = "SpoofDetectionManager";

    // 🔧 ENHANCED ANTI-SPOOFING THRESHOLDS
    private static final float HIGH_CONFIDENCE_THRESHOLD = 0.85f;    // Increased from 0.75f
    private static final float MEDIUM_CONFIDENCE_THRESHOLD = 0.70f;  // Increased from 0.55f
    private static final float LOW_CONFIDENCE_THRESHOLD = 0.50f;     // Increased from 0.35f

    private static final float REAL_SPOOF_RATIO_STRICT = 2.0f;  // Increased from 1.7f
    private static final float REAL_SPOOF_RATIO_LENIENT = 1.5f; // Increased from 1.3f

    // Streak counting with stricter requirements
    private static final int SPOOF_CONFIRMATION_LIMIT = 2;      // No change - 2 frames to confirm spoof
    private static final int REAL_CONFIRMATION_LIMIT = 5;      // Increased from 2 to 5 frames
    private static final int MAX_SPOOF_WARNINGS = 3;           // No change - 3 warnings max
    private static final int CONSECUTIVE_FRAME_REQUIREMENT = 4; // Increased from 2 to 4 consistent frames

    private final FaceSpoofDetector detector;

    // Counters
    private int spoofStreak = 0;
    private int realStreak = 0;
    private int spoofWarningCount = 0;
    private int lowConfidenceCount = 0;

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

    public SpoofDetectionManager(FaceSpoofDetector detector) {
        this.detector = detector;
    }
    
    /**
     * Set oval boundary for face validation
     */
    public void setOvalBoundary(android.graphics.RectF ovalRect) {
        this.ovalBoundary = ovalRect;
        Log.d(TAG, "Oval boundary set: " + ovalRect);
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
     * Make secure decision based on multiple criteria with enhanced security
     */
    private SpoofDetectionResult makeSecureDecision(boolean rawIsSpoof, float rawConfidence, ConfidenceLevel level) {
        String explanation;
        boolean finalIsSpoof;
        boolean shouldProceed = false;

        // Check for replay attack patterns
        boolean possibleReplayAttack = checkForReplayAttack(rawIsSpoof, rawConfidence);

        // Check for abnormal confidence patterns
        boolean suspiciousConfidencePattern = checkForAbnormalConfidencePattern();
        
        // Debug logging
        Log.d(TAG, String.format("🔍 Decision: rawIsSpoof=%b, confidence=%.4f, level=%s, realStreak=%d, replayAttack=%b, suspiciousPattern=%b",
                rawIsSpoof, rawConfidence, level, realStreak, possibleReplayAttack, suspiciousConfidencePattern));
        
        // 🟢 HIGH CONFIDENCE CASES - Much more lenient for real faces
        if (level == ConfidenceLevel.HIGH) {
            if (!rawIsSpoof) {
                // High confidence real face - trust the model more
                finalIsSpoof = false;
                // Much more lenient - only need 2 consecutive confirmations for high confidence
                shouldProceed = realStreak >= 2;
                explanation = "High confidence real face detected" + 
                    (shouldProceed ? "" : " - need " + (2 - realStreak) + " more frames");
            } else {
                // High confidence spoof
                finalIsSpoof = true;
                shouldProceed = false;
                explanation = "High confidence spoof detected";
            }
        }
        // 🟡 MEDIUM CONFIDENCE CASES - More lenient
        else if (level == ConfidenceLevel.MEDIUM) {
            if (!rawIsSpoof && !possibleReplayAttack && !suspiciousConfidencePattern) {
                // Medium confidence real with no suspicious indicators
                finalIsSpoof = false;
                // More lenient - only need 3 consecutive confirmations
                shouldProceed = realStreak >= 3;
                explanation = "Medium confidence real face - hold steady" +
                    (shouldProceed ? "" : " - need " + (3 - realStreak) + " more frames");
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
        // 🔴 LOW AND VERY LOW CONFIDENCE CASES - More lenient
        else {
            lowConfidenceCount++;

            if (lowConfidenceCount > 15 && !rawIsSpoof && !possibleReplayAttack && !suspiciousConfidencePattern) {
                // More lenient - only need 4 consecutive confirmations for low confidence
                finalIsSpoof = false;
                shouldProceed = realStreak >= 4;
                explanation = "Low confidence detection - please improve lighting or camera position";
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
                lowConfidenceCount = Math.max(0, lowConfidenceCount - 2); // Faster recovery
            } else if (result.confidenceLevel == ConfidenceLevel.MEDIUM) {
                lowConfidenceCount = Math.max(0, lowConfidenceCount - 1); // Slower recovery
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
     * Reset all counters and security state
     */
    public void reset() {
        spoofStreak = 0;
        realStreak = 0;
        spoofWarningCount = 0;
        lowConfidenceCount = 0;
        frameHistory.clear();
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