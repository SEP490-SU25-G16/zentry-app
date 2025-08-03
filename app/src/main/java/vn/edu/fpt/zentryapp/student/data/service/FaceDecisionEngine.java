package vn.edu.fpt.zentryapp.student.data.service;

import android.graphics.Rect;

import android.util.Log;

/**
 * Engine for making face anti-spoofing decisions
 * Separates decision logic from FaceIdService for better maintainability
 */
public class FaceDecisionEngine {
    private static final String TAG = "FaceDecisionEngine";
    
    private final FaceDecisionConfig config;
    
    public FaceDecisionEngine(FaceDecisionConfig config) {
        this.config = config;
    }
    
    /**
     * Evaluate face detection result and make decision
     */
    public FaceDecisionResult evaluate(FaceDetectionResult detection, 
                                     SpoofDetectionResult spoof, 
                                     OvalValidationResult oval) {
        Log.d(TAG, "Evaluating face decision - Detection: " + detection + 
              ", Spoof: " + spoof + ", Oval: " + oval);
        
        // Check if face is properly positioned
        if (oval != null && !oval.isValid()) {
            return new FaceDecisionResult(
                DecisionType.REJECT,
                "Please position your face within the oval guide",
                DecisionReason.OVAL_VIOLATION
            );
        }
        
        // High confidence real face - trust the model
        if (spoof.getConfidence() >= config.getHighConfidenceThreshold() && !spoof.isSpoof()) {
            return new FaceDecisionResult(
                DecisionType.ACCEPT,
                "High confidence real face detected",
                DecisionReason.HIGH_CONFIDENCE_REAL
            );
        }
        
        // Strong spoof detected - reject immediately
        if (spoof.isSpoof() && spoof.getConfidence() >= config.getStrongSpoofThreshold()) {
            return new FaceDecisionResult(
                DecisionType.REJECT,
                "Spoof detected! Please use a real face.",
                DecisionReason.STRONG_SPOOF
            );
        }
        
        // Medium confidence real face - more lenient
        if (!spoof.isSpoof() && spoof.getConfidence() >= config.getMediumConfidenceThreshold()) {
            return new FaceDecisionResult(
                DecisionType.ACCEPT,
                "Real face detected with acceptable confidence",
                DecisionReason.MEDIUM_CONFIDENCE_REAL
            );
        }
        
        // Face within oval but low spoof confidence - allow with caution
        if (oval != null && oval.isValid() && spoof.isSpoof() && 
            spoof.getConfidence() < config.getLowSpoofThreshold()) {
            return new FaceDecisionResult(
                DecisionType.ACCEPT,
                "Face within oval, allowing despite low spoof confidence",
                DecisionReason.OVAL_COMPLIANT_LOW_SPOOF
            );
        }
        
        // Low confidence cases - provide guidance
        if (spoof.getConfidence() < config.getLowConfidenceThreshold()) {
            return new FaceDecisionResult(
                DecisionType.GUIDANCE,
                "Low confidence detection - please improve lighting and position",
                DecisionReason.LOW_CONFIDENCE
            );
        }
        
        // Uncertain cases - give benefit of doubt if within oval
        if (oval != null && oval.isValid()) {
            return new FaceDecisionResult(
                DecisionType.ACCEPT,
                "Unclear verification but face properly positioned - proceeding",
                DecisionReason.OVAL_COMPLIANT_UNCLEAR
            );
        }
        
        // Default rejection
        return new FaceDecisionResult(
            DecisionType.REJECT,
            "Please position your face properly within the oval.",
            DecisionReason.DEFAULT_REJECTION
        );
    }
    
    /**
     * Configuration for face decision thresholds
     */
    public static class FaceDecisionConfig {
        private final float highConfidenceThreshold;
        private final float mediumConfidenceThreshold;
        private final float lowConfidenceThreshold;
        private final float strongSpoofThreshold;
        private final float lowSpoofThreshold;
        
        public FaceDecisionConfig(float highConfidenceThreshold,
                                float mediumConfidenceThreshold,
                                float lowConfidenceThreshold,
                                float strongSpoofThreshold,
                                float lowSpoofThreshold) {
            this.highConfidenceThreshold = highConfidenceThreshold;
            this.mediumConfidenceThreshold = mediumConfidenceThreshold;
            this.lowConfidenceThreshold = lowConfidenceThreshold;
            this.strongSpoofThreshold = strongSpoofThreshold;
            this.lowSpoofThreshold = lowSpoofThreshold;
        }
        
