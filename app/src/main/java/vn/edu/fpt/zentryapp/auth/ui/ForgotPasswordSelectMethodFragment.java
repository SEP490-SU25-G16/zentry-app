package vn.edu.fpt.zentryapp.auth.ui;

import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.NavController;
import androidx.navigation.fragment.NavHostFragment;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import vn.edu.fpt.zentryapp.R;
import vn.edu.fpt.zentryapp.auth.client.ApiClient;
import vn.edu.fpt.zentryapp.auth.client.AuthManager;
import vn.edu.fpt.zentryapp.auth.services.AuthService;
import vn.edu.fpt.zentryapp.databinding.FragmentForgotPasswordSelectMethodBinding;

public class ForgotPasswordSelectMethodFragment extends Fragment {

    private FragmentForgotPasswordSelectMethodBinding binding;
    private ForgotPasswordSelectMethodViewModel viewModel;
    private NavController navController;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        // Inflate layout và binding view
        binding = FragmentForgotPasswordSelectMethodBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view,
                              @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        // Initialize ViewModel
        viewModel = new ViewModelProvider(this).get(ForgotPasswordSelectMethodViewModel.class);

        // Initialize dependencies
        AuthManager authManager = new AuthManager(requireContext());
        // Send mail api chỗ auth
        viewModel.init(
                ApiClient.getClient(requireContext()).create(AuthService.class),
                authManager
        );

        navController = NavHostFragment.findNavController(this);

        setupClickListeners();
        observeViewModel();

    }

    private void setupClickListeners() {
        // Back button
        binding.ivForgotPasswordBack.setOnClickListener(v -> navController.navigateUp());

        // Continue button
        binding.btnForgotPasswordContinue.setOnClickListener(v -> {
            String email = binding.etForgotPasswordEmail.getText() != null
                    ? binding.etForgotPasswordEmail.getText().toString().trim()
                    : "";
            viewModel.sendForgotPasswordRequest(email);
        });
    }

    private void observeViewModel() {
        // Observe loading state
        viewModel.isLoading().observe(getViewLifecycleOwner(), isLoading -> {
            binding.btnForgotPasswordContinue.setEnabled(!isLoading);

            if (isLoading) {
                binding.btnForgotPasswordContinue.setText("Sending...");
            } else {
                binding.btnForgotPasswordContinue.setText("Continue");
            }
        });

        // Observe validation result
        viewModel.validationResult().observe(getViewLifecycleOwner(), validationResult -> {
            if (validationResult != null && !validationResult.isValid()) {
                // Show validation error
                binding.etForgotPasswordEmail.setError(validationResult.getErrorMessage());
            } else {
                // Clear error
                binding.etForgotPasswordEmail.setError(null);
            }
        });

        // Observe general error messages
        viewModel.errorMessage().observe(getViewLifecycleOwner(), errorMessage -> {
            if (errorMessage != null) {
                Toast.makeText(requireContext(), errorMessage, Toast.LENGTH_LONG).show();
            }
        });

        // Observe forgot password success
        viewModel.forgotPasswordSuccess().observe(getViewLifecycleOwner(), success -> {
            if (success != null && success.isSuccess()) {
                // Show success message
                Toast.makeText(requireContext(), success.getMessage(), Toast.LENGTH_LONG).show();

                // Navigate to verify code screen
                // TODO: Pass email to next screen if needed
                navController.navigate(R.id.action_selectMethod_to_verifyCode);
            }
        });
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
