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

/**
 * 🔧 REFACTORED Face ID Registration Fragment với Clean Architecture
 * 
 * ✅ FIXES:
 * - State machine với thread-safe transitions
 * - Unified confidence thresholds
 * - Separated UI success flow và background sync
 * - Proper error handling và retry mechanism
 * - Clean separation of concerns
 * 
 * 📋 ARCHITECTURE:
 * - StateManager: Thread-safe state machine
 * - SpoofDetectionManager: Enhanced spoof detection với consistent logic  
 * - UIController: Clean UI state management
 * - Success Activity: Separate success screen với background sync
 */
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
        // 1. State Manager với callback
        stateManager = new FaceRegistrationStateManager();
        stateManager.setStateChangeListener(this::onStateChanged);
        
        // 2. Camera và Overlay
        setupCameraAndOverlay();
        
        // 3. UI Controller
        uiController = new FaceRegistrationUIController(binding, faceOverlayView);
        uiController.showScreen(FaceRegistrationUIController.UIScreenState.SETUP);
        
        // 4. Face Tracker với optimized settings
        faceTracker = new FaceTracker(8); // ~0.27 seconds for stability
        
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
     * 🔄 State change callback từ StateManager
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
                // 🎯 Capture sau delay ngắn
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
        
        uiController.showScreen(FaceRegistrationUIController.UIScreenState.CAMERA);
        initializeFaceIdService();
    }
    
    /**
     * Initialize FaceIdService và related components
     */
    private void initializeFaceIdService() {
        stateManager.transitionTo(FaceRegistrationState.INITIALIZING, "Loading AI models...");
        
        FaceIdServiceManager.getInstance().initialize(requireContext(), new FaceIdServiceManager.InitCallback() {
            @Override
            public void onInitialized(FaceIdService service) {
                faceIdService = service;
                
                // Initialize SpoofDetectionManager với FaceSpoofDetector
                // Lưu ý: cần thêm getFaceSpoofDetector() method vào FaceIdService
                initializeSpoofDetection();
                
                checkCameraPermissionAndStart();
                Log.d(TAG, "✅ FaceIdService initialized");
            }
            
            @Override
            public void onError(String message) {
                Log.e(TAG, "❌ FaceIdService error: " + message);
                stateManager.transitionTo(FaceRegistrationState.FAILED_OTHER, 
                    "Failed to initialize: " + message);
            }
        });
    }
    
    /**
     * Initialize spoof detection với FaceSpoofDetector
     */
    private void initializeSpoofDetection() {
        if (faceIdService != null && faceIdService.getFaceSpoofDetector() != null) {
            spoofDetectionManager = new SpoofDetectionManager(faceIdService.getFaceSpoofDetector());
            Log.d(TAG, "✅ SpoofDetectionManager initialized");
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
     * 📷 Start camera và begin processing
     */
    private void startCamera() {
        if (cameraView != null && !isCameraStarted) {
            cameraView.startCamera(getViewLifecycleOwner(), this::processFrame);
            isCameraStarted = true;
            
            stateManager.transitionTo(FaceRegistrationState.READY, 
                "Position your face in the oval");
            
            Log.d(TAG, "📷 Camera started successfully");
        }
    }
    
    /**
     * 🔍 Process camera frame với enhanced logic
     */
    private void processFrame(Bitmap bitmap) {
        currentFrameBitmap = bitmap;
        
        // Skip nếu chưa ready
        if (faceIdService == null) {
            return;
        }
        
        // Skip nếu đã final state
        if (stateManager.getCurrentState().isFinalState()) {
            return;
        }
        
        // Process frame
        faceIdService.processContinuousFrame(bitmap, new FaceIdService.ContinuousProcessingCallback() {
            @Override
            public void onFaceDetected(Rect boundingBox, boolean isSpoof, float spoofScore) {
                currentFaceRect = boundingBox;
                
                // 🔧 Use enhanced spoof detection nếu available
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
     * Handle enhanced spoof detection result
     */
    private void handleEnhancedSpoofResult(SpoofDetectionManager.SpoofDetectionResult result, Rect boundingBox) {
        if (!isAdded() || stateManager.getCurrentState().isFinalState()) {
            return;
        }
        
        if (result.isSpoof && result.confidenceLevel == SpoofDetectionManager.ConfidenceLevel.HIGH) {
            stateManager.transitionTo(FaceRegistrationState.FACE_SPOOFED, result.explanation);
            resetFaceTracker();
            return;
        }
        
        if (!result.isSpoof) {
            // Update state
            if (stateManager.getCurrentState() != FaceRegistrationState.FACE_REAL &&
                stateManager.getCurrentState() != FaceRegistrationState.FACE_STABILIZING) {
                stateManager.transitionTo(FaceRegistrationState.FACE_REAL, "Real face detected");
            }
            
            // Process based on should proceed flag
            if (result.shouldProceed) {
                stateManager.transitionTo(FaceRegistrationState.FACE_STABLE, "Ready to capture!");
            } else {
                trackFaceStability(boundingBox);
            }
        }
    }
    
    /**
     * Fallback basic spoof handling
     */
    private void handleBasicSpoofResult(boolean isSpoof, float spoofScore, Rect boundingBox) {
        Log.d(TAG, "🔧 Using basic spoof detection: isSpoof=" + isSpoof + ", score=" + spoofScore);
        
        if (isSpoof && spoofScore > 0.7f) {
            stateManager.transitionTo(FaceRegistrationState.FACE_SPOOFED, 
                "Spoof detected! Please use a real face.");
            resetFaceTracker();
        } else if (!isSpoof && spoofScore > 0.6f) {
            stateManager.transitionTo(FaceRegistrationState.FACE_REAL, "Real face detected");
            trackFaceStability(boundingBox);
        }
    }
    
    /**
     * Track face stability
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
                }
            });
        }
    }
    
    /**
     * 📸 Capture và register face
     */
    private void captureAndRegisterFace() {
        if (currentFrameBitmap == null || currentFaceRect == null) {
            Log.w(TAG, "⚠️ Cannot capture - no frame or face rect");
            stateManager.transitionTo(FaceRegistrationState.FAILED_OTHER, 
                "Capture failed - no data available");
            return;
        }
        
        stateManager.transitionTo(FaceRegistrationState.PROCESSING, "Registering your face...");
        
        String userId = AuthManager.getInstance(requireContext()).getCurrentUserId();
        if (userId == null || userId.isEmpty()) {
            Log.e(TAG, "❌ No user ID available");
            stateManager.transitionTo(FaceRegistrationState.FAILED_OTHER, "User not logged in");
            return;
        }
        
        // 🎯 Register face với proper error handling
        faceIdService.captureAndRegisterFace(currentFrameBitmap, currentFaceRect, userId, 
            new FaceIdService.FaceIdCallback() {
                @Override
                public void onSuccess(String message) {
                    if (!isAdded()) return;
                    
                    Log.d(TAG, "✅ Registration successful: " + message);
                    stateManager.transitionTo(FaceRegistrationState.SUCCESS, message);
                }
                
                @Override
                public void onFailure(String errorMessage) {
                    if (!isAdded()) return;
                    
                    Log.e(TAG, "❌ Registration failed: " + errorMessage);
                    stateManager.transitionTo(FaceRegistrationState.FAILED_NETWORK, 
                        "Registration failed: " + errorMessage);
                }
            });
    }
    
    /**
     * 🎉 Handle success - Navigate to Success Activity
     */
    private void handleSuccessState() {
        try {
            stopCamera();
            
            // Save bitmap for background sync
            String bitmapPath = saveBitmapToTempFile(currentFrameBitmap);
            String userId = AuthManager.getInstance(requireContext()).getCurrentUserId();
            String successMessage = "Face ID has been registered successfully!";
            
            // 🚀 Launch Success Activity
            Intent successIntent = FaceIdSuccessActivity.createIntent(
                requireContext(), userId, successMessage, bitmapPath);
            startActivityForResult(successIntent, SUCCESS_ACTIVITY_REQUEST_CODE);
            
            Log.d(TAG, "🎉 Navigating to Success Activity");
            
        } catch (Exception e) {
            Log.e(TAG, "❌ Error handling success", e);
            Toast.makeText(requireContext(), "Registration completed!", Toast.LENGTH_LONG).show();
            requireActivity().onBackPressed();
        }
    }
    
    /**
     * ❌ Handle error states với retry
     */
    private void handleErrorState(FaceRegistrationState state) {
        new AlertDialog.Builder(requireContext())
            .setTitle("Registration Failed")
            .setMessage(state.getDefaultMessage() + "\n\nWould you like to try again?")
            .setPositiveButton("Retry", (dialog, which) -> {
                resetComponents();
                startFaceRegistration();
            })
            .setNegativeButton("Cancel", (dialog, which) -> {
                requireActivity().onBackPressed();
            })
            .setCancelable(false)
            .show();
    }
    
    /**
     * Save bitmap to temp file for background sync
     */
    private String saveBitmapToTempFile(Bitmap bitmap) throws IOException {
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
        stopCamera();
        resetComponents();
        uiController.showScreen(FaceRegistrationUIController.UIScreenState.SETUP);
    }
    
    /**
     * Stop camera
     */
    private void stopCamera() {
        if (cameraView != null && isCameraStarted) {
            cameraView.stopCamera();
            isCameraStarted = false;
        }
        resetFaceTracker();
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
        if (stateManager != null) {
            stateManager.reset();
        }
        
        if (spoofDetectionManager != null) {
            spoofDetectionManager.reset();
        }
        
        resetFaceTracker();
        
        currentFrameBitmap = null;
        currentFaceRect = null;
        
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