package vn.edu.fpt.zentryapp.student.ui.setting.ui;

import android.view.View;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.core.content.ContextCompat;

import vn.edu.fpt.zentryapp.R;
import vn.edu.fpt.zentryapp.databinding.FragmentStudentSettingRegisterFaceIdBinding;
import vn.edu.fpt.zentryapp.student.ui.components.OvalFaceOverlayView;
import vn.edu.fpt.zentryapp.student.ui.setting.state.FaceRegistrationState;

/**
 * UI Controller quản lý tất cả UI updates cho Face Registration
 * Tách riêng UI logic khỏi business logic
 */
public class FaceRegistrationUIController {
    private static final String TAG = "FaceRegUIController";
    
    private final FragmentStudentSettingRegisterFaceIdBinding binding;
    private final OvalFaceOverlayView faceOverlayView;
    
    // UI States
    public enum UIScreenState {
        SETUP,    // Màn hình giới thiệu
        CAMERA,   // Màn hình camera
        LOADING   // Màn hình loading khi processing
    }
    
    private UIScreenState currentScreenState = UIScreenState.SETUP;
    
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
        
        // Update progress visibility
        updateProgressVisibility(state);
        
        // Update screen if needed
        updateScreenForState(state);
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
        } else if (state == FaceRegistrationState.SUCCESS) {
            textColor = ContextCompat.getColor(binding.getRoot().getContext(), R.color.success_green);
        } else if (state.isProcessingState()) {
            textColor = ContextCompat.getColor(binding.getRoot().getContext(), R.color.processing_blue);
        } else {
            textColor = ContextCompat.getColor(binding.getRoot().getContext(), R.color.text_primary);
        }
        
        binding.tvStatusMessage.setTextColor(textColor);
    }
    
    /**
     * Update oval overlay appearance
     */
    private void updateOvalOverlay(FaceRegistrationState state) {
        if (faceOverlayView == null) return;
        
        switch (state) {
            case FACE_REAL:
            case FACE_STABLE:
                faceOverlayView.setOvalColor(ContextCompat.getColor(
                    faceOverlayView.getContext(), R.color.success_green));
                break;
                
            case FACE_SPOOFED:
            case FAILED_SPOOF:
                faceOverlayView.setOvalColor(ContextCompat.getColor(
                    faceOverlayView.getContext(), R.color.error_red));
                break;
                
            case FACE_STABILIZING:
                faceOverlayView.setOvalColor(ContextCompat.getColor(
                    faceOverlayView.getContext(), R.color.processing_blue));
                faceOverlayView.startProgressAnimation(700); // 0.7 seconds
                break;
                
            case NO_FACE:
            case FACE_DETECTED:
            default:
                faceOverlayView.setOvalColor(ContextCompat.getColor(
                    faceOverlayView.getContext(), R.color.white));
                break;
        }
    }
    
    /**
     * Update progress bar visibility
     */
    private void updateProgressVisibility(FaceRegistrationState state) {
        if (binding.progressBarRegisterFaceId == null) return;
        
        boolean showProgress = state.isProcessingState() || 
                              state == FaceRegistrationState.PROCESSING ||
                              state == FaceRegistrationState.CAPTURING;
        
        binding.progressBarRegisterFaceId.setVisibility(showProgress ? View.VISIBLE : View.GONE);
    }
    
    /**
     * Update screen based on state
     */
    private void updateScreenForState(FaceRegistrationState state) {
        switch (state) {
            case INITIALIZING:
                showScreen(UIScreenState.LOADING);
                break;
                
            case READY:
            case NO_FACE:
            case FACE_DETECTED:
            case FACE_REAL:
            case FACE_STABILIZING:
            case FACE_SPOOFED:
                if (currentScreenState != UIScreenState.CAMERA) {
                    showScreen(UIScreenState.CAMERA);
                }
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
     * Enable/disable camera controls
     */
    public void setCameraControlsEnabled(boolean enabled) {
        // Disable back button during processing
        binding.ivCameraBack.setEnabled(enabled);
        binding.ivCameraBack.setAlpha(enabled ? 1.0f : 0.5f);
    }
    
    /**
     * Get current screen state
     */
    public UIScreenState getCurrentScreenState() {
        return currentScreenState;
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
}