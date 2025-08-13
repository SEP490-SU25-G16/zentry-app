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
    private final Executor executor = Executors.newSingleThreadExecutor();
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
            int retries = 0;
            final int maxRetries = 3;

            while (!isInitialized && retries < maxRetries) {
                try {
                    if (retries > 0) {
                        Log.d(TAG, "Retry attempt " + retries + " for face detector initialization");
                        // Add a delay before retrying
                        Thread.sleep(500);
                    }

                    // Initialize MediaPipe face detector with more sensitive settings
                    float confidenceThreshold = vn.edu.fpt.zentryapp.faceid.ui.components.DeviceSpecificOptimizer.getOptimalDetectionConfidence();

                    FaceDetectorOptions options = FaceDetectorOptions.builder()
                            .setBaseOptions(
                                    BaseOptions.builder()
                                            .setModelAssetPath(MODEL_FILE)
                                            .build())
                            .setRunningMode(RunningMode.IMAGE)
                            .setMinDetectionConfidence(confidenceThreshold)  // Use device-specific confidence threshold
                            .setMinSuppressionThreshold(0.3f)
                            .build();

                    Log.d(TAG, "Initializing face detector with MODEL_FILE=" + MODEL_FILE +
                            ", confidence=" + confidenceThreshold +
                            ", on device " + android.os.Build.MANUFACTURER + " " +
                            android.os.Build.MODEL);
                    detector = com.google.mediapipe.tasks.vision.facedetector.FaceDetector.createFromOptions(context, options);

                    isInitialized = true;
                    Log.d(TAG, "Face detector initialized successfully");
                    break;
                } catch (Exception e) {
                    Log.e(TAG, "Error initializing face detector (attempt " + (retries + 1) + ")", e);
                    retries++;
                    if (retries >= maxRetries) {
                        Log.e(TAG, "Failed to initialize face detector after " + maxRetries + " attempts");
                    }
                }
            }

            initLatch.countDown();
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
     *
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

            if (bitmap == null) {
                Log.e(TAG, "Input bitmap is null");
                return results;
            }

            // Log bitmap info for debugging
            Log.d(TAG, "Input bitmap: " + bitmap.getWidth() + "x" + bitmap.getHeight() +
                    ", config=" + bitmap.getConfig() +
                    ", hasAlpha=" + bitmap.hasAlpha());

            // Check if bitmap is valid
            if (bitmap.getWidth() <= 0 || bitmap.getHeight() <= 0) {
                Log.e(TAG, "Invalid bitmap dimensions: " + bitmap.getWidth() + "x" + bitmap.getHeight());
                return results;
            }

			// Convert bitmap to MPImage
			MPImage image = new BitmapImageBuilder(bitmap).build();

			// Detect faces
			FaceDetectorResult detectionResult = detector.detect(image);

			// Log device info for troubleshooting
			Log.d(TAG, "Face detection on device: " +
					vn.edu.fpt.zentryapp.faceid.ui.components.DeviceSpecificOptimizer.getDeviceInfoString());

			int num = detectionResult == null ? 0 : detectionResult.detections().size();
			Log.d(TAG, "Phát hiện " + num + " khuôn mặt");
			if (num == 0) {
				Log.d(TAG, "No faces detected. Possible reasons: lighting conditions, face angle, or distance from camera");
				return results;
			}

			for (int i = 0; i < num; i++) {
				Detection det = detectionResult.detections().get(i);
				if (det.categories().size() > 0) {
					Log.d(TAG, "Face #" + i + " confidence: " + det.categories().get(0).score());
				}

				android.graphics.RectF rectF = det.boundingBox();
				Log.d(TAG, "Bounding box raw: " + rectF);
				Log.d(TAG, "Bitmap size: " + bitmap.getWidth() + "x" + bitmap.getHeight());

				Rect boundingBox = new Rect(
						(int) rectF.left,
						(int) rectF.top,
						(int) rectF.right,
						(int) rectF.bottom
				);

				// Clamp to image bounds
				boundingBox.left = Math.max(0, boundingBox.left);
				boundingBox.top = Math.max(0, boundingBox.top);
				boundingBox.right = Math.min(bitmap.getWidth(), boundingBox.right);
				boundingBox.bottom = Math.min(bitmap.getHeight(), boundingBox.bottom);

				if (boundingBox.width() <= 0 || boundingBox.height() <= 0) {
					Log.w(TAG, "Skipping invalid bbox: " + boundingBox);
					continue;
				}

				try {
					Bitmap croppedBitmap = Bitmap.createBitmap(
							bitmap,
							boundingBox.left,
							boundingBox.top,
							boundingBox.width(),
							boundingBox.height()
					);
					results.add(new FaceDetectionResult(croppedBitmap, boundingBox));
				} catch (Exception e) {
					Log.e(TAG, "Error cropping face: " + e.getMessage(), e);
				}
			}
			Log.d(TAG, "Returning " + results.size() + " face results");
			return results;
    } catch (Exception e) {
            throw new RuntimeException(e);
        }

    }

}

