package vn.edu.fpt.zentryapp.faceid.data.service;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.PointF;
import android.graphics.Rect;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import com.google.mediapipe.tasks.vision.facelandmarker.FaceLandmarker;
import com.google.mediapipe.tasks.vision.facelandmarker.FaceLandmarkerResult;
import com.google.mediapipe.tasks.vision.facelandmarker.FaceLandmarker.FaceLandmarkerOptions;
import com.google.mediapipe.tasks.vision.core.RunningMode;
import com.google.mediapipe.framework.image.BitmapImageBuilder;
import com.google.mediapipe.framework.image.MPImage;
import com.google.mediapipe.tasks.core.BaseOptions;
import com.google.mediapipe.tasks.components.containers.NormalizedLandmark;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

/**
 * MediaPipe-based face landmark extractor for real-time facial landmark detection
 * 
 * IMPORTANT: This class requires the face_landmarker.task file to be placed in:
 * app/src/main/assets/face_landmarker.task
 * 
 * Download instructions:
 * 1. Go to: https://storage.googleapis.com/mediapipe-models/face_landmarker/face_landmarker/float16/1/face_landmarker.task
 * 2. Download the file
 * 3. Place it in: app/src/main/assets/face_landmarker.task
 * 4. Clean and rebuild the project
 * 
 * This file is essential for real facial landmark detection and cannot be replaced by .tflite models.
 * 
 * PRELOADED MODEL: This class is now preloaded during app startup in FaceIdService.
 * 
 * RUNNING MODE: Uses IMAGE mode for processing individual face images (not continuous stream).
 */
public class MediaPipeFaceLandmarkExtractor {
    private static final String TAG = "MediaPipeFaceLandmarkExtractor";
    
    // MediaPipe components
    private final FaceLandmarker faceLandmarker;
    private final ExecutorService executor;
    private final Handler mainHandler;
    
    // Add volatile flag to track executor state
    private volatile boolean isExecutorActive = true;
    
    // Landmark storage
    private List<PointF> leftEyePoints = new ArrayList<>();
    private List<PointF> rightEyePoints = new ArrayList<>();
    private Map<Integer, PointF> faceLandmarks = new HashMap<>();
    private Map<Integer, List<PointF>> faceContours = new HashMap<>();
    
    // Eye state tracking
    private float leftEyeOpenProbability = 1.0f;
    private float rightEyeOpenProbability = 1.0f;
    private float[] headEulerAngles = new float[3]; // Pitch, roll, yaw
    
    // Eye regions for gaze estimation
    private Bitmap lastLeftEyeRegion;
    private Bitmap lastRightEyeRegion;
    
    /**
     * Constructor - Initializes MediaPipe FaceLandmarker with real model
     * This is called during app startup by FaceIdService
     */
    public MediaPipeFaceLandmarkExtractor(Context context) {
        this.executor = Executors.newSingleThreadExecutor();
        this.mainHandler = new Handler(Looper.getMainLooper());
        
        // Initialize MediaPipe FaceLandmarker with real model
        FaceLandmarker faceLandmarkerInstance = null;
        try {
            // Configure MediaPipe FaceLandmarker options
            BaseOptions baseOptions = BaseOptions.builder()
                    .setModelAssetPath("face_landmarker.task")
                    .build();
            
            FaceLandmarkerOptions options = FaceLandmarkerOptions.builder()
                    .setBaseOptions(baseOptions)
                    .setRunningMode(RunningMode.IMAGE)  // 🔧 FIXED: Use IMAGE mode instead of LIVE_STREAM
                    .setNumFaces(1)
                    .setOutputFaceBlendshapes(false)
                    .build();
            
            // Create FaceLandmarker instance
            faceLandmarkerInstance = FaceLandmarker.createFromOptions(context, options);
            Log.d(TAG, "MediaPipe FaceLandmarker initialized successfully with real model (PRELOADED)");
            
        } catch (Exception e) {
            Log.e(TAG, "Failed to initialize MediaPipe FaceLandmarker with real model", e);
            if (e.getMessage() != null && e.getMessage().contains("live stream mode")) {
                Log.e(TAG, "ERROR: Running mode configuration issue - using IMAGE mode");
            } else {
                Log.e(TAG, "ERROR: face_landmarker.task file not found!");
                Log.e(TAG, "SOLUTION: Download face_landmarker.task from:");
                Log.e(TAG, "https://storage.googleapis.com/mediapipe-models/face_landmarker/face_landmarker/float16/1/face_landmarker.task");
                Log.e(TAG, "Then place it in: app/src/main/assets/face_landmarker.task");
                Log.e(TAG, "Finally, clean and rebuild the project");
            }
            
            // Show user-friendly error message
            if (context instanceof android.app.Activity) {
                android.app.Activity activity = (android.app.Activity) context;
                activity.runOnUiThread(() -> {
                    android.widget.Toast.makeText(activity, 
                        "Face landmarker model not found. Please check logs for download instructions.", 
                        android.widget.Toast.LENGTH_LONG).show();
                });
            }
        }
        
        this.faceLandmarker = faceLandmarkerInstance;
    }
    
