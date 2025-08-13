package vn.edu.fpt.zentryapp.faceid.ui.setting.success;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.work.Data;
import androidx.work.OneTimeWorkRequest;
import androidx.work.WorkInfo;
import androidx.work.WorkManager;

import vn.edu.fpt.zentryapp.auth.client.AuthManager;
import vn.edu.fpt.zentryapp.databinding.ActivityFaceIdSuccessBinding;
import vn.edu.fpt.zentryapp.faceid.adapter.workers.FaceEmbeddingSyncWorker;

/**
 * Success Activity cho Face ID Registration
 * Hiển thị thành công và handle background sync
 */
public class FaceIdSuccessActivity extends AppCompatActivity {
    private static final String TAG = "FaceIdSuccessActivity";
    
    private static final String EXTRA_USER_ID = "user_id";
    private static final String EXTRA_SUCCESS_MESSAGE = "success_message";
    private static final String EXTRA_BITMAP_PATH = "bitmap_path";
    private static final String EXTRA_ACTION = "action"; // "register" | "update"
    
    private ActivityFaceIdSuccessBinding binding;
    private Handler handler = new Handler(Looper.getMainLooper());
    private WorkManager workManager;
    
    public static Intent createIntent(Context context, String userId, String successMessage, String bitmapPath) {
        Intent intent = new Intent(context, FaceIdSuccessActivity.class);
        intent.putExtra(EXTRA_USER_ID, userId);
        intent.putExtra(EXTRA_SUCCESS_MESSAGE, successMessage);
        intent.putExtra(EXTRA_BITMAP_PATH, bitmapPath);
        return intent;
    }

    public static Intent createIntent(Context context, String userId, String successMessage, String bitmapPath, String action) {
        Intent intent = createIntent(context, userId, successMessage, bitmapPath);
        intent.putExtra(EXTRA_ACTION, action);
        return intent;
    }
    
    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityFaceIdSuccessBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        
        workManager = WorkManager.getInstance(this);
        
        setupUI();
        setupClickListeners();
        startBackgroundSync();
        
        // Auto finish after 10 seconds if user doesn't interact
        handler.postDelayed(this::finishWithResult, 10000);
    }
    
    private void setupUI() {
        String successMessage = getIntent().getStringExtra(EXTRA_SUCCESS_MESSAGE);
        if (successMessage != null && !successMessage.isEmpty()) {
            binding.tvSuccessSubtitle.setText(successMessage);
        }
        
        // Animate success icon
        animateSuccessIcon();
    }
    
    private void setupClickListeners() {
        binding.ivBack.setOnClickListener(v -> finishWithResult());
    }
    
    private void startBackgroundSync() {
        String userId = getIntent().getStringExtra(EXTRA_USER_ID);
        String bitmapPath = getIntent().getStringExtra(EXTRA_BITMAP_PATH);
        
        if (userId == null || bitmapPath == null) {
            Log.w(TAG, "Missing data for background sync");
            return;
        }
        // Guard: ensure file exists before enqueueing work
        try {
            java.io.File f = new java.io.File(bitmapPath);
            if (!f.exists()) {
                Log.w(TAG, "Bitmap file not found, skip background sync: " + bitmapPath);
                if (binding != null) {
                    binding.progressSync.setVisibility(View.GONE);
                    binding.tvSyncStatus.setText("Ready");
                    binding.pbSync.setVisibility(View.GONE);
                }
                return;
            }
        } catch (Exception e) {
            Log.w(TAG, "Error checking bitmap file, skip background sync", e);
            return;
        }
        
        // Show sync progress
        binding.progressSync.setVisibility(View.VISIBLE);
        
        // Create work request
        String action = getIntent().getStringExtra(EXTRA_ACTION);
        Data inputData = new Data.Builder()
                .putString(FaceEmbeddingSyncWorker.KEY_USER_ID, userId)
                .putString(FaceEmbeddingSyncWorker.KEY_BITMAP_PATH, bitmapPath)
                .putString(FaceEmbeddingSyncWorker.KEY_ACTION, action)
                .build();
        
        OneTimeWorkRequest syncWork = new OneTimeWorkRequest.Builder(FaceEmbeddingSyncWorker.class)
                .setInputData(inputData)
                .build();
        
        // Observe work progress
        workManager.getWorkInfoByIdLiveData(syncWork.getId())
                .observe(this, workInfo -> {
                    if (workInfo != null) {
                        handleSyncWorkInfo(workInfo);
                    }
                });
        
        // Enqueue work
        workManager.enqueue(syncWork);
        
        Log.d(TAG, "Background sync work enqueued");
    }
    
    private void handleSyncWorkInfo(WorkInfo workInfo) {
        switch (workInfo.getState()) {
            case RUNNING:
                binding.tvSyncStatus.setText("Syncing with server...");
                break;
                
            case SUCCEEDED:
                binding.tvSyncStatus.setText("Sync completed successfully");
                binding.pbSync.setVisibility(View.GONE);
                
                // Hide progress after 2 seconds
                handler.postDelayed(() -> {
                    if (binding != null) {
                        binding.progressSync.setVisibility(View.GONE);
                    }
                }, 2000);
                break;
                
            case FAILED:
                binding.tvSyncStatus.setText("Sync failed (will retry later)");
                binding.pbSync.setVisibility(View.GONE);
                
                // Hide progress after 3 seconds
                handler.postDelayed(() -> {
                    if (binding != null) {
                        binding.progressSync.setVisibility(View.GONE);
                    }
                }, 3000);
                break;
                
            case CANCELLED:
            case BLOCKED:
            case ENQUEUED:
            default:
                // Keep showing progress
                break;
        }
    }
    
    private void animateSuccessIcon() {
        // Simple scale animation
        binding.ivSuccessIcon.setScaleX(0f);
        binding.ivSuccessIcon.setScaleY(0f);
        
        binding.ivSuccessIcon.animate()
                .scaleX(1f)
                .scaleY(1f)
                .setDuration(500)
                .setStartDelay(200)
                .start();
    }
    
    private void finishWithResult() {
        // Save Face ID registration status
        getSharedPreferences("prefs", 0)
                .edit()
                .putBoolean("faceid_registered", true)
                .putLong("faceid_registered_time", System.currentTimeMillis())
                .apply();
        
        // Set result and finish
        setResult(RESULT_OK);
        finish();
    }
    
    @Override
    protected void onDestroy() {
        super.onDestroy();
        handler.removeCallbacksAndMessages(null);
        binding = null;
    }
}