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

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

import vn.edu.fpt.zentryapp.R;
import vn.edu.fpt.zentryapp.auth.AuthManager;
import vn.edu.fpt.zentryapp.databinding.FragmentStudentSettingUpdateFaceIdBinding;
import vn.edu.fpt.zentryapp.student.data.service.FaceIdService;
import vn.edu.fpt.zentryapp.student.ui.components.CameraView;
import vn.edu.fpt.zentryapp.student.ui.components.FaceOverlayView;

public class StudentSettingUpdateFaceIdFragment extends Fragment {

    private static final int CAMERA_PERMISSION_REQUEST_CODE = 100;
    private static final String TAG = "UpdateFaceIdFragment";
    
    private FragmentStudentSettingUpdateFaceIdBinding binding;
    private FaceIdService faceIdService;
    private CameraView cameraView;
    private FaceOverlayView faceOverlayView;
    private boolean isCameraStarted = false;
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
    public void onViewCreated(@NonNull View view,
                              @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // Initialize FaceIdService
        faceIdService = new FaceIdService(requireContext());
        
        // Set up back button
        binding.ivBack.setOnClickListener(v -> requireActivity().onBackPressed());

        // Set last update time
        String lastUpdateTime = getLastUpdateTime();
        binding.tvLastUpdate.setText("Last update: " + lastUpdateTime);
        
        // Set up camera view
        setupCameraView();
        
        // Set up face overlay view
        setupFaceOverlayView();
        
        // Set up update button
        binding.btnUpdateFace.setOnClickListener(v -> {
            if (isCameraStarted && !isProcessing) {
                captureAndUpdateFace();
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
    
    private String getLastUpdateTime() {
        // Get last update time from SharedPreferences
        long lastUpdateTimestamp = requireContext().getSharedPreferences("prefs", 0)
                .getLong("faceid_last_update", System.currentTimeMillis());
        
        // Format the timestamp
        SimpleDateFormat dateFormat = new SimpleDateFormat("MMM dd, yyyy   /   HH:mm", Locale.US);
        return dateFormat.format(new Date(lastUpdateTimestamp)).toUpperCase();
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
    
    private void captureAndUpdateFace() {
        isProcessing = true;
        binding.progressBarUpdateFaceId.setVisibility(View.VISIBLE);
        binding.btnUpdateFace.setEnabled(false);
        
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
                
                // Update face ID
                updateFaceId(faceBitmap);
                
                // Update avatar preview
                binding.ivFaceAvatar.setImageBitmap(faceBitmap);
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
    
    private void updateFaceId(Bitmap faceBitmap) {
        // Get user ID from auth manager
        String userId = AuthManager.getInstance(requireContext()).getUserId();
        
        // Update face ID
        faceIdService.updateFaceId(faceBitmap, userId, new FaceIdService.FaceIdCallback() {
            @Override
            public void onSuccess(String message) {
                // Save current time as last update time
                long currentTime = System.currentTimeMillis();
                requireContext().getSharedPreferences("prefs", 0)
                        .edit()
                        .putLong("faceid_last_update", currentTime)
                        .apply();
                
                // Show success message
                showSuccessAndUpdateUI(message);
            }
            
            @Override
            public void onFailure(String errorMessage) {
                showError(errorMessage);
                resetProcessingState();
            }
        });
    }
    
    private void showSuccessAndUpdateUI(String message) {
        requireActivity().runOnUiThread(() -> {
            // Hide progress bar
            binding.progressBarUpdateFaceId.setVisibility(View.GONE);
            binding.btnUpdateFace.setEnabled(true);
            
            // Update last update time
            String lastUpdateTime = getLastUpdateTime();
            binding.tvLastUpdate.setText("Last update: " + lastUpdateTime);
            
            // Show success dialog
            new AlertDialog.Builder(requireContext())
                    .setTitle("Success")
                    .setMessage(message)
                    .setPositiveButton("OK", null)
                    .show();
            
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
        binding.progressBarUpdateFaceId.setVisibility(View.GONE);
        binding.btnUpdateFace.setEnabled(true);
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
