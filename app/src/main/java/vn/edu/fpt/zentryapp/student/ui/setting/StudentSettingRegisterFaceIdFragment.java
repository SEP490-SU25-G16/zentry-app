package vn.edu.fpt.zentryapp.student.ui.setting;

import android.Manifest;
import android.animation.ValueAnimator;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.Rect;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.method.ScrollingMovementMethod;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.LinearInterpolator;
import android.widget.ProgressBar;
import android.widget.TextView;
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
import java.util.Locale;

import vn.edu.fpt.zentryapp.R;
import vn.edu.fpt.zentryapp.auth.client.AuthManager;
import vn.edu.fpt.zentryapp.databinding.FragmentStudentSettingRegisterFaceIdBinding;
import vn.edu.fpt.zentryapp.student.data.service.FaceIdConfig;
import vn.edu.fpt.zentryapp.student.data.service.FaceIdEnhancer;
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

public class StudentSettingRegisterFaceIdFragment extends Fragment 
        implements FaceIdEnhancer.FaceIdEnhancerCallback {
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
    private FaceIdEnhancer faceIdEnhancer; // Add FaceIdEnhancer
    private boolean faceIdEnhancerInitialized = false;
    
    
    // 🔍 ERROR TRACKING
    private String lastDetailedErrorMessage = ""; // Store detailed error information
    private boolean hasDetailedError = false;

    // 📷 CAMERA COMPONENTS
    private CameraView cameraView;
    private OvalFaceOverlayView faceOverlayView;
    private boolean isCameraStarted = false;

    // 💾 CURRENT DATA
    private Bitmap currentFrameBitmap;
    private Rect currentFaceRect;

    // 5-Second Analysis
    private final java.util.List<Float> frameScores = new java.util.ArrayList<>();
    private boolean isAnalyzing = false;
    private static final int ANALYSIS_DURATION_MS = 5000;
    private static final float MIN_AVERAGE_SCORE_FOR_REGISTRATION = 0.75f;

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

        // Đảm bảo biến analysisOverlay ban đầu là null để thiết lập UI phân tích khi cần
        analysisOverlay = null;
        
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
        
        // Cập nhật UI overlay theo màu sắc dựa vào trạng thái
        updateOverlayColor(state);
    }
    
    /**
     * Cập nhật màu sắc overlay dựa vào trạng thái
     */
    private void updateOverlayColor(FaceRegistrationState state) {
        if (faceOverlayView == null || !isAdded()) return;
        
        int color;
        switch (state) {
            case FACE_REAL:
            case FACE_STABLE:
                color = ContextCompat.getColor(requireContext(), R.color.success_green);
                break;
                
            case FACE_DETECTED:
            case FACE_STABILIZING:
            case FACE_WARNING:
                color = ContextCompat.getColor(requireContext(), R.color.warning_yellow);
                break;
                
            case FACE_SPOOFED:
            case FACE_OUT_OF_BOUNDS:
            case FAILED_SPOOF:
            case MULTIPLE_FACES:
                color = ContextCompat.getColor(requireContext(), R.color.error_red);
                break;
                
            case NO_FACE:
            case READY:
                color = ContextCompat.getColor(requireContext(), R.color.white);
                break;
                
            case LIVENESS_CHALLENGE:
                color = ContextCompat.getColor(requireContext(), R.color.primary);
                break;
                
            case ANALYZING:
            case PROCESSING:
                color = ContextCompat.getColor(requireContext(), R.color.processing_blue);
                break;
                
            default:
                color = ContextCompat.getColor(requireContext(), R.color.white);
                break;
        }
        
        faceOverlayView.setOvalColor(color);
    }

    /**
     * Handle actions for specific states
     */
    private void handleStateActions(FaceRegistrationState state) {
        // Kiểm tra xem fragment có còn hoạt động không
        if (!isAdded() || getActivity() == null) {
            Log.w(TAG, "⚠️ Fragment not valid for state action: " + state);
            return;
        }

        // Ghi log cho trạng thái
        Log.d(TAG, "Xử lý trạng thái: " + state);
        
        // Cập nhật tvStatusMessage (Thêm vào để luôn cập nhật thông báo trạng thái)
        if (binding != null && binding.tvStatusMessage != null) {
            String message = state.getDefaultMessage();
            binding.tvStatusMessage.setText(message);
        }
        
        // Cập nhật UI loading nếu đang trong trạng thái xử lý
        if (state.isProcessingState()) {
            if (uiController != null) {
                uiController.showLoadingIndicator(true);
            }
        } else {
            if (uiController != null) {
                uiController.showLoadingIndicator(false);
            }
        }

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
                if (!isAnalyzing) {
                    // Hiển thị UI thông báo
                    Log.d(TAG, "Khuôn mặt đã ổn định, bắt đầu phân tích...");
                    stateManager.transitionTo(FaceRegistrationState.ANALYZING, 
                        "Đang phân tích khuôn mặt...");
                    
                    // Cập nhật UI để người dùng biết đang phân tích
                    if (binding != null && binding.tvStatusMessage != null) {
                        binding.tvStatusMessage.setText("Đang phân tích khuôn mặt...");
                    }
                    
                    // Bắt đầu phân tích
                    startAnalysis();
                }
                break;

            case FACE_DETECTED:
                // Cập nhật UI khi phát hiện khuôn mặt
                if (faceOverlayView != null) {
                    faceOverlayView.setOvalColor(ContextCompat.getColor(requireContext(), R.color.warning_yellow));
                }
                break;

            case NO_FACE:
                // Cập nhật UI khi không phát hiện khuôn mặt
                if (faceOverlayView != null) {
                    faceOverlayView.setOvalColor(ContextCompat.getColor(requireContext(), R.color.white));
                }
                break;

            case ANALYZING:
                // Đảm bảo UI phân tích được hiển thị
                if (analysisOverlay != null && analysisOverlay.getVisibility() != View.VISIBLE) {
                    analysisOverlay.setVisibility(View.VISIBLE);
                }
                break;

            case LIVENESS_CHALLENGE:
                // Hiển thị UI cho liveness challenge
                Log.d(TAG, "🔄 Kích hoạt Liveness Challenge");
                if (binding != null && binding.tvStatusMessage != null) {
                    binding.tvStatusMessage.setText("Hãy nhìn vào camera và nhấp mắt");
                }
                if (faceOverlayView != null) {
                    faceOverlayView.setOvalColor(ContextCompat.getColor(requireContext(), R.color.primary));
                }
                
                // Initialize FaceIdEnhancer if not already done
                initializeFaceIdEnhancer();
                break;
                
            case FACE_OUT_OF_BOUNDS:
                // Cập nhật UI khi khuôn mặt nằm ngoài khung hình
                if (faceOverlayView != null) {
                    faceOverlayView.setOvalColor(ContextCompat.getColor(requireContext(), R.color.error_red));
                }
                break;
                
            case READY:
                // Đảm bảo UI được đặt lại ở trạng thái sẵn sàng
                if (faceOverlayView != null) {
                    faceOverlayView.setOvalColor(ContextCompat.getColor(requireContext(), R.color.white));
                }
                break;
                
            case INITIALIZING:
                // Hiển thị UI loading khi đang khởi tạo
                if (uiController != null) {
                    uiController.showLoadingOverlay(true);
                }
                break;
                
            case FACE_REAL:
            case FACE_STABILIZING:
                // Cập nhật UI cho trạng thái ổn định khuôn mặt
                if (faceOverlayView != null) {
                    faceOverlayView.setOvalColor(ContextCompat.getColor(requireContext(), R.color.warning_yellow));
                }
                break;
                
            case FACE_SPOOFED:
                // Cập nhật UI khi phát hiện giả mạo
                if (faceOverlayView != null) {
                    faceOverlayView.setOvalColor(ContextCompat.getColor(requireContext(), R.color.error_red));
                }
                break;
                
            case PROCESSING:
                // Hiển thị UI đang xử lý
                if (uiController != null) {
                    uiController.showLoadingIndicator(true);
                }
                // Đảm bảo ẩn overlay phân tích
                if (analysisOverlay != null) {
                    analysisOverlay.setVisibility(View.GONE);
                }
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
            
            // Đánh dấu đã khởi tạo thành công
            faceIdServiceInitialized = true;
            
            initializeSpoofDetection();
            
            // Đảm bảo chuyển sang trạng thái READY trước khi khởi động camera
            stateManager.transitionTo(FaceRegistrationState.READY, "Position your face in the oval");
            
            checkCameraPermissionAndStart();
            return;
        }

        FaceIdServiceManager.getInstance().initialize(requireContext(), new FaceIdServiceManager.InitCallback() {
            @Override
            public void onInitialized(FaceIdService service) {
                if (!isAdded()) return;

                faceIdService = service;
                faceIdServiceInitialized = true;

                // 🔧 NEW: Set registration scenario for more lenient validation
                faceIdService.setScenario(FaceIdConfig.Scenario.REGISTRATION);

                // Initialize SpoofDetectionManager with FaceSpoofDetector
                initializeSpoofDetection();
                
                // Đảm bảo chuyển sang trạng thái READY trước khi khởi động camera
                stateManager.transitionTo(FaceRegistrationState.READY, "Position your face in the oval");

                checkCameraPermissionAndStart();
                Log.d(TAG, "✅ FaceIdService initialized with REGISTRATION scenario");
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
            spoofDetectionManager = new SpoofDetectionManager(faceIdService.getFaceSpoofDetector(), requireContext());
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

        // Kiểm tra xem FaceIdService đã khởi tạo chưa
        if (faceIdService == null || !faceIdServiceInitialized) {
            Log.w(TAG, "FaceIdService not initialized yet, skipping frame processing");
            return;
        }

        // Special handling for LIVENESS_CHALLENGE state
        if (stateManager.getCurrentState() == FaceRegistrationState.LIVENESS_CHALLENGE) {
            // Process frame using FaceIdService first to get face rect
            faceIdService.processContinuousFrame(bitmap, faceOverlayView.getOvalRect(),
                    new FaceIdService.ContinuousProcessingCallback() {
                @Override
                public void onFaceDetected(Rect boundingBox, boolean isSpoof, float spoofScore) {
                    currentFaceRect = boundingBox;
                    
                    // Update face position in overlay
                    if (faceOverlayView != null) {
                        boolean isGoodPosition = faceOverlayView.updateFacePosition(boundingBox);
                        if (!isGoodPosition) {
                            // If position is bad, don't process for liveness
                            if (stateManager.getCurrentState() != FaceRegistrationState.FACE_OUT_OF_BOUNDS) {
                                stateManager.transitionTo(FaceRegistrationState.FACE_OUT_OF_BOUNDS,
                                        "Position your face properly in the oval");
                            }
                            return;
                        }
                    }
                    
                    // Process the frame for liveness challenges
                    processFrameForLivenessChallenge(bitmap, boundingBox);
                }

                @Override
                public void onNoFaceDetected() {
                    currentFaceRect = null;
                    stateManager.transitionTo(FaceRegistrationState.NO_FACE, "Look at the camera");
                }

                @Override
                public void onMultipleFacesDetected() {
                    // Handle multiple faces
                    stateManager.transitionTo(FaceRegistrationState.MULTIPLE_FACES, "Only one person should be visible");
                }

                @Override
                public void onError(String errorMessage) {
                    Log.e(TAG, "Error processing frame: " + errorMessage);
                }
            });
            
            return; // Skip normal processing
        }

        if (isAnalyzing) {
            faceIdService.processContinuousFrame(bitmap, faceOverlayView.getOvalRect(), new FaceIdService.ContinuousProcessingCallback() {
                @Override
                public void onFaceDetected(Rect boundingBox, boolean isSpoof, float spoofScore) {
                    if (!isSpoof) {
                        frameScores.add(spoofScore);
                    }
                }

                @Override
                public void onNoFaceDetected() {}

                @Override
                public void onMultipleFacesDetected() {}

                @Override
                public void onError(String errorMessage) {}
            });
            return;
        }

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
                        // Cập nhật UI ngay lập tức
                        if (faceOverlayView != null) {
                            faceOverlayView.setOvalColor(ContextCompat.getColor(requireContext(), R.color.error_red));
                        }
                        return;
                    } else if (isGoodPosition && stateManager.getCurrentState() == FaceRegistrationState.FACE_OUT_OF_BOUNDS) {
                        // Khi vị trí đã tốt nhưng trạng thái vẫn là out of bounds, cập nhật trạng thái
                        stateManager.transitionTo(FaceRegistrationState.FACE_DETECTED, "Face detected");
                        // Cập nhật UI ngay lập tức
                        if (faceOverlayView != null) {
                            faceOverlayView.setOvalColor(ContextCompat.getColor(requireContext(), R.color.warning_yellow));
                        }
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
                
                // Cập nhật UI ngay lập tức
                if (faceOverlayView != null) {
                    faceOverlayView.setOvalColor(ContextCompat.getColor(requireContext(), R.color.white));
                }
                
                // Cập nhật thông báo trạng thái
                if (binding != null && binding.tvStatusMessage != null) {
                    binding.tvStatusMessage.setText("Look at the camera");
                }
                
                resetFaceTracker();
            }

            @Override
            public void onMultipleFacesDetected() {
                currentFaceRect = null;
                stateManager.transitionTo(FaceRegistrationState.MULTIPLE_FACES,
                        "Only one face should be visible");
                
                // Cập nhật UI ngay lập tức
                if (faceOverlayView != null) {
                    faceOverlayView.setOvalColor(ContextCompat.getColor(requireContext(), R.color.error_red));
                }
                
                // Cập nhật thông báo trạng thái
                if (binding != null && binding.tvStatusMessage != null) {
                    binding.tvStatusMessage.setText("Only one face should be visible");
                }
                
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

        if (result.triggerLivenessChallenge) {
            stateManager.transitionTo(FaceRegistrationState.LIVENESS_CHALLENGE, result.explanation);
            return;
        }

        if (result.isSpoof) {
            stateManager.transitionTo(FaceRegistrationState.FACE_SPOOFED, result.explanation);
            resetFaceTracker();
            return;
        }

        if (result.shouldProceed) {
            stateManager.transitionTo(FaceRegistrationState.FACE_STABLE, result.explanation);
        } else {
            // Use a FACE_SUSPICIOUS state to provide feedback without failing
            if (result.explanation.contains("Suspicious")) {
                stateManager.transitionTo(FaceRegistrationState.FACE_SUSPICIOUS, result.explanation);
            } else {
                stateManager.transitionTo(FaceRegistrationState.FACE_STABILIZING, result.explanation);
            }
            trackFaceStability(boundingBox);
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
                        // Cập nhật màu sắc oval để phản hồi trực quan
                        faceOverlayView.setOvalColor(ContextCompat.getColor(requireContext(), R.color.warning_yellow));
                        faceOverlayView.startProgressAnimation(3000); // 3 second animation
                    }
                    
                    // Cập nhật thông báo trạng thái
                    if (binding != null && binding.tvStatusMessage != null) {
                        binding.tvStatusMessage.setText("Hold still... " + percentage + "%");
                    }
                }

                @Override
                public void onFaceStable(Rect stableFaceRect) {
                    if (!isAdded()) return;

                    currentFaceRect = stableFaceRect;
                    stateManager.transitionTo(FaceRegistrationState.FACE_STABLE, "Perfect!");
                    
                    // Cập nhật màu oval khi khuôn mặt ổn định
                    if (faceOverlayView != null) {
                        faceOverlayView.setOvalColor(ContextCompat.getColor(requireContext(), R.color.success_green));
                    }
                    
                    // Cập nhật thông báo trạng thái
                    if (binding != null && binding.tvStatusMessage != null) {
                        binding.tvStatusMessage.setText("Perfect! Processing...");
                    }
                }

                @Override
                public void onFaceUnstable() {
                    if (!isAdded()) return;

                    stateManager.transitionTo(FaceRegistrationState.FACE_REAL,
                            "Keep your face steady");
                    
                    // Stop progress animation
                    if (faceOverlayView != null) {
                        // Đặt lại màu oval
                        faceOverlayView.setOvalColor(ContextCompat.getColor(requireContext(), R.color.warning_yellow));
                        faceOverlayView.stopProgressAnimation();
                    }
                    
                    // Cập nhật thông báo trạng thái
                    if (binding != null && binding.tvStatusMessage != null) {
                        binding.tvStatusMessage.setText("Keep your face steady");
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

        stateManager.transitionTo(FaceRegistrationState.PROCESSING, "Processing face data...");

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

        // 🔧 NEW: Show progress updates
        stateManager.transitionTo(FaceRegistrationState.PROCESSING, "Generating face embedding...");
        
        // Kiểm tra và ẩn overlay phân tích nếu đang hiển thị
        if (analysisOverlay != null && analysisOverlay.getVisibility() == View.VISIBLE) {
            analysisOverlay.setVisibility(View.GONE);
        }
        
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
                        
                        // Store detailed error information for UI display
                        lastDetailedErrorMessage = "Registration failure details:\n" + errorMessage;
                        hasDetailedError = true;
                        
                        // 🔧 NEW: Enhanced error categorization
                        if (errorMessage.contains("timeout") || errorMessage.contains("Timeout")) {
                            lastDetailedErrorMessage += "\n\nError type: Network Timeout";
                            handleNetworkError("Request timeout. Please try again.");
                        } else if (errorMessage.contains("Network error") || errorMessage.contains("Cannot connect")) {
                            lastDetailedErrorMessage += "\n\nError type: Network Connectivity";
                            handleNetworkError(errorMessage);
                        } else if (errorMessage.contains("Authentication failed")) {
                            lastDetailedErrorMessage += "\n\nError type: Authentication";
                            stateManager.transitionTo(FaceRegistrationState.FAILED_OTHER,
                                    "Authentication failed. Please login again.");
                        } else if (errorMessage.contains("Server error")) {
                            lastDetailedErrorMessage += "\n\nError type: Server";
                            stateManager.transitionTo(FaceRegistrationState.FAILED_OTHER,
                                    "Server error. Please try again later.");
                        } else if (errorMessage.contains("spoof") || errorMessage.contains("Spoof")) {
                            lastDetailedErrorMessage += "\n\nError type: Spoof Detection";
                            stateManager.transitionTo(FaceRegistrationState.FAILED_SPOOF,
                                    "Registration failed: " + errorMessage);
                        } else {
                            lastDetailedErrorMessage += "\n\nError type: Other/Unknown";
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
     * Handle network errors with retry option
     */
    private void handleNetworkError(String errorMessage) {
        if (!isAdded()) return;

        // Create alert dialog with retry option
        new AlertDialog.Builder(requireContext())
                .setTitle("Network Connection Issue")
                .setMessage("Cannot connect to the server. Please check your internet connection and try again.")
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

        // Handle all errors in a unified way - no longer using separate handler for network errors
        
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

        // For other errors, show regular retry dialog with detailed information
        AlertDialog.Builder builder = new AlertDialog.Builder(requireContext())
                .setTitle(title)
                .setMessage(detailedMessage)
                .setPositiveButton("Retry", (dialog, which) -> {
                    // Check if fragment is still attached before proceeding
                    if (!isAdded()) {
                        Log.w(TAG, "Fragment not attached, cannot retry");
                        return;
                    }

                    // Reset error tracking
                    hasDetailedError = false;
                    lastDetailedErrorMessage = "";
                    

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
                // No neutral button - removed offline mode and copy error options
                .setNegativeButton("Cancel", (dialog, which) -> {
                    // Check if fragment is still attached before proceeding
                    if (!isAdded()) {
                        Log.w(TAG, "Fragment not attached, cannot handle cancel");
                        return;
                    }

                    // Reset error tracking
                    hasDetailedError = false;
                    lastDetailedErrorMessage = "";
                    requireActivity().onBackPressed();
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
     * Initialize the FaceIdEnhancer for liveness challenges
     */
    private void initializeFaceIdEnhancer() {
        if (faceIdEnhancerInitialized) {
            // Already initialized, just reset it
            if (faceIdEnhancer != null) {
                faceIdEnhancer.reset();
            }
            return;
        }
        
        if (getContext() == null) {
            Log.e(TAG, "Cannot initialize FaceIdEnhancer: Context is null");
            return;
        }
        
        try {
            // Initialize the FaceIdEnhancer
            faceIdEnhancer = new FaceIdEnhancer(getContext(), this);
            faceIdEnhancer.setChallengeType(FaceIdEnhancer.ChallengeType.BLINK_AND_GAZE);
            faceIdEnhancerInitialized = true;
            Log.d(TAG, "FaceIdEnhancer initialized successfully");
        } catch (Exception e) {
            Log.e(TAG, "Error initializing FaceIdEnhancer", e);
        }
    }
    
    /**
     * Update the frame processing to use FaceIdEnhancer when in LIVENESS_CHALLENGE state
     */
    private void processFrameForLivenessChallenge(Bitmap bitmap, Rect faceRect) {
        if (faceIdEnhancer != null && faceIdEnhancerInitialized) {
            faceIdEnhancer.processFaceFrame(bitmap, faceRect);
        } else {
            Log.w(TAG, "Attempted to process liveness frame but FaceIdEnhancer not initialized");
        }
    }
    
    //------------------------------------------------------------------------------
    // FaceIdEnhancer.FaceIdEnhancerCallback Implementation
    //------------------------------------------------------------------------------
    
    @Override
    public void onStateChanged(FaceIdEnhancer.AuthState newState) {
        if (!isAdded()) return;
        
        Log.d(TAG, "FaceIdEnhancer state changed: " + newState);
        
        // Show liveness progress indicators when face is detected
        if (newState == FaceIdEnhancer.AuthState.FACE_DETECTED || 
            newState == FaceIdEnhancer.AuthState.ANALYZING) {
            showLivenessProgressIndicators();
        }
        
        // Update UI based on FaceIdEnhancer state
        if (newState == FaceIdEnhancer.AuthState.BLINK_VERIFIED) {
            // User blinked successfully
            if (binding != null) {
                // Update status message
                binding.tvStatusMessage.setText("Blink detected!");
                binding.tvInstructionMessage.setText("Now look at different directions");
                
                // Update progress indicators
                binding.ivBlinkIndicator.setColorFilter(
                    ContextCompat.getColor(requireContext(), R.color.success_green), 
                    android.graphics.PorterDuff.Mode.SRC_IN);
            }
        } else if (newState == FaceIdEnhancer.AuthState.GAZE_VERIFIED) {
            // User completed gaze challenge
            if (binding != null) {
                // Update status message
                binding.tvStatusMessage.setText("Gaze verified!");
                binding.tvInstructionMessage.setText("Completing verification...");
                
                // Update progress indicators
                binding.ivGazeIndicator.setColorFilter(
                    ContextCompat.getColor(requireContext(), R.color.success_green), 
                    android.graphics.PorterDuff.Mode.SRC_IN);
            }
        } else if (newState == FaceIdEnhancer.AuthState.VERIFIED) {
            // All liveness challenges completed
            Log.d(TAG, "Liveness verification complete!");
            // Transition to the next state in registration
            stateManager.transitionTo(FaceRegistrationState.FACE_REAL, "Liveness verified!");
        }
    }
    
    /**
     * Show liveness challenge progress indicators
     */
    private void showLivenessProgressIndicators() {
        if (binding != null && binding.llLivenessProgress != null && 
            binding.llLivenessProgress.getVisibility() != View.VISIBLE) {
            
            // Show progress indicators
            binding.llLivenessProgress.setVisibility(View.VISIBLE);
            // Ensure the liveness progress overlay is above camera and face overlay
            binding.llLivenessProgress.bringToFront();
            binding.llLivenessProgress.requestLayout();
            binding.llLivenessProgress.invalidate();
            
            // Update instruction text
            binding.tvStatusMessage.setText("Liveness Challenge");
            binding.tvInstructionMessage.setText("Please blink your eyes");
        }
    }
    
    @Override
    public void onBlinkDetected() {
        if (!isAdded()) return;
        
        Log.d(TAG, "👁️ Blink detected!");
        // Update UI to show blink was detected with visual feedback
        if (binding != null) {
            // Update status message with clear instructions
            binding.tvStatusMessage.setText("Blink detected! ✓");
            binding.tvInstructionMessage.setText("Now look left, right, and up");
            
            // Update progress indicator
            binding.ivBlinkIndicator.setColorFilter(
                ContextCompat.getColor(requireContext(), R.color.success_green), 
                android.graphics.PorterDuff.Mode.SRC_IN);
            
            // Add animation for visual feedback
            binding.ivBlinkIndicator.animate()
                .scaleX(1.2f).scaleY(1.2f)
                .setDuration(200)
                .withEndAction(() -> {
                    binding.ivBlinkIndicator.animate()
                        .scaleX(1.0f).scaleY(1.0f)
                        .setDuration(200);
                });
        }
    }
    
    @Override
    public void onGazeDirectionChanged(float x, float y) {
        if (!isAdded()) return;
        
        // Add visual indicator for gaze direction
        Log.d(TAG, "👀 Gaze direction: x=" + x + ", y=" + y);
        
        if (binding != null) {
            // Update instruction based on gaze direction
            String direction = "";
            if (x < -0.3f) {
                direction = "Looking left ✓";
                binding.tvInstructionMessage.setText("Now look right and up");
            } else if (x > 0.3f) {
                direction = "Looking right ✓";
                binding.tvInstructionMessage.setText("Now look up");
            } else if (y < -0.3f) {
                direction = "Looking up ✓";
                binding.tvInstructionMessage.setText("Great! Keep following directions");
            } else if (y > 0.3f) {
                direction = "Looking down ✓";
            }
            
            // Only update if we detected a specific direction
            if (!direction.isEmpty()) {
                binding.tvStatusMessage.setText(direction);
                
                // Add subtle animation to gaze indicator
                binding.ivGazeIndicator.animate()
                    .alpha(0.7f)
                    .setDuration(100)
                    .withEndAction(() -> {
                        binding.ivGazeIndicator.animate()
                            .alpha(1.0f)
                            .setDuration(100);
                    });
            }
        }
    }
    
    @Override
    public void onLivenessVerified(boolean isLive) {
        if (!isAdded()) return;
        
        Log.d(TAG, "🔐 Liveness verification result: " + (isLive ? "LIVE" : "NOT LIVE"));
        if (isLive) {
            // Proceed with face registration
            stateManager.transitionTo(FaceRegistrationState.FACE_REAL, "Liveness verified!");
        }
    }
    
    @Override
    public void onVerificationComplete(boolean success) {
        if (!isAdded()) return;
        
        Log.d(TAG, "✅ Verification complete: " + (success ? "SUCCESS" : "FAILED"));
        if (success) {
            // Proceed with face registration
            stateManager.transitionTo(FaceRegistrationState.FACE_REAL, "Verification complete!");
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
            // Đặt lại màu của oval để biểu thị trạng thái mới
            faceOverlayView.setOvalColor(ContextCompat.getColor(requireContext(), R.color.white));
        }

        // Clear data
        currentFrameBitmap = null;
        currentFaceRect = null;
        
        // Đặt lại biến phân tích
        isAnalyzing = false;
        frameScores.clear();
        
        // Ẩn overlay phân tích nếu đang hiển thị
        if (analysisOverlay != null) {
            analysisOverlay.setVisibility(View.GONE);
        }

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
        
        if (faceIdEnhancer != null) {
            faceIdEnhancer.close();
            faceIdEnhancer = null;
            faceIdEnhancerInitialized = false;
        }
        
        // Close FaceIdService to properly release MediaPipeFaceLandmarkExtractor
        if (faceIdService != null) {
            faceIdService.close();
            faceIdService = null;
        }

        mainHandler.removeCallbacksAndMessages(null);

        // Clear references
        cameraView = null;
        faceOverlayView = null;
        binding = null;

        Log.d(TAG, "🧹 Fragment cleaned up");
    }
    // Thêm các biến UI cần thiết
    private ProgressBar analysisProgressBar;
    private TextView analysisCountdownText;
    private View analysisOverlay;
    
    // Thêm biến theo dõi xem faceIdService đã khởi tạo thành công chưa
    private boolean faceIdServiceInitialized = false;
    
    /**
     * Start a 5-second analysis of face quality before proceeding with registration
     * Collects frame scores to ensure consistent high-quality face detection
     */
    private void startAnalysis() {
        // Kiểm tra nếu đã đang phân tích
        if (isAnalyzing) {
            Log.d(TAG, "Đã đang phân tích, bỏ qua yêu cầu mới");
            return;
        }
        
        isAnalyzing = true;
        frameScores.clear();
        
        // Kiểm tra fragment tồn tại
        if (!isAdded() || binding == null) return;
        
        // Khởi tạo và hiển thị UI phân tích nếu chưa tồn tại
        setupAnalysisUI();
        
        // Hiện overlay phân tích
        if (analysisOverlay != null) {
            analysisOverlay.setVisibility(View.VISIBLE);
        }
        
        // Start with initial analyzing state message
        stateManager.transitionTo(FaceRegistrationState.ANALYZING, "Đang phân tích... Giữ nguyên");
        
        // Hiển thị và cập nhật progressBar
        if (analysisProgressBar != null) {
            analysisProgressBar.setVisibility(View.VISIBLE);
            analysisProgressBar.setMax(ANALYSIS_DURATION_MS);
            analysisProgressBar.setProgress(0);
            
            // Animator để cập nhật progress một cách mượt mà
            final ValueAnimator progressAnimator = ValueAnimator.ofInt(0, ANALYSIS_DURATION_MS);
            progressAnimator.setDuration(ANALYSIS_DURATION_MS);
            progressAnimator.setInterpolator(new LinearInterpolator());
            progressAnimator.addUpdateListener(animation -> {
                if (analysisProgressBar != null && isAdded()) {
                    analysisProgressBar.setProgress((Integer) animation.getAnimatedValue());
                }
            });
            progressAnimator.start();
        }
        
        // Start countdown feedback
        final int[] secondsLeft = {ANALYSIS_DURATION_MS / 1000};
        final int countdownInterval = 1000; // 1 second
        
        // Countdown handler to update UI every second
        final Handler countdownHandler = new Handler(Looper.getMainLooper());
        final Runnable countdownRunnable = new Runnable() {
            @Override
            public void run() {
                if (!isAdded() || !isAnalyzing) return;
                
                secondsLeft[0]--;
                if (secondsLeft[0] > 0) {
                    // Update countdown message and UI
                    String message = "Đang phân tích... " + secondsLeft[0] + "s";
                    stateManager.transitionTo(FaceRegistrationState.ANALYZING, message);
                    
                    // Cập nhật text đếm ngược
                    if (analysisCountdownText != null) {
                        analysisCountdownText.setText(message);
                    }
                    
                    countdownHandler.postDelayed(this, countdownInterval);
                }
            }
        };
        
        // Start countdown updates
        countdownHandler.postDelayed(countdownRunnable, countdownInterval);
        
        // Schedule analysis completion
        mainHandler.postDelayed(() -> {
            // Stop analyzing
            isAnalyzing = false;
            countdownHandler.removeCallbacks(countdownRunnable);
            
            // Ẩn overlay phân tích
            if (analysisOverlay != null && isAdded()) {
                analysisOverlay.setVisibility(View.GONE);
            }
            
            // Check if fragment is still valid
            if (!isAdded()) {
                Log.w(TAG, "Fragment not attached during analysis completion");
                return;
            }
            
            // Check if we collected enough data
            if (frameScores.isEmpty()) {
                stateManager.transitionTo(FaceRegistrationState.FAILED_OTHER, 
                        "Không thể lấy được dữ liệu ổn định. Vui lòng thử lại.");
                return;
            }
            
            if (frameScores.size() < 10) {
                stateManager.transitionTo(FaceRegistrationState.FAILED_OTHER, 
                        "Không đủ dữ liệu chất lượng. Cần cải thiện ánh sáng và giữ vị trí ổn định.");
                return;
            }
            
            // Calculate statistics
            float sum = 0;
            float min = Float.MAX_VALUE;
            float max = Float.MIN_VALUE;
            
            for (float score : frameScores) {
                sum += score;
                min = Math.min(min, score);
                max = Math.max(max, score);
            }
            
            float averageScore = sum / frameScores.size();
            float variance = calculateVariance(frameScores, averageScore);
            
            Log.d(TAG, "Analysis complete: " + frameScores.size() + " frames analyzed");
            Log.d(TAG, "Scores - Avg: " + averageScore + ", Min: " + min + ", Max: " + max + ", Variance: " + variance);
            
            // Quality assessment
            boolean isConsistent = variance < 0.03; // Low variance indicates consistent detection
            boolean isHighQuality = averageScore >= MIN_AVERAGE_SCORE_FOR_REGISTRATION;
            boolean isAcceptableQuality = averageScore >= (MIN_AVERAGE_SCORE_FOR_REGISTRATION - 0.1f);
            
            // Log detailed quality information
            String qualityLog = String.format(Locale.US, 
                "Face Analysis Results - Frames: %d, Average Score: %.3f, Min: %.3f, Max: %.3f, Variance: %.5f, " +
                "isConsistent: %b, isHighQuality: %b, isAcceptableQuality: %b",
                frameScores.size(), averageScore, min, max, variance,
                isConsistent, isHighQuality, isAcceptableQuality);
            Log.d(TAG, qualityLog);
            
            // Different paths based on quality assessment
            if (isHighQuality && isConsistent) {
                // High quality and consistent - proceed with registration
                stateManager.transitionTo(FaceRegistrationState.PROCESSING, 
                        "Kiểm tra chất lượng đạt. Đang đăng ký...");
                captureAndRegisterFace();
            } else if (isAcceptableQuality) {
                // Acceptable but not ideal - warn user but proceed
                stateManager.transitionTo(FaceRegistrationState.PROCESSING, 
                        "Chất lượng chấp nhận được. Đang tiến hành đăng ký...");
                captureAndRegisterFace();
            } else {
                // Low quality - provide specific feedback based on issues
                String feedbackMessage = generateQualityFeedback(averageScore, variance);
                lastDetailedErrorMessage = qualityLog + "\n\nDetailed Analysis: " + feedbackMessage;
                hasDetailedError = true;
                Log.e(TAG, "❌ Analysis failed: " + lastDetailedErrorMessage);
                stateManager.transitionTo(FaceRegistrationState.FAILED_OTHER, feedbackMessage);
            }
        }, ANALYSIS_DURATION_MS);
    }
    
    /**
     * Thiết lập UI cho phân tích
     */
    private void setupAnalysisUI() {
        if (binding == null || !isAdded()) return;
        
        // Kiểm tra nếu đã tạo UI trước đó
        if (analysisOverlay != null) {
            // Đảm bảo hiển thị UI chính xác
            analysisOverlay.setVisibility(View.VISIBLE);
            return;
        }
        
        // Tạo overlay cho phân tích
        analysisOverlay = LayoutInflater.from(requireContext())
                .inflate(R.layout.overlay_face_analysis, binding.flStudentSettingRegisterFaceIdCameraContainer, false);
        
        // Thêm vào container
        binding.flStudentSettingRegisterFaceIdCameraContainer.addView(analysisOverlay);
        
        // Lấy reference đến các thành phần UI
        analysisProgressBar = analysisOverlay.findViewById(R.id.progressBarAnalysis);
        analysisCountdownText = analysisOverlay.findViewById(R.id.tvAnalysisCountdown);
        
        // Đảm bảo progressBar ở trạng thái mặc định ban đầu
        if (analysisProgressBar != null) {
            analysisProgressBar.setProgress(0);
        }
        
        // Đặt text ban đầu cho countdown
        if (analysisCountdownText != null) {
            analysisCountdownText.setText("Analyzing...");
        }
        
        // Hiển thị UI
        analysisOverlay.setVisibility(View.VISIBLE);
        
        Log.d(TAG, "Analysis UI initialized and shown");
    }

    /**
     * Calculate variance of collected scores to assess consistency
     */
    private float calculateVariance(java.util.List<Float> scores, float mean) {
        float sumSquaredDiff = 0;
        for (float score : scores) {
            float diff = score - mean;
            sumSquaredDiff += diff * diff;
        }
        return sumSquaredDiff / scores.size();
    }

    /**
     * Generate specific feedback based on detected quality issues
     */
    private String generateQualityFeedback(float averageScore, float variance) {
        StringBuilder feedback = new StringBuilder();
        
        if (variance > 0.05) {
            feedback.append("Phát hiện khuôn mặt không ổn định. Vui lòng giữ khuôn mặt ổn định hơn và thử lại.");
            feedback.append("\n\nLỗi chi tiết: Chỉ số biến thiên (variance) = ").append(String.format(Locale.US, "%.5f", variance));
            feedback.append(" (vượt quá ngưỡng 0.05)");
        } else if (averageScore < 0.4f) {
            feedback.append("Chất lượng phát hiện rất thấp. Vui lòng thử lại trong điều kiện ánh sáng tốt hơn.");
            feedback.append("\n\nLỗi chi tiết: Điểm trung bình = ").append(String.format(Locale.US, "%.3f", averageScore));
            feedback.append(" (thấp hơn ngưỡng tối thiểu 0.4)");
        } else if (averageScore < 0.6f) {
            feedback.append("Chất lượng phát hiện thấp. Cải thiện ánh sáng và giảm chuyển động khuôn mặt.");
            feedback.append("\n\nLỗi chi tiết: Điểm trung bình = ").append(String.format(Locale.US, "%.3f", averageScore));
            feedback.append(" (thấp hơn ngưỡng khuyến nghị 0.6)");
        } else {
            feedback.append("Không thể có được hình ảnh đủ rõ ràng. Vui lòng thử lại với ánh sáng và vị trí tốt hơn.");
            feedback.append("\n\nLỗi chi tiết: Kết hợp giữa điểm phát hiện và độ ổn định không đáp ứng yêu cầu");
        }
        
        return feedback.toString();
    }
    }
    

