package vn.edu.fpt.zentryapp.faceid.ui.setting.controller;

import android.animation.ValueAnimator;
import android.graphics.Bitmap;
import android.graphics.Rect;
import android.graphics.RectF;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.LinearInterpolator;
import android.widget.ProgressBar;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import vn.edu.fpt.zentryapp.R;
import vn.edu.fpt.zentryapp.faceid.data.service.FaceIdService;
import vn.edu.fpt.zentryapp.faceid.ui.setting.state.FaceRegistrationState;
import vn.edu.fpt.zentryapp.faceid.ui.setting.state.FaceRegistrationStateManager;

/**
 * Controller for handling face quality analysis
 */
public class FaceAnalysisController {
    private static final String TAG = "FaceAnalysisController";
    
    private static final int ANALYSIS_DURATION_MS = 5000;
    private static final float MIN_AVERAGE_SCORE_FOR_REGISTRATION = 0.75f;
    
    private final ViewGroup cameraContainer;
    private final FaceRegistrationStateManager stateManager;
    private final Handler mainHandler = new Handler(Looper.getMainLooper());
    private final FaceAnalysisCallback callback;
    
    // Analysis components
    private ProgressBar analysisProgressBar;
    private TextView analysisCountdownText;
    private View analysisOverlay;
    private final List<Float> frameScores = new ArrayList<>();
    private boolean isAnalyzing = false;
    
    public interface FaceAnalysisCallback {
        void onAnalysisComplete(boolean isHighQuality, String feedback);
    }
    
    public FaceAnalysisController(ViewGroup cameraContainer, 
                                 FaceRegistrationStateManager stateManager,
                                 FaceAnalysisCallback callback) {
        this.cameraContainer = cameraContainer;
        this.stateManager = stateManager;
        this.callback = callback;
    }
    
