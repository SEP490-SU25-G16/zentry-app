package vn.edu.fpt.zentryapp.faceid.ui.common;

import android.graphics.Bitmap;
import android.graphics.Rect;
import android.graphics.RectF;
import android.util.Log;

import androidx.fragment.app.Fragment;
import androidx.lifecycle.LifecycleOwner;

import vn.edu.fpt.zentryapp.faceid.data.service.FaceIdService;
import vn.edu.fpt.zentryapp.faceid.ui.components.CameraView;
import vn.edu.fpt.zentryapp.faceid.ui.components.OvalFaceOverlayView;

/**
 * Controller for handling camera operations and frame processing
 */
public class FaceIdCameraController {
    private static final String TAG = "FaceIdCameraController";
    
    private final Fragment fragment;
    private final CameraView cameraView;
    private final OvalFaceOverlayView faceOverlayView;
    private FaceIdService faceIdService;
    private FrameProcessedCallback frameProcessedCallback;
    
    // Spoof detection support
    private FaceIdSpoofDetectionController spoofDetectionController;
    
    public FaceIdCameraController(Fragment fragment, CameraView cameraView, OvalFaceOverlayView faceOverlayView) {
        this.fragment = fragment;
        this.cameraView = cameraView;
        this.faceOverlayView = faceOverlayView;
    }
    
    /**
     * Set the FaceIdService for processing frames
     */
    public void setFaceIdService(FaceIdService faceIdService) {
        this.faceIdService = faceIdService;
    }
    
    /**
     * Set the spoof detection controller
     */
    public void setSpoofDetectionController(FaceIdSpoofDetectionController controller) {
        this.spoofDetectionController = controller;
    }
    
    /**
     * Set callback for frame processing results
     */
    public void setFrameProcessedCallback(FrameProcessedCallback callback) {
        this.frameProcessedCallback = callback;
    }
    
    /**
     * Start camera with frame processing
     */
    public void startCamera(LifecycleOwner lifecycleOwner) {
        try {
            cameraView.startCamera(lifecycleOwner, this::processFrame);
        } catch (Exception e) {
            Log.e(TAG, "Failed to start camera: " + e.getMessage());
            if (frameProcessedCallback != null) {
                frameProcessedCallback.onFrameProcessed(null, null, false, false, 
                        "Camera error: " + e.getMessage());
            }
        }
    }
    
    /**
     * Stop camera
     */
    public void stopCamera() {
        try {
            if (cameraView != null) {
                cameraView.stopCamera();
            }
        } catch (Exception e) {
            Log.e(TAG, "Error stopping camera: " + e.getMessage());
        }
    }
    
    /**
     * Process camera frame with face detection
     */
    private void processFrame(Bitmap bitmap) {
        if (faceIdService == null || !fragment.isAdded()) {
            return;
        }
        
        // Get oval boundary for face validation
        RectF ovalRect = faceOverlayView != null ? faceOverlayView.getOvalRect() : null;
        
        // Process frame with face detection and spoof detection
        faceIdService.processContinuousFrame(bitmap, ovalRect, new FaceIdService.ContinuousProcessingCallback() {
            @Override
            public void onFaceDetected(Rect boundingBox, boolean isSpoof, float spoofScore) {
                if (!fragment.isAdded() || frameProcessedCallback == null) return;
                
                // Update face position in overlay for user guidance
                boolean isGoodPosition;
                if (faceOverlayView != null) {
                    isGoodPosition = faceOverlayView.updateFacePosition(boundingBox);
                } else {
                    isGoodPosition = true;
                }

                // Use enhanced spoof detection if available
                if (spoofDetectionController != null) {
                    spoofDetectionController.analyzeFrame(bitmap, boundingBox, result -> {
                        handleSpoofResult(bitmap, boundingBox, result, isGoodPosition);
                    });
                } else {
                    // Fallback to basic spoof detection
                    handleBasicSpoofResult(bitmap, boundingBox, isSpoof, spoofScore, isGoodPosition);
                }
            }
            
            @Override
            public void onNoFaceDetected() {
                if (!fragment.isAdded() || frameProcessedCallback == null) return;
                
                if (faceOverlayView != null) {
                    faceOverlayView.clear();
                }
                
                frameProcessedCallback.onFrameProcessed(bitmap, null, false, false, 
                        "No face detected. Look at the camera.");
            }
            
            @Override
            public void onMultipleFacesDetected() {
                if (!fragment.isAdded() || frameProcessedCallback == null) return;
                
                if (faceOverlayView != null) {
                    faceOverlayView.clear();
                }
                
                frameProcessedCallback.onFrameProcessed(bitmap, null, false, false, 
                        "Multiple faces detected. Only one face should be visible.");
            }
            
            @Override
            public void onError(String errorMessage) {
                if (!fragment.isAdded() || frameProcessedCallback == null) return;
                
                frameProcessedCallback.onFrameProcessed(bitmap, null, false, false, 
                        "Detection error: " + errorMessage);
            }
        });
    }
    
