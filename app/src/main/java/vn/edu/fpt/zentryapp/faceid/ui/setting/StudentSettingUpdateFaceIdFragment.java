package vn.edu.fpt.zentryapp.faceid.ui.setting;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

import vn.edu.fpt.zentryapp.databinding.FragmentStudentSettingUpdateFaceIdBinding;
import vn.edu.fpt.zentryapp.faceid.data.service.FaceIdConfig;
import vn.edu.fpt.zentryapp.faceid.ui.common.BaseFaceIdFragment;
import vn.edu.fpt.zentryapp.faceid.ui.common.FaceIdProcessingCallback;
import vn.edu.fpt.zentryapp.faceid.ui.common.FaceIdProcessor;

/**
 * Fragment for updating Face ID
 * Refactored to use the BaseFaceIdFragment
 */
public class StudentSettingUpdateFaceIdFragment extends BaseFaceIdFragment {
    private static final String TAG = "UpdateFaceIdFragment";
    
    private FragmentStudentSettingUpdateFaceIdBinding binding;
    private FaceIdProcessor faceIdProcessor;
    private boolean isProcessing = false;
    
    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        binding = FragmentStudentSettingUpdateFaceIdBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }
    
    @Override
    protected void setupClickListeners() {
        binding.ivBack.setOnClickListener(v -> requireActivity().onBackPressed());
        binding.btnCancel.setOnClickListener(v -> requireActivity().onBackPressed());
        
        binding.btnUpdate.setOnClickListener(v -> {
            if (!isProcessing) {
                updateFaceId();
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
        binding.btnUpdate.setEnabled(enabled);
    }
    
    @Override
    protected FaceIdConfig.Scenario getScenario() {
        return FaceIdConfig.Scenario.UPDATE;
    }
    
    @Override
    protected void onFaceIdServiceInitialized() {
        faceIdProcessor = new FaceIdProcessor(faceIdService);
    }
    
    /**
     * Update Face ID using current frame
     */
    private void updateFaceId() {
        if (currentFrameBitmap == null || currentFaceRect == null) {
            showErrorDialog("Update Failed", "No face detected");
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
        showLoading(true, "Updating your Face ID...");
        setActionButtonEnabled(false);
        
        String userId = getCurrentUserId();
        if (userId == null) {
            isProcessing = false;
            showLoading(false, "Update failed");
            return;
        }
        
        // Stop camera while processing
        stopCamera();
        
        // Get current time for update record
        String updateTime = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
                .format(new Date());
        
        // Update face ID with enhanced security validation using oval boundary
        faceIdProcessor.registerFace(
                currentFrameBitmap,
                currentFaceRect,
                faceOverlayView != null ? faceOverlayView.getOvalRect() : null,
                userId,
                new FaceIdProcessingCallback() {
                    @Override
                    public void onSuccess(String message, Object metadata) {
                        if (!isAdded()) return;
                        
                        isProcessing = false;
                        showLoading(false, "Update successful");
                        
                        showSuccessDialog("Face ID Updated",
                                "Your Face ID was successfully updated on " + updateTime,
                                "OK",
                                () -> {
                                    if (isAdded()) {
                                        requireActivity().onBackPressed();
                                    }
                                });
                    }
                    
                    @Override
                    public void onFailure(String errorMessage) {
                        if (!isAdded()) return;
                        
                        isProcessing = false;
                        showLoading(false, "Update failed");
                        
                        if (errorMessage.contains("spoof") || errorMessage.contains("Spoof")) {
                            showErrorDialog("Security Alert",
                                    "Spoof detection triggered. Please ensure you're using a real face.");
                        } else if (errorMessage.contains("Network")) {
                            showErrorDialog("Network Error",
                                    "Could not connect to the server. Please check your connection.");
                        } else {
                            showErrorDialog("Update Failed", errorMessage);
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
