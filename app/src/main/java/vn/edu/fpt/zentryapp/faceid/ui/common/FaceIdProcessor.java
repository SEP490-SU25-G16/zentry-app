package vn.edu.fpt.zentryapp.faceid.ui.common;

import android.graphics.Bitmap;
import android.graphics.Rect;
import android.graphics.RectF;
import android.util.Log;

import vn.edu.fpt.zentryapp.faceid.data.service.FaceIdService;

/**
 * Processor for Face ID operations (registration, verification, update)
 */
public class FaceIdProcessor {
    private static final String TAG = "FaceIdProcessor";
    
    private final FaceIdService faceIdService;
    
    public FaceIdProcessor(FaceIdService faceIdService) {
        this.faceIdService = faceIdService;
    }
    
    /**
     * Register a face ID (for registration and update)
     */
    public void registerFace(Bitmap bitmap, Rect faceRect, RectF ovalRect, String userId,
                             FaceIdProcessingCallback callback) {
        if (faceIdService == null) {
            callback.onFailure("Face ID service not initialized");
            return;
        }
        
        // Register face with enhanced security validation using oval boundary
        faceIdService.captureAndRegisterFace(
                bitmap,
                faceRect,
                ovalRect,
                userId,
                new FaceIdService.FaceIdCallback() {
                    @Override
                    public void onSuccess(String message) {
                        callback.onSuccess(message, null);
                    }
                    
                    @Override
                    public void onFailure(String errorMessage) {
                        callback.onFailure(errorMessage);
                    }
                });
    }
    
    /**
     * Verify a face ID
     */
    public void verifyFace(Bitmap bitmap, Rect faceRect, RectF ovalRect, String userId,
                           FaceIdProcessingCallback callback) {
        if (faceIdService == null) {
            callback.onFailure("Face ID service not initialized");
            return;
        }
        
        // Verify face with enhanced security validation using oval boundary
        faceIdService.verifyFace(
                bitmap,
                faceRect,
                ovalRect,
                userId,
                new FaceIdService.FaceVerificationCallback() {
                    @Override
                    public void onVerified(float confidence) {
                        callback.onSuccess("Verification successful", confidence);
                    }
                    
                    @Override
                    public void onVerificationFailed(String reason) {
                        callback.onFailure(reason);
                    }
                    
                    @Override
                    public void onError(String errorMessage) {
                        callback.onFailure(errorMessage);
                    }
                });
    }
}
