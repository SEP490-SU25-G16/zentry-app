package vn.edu.fpt.zentryapp.faceid.ui;

import android.text.method.ScrollingMovementMethod;
import android.util.Log;
import android.view.View;

import androidx.core.content.ContextCompat;

import lombok.Getter;
import vn.edu.fpt.zentryapp.R;
import vn.edu.fpt.zentryapp.databinding.FragmentStudentSettingRegisterFaceIdBinding;
import vn.edu.fpt.zentryapp.faceid.data.service.FaceProcessingState;
import vn.edu.fpt.zentryapp.faceid.ui.components.OvalFaceOverlayView;
import vn.edu.fpt.zentryapp.faceid.ui.setting.state.FaceRegistrationState;

/**
 * UI Controller quản lý tất cả UI updates cho Face Registration
 * Tách riêng UI logic khỏi business logic
 */
public class FaceRegistrationUIController {
    private final FragmentStudentSettingRegisterFaceIdBinding binding;
    private final OvalFaceOverlayView faceOverlayView;
    /**
     * -- GETTER --
     *  Get current screen state
     */
    @Getter
    private UIScreenState currentScreenState = UIScreenState.SETUP;

    // UI States
    public enum UIScreenState {
        SETUP,    // Màn hình giới thiệu
        CAMERA,   // Màn hình camera
        LOADING   // Màn hình loading khi processing
    }

    public FaceRegistrationUIController(FragmentStudentSettingRegisterFaceIdBinding binding,
                                         OvalFaceOverlayView faceOverlayView) {
        this.binding = binding;
        this.faceOverlayView = faceOverlayView;
    }

    /**
     * Show appropriate screen
     */
    public void showScreen(UIScreenState screenState) {
        if (currentScreenState == screenState) {
            return; // Already showing this screen
        }

        currentScreenState = screenState;

        // Hide all screens first
        binding.llSetupScreen.setVisibility(View.GONE);
        binding.flCameraScreen.setVisibility(View.GONE);

        // Show appropriate screen
        switch (screenState) {
            case SETUP:
                binding.llSetupScreen.setVisibility(View.VISIBLE);
                break;
            case CAMERA:
                binding.flCameraScreen.setVisibility(View.VISIBLE);
                break;
            case LOADING:
                binding.flCameraScreen.setVisibility(View.VISIBLE);
                showLoadingOverlay(true);
                break;
        }
    }

    /**
     * Update UI based on Face Registration State
     */
    public void updateForState(FaceRegistrationState state, String message) {
        // Update status message
        updateStatusMessage(state, message);

        // Update oval overlay
        updateOvalOverlay(state);

        // Cập nhật progress visibility based on state
        boolean showProgress = state.isProcessingState() ||
                state == FaceRegistrationState.PROCESSING ||
                state == FaceRegistrationState.CAPTURING;
                
        showLoadingIndicator(showProgress);

        // Update screen if needed
        updateScreenForState(state);
        
        // Log cập nhật UI để debug
        System.out.println("UI đã cập nhật cho trạng thái: " + state + " với thông báo: " + message);
    }

