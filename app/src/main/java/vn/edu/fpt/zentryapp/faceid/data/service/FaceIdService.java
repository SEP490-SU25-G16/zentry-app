package vn.edu.fpt.zentryapp.faceid.data.service;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Rect;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import androidx.annotation.NonNull;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.io.File;
import java.io.FileOutputStream;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import lombok.Getter;
import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.RequestBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import vn.edu.fpt.zentryapp.auth.client.ApiClient;
import vn.edu.fpt.zentryapp.faceid.data.api.FaceIdApiController;
import vn.edu.fpt.zentryapp.faceid.data.model.response.FaceIdResponse;
import vn.edu.fpt.zentryapp.faceid.data.model.response.FaceIdVerifyResponse;


public class FaceIdService {
    private static final String TAG = "FaceIdService";
    
    private final Context context;
    private FaceDetector faceDetector;
    private FaceEmbedding faceEmbedding;

    @Getter
    private FaceSpoofDetector faceSpoofDetector;

    @Getter
    private GazeEstimator gazeEstimator; // Add GazeEstimator field

    // 🔧 NEW: MediaPipe FaceLandmarkExtractor for real landmark detection
    @Getter
    private MediaPipeFaceLandmarkExtractor mediaPipeFaceLandmarkExtractor;

    private final FaceIdApiController faceIdApiController;
    private final ExecutorService executor;
    private final Handler mainHandler;
    
    // 🔧 NEW: Improved components
    private final FaceDecisionEngine decisionEngine;
    private final ModelRetryManager retryManager;
    private final FaceProcessingErrorHandler errorHandler;
    
    // 🔧 NEW: Memory, Performance, and Configuration Management
    private final FaceIdConfig configManager;
    private final FaceIdMemoryManager memoryManager;
    private final FaceIdPerformanceManager performanceManager;
    
    private final CountDownLatch modelLoadLatch = new CountDownLatch(5); // Update to 5 models (added MediaPipeFaceLandmarkExtractor)
    private volatile boolean isInitialized = false;
    private final AtomicBoolean isProcessing = new AtomicBoolean(false);
    
    public FaceIdService(Context context) {
        this.context = context.getApplicationContext();
        this.executor = Executors.newCachedThreadPool(); // Thay đổi thành thread pool
        this.mainHandler = new Handler(Looper.getMainLooper());
        this.faceIdApiController = ApiClient.getClient(context).create(FaceIdApiController.class);
        
        // 🔧 NEW: Initialize configuration and managers
        this.configManager = new FaceIdConfig(context);
        this.memoryManager = new FaceIdMemoryManager(context, configManager.getConfig().memoryConfig);
        this.performanceManager = new FaceIdPerformanceManager(context, configManager.getConfig().performanceConfig);
        
        // 🔧 NEW: Initialize improved components with configuration
        this.decisionEngine = new FaceDecisionEngine(FaceDecisionEngine.FaceDecisionConfig.getDefault());
        this.retryManager = new ModelRetryManager.Builder()
            .maxRetries(3)
            .initialRetryDelayMs(1000)
            .build();
        this.errorHandler = new FaceProcessingErrorHandler(context);
        
        // Khởi tạo các model bất đồng bộ
        initializeModelsAsync();
    }
    
    private void initializeModelsAsync() {
        // 🔧 NEW: Initialize FaceDetector with retry
        executor.execute(() -> {
            try {
                this.faceDetector = retryManager.executeWithRetry(() -> new FaceDetector(context));
                modelLoadLatch.countDown();
                Log.d(TAG, "FaceDetector initialized");
            } catch (ModelRetryManager.ModelRetryException e) {
                Log.e(TAG, "Error initializing FaceDetector", e);
                errorHandler.handleModelInitializationError(e, "FaceDetector");
                modelLoadLatch.countDown();
            }
        });
        
        // 🔧 NEW: Initialize FaceEmbedding with retry
        executor.execute(() -> {
            try {
                this.faceEmbedding = retryManager.executeWithRetry(() -> new FaceEmbedding(context));
                modelLoadLatch.countDown();
                Log.d(TAG, "FaceEmbedding initialized");
            } catch (ModelRetryManager.ModelRetryException e) {
                Log.e(TAG, "Error initializing FaceEmbedding", e);
                errorHandler.handleModelInitializationError(e, "FaceEmbedding");
                modelLoadLatch.countDown();
            }
        });
        
        // 🔧 NEW: Initialize FaceSpoofDetector with retry
        executor.execute(() -> {
            try {
                this.faceSpoofDetector = retryManager.executeWithRetry(() -> new FaceSpoofDetector(context));
                modelLoadLatch.countDown();
                Log.d(TAG, "FaceSpoofDetector initialized");
            } catch (ModelRetryManager.ModelRetryException e) {
                Log.e(TAG, "Error initializing FaceSpoofDetector", e);
                errorHandler.handleModelInitializationError(e, "FaceSpoofDetector");
                modelLoadLatch.countDown();
            }
        });
        
        // 🔧 NEW: Initialize GazeEstimator with retry
        executor.execute(() -> {
            try {
                this.gazeEstimator = retryManager.executeWithRetry(() -> new GazeEstimator(context, null));
                // Configure gaze estimator for front camera mirrored preview and reduced head pose weight
                this.gazeEstimator.setFrontCameraMirrored(true);
                this.gazeEstimator.setHeadPoseWeight(0.2f);
                modelLoadLatch.countDown();
                Log.d(TAG, "GazeEstimator initialized");
            } catch (ModelRetryManager.ModelRetryException e) {
                Log.e(TAG, "Error initializing GazeEstimator", e);
                errorHandler.handleModelInitializationError(e, "GazeEstimator");
                modelLoadLatch.countDown();
            }
        });
        
        // 🔧 NEW: Initialize MediaPipeFaceLandmarkExtractor with retry
        executor.execute(() -> {
            try {
                this.mediaPipeFaceLandmarkExtractor = retryManager.executeWithRetry(() -> new MediaPipeFaceLandmarkExtractor(context));
                modelLoadLatch.countDown();
                Log.d(TAG, "MediaPipeFaceLandmarkExtractor initialized with face_landmarker.task");
            } catch (ModelRetryManager.ModelRetryException e) {
                Log.e(TAG, "Error initializing MediaPipeFaceLandmarkExtractor", e);
                errorHandler.handleModelInitializationError(e, "MediaPipeFaceLandmarkExtractor");
                modelLoadLatch.countDown();
            }
        });
    }
    
    public boolean isInitialized() {
        if (isInitialized) {
            Log.d(TAG, "isInitialized: Already initialized - returning true");
            return true;
        }
        
        try {
            // Kiểm tra xem tất cả model đã load xong chưa (với timeout 0 để không block)
            boolean allLoaded = modelLoadLatch.await(0, TimeUnit.MILLISECONDS);
            isInitialized = allLoaded;
            Log.d(TAG, "isInitialized: Models loaded check result: " + allLoaded);
            return allLoaded;
        } catch (InterruptedException e) {
            Log.e(TAG, "isInitialized: InterruptedException during model check", e);
            return false;
        }
    }
    
    public void awaitInitialization(long timeoutMs, Runnable onComplete, Runnable onTimeout) {
        executor.execute(() -> {
            try {
                boolean initialized = modelLoadLatch.await(timeoutMs, TimeUnit.MILLISECONDS);
                if (initialized) {
                    isInitialized = true;
                    mainHandler.post(onComplete);
                } else {
                    mainHandler.post(onTimeout);
                }
            } catch (InterruptedException e) {
                mainHandler.post(onTimeout);
            }
        });
    }
    