    /**
     * Start a 5-second analysis of face quality before proceeding with registration
     * Collects frame scores to ensure consistent high-quality face detection
     */
    public void startAnalysis(FaceIdService faceIdService, RectF ovalRect, boolean livenessVerified) {
        // Check if already analyzing
        if (isAnalyzing) {
            Log.d(TAG, "Already analyzing, ignoring new request");
            return;
        }

        isAnalyzing = true;
        frameScores.clear();

        // Initialize and show analysis UI
        setupAnalysisUI();

        // Show overlay
        if (analysisOverlay != null) {
            analysisOverlay.setVisibility(View.VISIBLE);
        }

        // Start with initial analyzing state message
        stateManager.transitionTo(FaceRegistrationState.ANALYZING, "Analyzing... Keep still");

        // Show and update progressBar
        if (analysisProgressBar != null) {
            analysisProgressBar.setVisibility(View.VISIBLE);
            analysisProgressBar.setMax(ANALYSIS_DURATION_MS);
            analysisProgressBar.setProgress(0);

            // Animator for smooth progress updates
            final ValueAnimator progressAnimator = ValueAnimator.ofInt(0, ANALYSIS_DURATION_MS);
            progressAnimator.setDuration(ANALYSIS_DURATION_MS);
            progressAnimator.setInterpolator(new LinearInterpolator());
            progressAnimator.addUpdateListener(animation -> {
                if (analysisProgressBar != null) {
                    analysisProgressBar.setProgress((Integer) animation.getAnimatedValue());
                }
            });
            progressAnimator.start();
        }

        // Start countdown feedback
        final int[] secondsLeft = {ANALYSIS_DURATION_MS / 1000};
        final int countdownInterval = 1000; // 1 second

        // Countdown handler to update UI every second
        final Handler countdownHandler = new Handler(Looper.getMainLooper());
        final Runnable countdownRunnable = new Runnable() {
            @Override
            public void run() {
                if (!isAnalyzing) return;

                secondsLeft[0]--;
                if (secondsLeft[0] > 0) {
                    // Update countdown message and UI
                    String message = "Analyzing... " + secondsLeft[0] + "s";
                    stateManager.transitionTo(FaceRegistrationState.ANALYZING, message);

                    // Update countdown text
                    if (analysisCountdownText != null) {
                        analysisCountdownText.setText(message);
                    }

                    countdownHandler.postDelayed(this, countdownInterval);
                }
            }
        };

        // Start countdown updates
        countdownHandler.postDelayed(countdownRunnable, countdownInterval);

        // Schedule analysis completion
        mainHandler.postDelayed(() -> {
            // Stop analyzing
            isAnalyzing = false;
            countdownHandler.removeCallbacks(countdownRunnable);

            // Hide analysis overlay
            if (analysisOverlay != null) {
                analysisOverlay.setVisibility(View.GONE);
            }

            // Check if we collected enough data
            if (frameScores.isEmpty()) {
                stateManager.transitionTo(FaceRegistrationState.FAILED_OTHER,
                        "Could not get stable data. Please try again.");
                callback.onAnalysisComplete(false, "No data collected");
                return;
            }

            // Reduce the minimum required frames after liveness to avoid false negatives
            int minRequiredFrames = livenessVerified ? 6 : 10;
            if (frameScores.size() < minRequiredFrames) {
                stateManager.transitionTo(FaceRegistrationState.FAILED_OTHER,
                        "Not enough quality data. Improve lighting and keep position stable.");
                callback.onAnalysisComplete(false, "Not enough quality frames");
                return;
            }

            // Calculate statistics
            float sum = 0;
            float min = Float.MAX_VALUE;
            float max = Float.MIN_VALUE;

            for (float score : frameScores) {
                sum += score;
                min = Math.min(min, score);
                max = Math.max(max, score);
            }

            float averageScore = sum / frameScores.size();
            float variance = calculateVariance(frameScores, averageScore);

            Log.d(TAG, "Analysis complete: " + frameScores.size() + " frames analyzed");
            Log.d(TAG, "Scores - Avg: " + averageScore + ", Min: " + min + ", Max: " + max + ", Variance: " + variance);

            // Quality assessment
            boolean isConsistent = variance < 0.03; // Low variance indicates consistent detection
            boolean isHighQuality = averageScore >= MIN_AVERAGE_SCORE_FOR_REGISTRATION;
            boolean isAcceptableQuality = averageScore >= (MIN_AVERAGE_SCORE_FOR_REGISTRATION - 0.1f);

            // Log detailed quality information
            String qualityLog = String.format(Locale.US,
                    "Face Analysis Results - Frames: %d, Average Score: %.3f, Min: %.3f, Max: %.3f, Variance: %.5f, " +
                            "isConsistent: %b, isHighQuality: %b, isAcceptableQuality: %b",
                    frameScores.size(), averageScore, min, max, variance,
                    isConsistent, isHighQuality, isAcceptableQuality);
            Log.d(TAG, qualityLog);

            // Different paths based on quality assessment
            if (isHighQuality && isConsistent) {
                // High quality and consistent - proceed with registration
                stateManager.transitionTo(FaceRegistrationState.PROCESSING,
                        "Quality check passed. Registering...");
                callback.onAnalysisComplete(true, "High quality");
            } else if (isAcceptableQuality) {
                // Acceptable but not ideal - warn user but proceed
                stateManager.transitionTo(FaceRegistrationState.PROCESSING,
                        "Acceptable quality. Proceeding with registration...");
                callback.onAnalysisComplete(true, "Acceptable quality");
            } else {
                // Low quality - provide specific feedback based on issues
                String feedbackMessage = generateQualityFeedback(averageScore, variance);
                stateManager.transitionTo(FaceRegistrationState.FAILED_OTHER, feedbackMessage);
                callback.onAnalysisComplete(false, feedbackMessage);
            }
        }, ANALYSIS_DURATION_MS);
    }
    
    /**
     * Process a frame during analysis
     */
    public void processFrame(FaceIdService faceIdService, Bitmap bitmap, RectF ovalRect) {
        if (!isAnalyzing || faceIdService == null) return;
        
        faceIdService.processContinuousFrame(bitmap, ovalRect, new FaceIdService.ContinuousProcessingCallback() {
            @Override
            public void onFaceDetected(Rect boundingBox, boolean isSpoof, float spoofScore) {
                // During analysis, we collect frame scores for all frames
                // that are not flagged as spoof (or regardless of spoof if liveness verified)
                if (!isSpoof) {
                    frameScores.add(spoofScore);
                }
            }

            @Override
            public void onNoFaceDetected() {}

            @Override
            public void onMultipleFacesDetected() {}

            @Override
            public void onError(String errorMessage) {}
        });
    }
    