    /**
     * Check if the MediaPipe FaceLandmarker is available (model loaded successfully)
     * @return true if the model is available, false otherwise
     */
    public boolean isModelAvailable() {
        return faceLandmarker != null;
    }

    /**
     * Check if the extractor is still active and can be used
     * @return true if the extractor is active, false otherwise
     */
    public boolean isActive() {
        return isExecutorActive && !executor.isShutdown() && !executor.isTerminated();
    }


    /**
     * Callback for face landmark extraction results
     */
    public interface LandmarkExtractionCallback {
        void onLandmarksExtracted(boolean success);
    }
    
    /**
     * Convert NormalizedLandmark to PointF
     */
    private PointF convertNormalizedLandmarkToPointF(NormalizedLandmark landmark, int imageWidth, int imageHeight) {
        // NormalizedLandmark has x, y, z coordinates in range [0, 1]
        // Convert to pixel coordinates
        float pixelX = landmark.x() * imageWidth;
        float pixelY = landmark.y() * imageHeight;
        return new PointF(pixelX, pixelY);
    }
    
    /**
     * Convert List<NormalizedLandmark> to List<PointF>
     */
    private List<PointF> convertNormalizedLandmarksToPointF(List<NormalizedLandmark> normalizedLandmarks, int imageWidth, int imageHeight) {
        List<PointF> pointFLandmarks = new ArrayList<>();
        for (NormalizedLandmark landmark : normalizedLandmarks) {
            pointFLandmarks.add(convertNormalizedLandmarkToPointF(landmark, imageWidth, imageHeight));
        }
        return pointFLandmarks;
    }
    
    /**
     * Extract landmarks from a face image using real MediaPipe model
     * 
     * @param faceBitmap The face bitmap
     * @param faceRect The face bounding box
     * @param callback Callback for extraction results
     */
    public void extractLandmarks(Bitmap faceBitmap, Rect faceRect, LandmarkExtractionCallback callback) {
        // Check if executor is still active before executing
        if (!isExecutorActive || executor.isShutdown() || executor.isTerminated()) {
            Log.w(TAG, "Executor is not active, skipping landmark extraction");
            if (callback != null) {
                runOnMainThread(() -> callback.onLandmarksExtracted(false));
            }
            return;
        }
        
        executor.execute(() -> {
            try {
                // Double-check executor state inside the task
                if (!isExecutorActive || executor.isShutdown() || executor.isTerminated()) {
                    Log.w(TAG, "Executor became inactive during task execution");
                    runOnMainThread(() -> callback.onLandmarksExtracted(false));
                    return;
                }
                
                // Check if faceLandmarker is available (real model loaded)
                if (faceLandmarker == null) {
                    Log.e(TAG, "MediaPipe FaceLandmarker not available!");
                    Log.e(TAG, "REQUIRED: Download face_landmarker.task from:");
                    Log.e(TAG, "https://storage.googleapis.com/mediapipe-models/face_landmarker/face_landmarker/float16/1/face_landmarker.task");
                    Log.e(TAG, "Then place it in: app/src/main/assets/face_landmarker.task");
                    Log.e(TAG, "Finally, clean and rebuild the project");
                    
                    runOnMainThread(() -> callback.onLandmarksExtracted(false));
                    return;
                }
                
                // Convert bitmap to MPImage for MediaPipe processing
                MPImage mpImage = new BitmapImageBuilder(faceBitmap).build();
                
                // Process image with MediaPipe face landmarker (REAL DATA)
                FaceLandmarkerResult result = faceLandmarker.detect(mpImage);
                
                if (result == null || result.faceLandmarks().isEmpty()) {
                    Log.w(TAG, "No faces detected in real MediaPipe processing");
                    runOnMainThread(() -> callback.onLandmarksExtracted(false));
                    return;
                }
                
                // Get the first detected face and convert to PointF (REAL LANDMARKS)
                List<NormalizedLandmark> normalizedLandmarks = result.faceLandmarks().get(0);
                List<PointF> landmarks = convertNormalizedLandmarksToPointF(normalizedLandmarks, faceBitmap.getWidth(), faceBitmap.getHeight());
                
                // Process real face landmarks
                processFaceLandmarks(landmarks, faceBitmap);
                
                // Extract real eye regions for gaze estimation
                extractEyeRegions(faceBitmap, landmarks);
                
                Log.d(TAG, "Real MediaPipe landmarks extracted successfully. Landmarks: " + landmarks.size());
                runOnMainThread(() -> callback.onLandmarksExtracted(true));
                
            } catch (Exception e) {
                Log.e(TAG, "Error extracting real MediaPipe landmarks", e);
                runOnMainThread(() -> callback.onLandmarksExtracted(false));
            }
        });
    }
    
