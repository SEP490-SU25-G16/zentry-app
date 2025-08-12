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
import vn.edu.fpt.zentryapp.auth.ui.ForgotPasswordVerifyCodeViewModel;
import vn.edu.fpt.zentryapp.databinding.FragmentForgotPasswordVerifyCodeBinding;

public class ForgotPasswordVerifyCodeFragment extends Fragment {

    private FragmentForgotPasswordVerifyCodeBinding binding;
    private ForgotPasswordVerifyCodeViewModel viewModel;
    private NavController navController;
    private String userEmail;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        binding = FragmentForgotPasswordVerifyCodeBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // Get email from arguments
        userEmail = getArguments() != null
                ? getArguments().getString("email", "")
                : "";

        if (userEmail.isEmpty()) {
            Toast.makeText(requireContext(), "Email not found. Please go back and try again.", Toast.LENGTH_LONG).show();
            NavHostFragment.findNavController(this).navigateUp();
            return;
        }

        // Initialize ViewModel
        viewModel = new ViewModelProvider(this).get(ForgotPasswordVerifyCodeViewModel.class);

        // Initialize dependencies
        AuthManager authManager = AuthManager.getInstance(requireContext());
        viewModel.init(
                ApiClient.getClient(requireContext()).create(AuthService.class),
                authManager,
                userEmail
        );

        navController = NavHostFragment.findNavController(this);

        setupUI();
        setupClickListeners();
        observeViewModel();
    }

    private void setupUI() {
       binding.tvVerifyCodeMessage.setText("Code sent to " + userEmail);
        // Thêm TextWatcher thay vì dựa vào button click
        binding.pvVerifyCodeInput.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                android.util.Log.d("PINVIEW_INPUT", "Text changed: '" + s + "', Length: " + (s != null ? s.length() : 0));

                if (s != null && s.length() == 6) {
                    String code = s.toString().trim();
                    android.util.Log.d("PINVIEW_INPUT", "Auto-verifying code: " + code);
                    Toast.makeText(requireContext(), "Verifying: " + code, Toast.LENGTH_SHORT).show();
                    viewModel.verifyCode(code);
                }
            }

            @Override
            public void afterTextChanged(Editable s) {

            }

        });
    }

    private void setupClickListeners() {
        // Back button
        binding.ivVerifyCodeBack.setOnClickListener(v -> navController.navigateUp());

        // Verify button
        binding.btnVerifyCodeVerify.setOnClickListener(v -> {
            android.util.Log.d("VerifyButton", "=== BUTTON CLICKED ===");

            String code = "";

            // Debug: Check if PinView exists
            if (binding.pvVerifyCodeInput != null) {
                android.util.Log.d("VerifyButton", "PinView is not null");

                if (binding.pvVerifyCodeInput.getText() != null) {
                    code = binding.pvVerifyCodeInput.getText().toString().trim();
                    android.util.Log.d("VerifyButton", "Code retrieved: '" + code + "'");
                } else {
                    android.util.Log.e("VerifyButton", "PinView getText() returns null");
                    Toast.makeText(requireContext(), "PinView getText() returns null", Toast.LENGTH_SHORT).show();
                }
            } else {
                android.util.Log.e("VerifyButton", "PinView is null!");
                Toast.makeText(requireContext(), "PinView is null", Toast.LENGTH_SHORT).show();
            }

            android.util.Log.d("VerifyButton", "Final code: '" + code + "', Length: " + code.length());

            if (code.isEmpty()) {
                android.util.Log.w("VerifyButton", "Code is empty, showing toast");
                Toast.makeText(requireContext(), "Please enter verification code", Toast.LENGTH_SHORT).show();
                return;
            }

            android.util.Log.d("VerifyButton", "Calling viewModel.verifyCode()");
            viewModel.verifyCode(code);
        });

        // Resend code
        binding.tvVerifyCodeResendTimer.setOnClickListener(v -> {
            if (binding.tvVerifyCodeResendTimer.isEnabled()) {
                viewModel.resendCode();
            }
        });
    }

    private void observeViewModel() {
        // Observe loading state
        viewModel.isLoading().observe(getViewLifecycleOwner(), isLoading -> {
            binding.btnVerifyCodeVerify.setEnabled(!isLoading);

            if (isLoading) {
                binding.btnVerifyCodeVerify.setText("Verifying...");
            } else {
                binding.btnVerifyCodeVerify.setText("Verify");
            }
        });

        // Observe code validation errors
        viewModel.codeError().observe(getViewLifecycleOwner(), codeError -> {
            if (codeError != null) {
                Toast.makeText(requireContext(), codeError, Toast.LENGTH_SHORT).show();
            }
        });

        // Observe general error messages
        viewModel.errorMessage().observe(getViewLifecycleOwner(), errorMessage -> {
            if (errorMessage != null) {
                Toast.makeText(requireContext(), errorMessage, Toast.LENGTH_LONG).show();
            }
        });

        // Observe countdown state
        viewModel.countdownState().observe(getViewLifecycleOwner(), countdownState -> {
            if (countdownState != null) {
                binding.tvVerifyCodeResendTimer.setText(countdownState.getDisplayText());
                binding.tvVerifyCodeResendTimer.setEnabled(countdownState.isCanResend());
            }
        });

        // Observe resend code result
        viewModel.resendCodeResult().observe(getViewLifecycleOwner(), resendResult -> {
            if (resendResult != null && resendResult.isSuccess()) {
                Toast.makeText(requireContext(), resendResult.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });

        // Observe verify code success
        viewModel.verifyCodeSuccess().observe(getViewLifecycleOwner(), success -> {
            if (success != null && success.isSuccess()) {
                Toast.makeText(requireContext(), success.getMessage(), Toast.LENGTH_SHORT).show();

                // Pass email to next screen (create password) if needed
                Bundle args = new Bundle();
                args.putString("email", userEmail);
                args.putString("token", success.getVerificationToken()); // Pass token
                // Navigate to create new password screen
                navController.navigate(R.id.action_verifyCode_to_createPassword, args);
            }
        });
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
