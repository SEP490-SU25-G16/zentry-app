package vn.edu.fpt.zentryapp.faceid.adapter.workers;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

import java.io.File;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import vn.edu.fpt.zentryapp.faceid.data.service.FaceIdService;
import vn.edu.fpt.zentryapp.faceid.data.service.FaceIdServiceManager;

/**
 * Background Worker để sync Face Embedding với server
 * Chạy riêng biệt với UI flow để tránh blocking user
 */
public class FaceEmbeddingSyncWorker extends Worker {
    private static final String TAG = "FaceEmbeddingSyncWorker";
    
    public static final String KEY_USER_ID = "user_id";
    public static final String KEY_BITMAP_PATH = "bitmap_path";
    
    public FaceEmbeddingSyncWorker(@NonNull Context context, @NonNull WorkerParameters workerParams) {
        super(context, workerParams);
    }
    
    @NonNull
    @Override
    public Result doWork() {
        Log.d(TAG, "Starting face embedding sync work");
        
        String userId = getInputData().getString(KEY_USER_ID);
        String bitmapPath = getInputData().getString(KEY_BITMAP_PATH);
        
        if (userId == null || bitmapPath == null) {
            Log.e(TAG, "Missing required input data");
            return Result.failure();
        }
        
        try {
            // Load bitmap from file
            Bitmap bitmap = loadBitmapFromPath(bitmapPath);
            if (bitmap == null) {
                Log.e(TAG, "Failed to load bitmap from path: " + bitmapPath);
                return Result.failure();
            }
            
            // Initialize FaceIdService if needed
            FaceIdService faceIdService = getFaceIdService();
            if (faceIdService == null) {
                Log.e(TAG, "Failed to initialize FaceIdService");
                return Result.failure();
            }
            
            // Sync embedding with server
            boolean syncResult = syncEmbeddingWithServer(faceIdService, bitmap, userId);
            
            // Cleanup bitmap file
            cleanupBitmapFile(bitmapPath);
            
            if (syncResult) {
                Log.d(TAG, "Face embedding sync completed successfully");
                return Result.success();
            } else {
                Log.w(TAG, "Face embedding sync failed, will retry");
                return Result.retry();
            }
            
        } catch (Exception e) {
            Log.e(TAG, "Exception during face embedding sync", e);
            return Result.failure();
        }
    }
    
    private Bitmap loadBitmapFromPath(String bitmapPath) {
        try {
            File file = new File(bitmapPath);
            if (!file.exists()) {
                Log.e(TAG, "Bitmap file does not exist: " + bitmapPath);
                return null;
            }
            
            return BitmapFactory.decodeFile(bitmapPath);
        } catch (Exception e) {
            Log.e(TAG, "Error loading bitmap from path: " + bitmapPath, e);
            return null;
        }
    }
    
    private FaceIdService getFaceIdService() {
        final FaceIdService[] serviceRef = new FaceIdService[1];
        final CountDownLatch latch = new CountDownLatch(1);
        final AtomicBoolean initSuccess = new AtomicBoolean(false);
        
        // Initialize service synchronously in worker thread
        FaceIdServiceManager.getInstance().initialize(getApplicationContext(), new FaceIdServiceManager.InitCallback() {
            @Override
            public void onInitialized(FaceIdService service) {
                serviceRef[0] = service;
                initSuccess.set(true);
                latch.countDown();
            }
            
            @Override
            public void onError(String message) {
                Log.e(TAG, "FaceIdService initialization error: " + message);
                latch.countDown();
            }
        });
        
        try {
            // Wait up to 10 seconds for initialization
            boolean completed = latch.await(10, TimeUnit.SECONDS);
            if (completed && initSuccess.get()) {
                return serviceRef[0];
            }
        } catch (InterruptedException e) {
            Log.e(TAG, "Interrupted while waiting for FaceIdService initialization", e);
        }
        
        return null;
    }
    
    private boolean syncEmbeddingWithServer(FaceIdService faceIdService, Bitmap bitmap, String userId) {
        final AtomicBoolean syncSuccess = new AtomicBoolean(false);
        final CountDownLatch latch = new CountDownLatch(1);
        
        // Call registerFaceId which sends embedding to server
        faceIdService.registerFaceId(bitmap, userId, new FaceIdService.FaceIdCallback() {
            @Override
            public void onSuccess(String message) {
                Log.d(TAG, "Face embedding sync successful: " + message);
                syncSuccess.set(true);
                latch.countDown();
            }
            
            @Override
            public void onFailure(String errorMessage) {
                Log.e(TAG, "Face embedding sync failed: " + errorMessage);
                latch.countDown();
            }
        });
        
        try {
            // Wait up to 30 seconds for sync completion
            boolean completed = latch.await(30, TimeUnit.SECONDS);
            return completed && syncSuccess.get();
        } catch (InterruptedException e) {
            Log.e(TAG, "Interrupted while waiting for embedding sync", e);
            return false;
        }
    }
    
    private void cleanupBitmapFile(String bitmapPath) {
        try {
            File file = new File(bitmapPath);
            if (file.exists()) {
                boolean deleted = file.delete();
                Log.d(TAG, "Bitmap file cleanup: " + (deleted ? "success" : "failed"));
            }
        } catch (Exception e) {
            Log.e(TAG, "Error cleaning up bitmap file: " + bitmapPath, e);
        }
    }
}