    /**
     * Process real face landmarks from MediaPipe
     */
    private void processFaceLandmarks(List<PointF> landmarks, Bitmap faceBitmap) {
        if (landmarks.size() < 468) {
            Log.w(TAG, "Insufficient landmarks for processing. Expected: 468, Got: " + landmarks.size());
            return;
        }
        
        // Clear previous data
        leftEyePoints.clear();
        rightEyePoints.clear();
        faceLandmarks.clear();
        faceContours.clear();
        
        // Store all landmarks
        for (int i = 0; i < landmarks.size(); i++) {
            faceLandmarks.put(i, landmarks.get(i));
        }
        
        // Extract left eye landmarks (indices 33-46 for MediaPipe)
        for (int i = 33; i <= 46; i++) {
            if (i < landmarks.size()) {
                leftEyePoints.add(landmarks.get(i));
            }
        }
        
        // Extract right eye landmarks (indices 362-375 for MediaPipe)
        for (int i = 362; i <= 375; i++) {
            if (i < landmarks.size()) {
                rightEyePoints.add(landmarks.get(i));
            }
        }
        
        // Calculate head pose from real landmarks
        calculateHeadPose(landmarks);
        
        // Calculate eye open probabilities from real landmarks
        calculateEyeOpenProbabilities();
        
        Log.d(TAG, "Real face landmarks processed. Left eye points: " + leftEyePoints.size() + 
              ", Right eye points: " + rightEyePoints.size());
    }
    
    /**
     * Calculate head pose from landmarks
     */
    private void calculateHeadPose(List<PointF> landmarks) {
        // Simplified head pose calculation using key landmarks
        if (landmarks.size() >= 468) {
            // Use MediaPipe's 468-point face mesh for more accurate head pose
            // This is a simplified calculation - in practice, you'd use a more sophisticated algorithm
            
            // Calculate pitch (up/down) from nose and eyes
            if (landmarks.size() > 1 && landmarks.size() > 33 && landmarks.size() > 362) {
                PointF nose = landmarks.get(1);
                PointF leftEye = landmarks.get(33);
                PointF rightEye = landmarks.get(362);
                
                // Calculate pitch from nose position relative to eyes
                float eyeCenterY = (leftEye.y + rightEye.y) / 2.0f;
                float pitch = (nose.y - eyeCenterY) / 100.0f; // Normalize
                headEulerAngles[0] = Math.max(-30.0f, Math.min(30.0f, pitch * 30.0f));
            }
            
            // Calculate yaw (left/right) from face width
            if (landmarks.size() > 234 && landmarks.size() > 454) {
                PointF leftEar = landmarks.get(234);
                PointF rightEar = landmarks.get(454);
                float faceWidth = Math.abs(rightEar.x - leftEar.x);
                float yaw = (faceWidth - 200.0f) / 200.0f; // Normalize
                headEulerAngles[2] = Math.max(-45.0f, Math.min(45.0f, yaw * 45.0f));
            }
            
            // Calculate roll (tilt) from eye positions
            if (landmarks.size() > 33 && landmarks.size() > 362) {
                PointF leftEye = landmarks.get(33);
                PointF rightEye = landmarks.get(362);
                float roll = (rightEye.y - leftEye.y) / 100.0f; // Normalize
                headEulerAngles[1] = Math.max(-30.0f, Math.min(30.0f, roll * 30.0f));
            }
        }
    }
    
    /**
     * Calculate eye open probabilities using EAR (Eye Aspect Ratio)
     */
    private void calculateEyeOpenProbabilities() {
        float leftEAR = calculateEAR(leftEyePoints);
        float rightEAR = calculateEAR(rightEyePoints);
        
        // Convert EAR to probability (0.0 = closed, 1.0 = open)
        leftEyeOpenProbability = Math.max(0.0f, Math.min(1.0f, leftEAR / 0.3f));
        rightEyeOpenProbability = Math.max(0.0f, Math.min(1.0f, rightEAR / 0.3f));
    }
    
