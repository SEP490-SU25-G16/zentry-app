package com.zentry.app.ui.fragment.common;

import android.os.Bundle;
import android.text.InputType;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import com.zentry.app.R;
import com.zentry.app.databinding.FragmentSignInBinding;
import com.zentry.app.model.entity.UserRole;
import com.zentry.app.model.request.LoginRequest;
import com.zentry.app.navigation.AreaManager;
import com.zentry.app.viewmodel.AuthViewModel;

public class SignInFragment extends Fragment {
    private FragmentSignInBinding binding;
    private AuthViewModel viewModel;
    private boolean isPasswordVisible = false;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = FragmentSignInBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        initViewModel();
        setupClickListeners();
        observeViewModel();
        setupPasswordToggle();

        // Kiểm tra và đặt trạng thái "Remember Me" khi Fragment được tạo
        // Điều này sẽ tích hợp với logic checkLoginStatus()
        binding.cbRememberMe.setChecked(viewModel.getRememberMePreference());

        // Kiểm tra nếu user đã login và có chọn "Remember Me" từ trước
        checkLoginStatus();
    }

    private void initViewModel() {
        viewModel = new ViewModelProvider(this).get(AuthViewModel.class);
    }

    private void setupClickListeners() {
        // Sign in button click
        binding.btnSignIn.setOnClickListener(v -> handleSignIn());

        // Forgot password click (bỏ comment nếu đã triển khai)
        // binding.tvForgotPassword.setOnClickListener(v -> handleForgotPassword());

        // Google sign in click
        binding.btnGoogleSignIn.setOnClickListener(v -> handleGoogleSignIn());

        // Password toggle click
        binding.ivTogglePassword.setOnClickListener(v -> togglePasswordVisibility());
    }

    private void setupPasswordToggle() {
        // Ban đầu ẩn mật khẩu
        isPasswordVisible = false;
        updatePasswordVisibility();
    }

    private void togglePasswordVisibility() {
        isPasswordVisible = !isPasswordVisible;
        updatePasswordVisibility();
    }

    private void updatePasswordVisibility() {
        if (isPasswordVisible) {
            binding.edtPassword.setInputType(InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD);
            binding.ivTogglePassword.setImageResource(R.drawable.ic_visibility_off); // Đảm bảo icon này tồn tại
        } else {
            binding.edtPassword.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
            binding.ivTogglePassword.setImageResource(R.drawable.ic_visibility); // Đảm bảo icon này tồn tại
        }
        // Di chuyển con trỏ đến cuối văn bản
        binding.edtPassword.setSelection(binding.edtPassword.getText().length());
    }

    private void observeViewModel() {
        // Observe loading state
        viewModel.getLoading().observe(getViewLifecycleOwner(), isLoading -> {
            updateLoadingState(isLoading);
        });

        // Observe login success
        viewModel.getLoginSuccess().observe(getViewLifecycleOwner(), success -> {
            if (success != null && success) {
                handleLoginSuccess();
            }
        });

        // Observe errors
        viewModel.getError().observe(getViewLifecycleOwner(), error -> {
            if (error != null && !error.isEmpty()) {
                handleError(error);
            }
        });

        // Observe user token để lấy role info và điều hướng
        // Điều này sẽ được kích hoạt sau khi login thành công và token được cập nhật trong ViewModel
        viewModel.getUserToken().observe(getViewLifecycleOwner(), tokenModel -> {
            if (tokenModel != null) {
                // Sau khi có token, điều hướng dựa trên role
                navigateBasedOnRole();
            }
        });
    }

    private void handleSignIn() {
        String email = binding.edtEmail.getText().toString().trim();
        String password = binding.edtPassword.getText().toString().trim();

        // Ẩn thông báo lỗi trước
        binding.tvErrorMessage.setVisibility(View.GONE);

        if (!validateInput(email, password)) {
            return;
        }

        // Xóa lỗi cũ
        viewModel.clearError();

        // Thực hiện đăng nhập
        LoginRequest request = new LoginRequest(email, password);
        viewModel.login(request);
    }

    private void handleGoogleSignIn() {
        // Xử lý đăng nhập Google
        Toast.makeText(getContext(), "Google Sign In coming soon!", Toast.LENGTH_SHORT).show();

        // Nếu bạn đã triển khai đăng nhập Google:
        // viewModel.signInWithGoogle();
    }

    private boolean validateInput(String email, String password) {
        if (TextUtils.isEmpty(email)) {
            showErrorMessage("Email is required");
            binding.edtEmail.requestFocus();
            return false;
        }

        if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            showErrorMessage("Please enter a valid email");
            binding.edtEmail.requestFocus();
            return false;
        }

        if (TextUtils.isEmpty(password)) {
            showErrorMessage("Password is required");
            binding.edtPassword.requestFocus();
            return false;
        }

        if (password.length() < 6) {
            showErrorMessage("Password must be at least 6 characters");
            binding.edtPassword.requestFocus();
            return false;
        }

        return true;
    }

    private void showErrorMessage(String message) {
        binding.tvErrorMessage.setText(message);
        binding.tvErrorMessage.setVisibility(View.VISIBLE);
    }

    private void updateLoadingState(boolean isLoading) {
        // Vô hiệu hóa/kích hoạt các trường nhập và nút
        binding.edtEmail.setEnabled(!isLoading);
        binding.edtPassword.setEnabled(!isLoading);
        binding.btnSignIn.setEnabled(!isLoading);
        binding.btnGoogleSignIn.setEnabled(!isLoading);
        // binding.tvForgotPassword.setEnabled(!isLoading); // Bỏ comment nếu có
        binding.cbRememberMe.setEnabled(!isLoading);

        if (isLoading) {
            // Hiển thị thanh tiến trình và ẩn văn bản đăng nhập
            binding.progressBar.setVisibility(View.VISIBLE);
            binding.tvSignInText.setVisibility(View.GONE);
            // Ẩn thông báo lỗi trong khi tải
            binding.tvErrorMessage.setVisibility(View.GONE);
        } else {
            // Ẩn thanh tiến trình và hiển thị văn bản đăng nhập
            binding.progressBar.setVisibility(View.GONE);
            binding.tvSignInText.setVisibility(View.VISIBLE);
        }
    }

    private void handleLoginSuccess() {
        Toast.makeText(getContext(), "Login successful", Toast.LENGTH_SHORT).show();

        // Lưu trạng thái "Remember Me" dựa trên checkbox
        viewModel.saveRememberMePreference(binding.cbRememberMe.isChecked());

        // Đặt lại trạng thái login để tránh kích hoạt lại khi quay lại
        viewModel.resetLoginState();

        // Điều hướng sẽ được xử lý trong navigateBasedOnRole()
        // khi observe userToken
    }

    private void navigateBasedOnRole() {
        // Get the role string from ViewModel
        String roleString = viewModel.getUserToken().getValue() != null ?
                viewModel.getUserToken().getValue().getRole() : null;

        if (roleString == null) {
            showErrorMessage("User role not found. Please contact administrator.");
            viewModel.logout();
            return;
        }

        UserRole userRoleEnum = AreaManager.getUserRoleFromString(roleString);

        if (userRoleEnum == null) {
            showErrorMessage("Unknown user role: " + roleString + ". Please contact administrator.");
            viewModel.logout();
            return;
        }

        AreaManager.navigateToUserArea(this, userRoleEnum);
    }

    private void handleError(String error) {
        // Hiển thị thông báo lỗi trong TextView lỗi chuyên dụng
        showErrorMessage(error);

        // Cũng hiển thị Toast để phản hồi bổ sung
        Toast.makeText(getContext(), error, Toast.LENGTH_LONG).show();

        // Xóa lỗi sau khi hiển thị để tránh hiển thị lại khi xoay màn hình
        viewModel.clearError();
    }

    private void checkLoginStatus() {
        // Nếu user đã login VÀ trạng thái "Remember Me" được chọn
        if (viewModel.isLoggedIn() && viewModel.getRememberMePreference()) {
            Toast.makeText(getContext(), "Auto-logging in...", Toast.LENGTH_SHORT).show();
            // Điều hướng dựa trên vai trò
            // Lưu ý: userToken LiveData đã được set giá trị trong constructor của ViewModel
            // nếu có token đã lưu, nên navigateBasedOnRole() sẽ được gọi thông qua observer.
        } else {
            // Nếu không tự động đăng nhập, đảm bảo trạng thái "Remember Me" trên UI khớp với trạng thái đã lưu
            binding.cbRememberMe.setChecked(viewModel.getRememberMePreference());
            // Có thể thêm logic để đảm bảo các trường input trống nếu không auto-login
            binding.edtEmail.setText("");
            binding.edtPassword.setText("");
            viewModel.logout(); // Đảm bảo không có session cũ nào còn sót lại nếu Remember Me không được chọn
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}