package vn.edu.fpt.zentryapp.faceid.ui.common;

import android.Manifest;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.Rect;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

import vn.edu.fpt.zentryapp.auth.client.AuthManager;
import vn.edu.fpt.zentryapp.faceid.data.service.FaceIdConfig;
import vn.edu.fpt.zentryapp.faceid.data.service.FaceIdService;
import vn.edu.fpt.zentryapp.faceid.data.service.FaceIdServiceManager;
import vn.edu.fpt.zentryapp.faceid.ui.components.CameraView;
import vn.edu.fpt.zentryapp.faceid.ui.components.OvalFaceOverlayView;

/**
 * Base fragment for Face ID operations (registration, verification, update)
 * Handles common functionality like camera setup, permissions, and face processing
 */
public abstract class BaseFaceIdFragment extends Fragment {
    private static final String TAG = "BaseFaceIdFragment";
    private static final int CAMERA_PERMISSION_REQUEST_CODE = 100;

    // Core components
    protected FaceIdService faceIdService;
    protected FaceIdCameraController cameraController;
    protected FaceIdSpoofDetectionController spoofDetectionController;
    protected final Handler mainHandler = new Handler(Looper.getMainLooper());
    
    // UI components
    protected CameraView cameraView;
    protected OvalFaceOverlayView faceOverlayView;
    
    // State tracking
    protected boolean isCameraStarted = false;
    protected boolean isProcessing = false;
    protected Bitmap currentFrameBitmap;
    protected Rect currentFaceRect;

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        
        setupCameraAndOverlay();
        initializeControllers();
        setupClickListeners();
        