    /**
     * Check if analysis is currently running
     */
    public boolean isAnalyzing() {
        return isAnalyzing;
    }
    
    /**
     * Setup UI for analysis
     */
    private void setupAnalysisUI() {
        if (cameraContainer == null) return;

        // Check if already created
        if (analysisOverlay != null) {
            // Ensure correct visibility
            analysisOverlay.setVisibility(View.VISIBLE);
            return;
        }

        // Create overlay for analysis
        analysisOverlay = LayoutInflater.from(cameraContainer.getContext())
                .inflate(R.layout.overlay_face_analysis, cameraContainer, false);

        // Add to container
        cameraContainer.addView(analysisOverlay);

        // Get references to UI components
        analysisProgressBar = analysisOverlay.findViewById(R.id.progressBarAnalysis);
        analysisCountdownText = analysisOverlay.findViewById(R.id.tvAnalysisCountdown);

        // Set initial progress bar state
        if (analysisProgressBar != null) {
            analysisProgressBar.setProgress(0);
        }

        // Set initial countdown text
        if (analysisCountdownText != null) {
            analysisCountdownText.setText("Analyzing...");
        }

        // Show UI
        analysisOverlay.setVisibility(View.VISIBLE);

        Log.d(TAG, "Analysis UI initialized and shown");
    }
    
    /**
     * Calculate variance of collected scores to assess consistency
     */
    private float calculateVariance(List<Float> scores, float mean) {
        float sumSquaredDiff = 0;
        for (float score : scores) {
            float diff = score - mean;
            sumSquaredDiff += diff * diff;
        }
        return sumSquaredDiff / scores.size();
    }
    
    /**
     * Generate specific feedback based on detected quality issues
     */
    private String generateQualityFeedback(float averageScore, float variance) {
        StringBuilder feedback = new StringBuilder();

        if (variance > 0.05) {
            feedback.append("Unstable face detection. Please hold your face more steady and try again.");
            feedback.append("\n\nDetailed error: Variance = ").append(String.format(Locale.US, "%.5f", variance));
            feedback.append(" (exceeds threshold 0.05)");
        } else if (averageScore < 0.4f) {
            feedback.append("Very low detection quality. Please try again in better lighting conditions.");
            feedback.append("\n\nDetailed error: Average score = ").append(String.format(Locale.US, "%.3f", averageScore));
            feedback.append(" (below minimum threshold 0.4)");
        } else if (averageScore < 0.6f) {
            feedback.append("Low detection quality. Improve lighting and reduce face movement.");
            feedback.append("\n\nDetailed error: Average score = ").append(String.format(Locale.US, "%.3f", averageScore));
            feedback.append(" (below recommended threshold 0.6)");
        } else {
            feedback.append("Could not get a clear enough image. Please try again with better lighting and position.");
            feedback.append("\n\nDetailed error: Combined detection score and stability does not meet requirements");
        }

        return feedback.toString();
    }
    
    /**
     * Cancel ongoing analysis
     */
    public void cancelAnalysis() {
        if (!isAnalyzing) return;
        
        isAnalyzing = false;
        frameScores.clear();
        
        // Hide overlay
        if (analysisOverlay != null) {
            analysisOverlay.setVisibility(View.GONE);
        }
        
        // Remove pending callbacks
        mainHandler.removeCallbacksAndMessages(null);
    }
    
    /**
     * Clean up resources
     */
    public void cleanup() {
        cancelAnalysis();
        
        if (analysisOverlay != null && analysisOverlay.getParent() != null) {
            ((ViewGroup) analysisOverlay.getParent()).removeView(analysisOverlay);
            analysisOverlay = null;
        }
        
        analysisProgressBar = null;
        analysisCountdownText = null;
    }
}
