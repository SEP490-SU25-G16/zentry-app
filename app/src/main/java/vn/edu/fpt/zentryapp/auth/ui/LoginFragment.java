package vn.edu.fpt.zentryapp.auth.ui;

import android.os.Bundle;

import androidx.activity.OnBackPressedCallback;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.NavController;
import androidx.navigation.NavOptions;
import androidx.navigation.fragment.NavHostFragment;

import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import vn.edu.fpt.zentryapp.R;
import vn.edu.fpt.zentryapp.auth.client.ApiClient;
import vn.edu.fpt.zentryapp.auth.client.AuthManager;
import vn.edu.fpt.zentryapp.auth.services.AuthService;
import vn.edu.fpt.zentryapp.databinding.FragmentLoginBinding;

public class LoginFragment extends Fragment {

    private FragmentLoginBinding binding;
    private LoginViewModel loginViewModel;
    private NavController navController;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        binding = FragmentLoginBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // Initialize ViewModel
        loginViewModel = new ViewModelProvider(this).get(LoginViewModel.class);

        // Initialize dependencies
        AuthManager authManager = new AuthManager(requireContext());
        loginViewModel.init(
                ApiClient.getClient(requireContext()).create(AuthService.class),
                authManager
        );

        navController = NavHostFragment.findNavController(this);

        setupClickListeners();
        observeViewModel();
        setupBackPressHandler();
    }

    private void setupClickListeners() {
        // Forgot Password
        binding.tvLoginForgotPassword.setOnClickListener(v ->
                navController.navigate(R.id.action_login_to_selectMethod)
        );

        // Sign In Button
        binding.btnLoginSignIn.setOnClickListener(v -> {
            String email = binding.etLoginEmail.getText() != null
                    ? binding.etLoginEmail.getText().toString().trim()
                    : "";
            String password = binding.etLoginPassword.getText() != null
                    ? binding.etLoginPassword.getText().toString()
                    : "";

            loginViewModel.login(email, password);
        });

        // Google Sign In (placeholder)
        binding.btnLoginGoogle.setOnClickListener(v -> {
            // TODO: Implement Google Sign-In
        });
    }

    private void observeViewModel() {
        // Observe loading state
        loginViewModel.isLoading().observe(getViewLifecycleOwner(), isLoading -> {
            binding.btnLoginSignIn.setEnabled(!isLoading);

            if (isLoading) {
                binding.btnLoginSignIn.setText("Signing in...");
            } else {
                binding.btnLoginSignIn.setText("Sign In");
            }
        });

        // Observe general error messages
        loginViewModel.errorMessage().observe(getViewLifecycleOwner(), errorMessage -> {
            if (errorMessage != null) {
                binding.tvLoginPasswordError.setText(errorMessage);
                binding.tvLoginPasswordError.setVisibility(View.VISIBLE);
            } else {
                binding.tvLoginPasswordError.setVisibility(View.GONE);
            }
        });

        // Observe email validation errors
        loginViewModel.emailError().observe(getViewLifecycleOwner(), emailError -> {
            if (emailError != null) {
                binding.tilLoginEmail.setError(emailError);
            } else {
                binding.tilLoginEmail.setError(null);
            }
        });

        // Observe password validation errors
        loginViewModel.passwordError().observe(getViewLifecycleOwner(), passwordError -> {
            if (passwordError != null) {
                binding.tilLoginPassword.setError(passwordError);
            } else {
                binding.tilLoginPassword.setError(null);
            }
        });

        // Observe login success
        loginViewModel.loginSuccess().observe(getViewLifecycleOwner(), loginSuccess -> {
            if (loginSuccess != null) {
                handleLoginSuccess(loginSuccess);
            }
        });
    }

    private void handleLoginSuccess(LoginViewModel.LoginSuccess loginSuccess) {
        // Determine navigation destination
        int actionId = loginSuccess.isLecturer()
                ? R.id.action_login_to_lecturer
                : R.id.action_login_to_student;

        // Create NavOptions to clear back stack
        NavOptions navOptions = new NavOptions.Builder()
                .setPopUpTo(R.id.nav_graph_root, true)
                .build();

        // Navigate to appropriate screen
        navController.navigate(actionId, null, navOptions);
    }

    private void setupBackPressHandler() {
        requireActivity().getOnBackPressedDispatcher().addCallback(
                getViewLifecycleOwner(),
                new OnBackPressedCallback(true) {
                    @Override
                    public void handleOnBackPressed() {
                        if (navController.popBackStack()) {
                            // Successfully popped previous fragment
                        } else {
                            // No more fragments to pop, exit app or activity
                            setEnabled(false);
                            requireActivity().onBackPressed();
                        }
                    }
                }
        );
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}