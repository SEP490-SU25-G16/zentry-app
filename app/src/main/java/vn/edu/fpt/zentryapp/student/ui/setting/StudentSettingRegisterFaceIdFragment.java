package vn.edu.fpt.zentryapp.student.ui.setting;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.Rect;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;


import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

import vn.edu.fpt.zentryapp.auth.client.AuthManager;
import vn.edu.fpt.zentryapp.databinding.FragmentStudentSettingRegisterFaceIdBinding;
import vn.edu.fpt.zentryapp.student.data.service.FaceIdService;
import vn.edu.fpt.zentryapp.student.data.service.FaceIdServiceManager;
import vn.edu.fpt.zentryapp.student.data.service.FaceTracker;
import vn.edu.fpt.zentryapp.student.ui.components.CameraView;
import vn.edu.fpt.zentryapp.student.ui.components.OvalFaceOverlayView;
import vn.edu.fpt.zentryapp.student.ui.setting.detection.SpoofDetectionManager;
import vn.edu.fpt.zentryapp.student.ui.setting.state.FaceRegistrationState;
import vn.edu.fpt.zentryapp.student.ui.setting.state.FaceRegistrationStateManager;
import vn.edu.fpt.zentryapp.student.ui.setting.success.FaceIdSuccessActivity;
import vn.edu.fpt.zentryapp.student.ui.setting.ui.FaceRegistrationUIController;

public class StudentSettingRegisterFaceIdFragment extends Fragment {
    private static final String TAG = "RegisterFaceIdFragment";
    private static final int CAMERA_PERMISSION_REQUEST_CODE = 100;
    private static final int SUCCESS_ACTIVITY_REQUEST_CODE = 200;

    // 🎯 CORE COMPONENTS (Clean Architecture)
    private FragmentStudentSettingRegisterFaceIdBinding binding;
    private FaceRegistrationStateManager stateManager;
    private SpoofDetectionManager spoofDetectionManager;
    private FaceRegistrationUIController uiController;
    private FaceTracker faceTracker;
    private FaceIdService faceIdService;

    // 📷 CAMERA COMPONENTS
    private CameraView cameraView;
    private OvalFaceOverlayView faceOverlayView;
    private boolean isCameraStarted = false;

    // 💾 CURRENT DATA
    private Bitmap currentFrameBitmap;
    private Rect currentFaceRect;

    // 🔄 HANDLERS
    private final Handler mainHandler = new Handler(Looper.getMainLooper());

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        binding = FragmentStudentSettingRegisterFaceIdBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        initializeComponents();
        setupClickListeners();