        // Initialize FaceIdService
        initializeFaceIdService();
    }
    
    /**
     * Setup camera view and face overlay
     */
    protected void setupCameraAndOverlay() {
        ViewGroup cameraContainer = getCameraContainer();
        if (cameraContainer == null) return;
        
        // Setup camera view
        cameraView = new CameraView(requireContext());
        cameraContainer.addView(cameraView);
        
        // Setup overlay view
        faceOverlayView = new OvalFaceOverlayView(requireContext());
        cameraContainer.addView(faceOverlayView);
    }
    
    /**
     * Initialize FaceIdService with appropriate scenario
     */
    protected void initializeFaceIdService() {
        showLoading(true, "Initializing...");
        
        FaceIdServiceManager.getInstance().initialize(requireContext(), new FaceIdServiceManager.InitCallback() {
            @Override
            public void onInitialized(FaceIdService service) {
                if (!isAdded()) return;
                
                faceIdService = service;
                
                // Set scenario based on operation type
                faceIdService.setScenario(getScenario());
                
                // Initialize spoof detection controller
                if (faceIdService.getFaceSpoofDetector() != null) {
                    spoofDetectionController.initialize(
                            faceIdService.getFaceSpoofDetector(),
                            faceOverlayView != null ? faceOverlayView.getOvalRect() : null
                    );
                    // Wire spoof detection into camera pipeline so enhanced path is used
                    cameraController.setSpoofDetectionController(spoofDetectionController);
                }
                
                showLoading(false, "Look at the camera");
                
                // Initialize camera controller with the service
                cameraController.setFaceIdService(faceIdService);
                
                onFaceIdServiceInitialized();
                checkCameraPermissionAndStart();
            }
            
            @Override
            public void onError(String message) {
                if (!isAdded()) return;
                
                showLoading(false, "Initialization failed");
                
                showErrorDialog("Failed to initialize face detection", message);
            }
        });
    }
    
    /**
     * Initialize controllers
     */
    protected void initializeControllers() {
        // Create camera controller
        cameraController = new FaceIdCameraController(this, cameraView, faceOverlayView);
        
        // Create spoof detection controller
        spoofDetectionController = new FaceIdSpoofDetectionController(requireContext());
        
        // Set camera controller callbacks (frame + liveness requests)
        cameraController.setFrameProcessedCallback(new FaceIdCameraController.FrameProcessedCallback() {
            @Override
            public void onFrameProcessed(Bitmap bitmap, Rect faceRect, boolean isValidPosition, boolean isSpoof, String statusMessage) {
                BaseFaceIdFragment.this.onFrameProcessed(bitmap, faceRect, isValidPosition, isSpoof, statusMessage);
            }

            @Override
            public void onLivenessChallengeRequested(Rect faceRect) {
                BaseFaceIdFragment.this.onLivenessChallengeRequested(faceRect);
            }
        });
    }

    /**
     * Called when spoof detection requests a liveness challenge. Subclasses may override.
     */
    protected void onLivenessChallengeRequested(Rect faceRect) { /* no-op by default */ }
    
    /**
     * Check camera permission and start camera if granted
     */
    protected void checkCameraPermissionAndStart() {
        if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.CAMERA)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(requireActivity(),
                    new String[]{Manifest.permission.CAMERA},
                    CAMERA_PERMISSION_REQUEST_CODE);
        } else {
            startCamera();
        }
    }
    
    /**
     * Start camera
     */
    protected void startCamera() {
        cameraController.startCamera(getViewLifecycleOwner());
        isCameraStarted = true;
        updateStatus("Position your face in the oval");
    }
    
    /**
     * Stop camera
     */
    protected void stopCamera() {
        cameraController.stopCamera();
        isCameraStarted = false;
    }
    
    /**
     * Called when a frame is processed with face detection results
     */
    protected void onFrameProcessed(Bitmap bitmap, Rect faceRect, boolean isValidPosition, boolean isSpoof, String statusMessage) {
        currentFrameBitmap = bitmap;
        currentFaceRect = faceRect;
        
        updateStatus(statusMessage);
        setActionButtonEnabled(!isSpoof && isValidPosition);
    }
    
    /**
     * Get current user ID safely
     */
    protected String getCurrentUserId() {
        String userId = AuthManager.getInstance(requireContext()).getCurrentUserId();
        if (userId == null || userId.isEmpty()) {
            showErrorDialog("Operation Failed", "User not logged in");
            return null;
        }
        return userId;
    }
    
    /**
     * Show error dialog
     */
    protected void showErrorDialog(String title, String message) {
        if (!isAdded()) return;
        
        new AlertDialog.Builder(requireContext())
                .setTitle(title)
                .setMessage(message)
                .setPositiveButton("OK", null)
                .show();
    }
    
    /**
     * Show success dialog
     */
    protected void showSuccessDialog(String title, String message, String buttonText, Runnable onComplete) {
        if (!isAdded()) return;
        
        new AlertDialog.Builder(requireContext())
                .setTitle(title)
                .setMessage(message)
                .setPositiveButton(buttonText, (dialog, which) -> {
                    if (onComplete != null) {
                        onComplete.run();
                    }
                })
                .setCancelable(false)
                .show();
    }
    
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        if (requestCode == CAMERA_PERMISSION_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                startCamera();
            } else {
                showErrorDialog("Permission Required", 
                        "Camera permission is required for Face ID");
                
                // Go back if permission denied
                requireActivity().onBackPressed();
            }
        }
    }
    
    @Override
    public void onDestroyView() {
        super.onDestroyView();
        
        // Clean up
        stopCamera();
    }
    
    // Abstract methods to be implemented by subclasses
    
    /**
     * Setup click listeners for UI components
     */
    protected abstract void setupClickListeners();
    
    /**
     * Get the camera container view
     */
    protected abstract ViewGroup getCameraContainer();
    
    /**
     * Update loading state
     */
    protected abstract void showLoading(boolean isLoading, String message);
    
    /**
     * Update status message
     */
    protected abstract void updateStatus(String message);
    
    /**
     * Enable/disable action button
     */
    protected abstract void setActionButtonEnabled(boolean enabled);
    
    /**
     * Get the FaceId scenario for this operation
     */
    protected abstract FaceIdConfig.Scenario getScenario();
    
    /**
     * Called when FaceIdService is initialized
     */
    protected abstract void onFaceIdServiceInitialized();
}
