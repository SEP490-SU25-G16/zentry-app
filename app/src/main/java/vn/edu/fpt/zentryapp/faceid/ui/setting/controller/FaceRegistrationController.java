package vn.edu.fpt.zentryapp.faceid.ui.setting.controller;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Rect;
import android.graphics.RectF;
import android.os.Handler;
import android.os.Looper;
import android.text.method.ScrollingMovementMethod;
import android.util.Log;
import android.widget.TextView;

import androidx.appcompat.app.AlertDialog;

import vn.edu.fpt.zentryapp.auth.client.AuthManager;
import vn.edu.fpt.zentryapp.faceid.data.service.FaceIdService;
import vn.edu.fpt.zentryapp.faceid.data.service.FaceTracker;
import vn.edu.fpt.zentryapp.faceid.ui.setting.state.FaceRegistrationState;
import vn.edu.fpt.zentryapp.faceid.ui.setting.state.FaceRegistrationStateManager;

/**
 * Controller for handling face registration process
 */
public class FaceRegistrationController {
    private static final String TAG = "FaceRegController";

    private final Context context;
    private final FaceIdService faceIdService;
    private final FaceRegistrationStateManager stateManager;
    private final RegistrationCallback callback;

    // Error tracking
    private String lastDetailedErrorMessage = "";
    private boolean hasDetailedError = false;

    // Face tracking
    private FaceTracker faceTracker;

    public interface RegistrationCallback {
        void onRegistrationSuccess(String message);
        void onRegistrationFailure(String errorMessage, FaceRegistrationState errorState);
    }

    public FaceRegistrationController(Context context,
                                      FaceIdService faceIdService,
                                      FaceRegistrationStateManager stateManager,
                                      RegistrationCallback callback) {
        this.context = context;
        this.faceIdService = faceIdService;
        this.stateManager = stateManager;
        this.callback = callback;

        // Initialize face tracker
        faceTracker = new FaceTracker(10);
    }

    /**
     * Track face stability with enhanced metrics
     */
    public void trackFaceStability(Rect boundingBox) {
        if (faceTracker != null) {
            faceTracker.trackFace(boundingBox, new FaceTracker.FaceStabilityCallback() {
                @Override
                public void onFaceStabilizing(float progress) {
                    int percentage = Math.round(progress * 100);
                    stateManager.transitionTo(FaceRegistrationState.FACE_STABILIZING,
                            "Hold still... " + percentage + "%");
                }

                @Override
                public void onFaceStable(Rect stableFaceRect) {
                    stateManager.transitionTo(FaceRegistrationState.FACE_STABLE, "Perfect!");
                }

                @Override
                public void onFaceUnstable() {
                    stateManager.transitionTo(FaceRegistrationState.FACE_REAL,
                            "Keep your face steady");
                }
            });
        }
    }

    /**
     * Capture and register face with enhanced security validation
     */
    public void captureAndRegisterFace(Bitmap bitmap, Rect faceRect, RectF ovalRect) {
        if (bitmap == null || faceRect == null) {
            callback.onRegistrationFailure("No face detected", FaceRegistrationState.FAILED_OTHER);
            return;
        }

        stateManager.transitionTo(FaceRegistrationState.PROCESSING, "Processing face data...");

        String userId;
        try {
            userId = AuthManager.getInstance(context).getCurrentUserId();
            if (userId == null || userId.isEmpty()) {
                callback.onRegistrationFailure("User not logged in", FaceRegistrationState.FAILED_OTHER);
                return;
            }
        } catch (Exception e) {
            callback.onRegistrationFailure("Error getting user ID", FaceRegistrationState.FAILED_OTHER);
            return;
        }

        // Register face with enhanced security validation
        faceIdService.captureAndRegisterFace(
                bitmap,
                faceRect,
                ovalRect,
                userId,
                new FaceIdService.FaceIdCallback() {
                    @Override
                    public void onSuccess(String message) {
                        callback.onRegistrationSuccess(message);
                    }

                    @Override
                    public void onFailure(String errorMessage) {
                        // Store detailed error information for UI display
                        lastDetailedErrorMessage = "Registration failure details:\n" + errorMessage;
                        hasDetailedError = true;

                        // Enhanced error categorization
                        FaceRegistrationState errorState;

                        if (errorMessage.contains("timeout") || errorMessage.contains("Timeout")) {
                            errorState = FaceRegistrationState.FAILED_NETWORK;
                        } else if (errorMessage.contains("Network error") || errorMessage.contains("Cannot connect")) {
                            errorState = FaceRegistrationState.FAILED_NETWORK;
                        } else if (errorMessage.contains("spoof") || errorMessage.contains("Spoof")) {
                            errorState = FaceRegistrationState.FAILED_SPOOF;
                        } else {
                            errorState = FaceRegistrationState.FAILED_OTHER;
                        }

                        callback.onRegistrationFailure(errorMessage, errorState);
                    }
                });
    }

    /**
     * Handle error states with retry dialog
     */
    public void handleErrorState(FaceRegistrationState state) {
        // Prepare error message based on state
        String title = "Registration Failed";
        String message;

        // Set appropriate message based on error type
        if (state == FaceRegistrationState.FAILED_NETWORK) {
            title = "Network Connection Issue";
            message = "Cannot connect to the server. Please check your internet connection and try again.";
        } else if (state == FaceRegistrationState.FAILED_SPOOF) {
            message = "Spoof detection triggered. Please ensure you're using a real face and not a photo or video.\n\nWould you like to try again?";
        } else {
            message = state.getDefaultMessage() + "\n\nWould you like to try again?";
        }

        // Add detailed error information if available
        final String detailedMessage = hasDetailedError ?
                message + "\n\n--- DETAILED ERROR INFORMATION ---\n" + lastDetailedErrorMessage : message;

        // Log the detailed error for debugging
        Log.e(TAG, "Detailed error information: " + detailedMessage);

        // Show error dialog
        AlertDialog.Builder builder = new AlertDialog.Builder(context)
                .setTitle(title)
                .setMessage(detailedMessage)
                .setPositiveButton("Retry", (dialog, which) -> {
                    // Reset error tracking
                    hasDetailedError = false;
                    lastDetailedErrorMessage = "";

                    // Reset components
                    resetComponents();

                    // Small delay to ensure complete reset
                    new Handler(Looper.getMainLooper()).postDelayed(() -> {
                        stateManager.transitionTo(FaceRegistrationState.READY, "Position your face in the oval");
                    }, 500);
                })
                .setNegativeButton("Cancel", (dialog, which) -> {
                    // Reset error tracking
                    hasDetailedError = false;
                    lastDetailedErrorMessage = "";
                })
                .setCancelable(false);

        // Create and show the dialog
        AlertDialog dialog = builder.create();
        dialog.show();

        // Make the message scrollable for long detailed errors
        TextView messageView = dialog.findViewById(android.R.id.message);
        if (messageView != null) {
            messageView.setMovementMethod(new ScrollingMovementMethod());
        }
    }

    /**
     * Reset all components
     */
    public void resetComponents() {
        // Reset face tracker
        if (faceTracker != null) {
            faceTracker.reset();
        }

        // Reset error tracking
        hasDetailedError = false;
        lastDetailedErrorMessage = "";
    }
}