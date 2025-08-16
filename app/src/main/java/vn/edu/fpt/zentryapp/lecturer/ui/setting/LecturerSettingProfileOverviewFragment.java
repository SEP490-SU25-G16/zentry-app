package vn.edu.fpt.zentryapp.lecturer.ui.setting;

import android.os.Bundle;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.NavController;
import androidx.navigation.fragment.NavHostFragment;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import vn.edu.fpt.zentryapp.R;
import vn.edu.fpt.zentryapp.auth.client.AuthManager;
import vn.edu.fpt.zentryapp.databinding.FragmentLecturerSettingProfileOverviewBinding;
import vn.edu.fpt.zentryapp.lecturer.data.model.response.UserProfile;

public class LecturerSettingProfileOverviewFragment extends Fragment {

    private FragmentLecturerSettingProfileOverviewBinding binding;
    private LecturerSettingProfileOverviewViewModel viewModel;
    private NavController navController;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        binding = FragmentLecturerSettingProfileOverviewBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view,
                              @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        navController = NavHostFragment.findNavController(this);

        // Initialize ViewModel
        viewModel = new ViewModelProvider(this).get(LecturerSettingProfileOverviewViewModel.class);
        AuthManager authManager = AuthManager.getInstance(requireContext());
        viewModel.init(requireContext(), authManager);

        setupClickListeners();
        observeViewModel();
    }

    private void setupClickListeners() {
        // Back button
        binding.ivProfileOverviewBack.setOnClickListener(v ->
                navController.navigateUp());
    }

    private void observeViewModel() {
        // Observe loading state
        viewModel.isLoading().observe(getViewLifecycleOwner(), isLoading -> {
            binding.progressLoading.setVisibility(isLoading ? View.VISIBLE : View.GONE);

            // Disable cards during loading
            setCardsEnabled(!isLoading);
        });

        // Observe user profile data
        viewModel.userProfile().observe(getViewLifecycleOwner(), userProfile -> {
            if (userProfile != null) {
                updateUIWithUserProfile(userProfile);
            }
        });

        // Observe errors
        viewModel.errorMessage().observe(getViewLifecycleOwner(), error -> {
            if (error != null) {
                Toast.makeText(requireContext(), error, Toast.LENGTH_LONG).show();
            }
        });
    }

    private void setCardsEnabled(boolean enabled) {
        binding.cardProfileOverviewFullName.setEnabled(enabled);
        binding.cardProfileOverviewEmail.setEnabled(enabled);
        binding.cardProfileOverviewPhone.setEnabled(enabled);
        binding.cardProfileOverviewRole.setEnabled(enabled);
        binding.cardProfileOverviewStatus.setEnabled(enabled);
        binding.cardProfileOverviewCreatedAt.setEnabled(enabled);
    }

    private void updateUIWithUserProfile(UserProfile userProfile) {
        // Update UI with user profile data
        binding.tvProfileOverviewFullName.setText(userProfile.getFullName());
        binding.tvProfileOverviewEmail.setText(userProfile.getEmail());
        binding.tvProfileOverviewPhone.setText(userProfile.getFormattedPhoneNumber());
        binding.tvProfileOverviewRole.setText(userProfile.getRoleDisplayName());
        binding.tvProfileOverviewStatus.setText(userProfile.getStatusDisplayName());
        binding.tvProfileOverviewCode.setText(userProfile.getCode());

        // Hiển thị ngày tạo
        binding.tvProfileOverviewCreatedAt.setText("Joined: " + userProfile.getFormattedCreatedDate());

        // Set status color based on status
        int statusColor = getStatusColor(userProfile.getStatus());
        binding.tvProfileOverviewStatus.setTextColor(statusColor);

    }

    private int getStatusColor(String status) {
        if (status == null) return 0xFF757575; // Gray

        switch (status.toLowerCase()) {
            case "active":
                return 0xFF4CAF50; // Green
            case "inactive":
                return 0xFFFF9800; // Orange
            case "suspended":
                return 0xFFE53935; // Red
            default:
                return 0xFF757575; // Gray
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }

    @Override
    public void onResume() {
        super.onResume();
        // Refresh data when returning to this screen
        if (viewModel != null) {
            viewModel.refreshProfile();
        }
    }
}