        Log.d(TAG, "✅ Fragment initialized with clean architecture");
    }

    /**
     * 🏗️ Initialize all core components
     */
    private void initializeComponents() {
        // 1. State Manager with callback
        stateManager = new FaceRegistrationStateManager();
        stateManager.setStateChangeListener(this::onStateChanged);

        // 2. Camera and Overlay
        setupCameraAndOverlay();

        // 3. UI Controller
        uiController = new FaceRegistrationUIController(binding, faceOverlayView);
        uiController.showScreen(FaceRegistrationUIController.UIScreenState.SETUP);

        // 4. Face Tracker with optimized settings for stability
        faceTracker = new FaceTracker(10); // Increased from 8 to 10 frames for better stability (~ 0.33 seconds)

        Log.d(TAG, "📦 All components initialized successfully");
    }

    private void setupCameraAndOverlay() {
        cameraView = new CameraView(requireContext());
        binding.flStudentSettingRegisterFaceIdCameraContainer.addView(cameraView);

        faceOverlayView = new OvalFaceOverlayView(requireContext());
        binding.flStudentSettingRegisterFaceIdCameraContainer.addView(faceOverlayView);
    }

    private void setupClickListeners() {
        binding.ivStudentSettingRegisterFaceIdBack.setOnClickListener(v ->
                requireActivity().onBackPressed());

        binding.ivCameraBack.setOnClickListener(v -> {
            if (uiController.getCurrentScreenState() == FaceRegistrationUIController.UIScreenState.CAMERA) {
                backToSetup();
            } else {
                requireActivity().onBackPressed();
            }
        });

        binding.btnGetStarted.setOnClickListener(v -> startFaceRegistration());
        binding.btnNotNow.setOnClickListener(v -> requireActivity().onBackPressed());
    }

    /**
     * 🔄 State change callback from StateManager
     */
    private void onStateChanged(FaceRegistrationState state, String message) {
        if (!isAdded() || binding == null) {
            Log.w(TAG, "⚠️ Fragment not valid for state change: " + state);
            return;
        }

        Log.d(TAG, "🔄 State: " + state + " - " + message);

        // Update UI
        uiController.updateForState(state, message);

        // Handle state-specific actions
        handleStateActions(state);
    }

    /**
     * Handle actions for specific states
     */
    private void handleStateActions(FaceRegistrationState state) {
        switch (state) {
            case SUCCESS:
                handleSuccessState();
                break;

            case FAILED_SPOOF:
            case FAILED_NETWORK:
            case FAILED_OTHER:
            case TIMEOUT_DETECTION:
            case TIMEOUT_REGISTRATION:
                handleErrorState(state);
                break;

            case FACE_STABLE:
                // 🎯 Capture after a short delay
                mainHandler.postDelayed(() -> {
                    if (stateManager.getCurrentState() == FaceRegistrationState.FACE_STABLE) {
                        captureAndRegisterFace();
                    }
                }, 500);
                break;
        }
    }

    /**
     * 🚀 Start face registration process
     */
    private void startFaceRegistration() {
        Log.d(TAG, "🚀 Starting face registration process");

        // Ensure we have a clean state
        stopCamera();
        resetComponents();

        // Show camera screen
        uiController.showScreen(FaceRegistrationUIController.UIScreenState.CAMERA);

        // Proceed with initialization
        initializeFaceIdService();
    }

    /**
     * Initialize FaceIdService and related components
     */
    private void initializeFaceIdService() {
        stateManager.transitionTo(FaceRegistrationState.INITIALIZING, "Loading AI models...");

        // Check if already initialized to prevent duplicate initializations
        if (FaceIdServiceManager.getInstance().isInitialized() && faceIdService != null) {
            Log.d(TAG, "✅ FaceIdService already initialized, proceeding to camera");
            initializeSpoofDetection();
            checkCameraPermissionAndStart();
            return;
        }

        FaceIdServiceManager.getInstance().initialize(requireContext(), new FaceIdServiceManager.InitCallback() {
            @Override
            public void onInitialized(FaceIdService service) {
                if (!isAdded()) return;

                faceIdService = service;

                // Initialize SpoofDetectionManager with FaceSpoofDetector
                initializeSpoofDetection();

                checkCameraPermissionAndStart();
                Log.d(TAG, "✅ FaceIdService initialized");
            }

            @Override
            public void onError(String message) {
                if (!isAdded()) return;

                Log.e(TAG, "❌ FaceIdService error: " + message);
                stateManager.transitionTo(FaceRegistrationState.FAILED_OTHER,
                        "Failed to initialize: " + message);
            }
        });
    }

    /**
     * Initialize spoof detection with FaceSpoofDetector
     */
    private void initializeSpoofDetection() {
        if (faceIdService != null && faceIdService.getFaceSpoofDetector() != null) {
            spoofDetectionManager = new SpoofDetectionManager(faceIdService.getFaceSpoofDetector());
            // Set the oval boundary for enhanced security validation
            if (faceOverlayView != null) {
                spoofDetectionManager.setOvalBoundary(faceOverlayView.getOvalRect());
            }
            Log.d(TAG, "✅ SpoofDetectionManager initialized with oval boundary");
        } else {
            Log.w(TAG, "⚠️ FaceSpoofDetector not available, using fallback detection");
        }
    }

    private void checkCameraPermissionAndStart() {
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
     * 📷 Start camera and begin processing
     */
    private void startCamera() {
        // First check if fragment is still attached
        if (!isAdded()) {
            Log.w(TAG, "Fragment not attached, cannot start camera");
            return;
        }

        // Make sure camera is stopped first to prevent duplicate instances
        stopCamera();

        // Show loading state for camera initialization
        uiController.showLoadingOverlay(true);

        // Add a small delay to ensure camera is properly released
        new Handler(Looper.getMainLooper()).postDelayed(() -> {
            // Check again if fragment is still attached before proceeding
            if (!isAdded() || cameraView == null) {
                Log.w(TAG, "Fragment not attached or camera view is null after delay");
                return;
            }

            try {
                Log.d(TAG, "Starting camera after delay...");
                cameraView.startCamera(getViewLifecycleOwner(), this::processFrame);
                isCameraStarted = true;

                // Check again before updating UI
                if (!isAdded()) {
                    Log.w(TAG, "Fragment detached after starting camera");
                    return;
                }

                // Hide loading and show camera view
                uiController.showLoadingOverlay(false);

                Log.d(TAG, "✅ Camera started successfully");
                stateManager.transitionTo(FaceRegistrationState.READY,
                        "Position your face in the oval");
            } catch (Exception e) {
                // Check if fragment is still attached before updating state
                if (!isAdded()) {
                    Log.w(TAG, "Fragment detached during camera error handling");
                    return;
                }

                Log.e(TAG, "❌ Error starting camera: " + e.getMessage(), e);
                stateManager.transitionTo(FaceRegistrationState.FAILED_CAMERA,
                        "Failed to start camera: " + e.getMessage());
            }
        }, 500); // Small delay to ensure previous camera is fully released
    }

    /**
     * 🔍 Process camera frame with enhanced security logic
     */
    private void processFrame(Bitmap bitmap) {
        currentFrameBitmap = bitmap;

        // Skip if not ready
        if (faceIdService == null) {
            return;
        }

        // Skip if already in final state
        if (stateManager.getCurrentState().isFinalState()) {
            return;
        }

        // Process frame with oval boundary validation
        faceIdService.processContinuousFrame(bitmap, faceOverlayView.getOvalRect(), 
                new FaceIdService.ContinuousProcessingCallback() {
            @Override
            public void onFaceDetected(Rect boundingBox, boolean isSpoof, float spoofScore) {
                currentFaceRect = boundingBox;
                
                // Update face position in overlay for user guidance
                if (faceOverlayView != null) {
                    boolean isGoodPosition = faceOverlayView.updateFacePosition(boundingBox);
                    
                    // If position is bad, don't proceed with further processing
                    if (!isGoodPosition && stateManager.getCurrentState() != FaceRegistrationState.FACE_OUT_OF_BOUNDS) {
                        stateManager.transitionTo(FaceRegistrationState.FACE_OUT_OF_BOUNDS, 
                                "Position your face properly in the oval");
                        return;
                    }
                }

                // 🔧 Use enhanced spoof detection if available
                if (spoofDetectionManager != null) {
                    spoofDetectionManager.analyzeFrame(bitmap, boundingBox, result -> {
                        handleEnhancedSpoofResult(result, boundingBox);
                    });
                } else {
                    // Fallback to basic logic
                    handleBasicSpoofResult(isSpoof, spoofScore, boundingBox);
                }
            }

            @Override
            public void onNoFaceDetected() {
                currentFaceRect = null;
                stateManager.transitionTo(FaceRegistrationState.NO_FACE, "Look at the camera");
                resetFaceTracker();
            }

            @Override
            public void onMultipleFacesDetected() {
                currentFaceRect = null;
                stateManager.transitionTo(FaceRegistrationState.MULTIPLE_FACES,
                        "Only one face should be visible");
                resetFaceTracker();
            }

            @Override
            public void onError(String errorMessage) {
                Log.e(TAG, "❌ Frame processing error: " + errorMessage);
                stateManager.transitionTo(FaceRegistrationState.FAILED_OTHER,
                        "Detection error: " + errorMessage);
            }
        });
    }

    /**
     * Enhanced spoof result handling with better real face detection
     */
    private void handleEnhancedSpoofResult(SpoofDetectionManager.SpoofDetectionResult result, Rect boundingBox) {
        if (!isAdded() || stateManager.getCurrentState().isFinalState()) {
            return;
        }

        Log.d(TAG, "🔍 Enhanced spoof result: isSpoof=" + result.isSpoof + 
              ", confidence=" + result.confidence + 
              ", level=" + result.confidenceLevel + 
              ", shouldProceed=" + result.shouldProceed + 
              ", explanation=" + result.explanation);

        // Handle spoof detection with improved thresholds
        if (result.isSpoof) {
            // For high or medium confidence spoof detections, immediately transition to spoofed state
            if (result.confidenceLevel == SpoofDetectionManager.ConfidenceLevel.HIGH || 
                result.confidenceLevel == SpoofDetectionManager.ConfidenceLevel.MEDIUM) {
                stateManager.transitionTo(FaceRegistrationState.FACE_SPOOFED, result.explanation);
                resetFaceTracker();
                return;
            }
            
            // For lower confidence spoof, show warning but don't fail immediately
            if (stateManager.getCurrentState() != FaceRegistrationState.FACE_WARNING) {
                stateManager.transitionTo(FaceRegistrationState.FACE_WARNING, result.explanation);
            }
            resetFaceTracker();
            return;
        }

        // 🆕 IMPROVED: Better handling for real face detections
        if (!result.isSpoof) {
            // Update state based on confidence level
            if (result.confidenceLevel == SpoofDetectionManager.ConfidenceLevel.HIGH) {
                if (stateManager.getCurrentState() != FaceRegistrationState.FACE_REAL &&
                    stateManager.getCurrentState() != FaceRegistrationState.FACE_STABILIZING) {
                    stateManager.transitionTo(FaceRegistrationState.FACE_REAL, "High confidence real face detected");
                }
            } else if (result.confidenceLevel == SpoofDetectionManager.ConfidenceLevel.MEDIUM) {
                if (stateManager.getCurrentState() != FaceRegistrationState.FACE_REAL &&
                    stateManager.getCurrentState() != FaceRegistrationState.FACE_STABILIZING) {
                    stateManager.transitionTo(FaceRegistrationState.FACE_REAL, "Real face detected");
                }
            } else {
                // Low confidence real face - show guidance
                if (stateManager.getCurrentState() != FaceRegistrationState.FACE_WARNING) {
                    stateManager.transitionTo(FaceRegistrationState.FACE_WARNING, 
                        "Low confidence detection - please improve lighting and position");
                }
                resetFaceTracker();
                return;
            }

            // 🆕 IMPROVED: Process based on should proceed flag with better feedback
            if (result.shouldProceed) {
                stateManager.transitionTo(FaceRegistrationState.FACE_STABLE, "Ready to capture!");
            } else {
                // Provide better guidance based on explanation
                String guidance = result.explanation;
                if (guidance.contains("need")) {
                    // Extract frame count needed
                    if (guidance.contains("1 more frame")) {
                        guidance = "Almost there! Hold steady for 1 more frame";
                    } else if (guidance.contains("2 more frames")) {
                        guidance = "Keep holding! Need 2 more frames";
                    } else {
                        guidance = "Hold steady! " + guidance;
                    }
                }
                
                if (stateManager.getCurrentState() != FaceRegistrationState.FACE_STABILIZING) {
                    stateManager.transitionTo(FaceRegistrationState.FACE_STABILIZING, guidance);
                }
                trackFaceStability(boundingBox);
            }
        }
    }

    /**
     * Fallback basic spoof handling (improved for better real face detection)
     */
    private void handleBasicSpoofResult(boolean isSpoof, float spoofScore, Rect boundingBox) {
        Log.d(TAG, "🔧 Using basic spoof detection: isSpoof=" + isSpoof + ", score=" + spoofScore);

        // 🆕 IMPROVED: More lenient thresholds for real face detection
        if (isSpoof && spoofScore > 0.70f) {  // Increased from 0.65f for better security
            stateManager.transitionTo(FaceRegistrationState.FACE_SPOOFED,
                    "Spoof detected! Please use a real face.");
            resetFaceTracker();
        } else if (!isSpoof && spoofScore > 0.60f) {  // Reduced from 0.75f for better real face detection
            stateManager.transitionTo(FaceRegistrationState.FACE_REAL, "Real face detected");
            trackFaceStability(boundingBox);
        } else if (!isSpoof && spoofScore > 0.40f) {  // 🆕 NEW: Allow lower confidence real faces
            // Low confidence real face - show guidance
            stateManager.transitionTo(FaceRegistrationState.FACE_WARNING, 
                "Low confidence detection - please improve lighting and position");
            resetFaceTracker();
        } else {
            // Uncertain cases now show warning
            stateManager.transitionTo(FaceRegistrationState.FACE_WARNING, 
                    "Uncertain detection. Please improve lighting and position.");
            resetFaceTracker();
        }
    }

    /**
     * Track face stability with enhanced metrics
     */
    private void trackFaceStability(Rect boundingBox) {
        if (faceTracker != null) {
            faceTracker.trackFace(boundingBox, new FaceTracker.FaceStabilityCallback() {
                @Override
                public void onFaceStabilizing(float progress) {
                    if (!isAdded()) return;

                    int percentage = Math.round(progress * 100);
                    stateManager.transitionTo(FaceRegistrationState.FACE_STABILIZING,
                            "Hold still... " + percentage + "%");
                    
                    // Update progress animation in overlay
                    if (faceOverlayView != null && percentage > 0) {
                        faceOverlayView.startProgressAnimation(3000); // 3 second animation
                    }
                }

                @Override
                public void onFaceStable(Rect stableFaceRect) {
                    if (!isAdded()) return;

                    currentFaceRect = stableFaceRect;
                    stateManager.transitionTo(FaceRegistrationState.FACE_STABLE, "Perfect!");
                }

                @Override
                public void onFaceUnstable() {
                    if (!isAdded()) return;

                    stateManager.transitionTo(FaceRegistrationState.FACE_REAL,
                            "Keep your face steady");
                    
                    // Stop progress animation
                    if (faceOverlayView != null) {
                        faceOverlayView.stopProgressAnimation();
                    }
                }
            });
        }
    }

    /**
     * 📸 Capture and register face with enhanced security validation
     */
    private void captureAndRegisterFace() {
        // Check if fragment is still attached
        if (!isAdded()) {
            Log.w(TAG, "Fragment not attached, cannot capture and register face");
            return;
        }

        if (currentFrameBitmap == null || currentFaceRect == null) {
            Log.w(TAG, "⚠️ Cannot capture - no frame or face rect");
            stateManager.transitionTo(FaceRegistrationState.FAILED_OTHER,
                    "Capture failed - no data available");
            return;
        }

        stateManager.transitionTo(FaceRegistrationState.PROCESSING, "Registering your face...");

        // Check again if fragment is still attached
        if (!isAdded()) {
            Log.w(TAG, "Fragment detached during face registration process");
            return;
        }

        String userId;
        try {
            userId = AuthManager.getInstance(requireContext()).getCurrentUserId();
            if (userId == null || userId.isEmpty()) {
                Log.e(TAG, "❌ No user ID available");
                stateManager.transitionTo(FaceRegistrationState.FAILED_OTHER, "User not logged in");
                return;
            }
        } catch (IllegalStateException e) {
            Log.e(TAG, "❌ Fragment not attached when getting user ID", e);
            return;
        }

        // Stop camera before registration to prevent infinite loop on error
        stopCamera();

        // Capture local copies for use in callback
        final Bitmap capturedBitmap = currentFrameBitmap;
        final Rect capturedFaceRect = currentFaceRect;
        final String finalUserId = userId;

        // 🎯 Register face with enhanced security validation
        faceIdService.captureAndRegisterFace(
                capturedBitmap, 
                capturedFaceRect,
                faceOverlayView != null ? faceOverlayView.getOvalRect() : null,
                finalUserId,
                new FaceIdService.FaceIdCallback() {
                    @Override
                    public void onSuccess(String message) {
                        if (!isAdded()) {
                            Log.w(TAG, "Fragment not attached during success callback");
                            return;
                        }

                        Log.d(TAG, "✅ Registration successful: " + message);
                        stateManager.transitionTo(FaceRegistrationState.SUCCESS, message);
                    }

                    @Override
                    public void onFailure(String errorMessage) {
                        if (!isAdded()) {
                            Log.w(TAG, "Fragment not attached during failure callback");
                            return;
                        }

                        Log.e(TAG, "❌ Registration failed: " + errorMessage);
                        if (errorMessage.contains("Network error")) {
                            handleNetworkError(errorMessage);
                        } else if (errorMessage.contains("spoof") || errorMessage.contains("Spoof")) {
                            stateManager.transitionTo(FaceRegistrationState.FAILED_SPOOF,
                                    "Registration failed: " + errorMessage);
                        } else {
                            stateManager.transitionTo(FaceRegistrationState.FAILED_OTHER,
                                    "Registration failed: " + errorMessage);
                        }
                    }
                });
    }

    /**
     * 🎉 Handle success - Navigate to Success Activity
     */
    private void handleSuccessState() {
        // Check if fragment is still attached before proceeding
        if (!isAdded()) {
            Log.w(TAG, "Fragment not attached, cannot handle success state");
            return;
        }

        try {
            stopCamera();

            // Save bitmap for background sync
            String bitmapPath = saveBitmapToTempFile(currentFrameBitmap);
            String userId = AuthManager.getInstance(requireContext()).getCurrentUserId();
            String successMessage = "Face ID has been registered successfully!";

            // Double-check fragment is still attached before starting activity
            if (!isAdded()) {
                Log.w(TAG, "Fragment detached during success handling");
                return;
            }

            // 🚀 Launch Success Activity
            Intent successIntent = FaceIdSuccessActivity.createIntent(
                    requireContext(), userId, successMessage, bitmapPath);
            startActivityForResult(successIntent, SUCCESS_ACTIVITY_REQUEST_CODE);

            Log.d(TAG, "🎉 Navigating to Success Activity");

        } catch (Exception e) {
            Log.e(TAG, "❌ Error handling success", e);

            // Check if fragment is still attached before showing toast
            if (isAdded()) {
                Toast.makeText(requireContext(), "Registration completed!", Toast.LENGTH_LONG).show();

                // Check again before calling onBackPressed
                if (isAdded()) {
                    requireActivity().onBackPressed();
                }
            }
        }
    }

    /**
     * Handle network errors specifically with mock success option
     */
    private void handleNetworkError(String errorMessage) {
        if (!isAdded()) return;

        // Create alert dialog with options
        new AlertDialog.Builder(requireContext())
                .setTitle("Network Connection Issue")
                .setMessage("Cannot connect to the server. Would you like to:\n" +
                        "1. Try again\n" +
                        "2. Continue with offline mode (demo only)")
                .setPositiveButton("Try Again", (dialog, which) -> {
                    // Check if fragment is still attached before proceeding
                    if (!isAdded()) {
                        Log.w(TAG, "Fragment not attached, cannot retry registration");
                        return;
                    }

                    // Reset and try again
                    resetComponents();
                    startFaceRegistration();
                })
                .setNeutralButton("Offline Mode", (dialog, which) -> {
                    // Check if fragment is still attached before proceeding
                    if (!isAdded()) {
                        Log.w(TAG, "Fragment not attached, cannot use offline mode");
                        return;
                    }

                    // Proceed with mock success (for demo/testing purposes)
                    Log.d(TAG, "⚠️ Using offline mode - simulating successful registration");
                    Toast.makeText(requireContext(),
                            "Using offline mode (DEMO ONLY)",
                            Toast.LENGTH_LONG).show();

                    // Simulate success state
                    stateManager.transitionTo(FaceRegistrationState.SUCCESS,
                            "Face ID registered in offline mode (DEMO)");
                })
                .setNegativeButton("Cancel", (dialog, which) -> {
                    // Check if fragment is still attached before proceeding
                    if (!isAdded()) {
                        Log.w(TAG, "Fragment not attached, cannot handle cancel");
                        return;
                    }

                    // Go back
                    requireActivity().onBackPressed();
                })
                .setCancelable(false)
                .show();
    }

    /**
     * ❌ Handle error states with retry
     */
    private void handleErrorState(FaceRegistrationState state) {
        // Ensure camera is stopped to prevent infinite loop
        stopCamera();

        // Check if fragment is still attached
        if (!isAdded()) {
            Log.w(TAG, "Fragment not attached, cannot show error dialog");
            return;
        }

        // If there's a specific network error, use specialized handler
        if (state == FaceRegistrationState.FAILED_NETWORK) {
            String errorMessage = state.getDefaultMessage();
            handleNetworkError(errorMessage);
            return;
        }
        
        // Custom message for spoof detection failures
        String message = state.getDefaultMessage();
        if (state == FaceRegistrationState.FAILED_SPOOF) {
            message = "Spoof detection triggered. Please ensure you're using a real face and not a photo or video.\n\nWould you like to try again?";
        } else {
            message = message + "\n\nWould you like to try again?";
        }

        // For other errors, show regular retry dialog
        new AlertDialog.Builder(requireContext())
                .setTitle("Registration Failed")
                .setMessage(message)
                .setPositiveButton("Retry", (dialog, which) -> {
                    // Check if fragment is still attached before proceeding
                    if (!isAdded()) {
                        Log.w(TAG, "Fragment not attached, cannot retry");
                        return;
                    }

                    // Make sure everything is fully reset before retry
                    resetComponents();
                    // Small delay to ensure complete reset
                    new Handler(Looper.getMainLooper()).postDelayed(() -> {
                        // Check again if fragment is attached before starting camera
                        if (!isAdded()) {
                            Log.w(TAG, "Fragment not attached, cannot start registration");
                            return;
                        }
                        startFaceRegistration();
                    }, 500);
                })
                .setNegativeButton("Cancel", (dialog, which) -> {
                    // Check if fragment is still attached before proceeding
                    if (!isAdded()) {
                        Log.w(TAG, "Fragment not attached, cannot handle cancel");
                        return;
                    }
                    requireActivity().onBackPressed();
                })
                .setCancelable(false)
                .show();
    }

    /**
     * Save bitmap to temp file for background sync
     */
    private String saveBitmapToTempFile(Bitmap bitmap) throws IOException {
        // Check if fragment is still attached
        if (!isAdded()) {
            throw new IllegalStateException("Fragment not attached, cannot save bitmap");
        }

        File tempDir = new File(requireContext().getCacheDir(), "face_registration");
        if (!tempDir.exists()) {
            tempDir.mkdirs();
        }

        File tempFile = new File(tempDir, "face_" + System.currentTimeMillis() + ".jpg");

        try (FileOutputStream fos = new FileOutputStream(tempFile)) {
            bitmap.compress(Bitmap.CompressFormat.JPEG, 90, fos);
        }

        return tempFile.getAbsolutePath();
    }

    /**
     * Back to setup screen
     */
    private void backToSetup() {
        // Check if fragment is still attached
        if (!isAdded()) {
            Log.w(TAG, "Fragment not attached, cannot go back to setup");
            return;
        }

        stopCamera();
        resetComponents();
        uiController.showScreen(FaceRegistrationUIController.UIScreenState.SETUP);
    }

    /**
     * Stop camera
     */
    private void stopCamera() {
        try {
            // Make sure camera is fully stopped
            if (cameraView != null) {
                cameraView.stopCamera();
                Log.d(TAG, "Camera stopped");
            }
        } catch (Exception e) {
            Log.e(TAG, "Error stopping camera: " + e.getMessage(), e);
        } finally {
            // Mark as stopped regardless of exceptions
            isCameraStarted = false;
            resetFaceTracker();
        }
    }

    private void resetFaceTracker() {
        if (faceTracker != null) {
            faceTracker.reset();
        }
    }

    /**
     * Reset all components
     */
    private void resetComponents() {
        // Stop camera first
        stopCamera();

        // Reset all managers and state
        if (stateManager != null) {
            stateManager.reset();
        }

        if (spoofDetectionManager != null) {
            spoofDetectionManager.reset();
        }

        resetFaceTracker();
        
        if (faceOverlayView != null) {
            faceOverlayView.clear();
        }

        // Clear data
        currentFrameBitmap = null;
        currentFaceRect = null;

        // Clear any pending handlers/callbacks
        if (mainHandler != null) {
            mainHandler.removeCallbacksAndMessages(null);
        }

        Log.d(TAG, "🔄 All components reset");
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        if (requestCode == CAMERA_PERMISSION_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                startCamera();
            } else {
                stateManager.transitionTo(FaceRegistrationState.FAILED_PERMISSION,
                        "Camera permission is required");
            }
        }
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == SUCCESS_ACTIVITY_REQUEST_CODE) {
            // Success Activity finished, go back
            requireActivity().onBackPressed();
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();

        stopCamera();

        // Cleanup components
        if (stateManager != null) {
            stateManager.cleanup();
        }

        if (uiController != null) {
            uiController.cleanup();
        }

        mainHandler.removeCallbacksAndMessages(null);

        // Clear references
        cameraView = null;
        faceOverlayView = null;
        binding = null;

        Log.d(TAG, "🧹 Fragment cleaned up");
    }
}