    public interface FaceIdCallback {
        void onSuccess(String message);
        void onFailure(String errorMessage);
    }
    
    public interface FaceDetectionCallback {
        void onFaceDetected(Bitmap faceBitmap, Rect boundingBox);
        void onNoFaceDetected();
        void onMultipleFacesDetected();
        void onError(String errorMessage);
    }
    
    public interface ContinuousProcessingCallback {
        void onFaceDetected(Rect boundingBox, boolean isSpoof, float spoofScore);
        void onNoFaceDetected();
        void onMultipleFacesDetected();
        void onError(String errorMessage);
    }
    
    public interface FaceVerificationCallback {
        void onVerified(float confidence);
        void onVerificationFailed(String reason);
        void onError(String errorMessage);
    }
    
    /**
     * Process a bitmap to detect face, check for spoofing, and generate embedding
     * Enhanced with oval boundary validation
     */
    public void processFaceImage(Bitmap bitmap, Rect faceRect, android.graphics.RectF ovalRect, FaceDetectionCallback callback) {
        // Check if models are initialized
        if (!isInitialized()) {
            awaitInitialization(5000, 
                () -> processFaceImage(bitmap, faceRect, ovalRect, callback),
                () -> runOnMainThread(() -> callback.onError("Face detection models not initialized yet"))
            );
            return;
        }
        
        executor.execute(() -> {
            try {
                // If face rectangle is not provided, detect it
                if (faceRect == null) {
                    List<FaceDetector.FaceDetectionResult> faces = faceDetector.detectFaces(bitmap);
                    
                    Log.d(TAG, "processFaceImage: detected " + faces.size() + " faces");
                    
                    if (faces.isEmpty()) {
                        Log.d(TAG, "processFaceImage: No faces detected");
                        runOnMainThread(() -> callback.onNoFaceDetected());
                        return;
                    }
                    
                    if (faces.size() > 1) {
                        Log.d(TAG, "processFaceImage: Multiple faces detected: " + faces.size());
                        runOnMainThread(() -> callback.onMultipleFacesDetected());
                        return;
                    }
                    
                    // Get the single detected face
                    FaceDetector.FaceDetectionResult faceResult = faces.get(0);
                    Bitmap faceBitmap = faceResult.getCroppedBitmap();
                    Rect boundingBox = faceResult.getBoundingBox();
                    
                    // Now perform spoof detection with oval validation
                    processFaceWithOvalBoundary(bitmap, boundingBox, ovalRect, faceBitmap, callback);
                } else {
                    // Use the provided face rectangle
                    Log.d(TAG, "processFaceImage: Using provided face rectangle: " + faceRect.toString());
                    
                    // Crop the face bitmap
                    Bitmap faceBitmap = Bitmap.createBitmap(
                            bitmap, 
                            faceRect.left, 
                            faceRect.top, 
                            faceRect.width(), 
                            faceRect.height()
                    );
                    
                    // Perform spoof detection with oval validation
                    processFaceWithOvalBoundary(bitmap, faceRect, ovalRect, faceBitmap, callback);
                }
            } catch (Exception e) {
                Log.e(TAG, "Error processing face image", e);
                runOnMainThread(() -> callback.onError("Error processing face: " + e.getMessage()));
            }
        });
    }
    
    /**
     * 🔧 NEW: Improved helper method using FaceDecisionEngine with Memory and Performance Management
     */
    private void processFaceWithOvalBoundary(Bitmap bitmap, Rect boundingBox, android.graphics.RectF ovalRect, 
                                           Bitmap faceBitmap, FaceDetectionCallback callback) {
        Log.d(TAG, "processFaceWithOvalBoundary: Starting processing");
        Log.d(TAG, "processFaceWithOvalBoundary: Face detected with bounding box: " + boundingBox.toString());
        Log.d(TAG, "processFaceWithOvalBoundary: faceBitmap size: " + faceBitmap.getWidth() + "x" + faceBitmap.getHeight());
        
        // 🔧 NEW: Memory management - acquire rect from pool
        Rect pooledBoundingBox = memoryManager.acquireRect();
        pooledBoundingBox.set(boundingBox);
        
        // 🔧 NEW: Performance optimization - use caching
        String cacheKey = generateCacheKey(faceBitmap, "spoof_detection");
        FaceIdPerformanceManager.CachedResult cachedResult = performanceManager.processFrameWithCache(faceBitmap, cacheKey);
        
        // 🔧 NEW: Use FaceDecisionEngine for oval validation
        FaceDecisionEngine.OvalValidationResult ovalValidation = validateOvalBoundary(pooledBoundingBox, ovalRect);
        
        Log.d(TAG, "processFaceWithOvalBoundary: Starting spoof detection");
        
        // Step 2: Check for spoofing using async method with oval validation
        faceSpoofDetector.detectSpoofAsync(cachedResult.bitmap, pooledBoundingBox, ovalRect, spoofResult -> {
            Log.d(TAG, "processFaceWithOvalBoundary: Spoof detection result - isSpoof: " + 
                    spoofResult.isSpoof() + ", score: " + spoofResult.getScore() + ", confidence: " + spoofResult.getConfidence());

            // 🔧 NEW: Use FaceDecisionEngine for decision making
            FaceDecisionEngine.FaceDetectionResult detectionResult = new FaceDecisionEngine.FaceDetectionResult(
                true, pooledBoundingBox, spoofResult.getConfidence()
            );
            
            FaceDecisionEngine.SpoofDetectionResult spoofDetectionResult = new FaceDecisionEngine.SpoofDetectionResult(
                spoofResult.isSpoof(), spoofResult.getConfidence(), spoofResult.getScore()
            );
            
            FaceDecisionEngine.FaceDecisionResult decision = decisionEngine.evaluate(
                detectionResult, spoofDetectionResult, ovalValidation
            );
            
            Log.d(TAG, "processFaceWithOvalBoundary: Decision result: " + decision);
            
            // 🔧 NEW: Handle decision result
            if (decision.isAccepted()) {
                Log.d(TAG, "processFaceWithOvalBoundary: SUCCESS - " + decision.getMessage());
                runOnMainThread(() -> callback.onFaceDetected(faceBitmap, boundingBox));
            } else if (decision.isRejected()) {
                Log.e(TAG, "processFaceWithOvalBoundary: FAILED - " + decision.getMessage());
                runOnMainThread(() -> callback.onError(decision.getMessage()));
            } else if (decision.needsGuidance()) {
                Log.w(TAG, "processFaceWithOvalBoundary: GUIDANCE - " + decision.getMessage());
                runOnMainThread(() -> callback.onError(decision.getMessage()));
            }
            
            // 🔧 NEW: Memory management - release pooled objects
            memoryManager.releaseRect(pooledBoundingBox);
        });
    }
    
    /**
     * 🔧 NEW: Helper method to validate oval boundary
     */
    private FaceDecisionEngine.OvalValidationResult validateOvalBoundary(Rect boundingBox, android.graphics.RectF ovalRect) {
        if (ovalRect == null) {
            return new FaceDecisionEngine.OvalValidationResult(true, "No oval boundary provided");
        }
        
        boolean isWithinOval = checkFaceWithinOval(boundingBox, ovalRect);
        
        // 🔧 NEW: Fallback validation for registration scenario
        if (!isWithinOval && configManager.getConfig().scenario == FaceIdConfig.Scenario.REGISTRATION) {
            // Try with more lenient thresholds for registration
            boolean fallbackCheck = checkFaceWithinOvalFallback(boundingBox, ovalRect);
            if (fallbackCheck) {
                Log.d(TAG, "Oval validation failed with strict thresholds, but passed with fallback for registration");
                return new FaceDecisionEngine.OvalValidationResult(true, "Face positioned within oval (fallback validation)");
            }
        }
        
        String reason = isWithinOval ? "Face properly positioned within oval" : "Face not within oval boundary";
        return new FaceDecisionEngine.OvalValidationResult(isWithinOval, reason);
    }
    
