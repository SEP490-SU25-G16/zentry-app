package vn.edu.fpt.zentryapp.auth.ui;

import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
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
import vn.edu.fpt.zentryapp.auth.client.ApiClient;
import vn.edu.fpt.zentryapp.auth.client.AuthManager;
import vn.edu.fpt.zentryapp.auth.services.AuthService;
import vn.edu.fpt.zentryapp.auth.ui.CreateNewPasswordViewModel;
import vn.edu.fpt.zentryapp.databinding.FragmentCreateNewPasswordBinding;

public class CreateNewPasswordFragment extends Fragment {

    private FragmentCreateNewPasswordBinding binding;
    private CreateNewPasswordViewModel viewModel;
    private NavController navController;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        binding = FragmentCreateNewPasswordBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // Get email from arguments
        String userEmail = getArguments() != null
                ? getArguments().getString("email", "")
                : "";
        String verificationToken = getArguments() != null
                ? getArguments().getString("token", "")
                : "";

        if (userEmail.isEmpty()) {
            Toast.makeText(requireContext(), "Session expired. Please start over.", Toast.LENGTH_LONG).show();
            NavHostFragment.findNavController(this).navigateUp();
            return;
        }

        // Initialize ViewModel
        viewModel = new ViewModelProvider(this).get(CreateNewPasswordViewModel.class);

        // Initialize dependencies
        AuthManager authManager = AuthManager.getInstance(requireContext());
        viewModel.init(
                ApiClient.getClient(requireContext()).create(AuthService.class),
                authManager,
                userEmail,
                verificationToken
        );

        navController = NavHostFragment.findNavController(this);

        setupTextWatchers();
        setupClickListeners();
        observeViewModel();
    }

    private void setupTextWatchers() {
        // Password strength checker
        binding.etCreatePasswordNew.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                viewModel.checkPasswordStrength(s.toString());
            }

            @Override
            public void afterTextChanged(Editable s) {
            }
        });
    }

    private void setupClickListeners() {
        // Back button
        binding.ivCreatePasswordBack.setOnClickListener(v -> navController.navigateUp());

        // Continue button
        binding.btnCreatePasswordContinue.setOnClickListener(v -> {
            String newPassword = binding.etCreatePasswordNew.getText() != null
                    ? binding.etCreatePasswordNew.getText().toString()
                    : "";
            String confirmPassword = binding.etCreatePasswordReType.getText() != null
                    ? binding.etCreatePasswordReType.getText().toString()
                    : "";

            viewModel.createNewPassword(newPassword, confirmPassword);
        });
    }

    private void observeViewModel() {
        // Observe loading state
        viewModel.isLoading().observe(getViewLifecycleOwner(), isLoading -> {
            binding.btnCreatePasswordContinue.setEnabled(!isLoading);

            if (isLoading) {
                binding.btnCreatePasswordContinue.setText("Creating Password...");
            } else {
                binding.btnCreatePasswordContinue.setText("Continue");
            }
        });

        // Observe password validation
        viewModel.passwordValidation().observe(getViewLifecycleOwner(), validation -> {
            if (validation != null) {
                binding.tilCreatePasswordNew.setError(validation.getPasswordError());
            }
        });

        // Observe confirm password validation
        viewModel.confirmPasswordError().observe(getViewLifecycleOwner(), error -> {
            binding.tilCreatePasswordReType.setError(error);
        });

        // Observe password strength (if you have UI for this)
        viewModel.passwordStrength().observe(getViewLifecycleOwner(), strength -> {
            if (strength != null) {
                // Update password strength indicator
                // binding.progressPasswordStrength.setProgress(strength.getProgress());
                // binding.tvPasswordStrength.setText(strength.getFeedback());
                // binding.tvPasswordStrength.setTextColor(strength.getColor());
            }
        });

        // Observe general error messages
        viewModel.errorMessage().observe(getViewLifecycleOwner(), errorMessage -> {
            if (errorMessage != null) {
                Toast.makeText(requireContext(), errorMessage, Toast.LENGTH_LONG).show();
            }
        });

        // Observe create password success
        viewModel.createPasswordSuccess().observe(getViewLifecycleOwner(), success -> {
            if (success != null && success.isSuccess()) {
                Toast.makeText(requireContext(), success.getMessage(), Toast.LENGTH_SHORT).show();
                // Navigate to success screen
                navController.navigate(R.id.action_createPassword_to_success);
            }
        });
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
