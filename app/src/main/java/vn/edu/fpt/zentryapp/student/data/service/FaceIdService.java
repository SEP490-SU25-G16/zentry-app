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

/**
 * Service to handle Face ID operations including:
 * - Face detection
 * - Anti-spoofing
 * - Face embedding generation
 * - Communication with backend for verification
 */
public class FaceIdService {
    private static final String TAG = "FaceIdService";
    
    private final Context context;
    private FaceDetector faceDetector;
    private FaceEmbedding faceEmbedding;
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
        if (isInitialized) return true;
        
        try {
            // Kiểm tra xem tất cả model đã load xong chưa (với timeout 0 để không block)
            boolean allLoaded = modelLoadLatch.await(0, TimeUnit.MILLISECONDS);
            isInitialized = allLoaded;
            return allLoaded;
        } catch (InterruptedException e) {
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
    
    /**
     * Process a bitmap to detect face, check for spoofing, and generate embedding
     */
    public void processFaceImage(Bitmap bitmap, FaceDetectionCallback callback) {
        // Check if models are initialized
        if (!isInitialized()) {
            awaitInitialization(5000, 
                () -> processFaceImage(bitmap, callback),
                () -> runOnMainThread(() -> callback.onError("Face detection models not initialized yet"))
            );
            return;
        }
        
        executor.execute(() -> {
            try {
                // Step 1: Detect face
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

                Log.d(TAG, "processFaceImage: Face detected with bounding box: " + boundingBox.toString());

                // Step 2: Check for spoofing using async method
                faceSpoofDetector.detectSpoofAsync(bitmap, boundingBox, spoofResult -> {
                    Log.d(TAG, "processFaceImage: Spoof detection result - isSpoof: " + spoofResult.isSpoof() + ", score: " + spoofResult.getScore());

                    if (spoofResult.isSpoof()) {
                        Log.d(TAG, "processFaceImage: Spoof detected");
                        callback.onError("Spoof detected! Please use a real face.");
                        return;
                    }

                    // Return the detected face
                    Log.d(TAG, "processFaceImage: Face verified as real, proceeding");
                    callback.onFaceDetected(faceBitmap, boundingBox);
                });

            } catch (Exception e) {
                Log.e(TAG, "Error processing face image", e);
                runOnMainThread(() -> callback.onError("Error processing face: " + e.getMessage()));
            }
        });
    }
    
    /**
     * Process a frame continuously for zero-touch face recognition
     * @param bitmap Current frame bitmap
     * @param callback Callback for continuous processing results
     * @return true if processing was started, false if already processing
     */
    public boolean processContinuousFrame(Bitmap bitmap, ContinuousProcessingCallback callback) {
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
                
                // Step 2: Check for spoofing
                faceSpoofDetector.detectSpoofAsync(bitmap, boundingBox, spoofResult -> {
                    Log.d(TAG, "processContinuousFrame: Spoof detection completed - isSpoof: " + spoofResult.isSpoof() + ", score: " + spoofResult.getScore());
                    runOnMainThread(() -> {
                        Log.d(TAG, "processContinuousFrame: Calling callback with isSpoof: " + spoofResult.isSpoof() + ", score: " + spoofResult.getScore());
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
     * Capture and process a stable face for registration
     * @param bitmap Bitmap containing the face
     * @param boundingBox Bounding box of the face
     * @param userId User ID for registration
     * @param callback Callback for registration result
     */
    public void captureAndRegisterFace(Bitmap bitmap, Rect boundingBox, String userId, FaceIdCallback callback) {
        executor.execute(() -> {
            try {
                // Crop the face from the bitmap
                Bitmap faceBitmap = Bitmap.createBitmap(
                        bitmap, 
                        boundingBox.left, 
                        boundingBox.top, 
                        boundingBox.width(), 
                        boundingBox.height()
                );
                
                // Register the face
                registerFaceId(faceBitmap, userId, callback);
                
            } catch (Exception e) {
                Log.e(TAG, "Error capturing face for registration", e);
                runOnMainThread(() -> callback.onFailure("Error capturing face: " + e.getMessage()));
            }
        });
    }
    
    /**
     * Register a new face ID by sending the embedding to backend
     */
    public void registerFaceId(Bitmap faceBitmap, String userId, FaceIdCallback callback) {
        // Check if models are initialized
        if (!isInitialized()) {
            awaitInitialization(5000, 
                () -> registerFaceId(faceBitmap, userId, callback),
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
                    Call<FaceIdResponse> call = faceIdApi.registerFaceId(filePart, userIdPart);
                    call.enqueue(new Callback<FaceIdResponse>() {
                        @Override
                        public void onResponse(@NonNull Call<FaceIdResponse> call, @NonNull Response<FaceIdResponse> response) {
                            if (response.isSuccessful() && response.body() != null) {
                                runOnMainThread(() -> callback.onSuccess("Face ID registered successfully"));
                            } else {
                                runOnMainThread(() -> callback.onFailure("Failed to register Face ID: " + response.message()));
                            }
                        }
                        
                        @Override
                        public void onFailure(@NonNull Call<FaceIdResponse> call, @NonNull Throwable t) {
                            runOnMainThread(() -> callback.onFailure("Network error: " + t.getMessage()));
                        }
                    });
                    
                } catch (Exception e) {
                    Log.e(TAG, "Error registering face ID", e);
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
     * Get FaceSpoofDetector instance for advanced spoof detection management
     * @return FaceSpoofDetector instance or null if not initialized
     */
    public FaceSpoofDetector getFaceSpoofDetector() {
        return faceSpoofDetector;
    }
    
    /**
     * Helper method to run code on the main thread
     */
    private void runOnMainThread(Runnable runnable) {
        mainHandler.post(runnable);
    }
} 