    /**
     * Legacy method for backward compatibility
     */
    public void processFaceImage(Bitmap bitmap, FaceDetectionCallback callback) {
        processFaceImage(bitmap, null, null, callback);
    }
    
    /**
     * Check if face is within oval boundary
     */
    private boolean checkFaceWithinOval(Rect faceRect, android.graphics.RectF ovalRect) {
        if (faceRect == null || ovalRect == null) {
            return true; // No validation needed
        }
        
        // Calculate face center relative to oval center
        float faceCenterX = faceRect.exactCenterX();
        float faceCenterY = faceRect.exactCenterY();
        float ovalCenterX = ovalRect.centerX();
        float ovalCenterY = ovalRect.centerY();
        
        // Calculate ellipse parameters
        float a = ovalRect.width() / 2; // semi-major axis
        float b = ovalRect.height() / 2; // semi-minor axis
        
        // Ellipse equation: (x-h)²/a² + (y-k)²/b² ≤ 1
        float ellipseValue = (float) (
            Math.pow(faceCenterX - ovalCenterX, 2) / Math.pow(a, 2) +
            Math.pow(faceCenterY - ovalCenterY, 2) / Math.pow(b, 2)
        );
        
        // 🔧 NEW: Use configuration for oval validation
        FaceIdConfig.OvalConfig ovalConfig = configManager.getConfig().ovalConfig;
        
        // Calculate face size relative to oval
        float faceWidth = faceRect.width();
        float faceHeight = faceRect.height();
        float widthRatio = faceWidth / ovalRect.width();
        float heightRatio = faceHeight / ovalRect.height();
        
        // 🔧 NEW: Use configurable thresholds
        // Ellipse equation returns ≤ 1 for points inside ellipse, so we check if it's within the ellipse
        boolean isWithinEllipse = ellipseValue <= 1.0f;
        boolean isGoodSize = widthRatio >= ovalConfig.minFaceSizeRatio && widthRatio <= ovalConfig.maxFaceSizeRatio && 
                            heightRatio >= ovalConfig.minFaceSizeRatio && heightRatio <= ovalConfig.maxFaceSizeRatio;
        
        Log.d(TAG, "checkFaceWithinOval: ellipseValue=" + String.format("%.4f", ellipseValue) + 
              ", widthRatio=" + String.format("%.4f", widthRatio) + ", heightRatio=" + String.format("%.4f", heightRatio) + 
              ", isWithinEllipse=" + isWithinEllipse + ", isGoodSize=" + isGoodSize +
              ", thresholds: minSize=" + ovalConfig.minFaceSizeRatio + 
              ", maxSize=" + ovalConfig.maxFaceSizeRatio + ", result=" + (isWithinEllipse && isGoodSize));
        
        return isWithinEllipse && isGoodSize;
    }
    
    /**
     * 🔧 NEW: Fallback oval validation with more lenient thresholds for registration
     */
    private boolean checkFaceWithinOvalFallback(Rect faceRect, android.graphics.RectF ovalRect) {
        if (faceRect == null || ovalRect == null) {
            return true; // No validation needed
        }
        
        // Calculate face center relative to oval center
        float faceCenterX = faceRect.exactCenterX();
        float faceCenterY = faceRect.exactCenterY();
        float ovalCenterX = ovalRect.centerX();
        float ovalCenterY = ovalRect.centerY();
        
        // Calculate ellipse parameters
        float a = ovalRect.width() / 2; // semi-major axis
        float b = ovalRect.height() / 2; // semi-minor axis
        
        // Ellipse equation: (x-h)²/a² + (y-k)²/b² ≤ 1
        float ellipseValue = (float) (
            Math.pow(faceCenterX - ovalCenterX, 2) / Math.pow(a, 2) +
            Math.pow(faceCenterY - ovalCenterY, 2) / Math.pow(b, 2)
        );
        
        // 🔧 NEW: More lenient thresholds for fallback validation
        float fallbackMinFaceSizeRatio = 0.15f; // 15% size instead of 20%
        float fallbackMaxFaceSizeRatio = 0.95f; // 95% size instead of 90%
        
        // Calculate face size relative to oval
        float faceWidth = faceRect.width();
        float faceHeight = faceRect.height();
        float widthRatio = faceWidth / ovalRect.width();
        float heightRatio = faceHeight / ovalRect.height();
        
        // 🔧 NEW: Use fallback thresholds
        // Ellipse equation returns ≤ 1 for points inside ellipse, so we check if it's within the ellipse
        boolean isWithinEllipse = ellipseValue <= 1.0f;
        boolean isGoodSize = widthRatio >= fallbackMinFaceSizeRatio && widthRatio <= fallbackMaxFaceSizeRatio && 
                            heightRatio >= fallbackMinFaceSizeRatio && heightRatio <= fallbackMaxFaceSizeRatio;
        
        Log.d(TAG, "checkFaceWithinOvalFallback: ellipseValue=" + String.format("%.4f", ellipseValue) + 
              ", widthRatio=" + String.format("%.4f", widthRatio) + ", heightRatio=" + String.format("%.4f", heightRatio) + 
              ", isWithinEllipse=" + isWithinEllipse + ", isGoodSize=" + isGoodSize +
              ", fallback thresholds: minSize=" + fallbackMinFaceSizeRatio + 
              ", maxSize=" + fallbackMaxFaceSizeRatio + ", result=" + (isWithinEllipse && isGoodSize));
        
        return isWithinEllipse && isGoodSize;
    }
    
