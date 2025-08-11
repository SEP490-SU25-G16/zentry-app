package vn.edu.fpt.zentryapp.faceid.data.service;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Rect;
import android.util.Log;

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
import vn.edu.fpt.zentryapp.faceid.utils.SharedExecutors;
import java.util.concurrent.TimeUnit;

/**
 * Utility class for face detection using MediaPipe
 * Adapted from the OnDevice-Face-Recognition-Android project
 */
public class FaceDetector {
    private static final String TAG = "FaceDetector";
    private static final String MODEL_FILE = "blaze_face_short_range.tflite";
    
    private final Context context;
    private com.google.mediapipe.tasks.vision.facedetector.FaceDetector detector;
    private final Executor executor = SharedExecutors.getMlExecutor();
    private volatile boolean isInitialized = false;
    private final CountDownLatch initLatch = new CountDownLatch(1);
    
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
                        .setMinDetectionConfidence(0.5f)
                        .setMinSuppressionThreshold(0.3f)
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
        List<FaceDetectionResult> results = new ArrayList<>();

        try {
            // Ensure detector is initialized
            if (!isInitialized) {
                Log.e(TAG, "Face detector not initialized yet");
                return results;
            }
            
            // Convert bitmap to MPImage
            MPImage image = new BitmapImageBuilder(bitmap).build();

            // Detect faces
            FaceDetectorResult detectionResult = detector.detect(image);

            Log.d(TAG, "Phát hiện " + detectionResult.detections().size() + " khuôn mặt");

            // Process detection results
            for (Detection detection : detectionResult.detections()) {
                // Lấy bounding box từ detection
                android.graphics.RectF rectF = detection.boundingBox();

                Log.d(TAG, "Bounding box raw: " + rectF.toString());
                Log.d(TAG, "Bitmap size: " + bitmap.getWidth() + "x" + bitmap.getHeight());

                // Chuyển đổi từ tỷ lệ sang pixel - FIX HERE
                // Chú ý: rectF chứa tọa độ thực tế, không phải tỷ lệ
                Rect boundingBox = new Rect(
                        (int) rectF.left,
                        (int) rectF.top,
                        (int) rectF.right,
                        (int) rectF.bottom
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
    
    /**
     * Release resources
     */
    public void close() {
        if (detector != null) {
            detector.close();
        }
    }
} 