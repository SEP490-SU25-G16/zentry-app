package vn.edu.fpt.zentryapp.student.ui.setting;

import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;
import androidx.navigation.fragment.NavHostFragment;

import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import vn.edu.fpt.zentryapp.R;
import vn.edu.fpt.zentryapp.auth.client.AuthManager;
import vn.edu.fpt.zentryapp.databinding.FragmentStudentSettingBinding;

public class StudentSettingFragment extends Fragment {

    private FragmentStudentSettingBinding binding;
    private NavController navController;
    private boolean hasDevice;
    private boolean hasFaceId;
    private StudentSettingViewModel viewModel;

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

        // Xử lý click Device: điều hướng dựa trên trạng thái đăng ký thiết bị
        binding.llStudentSettingRowDevice.setOnClickListener(v -> {
            if (hasDevice) {
                navController.navigate(R.id.action_studentSetting_to_deviceInfo);
            } else {
                navController.navigate(R.id.action_studentSetting_to_deviceRegister);
            }
        });

        // Xử lý click Face ID: điều hướng dựa trên trạng thái đăng ký Face ID
        binding.llStudentSettingRowFaceId.setOnClickListener(v -> {
            if (hasFaceId) {
                navController.navigate(R.id.action_studentSetting_to_updateFaceId);
            } else {
                navController.navigate(R.id.action_studentSetting_to_registerFaceId);
            }
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

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