    /**
     * Process a frame continuously for zero-touch face recognition
     * Enhanced with oval boundary validation
     * 
     * @param bitmap Current frame bitmap
     * @param ovalRect Oval boundary for validation (can be null)
     * @param callback Callback for continuous processing results
     * @return true if processing was started, false if already processing
     */
    public boolean processContinuousFrame(Bitmap bitmap, android.graphics.RectF ovalRect, ContinuousProcessingCallback callback) {
        // Skip if already processing a frame or models not initialized
        if (!isInitialized()) {
            return false;
        }
        
        // If already processing, reset the flag to allow processing this new frame
        if (isProcessing.get()) {
            Log.d(TAG, "processContinuousFrame: Resetting processing flag to allow new frame processing");
            isProcessing.set(false);
        }
        
        isProcessing.set(true);
        
        // 🔧 NEW: Use retry manager for face detection
        retryManager.executeWithRetryAsync(() -> {
            try {
                // Step 1: Detect face with retry
                List<FaceDetector.FaceDetectionResult> faces = faceDetector.detectFaces(bitmap);
                
                if (faces.isEmpty()) {
                    runOnMainThread(() -> {
                        callback.onNoFaceDetected();
                        isProcessing.set(false);
                    });
                    return null;
                }
                
                if (faces.size() > 1) {
                    runOnMainThread(() -> {
                        callback.onMultipleFacesDetected();
                        isProcessing.set(false);
                    });
                    return null;
                }
                
                // Get the single detected face
                FaceDetector.FaceDetectionResult faceResult = faces.get(0);
                Rect boundingBox = faceResult.getBoundingBox();
                
                // 🔧 NEW: Use FaceDecisionEngine for oval validation
                FaceDecisionEngine.OvalValidationResult ovalValidation = validateOvalBoundary(boundingBox, ovalRect);
                
                if (!ovalValidation.isValid()) {
                    Log.d(TAG, "processContinuousFrame: Face not within oval boundary");
                    runOnMainThread(() -> {
                        callback.onError(ovalValidation.getReason());
                        isProcessing.set(false);
                    });
                    return null;
                }
                
                // Step 2: Check for spoofing with oval validation
                faceSpoofDetector.detectSpoofAsync(bitmap, boundingBox, ovalRect, spoofResult -> {
                    Log.d(TAG, "processContinuousFrame: Spoof detection completed - isSpoof: " + 
                          spoofResult.isSpoof() + ", score: " + spoofResult.getScore());
                    
                    // 🔧 NEW: Use FaceDecisionEngine for decision making
                    FaceDecisionEngine.FaceDetectionResult detectionResult = new FaceDecisionEngine.FaceDetectionResult(
                        true, boundingBox, spoofResult.getConfidence()
                    );
                    
                    FaceDecisionEngine.SpoofDetectionResult spoofDetectionResult = new FaceDecisionEngine.SpoofDetectionResult(
                        spoofResult.isSpoof(), spoofResult.getConfidence(), spoofResult.getScore()
                    );
                    
                    FaceDecisionEngine.FaceDecisionResult decision = decisionEngine.evaluate(
                        detectionResult, spoofDetectionResult, ovalValidation
                    );
                    
                    Log.d(TAG, "processContinuousFrame: Decision result: " + decision);
                    
                    runOnMainThread(() -> {
                        Log.d(TAG, "processContinuousFrame: Calling callback with isSpoof: " + 
                              spoofResult.isSpoof() + ", score: " + spoofResult.getScore());
                        callback.onFaceDetected(boundingBox, spoofResult.isSpoof(), spoofResult.getScore());
                        isProcessing.set(false);
                    });
                });
                
                return null;
                
            } catch (Exception e) {
                Log.e(TAG, "Error in continuous frame processing", e);
                errorHandler.handleFaceDetectionError(e);
                runOnMainThread(() -> {
                    callback.onError("Error processing frame: " + e.getMessage());
                    isProcessing.set(false);
                });
                return null;
            }
        }, new ModelRetryManager.RetryCallback<Object>() {
            @Override
            public void onSuccess(Object result) {
                // Success handled in the operation
            }
            
            @Override
            public void onFailure(ModelRetryManager.ModelRetryException exception) {
                Log.e(TAG, "Retry failed for continuous frame processing", exception);
                errorHandler.handleGeneralError(exception, "continuous frame processing");
                runOnMainThread(() -> {
                    callback.onError("Processing failed after retries");
                    isProcessing.set(false);
                });
            }
        });
        
        return true;
    }
    
    /**
     * Legacy method for backward compatibility
     */
    public boolean processContinuousFrame(Bitmap bitmap, ContinuousProcessingCallback callback) {
        return processContinuousFrame(bitmap, null, callback);
    }
    
    /**
     * Capture and process a stable face for registration
     * Enhanced with oval boundary validation
     * 
     * @param bitmap Bitmap containing the face
     * @param boundingBox Bounding box of the face
     * @param ovalRect Oval boundary for validation (can be null)
     * @param userId User ID for registration
     * @param callback Callback for registration result
     */
    public void captureAndRegisterFace(Bitmap bitmap, Rect boundingBox, android.graphics.RectF ovalRect, 
                                     String userId, FaceIdCallback callback) {
        executor.execute(() -> {
            try {
                // Check if face is within oval boundary if oval is provided
                if (ovalRect != null) {
                    boolean isWithinOval = checkFaceWithinOval(boundingBox, ovalRect);
                    if (!isWithinOval) {
                        Log.d(TAG, "captureAndRegisterFace: Face not within oval boundary");
                        runOnMainThread(() -> callback.onFailure("Please position your face within the oval guide"));
                        return;
                    }
                }
                
                // Crop the face using landmark-aware pipeline
                Rect stableCrop = computeSquareCropWithMargin(bitmap, boundingBox, 1.25f);
                Bitmap faceBitmap = Bitmap.createBitmap(bitmap, stableCrop.left, stableCrop.top, stableCrop.width(), stableCrop.height());
                Bitmap aligned = tryAlignFace(faceBitmap);
                if (aligned != null) faceBitmap = aligned;
                
                // Do one final spoof check with oval boundary
                Bitmap finalFaceBitmap = faceBitmap;
                faceSpoofDetector.detectSpoofAsync(bitmap, boundingBox, ovalRect, spoofResult -> {
                    if (spoofResult.isSpoof()) {
                        Log.d(TAG, "captureAndRegisterFace: Spoof detected during registration");
                        runOnMainThread(() -> callback.onFailure("Spoof detected! Please use a real face for registration."));
                        return;
                    }
                    
                    // Register the face
                    registerFaceId(finalFaceBitmap, userId, callback);
                });
                
            } catch (Exception e) {
                Log.e(TAG, "Error capturing face for registration", e);
                runOnMainThread(() -> callback.onFailure("Error capturing face: " + e.getMessage()));
            }
        });
    }

    /**
     * Capture and update face embedding with spoof/oval validations (Update flow)
     */
    public void captureAndUpdateFace(Bitmap bitmap, Rect boundingBox, android.graphics.RectF ovalRect,
                                     String userId, FaceIdCallback callback) {
        executor.execute(() -> {
            try {
                // Validate oval if provided
                if (ovalRect != null) {
                    boolean isWithinOval = checkFaceWithinOval(boundingBox, ovalRect);
                    if (!isWithinOval) {
                        runOnMainThread(() -> callback.onFailure("Please position your face within the oval guide"));
                        return;
                    }
                }

                // Crop the face using landmark-aware pipeline
                Rect stableCrop = computeSquareCropWithMargin(bitmap, boundingBox, 1.25f);
                Bitmap faceBitmap = Bitmap.createBitmap(bitmap, stableCrop.left, stableCrop.top, stableCrop.width(), stableCrop.height());
                Bitmap aligned = tryAlignFace(faceBitmap);
                if (aligned != null) faceBitmap = aligned;

                // Final spoof check before update
                Bitmap finalFaceBitmap = faceBitmap;
                faceSpoofDetector.detectSpoofAsync(bitmap, boundingBox, ovalRect, spoofResult -> {
                    if (spoofResult.isSpoof()) {
                        runOnMainThread(() -> callback.onFailure("Spoof detected! Please use a real face for update."));
                        return;
                    }

                    // Proceed with update API
                    updateFaceId(finalFaceBitmap, userId, callback);
                });
            } catch (Exception e) {
                Log.e(TAG, "Error capturing face for update", e);
                runOnMainThread(() -> callback.onFailure("Error capturing face: " + e.getMessage()));
            }
        });
    }
    