    /**
     * Update status message with color coding
     */
    private void updateStatusMessage(FaceRegistrationState state, String message) {
        if (binding.tvStatusMessage == null) return;

        binding.tvStatusMessage.setText(message);

        // Color code messages based on state type
        int textColor;
        if (state.isErrorState()) {
            textColor = ContextCompat.getColor(binding.getRoot().getContext(), R.color.error_red);
            // Set error message with larger text and bold for error states
            binding.tvStatusMessage.setTextSize(16); // Increase text size for errors
            binding.tvStatusMessage.setPadding(16, 16, 16, 16); // Add padding for emphasis
        } else if (state == FaceRegistrationState.SUCCESS) {
            textColor = ContextCompat.getColor(binding.getRoot().getContext(), R.color.success_green);
            // Reset text size and padding for success
            binding.tvStatusMessage.setTextSize(14);
            binding.tvStatusMessage.setPadding(8, 8, 8, 8);
        } else if (state.isProcessingState()) {
            textColor = ContextCompat.getColor(binding.getRoot().getContext(), R.color.processing_blue);
            // Reset text size and padding for processing
            binding.tvStatusMessage.setTextSize(14);
            binding.tvStatusMessage.setPadding(8, 8, 8, 8);
        } else {
            textColor = ContextCompat.getColor(binding.getRoot().getContext(), R.color.text_primary);
            // Reset text size and padding for normal states
            binding.tvStatusMessage.setTextSize(14);
            binding.tvStatusMessage.setPadding(8, 8, 8, 8);
        }

        binding.tvStatusMessage.setTextColor(textColor);
        
        // Make error messages scrollable if they're long
        if (state.isErrorState() && message.length() > 100) {
            binding.tvStatusMessage.setMovementMethod(new ScrollingMovementMethod());
            binding.tvStatusMessage.setMaxHeight(300); // Limit height but allow scrolling
        } else {
            binding.tvStatusMessage.setMovementMethod(null);
            binding.tvStatusMessage.setMaxHeight(Integer.MAX_VALUE);
        }
        
        // Log status messages for debugging
        if (state.isErrorState()) {
            Log.e("FaceRegUIController", "Error state: " + state + " - " + message);
        } else {
            Log.d("FaceRegUIController", "State: " + state + " - " + message);
        }
    }

    /**
     * Update oval overlay appearance
     */
    private void updateOvalOverlay(FaceRegistrationState state) {
        if (faceOverlayView == null) return;

        switch (state) {
            case FACE_REAL:
                faceOverlayView.setOvalColor(ContextCompat.getColor(
                        faceOverlayView.getContext(), R.color.success_green));
                faceOverlayView.updateState(FaceProcessingState.FACE_REAL, state.getDefaultMessage());
                break;

            case LIVENESS_CHALLENGE:
                faceOverlayView.setOvalColor(ContextCompat.getColor(
                        faceOverlayView.getContext(), R.color.liveness_challenge_blue));
                faceOverlayView.updateState(FaceProcessingState.LIVENESS_CHALLENGE, state.getDefaultMessage());
                break;

            case FACE_STABLE:
                faceOverlayView.setOvalColor(ContextCompat.getColor(
                        faceOverlayView.getContext(), R.color.success_green));
                faceOverlayView.updateState(FaceProcessingState.FACE_STABLE, state.getDefaultMessage());
                break;

            case FACE_SPOOFED:
            case FAILED_SPOOF:
                faceOverlayView.setOvalColor(ContextCompat.getColor(
                        faceOverlayView.getContext(), R.color.error_red));
                faceOverlayView.updateState(FaceProcessingState.FACE_SPOOFED, state.getDefaultMessage());
                break;

            case FACE_STABILIZING:
                faceOverlayView.setOvalColor(ContextCompat.getColor(
                        faceOverlayView.getContext(), R.color.processing_blue));
                faceOverlayView.updateState(FaceProcessingState.FACE_STABILIZING, state.getDefaultMessage());
                faceOverlayView.startProgressAnimation(700); // 0.7 seconds
                break;
                
            case ANALYZING:
                faceOverlayView.setOvalColor(ContextCompat.getColor(
                        faceOverlayView.getContext(), R.color.processing_blue));
                faceOverlayView.updateState(FaceProcessingState.FACE_STABLE, state.getDefaultMessage());
                break;

            case FACE_DETECTED:
                faceOverlayView.setOvalColor(ContextCompat.getColor(
                        faceOverlayView.getContext(), R.color.warning_yellow));
                faceOverlayView.updateState(FaceProcessingState.FACE_DETECTED, state.getDefaultMessage());
                break;

            case NO_FACE:
                faceOverlayView.setOvalColor(ContextCompat.getColor(
                        faceOverlayView.getContext(), R.color.white));
                faceOverlayView.updateState(FaceProcessingState.NO_FACE, state.getDefaultMessage());
                break;

            case FACE_OUT_OF_BOUNDS:
                faceOverlayView.setOvalColor(ContextCompat.getColor(
                        faceOverlayView.getContext(), R.color.warning_yellow));
                faceOverlayView.updateState(FaceProcessingState.FACE_DETECTED, state.getDefaultMessage());
                break;

            default:
                faceOverlayView.setOvalColor(ContextCompat.getColor(
                        faceOverlayView.getContext(), R.color.white));
                faceOverlayView.updateState(FaceProcessingState.READY, state.getDefaultMessage());
                break;
        }
    }

