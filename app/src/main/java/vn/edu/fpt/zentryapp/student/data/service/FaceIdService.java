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
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

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
    private final FaceDetector faceDetector;
    private final FaceEmbedding faceEmbedding;
    private final FaceSpoofDetector faceSpoofDetector;
    private final FaceIdApi faceIdApi;
    private final Executor executor;
    private final Handler mainHandler;
    
    public FaceIdService(Context context) {
        this.context = context;
        this.faceDetector = new FaceDetector(context);
        this.faceEmbedding = new FaceEmbedding(context);
        this.faceSpoofDetector = new FaceSpoofDetector(context);
        this.faceIdApi = ApiClient.getClient(context).create(FaceIdApi.class);
        this.executor = Executors.newSingleThreadExecutor();
        this.mainHandler = new Handler(Looper.getMainLooper());
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
    
    /**
     * Process a bitmap to detect face, check for spoofing, and generate embedding
     */
    public void processFaceImage(Bitmap bitmap, FaceDetectionCallback callback) {
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
     * Register a new face ID by sending the embedding to backend
     */
    public void registerFaceId(Bitmap faceBitmap, String userId, FaceIdCallback callback) {
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