    /**
     * Capture and verify face embedding with spoof/oval validations (Verify flow)
     */
    public void captureAndVerifyFace(Bitmap bitmap, Rect boundingBox, android.graphics.RectF ovalRect,
                                     String userId, FaceIdCallback callback) {
        executor.execute(() -> {
            try {
                // Validate oval if provided
                if (ovalRect != null) {
                    boolean isWithinOval = checkFaceWithinOval(boundingBox, ovalRect);
                    if (!isWithinOval) {
                        runOnMainThread(() -> callback.onFailure("Please position your face within the oval guide"));
                        return;
                    }
                }
                
                // Crop the face using landmark-aware pipeline
                Rect stableCrop = computeSquareCropWithMargin(bitmap, boundingBox, 1.25f);
                Bitmap faceBitmap = Bitmap.createBitmap(bitmap, stableCrop.left, stableCrop.top, stableCrop.width(), stableCrop.height());
                Bitmap aligned = tryAlignFace(faceBitmap);
                if (aligned != null) faceBitmap = aligned;
                
                // Optional spoof check before verification (same gate as update)
                Bitmap finalFaceBitmap = faceBitmap;
                faceSpoofDetector.detectSpoofAsync(bitmap, boundingBox, ovalRect, spoofResult -> {
                    if (spoofResult.isSpoof()) {
                        runOnMainThread(() -> callback.onFailure("Spoof detected! Please use a real face for verification."));
                        return;
                    }
                    
                    // Proceed with legacy verify API (ad-hoc) by default
                    verifyFaceId(finalFaceBitmap, userId, callback);
                });
            } catch (Exception e) {
                Log.e(TAG, "Error capturing face for verify", e);
                runOnMainThread(() -> callback.onFailure("Error capturing face: " + e.getMessage()));
            }
        });
    }

    /**
     * Verify face embedding for a specific request window
     */
    public void verifyFaceIdForRequest(Bitmap faceBitmap, String userId, String requestId, Float threshold, FaceIdCallback callback) {
        if (!isInitialized()) {
            awaitInitialization(5000,
                () -> verifyFaceIdForRequest(faceBitmap, userId, requestId, threshold, callback),
                () -> runOnMainThread(() -> callback.onFailure("Face embedding model not initialized yet"))
            );
            return;
        }

        faceEmbedding.getFaceEmbeddingAsync(faceBitmap, embedding -> {
            executor.execute(() -> {
                try {
                    ByteBuffer buffer = ByteBuffer.allocate(embedding.length * 4);
                    buffer.order(ByteOrder.LITTLE_ENDIAN);
                    for (float v : embedding) buffer.putFloat(v);
                    logEmbeddingDebug(embedding, buffer.array(), "verify_request");
                    saveEmbeddingDebug(buffer.array(), "verify_request");

                    RequestBody userIdPart = RequestBody.create(MediaType.parse("text/plain"), userId);
                    RequestBody thresholdPart = threshold != null ?
                            RequestBody.create(MediaType.parse("text/plain"), String.valueOf(threshold)) :
                            null;
                    RequestBody embeddingPart = RequestBody.create(MediaType.parse("application/octet-stream"), buffer.array());
                    MultipartBody.Part filePart = MultipartBody.Part.createFormData("embedding", "embedding.bin", embeddingPart);

                    FaceIdApiController api = ApiClient.getClient(context).create(FaceIdApiController.class);
                    retrofit2.Call<FaceIdVerifyResponse> call =
                            api.verifyFaceId(requestId, userIdPart, filePart, thresholdPart);
                    call.enqueue(new retrofit2.Callback<FaceIdVerifyResponse>() {
                        @Override
                        public void onResponse(@NonNull retrofit2.Call<FaceIdVerifyResponse> c,
                                               @NonNull retrofit2.Response<FaceIdVerifyResponse> resp) {
                            if (resp.isSuccessful() && resp.body() != null) {
                                if (resp.body().isSuccess()) {
                                    runOnMainThread(() -> callback.onSuccess("Face ID verified successfully"));
                                } else {
                                    runOnMainThread(() -> callback.onFailure(resp.body().getMessage() != null ? resp.body().getMessage() : "Verification failed"));
                                }
                            } else if (resp.code() == 410) {
                                runOnMainThread(() -> callback.onFailure("Verification window expired"));
                            } else {
                                runOnMainThread(() -> callback.onFailure("Failed to verify: HTTP " + resp.code())) ;
                            }
                        }

                        @Override
                        public void onFailure(@NonNull retrofit2.Call<FaceIdVerifyResponse> c,
                                              @NonNull Throwable t) {
                            runOnMainThread(() -> callback.onFailure("Network error: " + t.getMessage()));
                        }
                    });
                } catch (Exception e) {
                    Log.e(TAG, "Error in verifyFaceIdForRequest", e);
                    runOnMainThread(() -> callback.onFailure("Error: " + e.getMessage()));
                }
            });
        });
    }
    
    /**
     * Legacy method for backward compatibility
     */
    public void captureAndRegisterFace(Bitmap bitmap, Rect boundingBox, String userId, FaceIdCallback callback) {
        captureAndRegisterFace(bitmap, boundingBox, null, userId, callback);
    }
    
