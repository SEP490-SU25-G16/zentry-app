package vn.edu.fpt.zentryapp.faceid.ui.setting.controller;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Rect;
import android.os.Vibrator;
import android.util.Log;
import android.view.View;

import androidx.core.content.ContextCompat;

import vn.edu.fpt.zentryapp.R;
import vn.edu.fpt.zentryapp.databinding.FragmentStudentSettingRegisterFaceIdBinding;
import vn.edu.fpt.zentryapp.faceid.data.service.FaceIdEnhancer;
import vn.edu.fpt.zentryapp.faceid.ui.components.OvalFaceOverlayView;
import vn.edu.fpt.zentryapp.faceid.ui.setting.state.FaceRegistrationState;
import vn.edu.fpt.zentryapp.faceid.ui.setting.state.FaceRegistrationStateManager;

/**
 * Controller for handling liveness verification
 */
public class LivenessVerificationController implements FaceIdEnhancer.FaceIdEnhancerCallback {
    private static final String TAG = "LivenessVerification";
    
    private final Context context;
    private final FragmentStudentSettingRegisterFaceIdBinding binding;
    private final OvalFaceOverlayView faceOverlayView;
    private final FaceRegistrationStateManager stateManager;
    private final LivenessVerificationCallback callback;
    
    private FaceIdEnhancer faceIdEnhancer;
    private boolean isInitialized = false;
    
    public interface LivenessVerificationCallback {
        void onLivenessVerified();
        void onLivenessFailed(String reason);
    }
    
    public LivenessVerificationController(Context context, 
                                         FragmentStudentSettingRegisterFaceIdBinding binding,
                                         OvalFaceOverlayView faceOverlayView,
                                         FaceRegistrationStateManager stateManager,
                                         LivenessVerificationCallback callback) {
        this.context = context;
        this.binding = binding;
        this.faceOverlayView = faceOverlayView;
        this.stateManager = stateManager;
        this.callback = callback;
    }
    
    /**
     * Initialize the FaceIdEnhancer for liveness challenges
     */
    public void initialize() {
        if (isInitialized) {
            // Already initialized, just reset it
            if (faceIdEnhancer != null) {
                faceIdEnhancer.reset();
            }
            return;
        }

        try {
            // Initialize the FaceIdEnhancer
            faceIdEnhancer = new FaceIdEnhancer(context, this);
            // Only require gaze (RIGHT -> LEFT) to match current UX and avoid blocking on blink
            faceIdEnhancer.setChallengeType(FaceIdEnhancer.ChallengeType.GAZE_ONLY);
            isInitialized = true;
            Log.d(TAG, "FaceIdEnhancer initialized successfully");
        } catch (Exception e) {
            Log.e(TAG, "Error initializing FaceIdEnhancer", e);
        }
    }
    
    /**
     * Process frame for liveness challenge
     */
    public void processFrame(Bitmap bitmap, Rect faceRect) {
        if (faceIdEnhancer != null && isInitialized) {
            faceIdEnhancer.processFaceFrame(bitmap, faceRect);
        } else {
            Log.w(TAG, "Attempted to process liveness frame but FaceIdEnhancer not initialized");
        }
    }
    
    /**
     * Show liveness challenge progress indicators
     */
    public void showProgressIndicators() {
        if (binding != null && binding.llLivenessProgress != null &&
                binding.llLivenessProgress.getVisibility() != View.VISIBLE) {

            // Show progress indicators
            binding.llLivenessProgress.setVisibility(View.VISIBLE);
            // Ensure the liveness progress overlay is above camera and face overlay
            binding.llLivenessProgress.bringToFront();
            binding.llLivenessProgress.requestLayout();
            binding.llLivenessProgress.invalidate();

            // Update instruction text
            binding.tvStatusMessage.setText("Liveness Challenge");
            binding.tvInstructionMessage.setText("Please blink your eyes");
        }
    }
    
    /**
     * Update UI for liveness challenge
     */
    public void updateUI() {
        if (binding != null && binding.tvStatusMessage != null) {
            binding.tvStatusMessage.setText("Hãy nhìn vào camera và nhấp mắt");
        }
        if (faceOverlayView != null) {
            faceOverlayView.setOvalColor(ContextCompat.getColor(context, R.color.primary));
        }
        
        // Ensure liveness overlay is visible and above camera
        if (binding != null && binding.llLivenessProgress != null) {
            binding.llLivenessProgress.setVisibility(View.VISIBLE);
            binding.llLivenessProgress.bringToFront();
        }
    }
    
    /**
     * Hide liveness challenge UI
     */
    public void hideUI() {
        if (binding != null && binding.llLivenessProgress != null) {
            binding.llLivenessProgress.setVisibility(View.GONE);
        }
    }
    
    /**
     * Close and clean up resources
     */
    public void cleanup() {
        if (faceIdEnhancer != null) {
            faceIdEnhancer.close();
            faceIdEnhancer = null;
        }
        isInitialized = false;
    }
    
    //------------------------------------------------------------------------------
    // FaceIdEnhancer.FaceIdEnhancerCallback Implementation
    //------------------------------------------------------------------------------
    
