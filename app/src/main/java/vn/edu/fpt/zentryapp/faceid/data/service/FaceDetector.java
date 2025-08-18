package vn.edu.fpt.zentryapp.faceid.data.service;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Rect;
import android.util.Log;

import vn.edu.fpt.zentryapp.BuildConfig;
import com.google.mediapipe.tasks.vision.facedetector.FaceDetectorResult;
import com.google.mediapipe.tasks.vision.facedetector.FaceDetector.FaceDetectorOptions;
import com.google.mediapipe.tasks.vision.core.RunningMode;
import com.google.mediapipe.framework.image.BitmapImageBuilder;
import com.google.mediapipe.framework.image.MPImage;
import com.google.mediapipe.tasks.components.containers.Detection;
import com.google.mediapipe.tasks.core.BaseOptions;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

/**
 * Utility class for face detection using MediaPipe
 * Adapted from the OnDevice-Face-Recognition-Android project
 */
public class FaceDetector {
    private static final String TAG = "FaceDetector";
    private static final String MODEL_FILE = "blaze_face_short_range.tflite";
    // Target minimum dimension (shorter side) for stable detector input across devices
    private static final int DETECT_TARGET_MIN_DIMENSION = 640;
    private static final int DETECT_MIN_ALLOWED_DIMENSION = 320;
    
    private final Context context;
    private com.google.mediapipe.tasks.vision.facedetector.FaceDetector detector;
    private final Executor executor = Executors.newSingleThreadExecutor();
    private volatile boolean isInitialized = false;
    private final CountDownLatch initLatch = new CountDownLatch(1);
    private volatile float minDetectionConfidence = 0.5f;
    private volatile float minSuppressionThreshold = 0.3f;
    
    public static class FaceDetectionResult {
        private final Bitmap croppedBitmap;
        private final Rect boundingBox;
        
        public FaceDetectionResult(Bitmap croppedBitmap, Rect boundingBox) {
            this.croppedBitmap = croppedBitmap;
            this.boundingBox = boundingBox;
        }
        
        public Bitmap getCroppedBitmap() {
            return croppedBitmap;
        }
        
        public Rect getBoundingBox() {
            return boundingBox;
        }
    }
    
    public FaceDetector(Context context) {
        this.context = context.getApplicationContext();
        
        // Khởi tạo model bất đồng bộ
        executor.execute(() -> {
            try {
                // Initialize MediaPipe face detector
                FaceDetectorOptions options = FaceDetectorOptions.builder()
                        .setBaseOptions(
                                BaseOptions.builder()
                                        .setModelAssetPath(MODEL_FILE)
                                        .build())
                        .setRunningMode(RunningMode.IMAGE)
                        .setMinDetectionConfidence(minDetectionConfidence)
                        .setMinSuppressionThreshold(minSuppressionThreshold)
                        .build();
                detector = com.google.mediapipe.tasks.vision.facedetector.FaceDetector.createFromOptions(context, options);
                
                isInitialized = true;
                Log.d(TAG, "Face detector initialized successfully");
            } catch (Exception e) {
                Log.e(TAG, "Error initializing face detector", e);
            } finally {
                initLatch.countDown();
            }
        });
    }

    public void setMinDetectionConfidence(float confidence) {
        this.minDetectionConfidence = confidence;
        Log.d(TAG, "minDetectionConfidence set to " + confidence);
    }

    public void setMinSuppressionThreshold(float threshold) {
        this.minSuppressionThreshold = threshold;
        Log.d(TAG, "minSuppressionThreshold set to " + threshold);
    }
    
    public boolean isInitialized() {
        return isInitialized;
    }
    
    public void awaitInitialization(long timeoutMs) throws InterruptedException {
        initLatch.await(timeoutMs, TimeUnit.MILLISECONDS);
    }
    
