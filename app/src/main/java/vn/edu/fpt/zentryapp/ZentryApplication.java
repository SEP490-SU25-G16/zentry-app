package vn.edu.fpt.zentryapp;

import android.app.Application;
import android.util.Log;

import vn.edu.fpt.zentryapp.faceid.data.service.FaceIdService;
import vn.edu.fpt.zentryapp.faceid.data.service.FaceIdServiceManager;

public class ZentryApplication extends Application {
    private static final String TAG = "ZentryApplication";
    
    @Override
    public void onCreate() {
        super.onCreate();
        
        // Preload FaceIdService ngay khi app khởi động
        preloadFaceIdService();
    }
    
    private void preloadFaceIdService() {
        // Preload trên background thread để không ảnh hưởng đến thời gian khởi động app
        new Thread(() -> {
            try {
                // Chờ 1 giây sau khi app khởi động để không ảnh hưởng đến hiệu suất khởi động
                Thread.sleep(1000);
                
                // Bắt đầu khởi tạo service
                FaceIdServiceManager.getInstance().initialize(getApplicationContext(), 
                        new FaceIdServiceManager.InitCallback() {
                            @Override
                            public void onInitialized(FaceIdService service) {
                                Log.d(TAG, "FaceIdService preloaded successfully");
                            }
                            
                            @Override
                            public void onError(String message) {
                                Log.e(TAG, "Failed to preload FaceIdService: " + message);
                            }
                        });
            } catch (Exception e) {
                Log.e(TAG, "Error in preloadFaceIdService", e);
            }
        }).start();
    }
} 