    /**
     * Calculate Eye Aspect Ratio (EAR)
     */
    private float calculateEAR(List<PointF> eyePoints) {
        if (eyePoints.size() < 6) {
            return 0.3f; // Default value
        }
        
        // EAR = (||p2-p6|| + ||p3-p5||) / (2 * ||p1-p4||)
        float A = distance(eyePoints.get(1), eyePoints.get(5));
        float B = distance(eyePoints.get(2), eyePoints.get(4));
        float C = distance(eyePoints.get(0), eyePoints.get(3));
        
        return (A + B) / (2.0f * C);
    }
    
    /**
     * Calculate distance between two points
     */
    private float distance(PointF p1, PointF p2) {
        float dx = p1.x - p2.x;
        float dy = p1.y - p2.y;
        return (float) Math.sqrt(dx * dx + dy * dy);
    }
    
    /**
     * Extract eye regions for gaze estimation
     */
    private void extractEyeRegions(Bitmap faceBitmap, List<PointF> landmarks) {
        try {
            // Get eye landmarks
            if (landmarks.size() < 375) {
                Log.w(TAG, "Insufficient landmarks for eye region extraction");
                return;
            }
            
            // Left eye region (indices 33-46)
            PointF leftEyeCenter = calculateEyeCenter(landmarks, 33, 46);
            PointF rightEyeCenter = calculateEyeCenter(landmarks, 362, 375);
            
            if (leftEyeCenter == null || rightEyeCenter == null) {
                Log.w(TAG, "Eye centers not found");
                return;
            }
            
            // Calculate eye regions with padding
            int eyeSize = (int) (faceBitmap.getWidth() * 0.2); // 20% of face width
            
            Rect leftEyeRect = new Rect(
                    (int) (leftEyeCenter.x - eyeSize/2),
                    (int) (leftEyeCenter.y - eyeSize/2),
                    (int) (leftEyeCenter.x + eyeSize/2),
                    (int) (leftEyeCenter.y + eyeSize/2)
            );
            
            Rect rightEyeRect = new Rect(
                    (int) (rightEyeCenter.x - eyeSize/2),
                    (int) (rightEyeCenter.y - eyeSize/2),
                    (int) (rightEyeCenter.x + eyeSize/2),
                    (int) (rightEyeCenter.y + eyeSize/2)
            );
            
            // Ensure eye regions are within face bitmap bounds
            leftEyeRect.left = Math.max(0, leftEyeRect.left);
            leftEyeRect.top = Math.max(0, leftEyeRect.top);
            leftEyeRect.right = Math.min(faceBitmap.getWidth(), leftEyeRect.right);
            leftEyeRect.bottom = Math.min(faceBitmap.getHeight(), leftEyeRect.bottom);
            
            rightEyeRect.left = Math.max(0, rightEyeRect.left);
            rightEyeRect.top = Math.max(0, rightEyeRect.top);
            rightEyeRect.right = Math.min(faceBitmap.getWidth(), rightEyeRect.right);
            rightEyeRect.bottom = Math.min(faceBitmap.getHeight(), rightEyeRect.bottom);
            
            // Extract eye regions
            if (leftEyeRect.width() > 0 && leftEyeRect.height() > 0) {
                lastLeftEyeRegion = Bitmap.createBitmap(
                        faceBitmap,
                        leftEyeRect.left,
                        leftEyeRect.top,
                        leftEyeRect.width(),
                        leftEyeRect.height()
                );
            }
            
            if (rightEyeRect.width() > 0 && rightEyeRect.height() > 0) {
                lastRightEyeRegion = Bitmap.createBitmap(
                        faceBitmap,
                        rightEyeRect.left,
                        rightEyeRect.top,
                        rightEyeRect.width(),
                        rightEyeRect.height()
                );
            }
            
            Log.d(TAG, "Eye regions extracted. Left eye: " + leftEyeRect + ", Right eye: " + rightEyeRect);
            
        } catch (Exception e) {
            Log.e(TAG, "Error extracting eye regions", e);
        }
    }
    
    /**
     * Calculate eye center from landmarks
     */
    private PointF calculateEyeCenter(List<PointF> landmarks, int startIndex, int endIndex) {
        if (landmarks.size() <= endIndex) {
            return null;
        }
        
        float sumX = 0, sumY = 0;
        int count = 0;
        
        for (int i = startIndex; i <= endIndex && i < landmarks.size(); i++) {
            PointF point = landmarks.get(i);
            sumX += point.x;
            sumY += point.y;
            count++;
        }
        
        if (count == 0) {
            return null;
        }
        
        return new PointF(sumX / count, sumY / count);
    }
    