    @Override
    public void onStateChanged(FaceIdEnhancer.AuthState newState) {
        Log.d(TAG, "FaceIdEnhancer state changed: " + newState);

        // Show liveness progress indicators when face is detected
        if (newState == FaceIdEnhancer.AuthState.FACE_DETECTED ||
                newState == FaceIdEnhancer.AuthState.ANALYZING) {
            showProgressIndicators();
        }

        // Update UI based on FaceIdEnhancer state
        if (newState == FaceIdEnhancer.AuthState.BLINK_VERIFIED) {
            // User blinked successfully
            if (binding != null) {
                // Update status message
                binding.tvStatusMessage.setText("Blink detected!");
                binding.tvInstructionMessage.setText("Now look at different directions");

                // Update progress indicators
                binding.ivBlinkIndicator.setColorFilter(
                        ContextCompat.getColor(context, R.color.success_green),
                        android.graphics.PorterDuff.Mode.SRC_IN);
            }
        } else if (newState == FaceIdEnhancer.AuthState.GAZE_VERIFIED) {
            // User completed gaze challenge
            if (binding != null) {
                // Update status message
                binding.tvStatusMessage.setText("Gaze verified! ✓");
                binding.tvInstructionMessage.setText("Look straight at the camera");

                // Update progress indicators
                binding.ivGazeIndicator.setColorFilter(
                        ContextCompat.getColor(context, R.color.success_green),
                        android.graphics.PorterDuff.Mode.SRC_IN);
            }
        } else if (newState == FaceIdEnhancer.AuthState.VERIFIED) {
            // All liveness challenges completed
            Log.d(TAG, "Liveness verification complete!");
            
            // Transition to the next state in registration
            stateManager.transitionTo(FaceRegistrationState.FACE_REAL, "Liveness verified!");
            
            // Hide liveness overlay
            hideUI();
            
            // Notify callback of success
            if (callback != null) {
                callback.onLivenessVerified();
            }
        }
    }

    @Override
    public void onBlinkDetected() {
        Log.d(TAG, "👁️ Blink detected!");
        // Update UI to show blink was detected with visual feedback
        if (binding != null) {
            // Update status message with clear instructions
            binding.tvStatusMessage.setText("Blink detected! ✓");
            binding.tvInstructionMessage.setText("Now look left, right, and up");

            // Update progress indicator
            binding.ivBlinkIndicator.setColorFilter(
                    ContextCompat.getColor(context, R.color.success_green),
                    android.graphics.PorterDuff.Mode.SRC_IN);

            // Add animation for visual feedback
            binding.ivBlinkIndicator.animate()
                    .scaleX(1.2f).scaleY(1.2f)
                    .setDuration(200)
                    .withEndAction(() -> {
                        binding.ivBlinkIndicator.animate()
                                .scaleX(1.0f).scaleY(1.0f)
                                .setDuration(200);
                    });
        }
    }

    @Override
    public void onGazeStepVerified(FaceIdEnhancer.Direction direction, int stepIndex, int totalSteps) {
        // Haptic feedback: vibrate briefly for confirmation
        try {
            Vibrator v = (Vibrator) context.getSystemService(Context.VIBRATOR_SERVICE);
            if (v != null) {
                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                    v.vibrate(android.os.VibrationEffect.createOneShot(60, android.os.VibrationEffect.DEFAULT_AMPLITUDE));
                } else {
                    v.vibrate(60);
                }
            }
        } catch (Exception ignored) {}

        // Update UI instruction to next step
        if (binding != null) {
            if (stepIndex + 1 < totalSteps) {
                // Next direction required
                FaceIdEnhancer.Direction next = (direction == FaceIdEnhancer.Direction.RIGHT) ? 
                        FaceIdEnhancer.Direction.LEFT : FaceIdEnhancer.Direction.RIGHT;
                binding.tvStatusMessage.setText("Good! ✓");
                binding.tvInstructionMessage.setText(next == FaceIdEnhancer.Direction.LEFT ? 
                        "Please look left" : "Please look right");
            } else {
                // Sequence completed; ask user to look straight for analysis
                binding.tvStatusMessage.setText("Gaze verified! ✓");
                binding.tvInstructionMessage.setText("Look straight at the camera");
            }

            // Mark the corresponding icon as green
            if (direction == FaceIdEnhancer.Direction.RIGHT) {
                binding.ivGazeIndicator.setColorFilter(
                        ContextCompat.getColor(context, R.color.success_green),
                        android.graphics.PorterDuff.Mode.SRC_IN);
            }
        }
    }

    @Override
    public void onGazePrompt(FaceIdEnhancer.Direction required, int stepIndex, int totalSteps) {
        if (binding == null) return;
        
        String prompt = required == FaceIdEnhancer.Direction.LEFT ? "Please look left" : "Please look right";
        binding.tvInstructionMessage.setText(prompt);
        binding.tvStatusMessage.setText("Step " + (stepIndex + 1) + "/" + totalSteps);
    }

    @Override
    public void onGazeDirectionChanged(float x, float y) {
        // Keep lightweight; prompts are driven by onGazePrompt/onGazeStepVerified
        Log.d(TAG, "👀 Gaze direction (adjusted): x=" + x + ", y=" + y);
    }

    @Override
    public void onLivenessVerified(boolean isLive) {
        Log.d(TAG, "🔐 Liveness verification result: " + (isLive ? "LIVE" : "NOT LIVE"));
        if (isLive) {
            // Notify callback of success
            if (callback != null) {
                callback.onLivenessVerified();
            }
        } else {
            // Notify callback of failure
            if (callback != null) {
                callback.onLivenessFailed("Liveness verification failed");
            }
        }
    }

    @Override
    public void onVerificationComplete(boolean success) {
        Log.d(TAG, "✅ Verification complete: " + (success ? "SUCCESS" : "FAILED"));
        if (success) {
            // Notify callback of success
            if (callback != null) {
                callback.onLivenessVerified();
            }
        } else {
            // Notify callback of failure
            if (callback != null) {
                callback.onLivenessFailed("Verification failed");
            }
        }
    }
}