        // Getters
        public float getHighConfidenceThreshold() { return highConfidenceThreshold; }
        public float getMediumConfidenceThreshold() { return mediumConfidenceThreshold; }
        public float getLowConfidenceThreshold() { return lowConfidenceThreshold; }
        public float getStrongSpoofThreshold() { return strongSpoofThreshold; }
        public float getLowSpoofThreshold() { return lowSpoofThreshold; }
        
        // Default configuration
        public static FaceDecisionConfig getDefault() {
            return new FaceDecisionConfig(0.85f, 0.60f, 0.40f, 0.85f, 0.70f);
        }
    }
    
    /**
     * Result of face detection
     */
    public static class FaceDetectionResult {
        private final boolean isDetected;
        private final Rect boundingBox;
        private final float confidence;
        
        public FaceDetectionResult(boolean isDetected, Rect boundingBox, float confidence) {
            this.isDetected = isDetected;
            this.boundingBox = boundingBox;
            this.confidence = confidence;
        }
        
        public boolean isDetected() { return isDetected; }
        public Rect getBoundingBox() { return boundingBox; }
        public float getConfidence() { return confidence; }
        
        @Override
        public String toString() {
            return "FaceDetectionResult{detected=" + isDetected + 
                   ", confidence=" + confidence + "}";
        }
    }
    
    /**
     * Result of spoof detection
     */
    public static class SpoofDetectionResult {
        private final boolean isSpoof;
        private final float confidence;
        private final float score;
        
        public SpoofDetectionResult(boolean isSpoof, float confidence, float score) {
            this.isSpoof = isSpoof;
            this.confidence = confidence;
            this.score = score;
        }
        
        public boolean isSpoof() { return isSpoof; }
        public float getConfidence() { return confidence; }
        public float getScore() { return score; }
        
        @Override
        public String toString() {
            return "SpoofDetectionResult{isSpoof=" + isSpoof + 
                   ", confidence=" + confidence + ", score=" + score + "}";
        }
    }
    
    /**
     * Result of oval validation
     */
    public static class OvalValidationResult {
        private final boolean isValid;
        private final String reason;
        
        public OvalValidationResult(boolean isValid, String reason) {
            this.isValid = isValid;
            this.reason = reason;
        }
        
        public boolean isValid() { return isValid; }
        public String getReason() { return reason; }
        
        @Override
        public String toString() {
            return "OvalValidationResult{valid=" + isValid + ", reason='" + reason + "'}";
        }
    }
    
    /**
     * Final decision result
     */
    public static class FaceDecisionResult {
        private final DecisionType type;
        private final String message;
        private final DecisionReason reason;
        
        public FaceDecisionResult(DecisionType type, String message, DecisionReason reason) {
            this.type = type;
            this.message = message;
            this.reason = reason;
        }
        
        public DecisionType getType() { return type; }
        public String getMessage() { return message; }
        public DecisionReason getReason() { return reason; }
        
        public boolean isAccepted() { return type == DecisionType.ACCEPT; }
        public boolean isRejected() { return type == DecisionType.REJECT; }
        public boolean needsGuidance() { return type == DecisionType.GUIDANCE; }
        
        @Override
        public String toString() {
            return "FaceDecisionResult{type=" + type + 
                   ", message='" + message + "', reason=" + reason + "}";
        }
    }
    
    /**
     * Decision types
     */
    public enum DecisionType {
        ACCEPT,     // Face is accepted as real
        REJECT,     // Face is rejected as spoof
        GUIDANCE    // Need user guidance
    }
    
    /**
     * Decision reasons
     */
    public enum DecisionReason {
        HIGH_CONFIDENCE_REAL,
        MEDIUM_CONFIDENCE_REAL,
        STRONG_SPOOF,
        OVAL_VIOLATION,
        OVAL_COMPLIANT_LOW_SPOOF,
        OVAL_COMPLIANT_UNCLEAR,
        LOW_CONFIDENCE,
        DEFAULT_REJECTION
    }
} 