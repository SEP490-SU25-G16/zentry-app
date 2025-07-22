package vn.edu.fpt.zentryapp.auth.ui;

import android.os.Bundle;

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
import vn.edu.fpt.zentryapp.databinding.FragmentPasswordResetSuccessBinding;

public class PasswordResetSuccessFragment extends Fragment {

    private FragmentPasswordResetSuccessBinding binding;
    private PasswordResetSuccessViewModel viewModel;
    private NavController navController;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        binding = FragmentPasswordResetSuccessBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // Get email from arguments if available
        String userEmail = getArguments() != null
                ? getArguments().getString("email", "")
                : "";

        // Initialize ViewModel
        viewModel = new ViewModelProvider(this).get(PasswordResetSuccessViewModel.class);
        viewModel.init(userEmail);

        navController = NavHostFragment.findNavController(this);

        setupClickListeners();
        observeViewModel();
    }

    private void setupClickListeners() {
        // Sign In button - navigate immediately and stop countdown
        binding.btnPasswordResetSuccessSignIn.setOnClickListener(v -> {
            viewModel.navigateImmediately();
        });
    }

    private void observeViewModel() {
        // Observe countdown state
        viewModel.countdownState().observe(getViewLifecycleOwner(), countdownState -> {
            if (countdownState != null) {
                // Update countdown text (if you have a TextView for it)
                // binding.tvCountdown.setText(countdownState.getCountdownText());

                // Update button text with countdown
                binding.btnPasswordResetSuccessSignIn.setText(countdownState.getButtonText());
            }
        });

        // Observe navigation trigger
        viewModel.shouldNavigateToLogin().observe(getViewLifecycleOwner(), shouldNavigate -> {
            if (Boolean.TRUE.equals(shouldNavigate)) {
                navigateToLogin();
            }
        });

        // Observe success message
        viewModel.successMessage().observe(getViewLifecycleOwner(), message -> {
            if (message != null) {
                // Update success message if you have a TextView for it
                // binding.tvSuccessMessage.setText(message);
            }
        });
    }

    /**
     * Navigate to login screen and clear back stack
     */
    private void navigateToLogin() {
        NavOptions navOptions = new NavOptions.Builder()
                .setPopUpTo(R.id.nav_graph_root, true)
                .build();

        navController.navigate(R.id.loginFragment, null, navOptions);
    }

    @Override
    public void onPause() {
        super.onPause();
        // Pause countdown when fragment goes to background
        if (viewModel != null) {
            viewModel.pauseCountdown();
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        // Resume countdown when fragment comes back to foreground
        if (viewModel != null && viewModel.isCountdownActive()) {
            long remainingMs = viewModel.getRemainingTimeMs();
            if (remainingMs > 0) {
                viewModel.resumeCountdown(remainingMs);
            }
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
