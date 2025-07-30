package vn.edu.fpt.zentryapp.student.data.service;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Singleton manager for FaceIdService to handle preloading and async initialization
 */
public class FaceIdServiceManager {
    private static final String TAG = "FaceIdServiceManager";
    
    private static FaceIdServiceManager instance;
    private FaceIdService faceIdService;
    private boolean isInitializing = false;
    private boolean isInitialized = false;
    
    private final List<InitCallback> pendingCallbacks = new ArrayList<>();
    
    public interface InitCallback {
        void onInitialized(FaceIdService service);
        void onError(String message);
    }
    
    private FaceIdServiceManager() {
        // Private constructor
    }
    
    public static synchronized FaceIdServiceManager getInstance() {
        if (instance == null) {
            instance = new FaceIdServiceManager();
        }
        return instance;
    }
    
    public void initialize(Context context, InitCallback callback) {
        if (isInitialized) {
            // Đã khởi tạo xong, trả về service ngay lập tức
            callback.onInitialized(faceIdService);
            return;
        }
        
        // Thêm callback vào hàng đợi
        synchronized (pendingCallbacks) {
            pendingCallbacks.add(callback);
            
            // Nếu đang khởi tạo, không làm gì thêm
            if (isInitializing) {
                return;
            }
            
            // Đánh dấu đang khởi tạo
            isInitializing = true;
        }
        
        // Khởi tạo service trên background thread
        ExecutorService executor = Executors.newSingleThreadExecutor();
        Handler mainHandler = new Handler(Looper.getMainLooper());
        
        executor.execute(() -> {
            try {
                // Khởi tạo service trên background thread
                faceIdService = new FaceIdService(context.getApplicationContext());
                
                // Đánh dấu đã khởi tạo xong
                isInitialized = true;
                isInitializing = false;
                
                // Gọi tất cả callback đang chờ
                mainHandler.post(() -> {
                    synchronized (pendingCallbacks) {
                        for (InitCallback cb : pendingCallbacks) {
                            cb.onInitialized(faceIdService);
                        }
                        pendingCallbacks.clear();
                    }
                });
                
            } catch (Exception e) {
                Log.e(TAG, "Error initializing FaceIdService", e);
                isInitializing = false;
                
                // Thông báo lỗi cho tất cả callback
                mainHandler.post(() -> {
                    synchronized (pendingCallbacks) {
                        for (InitCallback cb : pendingCallbacks) {
                            cb.onError("Failed to initialize Face ID service: " + e.getMessage());
                        }
                        pendingCallbacks.clear();
                    }
                });
            }
        });
    }
    
    public boolean isInitialized() {
        return isInitialized;
    }
    
    public FaceIdService getService() {
        if (!isInitialized) {
            throw new IllegalStateException("FaceIdService not initialized. Call initialize() first.");
        }
        return faceIdService;
    }
} 