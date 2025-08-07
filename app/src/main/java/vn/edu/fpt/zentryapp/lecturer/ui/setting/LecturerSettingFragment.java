package vn.edu.fpt.zentryapp.lecturer.ui.setting;

import android.app.AlertDialog;
import android.os.Bundle;
import android.util.Log;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.NavController;
import androidx.navigation.NavOptions;
import androidx.navigation.fragment.NavHostFragment;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import vn.edu.fpt.zentryapp.R;
import vn.edu.fpt.zentryapp.auth.client.AuthManager;
import vn.edu.fpt.zentryapp.databinding.FragmentLecturerSettingBinding;

public class LecturerSettingFragment extends Fragment {

    private FragmentLecturerSettingBinding binding;
    private LecturerSettingViewModel viewModel;
    private NavController navController;
    private boolean hasDevice;
    private AuthManager authManager;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        binding = FragmentLecturerSettingBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        navController = NavHostFragment.findNavController(this);
        authManager = AuthManager.getInstance(requireContext());
        // Initialize ViewModel
        viewModel = new ViewModelProvider(this).get(LecturerSettingViewModel.class);
        AuthManager authManager = AuthManager.getInstance(requireContext());
        viewModel.init(authManager);
        hasDevice = checkIfDeviceRegistered();
        setupClickListeners();
        observeViewModel();
    }

    // Thêm các phương thức kiểm tra trạng thái như Student
    private boolean checkIfDeviceRegistered() {
        return authManager.isDeviceRegistered();
    }

    private void setupClickListeners() {
        // Profile row
        binding.llSettingRowProfile.setOnClickListener(v -> {
            navController.navigate(R.id.action_setting_to_profileOverview);
        });

        // Notifications row
        binding.llSettingRowNotifications.setOnClickListener(v -> {
            navController.navigate(R.id.action_setting_to_notification);
        });

        // Device row - Logic điều kiện như Student
        binding.llSettingRowDevice.setOnClickListener(v -> {
            if (hasDevice) {
                // Nếu đã đăng ký thiết bị, chuyển đến màn hình thông tin thiết bị
                navController.navigate(R.id.action_setting_to_deviceInfo);
            } else {
                // Nếu chưa đăng ký, chuyển đến màn hình đăng ký thiết bị
                navController.navigate(R.id.action_setting_to_deviceRegister);
            }
        });

        // Logout row
        binding.llSettingRowLogout.setOnClickListener(v -> {
            performLogout();
        });


    }

    private void observeViewModel() {
        // Observe loading state
        viewModel.isLoading().observe(getViewLifecycleOwner(), isLoading -> {
            // Show/hide loading indicator if needed
            // For now, just disable interactions during loading
            setUIEnabled(!isLoading);
        });

        // Observe user profile
        viewModel.userProfile().observe(getViewLifecycleOwner(), profile -> {
            if (profile != null) {
                updateUserProfileUI(profile);
            }
        });

        // Observe app settings
        viewModel.appSettings().observe(getViewLifecycleOwner(), settings -> {
            if (settings != null) {
                updateSettingsUI(settings);
            }
        });

        // Observe logout success
        viewModel.logoutSuccess().observe(getViewLifecycleOwner(), success -> {
            if (Boolean.TRUE.equals(success)) {
                handleLogoutSuccess();
            }
        });

        // Observe error messages
        viewModel.errorMessage().observe(getViewLifecycleOwner(), error -> {
            if (error != null) {
                Toast.makeText(requireContext(), error, Toast.LENGTH_LONG).show();
            }
        });
    }

    private void updateUserProfileUI(LecturerSettingViewModel.UserProfile profile) {
        binding.tvSettingName.setText(profile.getDisplayName());
        binding.tvSettingEmail.setText(profile.getDisplayEmail());

        // Log user info for debugging
        android.util.Log.d("LecturerSetting",
                "Profile loaded: " + profile.getDisplayName() + " (" + profile.getRole() + ")");
    }

    private void updateSettingsUI(LecturerSettingViewModel.AppSettings settings) {
        // Update UI based on app settings
        // For example, show notification status, theme info, etc.

        // Add visual indicators for settings status
        if (settings.isNotificationsEnabled()) {
            // Could add a small indicator on notifications row
        }

        if (settings.isFaceIdEnabled()) {
            // Could add a small indicator on device row
        }
    }

    private void showLogoutConfirmationDialog() {
        new AlertDialog.Builder(requireContext())
                .setTitle("Logout Confirmation")
                .setMessage("Are you sure you want to logout?")
                .setPositiveButton("Logout", (dialog, which) -> {
                    performLogout();
                })
                .setNegativeButton("Cancel", (dialog, which) -> {
                    dialog.dismiss();
                })
                .show();
    }

    private void performLogout() {
        try {
            android.util.Log.d("LecturerSettingFragment", "Performing logout");
            
            // 1. Xóa token và thông tin người dùng
            vn.edu.fpt.zentryapp.auth.client.AuthManager authManager = 
                vn.edu.fpt.zentryapp.auth.client.AuthManager.getInstance(requireContext());
            authManager.clearTokens();
            
            // 2. Tìm activity container và lấy NavController gốc
            androidx.navigation.NavController rootNavController = null;
            try {
                // Lấy NavController từ activity container
                rootNavController = androidx.navigation.Navigation.findNavController(
                    requireActivity(), R.id.nav_host_fragment);
                
                // Tạo NavOptions để xóa back stack
                androidx.navigation.NavOptions navOptions = new androidx.navigation.NavOptions.Builder()
                    .setPopUpTo(R.id.nav_graph_root, true)
                    .build();
                
                // Navigate về LoginFragment
                rootNavController.navigate(R.id.loginFragment, null, navOptions);
                
                android.util.Log.d("LecturerSettingFragment", "Navigated to login using root NavController");
                return;
            } catch (Exception e) {
                android.util.Log.e("LecturerSettingFragment", "Error navigating with root NavController: ", e);
            }
            
            // 3. Fallback: Sử dụng Intent để khởi động lại ứng dụng
            try {
                android.content.Intent intent = requireActivity().getPackageManager()
                    .getLaunchIntentForPackage(requireActivity().getPackageName());
                if (intent != null) {
                    // Xóa stack cũ
                    intent.addFlags(android.content.Intent.FLAG_ACTIVITY_CLEAR_TOP | 
                                   android.content.Intent.FLAG_ACTIVITY_CLEAR_TASK |
                                   android.content.Intent.FLAG_ACTIVITY_NEW_TASK);
                    startActivity(intent);
                    requireActivity().finish();
                    android.util.Log.d("LecturerSettingFragment", "Restarted app using Intent");
                    return;
                }
            } catch (Exception e) {
                android.util.Log.e("LecturerSettingFragment", "Error restarting app: ", e);
            }
            
            // 4. Ultimate fallback: Kết thúc activity hiện tại
            requireActivity().finish();
            android.util.Log.d("LecturerSettingFragment", "Finished current activity");
            
        } catch (Exception e) {
            android.util.Log.e("LecturerSettingFragment", "Error during logout: ", e);
        }
    }

    private void handleLogoutSuccess() {
        Toast.makeText(requireContext(), "Logged out successfully", Toast.LENGTH_SHORT).show();

        // Navigate to login screen and clear all back stack
        NavOptions navOptions = new NavOptions.Builder()
                .setPopUpTo(R.id.nav_graph_root, true)
                .build();

        navController.navigate(R.id.loginFragment, null, navOptions);
    }


    private void setUIEnabled(boolean enabled) {
        // Enable/disable UI elements during loading
        binding.llSettingRowProfile.setEnabled(enabled);
        binding.llSettingRowNotifications.setEnabled(enabled);
        binding.llSettingRowDevice.setEnabled(enabled);
        binding.llSettingRowLogout.setEnabled(enabled);

        // Set alpha to indicate disabled state
        float alpha = enabled ? 1.0f : 0.6f;
        binding.llSettingRowProfile.setAlpha(alpha);
        binding.llSettingRowNotifications.setAlpha(alpha);
        binding.llSettingRowDevice.setAlpha(alpha);
        binding.llSettingRowLogout.setAlpha(alpha);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
