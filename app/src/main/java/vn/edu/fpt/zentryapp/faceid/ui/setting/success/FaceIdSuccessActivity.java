package vn.edu.fpt.zentryapp.faceid.ui.setting.success;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

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
 * Hỗ trợ 2 trường hợp:
 * 1. Sau khi đăng ký thành công - không có button update
 * 2. Kiểm tra trạng thái thành công - có button update
 */
public class FaceIdSuccessActivity extends AppCompatActivity {
    private static final String TAG = "FaceIdSuccessActivity";
    
    private static final String EXTRA_USER_ID = "user_id";
    private static final String EXTRA_SUCCESS_MESSAGE = "success_message";
    private static final String EXTRA_BITMAP_PATH = "bitmap_path";
    private static final String EXTRA_ACTION = "action"; // "register" | "update" | "status_check"
    private static final String EXTRA_SHOW_UPDATE_BUTTON = "show_update_button"; // true để hiển thị button update
    private static final String EXTRA_USER_NAME = "user_name"; // Thêm biến mới để lấy tên người dùng
    
    private ActivityFaceIdSuccessBinding binding;
    private Handler handler = new Handler(Looper.getMainLooper());
    private WorkManager workManager;
    
    public static Intent createIntent(Context context, String userId, String successMessage, String bitmapPath) {
        Intent intent = new Intent(context, FaceIdSuccessActivity.class);
        intent.putExtra(EXTRA_USER_ID, userId);
        intent.putExtra(EXTRA_SUCCESS_MESSAGE, successMessage);
        intent.putExtra(EXTRA_BITMAP_PATH, bitmapPath);
        intent.putExtra(EXTRA_ACTION, "register");
        intent.putExtra(EXTRA_SHOW_UPDATE_BUTTON, false); // Sau khi đăng ký thành công, không hiển thị button update
        return intent;
    }

    public static Intent createIntent(Context context, String userId, String successMessage, String bitmapPath, String action) {
        Intent intent = createIntent(context, userId, successMessage, bitmapPath);
        intent.putExtra(EXTRA_ACTION, action);
        // ✅ FIX: Hiển thị button update khi update thành công, chỉ ẩn khi đăng ký thành công
        boolean showUpdateButton = !"register".equals(action); // Hiển thị button update cho update và verify, ẩn cho register
        intent.putExtra(EXTRA_SHOW_UPDATE_BUTTON, showUpdateButton);
        return intent;
    }
    
    // ✅ NEW: Intent cho kiểm tra trạng thái (có button update)
    public static Intent createStatusCheckIntent(Context context, String userId, String successMessage) {
        Intent intent = new Intent(context, FaceIdSuccessActivity.class);
        intent.putExtra(EXTRA_USER_ID, userId);
        intent.putExtra(EXTRA_SUCCESS_MESSAGE, successMessage);
        intent.putExtra(EXTRA_ACTION, "status_check");
        intent.putExtra(EXTRA_SHOW_UPDATE_BUTTON, true); // Hiển thị button update khi kiểm tra trạng thái
        return intent;
    }
    
    // ✅ NEW: Intent cho đăng ký thành công với userName
    public static Intent createRegisterSuccessIntent(Context context, String userId, String userName, String bitmapPath) {
        Intent intent = new Intent(context, FaceIdSuccessActivity.class);
        intent.putExtra(EXTRA_USER_ID, userId);
        intent.putExtra(EXTRA_USER_NAME, userName);
        intent.putExtra(EXTRA_BITMAP_PATH, bitmapPath);
        intent.putExtra(EXTRA_ACTION, "register");
        intent.putExtra(EXTRA_SHOW_UPDATE_BUTTON, false);
        return intent;
    }
    
    // ✅ NEW: Intent cho cập nhật thành công với userName
    public static Intent createUpdateSuccessIntent(Context context, String userId, String userName, String bitmapPath) {
        Intent intent = new Intent(context, FaceIdSuccessActivity.class);
        intent.putExtra(EXTRA_USER_ID, userId);
        intent.putExtra(EXTRA_USER_NAME, userName);
        intent.putExtra(EXTRA_BITMAP_PATH, bitmapPath);
        intent.putExtra(EXTRA_ACTION, "update");
        intent.putExtra(EXTRA_SHOW_UPDATE_BUTTON, false);
        return intent;
    }
    
