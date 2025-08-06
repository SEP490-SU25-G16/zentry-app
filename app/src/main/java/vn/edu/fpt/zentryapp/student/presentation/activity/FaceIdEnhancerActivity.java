package vn.edu.fpt.zentryapp.student.presentation.activity;

import android.Manifest;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.Rect;
import android.os.Bundle;
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
import com.google.mlkit.vision.common.InputImage;
import com.google.mlkit.vision.face.Face;
import com.google.mlkit.vision.face.FaceDetection;
import com.google.mlkit.vision.face.FaceDetector;
import com.google.mlkit.vision.face.FaceDetectorOptions;

import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import vn.edu.fpt.zentryapp.R;
import vn.edu.fpt.zentryapp.student.data.service.FaceIdEnhancer;

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
        FaceDetectorOptions options = new FaceDetectorOptions.Builder()
                .setPerformanceMode(FaceDetectorOptions.PERFORMANCE_MODE_FAST)
                .setClassificationMode(FaceDetectorOptions.CLASSIFICATION_MODE_ALL)
                .build();
        faceDetector = FaceDetection.getClient(options);
        
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
        // Get image for face detection
        InputImage image = InputImage.fromMediaImage(
                imageProxy.getImage(), 
                imageProxy.getImageInfo().getRotationDegrees()
        );
        
        // Process the image with ML Kit face detector
        faceDetector.process(image)
                .addOnSuccessListener(faces -> processFaces(faces, imageProxy))
                .addOnFailureListener(e -> Log.e(TAG, "Face detection failed", e))
                .addOnCompleteListener(task -> imageProxy.close());
    }
    
    /**
     * Process detected faces
     */
    private void processFaces(List<Face> faces, ImageProxy imageProxy) {
        if (faces.isEmpty()) {
            // No face detected
            runOnUiThread(() -> updateStatus("No face detected"));
            return;
        }
        
        // Get the first detected face
        Face face = faces.get(0);
        
        // Convert ImageProxy to Bitmap
        Bitmap faceBitmap = imageToBitmap(imageProxy);
        if (faceBitmap == null) {
            return;
        }
        
        // Get face bounding box
        Rect faceRect = face.getBoundingBox();
        
        // Process with face ID enhancer
        faceIdEnhancer.processFaceFrame(faceBitmap, faceRect);
    }
    
    /**
     * Convert ImageProxy to Bitmap
     */
    @OptIn(markerClass = ExperimentalGetImage.class)
    private Bitmap imageToBitmap(ImageProxy image) {
        if (image.getImage() == null) {
            return null;
        }
        
        // Use InputImage utility from ML Kit to convert image and get bitmap
        try {
            InputImage inputImage = InputImage.fromMediaImage(
                    image.getImage(), 
                    image.getImageInfo().getRotationDegrees()
            );
            
            // Get bitmap from InputImage
            Bitmap bitmap = inputImage.getBitmapInternal();
            if (bitmap != null) {
                // Create a copy of the bitmap since the original may be recycled
                return Bitmap.createBitmap(bitmap);
            } else {
                Log.e(TAG, "Failed to get bitmap from InputImage");
            }
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
        updateStatus("Blink detected! Now move your gaze around");
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
