package vn.edu.fpt.zentryapp.auth.ui;

import android.text.TextUtils;
import android.util.Patterns;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import lombok.AllArgsConstructor;
import lombok.Getter;
import vn.edu.fpt.zentryapp.auth.client.AuthManager;
import vn.edu.fpt.zentryapp.auth.services.AuthService;

public class ForgotPasswordSelectMethodViewModel extends ViewModel {

    // LiveData cho UI state
    private final MutableLiveData<Boolean> _isLoading = new MutableLiveData<>(false);
    private final MutableLiveData<String> _errorMessage = new MutableLiveData<>();
    private final MutableLiveData<String> _emailError = new MutableLiveData<>();
    private final MutableLiveData<ForgotPasswordSuccess> _forgotPasswordSuccess = new MutableLiveData<>();
    private final MutableLiveData<ValidationResult> _validationResult = new MutableLiveData<>();

    // Public getters cho Fragment observe
    public LiveData<Boolean> isLoading() { return _isLoading; }
    public LiveData<String> errorMessage() { return _errorMessage; }
    public LiveData<String> emailError() { return _emailError; }
    public LiveData<ForgotPasswordSuccess> forgotPasswordSuccess() { return _forgotPasswordSuccess; }
    public LiveData<ValidationResult> validationResult() { return _validationResult; }

    private AuthService authService;
    private AuthManager authManager;

    public void init(AuthService authService, AuthManager authManager) {
        this.authService = authService;
        this.authManager = authManager;
    }

    /**
     * Validate email và gửi forgot password request
     */
    public void sendForgotPasswordRequest(String email) {
        // Clear previous errors
        clearErrors();

        // Validate email
        ValidationResult validation = validateEmail(email);
        _validationResult.setValue(validation);

        if (!validation.isValid()) {
            return;
        }

        // Show loading
        _isLoading.setValue(true);

        // Tạm thời dùng fake request
        performFakeForgotPassword(email);

        // TODO: Uncomment when ready to use real API
        // performRealForgotPassword(email);
    }

    /**
     * Validate email input
     */
    private ValidationResult validateEmail(String email) {
        if (TextUtils.isEmpty(email)) {
            return new ValidationResult(false, "Email không được để trống");
        }

        if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            return new ValidationResult(false, "Email không hợp lệ");
        }

        return new ValidationResult(true, null);
    }

    /**
     * Fake forgot password simulation
     */
    private void performFakeForgotPassword(String email) {
        // Simulate network delay
        new android.os.Handler().postDelayed(() -> {
            _isLoading.setValue(false);

            // Simulate success (in real app, this would depend on API response)
            _forgotPasswordSuccess.setValue(new ForgotPasswordSuccess(
                    email,
                    "Verification code has been sent to " + maskEmail(email),
                    true
            ));
        }, 2000); // 2 second delay to simulate network
    }

    /**
     * Real forgot password API call (commented for now)
     */
    private void performRealForgotPassword(String email) {
        /*
        ForgotPasswordRequest request = new ForgotPasswordRequest(email);

        authService.forgotPassword(request).enqueue(new Callback<ForgotPasswordResponse>() {
            @Override
            public void onResponse(Call<ForgotPasswordResponse> call, Response<ForgotPasswordResponse> response) {
                _isLoading.setValue(false);

                if (response.isSuccessful() && response.body() != null) {
                    ForgotPasswordResponse forgotPasswordResponse = response.body();

                    _forgotPasswordSuccess.setValue(new ForgotPasswordSuccess(
                        email,
                        forgotPasswordResponse.getMessage(),
                        true
                    ));
                } else {
                    _errorMessage.setValue("Không thể gửi email reset. Vui lòng thử lại.");
                }
            }

            @Override
            public void onFailure(Call<ForgotPasswordResponse> call, Throwable t) {
                _isLoading.setValue(false);
                _errorMessage.setValue("Lỗi kết nối. Vui lòng thử lại.");
            }
        });
        */
    }

    /**
     * Mask email for security display
     */
    private String maskEmail(String email) {
        if (email == null || !email.contains("@")) {
            return email;
        }

        String[] parts = email.split("@");
        String username = parts[0];
        String domain = parts[1];

        if (username.length() <= 2) {
            return email; // Too short to mask
        }

        String maskedUsername = username.substring(0, 2) + "***" + username.substring(username.length() - 1);
        return maskedUsername + "@" + domain;
    }

    /**
     * Clear all error messages
     */
    private void clearErrors() {
        _errorMessage.setValue(null);
        _emailError.setValue(null);
    }

    // Inner Classes

    /**
     * Validation result wrapper
     */
    @Getter
    @AllArgsConstructor
    public static class ValidationResult {
        private final boolean valid;
        private final String errorMessage;
    }

    /**
     * Forgot password success data
     */
    @Getter
    public static class ForgotPasswordSuccess {
        private final String email;
        private final String message;
        private final boolean success;

        public ForgotPasswordSuccess(String email, String message, boolean success) {
            this.email = email;
            this.message = message;
            this.success = success;
        }
    }

    /**
     * Forgot password request model (for future API use)
     */
    @Getter
    public static class ForgotPasswordRequest {
        private final String email;

        public ForgotPasswordRequest(String email) {
            this.email = email;
        }
    }

    /**
     * Forgot password response model (for future API use)
     */
    @Getter
    public static class ForgotPasswordResponse {
        private final String message;
        private final boolean success;
        private final String token; // Optional: temporary token for verification

        public ForgotPasswordResponse(String message, boolean success, String token) {
            this.message = message;
            this.success = success;
            this.token = token;
        }
    }
}