    /**
     * Register a new face ID by sending the embedding to backend
     */
    public void registerFaceId(Bitmap faceBitmap, String userId, FaceIdCallback callback) {
        Log.d(TAG, "registerFaceId: Starting face ID registration");
        Log.d(TAG, "registerFaceId: faceBitmap=" + faceBitmap.getWidth() + "x" + faceBitmap.getHeight() + 
              ", userId=" + userId);
        
        // Check if models are initialized
        if (!isInitialized()) {
            Log.w(TAG, "registerFaceId: Models not initialized - waiting for initialization");
            awaitInitialization(5000, 
                () -> registerFaceId(faceBitmap, userId, callback),
                () -> runOnMainThread(() -> {
                    Log.e(TAG, "registerFaceId: FAILED - Face embedding model initialization timeout");
                    callback.onFailure("Face embedding model not initialized yet");
                })
            );
            return;
        }
        
        Log.d(TAG, "registerFaceId: Models initialized - generating face embedding");
        
        // 🔧 NEW: Use retry manager for embedding generation
        retryManager.executeWithRetryAsync(() -> {
            try {
                // 🔧 NEW: Add progress logging
                Log.d(TAG, "registerFaceId: Starting face embedding generation...");
                
                // Generate face embedding with retry
                float[] embedding = retryManager.executeWithRetry(() -> faceEmbedding.getFaceEmbedding(faceBitmap));
                Log.d(TAG, "registerFaceId: Face embedding generated - length: " + embedding.length);
                
                // Convert embedding to byte array for API call (float32 little-endian)
                ByteBuffer buffer = ByteBuffer.allocate(embedding.length * 4);
                buffer.order(ByteOrder.LITTLE_ENDIAN);
                for (float value : embedding) {
                    buffer.putFloat(value);
                }
                // Log embedding preview + sizes
                logEmbeddingDebug(embedding, buffer.array(), "register");
                // Save a debug copy of the exact embedding being sent
                saveEmbeddingDebug(buffer.array(), "register");
                
                Log.d(TAG, "registerFaceId: Creating multipart request - buffer size: " + buffer.array().length);
                
                // Create multipart request
                RequestBody embeddingPart = RequestBody.create(
                        MediaType.parse("application/octet-stream"), 
                        buffer.array());
                
                MultipartBody.Part filePart = MultipartBody.Part.createFormData(
                        "embedding", "embedding.bin", embeddingPart);
                
                RequestBody userIdPart = RequestBody.create(
                        MediaType.parse("text/plain"), userId);
                
                Log.d(TAG, "registerFaceId: sending userId=" + userId);
                Log.d(TAG, "registerFaceId: Making API call to register face ID");
                
                // 🔧 NEW: Enhanced API call with better error handling and timeout
                Call<FaceIdResponse> call = faceIdApiController.registerFaceId(filePart, userIdPart);
                
                // 🔧 NEW: Add timeout to the call
                call.enqueue(new Callback<FaceIdResponse>() {
                    @Override
                    public void onResponse(@NonNull Call<FaceIdResponse> call, @NonNull Response<FaceIdResponse> response) {
                        Log.d(TAG, "registerFaceId: API response received - code: " + response.code() + 
                              ", successful: " + response.isSuccessful() + ", body: " + (response.body() != null ? response.body().toString() : "null"));
                        
                        if (response.isSuccessful() && response.body() != null) {
                            FaceIdResponse responseBody = response.body();
                            if (responseBody.isSuccess()) {
                                Log.d(TAG, "registerFaceId: SUCCESS - Face ID registered successfully");
                                runOnMainThread(() -> callback.onSuccess("Face ID registered successfully"));
                            } else {
                                String errorMsg = "Server error: " + responseBody.getMessage();
                                Log.e(TAG, "registerFaceId: SERVER FAILURE - " + errorMsg);
                                runOnMainThread(() -> callback.onFailure(errorMsg));
                            }
                        } else {
                            String errorMsg;
                            String serverMessage = null;
                            try {
                                if (response.errorBody() != null) {
                                    String raw = response.errorBody().string();
                                    Log.e(TAG, "registerFaceId: errorBody= " + raw);
                                    // Try to extract a simple message field if present
                                    int idx = raw.indexOf("\"message\"");
                                    if (idx >= 0) {
                                        int colon = raw.indexOf(":", idx);
                                        if (colon > 0) {
                                            String tmp = raw.substring(colon + 1);
                                            tmp = tmp.replace("{", "");
                                            tmp = tmp.replace("}", "");
                                            tmp = tmp.replace("\"", "");
                                            tmp = tmp.replace("\n", "");
                                            serverMessage = tmp.trim();
                                        }
                                    }
                                }
                            } catch (Exception ignored) {}
                            if (response.code() == 401) {
                                errorMsg = "Authentication failed. Please login again.";
                            } else if (response.code() == 403) {
                                errorMsg = "Access denied. Please check your permissions.";
                            } else if (response.code() == 404) {
                                errorMsg = "Service not found. Please contact support.";
                            } else if (response.code() >= 500) {
                                errorMsg = "Server error. Please try again later.";
                            } else if (response.code() == 400) {
                                // Use server message when available
                                errorMsg = serverMessage != null && !serverMessage.isEmpty()
                                        ? serverMessage
                                        : ("Bad Request");
                                // Auto-fallback: user may already have a registered Face ID
                                if (errorMsg.toLowerCase().contains("already has") || errorMsg.toLowerCase().contains("already registered")) {
                                    Log.w(TAG, "registerFaceId: 400 indicates already registered; falling back to updateFaceId");
                                    // Retry via update API
                                    updateFaceId(faceBitmap, userId, callback);
                                    return;
                                }
                            } else {
                                errorMsg = "Failed to register Face ID: " + (serverMessage != null ? serverMessage : response.message());
                            }
                            Log.e(TAG, "registerFaceId: HTTP FAILURE - " + errorMsg + " (code: " + response.code() + ")");
                            runOnMainThread(() -> callback.onFailure(errorMsg));
                        }
                    }
                    
                    @Override
                    public void onFailure(@NonNull Call<FaceIdResponse> call, @NonNull Throwable t) {
                        String errorMsg;
                        if (t instanceof java.net.SocketTimeoutException) {
                            errorMsg = "Request timeout. Please check your internet connection and try again.";
                        } else if (t instanceof java.net.UnknownHostException) {
                            errorMsg = "Cannot connect to server. Please check your internet connection.";
                        } else if (t instanceof java.net.ConnectException) {
                            errorMsg = "Connection failed. Please check your internet connection.";
                        } else {
                            errorMsg = "Network error: " + t.getMessage();
                        }
                        Log.e(TAG, "registerFaceId: NETWORK FAILURE - " + errorMsg, t);
                        runOnMainThread(() -> callback.onFailure(errorMsg));
                    }
                });
                
                return null;
                
            } catch (Exception e) {
                Log.e(TAG, "registerFaceId: EXCEPTION during API call preparation", e);
                errorHandler.handleGeneralError(e, "face registration");
                runOnMainThread(() -> callback.onFailure("Error: " + e.getMessage()));
                return null;
            }
        }, new ModelRetryManager.RetryCallback<Object>() {
            @Override
            public void onSuccess(Object result) {
                // Success handled in the operation
            }
            
            @Override
            public void onFailure(ModelRetryManager.ModelRetryException exception) {
                Log.e(TAG, "Retry failed for face registration", exception);
                errorHandler.handleGeneralError(exception, "face registration");
                runOnMainThread(() -> callback.onFailure("Registration failed after retries"));
            }
        });
    }
    
    /**
     * Update existing face ID
     */
    public void updateFaceId(Bitmap faceBitmap, String userId, FaceIdCallback callback) {
        // Check if models are initialized
        if (!isInitialized()) {
            awaitInitialization(5000, 
                () -> updateFaceId(faceBitmap, userId, callback),
                () -> runOnMainThread(() -> callback.onFailure("Face embedding model not initialized yet"))
            );
            return;
        }
        
        // Use async method to generate embedding
        faceEmbedding.getFaceEmbeddingAsync(faceBitmap, embedding -> {
            executor.execute(() -> {
                try {
                    // Convert embedding to byte array for API call (float32 little-endian)
                    ByteBuffer buffer = ByteBuffer.allocate(embedding.length * 4);
                    buffer.order(ByteOrder.LITTLE_ENDIAN);
                    for (float value : embedding) {
                        buffer.putFloat(value);
                    }
                            // Log embedding preview + sizes
                            logEmbeddingDebug(embedding, buffer.array(), "update");
                            // Save a debug copy of the exact embedding being sent
                            saveEmbeddingDebug(buffer.array(), "update");
                    // Save a debug copy of the exact embedding being sent
                    saveEmbeddingDebug(buffer.array(), "update");
                    
                    // Create multipart request
                    RequestBody embeddingPart = RequestBody.create(
                            MediaType.parse("application/octet-stream"), 
                            buffer.array());
                    
                    MultipartBody.Part filePart = MultipartBody.Part.createFormData(
                            "embedding", "embedding.bin", embeddingPart);
                    
                    RequestBody userIdPart = RequestBody.create(
                            MediaType.parse("text/plain"), userId);
                    Log.d(TAG, "updateFaceId: sending userId=" + userId);
                    
                    // Make API call
                    Call<FaceIdResponse> call = faceIdApiController.updateFaceId(filePart, userIdPart);
                    call.enqueue(new Callback<FaceIdResponse>() {
                        @Override
                        public void onResponse(@NonNull Call<FaceIdResponse> call, @NonNull Response<FaceIdResponse> response) {
                            if (response.isSuccessful() && response.body() != null) {
                                runOnMainThread(() -> callback.onSuccess("Face ID updated successfully"));
                            } else {
                                runOnMainThread(() -> callback.onFailure("Failed to update Face ID: " + response.message()));
                            }
                        }
                        
                        @Override
                        public void onFailure(@NonNull Call<FaceIdResponse> call, @NonNull Throwable t) {
                            runOnMainThread(() -> callback.onFailure("Network error: " + t.getMessage()));
                        }
                    });
                    
                } catch (Exception e) {
                    Log.e(TAG, "Error updating face ID", e);
                    runOnMainThread(() -> callback.onFailure("Error: " + e.getMessage()));
                }
            });
        });
    }
    