    /**
     * Detect faces in the given bitmap
     * @param bitmap Input bitmap
     * @return List of face detection results
     */
    public List<FaceDetectionResult> detectFaces(Bitmap bitmap) {
        if (BuildConfig.DEBUG) {
            Log.d("DEBUG_FACE_DETECTOR", "Input Bitmap - Size: " + bitmap.getWidth() + "x" + bitmap.getHeight() +
                    ", Config: " + bitmap.getConfig() + ", HasAlpha: " + bitmap.hasAlpha() +
                    ", IsMutable: " + bitmap.isMutable());

            if (bitmap.getWidth() < 100 || bitmap.getHeight() < 100) {
                Log.w("DEBUG_FACE_DETECTOR", "Bitmap too small for face detection");
            }

            try {
                int pixel = bitmap.getPixel(0, 0);
                Log.d("DEBUG_FACE_DETECTOR", "First pixel sample: " + pixel);
            } catch (Exception e) {
                Log.e("DEBUG_FACE_DETECTOR", "Bitmap corrupted or invalid", e);
            }
        }

        List<FaceDetectionResult> results = new ArrayList<>();

        try {
            // Ensure detector is initialized
            if (!isInitialized) {
                Log.e(TAG, "Face detector not initialized yet");
                return results;
            }
            
            // Prepare working bitmap with optional downscale to stabilize detector input across devices
            final int origW = bitmap.getWidth();
            final int origH = bitmap.getHeight();
            final int minDim = Math.min(origW, origH);
            float scaleForDetection = 1.0f;
            Bitmap detectBitmap = bitmap;
            if (minDim > DETECT_TARGET_MIN_DIMENSION) {
                scaleForDetection = (float) DETECT_TARGET_MIN_DIMENSION / (float) minDim;
                int newW = Math.max(1, Math.round(origW * scaleForDetection));
                int newH = Math.max(1, Math.round(origH * scaleForDetection));
                try {
                    detectBitmap = Bitmap.createScaledBitmap(bitmap, newW, newH, true);
                    if (BuildConfig.DEBUG) {
                        Log.d("DEBUG_FACE_DETECTOR", "Downscaled for detection to: " + newW + "x" + newH +
                                " (scale=" + scaleForDetection + ")");
                    }
                } catch (Exception e) {
                    Log.w(TAG, "Failed to create downscaled bitmap for detection, using original", e);
                    detectBitmap = bitmap;
                    scaleForDetection = 1.0f;
                }
            }

            // First attempt detection
            List<Detection> detections = runDetection(detectBitmap);
            if (BuildConfig.DEBUG) {
                Log.d(TAG, "Phát hiện " + detections.size() + " khuôn mặt (attempt#1)");
            }

            // Simple pyramid retry: try a slight downscale if nothing found and input was small or unchanged
            Bitmap retryBitmap = null;
            float retryScale = 1.0f;
            if (detections.isEmpty()) {
                int detectMinDim = Math.min(detectBitmap.getWidth(), detectBitmap.getHeight());
                int targetRetryMin = Math.max(DETECT_MIN_ALLOWED_DIMENSION, Math.round(detectMinDim * 0.75f));
                if (detectMinDim > targetRetryMin + 4) {
                    float factor = (float) targetRetryMin / (float) detectMinDim;
                    int retryW = Math.max(1, Math.round(detectBitmap.getWidth() * factor));
                    int retryH = Math.max(1, Math.round(detectBitmap.getHeight() * factor));
                    try {
                        retryBitmap = Bitmap.createScaledBitmap(detectBitmap, retryW, retryH, true);
                        retryScale = scaleForDetection * factor;
                        if (BuildConfig.DEBUG) {
                            Log.d("DEBUG_FACE_DETECTOR", "Retry downscale for detection: " + retryW + "x" + retryH +
                                    " (retryScale=" + retryScale + ")");
                        }
                        detections = runDetection(retryBitmap);
                        if (BuildConfig.DEBUG) {
                            Log.d(TAG, "Phát hiện " + detections.size() + " khuôn mặt (attempt#2)");
                        }
                    } catch (Exception e) {
                        Log.w(TAG, "Retry downscale failed", e);
                    }
                }
            }

            // Process detection results
            final float inverseScale = (retryBitmap != null && !detections.isEmpty()) ? (1.0f / retryScale)
                    : (1.0f / scaleForDetection);
            for (Detection detection : detections) {
                // Lấy bounding box từ detection
                android.graphics.RectF rectF = detection.boundingBox();

                Log.d(TAG, "Bounding box raw: " + rectF.toString());
                Log.d(TAG, "Bitmap size: " + bitmap.getWidth() + "x" + bitmap.getHeight());

                // Map bounding box from detection bitmap scale back to original bitmap coordinates
                Rect boundingBox = new Rect(
                        (int) Math.round(rectF.left * inverseScale),
                        (int) Math.round(rectF.top * inverseScale),
                        (int) Math.round(rectF.right * inverseScale),
                        (int) Math.round(rectF.bottom * inverseScale)
                );

                Log.d(TAG, "Bounding box converted: " + boundingBox.toString());

                // Ensure bounding box is within image bounds
                boundingBox.left = Math.max(0, boundingBox.left);
                boundingBox.top = Math.max(0, boundingBox.top);
                boundingBox.right = Math.min(bitmap.getWidth(), boundingBox.right);
                boundingBox.bottom = Math.min(bitmap.getHeight(), boundingBox.bottom);

                Log.d(TAG, "Bounding box adjusted: " + boundingBox.toString());

                // Skip invalid bounding boxes
                if (boundingBox.width() <= 0 || boundingBox.height() <= 0) {
                    Log.e(TAG, "Invalid bounding box: " + boundingBox.toString());
                    continue;
                }

                try {
                    // Crop face from bitmap
                    Bitmap croppedBitmap = Bitmap.createBitmap(
                            bitmap,
                            boundingBox.left,
                            boundingBox.top,
                            boundingBox.width(),
                            boundingBox.height()
                    );

                    Log.d(TAG, "Cropped bitmap size: " + croppedBitmap.getWidth() + "x" + croppedBitmap.getHeight());

                    // Add to results
                    results.add(new FaceDetectionResult(croppedBitmap, boundingBox));
                } catch (Exception e) {
                    Log.e(TAG, "Error cropping face: " + e.getMessage(), e);
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "Error detecting faces", e);
        }

        Log.d(TAG, "Returning " + results.size() + " face results");
        return results;
    }

    private List<Detection> runDetection(Bitmap input) {
        try {
            MPImage image = new BitmapImageBuilder(input).build();
            FaceDetectorResult detectionResult = detector.detect(image);
            return detectionResult.detections();
        } catch (Exception e) {
            Log.e(TAG, "Detector.detect failed", e);
            return new ArrayList<>();
        }
    }
    
    /**
     * Release resources
     */
    public void close() {
        if (detector != null) {
            detector.close();
        }
    }
} 