package vn.edu.fpt.zentryapp.faceid.ui.setting;

import android.Manifest;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.Rect;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

import vn.edu.fpt.zentryapp.auth.client.AuthManager;
import vn.edu.fpt.zentryapp.databinding.FragmentStudentSettingUpdateFaceIdBinding;
import vn.edu.fpt.zentryapp.faceid.data.service.FaceIdConfig;
import vn.edu.fpt.zentryapp.faceid.data.service.FaceIdService;
import vn.edu.fpt.zentryapp.faceid.data.service.FaceIdServiceManager;
import vn.edu.fpt.zentryapp.faceid.ui.components.CameraView;
import vn.edu.fpt.zentryapp.faceid.ui.components.OvalFaceOverlayView;
import vn.edu.fpt.zentryapp.faceid.ui.setting.detection.SpoofDetectionManager;

public class StudentSettingUpdateFaceIdFragment extends Fragment {

    private static final int CAMERA_PERMISSION_REQUEST_CODE = 100;
    private static final String TAG = "UpdateFaceIdFragment";
    
    private FragmentStudentSettingUpdateFaceIdBinding binding;
    private FaceIdService faceIdService;
    private SpoofDetectionManager spoofDetectionManager;
    private CameraView cameraView;
    private OvalFaceOverlayView faceOverlayView;
    private boolean isCameraStarted = false;
    private boolean isProcessing = false;
    
    // Current frame data
    private Bitmap currentFrameBitmap;
    private Rect currentFaceRect;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        binding = FragmentStudentSettingUpdateFaceIdBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        
        setupViews();
        setupClickListeners();
        
