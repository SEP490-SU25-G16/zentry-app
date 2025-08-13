package vn.edu.fpt.zentryapp.faceid.data.service;

import android.graphics.Rect;
import android.util.Log;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;

/**
 * Enhanced temporal variance analyzer for improved liveness detection.
 * This class analyzes face movement patterns over time to distinguish real faces from spoofs.
 * It's designed to work with the FaceSpoofDetector and FaceAnalysisController for better integration.
 */
public class TemporalVarianceAnalyzer {
    private static final String TAG = "TemporalVarianceAnalyzer";
    
    // Tuned thresholds for better real face detection
    private static final float MIN_POSITION_VARIANCE = 0.00005f; // Reduced to catch micro-movements
    private static final float MAX_POSITION_VARIANCE = 0.10f;    // Increased to allow natural movement
    private static final float MIN_SIZE_VARIANCE = 0.00005f;     // Reduced to catch micro-movements
    private static final float MAX_SIZE_VARIANCE = 0.08f;        // Increased to allow natural movement
    
    // Frame history for temporal analysis - increased for better statistical significance
    private static final int DEFAULT_FRAME_HISTORY_SIZE = 12;
    private final Queue<FrameData> frameHistory;
    private final int maxFrameHistorySize;

    /**
     * Class to track temporal data for analysis
     */
    public static class FrameData {
        final float[] modelResults;
        final Rect faceRect;
        final long timestamp;
        final float confidence;
        
        public FrameData(float[] modelResults, Rect faceRect, float confidence) {
            this.modelResults = modelResults != null ? modelResults.clone() : null;
            this.faceRect = new Rect(faceRect);
            this.timestamp = System.currentTimeMillis();
            this.confidence = confidence;
        }
    }
    
    /**
     * Result of temporal variance analysis
     */
    public static class TemporalVarianceResult {
        private final boolean hasNaturalMovement;
        private final float positionVariance;
        private final float sizeVariance;
        private final float classificationStability;
        private final boolean hasAbnormalPattern;
        
        public TemporalVarianceResult(boolean hasNaturalMovement, float positionVariance, 
                                     float sizeVariance, float classificationStability,
                                     boolean hasAbnormalPattern) {
            this.hasNaturalMovement = hasNaturalMovement;
            this.positionVariance = positionVariance;
            this.sizeVariance = sizeVariance;
            this.classificationStability = classificationStability;
            this.hasAbnormalPattern = hasAbnormalPattern;
        }
        
        public boolean hasNaturalMovement() {
            return hasNaturalMovement;
        }
        
        public float getPositionVariance() {
            return positionVariance;
        }
        
        public float getSizeVariance() {
            return sizeVariance;
        }
        
        public float getClassificationStability() {
            return classificationStability;
        }
        
        public boolean hasAbnormalPattern() {
            return hasAbnormalPattern;
        }
    }
    
    /**
     * Create a temporal variance analyzer with default frame history size
     */
    public TemporalVarianceAnalyzer() {
        this(DEFAULT_FRAME_HISTORY_SIZE);
    }
    
    /**
     * Create a temporal variance analyzer with custom frame history size
     */
    public TemporalVarianceAnalyzer(int frameHistorySize) {
        this.frameHistory = new LinkedList<>();
        this.maxFrameHistorySize = frameHistorySize;
    }
    
    /**
     * Add a new frame to history
     */
    public void addFrame(float[] modelResults, Rect faceRect, float confidence) {
        FrameData currentFrame = new FrameData(modelResults, faceRect, confidence);
        frameHistory.add(currentFrame);
        if (frameHistory.size() > maxFrameHistorySize) {
            frameHistory.poll();
        }
    }
    
    /**
     * Clear frame history
     */
    public void clearHistory() {
        frameHistory.clear();
    }
    
    /**
     * Get current frame history size
     */
    public int getHistorySize() {
        return frameHistory.size();
    }
    
    /**
     * Check if we have enough history for analysis
     */
    public boolean hasEnoughHistory(int minFrames) {
        return frameHistory.size() >= minFrames;
    }
    
    /**
     * Calculate position variance across frames
     */
    public float calculatePositionVariance() {
        if (frameHistory.size() < 2) {
            return 0.01f; // Default value if not enough data
        }
        
        float sumX = 0;
        float sumY = 0;
        float sumSqX = 0;
        float sumSqY = 0;
        int count = 0;
        
        for (FrameData frame : frameHistory) {
            float centerX = frame.faceRect.exactCenterX();
            float centerY = frame.faceRect.exactCenterY();
            
            sumX += centerX;
            sumY += centerY;
            sumSqX += centerX * centerX;
            sumSqY += centerY * centerY;
            count++;
        }
        
        float meanX = sumX / count;
        float meanY = sumY / count;
        float varianceX = (sumSqX / count) - (meanX * meanX);
        float varianceY = (sumSqY / count) - (meanY * meanY);
        
        // Normalize by face size
        FrameData lastFrame = getLastFrame();
        if (lastFrame != null) {
            float faceSize = Math.max(lastFrame.faceRect.width(), lastFrame.faceRect.height());
            varianceX /= (faceSize * faceSize);
            varianceY /= (faceSize * faceSize);
        }
        
        return (varianceX + varianceY) / 2;
    }
    
