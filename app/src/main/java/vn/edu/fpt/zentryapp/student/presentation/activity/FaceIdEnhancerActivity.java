package vn.edu.fpt.zentryapp.student.presentation.activity;

import android.Manifest;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.Rect;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.OptIn;
import androidx.appcompat.app.AppCompatActivity;
import androidx.camera.core.CameraSelector;
import androidx.camera.core.ExperimentalGetImage;
import androidx.camera.core.ImageAnalysis;
import androidx.camera.core.ImageProxy;
import androidx.camera.core.Preview;
import androidx.camera.lifecycle.ProcessCameraProvider;
import androidx.camera.view.PreviewView;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.google.common.util.concurrent.ListenableFuture;
import com.google.mediapipe.tasks.vision.facedetector.FaceDetectorResult;
import com.google.mediapipe.tasks.vision.facedetector.FaceDetector.FaceDetectorOptions;
import com.google.mediapipe.tasks.vision.core.RunningMode;
import com.google.mediapipe.framework.image.BitmapImageBuilder;
import com.google.mediapipe.framework.image.MPImage;
import com.google.mediapipe.tasks.components.containers.Detection;
import com.google.mediapipe.tasks.core.BaseOptions;

import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;


import vn.edu.fpt.zentryapp.R;
import vn.edu.fpt.zentryapp.student.data.service.FaceIdEnhancer;
import vn.edu.fpt.zentryapp.student.data.service.FaceDetector;

