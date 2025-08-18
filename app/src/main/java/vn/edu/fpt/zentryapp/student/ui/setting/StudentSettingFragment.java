package vn.edu.fpt.zentryapp.student.ui.setting;

import android.os.Bundle;
import android.content.Context;
import android.content.SharedPreferences;
import android.text.TextUtils;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.NavController;
import androidx.navigation.fragment.NavHostFragment;

import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.content.Intent;
import android.widget.Toast;

import vn.edu.fpt.zentryapp.R;
import vn.edu.fpt.zentryapp.auth.client.AuthManager;
import vn.edu.fpt.zentryapp.auth.client.ApiClient;
import vn.edu.fpt.zentryapp.auth.models.ApiResponse;
import vn.edu.fpt.zentryapp.student.data.api.UserApiService;
import vn.edu.fpt.zentryapp.student.data.model.response.UserProfileDto;
import vn.edu.fpt.zentryapp.faceid.data.service.FaceIdRequestManager;
import vn.edu.fpt.zentryapp.faceid.data.model.response.FaceIdRequestStatusResponse;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import vn.edu.fpt.zentryapp.databinding.FragmentStudentSettingBinding;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;

public class StudentSettingFragment extends Fragment {

    private FragmentStudentSettingBinding binding;
    private NavController navController;
    private boolean hasDevice;
    private boolean hasFaceId;
    private StudentSettingViewModel viewModel;
    private boolean isFaceIdClickProcessing = false;
    
    // ✅ NEW: Face ID Request Manager để quản lý verification request lifecycle
    private FaceIdRequestManager requestManager;
    private boolean hasPendingVerificationRequest = false;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        // Inflate layout và binding view
        binding = FragmentStudentSettingBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view,
                              @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        viewModel = new ViewModelProvider(this).get(StudentSettingViewModel.class);
        AuthManager authManager = AuthManager.getInstance(requireContext());
        viewModel.init(requireContext(), authManager);

        navController = NavHostFragment.findNavController(this);

        // Khởi tạo trạng thái đăng ký thiết bị và Face ID từ lưu trữ hoặc API
        hasDevice = authManager.isDeviceRegistered();
        hasFaceId = checkIfFaceIdRegistered();

        // Fetch latest HasFaceId from API and update cache
        refreshUserProfileHasFaceId();

        // ✅ NEW: Initialize Face ID Request Manager
        initializeFaceIdRequestManager();
        
        // ✅ NEW: Check verification request status
        checkVerificationRequestStatus();

        // Xử lý click Device: điều hướng dựa trên trạng thái đăng ký thiết bị
        binding.llStudentSettingRowDevice.setOnClickListener(v -> {
            if (hasDevice) {
                navController.navigate(R.id.action_studentSetting_to_deviceInfo);
            } else {
                navController.navigate(R.id.action_studentSetting_to_deviceRegister);
            }
        });