    /**
     * Calculate size variance across frames
     */
    public float calculateSizeVariance() {
        if (frameHistory.size() < 2) {
            return 0.005f; // Default value if not enough data
        }
        
        float sumW = 0;
        float sumH = 0;
        float sumSqW = 0;
        float sumSqH = 0;
        int count = 0;
        
        for (FrameData frame : frameHistory) {
            float width = frame.faceRect.width();
            float height = frame.faceRect.height();
            
            sumW += width;
            sumH += height;
            sumSqW += width * width;
            sumSqH += height * height;
            count++;
        }
        
        float meanW = sumW / count;
        float meanH = sumH / count;
        float varianceW = (sumSqW / count) - (meanW * meanW);
        float varianceH = (sumSqH / count) - (meanH * meanH);
        
        // Normalize by face size
        FrameData lastFrame = getLastFrame();
        if (lastFrame != null) {
            float faceWidth = lastFrame.faceRect.width();
            float faceHeight = lastFrame.faceRect.height();
            varianceW /= (faceWidth * faceWidth);
            varianceH /= (faceHeight * faceHeight);
        }
        
        return (varianceW + varianceH) / 2;
    }
    
    /**
     * Calculate confidence variance across frames
     */
    public float calculateConfidenceVariance() {
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
     * Check for abnormal classification patterns (like rapid flips between real/spoof)
     */
    public boolean checkAbnormalPattern() {
        if (frameHistory.size() < 4) {
            return false; // Not enough data
        }
        
        // Convert queue to list for easier processing
        List<FrameData> frames = new ArrayList<>(frameHistory);
        
        // Count classification flips (real->spoof->real) - more sophisticated analysis
        int classificationFlips = 0;
        boolean lastWasReal = false;
        boolean currentIsReal = false;
        
        // We need model results for this analysis
        if (frames.get(0).modelResults == null) {
            return false;
        }
        
        for (int i = 1; i < frames.size(); i++) {
            FrameData prevFrame = frames.get(i-1);
            FrameData currFrame = frames.get(i);
            
            // Skip if model results missing
            if (prevFrame.modelResults == null || currFrame.modelResults == null) {
                continue;
            }
            
            // Assuming index 1 is real probability, find max of 0 and 2 for spoof
            float prevReal = prevFrame.modelResults[1];
            float prevSpoof = Math.max(prevFrame.modelResults[0], prevFrame.modelResults[2]);
            float currReal = currFrame.modelResults[1];
            float currSpoof = Math.max(currFrame.modelResults[0], currFrame.modelResults[2]);
            
            lastWasReal = prevReal > prevSpoof;
            currentIsReal = currReal > currSpoof;
            
            // Check for flip with significant margin (to filter minor fluctuations)
            if (lastWasReal != currentIsReal && 
                Math.abs(prevReal - prevSpoof) > 0.15f && 
                Math.abs(currReal - currSpoof) > 0.15f) {
                classificationFlips++;
            }
        }
        
        // Calculate confidence stability
        float confidenceVariance = calculateConfidenceVariance();
        boolean suspiciouslyStableConfidence = confidenceVariance < 0.0008f;
        
        // More sophisticated pattern detection
        boolean abnormalPattern = (classificationFlips > 2) || 
                               (suspiciouslyStableConfidence && classificationFlips > 0);
        
        Log.d(TAG, "Pattern analysis: flips=" + classificationFlips + 
              ", confVariance=" + confidenceVariance + 
              ", abnormal=" + abnormalPattern);
        
        return abnormalPattern;
    }
    
    /**
     * Get the last frame from history
     */
    private FrameData getLastFrame() {
        if (frameHistory.isEmpty()) {
            return null;
        }
        
        // Convert queue to array and get last element
        return frameHistory.toArray(new FrameData[0])[frameHistory.size() - 1];
    }
    
    /**
     * Analyze temporal variance in face position and size
     * @param minFrames Minimum frames required for analysis
     * @return Temporal variance analysis result
     */
    public TemporalVarianceResult analyze(int minFrames) {
        if (frameHistory.size() < minFrames) {
            // Not enough data, provide default analysis
            return new TemporalVarianceResult(
                true, // Assume natural movement when insufficient data
                0.01f, 
                0.005f, 
                0.01f,
                false
            );
        }
        
        // Calculate variances
        float positionVariance = calculatePositionVariance();
        float sizeVariance = calculateSizeVariance();
        float confidenceVariance = calculateConfidenceVariance();
        boolean abnormalPattern = checkAbnormalPattern();
        
        // Determine if movement is natural
        boolean hasNaturalMovement = positionVariance >= MIN_POSITION_VARIANCE && 
                                   positionVariance <= MAX_POSITION_VARIANCE &&
                                   sizeVariance >= MIN_SIZE_VARIANCE &&
                                   sizeVariance <= MAX_SIZE_VARIANCE;
        
        Log.d(TAG, String.format("Temporal analysis: posVar=%.5f, sizeVar=%.5f, confVar=%.5f, natural=%b, abnormal=%b",
               positionVariance, sizeVariance, confidenceVariance, hasNaturalMovement, abnormalPattern));
        
        return new TemporalVarianceResult(
            hasNaturalMovement,
            positionVariance,
            sizeVariance,
            confidenceVariance,
            abnormalPattern
        );
    }
    
    /**
     * Custom analyze method with scenario-specific thresholds
     */
    public TemporalVarianceResult analyzeWithScenario(FaceIdConfig.Scenario scenario, boolean livenessVerified) {
        // Adjust thresholds based on scenario and liveness status
        float minPosVar, maxPosVar, minSizeVar, maxSizeVar;
        int minRequiredFrames;
        
        switch (scenario) {
            case REGISTRATION:
                // More lenient for registration, especially with liveness
                minPosVar = livenessVerified ? 0.00001f : 0.00003f;
                maxPosVar = livenessVerified ? 0.15f : 0.12f;
                minSizeVar = livenessVerified ? 0.00001f : 0.00003f;
                maxSizeVar = livenessVerified ? 0.12f : 0.10f;
                minRequiredFrames = livenessVerified ? 2 : 3;
                break;
                
            case VERIFICATION:
                // Standard thresholds for verification
                minPosVar = livenessVerified ? 0.00002f : 0.00005f;
                maxPosVar = livenessVerified ? 0.12f : 0.10f;
                minSizeVar = livenessVerified ? 0.00002f : 0.00005f;
                maxSizeVar = livenessVerified ? 0.10f : 0.08f;
                minRequiredFrames = livenessVerified ? 3 : 4;
                break;
                
            case SECURITY_CHECK:
                // More strict for security checks
                minPosVar = livenessVerified ? 0.00005f : 0.0001f;
                maxPosVar = livenessVerified ? 0.10f : 0.08f;
                minSizeVar = livenessVerified ? 0.00005f : 0.0001f;
                maxSizeVar = livenessVerified ? 0.08f : 0.06f;
                minRequiredFrames = livenessVerified ? 4 : 5;
                break;
                
            default:
                // Default thresholds
                minPosVar = MIN_POSITION_VARIANCE;
                maxPosVar = MAX_POSITION_VARIANCE;
                minSizeVar = MIN_SIZE_VARIANCE;
                maxSizeVar = MAX_SIZE_VARIANCE;
                minRequiredFrames = 3;
        }
        
        if (frameHistory.size() < minRequiredFrames) {
            // Not enough data for this scenario
            return new TemporalVarianceResult(
                true, // Assume natural movement when insufficient data
                0.01f, 
                0.005f, 
                0.01f,
                false
            );
        }
        
        // Calculate variances
        float positionVariance = calculatePositionVariance();
        float sizeVariance = calculateSizeVariance();
        float confidenceVariance = calculateConfidenceVariance();
        boolean abnormalPattern = checkAbnormalPattern();
        
        // Determine if movement is natural based on scenario-specific thresholds
        boolean hasNaturalMovement = positionVariance >= minPosVar && 
                                   positionVariance <= maxPosVar &&
                                   sizeVariance >= minSizeVar &&
                                   sizeVariance <= maxSizeVar;
        
        // If liveness is verified, be more lenient with the natural movement check
        if (livenessVerified && !hasNaturalMovement) {
            // Just outside thresholds but liveness verified - still consider natural
            if (positionVariance <= maxPosVar * 1.5f && 
                sizeVariance <= maxSizeVar * 1.5f) {
                hasNaturalMovement = true;
                Log.d(TAG, "Allowing borderline movement due to liveness verification");
            }
        }
        
        Log.d(TAG, String.format("Scenario (%s) temporal analysis: posVar=%.5f [%.5f-%.5f], sizeVar=%.5f [%.5f-%.5f], " +
                "confVar=%.5f, natural=%b, abnormal=%b, liveness=%b",
               scenario, positionVariance, minPosVar, maxPosVar, sizeVariance, minSizeVar, maxSizeVar,
               confidenceVariance, hasNaturalMovement, abnormalPattern, livenessVerified));
        
        return new TemporalVarianceResult(
            hasNaturalMovement,
            positionVariance,
            sizeVariance,
            confidenceVariance,
            abnormalPattern
        );
    }
}
