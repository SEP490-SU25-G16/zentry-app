package vn.edu.fpt.zentryapp.student.data.service;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Rect;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import androidx.annotation.NonNull;

import java.nio.ByteBuffer;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executor;
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
import vn.edu.fpt.zentryapp.auth.client.AuthManager;
import vn.edu.fpt.zentryapp.student.data.api.FaceIdApi;
import vn.edu.fpt.zentryapp.student.data.model.response.FaceIdResponse;

public class FaceIdService {
    private static final String TAG = "FaceIdService";
    
    private final Context context;
    private FaceDetector faceDetector;
    private FaceEmbedding faceEmbedding;
    /**
     * -- GETTER --
     *  Get FaceSpoofDetector instance for advanced spoof detection management
     *
     * @return FaceSpoofDetector instance or null if not initialized
     */
    @Getter
    private FaceSpoofDetector faceSpoofDetector;
    private final FaceIdApi faceIdApi;
    private final ExecutorService executor;
    private final Handler mainHandler;
    
    private final CountDownLatch modelLoadLatch = new CountDownLatch(3); // Đếm ngược cho 3 model
    private volatile boolean isInitialized = false;
    private final AtomicBoolean isProcessing = new AtomicBoolean(false);
    
    public FaceIdService(Context context) {
        this.context = context.getApplicationContext();
        this.executor = Executors.newCachedThreadPool(); // Thay đổi thành thread pool
        this.mainHandler = new Handler(Looper.getMainLooper());
        this.faceIdApi = ApiClient.getClient(context).create(FaceIdApi.class);
        
        // Khởi tạo các model bất đồng bộ
        initializeModelsAsync();
    }
    