        // Initialize FaceIdService
        initializeFaceIdService();
    }
    
    private void setupViews() {
        // Setup camera view
        cameraView = new CameraView(requireContext());
        binding.flCameraContainer.addView(cameraView);
        
        // Setup overlay view with enhanced oval UI
        faceOverlayView = new OvalFaceOverlayView(requireContext());
        binding.flCameraContainer.addView(faceOverlayView);
    }
    
    private void setupClickListeners() {
        binding.ivBack.setOnClickListener(v -> requireActivity().onBackPressed());
        binding.btnCancel.setOnClickListener(v -> requireActivity().onBackPressed());
        
        binding.btnUpdate.setOnClickListener(v -> {
            if (!isProcessing) {
                updateFaceId();
            }
        });
    }
    
    private void initializeFaceIdService() {
        binding.progressBar.setVisibility(View.VISIBLE);
        binding.tvStatus.setText("Initializing...");
        
        FaceIdServiceManager.getInstance().initialize(requireContext(), new FaceIdServiceManager.InitCallback() {
            @Override
            public void onInitialized(FaceIdService service) {
                if (!isAdded()) return;
                
                faceIdService = service;
                
                // 🔧 NEW: Set update scenario for balanced validation
                faceIdService.setScenario(FaceIdConfig.Scenario.UPDATE);
                
                // Initialize SpoofDetectionManager with enhanced security
                if (faceIdService.getFaceSpoofDetector() != null) {
                    spoofDetectionManager = new SpoofDetectionManager(faceIdService.getFaceSpoofDetector(), requireContext());
                    // Set the oval boundary for enhanced security validation
                    if (faceOverlayView != null) {
                        spoofDetectionManager.setOvalBoundary(faceOverlayView.getOvalRect());
                    }
                }
                
                binding.progressBar.setVisibility(View.GONE);
                binding.tvStatus.setText("Look at the camera");
                
                checkCameraPermissionAndStart();
            }
            
            @Override
            public void onError(String message) {
                if (!isAdded()) return;
                
                binding.progressBar.setVisibility(View.GONE);
                binding.tvStatus.setText("Initialization failed");
                
                showErrorDialog("Failed to initialize face detection", message);
            }
        });
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
    
    private void startCamera() {
        if (isCameraStarted) {
            return;
        }
        
        try {
            cameraView.startCamera(getViewLifecycleOwner(), this::processFrame);
            isCameraStarted = true;
            binding.tvStatus.setText("Position your face in the oval");
        } catch (Exception e) {
            showErrorDialog("Camera Error", "Failed to start camera: " + e.getMessage());
        }
    }
    
    private void processFrame(Bitmap bitmap) {
        if (faceIdService == null || isProcessing) {
            return;
        }
        
        currentFrameBitmap = bitmap;
        
        // Process frame with oval boundary validation for enhanced security
        faceIdService.processContinuousFrame(bitmap, faceOverlayView.getOvalRect(), 
                new FaceIdService.ContinuousProcessingCallback() {
            @Override
            public void onFaceDetected(Rect boundingBox, boolean isSpoof, float spoofScore) {
                currentFaceRect = boundingBox;
                
                // Update face position in overlay for user guidance
                if (faceOverlayView != null) {
                    boolean isGoodPosition = faceOverlayView.updateFacePosition(boundingBox);
                    
                    // If position is bad, don't proceed with further processing
                    if (!isGoodPosition) {
                        binding.tvStatus.setText("Position your face properly in the oval");
                        binding.btnUpdate.setEnabled(false);
                        return;
                    }
                }
                
                // Use enhanced spoof detection with increased security
                if (spoofDetectionManager != null) {
                    spoofDetectionManager.analyzeFrame(bitmap, boundingBox, result -> {
                        handleSpoofResult(result, boundingBox);
                    });
                } else {
                    // Fallback to basic spoof detection with stricter thresholds
                    if (isSpoof || spoofScore > 0.65f) {  // Lowered from 0.7f for more sensitivity
                        binding.tvStatus.setText("Spoof detected! Please use your real face.");
                        binding.btnUpdate.setEnabled(false);
                    } else if (!isSpoof && spoofScore < 0.15f) {  // Decreased from 0.3f for more security
                        binding.tvStatus.setText("Face detected. Ready to update!");
                        binding.btnUpdate.setEnabled(true);
                    } else {
                        binding.tvStatus.setText("Uncertain detection. Please improve lighting and position.");
                        binding.btnUpdate.setEnabled(false);
                    }
                }
            }
            
            @Override
            public void onNoFaceDetected() {
                currentFaceRect = null;
                binding.tvStatus.setText("No face detected. Look at the camera.");
                binding.btnUpdate.setEnabled(false);
                
                if (faceOverlayView != null) {
                    faceOverlayView.clear();
                }
            }
            
            @Override
            public void onMultipleFacesDetected() {
                currentFaceRect = null;
                binding.tvStatus.setText("Multiple faces detected. Only one face should be visible.");
                binding.btnUpdate.setEnabled(false);
                
                if (faceOverlayView != null) {
                    faceOverlayView.clear();
                }
            }
            
            @Override
            public void onError(String errorMessage) {
                binding.tvStatus.setText("Detection error: " + errorMessage);
                binding.btnUpdate.setEnabled(false);
            }
        });
    }
    
    /**
     * Handle enhanced spoof detection result with improved security
     */
    private void handleSpoofResult(SpoofDetectionManager.SpoofDetectionResult result, Rect boundingBox) {
        if (!isAdded()) {
            return;
        }

        // Handle spoof detection with higher security threshold
        if (result.isSpoof) {
            binding.tvStatus.setText(result.explanation);
            binding.btnUpdate.setEnabled(false);
            return;
        }

        // For real face detections
        if (!result.isSpoof) {
            // Check face position using oval view
            boolean isInGoodPosition = faceOverlayView != null && 
                                      faceOverlayView.validateFaceWithinOval(boundingBox);
            
            if (!isInGoodPosition) {
                binding.tvStatus.setText("Position your face properly in the oval");
                binding.btnUpdate.setEnabled(false);
                return;
            }
            
            // Enable update button for real faces with good position
            if (result.shouldProceed) {
                binding.tvStatus.setText("Real face detected. Ready to update!");
                binding.btnUpdate.setEnabled(true);
            } else {
                binding.tvStatus.setText("Keep your face steady");
                binding.btnUpdate.setEnabled(false);
            }
        }
    }
    
    private void updateFaceId() {
        if (currentFrameBitmap == null || currentFaceRect == null) {
            showErrorDialog("Update Failed", "No face detected");
            return;
        }
        
        // Final validation of face position
        if (faceOverlayView != null) {
            boolean isInGoodPosition = faceOverlayView.validateFaceWithinOval(currentFaceRect);
            if (!isInGoodPosition) {
                binding.tvStatus.setText("Please position your face properly in the oval");
                return;
            }
        }
        
        isProcessing = true;
        binding.progressBar.setVisibility(View.VISIBLE);
        binding.tvStatus.setText("Updating your Face ID...");
        binding.btnUpdate.setEnabled(false);
        
        String userId = AuthManager.getInstance(requireContext()).getCurrentUserId();
        if (userId == null || userId.isEmpty()) {
            isProcessing = false;
            binding.progressBar.setVisibility(View.GONE);
            showErrorDialog("Update Failed", "User not logged in");
            return;
        }
        
        // Stop camera while processing
        stopCamera();
        
        // Get current time for update record
        String updateTime = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
                .format(new Date());
        
        // Update face ID with enhanced security validation using oval boundary
        faceIdService.captureAndRegisterFace(
                currentFrameBitmap, 
                currentFaceRect, 
                faceOverlayView != null ? faceOverlayView.getOvalRect() : null,
                userId, 
                new FaceIdService.FaceIdCallback() {
                    @Override
                    public void onSuccess(String message) {
                        if (!isAdded()) return;
                        
                        isProcessing = false;
                        binding.progressBar.setVisibility(View.GONE);
                        
                        showSuccessDialog("Face ID Updated", 
                                "Your Face ID was successfully updated on " + updateTime);
                    }
                    
                    @Override
                    public void onFailure(String errorMessage) {
                        if (!isAdded()) return;
                        
                        isProcessing = false;
                        binding.progressBar.setVisibility(View.GONE);
                        
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
    
    private void showSuccessDialog(String title, String message) {
        new AlertDialog.Builder(requireContext())
                .setTitle(title)
                .setMessage(message)
                .setPositiveButton("OK", (dialog, which) -> {
                    if (isAdded()) {
                        requireActivity().onBackPressed();
                    }
                })
                .setCancelable(false)
                .show();
    }
    
    private void showErrorDialog(String title, String message) {
        new AlertDialog.Builder(requireContext())
                .setTitle(title)
                .setMessage(message)
                .setPositiveButton("OK", null)
                .show();
    }
    
    private void stopCamera() {
        try {
            if (cameraView != null) {
                cameraView.stopCamera();
            }
        } catch (Exception e) {
            // Ignore
        } finally {
            isCameraStarted = false;
        }
    }
    
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        if (requestCode == CAMERA_PERMISSION_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                startCamera();
            } else {
                showErrorDialog("Permission Required", 
                        "Camera permission is required to update Face ID");
                
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
        binding = null;
    }
}
