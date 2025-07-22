package vn.edu.fpt.zentryapp.student.ui.setting;

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
import androidx.navigation.NavController;
import androidx.navigation.fragment.NavHostFragment;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import vn.edu.fpt.zentryapp.R;
import vn.edu.fpt.zentryapp.auth.AuthManager;
import vn.edu.fpt.zentryapp.databinding.FragmentStudentSettingVerifyFaceIdBinding;
import vn.edu.fpt.zentryapp.student.data.service.FaceIdService;
import vn.edu.fpt.zentryapp.student.ui.components.CameraView;
import vn.edu.fpt.zentryapp.student.ui.components.FaceOverlayView;

public class StudentSettingVerifyFaceIdFragment extends Fragment {

    private static final int CAMERA_PERMISSION_REQUEST_CODE = 100;
    private static final String TAG = "VerifyFaceIdFragment";
    
    private FragmentStudentSettingVerifyFaceIdBinding binding;
    private FaceIdService faceIdService;
    private CameraView cameraView;
    private FaceOverlayView faceOverlayView;
    private boolean isCameraStarted = false;
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
    public void onViewCreated(@NonNull View view,
                              @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        
        // Initialize FaceIdService
        faceIdService = new FaceIdService(requireContext());
        
        // Get NavController
        navController = NavHostFragment.findNavController(this);
        
        // Set up back button
        binding.ivBack.setOnClickListener(v -> requireActivity().onBackPressed());
        
        // Set up camera view
        setupCameraView();
        
        // Set up face overlay view
        setupFaceOverlayView();
        
        // Set up verify button
        binding.btnVerifyFaceId.setOnClickListener(v -> {
            if (isCameraStarted && !isProcessing) {
                captureAndVerifyFace();
            } else {
                Toast.makeText(requireContext(), "Camera not ready", Toast.LENGTH_SHORT).show();
            }
        });
        
        // Request camera permission if needed
        if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.CAMERA) 
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(requireActivity(), 
                    new String[]{Manifest.permission.CAMERA}, 
                    CAMERA_PERMISSION_REQUEST_CODE);
        } else {
            startCamera();
        }
    }
    
    private void setupCameraView() {
        // Create camera view
        cameraView = new CameraView(requireContext());
        
        // Add camera view to container
        binding.flCameraContainer.addView(cameraView);
    }
    
    private void setupFaceOverlayView() {
        // Create face overlay view
        faceOverlayView = new FaceOverlayView(requireContext());
        
        // Add face overlay view to container
        binding.flCameraContainer.addView(faceOverlayView);
    }
    
    private void startCamera() {
        if (cameraView != null) {
            cameraView.startCamera(getViewLifecycleOwner());
            isCameraStarted = true;
        }
    }
    
    private void captureAndVerifyFace() {
        isProcessing = true;
        binding.progressBarVerifyFaceId.setVisibility(View.VISIBLE);
        binding.btnVerifyFaceId.setEnabled(false);
        binding.tvVerificationStatus.setText("Verifying...");
        
        // Capture photo
        cameraView.capturePhoto(new CameraView.CaptureCallback() {
            @Override
            public void onCaptured(Bitmap bitmap) {
                // Process face image
                processFaceImage(bitmap);
            }
            
            @Override
            public void onError(String message) {
                showError("Failed to capture image: " + message);
                resetProcessingState();
            }
        });
    }
    
    private void processFaceImage(Bitmap bitmap) {
        // Process face image
        faceIdService.processFaceImage(bitmap, new FaceIdService.FaceDetectionCallback() {
            @Override
            public void onFaceDetected(Bitmap faceBitmap, Rect boundingBox) {
                // Show face detection result
                faceOverlayView.setFaceDetectionResult(boundingBox, false, "Face detected");
                
                // Verify face ID
                verifyFaceId(faceBitmap);
            }
            
            @Override
            public void onNoFaceDetected() {
                showError("No face detected. Please try again.");
                resetProcessingState();
            }
            
            @Override
            public void onMultipleFacesDetected() {
                showError("Multiple faces detected. Please ensure only one face is in the frame.");
                resetProcessingState();
            }
            
            @Override
            public void onError(String errorMessage) {
                showError(errorMessage);
                resetProcessingState();
            }
        });
    }
    
    private void verifyFaceId(Bitmap faceBitmap) {
        // Get user ID from auth manager
        String userId = AuthManager.getInstance(requireContext()).getUserId();
        
        // Verify face ID
        faceIdService.verifyFaceId(faceBitmap, userId, new FaceIdService.FaceIdCallback() {
            @Override
            public void onSuccess(String message) {
                // Show success message
                showVerificationResult(true, message);
            }
            
            @Override
            public void onFailure(String errorMessage) {
                // Show failure message
                showVerificationResult(false, errorMessage);
            }
        });
    }
    
    private void showVerificationResult(boolean isSuccess, String message) {
        requireActivity().runOnUiThread(() -> {
            // Hide progress bar
            binding.progressBarVerifyFaceId.setVisibility(View.GONE);
            binding.btnVerifyFaceId.setEnabled(true);
            
            // Update verification status
            if (isSuccess) {
                binding.tvVerificationStatus.setText("Verification successful");
                binding.tvVerificationStatus.setTextColor(ContextCompat.getColor(requireContext(), R.color.green_success));
                
                // Show success dialog
                new AlertDialog.Builder(requireContext())
                        .setTitle("Success")
                        .setMessage(message)
                        .setPositiveButton("Continue", (dialog, which) -> {
                            // Navigate to the next screen or perform action
                            // This depends on the flow where verification is used
                            navController.navigateUp();
                        })
                        .setCancelable(false)
                        .show();
            } else {
                binding.tvVerificationStatus.setText("Verification failed");
                binding.tvVerificationStatus.setTextColor(ContextCompat.getColor(requireContext(), R.color.red_error));
                
                // Show error dialog
                new AlertDialog.Builder(requireContext())
                        .setTitle("Verification Failed")
                        .setMessage(message)
                        .setPositiveButton("Try Again", (dialog, which) -> {
                            resetProcessingState();
                        })
                        .setNegativeButton("Cancel", (dialog, which) -> {
                            requireActivity().onBackPressed();
                        })
                        .setCancelable(false)
                        .show();
            }
            
            isProcessing = false;
        });
    }
    
    private void showError(String message) {
        requireActivity().runOnUiThread(() -> {
            Toast.makeText(requireContext(), message, Toast.LENGTH_LONG).show();
            resetProcessingState();
        });
    }
    
    private void resetProcessingState() {
        isProcessing = false;
        binding.progressBarVerifyFaceId.setVisibility(View.GONE);
        binding.btnVerifyFaceId.setEnabled(true);
        binding.tvVerificationStatus.setText("Ready to verify");
        binding.tvVerificationStatus.setTextColor(ContextCompat.getColor(requireContext(), R.color.black));
        faceOverlayView.clear();
    }
    
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        if (requestCode == CAMERA_PERMISSION_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                startCamera();
            } else {
                Toast.makeText(requireContext(), "Camera permission is required", Toast.LENGTH_LONG).show();
                requireActivity().onBackPressed();
            }
        }
    }
    
    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (cameraView != null) {
            cameraView.stopCamera();
        }
        binding = null;
    }
} 