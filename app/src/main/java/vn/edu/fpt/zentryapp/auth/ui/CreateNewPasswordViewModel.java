package vn.edu.fpt.zentryapp.auth.ui;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import android.text.TextUtils;

import java.util.regex.Pattern;

import lombok.AllArgsConstructor;
import lombok.Getter;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import vn.edu.fpt.zentryapp.auth.client.AuthManager;
import vn.edu.fpt.zentryapp.auth.services.AuthService;

public class CreateNewPasswordViewModel extends ViewModel {

    // Password validation constants
    private static final int MIN_PASSWORD_LENGTH = 8;
    private static final Pattern PASSWORD_PATTERN = Pattern.compile(
            "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[@$!%*?&])[A-Za-z\\d@$!%*?&].*$"
    );

    // LiveData cho UI state
    private final MutableLiveData<Boolean> _isLoading = new MutableLiveData<>(false);
    private final MutableLiveData<String> _errorMessage = new MutableLiveData<>();
    private final MutableLiveData<PasswordValidationResult> _passwordValidation = new MutableLiveData<>();
    private final MutableLiveData<String> _confirmPasswordError = new MutableLiveData<>();
    private final MutableLiveData<CreatePasswordSuccess> _createPasswordSuccess = new MutableLiveData<>();
    private final MutableLiveData<PasswordStrength> _passwordStrength = new MutableLiveData<>();

    // Public getters cho Fragment observe
    public LiveData<Boolean> isLoading() {
        return _isLoading;
    }

    public LiveData<String> errorMessage() {
        return _errorMessage;
    }

    public LiveData<PasswordValidationResult> passwordValidation() {
        return _passwordValidation;
    }

    public LiveData<String> confirmPasswordError() {
        return _confirmPasswordError;
    }

    public LiveData<CreatePasswordSuccess> createPasswordSuccess() {
        return _createPasswordSuccess;
    }

    public LiveData<PasswordStrength> passwordStrength() {
        return _passwordStrength;
    }

    private AuthService authService;
    private AuthManager authManager;
    private String userEmail; // Email from previous screen
    private String verificationToken; // Token from verification step

    public void init(AuthService authService, AuthManager authManager, String email, String token) {
        this.authService = authService;
        this.authManager = authManager;
        this.userEmail = email;
        this.verificationToken = token;
    }

    /**
     * Validate and create new password
     */
    public void createNewPassword(String newPassword, String confirmPassword) {
        // Clear previous errors
        clearErrors();

        // Validate passwords
        PasswordValidationResult validation = validatePasswords(newPassword, confirmPassword);

        if (!validation.isValid()) {
            if (validation.getPasswordError() != null) {
                _passwordValidation.setValue(validation);
            }
            if (validation.getConfirmPasswordError() != null) {
                _confirmPasswordError.setValue(validation.getConfirmPasswordError());
            }
            return;
        }

        // Show loading
        _isLoading.setValue(true);

        // Tạm thời dùng fake reset password
        performFakeResetPassword(newPassword);

        // TODO: Uncomment when ready to use real API
        // performRealResetPassword(newPassword);
    }

    /**
     * Check password strength while typing
     */
    public void checkPasswordStrength(String password) {
        PasswordStrength strength = calculatePasswordStrength(password);
        _passwordStrength.setValue(strength);
    }

    /**
     * Validate passwords
     */
    private PasswordValidationResult validatePasswords(String newPassword, String confirmPassword) {
        String passwordError = null;
        String confirmPasswordError = null;

        // Validate new password
        if (TextUtils.isEmpty(newPassword)) {
            passwordError = "Password không được để trống";
        } else if (newPassword.length() < MIN_PASSWORD_LENGTH) {
            passwordError = "Password phải có ít nhất " + MIN_PASSWORD_LENGTH + " ký tự";
        } else if (!PASSWORD_PATTERN.matcher(newPassword).matches()) {
            passwordError = "Password phải chứa ít nhất 1 chữ hoa, 1 chữ thường, 1 số và 1 ký tự đặc biệt";
        }

        // Validate confirm password
        if (TextUtils.isEmpty(confirmPassword)) {
            confirmPasswordError = "Nhập lại password";
        } else if (!newPassword.equals(confirmPassword)) {
            confirmPasswordError = "Password không khớp";
        }

        boolean isValid = passwordError == null && confirmPasswordError == null;
        return new PasswordValidationResult(isValid, passwordError, confirmPasswordError);
    }