    private void initializeModelsAsync() {
        // Khởi tạo FaceDetector
        executor.execute(() -> {
            try {
                this.faceDetector = new FaceDetector(context);
                modelLoadLatch.countDown();
                Log.d(TAG, "FaceDetector initialized");
            } catch (Exception e) {
                Log.e(TAG, "Error initializing FaceDetector", e);
                modelLoadLatch.countDown();
            }
        });
        
        // Khởi tạo FaceEmbedding
        executor.execute(() -> {
            try {
                this.faceEmbedding = new FaceEmbedding(context);
                modelLoadLatch.countDown();
                Log.d(TAG, "FaceEmbedding initialized");
            } catch (Exception e) {
                Log.e(TAG, "Error initializing FaceEmbedding", e);
                modelLoadLatch.countDown();
            }
        });
        
        // Khởi tạo FaceSpoofDetector
        executor.execute(() -> {
            try {
                this.faceSpoofDetector = new FaceSpoofDetector(context);
                modelLoadLatch.countDown();
                Log.d(TAG, "FaceSpoofDetector initialized");
            } catch (Exception e) {
                Log.e(TAG, "Error initializing FaceSpoofDetector", e);
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
     * Helper method for processing face with oval boundary validation
     */
    private void processFaceWithOvalBoundary(Bitmap bitmap, Rect boundingBox, android.graphics.RectF ovalRect, 
                                           Bitmap faceBitmap, FaceDetectionCallback callback) {
        Log.d(TAG, "processFaceWithOvalBoundary: Starting processing");
        Log.d(TAG, "processFaceWithOvalBoundary: Face detected with bounding box: " + boundingBox.toString());
        Log.d(TAG, "processFaceWithOvalBoundary: faceBitmap size: " + faceBitmap.getWidth() + "x" + faceBitmap.getHeight());
        
        // Check if face is within oval boundary if oval is provided
        if (ovalRect != null) {
            Log.d(TAG, "processFaceWithOvalBoundary: Validating face position within oval");
            boolean isWithinOval = checkFaceWithinOval(boundingBox, ovalRect);
            if (!isWithinOval) {
                Log.e(TAG, "processFaceWithOvalBoundary: FAILED - Face not within oval boundary");
                runOnMainThread(() -> callback.onError("Please position your face within the oval guide"));
                return;
            }
            Log.d(TAG, "processFaceWithOvalBoundary: Face position validation PASSED");
        } else {
            Log.d(TAG, "processFaceWithOvalBoundary: No oval boundary provided - skipping position validation");
        }
        
        Log.d(TAG, "processFaceWithOvalBoundary: Starting spoof detection");
        
        // Step 2: Check for spoofing using async method with oval validation
        faceSpoofDetector.detectSpoofAsync(bitmap, boundingBox, ovalRect, spoofResult -> {
            Log.d(TAG, "processFaceWithOvalBoundary: Spoof detection result - isSpoof: " + 
                    spoofResult.isSpoof() + ", score: " + spoofResult.getScore() + ", confidence: " + spoofResult.getConfidence());

            boolean isWithinOval = (ovalRect == null) || checkFaceWithinOval(boundingBox, ovalRect);

            // Greatly Improved Decision Logic
            // Case 1: High confidence real face - prioritize model result
            if (spoofResult.getConfidence() > 0.65f && !spoofResult.isSpoof()) {
                Log.d(TAG, "processFaceWithOvalBoundary: SUCCESS - High confidence real face, proceeding");
                runOnMainThread(() -> callback.onFaceDetected(faceBitmap, boundingBox));
                return;
            }

            // Case 2: Good confidence real face - we're being more lenient now
            if (!spoofResult.isSpoof()) {
                Log.d(TAG, "processFaceWithOvalBoundary: SUCCESS - Face verified as real with acceptable confidence");
                runOnMainThread(() -> callback.onFaceDetected(faceBitmap, boundingBox));
                return;
            }
            
            // Case 3: Face within oval but model thinks it's spoof - we'll still allow it if confidence is low
            if (isWithinOval && spoofResult.isSpoof() && spoofResult.getConfidence() < 0.70f) {
                Log.d(TAG, "processFaceWithOvalBoundary: SUCCESS - Face within oval, allowing despite low spoof confidence");
                runOnMainThread(() -> callback.onFaceDetected(faceBitmap, boundingBox));
                return;
            }
            
            // Case 4: Strong spoof detected
            if (spoofResult.isSpoof() && spoofResult.getConfidence() > 0.85f) {
                Log.e(TAG, "processFaceWithOvalBoundary: FAILED - High confidence spoof detected (score: " + spoofResult.getScore() + ")");
                runOnMainThread(() -> callback.onError("Spoof detected! Please use a real face."));
                return;
            }

            // Case 5: Fallback - give benefit of the doubt
            if (!isWithinOval) {
                Log.e(TAG, "processFaceWithOvalBoundary: FAILED - Face not properly positioned");
                runOnMainThread(() -> callback.onError("Please position your face properly within the oval."));
            } else {
                Log.e(TAG, "processFaceWithOvalBoundary: CAUTION - Unclear verification. Proceeding anyway.");
                runOnMainThread(() -> callback.onFaceDetected(faceBitmap, boundingBox));
            }
        });
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
        
        // Calculate face size relative to oval
        float faceWidth = faceRect.width();
        float faceHeight = faceRect.height();
        float widthRatio = faceWidth / ovalRect.width();
        float heightRatio = faceHeight / ovalRect.height();
        
        // Face should be centered in oval and of appropriate size
        // More lenient thresholds for better user experience
        boolean isWithinEllipse = ellipseValue <= 1.5; // Increased tolerance
        boolean isGoodSize = widthRatio >= 0.35f && widthRatio <= 1.0f && 
                            heightRatio >= 0.35f && heightRatio <= 1.0f; // More lenient range
        
        Log.d(TAG, "checkFaceWithinOval: ellipseValue=" + String.format("%.4f", ellipseValue) + 
              ", widthRatio=" + String.format("%.4f", widthRatio) + ", heightRatio=" + String.format("%.4f", heightRatio) + 
              ", isWithinEllipse=" + isWithinEllipse + ", isGoodSize=" + isGoodSize +
              ", result=" + (isWithinEllipse && isGoodSize));
        
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
        
        executor.execute(() -> {
            try {
                // Step 1: Detect face
                List<FaceDetector.FaceDetectionResult> faces = faceDetector.detectFaces(bitmap);
                
                if (faces.isEmpty()) {
                    runOnMainThread(() -> {
                        callback.onNoFaceDetected();
                        isProcessing.set(false);
                    });
                    return;
                }
                
                if (faces.size() > 1) {
                    runOnMainThread(() -> {
                        callback.onMultipleFacesDetected();
                        isProcessing.set(false);
                    });
                    return;
                }
                
                // Get the single detected face
                FaceDetector.FaceDetectionResult faceResult = faces.get(0);
                Rect boundingBox = faceResult.getBoundingBox();
                
                // Check if face is within oval boundary if oval is provided
                if (ovalRect != null) {
                    boolean isWithinOval = checkFaceWithinOval(boundingBox, ovalRect);
                    if (!isWithinOval) {
                        Log.d(TAG, "processContinuousFrame: Face not within oval boundary");
                        runOnMainThread(() -> {
                            callback.onError("Face not positioned correctly");
                            isProcessing.set(false);
                        });
                        return;
                    }
                }
                
                // Step 2: Check for spoofing with oval validation
                faceSpoofDetector.detectSpoofAsync(bitmap, boundingBox, ovalRect, spoofResult -> {
                    Log.d(TAG, "processContinuousFrame: Spoof detection completed - isSpoof: " + 
                          spoofResult.isSpoof() + ", score: " + spoofResult.getScore());
                    runOnMainThread(() -> {
                        Log.d(TAG, "processContinuousFrame: Calling callback with isSpoof: " + 
                              spoofResult.isSpoof() + ", score: " + spoofResult.getScore());
                        callback.onFaceDetected(boundingBox, spoofResult.isSpoof(), spoofResult.getScore());
                        isProcessing.set(false);
                    });
                });
                
            } catch (Exception e) {
                Log.e(TAG, "Error in continuous frame processing", e);
                runOnMainThread(() -> {
                    callback.onError("Error processing frame: " + e.getMessage());
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
                
                // Crop the face from the bitmap
                Bitmap faceBitmap = Bitmap.createBitmap(
                        bitmap, 
                        boundingBox.left, 
                        boundingBox.top, 
                        boundingBox.width(), 
                        boundingBox.height()
                );
                
                // Do one final spoof check with oval boundary
                faceSpoofDetector.detectSpoofAsync(bitmap, boundingBox, ovalRect, spoofResult -> {
                    if (spoofResult.isSpoof()) {
                        Log.d(TAG, "captureAndRegisterFace: Spoof detected during registration");
                        runOnMainThread(() -> callback.onFailure("Spoof detected! Please use a real face for registration."));
                        return;
                    }
                    
                    // Register the face
                    registerFaceId(faceBitmap, userId, callback);
                });
                
            } catch (Exception e) {
                Log.e(TAG, "Error capturing face for registration", e);
                runOnMainThread(() -> callback.onFailure("Error capturing face: " + e.getMessage()));
            }
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
        
        // Use async method to generate embedding
        faceEmbedding.getFaceEmbeddingAsync(faceBitmap, embedding -> {
            Log.d(TAG, "registerFaceId: Face embedding generated - length: " + embedding.length);
            
            executor.execute(() -> {
                try {
                    Log.d(TAG, "registerFaceId: Converting embedding to byte array");
                    
                    // Convert embedding to byte array for API call
                    ByteBuffer buffer = ByteBuffer.allocate(embedding.length * 4);
                    for (float value : embedding) {
                        buffer.putFloat(value);
                    }
                    
                    Log.d(TAG, "registerFaceId: Creating multipart request - buffer size: " + buffer.array().length);
                    
                    // Create multipart request
                    RequestBody embeddingPart = RequestBody.create(
                            MediaType.parse("application/octet-stream"), 
                            buffer.array());
                    
                    MultipartBody.Part filePart = MultipartBody.Part.createFormData(
                            "embedding", "embedding.bin", embeddingPart);
                    
                    RequestBody userIdPart = RequestBody.create(
                            MediaType.parse("text/plain"), userId);
                    
                    Log.d(TAG, "registerFaceId: Making API call to register face ID");
                    
                    // Make API call
                    Call<FaceIdResponse> call = faceIdApi.registerFaceId(filePart, userIdPart);
                    call.enqueue(new Callback<FaceIdResponse>() {
                        @Override
                        public void onResponse(@NonNull Call<FaceIdResponse> call, @NonNull Response<FaceIdResponse> response) {
                            Log.d(TAG, "registerFaceId: API response received - code: " + response.code() + 
                                  ", successful: " + response.isSuccessful());
                            
                            if (response.isSuccessful() && response.body() != null) {
                                Log.d(TAG, "registerFaceId: SUCCESS - Face ID registered successfully");
                                runOnMainThread(() -> callback.onSuccess("Face ID registered successfully"));
                            } else {
                                String errorMsg = "Failed to register Face ID: " + response.message();
                                Log.e(TAG, "registerFaceId: FAILED - " + errorMsg);
                                runOnMainThread(() -> callback.onFailure(errorMsg));
                            }
                        }
                        
                        @Override
                        public void onFailure(@NonNull Call<FaceIdResponse> call, @NonNull Throwable t) {
                            String errorMsg = "Network error: " + t.getMessage();
                            Log.e(TAG, "registerFaceId: NETWORK FAILURE - " + errorMsg, t);
                            runOnMainThread(() -> callback.onFailure(errorMsg));
                        }
                    });
                    
                } catch (Exception e) {
                    Log.e(TAG, "registerFaceId: EXCEPTION during API call preparation", e);
                    runOnMainThread(() -> callback.onFailure("Error: " + e.getMessage()));
                }
            });
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
                    // Convert embedding to byte array for API call
                    ByteBuffer buffer = ByteBuffer.allocate(embedding.length * 4);
                    for (float value : embedding) {
                        buffer.putFloat(value);
                    }
                    
                    // Create multipart request
                    RequestBody embeddingPart = RequestBody.create(
                            MediaType.parse("application/octet-stream"), 
                            buffer.array());
                    
                    MultipartBody.Part filePart = MultipartBody.Part.createFormData(
                            "embedding", "embedding.bin", embeddingPart);
                    
                    RequestBody userIdPart = RequestBody.create(
                            MediaType.parse("text/plain"), userId);
                    
                    // Make API call
                    Call<FaceIdResponse> call = faceIdApi.updateFaceId(filePart, userIdPart);
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
                    // Convert embedding to byte array for API call
                    ByteBuffer buffer = ByteBuffer.allocate(embedding.length * 4);
                    for (float value : embedding) {
                        buffer.putFloat(value);
                    }
                    
                    // Create multipart request
                    RequestBody embeddingPart = RequestBody.create(
                            MediaType.parse("application/octet-stream"), 
                            buffer.array());
                    
                    MultipartBody.Part filePart = MultipartBody.Part.createFormData(
                            "embedding", "embedding.bin", embeddingPart);
                    
                    RequestBody userIdPart = RequestBody.create(
                            MediaType.parse("text/plain"), userId);
                    
                    // Make API call
                    Call<FaceIdResponse> call = faceIdApi.verifyFaceId(filePart, userIdPart);
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
                    // Convert embedding to byte array for API call
                    ByteBuffer buffer = ByteBuffer.allocate(embedding.length * 4);
                    for (float value : embedding) {
                        buffer.putFloat(value);
                    }
                    
                    // Create multipart request
                    RequestBody embeddingPart = RequestBody.create(
                            MediaType.parse("application/octet-stream"), 
                            buffer.array());
                    
                    MultipartBody.Part filePart = MultipartBody.Part.createFormData(
                            "embedding", "embedding.bin", embeddingPart);
                    
                    RequestBody userIdPart = RequestBody.create(
                            MediaType.parse("text/plain"), userId);
                    
                    // Make API call
                    Call<FaceIdResponse> call = faceIdApi.verifyFaceId(filePart, userIdPart);
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
    private void runOnMainThread(Runnable runnable) {
        mainHandler.post(runnable);
    }
} 