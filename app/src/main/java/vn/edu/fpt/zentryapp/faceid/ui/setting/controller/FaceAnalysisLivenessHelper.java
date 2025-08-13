package vn.edu.fpt.zentryapp.faceid.ui.setting.controller;

import android.graphics.Rect;
import android.util.Log;

import java.util.LinkedList;
import java.util.Queue;

import vn.edu.fpt.zentryapp.faceid.data.service.FaceIdConfig;
import vn.edu.fpt.zentryapp.faceid.data.service.TemporalVarianceAnalyzer;

/**
 * Helper class to integrate FaceAnalysisController with spoof detection systems.
 * Provides a bridge between temporal analysis and face quality assessment.
 */
public class FaceAnalysisLivenessHelper {
    private static final String TAG = "FaceAnalysisLivenessHelper";
    
    // Confidence multipliers for quality boost
    private static final float LIVENESS_VERIFIED_BOOST = 0.15f;
    private static final float NATURAL_MOVEMENT_BOOST = 0.10f;
    private static final float CONSISTENT_DETECTION_BOOST = 0.08f;
    
    // Thresholds for natural movement 
    private static final float MIN_EXPECTED_POSITION_VARIANCE = 0.0001f;
    private static final float MAX_EXPECTED_POSITION_VARIANCE = 0.10f;
    private static final float MIN_EXPECTED_SIZE_VARIANCE = 0.0001f;
    private static final float MAX_EXPECTED_SIZE_VARIANCE = 0.08f;
    
    // Temporal variance analyzer
    private final TemporalVarianceAnalyzer temporalAnalyzer;
    
    // Analysis scenario
    private FaceIdConfig.Scenario scenario;
    
    // Recent quality scores with timestamps
    private static class TimestampedScore {
        final float score;
        final long timestamp;
        
        TimestampedScore(float score) {
            this.score = score;
            this.timestamp = System.currentTimeMillis();
        }
    }
    
    // Queue of recent quality scores
    private final Queue<TimestampedScore> recentScores = new LinkedList<>();
    private static final int MAX_RECENT_SCORES = 10;
    
    /**
     * Initialize helper with specified scenario
     */
    public FaceAnalysisLivenessHelper(FaceIdConfig.Scenario scenario) {
        this.temporalAnalyzer = new TemporalVarianceAnalyzer();
        this.scenario = scenario;
    }
    
    /**
     * Set current scenario
     */
    public void setScenario(FaceIdConfig.Scenario scenario) {
        this.scenario = scenario;
    }
    
    /**
     * Add frame data for temporal analysis
     */
    public void addFrameData(float[] modelResults, Rect faceRect, float confidence) {
        temporalAnalyzer.addFrame(modelResults, faceRect, confidence);
    }
    
    /**
     * Clear all history data
     */
    public void clearHistory() {
        temporalAnalyzer.clearHistory();
        recentScores.clear();
    }
    
    /**
     * Add recent quality score to history
     */
    public void addQualityScore(float score) {
        recentScores.add(new TimestampedScore(score));
        if (recentScores.size() > MAX_RECENT_SCORES) {
            recentScores.poll();
        }
    }
    
    /**
     * Calculate quality consistency (variance) from recent scores
     */
    public float calculateQualityConsistency() {
        if (recentScores.size() < 2) {
            return 0.01f; // Default variance when not enough data
        }
        
        float sum = 0;
        float sumSq = 0;
        int count = 0;
        
        for (TimestampedScore ts : recentScores) {
            sum += ts.score;
            sumSq += ts.score * ts.score;
            count++;
        }
        
        float mean = sum / count;
        float variance = (sumSq / count) - (mean * mean);
        
        return variance;
    }
    
    /**
     * Apply liveness-aware quality boost to base score
     */
    public float applyLivenessBoost(float baseScore, boolean livenessVerified) {
        // Start with base score
        float adjustedScore = baseScore;
        
        // Apply liveness verification boost if verified
        if (livenessVerified) {
            adjustedScore += LIVENESS_VERIFIED_BOOST;
            Log.d(TAG, "Applied liveness boost: +" + LIVENESS_VERIFIED_BOOST + 
                  " (new score: " + adjustedScore + ")");
        }
        
        // Check if we have enough frames for temporal analysis
        if (temporalAnalyzer.hasEnoughHistory(3)) {
            // Analyze temporal variance with scenario-specific settings
            TemporalVarianceAnalyzer.TemporalVarianceResult tvResult = 
                temporalAnalyzer.analyzeWithScenario(scenario, livenessVerified);
            
            // Apply natural movement boost if movement looks natural
            if (tvResult.hasNaturalMovement()) {
                adjustedScore += NATURAL_MOVEMENT_BOOST;
                Log.d(TAG, "Applied natural movement boost: +" + NATURAL_MOVEMENT_BOOST + 
                      " (new score: " + adjustedScore + ")");
            }
            
            // Apply consistency boost if detection is stable
            float consistencyVariance = calculateQualityConsistency();
            if (consistencyVariance < 0.01f) {
                adjustedScore += CONSISTENT_DETECTION_BOOST;
                Log.d(TAG, "Applied consistency boost: +" + CONSISTENT_DETECTION_BOOST + 
                      " (new score: " + adjustedScore + ", variance: " + consistencyVariance + ")");
            }
        }
        
        // Ensure score is in valid range [0,1]
        return Math.max(0.0f, Math.min(1.0f, adjustedScore));
    }
    
    /**
     * Generate insights about temporal variance for feedback
     */
    public String generateTemporalInsights(boolean livenessVerified) {
        if (!temporalAnalyzer.hasEnoughHistory(3)) {
            return "Insufficient temporal data for analysis";
        }
        
        TemporalVarianceAnalyzer.TemporalVarianceResult tvResult = 
            temporalAnalyzer.analyzeWithScenario(scenario, livenessVerified);
        
        StringBuilder insights = new StringBuilder();
        
        if (tvResult.hasNaturalMovement()) {
            insights.append("Natural face movement detected ✓\n");
        } else {
            if (tvResult.getPositionVariance() < MIN_EXPECTED_POSITION_VARIANCE) {
                insights.append("Face position too stable - may indicate a photo ✗\n");
            } else if (tvResult.getPositionVariance() > MAX_EXPECTED_POSITION_VARIANCE) {
                insights.append("Excessive face movement detected ✗\n");
            }
            
            if (tvResult.getSizeVariance() < MIN_EXPECTED_SIZE_VARIANCE) {
                insights.append("Face size too stable - may indicate a photo ✗\n");
            } else if (tvResult.getSizeVariance() > MAX_EXPECTED_SIZE_VARIANCE) {
                insights.append("Excessive face size changes detected ✗\n");
            }
        }
        
        if (tvResult.hasAbnormalPattern()) {
            insights.append("Abnormal detection pattern detected ✗\n");
        } else {
            insights.append("Consistent detection pattern ✓\n");
        }
        
        float consistencyVariance = calculateQualityConsistency();
        if (consistencyVariance < 0.01f) {
            insights.append("Stable quality detection ✓\n");
        } else if (consistencyVariance > 0.05f) {
            insights.append("Unstable quality detection ✗\n");
        }
        
        return insights.toString();
    }
}