    // ✅ NEW: Intent cho xác thực thành công với userName
    public static Intent createVerifySuccessIntent(Context context, String userId, String userName) {
        Intent intent = new Intent(context, FaceIdSuccessActivity.class);
        intent.putExtra(EXTRA_USER_ID, userId);
        intent.putExtra(EXTRA_USER_NAME, userName);
        intent.putExtra(EXTRA_ACTION, "verify");
        intent.putExtra(EXTRA_SHOW_UPDATE_BUTTON, false);
        return intent;
    }
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        // ✅ NEW: Kiểm tra xem Activity đã tồn tại chưa
        if (isTaskRoot() && getIntent().hasCategory(Intent.CATEGORY_LAUNCHER)) {
            // Nếu đây là root activity, kiểm tra xem có cần finish không
            finish();
            return;
        }
        
        binding = ActivityFaceIdSuccessBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        
        workManager = WorkManager.getInstance(this);
        
        setupUI();
        
        // ✅ NEW: Gọi setupClickListeners sau khi setupUI
        setupClickListeners();
        
        // Chỉ start background sync khi có bitmap path (trường hợp đăng ký/update)
        String bitmapPath = getIntent().getStringExtra(EXTRA_BITMAP_PATH);
        if (bitmapPath != null && !bitmapPath.isEmpty()) {
            startBackgroundSync();
        }
        