public class FaceIdEnhancerActivity extends AppCompatActivity implements 
        FaceIdEnhancer.FaceIdEnhancerCallback {
    
    private static final String TAG = "FaceIdEnhancerActivity";
    private static final int REQUEST_CAMERA_PERMISSION = 10;
    
    // UI components
    private PreviewView previewView;
    private TextView statusTextView;
    private TextView instructionTextView;
    private Button resetButton;
    
    // Camera processing
    private ExecutorService cameraExecutor;
    private FaceDetector faceDetector;
    
    // Face ID enhancer
    private FaceIdEnhancer faceIdEnhancer;
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_face_id_enhancer);
        
        // Initialize UI components
        previewView = findViewById(R.id.previewView);
        statusTextView = findViewById(R.id.statusTextView);
        instructionTextView = findViewById(R.id.instructionTextView);
        resetButton = findViewById(R.id.resetButton);
        
        // Set up reset button
        resetButton.setOnClickListener(v -> resetVerification());
        
        // Initialize face detector
        faceDetector = new FaceDetector(this);
        
        // Initialize camera executor
        cameraExecutor = Executors.newSingleThreadExecutor();
        
        // Initialize face ID enhancer
        faceIdEnhancer = new FaceIdEnhancer(this, this);
        
        // Set challenge type (can be customized)
        faceIdEnhancer.setChallengeType(FaceIdEnhancer.ChallengeType.BLINK_AND_GAZE);
        
        // Check camera permission
        if (allPermissionsGranted()) {
            startCamera();
        } else {
            ActivityCompat.requestPermissions(this, 
                    new String[]{Manifest.permission.CAMERA}, 
                    REQUEST_CAMERA_PERMISSION);
        }
        
        // Show initial instructions
        updateInstructions(faceIdEnhancer.getCurrentState());
    }
    
    /**
     * Start the camera with face detection
     */
    private void startCamera() {
        ListenableFuture<ProcessCameraProvider> cameraProviderFuture = 
                ProcessCameraProvider.getInstance(this);
        
        cameraProviderFuture.addListener(() -> {
            try {
                ProcessCameraProvider cameraProvider = cameraProviderFuture.get();
                
                // Set up the preview use case
                Preview preview = new Preview.Builder().build();
                preview.setSurfaceProvider(previewView.getSurfaceProvider());
                
                // Set up the image analysis use case
                ImageAnalysis imageAnalysis = new ImageAnalysis.Builder()
                        .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                        .build();
                
                imageAnalysis.setAnalyzer(cameraExecutor, this::analyzeFace);
                
                // Select the front camera
                CameraSelector cameraSelector = new CameraSelector.Builder()
                        .requireLensFacing(CameraSelector.LENS_FACING_FRONT)
                        .build();
                
                // Unbind any bound use cases before rebinding
                cameraProvider.unbindAll();
                
                // Bind use cases to camera
                cameraProvider.bindToLifecycle(this, cameraSelector, preview, imageAnalysis);
                
            } catch (ExecutionException | InterruptedException e) {
                Log.e(TAG, "Error starting camera", e);
            }
        }, ContextCompat.getMainExecutor(this));
    }
    
    /**
     * Analyze each camera frame for faces
     */
    @OptIn(markerClass = ExperimentalGetImage.class)
    private void analyzeFace(ImageProxy imageProxy) {
        // Convert ImageProxy to Bitmap
        Bitmap bitmap = imageToBitmap(imageProxy);
        if (bitmap == null) {
            imageProxy.close();
            return;
        }
        
        // Process the image with MediaPipe face detector
        List<FaceDetector.FaceDetectionResult> results = faceDetector.detectFaces(bitmap);
        
        if (results.isEmpty()) {
            // No face detected
            runOnUiThread(() -> updateStatus("No face detected"));
            imageProxy.close();
            return;
        }
        
        // Get the first detected face
        FaceDetector.FaceDetectionResult result = results.get(0);
        Bitmap faceBitmap = result.getCroppedBitmap();
        Rect faceRect = result.getBoundingBox();
        
        // Process with face ID enhancer
        faceIdEnhancer.processFaceFrame(faceBitmap, faceRect);
        
        imageProxy.close();
    }
    
    /**
     * Convert ImageProxy to Bitmap
     */
    @OptIn(markerClass = ExperimentalGetImage.class)
    private Bitmap imageToBitmap(ImageProxy image) {
        if (image.getImage() == null) {
            return null;
        }
        
        try {
            // Convert YUV to RGB
            android.media.Image mediaImage = image.getImage();
            int width = mediaImage.getWidth();
            int height = mediaImage.getHeight();
            
            // Create a bitmap from the YUV image
            Bitmap bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
            
            // Convert YUV to RGB (simplified conversion)
            // In a real implementation, you'd use a more sophisticated YUV to RGB conversion
            android.media.Image.Plane[] planes = mediaImage.getPlanes();
            if (planes.length >= 3) {
                // Use the Y plane for grayscale conversion
                android.media.Image.Plane yPlane = planes[0];
                java.nio.ByteBuffer yBuffer = yPlane.getBuffer();
                int[] pixels = new int[width * height];
                
                for (int i = 0; i < pixels.length; i++) {
                    int y = yBuffer.get() & 0xFF;
                    pixels[i] = (0xFF << 24) | (y << 16) | (y << 8) | y;
                }
                
                bitmap.setPixels(pixels, 0, width, 0, 0, width, height);
            }
            
            return bitmap;
            
        } catch (Exception e) {
            Log.e(TAG, "Error converting image to bitmap", e);
        }
        
        return null;
    }
    
    /**
     * Reset the verification process
     */
    private void resetVerification() {
        faceIdEnhancer.reset();
        updateStatus("Place your face in the frame");
        updateInstructions(faceIdEnhancer.getCurrentState());
    }
    
    /**
     * Update the status text
     */
    private void updateStatus(String status) {
        runOnUiThread(() -> statusTextView.setText(status));
    }
    
    /**
     * Update instructions based on current state
     */
    private void updateInstructions(FaceIdEnhancer.AuthState state) {
        String instructions = "";
        
        switch (state) {
            case WAITING:
                instructions = "Please position your face in the frame";
                break;
            case FACE_DETECTED:
                instructions = "Hold still while we analyze your face";
                break;
            case ANALYZING:
                instructions = "Please blink and move your gaze around the screen";
                break;
            case BLINK_VERIFIED:
                instructions = "Blink detected! Now move your gaze to each corner";
                break;
            case GAZE_VERIFIED:
                instructions = "Gaze verified! Almost done...";
                break;
            case VERIFIED:
                instructions = "Verification complete! You are authenticated.";
                break;
            case FAILED:
                instructions = "Verification failed. Please try again.";
                break;
        }
        
        final String finalInstructions = instructions;
        runOnUiThread(() -> instructionTextView.setText(finalInstructions));
    }
    
    /**
     * Check if all required permissions are granted
     */
    private boolean allPermissionsGranted() {
        return ContextCompat.checkSelfPermission(
                this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED;
    }
    
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, 
                                          @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        
        if (requestCode == REQUEST_CAMERA_PERMISSION) {
            if (allPermissionsGranted()) {
                startCamera();
            } else {
                Toast.makeText(this, "Camera permission is required", Toast.LENGTH_SHORT).show();
                finish();
            }
        }
    }
    
    @Override
    protected void onDestroy() {
        super.onDestroy();
        cameraExecutor.shutdown();
        faceIdEnhancer.close();
    }
    
    //------------------------------------------------------------------------------
    // FaceIdEnhancer.FaceIdEnhancerCallback Implementation
    //------------------------------------------------------------------------------
    
    @Override
    public void onStateChanged(FaceIdEnhancer.AuthState newState) {
        updateInstructions(newState);
        
        switch (newState) {
            case WAITING:
                updateStatus("Waiting for face");
                break;
            case FACE_DETECTED:
                updateStatus("Face detected");
                Log.d(TAG, "Face detected, waiting for landmark extraction");
                break;
            case ANALYZING:
                updateStatus("Analyzing face...");
                Log.d(TAG, "Landmark extraction succeeded, analyzing for blinks and gaze");
                break;
            case BLINK_VERIFIED:
                updateStatus("Blink verified");
                Log.d(TAG, "Blink verified, now need gaze verification");
                break;
            case GAZE_VERIFIED:
                updateStatus("Gaze verified");
                Log.d(TAG, "Gaze verified, completing verification");
                break;
            case VERIFIED:
                updateStatus("Verification successful!");
                Log.d(TAG, "Liveness verification completed successfully");
                break;
            case FAILED:
                updateStatus("Verification failed");
                Log.d(TAG, "Liveness verification failed");
                break;
        }
    }
    
    @Override
    public void onBlinkDetected() {
        Log.d(TAG, "Blink detected! EyeBlinkDetector callback received");
        runOnUiThread(() -> {
            updateStatus("Blink detected! Now move your gaze around");
            // Add visual feedback
            statusTextView.setTextColor(getResources().getColor(android.R.color.holo_green_dark));
            // Reset color after 2 seconds
            new Handler(Looper.getMainLooper()).postDelayed(() -> {
                statusTextView.setTextColor(getResources().getColor(android.R.color.black));
            }, 2000);
        });
    }
    
    @Override
    public void onGazeDirectionChanged(float x, float y) {
        // Log gaze direction changes
        Log.d(TAG, "Gaze direction changed: x=" + x + ", y=" + y);
        
        // Optionally update UI with gaze direction
        // For example, show a dot representing where the user is looking
    }
    
    @Override
    public void onLivenessVerified(boolean isLive) {
        if (isLive) {
            Log.d(TAG, "Liveness verified - Real person detected");
            updateStatus("Liveness verified - You are a real person!");
        }
    }
    
    @Override
    public void onVerificationComplete(boolean success) {
        if (success) {
            // Show success UI
            Log.d(TAG, "Verification complete: SUCCESS");
            runOnUiThread(() -> {
                resetButton.setVisibility(View.VISIBLE);
                Toast.makeText(this, "Liveness verification successful!", Toast.LENGTH_LONG).show();
                // Could navigate to next screen or show success animation
            });
        } else {
            // Show failure UI
            Log.d(TAG, "Verification complete: FAILURE");
            runOnUiThread(() -> {
                resetButton.setVisibility(View.VISIBLE);
                Toast.makeText(this, "Liveness verification failed", Toast.LENGTH_LONG).show();
                // Could show error message or retry option
            });
        }
    }
}