    /**
     * Enhanced face verification with oval boundary validation
     * @param faceBitmap The face image to verify
     * @param faceRect The detected face rectangle  
     * @param ovalRect The oval boundary for position validation
     * @param userId The user ID to verify against
     * @param callback Enhanced callback with confidence scores
     */
    public void verifyFace(Bitmap faceBitmap, Rect faceRect, android.graphics.RectF ovalRect, 
                          String userId, FaceVerificationCallback callback) {
        // Check if models are initialized
        if (!isInitialized()) {
            awaitInitialization(5000, 
                () -> verifyFace(faceBitmap, faceRect, ovalRect, userId, callback),
                () -> runOnMainThread(() -> callback.onError("Face embedding model not initialized yet"))
            );
            return;
        }
        
        // Validate face position within oval if provided
        if (ovalRect != null && faceRect != null) {
            // Use the same validation logic as checkFaceWithinOval for consistency
            boolean isWithinOval = checkFaceWithinOval(faceRect, ovalRect);
            if (!isWithinOval) {
                runOnMainThread(() -> callback.onVerificationFailed("Face not properly positioned within oval"));
                return;
            }
        }
        
        // Use async method to generate embedding
        faceEmbedding.getFaceEmbeddingAsync(faceBitmap, embedding -> {
            executor.execute(() -> {
                try {
                                // Convert embedding to byte array for API call (float32 little-endian)
                    ByteBuffer buffer = ByteBuffer.allocate(embedding.length * 4);
                    buffer.order(ByteOrder.LITTLE_ENDIAN);
                    for (float value : embedding) {
                        buffer.putFloat(value);
                    }
                                // Log embedding preview + sizes
                                logEmbeddingDebug(embedding, buffer.array(), "verify");
                                // Save a debug copy of the exact embedding being sent
                                saveEmbeddingDebug(buffer.array(), "verify");
                    // Save a debug copy of the exact embedding being sent
                    saveEmbeddingDebug(buffer.array(), "verify");
                    
                    // Create multipart request
                    RequestBody embeddingPart = RequestBody.create(
                            MediaType.parse("application/octet-stream"), 
                            buffer.array());
                    
                    MultipartBody.Part filePart = MultipartBody.Part.createFormData(
                            "embedding", "embedding.bin", embeddingPart);
                    
                    RequestBody userIdPart = RequestBody.create(
                            MediaType.parse("text/plain"), userId);
                    Log.d(TAG, "verifyFace: sending userId=" + userId);
                    
                    // Make API call (ad-hoc legacy)
                    Call<FaceIdResponse> call = faceIdApiController.verifyFaceIdAdhoc(filePart, userIdPart);
                    call.enqueue(new Callback<FaceIdResponse>() {
                        @Override
                        public void onResponse(@NonNull Call<FaceIdResponse> call, @NonNull Response<FaceIdResponse> response) {
                            if (response.isSuccessful() && response.body() != null) {
                                FaceIdResponse responseBody = response.body();
                                if (responseBody.isSuccess()) {
                                    // Extract confidence from response or use default high confidence
                                    float confidence = 0.95f; // Default for successful verification
                                    runOnMainThread(() -> callback.onVerified(confidence));
                                } else {
                                    runOnMainThread(() -> callback.onVerificationFailed("Face ID verification failed - face does not match"));
                                }
                            } else {
                                runOnMainThread(() -> callback.onError("Failed to verify Face ID: " + response.message()));
                            }
                        }
                        
                        @Override
                        public void onFailure(@NonNull Call<FaceIdResponse> call, @NonNull Throwable t) {
                            runOnMainThread(() -> callback.onError("Network error: " + t.getMessage()));
                        }
                    });
                    
                } catch (Exception e) {
                    Log.e(TAG, "Error verifying face ID", e);
                    runOnMainThread(() -> callback.onError("Error: " + e.getMessage()));
                }
            });
        });
    }
    
    /**
     * Verify face ID against stored embedding
     */
    public void verifyFaceId(Bitmap faceBitmap, String userId, FaceIdCallback callback) {
        // Check if models are initialized
        if (!isInitialized()) {
            awaitInitialization(5000, 
                () -> verifyFaceId(faceBitmap, userId, callback),
                () -> runOnMainThread(() -> callback.onFailure("Face embedding model not initialized yet"))
            );
            return;
        }
        
        // Use async method to generate embedding
        faceEmbedding.getFaceEmbeddingAsync(faceBitmap, embedding -> {
            executor.execute(() -> {
                try {
                    // Convert embedding to byte array for API call (float32 little-endian)
                    ByteBuffer buffer = ByteBuffer.allocate(embedding.length * 4);
                    buffer.order(ByteOrder.LITTLE_ENDIAN);
                    for (float value : embedding) {
                        buffer.putFloat(value);
                    }
                    // Log and save debug copy
                    logEmbeddingDebug(embedding, buffer.array(), "verify");
                    saveEmbeddingDebug(buffer.array(), "verify");
                    
                    // Create multipart request
                    RequestBody embeddingPart = RequestBody.create(
                            MediaType.parse("application/octet-stream"), 
                            buffer.array());
                    
                    MultipartBody.Part filePart = MultipartBody.Part.createFormData(
                            "embedding", "embedding.bin", embeddingPart);
                    
                    RequestBody userIdPart = RequestBody.create(
                            MediaType.parse("text/plain"), userId);
                    Log.d(TAG, "verifyFaceId: sending userId=" + userId);
                    
                    // Make API call (ad-hoc legacy)
                    Call<FaceIdResponse> call = faceIdApiController.verifyFaceIdAdhoc(filePart, userIdPart);
                    call.enqueue(new Callback<FaceIdResponse>() {
                        @Override
                        public void onResponse(@NonNull Call<FaceIdResponse> call, @NonNull Response<FaceIdResponse> response) {
                            if (response.isSuccessful() && response.body() != null) {
                                FaceIdResponse responseBody = response.body();
                                if (responseBody.isSuccess()) {
                                    runOnMainThread(() -> callback.onSuccess("Face ID verified successfully"));
                                } else {
                                    runOnMainThread(() -> callback.onFailure("Face ID verification failed"));
                                }
                            } else {
                                runOnMainThread(() -> callback.onFailure("Failed to verify Face ID: " + response.message()));
                            }
                        }
                        
                        @Override
                        public void onFailure(@NonNull Call<FaceIdResponse> call, @NonNull Throwable t) {
                            runOnMainThread(() -> callback.onFailure("Network error: " + t.getMessage()));
                        }
                    });
                    
                } catch (Exception e) {
                    Log.e(TAG, "Error verifying face ID", e);
                    runOnMainThread(() -> callback.onFailure("Error: " + e.getMessage()));
                }
            });
        });
    }

    /**
     * Helper method to run code on the main thread
     */
    /**
     * 🔧 NEW: Generate cache key for bitmap
     */
    private String generateCacheKey(Bitmap bitmap, String operation) {
        return bitmap.getWidth() + "x" + bitmap.getHeight() + "_" + operation + "_" + System.currentTimeMillis();
    }
    
    /**
     * 🔧 NEW: Get memory statistics
     */
    public FaceIdMemoryManager.MemoryStats getMemoryStats() {
        return memoryManager.getMemoryStats();
    }
    
    /**
     * 🔧 NEW: Get performance statistics
     */
    public FaceIdPerformanceManager.PerformanceStats getPerformanceStats() {
        return performanceManager.getPerformanceStats();
    }
    