        // Xử lý click Face ID: luôn xác nhận trạng thái mới nhất qua API trước khi điều hướng
        binding.llStudentSettingRowFaceId.setOnClickListener(v -> {
            // ✅ NEW: Check request status trước khi cho phép click
            if (requestManager != null && hasPendingVerificationRequest) {
                // Có pending request, check xem có thể verify không
                // Nếu request đã được xử lý (verified, expired, cancelled, failed), block click
                if (!isVerificationRequestActive()) {
                    // Request không thể verify → Show message và return
                    String message = getRequestStatusMessageForPendingRequest();
                    Toast.makeText(requireContext(), message, Toast.LENGTH_LONG).show();
                    Log.d("StudentSettingFragment", "Blocked Face ID click: " + message);
                    return;
                }
            }
            
            // ✅ NEW: Chống duplicate click
            if (isFaceIdClickProcessing) {
                Log.d("StudentSettingFragment", "Face ID click đang xử lý, bỏ qua");
                return;
            }
            
            // ✅ NEW: Disable click trong khi xử lý
            isFaceIdClickProcessing = true;
            binding.llStudentSettingRowFaceId.setEnabled(false);
            
            // ✅ NEW: Hiển thị loading indicator
            showFaceIdLoading(true);
            
            String userId = AuthManager.getInstance(requireContext()).getCurrentUserId();
            Log.d("StudentSettingFragment", "Face ID clicked, checking HasFaceId via API for userId=" + userId);
            
            UserApiService api = ApiClient.getClient(requireContext()).create(UserApiService.class);
            api.getUser(userId).enqueue(new Callback<ApiResponse<UserProfileDto>>() {
                @Override
                public void onResponse(@NonNull Call<ApiResponse<UserProfileDto>> call, @NonNull Response<ApiResponse<UserProfileDto>> response) {
                    boolean latestHasFaceId = hasFaceId;
                    Log.d("StudentSettingFragment", "GET /api/User response code=" + response.code() + ", success=" + response.isSuccessful());
                    if (response.isSuccessful() && response.body() != null && response.body().getData() != null) {
                        latestHasFaceId = response.body().getData().isHasFaceId();
                        Log.d("StudentSettingFragment", "Parsed body: HasFaceId=" + latestHasFaceId);
                        hasFaceId = latestHasFaceId;
                        requireContext().getSharedPreferences("prefs", 0)
                                .edit().putBoolean("faceid_registered", latestHasFaceId).apply();
                    } else {
                        try {
                            String err = response.errorBody() != null ? response.errorBody().string() : null;
                            Log.w("StudentSettingFragment", "Failed to parse user profile. errorBody=" + err);
                        } catch (Exception ignored) {}
                    }

                    // ✅ NEW: Kiểm tra xem Activity đã tồn tại chưa
                    if (isActivityDestroyed()) {
                        Log.d("StudentSettingFragment", "Activity đã destroy, không start Activity mới");
                        return;
                    }

                    if (latestHasFaceId) {
                        // Nếu đã có Face ID, chuyển tới màn hình thông tin Face ID (không phải success)
                        // ✅ NEW: Sử dụng FaceIdInfoActivity để hiển thị thông tin và cho phép update
                        Intent infoIntent = vn.edu.fpt.zentryapp.faceid.ui.setting.FaceIdInfoActivity.createIntent(
                            requireContext(), 
                            userId, 
                            authManager.getCurrentUserName()
                        );
                        startActivity(infoIntent);
                    } else {
                        // ✅ NEW: Sử dụng startActivity để ẩn navbar hoàn toàn
                        Intent registerIntent = new Intent(requireContext(), vn.edu.fpt.zentryapp.faceid.ui.setting.StudentSettingRegisterFaceIdActivity.class);
                        startActivity(registerIntent);
                    }
                    
                    // ✅ NEW: Reset trạng thái sau khi hoàn thành
                    resetFaceIdClickState();
                }

                @Override
                public void onFailure(@NonNull Call<ApiResponse<UserProfileDto>> call, @NonNull Throwable t) {
                    Log.e("StudentSettingFragment", "GET /api/User failed: " + t.getMessage(), t);
                    
                    // ✅ NEW: Kiểm tra xem Activity đã tồn tại chưa
                    if (isActivityDestroyed()) {
                        Log.d("StudentSettingFragment", "Activity đã destroy, không start Activity mới");
                        return;
                    }
                    
                    // Nếu API lỗi, fallback theo cache
                    if (hasFaceId) {
                        // ✅ NEW: Sử dụng FaceIdInfoActivity để hiển thị thông tin và cho phép update
                        String userId = AuthManager.getInstance(requireContext()).getCurrentUserId();
                        Intent infoIntent = vn.edu.fpt.zentryapp.faceid.ui.setting.FaceIdInfoActivity.createIntent(
                            requireContext(), 
                            userId, 
                            authManager.getCurrentUserName()
                        );
                        startActivity(infoIntent);
                    } else {
                        // ✅ NEW: Sử dụng startActivity để ẩn navbar hoàn toàn
                        Intent registerIntent = new Intent(requireContext(), vn.edu.fpt.zentryapp.faceid.ui.setting.StudentSettingRegisterFaceIdActivity.class);
                        startActivity(registerIntent);
                    }
                    
                    // ✅ NEW: Reset trạng thái sau khi hoàn thành
                    resetFaceIdClickState();
                }
            });
        });

        // Xử lý click Notifications để điều hướng sang màn hình cài đặt thông báo
        binding.llStudentSettingRowNotifications.setOnClickListener(v -> {
            try {
                Log.d("StudentSettingFragment", "Navigating to StudentSettingNotificationFragment");

                // Tạo bundle để truyền source
                Bundle args = new Bundle();
                args.putString(StudentSettingNotificationFragment.ARG_SOURCE,
                               StudentSettingNotificationFragment.SOURCE_SETTINGS);

                // Navigate với bundle
                navController.navigate(R.id.action_studentSetting_to_notifications, args);
            } catch (Exception e) {
                Log.e("StudentSettingFragment", "Navigation error", e);
            }
        });

