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

import vn.edu.fpt.zentryapp.auth.client.AuthManager;
import vn.edu.fpt.zentryapp.databinding.FragmentLecturerSettingDeviceRegisterBinding;

public class LecturerSettingDeviceRegisterFragment extends Fragment {

    private static final String TAG = "LecturerDeviceRegister";

    private FragmentLecturerSettingDeviceRegisterBinding binding;
    private LecturerSettingDeviceRegisterViewModel viewModel;
    private NavController navController;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        binding = FragmentLecturerSettingDeviceRegisterBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view,
                              @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        navController = NavHostFragment.findNavController(this);

        // Initialize ViewModel
        viewModel = new ViewModelProvider(this).get(LecturerSettingDeviceRegisterViewModel.class);
        AuthManager authManager = AuthManager.getInstance(requireContext());
        viewModel.init(requireContext(), authManager);

        setupClickListeners();
        observeViewModel();
    }

    private void setupClickListeners() {
        // Back button
        binding.ivDeviceRegisterBack.setOnClickListener(v ->
                navController.navigateUp());

        // ✅ Register button - Call API to register device
        binding.btnDeviceRegister.setOnClickListener(v -> {
            viewModel.registerDevice(requireContext());
        });
    }

    private void observeViewModel() {
        // Loading state
        viewModel.isLoading().observe(getViewLifecycleOwner(), isLoading -> {
            binding.btnDeviceRegister.setEnabled(!isLoading);

            if (isLoading) {
                binding.btnDeviceRegister.setText("Registering...");
            } else {
                // Check if device is already registered
                Boolean isRegistered = viewModel.isDeviceRegistered().getValue();
                if (Boolean.TRUE.equals(isRegistered)) {
                    binding.btnDeviceRegister.setText("Device Already Registered");
                } else {
                    binding.btnDeviceRegister.setText("Register Device");
                }
            }
        });

        // Device registration status
        viewModel.isDeviceRegistered().observe(getViewLifecycleOwner(), isRegistered -> {
            if (Boolean.TRUE.equals(isRegistered)) {
                binding.btnDeviceRegister.setText("Device Already Registered");
                binding.btnDeviceRegister.setEnabled(false);
                // Change button color or style if needed
                binding.btnDeviceRegister.setAlpha(0.6f);
            } else {
                binding.btnDeviceRegister.setText("Register Device");
                binding.btnDeviceRegister.setEnabled(true);
                binding.btnDeviceRegister.setAlpha(1.0f);
            }
        });

        // Success message
        viewModel.successMessage().observe(getViewLifecycleOwner(), message -> {
            if (message != null) {
                Toast.makeText(requireContext(), message, Toast.LENGTH_LONG).show();

                // Navigate back after successful registration
                navController.navigateUp();
            }
        });

        // Error message
        viewModel.errorMessage().observe(getViewLifecycleOwner(), error -> {
            if (error != null) {
                Toast.makeText(requireContext(), error, Toast.LENGTH_LONG).show();
            }
        });
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