    /**
     * Calculate password strength
     */
    private PasswordStrength calculatePasswordStrength(String password) {
        if (TextUtils.isEmpty(password)) {
            return new PasswordStrength(PasswordStrength.Level.NONE, "");
        }

        int score = 0;
        String feedback = "";

        // Length check
        if (password.length() >= 8) score++;
        if (password.length() >= 12) score++;

        // Character variety checks
        if (password.matches(".*[a-z].*")) score++; // lowercase
        if (password.matches(".*[A-Z].*")) score++; // uppercase
        if (password.matches(".*\\d.*")) score++; // digit
        if (password.matches(".*[@$!%*?&].*")) score++; // special char

        // Determine strength level and feedback
        if (score <= 2) {
            return new PasswordStrength(PasswordStrength.Level.WEAK, "Weak password");
        } else if (score <= 4) {
            return new PasswordStrength(PasswordStrength.Level.MEDIUM, "Medium password");
        } else {
            return new PasswordStrength(PasswordStrength.Level.STRONG, "Strong password");
        }
    }

    /**
     * Fake reset password simulation
     */
    private void performFakeResetPassword(String newPassword) {
        // Simulate network delay
        new android.os.Handler().postDelayed(() -> {
            _isLoading.setValue(false);

            // Simulate success
            _createPasswordSuccess.setValue(new CreatePasswordSuccess(
                    userEmail,
                    "Password has been reset successfully",
                    true
            ));
        }, 2000);
    }

    /**
     * Real reset password API call (commented for now)
     */
    private void performRealResetPassword(String newPassword) {
        /*
        ResetPasswordRequest request = new ResetPasswordRequest(userEmail, verificationToken, newPassword);

        authService.resetPassword(request).enqueue(new Callback<ResetPasswordResponse>() {
            @Override
            public void onResponse(Call<ResetPasswordResponse> call, Response<ResetPasswordResponse> response) {
                _isLoading.setValue(false);

                if (response.isSuccessful() && response.body() != null) {
                    ResetPasswordResponse resetResponse = response.body();

                    if (resetResponse.isSuccess()) {
                        _createPasswordSuccess.setValue(new CreatePasswordSuccess(
                            userEmail,
                            resetResponse.getMessage(),
                            true
                        ));
                    } else {
                        _errorMessage.setValue(resetResponse.getMessage());
                    }
                } else {
                    _errorMessage.setValue("Failed to reset password. Please try again.");
                }
            }

            @Override
            public void onFailure(Call<ResetPasswordResponse> call, Throwable t) {
                _isLoading.setValue(false);
                _errorMessage.setValue("Network error. Please try again.");
            }
        });
        */
    }

    /**
     * Clear all error messages
     */
    private void clearErrors() {
        _errorMessage.setValue(null);
        _confirmPasswordError.setValue(null);
        _passwordValidation.setValue(new PasswordValidationResult(true, null, null));
    }

    // Inner Classes

    /**
     * Password validation result wrapper
     */
    @Getter
    @AllArgsConstructor
    public static class PasswordValidationResult {
        private final boolean valid;
        private final String passwordError;
        private final String confirmPasswordError;
    }

    /**
     * Password strength indicator
     */
    @Getter
    public static class PasswordStrength {
        public enum Level {
            NONE, WEAK, MEDIUM, STRONG
        }

        private final Level level;
        private final String feedback;

        public PasswordStrength(Level level, String feedback) {
            this.level = level;
            this.feedback = feedback;
        }

        public int getColor() {
            switch (level) {
                case WEAK:
                    return 0xFFE53935; // Red
                case MEDIUM:
                    return 0xFFFF9800; // Orange
                case STRONG:
                    return 0xFF4CAF50; // Green
                default:
                    return 0xFFBDBDBD; // Grey
            }
        }

        public int getProgress() {
            switch (level) {
                case WEAK:
                    return 33;
                case MEDIUM:
                    return 66;
                case STRONG:
                    return 100;
                default:
                    return 0;
            }
        }
    }

    /**
     * Create password success data
     */
    @Getter
    @AllArgsConstructor
    public static class CreatePasswordSuccess {
        private final String email;
        private final String message;
        private final boolean success;
    }

    /**
     * Reset password request model (for future API use)
     */
    @Getter
    @AllArgsConstructor
    public static class ResetPasswordRequest {
        private final String email;
        private final String token;
        private final String newPassword;

    }

    /**
     * Reset password response model (for future API use)
     */
    @Getter
    @AllArgsConstructor
    public static class ResetPasswordResponse {
        private final String message;
        private final boolean success;

    }
}
