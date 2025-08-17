package vn.edu.fpt.zentryapp.student.ui.setting;

import android.os.Bundle;

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

import vn.edu.fpt.zentryapp.R;
import vn.edu.fpt.zentryapp.auth.client.AuthManager;
import vn.edu.fpt.zentryapp.auth.client.ApiClient;
import vn.edu.fpt.zentryapp.auth.models.ApiResponse;
import vn.edu.fpt.zentryapp.student.data.api.UserApiService;
import vn.edu.fpt.zentryapp.student.data.model.response.UserProfileDto;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import vn.edu.fpt.zentryapp.databinding.FragmentStudentSettingBinding;

public class StudentSettingFragment extends Fragment {

    private FragmentStudentSettingBinding binding;
    private NavController navController;
    private boolean hasDevice;
    private boolean hasFaceId;
    private StudentSettingViewModel viewModel;
    private boolean isFaceIdClickProcessing = false;

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
        hasDevice = checkIfDeviceRegistered();
        hasFaceId = checkIfFaceIdRegistered();

        // Fetch latest HasFaceId from API and update cache
        refreshUserProfileHasFaceId();

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

    private boolean checkIfDeviceRegistered() {
        // Ví dụ: đọc từ SharedPreferences
        return getContext().getSharedPreferences("prefs", 0)
                .getBoolean("device_registered", false);
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
