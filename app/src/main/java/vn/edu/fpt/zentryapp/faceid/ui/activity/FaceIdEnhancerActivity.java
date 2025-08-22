package vn.edu.fpt.zentryapp.faceid.ui.activity;

import android.Manifest;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.Rect;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.View;
import android.view.OrientationEventListener;
import android.view.Surface;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.OptIn;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;


import com.chaos.view.BuildConfig;
import com.google.common.util.concurrent.ListenableFuture;
import com.otaliastudios.cameraview.controls.Preview;

import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;


import vn.edu.fpt.zentryapp.R;
import vn.edu.fpt.zentryapp.faceid.data.service.FaceIdEnhancer;
import vn.edu.fpt.zentryapp.faceid.data.service.FaceDetector;
import vn.edu.fpt.zentryapp.faceid.util.CoordinateMapper;

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
    private Preview preview;
    private ImageAnalysis imageAnalysis;
    private ProcessCameraProvider cameraProvider;
    private OrientationEventListener orientationEventListener;
    // Warm-up frames to let AE/AF/denoise stabilize
    private int warmupFramesRemaining = 8;

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

        // Set challenge type: only gaze (RIGHT -> LEFT)
        faceIdEnhancer.setChallengeType(FaceIdEnhancer.ChallengeType.GAZE_ONLY);

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
                cameraProvider = cameraProviderFuture.get();

                int rotation = previewView.getDisplay().getRotation();
                // Set up the preview use case
                preview = new Preview.Builder()
                        .setTargetRotation(rotation)
                        .build();
                preview.setSurfaceProvider(previewView.getSurfaceProvider());

                // Set up the image analysis use case (adaptive resolution via aspect ratio)
                int targetAspect = AspectRatio.RATIO_16_9; // prefer wide for front cameras
                imageAnalysis = new ImageAnalysis.Builder()
                        .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                        .setTargetRotation(rotation)
                        .setTargetAspectRatio(targetAspect)
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

                // Orientation listener to dynamically update targetRotation
                if (orientationEventListener == null) {
                    orientationEventListener = new OrientationEventListener(this) {
                        @Override
                        public void onOrientationChanged(int orientation) {
                            if (orientation == ORIENTATION_UNKNOWN) return;
                            int newRotation;
                            if (orientation >= 45 && orientation < 135) {
                                newRotation = Surface.ROTATION_270;
                            } else if (orientation >= 135 && orientation < 225) {
                                newRotation = Surface.ROTATION_180;
                            } else if (orientation >= 225 && orientation < 315) {
                                newRotation = Surface.ROTATION_90;
                            } else {
                                newRotation = Surface.ROTATION_0;
                            }

                            try {
                                if (imageAnalysis != null) {
                                    imageAnalysis.setTargetRotation(newRotation);
                                }
                                if (preview != null) {
                                    preview.setTargetRotation(newRotation);
                                }
                            } catch (Exception ignored) { }
                        }
                    };
                }
                if (orientationEventListener != null) {
                    orientationEventListener.enable();
                }

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
        if (BuildConfig.DEBUG) {
            Log.d("DEBUG_ANALYZE", "Analyzing frame - Format: " + imageProxy.getFormat() +
                    ", Size: " + imageProxy.getWidth() + "x" + imageProxy.getHeight());
        }

        // Warm-up phase: skip first few frames for exposure/noise stabilization
        if (warmupFramesRemaining > 0) {
            warmupFramesRemaining--;
            if (BuildConfig.DEBUG && warmupFramesRemaining % 2 == 0) {
                Log.d("DEBUG_ANALYZE", "Skipping warm-up frame, remaining=" + warmupFramesRemaining);
            }
            imageProxy.close();
            return;
        }

        // Convert ImageProxy to Bitmap
        Bitmap bitmap = imageToBitmap(imageProxy);

        if (BuildConfig.DEBUG) {
            if (bitmap != null) {
                Log.d("DEBUG_ANALYZE", "Bitmap conversion OK - Size: " +
                        bitmap.getWidth() + "x" + bitmap.getHeight());
            } else {
                Log.e("DEBUG_ANALYZE", "Bitmap conversion FAILED - imageToBitmap returned null");
            }
        }

        if (bitmap == null) {
            imageProxy.close();
            return;
        }

        // Update coordinate mapping using standardized policy
        try {
            int viewW = previewView.getWidth();
            int viewH = previewView.getHeight();
            boolean isPreviewMirrored = true;   // front camera preview is mirrored visually
            boolean isBitmapMirrored = false;   // bitmap is NOT mirrored in this activity path
            CoordinateMapper.getInstance().updateMappingWithPolicy(
                    viewW, viewH, bitmap.getWidth(), bitmap.getHeight(),
                    isPreviewMirrored, isBitmapMirrored);
        } catch (Exception ignored) {}

        // Process the image with MediaPipe face detector
        List<FaceDetector.FaceDetectionResult> results = faceDetector.detectFaces(bitmap);
        if (BuildConfig.DEBUG) {
            Log.d("DEBUG_ANALYZE", "Face detection result: " + results.size() + " faces detected");
            if (results.isEmpty()) {
                Log.w("DEBUG_ANALYZE", "NO FACE DETECTED - This is the main issue!");
            }
        }


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
        try {
            Bitmap bmp = vn.edu.fpt.zentryapp.faceid.util.YuvToRgbConverter.convert(image, image.getImageInfo().getRotationDegrees());
            return bmp;
        } catch (Exception e) {
            Log.e(TAG, "Error converting image to bitmap", e);
            return null;
        }
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
        // Call super to satisfy lint and keep behavior consistent
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        // Handle permission result
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

    @Override
    protected void onPause() {
        super.onPause();
        if (orientationEventListener != null) orientationEventListener.disable();
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (orientationEventListener != null) orientationEventListener.enable();
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
                updateStatus("Gaze verified. Look straight for analysis");
                Log.d(TAG, "Gaze verified, completing verification");
                break;
            case VERIFIED:
                updateStatus("Verification successful!");
                Log.d(TAG, "Liveness verification completed successfully");
                // After full verification, proceed to next flow or finish
                runOnUiThread(() -> {
                    Toast.makeText(this, "Liveness verified. Proceeding...", Toast.LENGTH_SHORT).show();
                    finish();
                });
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

    @Override
    public void onGazeStepVerified(FaceIdEnhancer.Direction direction, int stepIndex, int totalSteps) {
        runOnUiThread(() -> {
            try {
                android.os.Vibrator v = (android.os.Vibrator) getSystemService(android.content.Context.VIBRATOR_SERVICE);
                if (v != null) {
                    if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                        v.vibrate(android.os.VibrationEffect.createOneShot(60, android.os.VibrationEffect.DEFAULT_AMPLITUDE));
                    } else {
                        v.vibrate(60);
                    }
                }
            } catch (Exception ignored) {}

            // Update UI to show step success and next instruction
            statusTextView.setText("Step " + (stepIndex + 1) + " ✓");
            if (stepIndex + 1 < totalSteps) {
                boolean nextLeft = (direction == FaceIdEnhancer.Direction.RIGHT);
                instructionTextView.setText(nextLeft ? "Please look left" : "Please look right");
            } else {
                instructionTextView.setText("Look straight at the camera");
            }
        });
    }

    @Override
    public void onGazePrompt(FaceIdEnhancer.Direction required, int stepIndex, int totalSteps) {
        runOnUiThread(() -> {
            String prompt = required == FaceIdEnhancer.Direction.LEFT ? "Please look left" : "Please look right";
            instructionTextView.setText(prompt);
            statusTextView.setText("Step " + (stepIndex + 1) + "/" + totalSteps);
        });
    }
}