    /**
     * Get the 6 key points for EAR calculation for left eye
     */
    public List<PointF> getLeftEyeEARPoints() {
        if (leftEyePoints.size() < 6) {
            return new ArrayList<>();
        }
        
        List<PointF> earPoints = new ArrayList<>();
        
        // Select 6 key points for EAR calculation
        if (leftEyePoints.size() >= 8) {
            // Left corner (0)
            earPoints.add(leftEyePoints.get(0));
            // Top left (1)
            earPoints.add(leftEyePoints.get(1));
            // Top right (2)
            earPoints.add(leftEyePoints.get(2));
            // Right corner (3)
            earPoints.add(leftEyePoints.get(3));
            // Bottom right (4)
            earPoints.add(leftEyePoints.get(5));
            // Bottom left (5)
            earPoints.add(leftEyePoints.get(7));
        } else {
            earPoints.addAll(leftEyePoints);
        }
        
        return earPoints;
    }
    
    /**
     * Get the 6 key points for EAR calculation for right eye
     */
    public List<PointF> getRightEyeEARPoints() {
        if (rightEyePoints.size() < 6) {
            return new ArrayList<>();
        }
        
        List<PointF> earPoints = new ArrayList<>();
        
        if (rightEyePoints.size() >= 8) {
            // Left corner (0)
            earPoints.add(rightEyePoints.get(0));
            // Top left (1)
            earPoints.add(rightEyePoints.get(1));
            // Top right (2)
            earPoints.add(rightEyePoints.get(2));
            // Right corner (3)
            earPoints.add(rightEyePoints.get(3));
            // Bottom right (4)
            earPoints.add(rightEyePoints.get(5));
            // Bottom left (5)
            earPoints.add(rightEyePoints.get(7));
        } else {
            earPoints.addAll(rightEyePoints);
        }
        
        return earPoints;
    }
    
    /**
     * Get the left eye open probability
     */
    public float getLeftEyeOpenProbability() {
        return leftEyeOpenProbability;
    }
    
    /**
     * Get the right eye open probability
     */
    public float getRightEyeOpenProbability() {
        return rightEyeOpenProbability;
    }
    
    /**
     * Get the head pose in Euler angles [pitch, roll, yaw]
     */
    public float[] getHeadEulerAngles() {
        return headEulerAngles;
    }
    
    /**
     * Get the last extracted left eye region
     */
    public Bitmap getLeftEyeRegion() {
        return lastLeftEyeRegion;
    }
    
    /**
     * Get the last extracted right eye region
     */
    public Bitmap getRightEyeRegion() {
        return lastRightEyeRegion;
    }
    
    /**
     * Check if eyes are closed based on probabilities
     */
    public boolean areEyesClosed() {
        return leftEyeOpenProbability < 0.3f && rightEyeOpenProbability < 0.3f;
    }
    
    /**
     * Check if all required landmarks are available for eye tracking
     */
    public boolean hasRequiredLandmarks() {
        return !leftEyePoints.isEmpty() && !rightEyePoints.isEmpty();
    }
    
    /**
     * Run a task on the main thread
     */
    private void runOnMainThread(Runnable runnable) {
        if (Looper.myLooper() == Looper.getMainLooper()) {
            runnable.run();
        } else {
            mainHandler.post(runnable);
        }
    }
    
    /**
     * Close and release resources
     */
    public void close() {
        try {
            // Set executor as inactive first
            isExecutorActive = false;
            
            if (faceLandmarker != null) {
                faceLandmarker.close();
            }
            
            if (executor != null && !executor.isShutdown()) {
                // Shutdown the executor gracefully
                executor.shutdown();
                
                // Wait for tasks to complete with timeout
                try {
                    if (!executor.awaitTermination(2, TimeUnit.SECONDS)) {
                        // Force shutdown if tasks don't complete in time
                        executor.shutdownNow();
                        if (!executor.awaitTermination(1, TimeUnit.SECONDS)) {
                            Log.w(TAG, "Executor did not terminate gracefully");
                        }
                    }
                } catch (InterruptedException e) {
                    // Force shutdown on interruption
                    executor.shutdownNow();
                    Thread.currentThread().interrupt();
                }
            }
            
            Log.d(TAG, "MediaPipe face landmark extractor closed");
        } catch (Exception e) {
            Log.e(TAG, "Error closing MediaPipe face landmark extractor", e);
        }
    }
} 