    /**
     * Hiển thị loading indicator
     */
    public void showLoadingIndicator(boolean show) {
        if (binding.progressBarRegisterFaceId != null) {
            binding.progressBarRegisterFaceId.setVisibility(show ? View.VISIBLE : View.GONE);
        }
    }

    /**
     * Update screen based on state
     */
    private void updateScreenForState(FaceRegistrationState state) {
        switch (state) {
            case INITIALIZING:
                showScreen(UIScreenState.LOADING);
                // Make sure loading overlay is visible during initialization
                showLoadingOverlay(true);
                break;

            case READY:
                // When ready, ensure we show the camera and hide loading overlay
                showScreen(UIScreenState.CAMERA);
                showLoadingOverlay(false);
                break;

            case NO_FACE:
            case FACE_DETECTED:
            case FACE_REAL:
            case FACE_STABILIZING:
            case FACE_SPOOFED:
            case ANALYZING:
                if (currentScreenState != UIScreenState.CAMERA) {
                    showScreen(UIScreenState.CAMERA);
                }
                // Ensure loading overlay is hidden during these states
                showLoadingOverlay(false);
                break;

            case PROCESSING:
            case CAPTURING:
                showScreen(UIScreenState.LOADING);
                break;

            case SUCCESS:
                // Success sẽ được handle bởi navigation sang Activity khác
                break;
        }
    }

    /**
     * Show/hide loading overlay
     */
    public void showLoadingOverlay(boolean show) {
        if (show) {
            binding.skeletonLayout.setVisibility(View.VISIBLE);
            binding.flStudentSettingRegisterFaceIdCameraContainer.setVisibility(View.INVISIBLE);
        } else {
            binding.skeletonLayout.setVisibility(View.GONE);
            binding.flStudentSettingRegisterFaceIdCameraContainer.setVisibility(View.VISIBLE);
        }
    }

    /**
     * Show error with retry option
     */
    public void showErrorWithRetry(String errorMessage, Runnable onRetry) {
        // Update status message
        if (binding.tvStatusMessage != null) {
            binding.tvStatusMessage.setText(errorMessage);
            binding.tvStatusMessage.setTextColor(
                    ContextCompat.getColor(binding.getRoot().getContext(), R.color.error_red));
        }

        // Show retry button (nếu có trong layout)
        // Có thể extend layout để có retry button
    }


    /**
     * Cleanup UI controller
     */
    public void cleanup() {
        // Clear any pending UI updates
        if (faceOverlayView != null) {
            faceOverlayView.stopProgressAnimation();
        }
    }

    public int getColorForState(FaceRegistrationState state) {
        final var ctx = binding.getRoot().getContext();
        switch (state) {
            case FACE_REAL:
            case FACE_STABLE:
                return ContextCompat.getColor(ctx, R.color.success_green);

            case FACE_DETECTED:
            case FACE_STABILIZING:
            case FACE_WARNING:
            case FACE_OUT_OF_BOUNDS:
                return ContextCompat.getColor(ctx, R.color.warning_yellow);

            case FACE_SPOOFED:
            case FAILED_SPOOF:
            case MULTIPLE_FACES:
                return ContextCompat.getColor(ctx, R.color.error_red);

            case NO_FACE:
            case READY:
                return ContextCompat.getColor(ctx, R.color.white);

            case LIVENESS_CHALLENGE:
                return ContextCompat.getColor(ctx, R.color.liveness_challenge_blue);

            case ANALYZING:
            case PROCESSING:
                return ContextCompat.getColor(ctx, R.color.processing_blue);

            default:
                return ContextCompat.getColor(ctx, R.color.white);
        }
    }
}