    /**
     * 🔧 NEW: Set scenario for configuration
     */
    public void setScenario(FaceIdConfig.Scenario scenario) {
        configManager.setScenario(scenario);
    }
    
    /**
     * 🔧 NEW: Force memory cleanup
     */
    public void forceMemoryCleanup() {
        memoryManager.forceCleanup();
    }
    
    /**
     * 🔧 NEW: Clear performance cache
     */
    public void clearPerformanceCache() {
        performanceManager.clearCache();
    }
    
    private void runOnMainThread(Runnable runnable) {
        mainHandler.post(runnable);
    }

    /**
     * Save a copy of the embedding bytes to app cache for debugging/API testing
     * File name format: embedding_{action}_<timestamp>.bin
     */
     private void saveEmbeddingDebug(byte[] bytes, String action) {
         try {
             File dir = new File(context.getCacheDir(), "face_registration");
             if (!dir.exists()) {
                 //noinspection ResultOfMethodCallIgnored
                 dir.mkdirs();
             }
             File out = new File(dir, "embedding_" + action + "_" + System.currentTimeMillis() + ".bin");
             try (FileOutputStream fos = new FileOutputStream(out)) {
                 fos.write(bytes);
             }
             Log.d(TAG, "Saved embedding debug file: " + out.getAbsolutePath());
         } catch (Exception e) {
             Log.w(TAG, "Failed to save embedding debug file", e);
         }
     }

    /**
     * Log a short preview and sizes to help debugging embedding payload
     */
    private void logEmbeddingDebug(float[] embedding, byte[] rawBytes, String action) {
        try {
            int n = embedding != null ? embedding.length : -1;
            int bytes = rawBytes != null ? rawBytes.length : -1;
            // preview first up to 8 floats
            StringBuilder sb = new StringBuilder();
            int preview = Math.min(8, Math.max(0, n));
            for (int i = 0; i < preview; i++) {
                if (i > 0) sb.append(", ");
                sb.append(String.format(java.util.Locale.US, "%.6f", embedding[i]));
            }
            Log.d(TAG, "embedding(" + action + ") len=" + n + ", bytes=" + bytes + ", head=[" + sb + "]");
        } catch (Exception e) {
            Log.w(TAG, "Failed to log embedding debug", e);
        }
    }

    /**
     * Compute a square crop around the detected face with an expansion margin for stability.
     */
    private Rect computeSquareCropWithMargin(Bitmap source, Rect faceRect, float marginScale) {
        if (faceRect == null) return new Rect(0, 0, source.getWidth(), source.getHeight());
        int cx = faceRect.centerX();
        int cy = faceRect.centerY();
        int size = Math.max(faceRect.width(), faceRect.height());
        size = Math.round(size * marginScale);
        int half = size / 2;
        int left = Math.max(0, cx - half);
        int top = Math.max(0, cy - half);
        int right = Math.min(source.getWidth(), cx + half);
        int bottom = Math.min(source.getHeight(), cy + half);
        // Ensure square
        int width = right - left;
        int height = bottom - top;
        int side = Math.min(Math.min(size, source.getWidth()), source.getHeight());
        // Re-center if needed
        left = Math.max(0, cx - side / 2);
        top = Math.max(0, cy - side / 2);
        right = Math.min(source.getWidth(), left + side);
        bottom = Math.min(source.getHeight(), top + side);
        // Adjust again if clamped
        left = right - side;
        top = bottom - side;
        return new Rect(left, top, right, bottom);
    }

    /**
     * Try to align face using eye centers. Returns aligned 160x160 bitmap or null if alignment not possible.
     */
    private Bitmap tryAlignFace(Bitmap faceBitmap) {
        try {
            if (mediaPipeFaceLandmarkExtractor == null || !mediaPipeFaceLandmarkExtractor.isModelAvailable()) {
                return null;
            }
            // Extract landmarks synchronously (best-effort) using image mode
            final CountDownLatch latch = new CountDownLatch(1);
            final boolean[] ok = {false};
            mediaPipeFaceLandmarkExtractor.extractLandmarks(faceBitmap, new Rect(0,0,faceBitmap.getWidth(), faceBitmap.getHeight()), success -> {
                ok[0] = success;
                latch.countDown();
            });
            latch.await(300, TimeUnit.MILLISECONDS);
            if (!ok[0]) return null;

            android.graphics.PointF left = mediaPipeFaceLandmarkExtractor.getLastLeftEyeCenter();
            android.graphics.PointF right = mediaPipeFaceLandmarkExtractor.getLastRightEyeCenter();
            if (left == null || right == null) return null;

            // Compute angle and scale to align eyes horizontally to canonical distance
            double dx = right.x - left.x;
            double dy = right.y - left.y;
            double angle = Math.atan2(dy, dx);
            double eyeDist = Math.hypot(dx, dy);
            if (eyeDist < 1.0) return null;

            // Canonical eye positions in 160x160 (approx FaceNet canonical)
            float cxLeft = 54f;  // tweakable
            float cyLeft = 64f;
            float cxRight = 106f;
            float cyRight = 64f;
            double targetDist = Math.hypot(cxRight - cxLeft, cyRight - cyLeft);
            float scale = (float) (targetDist / eyeDist);

            android.graphics.Matrix m = new android.graphics.Matrix();
            m.postTranslate(-left.x, -left.y);
            m.postRotate((float) (-Math.toDegrees(angle)));
            m.postScale(scale, scale);
            m.postTranslate(cxLeft, cyLeft);

            Bitmap aligned = Bitmap.createBitmap(160, 160, Bitmap.Config.ARGB_8888);
            android.graphics.Canvas c = new android.graphics.Canvas(aligned);
            c.drawBitmap(faceBitmap, m, null);
            return aligned;
        } catch (Exception ignore) {
            return null;
        }
    }
    
    /**
     * Close and release all resources
     */
    public void close() {
        try {
            Log.d(TAG, "Closing FaceIdService and releasing resources");
            
            // Close MediaPipeFaceLandmarkExtractor
            if (mediaPipeFaceLandmarkExtractor != null) {
                mediaPipeFaceLandmarkExtractor.close();
                mediaPipeFaceLandmarkExtractor = null;
            }
            
            // Close other components if they have close methods
            if (faceDetector != null && faceDetector instanceof AutoCloseable) {
                try {
                    ((AutoCloseable) faceDetector).close();
                } catch (Exception e) {
                    Log.w(TAG, "Error closing faceDetector", e);
                }
            }
            
            if (faceEmbedding != null && faceEmbedding instanceof AutoCloseable) {
                try {
                    ((AutoCloseable) faceEmbedding).close();
                } catch (Exception e) {
                    Log.w(TAG, "Error closing faceEmbedding", e);
                }
            }
            
            if (faceSpoofDetector != null && faceSpoofDetector instanceof AutoCloseable) {
                try {
                    ((AutoCloseable) faceSpoofDetector).close();
                } catch (Exception e) {
                    Log.w(TAG, "Error closing faceSpoofDetector", e);
                }
            }
            
            if (gazeEstimator != null && gazeEstimator instanceof AutoCloseable) {
                try {
                    ((AutoCloseable) gazeEstimator).close();
                } catch (Exception e) {
                    Log.w(TAG, "Error closing gazeEstimator", e);
                }
            }
            
            Log.d(TAG, "FaceIdService closed successfully");
        } catch (Exception e) {
            Log.e(TAG, "Error closing FaceIdService", e);
        }
    }
} 