    /**
     * Handle enhanced spoof detection result
     */
    private void handleSpoofResult(Bitmap bitmap, Rect boundingBox, 
                                  FaceIdSpoofDetectionController.SpoofDetectionResult result, 
                                  boolean isGoodPosition) {
        if (!fragment.isAdded() || frameProcessedCallback == null) return;
        
        if (result.isSpoof) {
            frameProcessedCallback.onFrameProcessed(bitmap, boundingBox, isGoodPosition, true, 
                    result.explanation);
            return;
        }
        
        if (result.triggerLivenessChallenge) {
            // Request liveness challenge while keeping state responsive
            if (frameProcessedCallback != null) {
                frameProcessedCallback.onLivenessChallengeRequested(boundingBox);
            }
            return;
        }

        // For real face detections
        if (!result.isSpoof) {
            // Check face position using oval view
            boolean isInGoodPosition = isGoodPosition && faceOverlayView != null && 
                    faceOverlayView.validateFaceWithinOval(boundingBox);
            
            String message;
            if (!isInGoodPosition) {
                message = "Position your face properly in the oval";
            } else if (result.shouldProceed) {
                message = "Real face detected. Ready to proceed!";
            } else {
                message = "Keep your face steady";
            }
            
            frameProcessedCallback.onFrameProcessed(bitmap, boundingBox, isInGoodPosition, 
                    false, message);
        }
    }
    
    /**
     * Fallback basic spoof handling
     */
    private void handleBasicSpoofResult(Bitmap bitmap, Rect boundingBox, boolean isSpoof, 
                                       float spoofScore, boolean isGoodPosition) {
        if (!fragment.isAdded() || frameProcessedCallback == null) return;
        
        String message;
        boolean isSpoofed = isSpoof || spoofScore > 0.65f; // Lowered from 0.7f for more sensitivity
        boolean isDefinitelyReal = !isSpoof && spoofScore < 0.15f; // Decreased from 0.3f for more security
        
        if (isSpoofed) {
            message = "Spoof detected! Please use your real face.";
            frameProcessedCallback.onFrameProcessed(bitmap, boundingBox, isGoodPosition, true, message);
        } else if (isDefinitelyReal) {
            message = "Face detected. Ready to proceed!";
            frameProcessedCallback.onFrameProcessed(bitmap, boundingBox, isGoodPosition, false, message);
        } else {
            message = "Uncertain detection. Please improve lighting and position.";
            frameProcessedCallback.onFrameProcessed(bitmap, boundingBox, false, false, message);
        }
    }
    
    /**
     * Callback interface for frame processing results
     */
    public interface FrameProcessedCallback {
        void onFrameProcessed(Bitmap bitmap, Rect faceRect, boolean isValidPosition, boolean isSpoof, String statusMessage);
        void onLivenessChallengeRequested(Rect faceRect);
    }
}