        // Xử lý click Profile Overview để điều hướng sang màn hình tổng quan profile
        binding.llStudentSettingRowProfileOverview.setOnClickListener(v ->
                navController.navigate(R.id.action_studentSetting_to_profileOverview)
        );

        // Xử lý Logout
        binding.llStudentSettingRowLogout.setOnClickListener(v -> {
            try {
                Log.d("StudentSettingFragment", "Performing logout");
                authManager.clearTokens();
                // 2. Điều hướng về LoginFragment với popUpTo để xóa back stack
                androidx.navigation.NavOptions navOptions = new androidx.navigation.NavOptions.Builder()
                    .setPopUpTo(R.id.nav_graph_root, true)
                    .build();

                // Sử dụng action global logout đã định nghĩa trong nav_graph_root.xml
                NavController navController = androidx.navigation.Navigation.findNavController(
                    requireActivity(), R.id.nav_host_fragment);
                navController.navigate(R.id.action_global_logout, null, navOptions);

                Log.d("StudentSettingFragment", "Logout navigation completed");
            } catch (Exception e) {
                Log.e("StudentSettingFragment", "Error during logout: ", e);
            }
        });

        binding.tvStudentSettingName.setText(authManager.getCurrentUserName());
        binding.tvStudentSettingEmail.setText(authManager.getCurrentUserEmail());
    }

    private boolean checkIfFaceIdRegistered() {
        return getContext().getSharedPreferences("prefs", 0)
                .getBoolean("faceid_registered", false);
    }

    private void resetFaceIdClickState() {
        isFaceIdClickProcessing = false;
        binding.llStudentSettingRowFaceId.setEnabled(true);
        showFaceIdLoading(false);
    }
    
    // ✅ NEW: Kiểm tra xem Activity đã destroy chưa
    private boolean isActivityDestroyed() {
        return getActivity() == null || 
               getActivity().isFinishing() || 
               getActivity().isDestroyed() ||
               !isAdded() ||
               isDetached();
    }
    
    // ✅ NEW: Tối ưu hóa việc refresh user profile
    private void refreshUserProfileHasFaceId() {
        // Chỉ refresh nếu cần thiết
        if (hasFaceId) {
            return;
        }
        
        String userId = AuthManager.getInstance(requireContext()).getCurrentUserId();
        UserApiService api = ApiClient.getClient(requireContext()).create(UserApiService.class);
        api.getUser(userId).enqueue(new Callback<ApiResponse<UserProfileDto>>() {
            @Override
            public void onResponse(@NonNull Call<ApiResponse<UserProfileDto>> call, @NonNull Response<ApiResponse<UserProfileDto>> response) {
                if (response.isSuccessful() && response.body() != null && response.body().getData() != null) {
                    hasFaceId = response.body().getData().isHasFaceId();
                    requireContext().getSharedPreferences("prefs", 0)
                            .edit().putBoolean("faceid_registered", hasFaceId).apply();
                }
            }

            @Override
            public void onFailure(@NonNull Call<ApiResponse<UserProfileDto>> call, @NonNull Throwable t) {
                Log.w("StudentSettingFragment", "Failed to refresh user profile: " + t.getMessage());
            }
        });
    }

    // ✅ NEW: Initialize Face ID Request Manager
    private void initializeFaceIdRequestManager() {
        requestManager = new FaceIdRequestManager(requireContext());
        Log.d("StudentSettingFragment", "FaceIdRequestManager initialized");
    }

    // ✅ NEW: Check verification request status
    private void checkVerificationRequestStatus() {
        try {
            // Check if there's a pending verification request from SharedPreferences
            SharedPreferences prefs = requireActivity().getSharedPreferences("face_verification", Context.MODE_PRIVATE);
            String requestId = prefs.getString("pending_request_id", null);
            String sessionId = prefs.getString("pending_session_id", null);
            String expiresAt = prefs.getString("pending_expires_at", null);
            
            if (requestId != null && sessionId != null && !TextUtils.isEmpty(requestId) && !TextUtils.isEmpty(sessionId)) {
                Log.d("StudentSettingFragment", "Found pending verification request: " + requestId);
                
                // Check if request is expired
                if (isRequestExpired(expiresAt)) {
                    Log.d("StudentSettingFragment", "Request expired, clearing from preferences");
                    prefs.edit().clear().apply();
                    hasPendingVerificationRequest = false;
                } else {
                    // Initialize request manager with this request
                    long expirationTime = parseExpirationTime(expiresAt);
                    requestManager.initializeRequest(requestId, sessionId, expirationTime);
                    setupRequestManagerCallbacks();
                    hasPendingVerificationRequest = true;
                    Log.d("StudentSettingFragment", "Initialized request manager for pending request");
                }
            } else {
                hasPendingVerificationRequest = false;
                Log.d("StudentSettingFragment", "No pending verification request found");
            }
        } catch (Exception e) {
            Log.e("StudentSettingFragment", "Error checking verification request status", e);
            hasPendingVerificationRequest = false;
        }
    }
    
    // ✅ NEW: Check if Face ID request is expired
    private boolean isRequestExpired(String expiresAt) {
        if (TextUtils.isEmpty(expiresAt)) {
            Log.w("StudentSettingFragment", "No expiration timestamp provided, treating as expired for security");
            return true; // Treat as expired if no timestamp provided
        }
        
        try {
            // Parse ISO 8601 timestamp (e.g., "2024-01-01T12:00:00Z")
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.US);
            sdf.setTimeZone(TimeZone.getTimeZone("UTC"));
            Date expirationDate = sdf.parse(expiresAt);
            
            if (expirationDate == null) {
                Log.w("StudentSettingFragment", "Failed to parse expiration date: " + expiresAt);
                return true; // Treat as expired if parsing fails
            }
            
            long currentTime = System.currentTimeMillis();
            long expirationTime = expirationDate.getTime();
            
            // Add 5-minute buffer for network delays and processing time
            long bufferTime = 5 * 60 * 1000; // 5 minutes in milliseconds
            
            boolean isExpired = currentTime > (expirationTime + bufferTime);
            
            Log.d("StudentSettingFragment", "Expiration check: " + expiresAt + ", isExpired: " + isExpired);
            return isExpired;
            
        } catch (ParseException e) {
            Log.e("StudentSettingFragment", "Failed to parse expiration timestamp: " + expiresAt, e);
            return true; // Treat as expired if parsing fails
        }
    }
    
    // ✅ NEW: Parse expiration time from string to milliseconds
    private long parseExpirationTime(String expiresAt) {
        if (TextUtils.isEmpty(expiresAt)) {
            return System.currentTimeMillis() + (5 * 60 * 1000); // Default 5 minutes
        }
        
        try {
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.US);
            sdf.setTimeZone(TimeZone.getTimeZone("UTC"));
            Date expirationDate = sdf.parse(expiresAt);
            
            if (expirationDate != null) {
                return expirationDate.getTime();
            }
        } catch (ParseException e) {
            Log.e("StudentSettingFragment", "Failed to parse expiration time: " + expiresAt, e);
        }
        
        // Fallback to default 5 minutes
        return System.currentTimeMillis() + (5 * 60 * 1000);
    }
    
    // ✅ NEW: Setup callbacks for FaceIdRequestManager
    private void setupRequestManagerCallbacks() {
        if (requestManager == null) return;
        
        requestManager.setStatusCallback(new FaceIdRequestManager.RequestStatusCallback() {
            @Override
            public void onRequestStatusUpdated(FaceIdRequestManager.RequestState state, 
                                            FaceIdRequestStatusResponse response) {
                if (!isAdded()) return;
                
                Log.d("StudentSettingFragment", "Request status updated: " + state);
                updateFaceIdUIState(state);
            }
            
            @Override
            public void onRequestExpired() {
                if (!isAdded()) return;
                updateFaceIdUIState(FaceIdRequestManager.RequestState.EXPIRED);
            }
            
            @Override
            public void onRequestCancelled() {
                if (!isAdded()) return;
                updateFaceIdUIState(FaceIdRequestManager.RequestState.CANCELLED);
            }
            
            @Override
            public void onRequestFailed(String error) {
                if (!isAdded()) return;
                updateFaceIdUIState(FaceIdRequestManager.RequestState.FAILED);
            }
        });
        
        requestManager.setExpiredCallback(() -> {
            if (!isAdded()) return;
            updateFaceIdUIState(FaceIdRequestManager.RequestState.EXPIRED);
        });
    }
    
    // ✅ NEW: Update UI based on request state
    private void updateFaceIdUIState(FaceIdRequestManager.RequestState state) {
        if (binding == null || !isAdded()) return;
        
        Log.d("StudentSettingFragment", "Updating Face ID UI state: " + state);
        
        switch (state) {
            case VERIFIED:
                // ✅ Request đã verify thành công → Disable UI
                binding.llStudentSettingRowFaceId.setEnabled(false);
                binding.llStudentSettingRowFaceId.setAlpha(0.5f);
                hasPendingVerificationRequest = false;
                // Clear pending request from SharedPreferences
                clearPendingVerificationRequest();
                break;
                
            case EXPIRED:
                // ⏰ Request đã quá hạn → Disable UI
                binding.llStudentSettingRowFaceId.setEnabled(false);
                binding.llStudentSettingRowFaceId.setAlpha(0.5f);
                hasPendingVerificationRequest = false;
                // Clear expired request from SharedPreferences
                clearPendingVerificationRequest();
                break;
                
            case CANCELLED:
                // ❌ Request đã bị hủy → Disable UI
                binding.llStudentSettingRowFaceId.setEnabled(false);
                binding.llStudentSettingRowFaceId.setAlpha(0.5f);
                hasPendingVerificationRequest = false;
                // Clear cancelled request from SharedPreferences
                clearPendingVerificationRequest();
                break;
                
            case FAILED:
                // ❌ Request verification thất bại → Disable UI
                binding.llStudentSettingRowFaceId.setEnabled(false);
                binding.llStudentSettingRowFaceId.setAlpha(0.5f);
                hasPendingVerificationRequest = false;
                // Clear failed request from SharedPreferences
                clearPendingVerificationRequest();
                break;
                
            case PENDING:
            default:
                // ✅ Request đang pending → Enable UI bình thường
                binding.llStudentSettingRowFaceId.setEnabled(true);
                binding.llStudentSettingRowFaceId.setAlpha(1.0f);
                hasPendingVerificationRequest = true;
                break;
        }
    }
    
    // ✅ NEW: Clear pending verification request from SharedPreferences
    private void clearPendingVerificationRequest() {
        try {
            SharedPreferences prefs = requireActivity().getSharedPreferences("face_verification", Context.MODE_PRIVATE);
            prefs.edit().clear().apply();
            Log.d("StudentSettingFragment", "Cleared pending verification request from SharedPreferences");
        } catch (Exception e) {
            Log.e("StudentSettingFragment", "Error clearing pending verification request", e);
        }
    }
    
    // ✅ NEW: Get status message for user feedback
    private String getRequestStatusMessage(FaceIdRequestManager.RequestState state) {
        switch (state) {
            case VERIFIED:
                return "Face ID verification already completed successfully!";
            case EXPIRED:
                return "Face ID verification window has expired!";
            case CANCELLED:
                return "Face ID verification request was cancelled!";
            case FAILED:
                return "Face ID verification failed!";
            default:
                return "Face ID verification not available!";
        }
    }

    // ✅ NEW: Get status message for pending verification request
    private String getRequestStatusMessageForPendingRequest() {
        // Sử dụng SharedPreferences để check request status
        try {
            SharedPreferences prefs = requireActivity().getSharedPreferences("face_verification", Context.MODE_PRIVATE);
            String requestId = prefs.getString("pending_request_id", null);
            String expiresAt = prefs.getString("pending_expires_at", null);
            
            if (requestId == null) {
                return "Face ID verification not available.";
            }
            
            // Check if request is expired
            if (isRequestExpired(expiresAt)) {
                return "Face ID verification window has expired!";
            }
            
            // Nếu có request và chưa expired, có thể verify
            return "Face ID verification available.";
            
        } catch (Exception e) {
            Log.e("StudentSettingFragment", "Error getting request status message", e);
            return "Face ID verification not available.";
        }
    }
    
    // ✅ NEW: Check if the current verification request is active (not expired, not cancelled, not failed)
    private boolean isVerificationRequestActive() {
        try {
            SharedPreferences prefs = requireActivity().getSharedPreferences("face_verification", Context.MODE_PRIVATE);
            String requestId = prefs.getString("pending_request_id", null);
            String expiresAt = prefs.getString("pending_expires_at", null);
            
            if (requestId == null) {
                return false;
            }
            
            // Check if request is expired
            return !isRequestExpired(expiresAt);
            
        } catch (Exception e) {
            Log.e("StudentSettingFragment", "Error checking if verification request is active", e);
            return false;
        }
    }

    private void showFaceIdLoading(boolean show) {
        if (show) {
            binding.llStudentSettingRowFaceId.setVisibility(View.GONE);
            binding.llStudentSettingRowFaceIdLoading.setVisibility(View.VISIBLE);
        } else {
            binding.llStudentSettingRowFaceId.setVisibility(View.VISIBLE);
            binding.llStudentSettingRowFaceIdLoading.setVisibility(View.GONE);
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