        // ✅ NEW: Chỉ auto-finish khi đăng ký/update thành công, không áp dụng cho status check
        String action = getIntent().getStringExtra(EXTRA_ACTION);
        if ("register".equals(action) || "update".equals(action)) {
            // Auto finish after 10 seconds chỉ khi đăng ký/update thành công
            // ✅ NEW: Sử dụng finishWithResult để quay về setting
            handler.postDelayed(this::finishWithResult, 10000);
            Log.d(TAG, "✅ Auto-finish timer đã được set (10 giây) cho action: " + action + " - sẽ quay về setting");
        } else if ("status_check".equals(action)) {
            // Không auto-finish cho status check từ setting - user có thể xem và tương tác tự do
            Log.d(TAG, "✅ Không set auto-finish timer cho status check - user có thể tương tác tự do");
        } else {
            // Fallback: không set timer
            Log.d(TAG, "⚠️ Action không xác định: " + action + " - không set auto-finish timer");
        }
    }
    
    private void setupUI() {
        String successMessage = getIntent().getStringExtra(EXTRA_SUCCESS_MESSAGE);
        if (successMessage != null && !successMessage.isEmpty()) {
            binding.tvSuccessSubtitle.setText(successMessage);
        }
        
        // ✅ NEW: Luôn hiển thị button "Tiếp tục" và ẩn button "Update Face ID"
        binding.llBottomButtons.setVisibility(View.VISIBLE);
        binding.btnContinue.setVisibility(View.VISIBLE);
        binding.btnUpdateFaceId.setVisibility(View.GONE);
        Log.d(TAG, "✅ Hiển thị button Tiếp tục, ẩn button Update Face ID");
        
        // ✅ NEW: Cập nhật tiêu đề và thông báo theo action
        updateTitleAndMessage();
        
        // Animate success icon
        animateSuccessIcon();

    }
    
    // ✅ NEW: Cập nhật tiêu đề và thông báo theo action
    private void updateTitleAndMessage() {
        String action = getIntent().getStringExtra(EXTRA_ACTION);
        String userName = getIntent().getStringExtra(EXTRA_USER_NAME);
        
        if (userName == null || userName.isEmpty()) {
            userName = "Bạn";
        }
        
        switch (action) {
            case "register":
                binding.tvSuccessTitle.setText("Chúc mừng bạn đã đăng ký Face ID thành công!");
                binding.tvSuccessSubtitle.setText("Giờ đây " + userName + " có thể sử dụng Face ID để nhanh chóng điểm danh và truy cập ứng dụng một cách an toàn.");
                break;
                
            case "update":
                binding.tvSuccessTitle.setText("Chúc mừng bạn đã cập nhật Face ID thành công!");
                binding.tvSuccessSubtitle.setText("Face ID của " + userName + " đã được cập nhật với thông tin mới nhất.");
                break;
                
            case "verify":
                binding.tvSuccessTitle.setText("Chúc mừng bạn đã xác thực Face ID thành công!");
                binding.tvSuccessSubtitle.setText("Face ID của " + userName + " đã được xác thực và hoạt động bình thường.");
                break;
                
            default:
                // Giữ nguyên text mặc định
                break;
        }
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
                return;
            }
        } catch (Exception e) {
            Log.w(TAG, "Error checking bitmap file, skip background sync", e);
            return;
        }

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
                });
        
        // Enqueue work
        workManager.enqueue(syncWork);
        
        Log.d(TAG, "Background sync work enqueued");
    }

    private void animateSuccessIcon() {
        // ✅ NEW: Animation đẹp mắt hơn cho success icon
        binding.ivSuccessIconContainer.setScaleX(0f);
        binding.ivSuccessIconContainer.setScaleY(0f);
        binding.ivSuccessIconContainer.setAlpha(0f);
        
        // Animate icon container
        binding.ivSuccessIconContainer.animate()
                .scaleX(1f)
                .scaleY(1f)
                .alpha(1f)
                .setDuration(800)
                .setStartDelay(300)
                .setInterpolator(new android.view.animation.OvershootInterpolator(1.2f))
                .start();
        
        // Animate icon inside
        binding.ivSuccessIcon.setScaleX(0f);
        binding.ivSuccessIcon.setScaleY(0f);
        binding.ivSuccessIcon.setRotation(180f);
        
        binding.ivSuccessIcon.animate()
                .scaleX(1f)
                .scaleY(1f)
                .rotation(0f)
                .setDuration(600)
                .setStartDelay(800)
                .setInterpolator(new android.view.animation.BounceInterpolator())
                .start();
        
        // Animate main content entrance
        binding.llMainContent.setTranslationY(100f);
        binding.llMainContent.setAlpha(0f);
        
        binding.llMainContent.animate()
                .translationY(0f)
                .alpha(1f)
                .setDuration(1000)
                .setStartDelay(200)
                .setInterpolator(new android.view.animation.DecelerateInterpolator())
                .start();
    }
    
    private void finishWithResult() {
        // Save Face ID registration status
        getSharedPreferences("prefs", 0)
                .edit()
                .putBoolean("faceid_registered", true)
                .putLong("faceid_registered_time", System.currentTimeMillis())
                .apply();
        
        // ✅ NEW: Quay về màn hình setting thay vì màn liveness
        Log.d(TAG, "✅ Quay về màn hình setting");
        
        // ✅ NEW: Quay về màn hình setting bằng cách finish tất cả Face ID activities
        // Sử dụng Intent với flags để clear activity stack và quay về MainActivity
        Intent backToMainIntent = new Intent(this, vn.edu.fpt.zentryapp.MainActivity.class);
        backToMainIntent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        backToMainIntent.putExtra("navigate_to", "student_settings");
        startActivity(backToMainIntent);
        
        // Finish activity hiện tại
        finish();
    }
    
    private void resetAutoFinishTimer() {
        // Xóa timer cũ
        handler.removeCallbacksAndMessages(null);
        
        // Chỉ set timer mới cho register/update, không set cho status check
        String action = getIntent().getStringExtra(EXTRA_ACTION);
        if ("register".equals(action) || "update".equals(action)) {
            // ✅ NEW: Sử dụng finishWithResult để quay về setting
            handler.postDelayed(this::finishWithResult, 10000);
            Log.d(TAG, "✅ Auto-finish timer đã được reset cho action: " + action + " - sẽ quay về setting");
        }
    }

    private void setupClickListeners() {
        binding.ivBack.setOnClickListener(v -> finishWithResult());
        
        // ✅ NEW: Xử lý button "Tiếp tục" - quay về setting
        binding.btnContinue.setOnClickListener(v -> {
            Log.d(TAG, "✅ Button Tiếp tục được click");
            // ✅ NEW: Reset auto-finish timer khi user tương tác
            resetAutoFinishTimer();
            
            // Quay về setting thay vì màn liveness
            finishWithResult();
        });
        
        // ✅ NEW: Xử lý button Update Face ID (nếu cần)
        binding.btnUpdateFaceId.setOnClickListener(v -> {
            Log.d(TAG, "✅ Button Update Face ID được click");
            // ✅ NEW: Reset auto-finish timer khi user tương tác
            resetAutoFinishTimer();
            try {
                Intent updateIntent = new Intent(this, vn.edu.fpt.zentryapp.faceid.ui.setting.StudentSettingUpdateFaceIdActivity.class);
                startActivity(updateIntent);
                Log.d(TAG, "✅ Đã launch StudentSettingUpdateFaceIdActivity");
            } catch (Exception e) {
                Log.e(TAG, "❌ Lỗi khi launch Update Face ID Activity", e);
                Toast.makeText(this, "Không thể mở màn hình Update Face ID", Toast.LENGTH_SHORT).show();
            }
        });
        
        // ✅ NEW: Reset timer khi user chạm vào màn hình
        binding.getRoot().setOnTouchListener((v, event) -> {
            resetAutoFinishTimer();
            return false; // Không consume event
        });
    }
    
    @Override
    protected void onDestroy() {
        super.onDestroy();
        handler.removeCallbacksAndMessages(null);
        binding = null;
    }
}