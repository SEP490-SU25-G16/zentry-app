package vn.edu.fpt.zentryapp.faceid.ui.setting;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.navigation.NavController;
import androidx.navigation.fragment.NavHostFragment;

import vn.edu.fpt.zentryapp.R;
import vn.edu.fpt.zentryapp.databinding.FragmentStudentSettingVerifyFaceIdBinding;
import vn.edu.fpt.zentryapp.faceid.data.service.FaceIdConfig;
import vn.edu.fpt.zentryapp.faceid.ui.common.BaseFaceIdFragment;
import vn.edu.fpt.zentryapp.faceid.ui.common.FaceIdProcessingCallback;
import vn.edu.fpt.zentryapp.faceid.ui.common.FaceIdProcessor;

/**
 * Fragment for verifying Face ID
 * Refactored to use the BaseFaceIdFragment
 */
public class StudentSettingVerifyFaceIdFragment extends BaseFaceIdFragment {
    private static final String TAG = "VerifyFaceIdFragment";
    
    private FragmentStudentSettingVerifyFaceIdBinding binding;
    private FaceIdProcessor faceIdProcessor;
    private boolean isProcessing = false;
    private NavController navController;
    
    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        binding = FragmentStudentSettingVerifyFaceIdBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }
    
    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        navController = NavHostFragment.findNavController(this);
    }
    
    @Override
    protected void setupClickListeners() {
        binding.ivBack.setOnClickListener(v -> requireActivity().onBackPressed());
        binding.btnCancel.setOnClickListener(v -> requireActivity().onBackPressed());
        
        binding.btnVerify.setOnClickListener(v -> {
            if (!isProcessing) {
                verifyFaceId();
            }
        });
    }
    
    @Override
    protected ViewGroup getCameraContainer() {
        return binding.flCameraContainer;
    }
    
    @Override
    protected void showLoading(boolean isLoading, String message) {
        if (binding == null) return;
        
        binding.progressBar.setVisibility(isLoading ? View.VISIBLE : View.GONE);
        binding.tvStatus.setText(message);
    }
    
    @Override
    protected void updateStatus(String message) {
        if (binding == null) return;
        binding.tvStatus.setText(message);
    }
    
    @Override
    protected void setActionButtonEnabled(boolean enabled) {
        if (binding == null) return;
        binding.btnVerify.setEnabled(enabled);
    }
    
    @Override
    protected FaceIdConfig.Scenario getScenario() {
        return FaceIdConfig.Scenario.VERIFICATION;
    }
    
    @Override
    protected void onFaceIdServiceInitialized() {
        faceIdProcessor = new FaceIdProcessor(faceIdService);
    }
    
    /**
     * Verify Face ID using current frame
     */
    private void verifyFaceId() {
        if (currentFrameBitmap == null || currentFaceRect == null) {
            showErrorDialog("Verification Failed", "No face detected");
            return;
        }
        
        // Final validation of face position
        if (faceOverlayView != null) {
            boolean isInGoodPosition = faceOverlayView.validateFaceWithinOval(currentFaceRect);
            if (!isInGoodPosition) {
                updateStatus("Please position your face properly in the oval");
                return;
            }
        }
        
        isProcessing = true;
        showLoading(true, "Verifying your Face ID...");
        setActionButtonEnabled(false);
        
        String userId = getCurrentUserId();
        if (userId == null) {
            isProcessing = false;
            showLoading(false, "Verification failed");
            return;
        }
        
        // Verify face ID with enhanced security validation using oval boundary
        faceIdProcessor.verifyFace(
                currentFrameBitmap,
                currentFaceRect,
                faceOverlayView != null ? faceOverlayView.getOvalRect() : null,
                userId,
                new FaceIdProcessingCallback() {
                    @Override
                    public void onSuccess(String message, Object metadata) {
                        if (!isAdded()) return;
                        
                        isProcessing = false;
                        showLoading(false, "Verification successful");
                        
                        // Get confidence score from metadata
                        float confidence = 0;
                        if (metadata instanceof Float) {
                            confidence = (Float) metadata;
                        }
                        
                        // Show success message
                        showSuccessDialog("Verification Successful",
                                "Your Face ID has been verified with " +
                                        Math.round(confidence * 100) + "% confidence",
                                "Continue",
                                () -> {
                                    if (isAdded()) {
                                        // Navigate back to settings after successful verification
                                        navController.navigate(R.id.action_verifyFaceId_to_settings);
                                    }
                                });
                    }
                    
                    @Override
                    public void onFailure(String errorMessage) {
                        if (!isAdded()) return;
                        
                        isProcessing = false;
                        showLoading(false, "Verification failed");
                        
                        // Show appropriate error message
                        if (errorMessage.contains("spoof") || errorMessage.contains("Spoof")) {
                            showErrorDialog("Security Alert",
                                    "Spoof detection triggered. Please ensure you're using a real face.");
                        } else if (errorMessage.contains("confidence") || errorMessage.contains("match")) {
                            showErrorDialog("Verification Failed",
                                    "Your face doesn't match our records. Please try again.");
                        } else if (errorMessage.contains("Network")) {
                            showErrorDialog("Network Error",
                                    "Could not connect to the server. Please check your connection.");
                        } else {
                            showErrorDialog("Verification Error", errorMessage);
                        }
                        
                        // Restart camera
                        startCamera();
                    }
                });
    }
    
    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
