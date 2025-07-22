package vn.edu.fpt.zentryapp.lecturer.ui.setting;

import android.app.AlertDialog;
import android.os.Bundle;
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

        // Initialize ViewModel
        viewModel = new ViewModelProvider(this).get(LecturerSettingViewModel.class);
        AuthManager authManager = AuthManager.getInstance(requireContext());
        viewModel.init(authManager);

        setupClickListeners();
        observeViewModel();
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

        // Device row
        binding.llSettingRowDevice.setOnClickListener(v -> {
            navController.navigate(R.id.action_setting_to_deviceInfo);
        });

        // Logout row
        binding.llSettingRowLogout.setOnClickListener(v -> {
            showLogoutConfirmationDialog();
        });

        // Avatar click (for future profile picture update)
        binding.ivSettingAvatar.setOnClickListener(v -> {
            Toast.makeText(requireContext(), "Avatar update coming soon", Toast.LENGTH_SHORT).show();
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

        // Set default avatar (in the future, load from URL if available)
        binding.ivSettingAvatar.setImageResource(R.drawable.ic_launcher_foreground);

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
        viewModel.performLogout();
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
