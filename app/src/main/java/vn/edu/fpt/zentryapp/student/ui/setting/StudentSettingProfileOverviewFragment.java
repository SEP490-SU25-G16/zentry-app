package vn.edu.fpt.zentryapp.student.ui.setting;

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

import vn.edu.fpt.zentryapp.auth.client.AuthManager;
import vn.edu.fpt.zentryapp.databinding.FragmentStudentSettingProfileOverviewBinding;
import vn.edu.fpt.zentryapp.lecturer.data.model.response.UserProfile;

public class StudentSettingProfileOverviewFragment extends Fragment {

    private FragmentStudentSettingProfileOverviewBinding binding;
    private StudentSettingProfileOverviewViewModel viewModel;
    private NavController navController;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        binding = FragmentStudentSettingProfileOverviewBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view,
                              @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        navController = NavHostFragment.findNavController(this);

        // Initialize ViewModel
        viewModel = new ViewModelProvider(this).get(StudentSettingProfileOverviewViewModel.class);
        AuthManager authManager = AuthManager.getInstance(requireContext());
        viewModel.init(requireContext(), authManager);

        setupClickListeners();
        observeViewModel();
    }

    private void setupClickListeners() {
        // Back button
        binding.ivStudentSettingProfileOverviewBack.setOnClickListener(v ->
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
        binding.cardStudentSettingProfileOverviewFullName.setEnabled(enabled);
        binding.cardStudentSettingProfileOverviewEmail.setEnabled(enabled);
        binding.cardStudentSettingProfileOverviewPhone.setEnabled(enabled);
        binding.cardStudentSettingProfileOverviewRole.setEnabled(enabled);
        binding.cardStudentSettingProfileOverviewStatus.setEnabled(enabled);
        binding.cardStudentSettingProfileOverviewCreatedAt.setEnabled(enabled);
        binding.cardStudentSettingProfileOverviewFaceId.setEnabled(enabled);
    }

    private void updateUIWithUserProfile(UserProfile userProfile) {
        // Update UI with user profile data
        binding.tvStudentSettingProfileOverviewFullName.setText(userProfile.getFullName());
        binding.tvStudentSettingProfileOverviewEmail.setText(userProfile.getEmail());
        binding.tvStudentSettingProfileOverviewPhone.setText(userProfile.getFormattedPhoneNumber());
        binding.tvStudentSettingProfileOverviewRole.setText(userProfile.getRoleDisplayName());
        binding.tvStudentSettingProfileOverviewStatus.setText(userProfile.getStatusDisplayName());

        // Hiển thị ngày tạo
        binding.tvStudentSettingProfileOverviewCreatedAt.setText("Joined: " + userProfile.getFormattedCreatedDate());

        // Hiển thị Face ID status
        binding.tvStudentSettingProfileOverviewFaceId.setText("Face ID: " + userProfile.getFaceIdStatus());

        // Set status color based on status
        int statusColor = getStatusColor(userProfile.getStatus());
        binding.tvStudentSettingProfileOverviewStatus.setTextColor(statusColor);

        // Set Face ID icon color based on status
        int faceIdColor = userProfile.isHasFaceId() ? 0xFF4CAF50 : 0xFFFF9800; // Green or Orange
        binding.ivStudentSettingProfileOverviewFaceIdIcon.setColorFilter(faceIdColor);
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
