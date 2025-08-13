package vn.edu.fpt.zentryapp.auth.ui;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import android.os.Handler;

import vn.edu.fpt.zentryapp.auth.client.AuthManager;
import vn.edu.fpt.zentryapp.auth.models.CreatePasswordResult;
import vn.edu.fpt.zentryapp.auth.models.PasswordStrength;
import vn.edu.fpt.zentryapp.auth.models.PasswordValidation;
import vn.edu.fpt.zentryapp.auth.services.AuthService;

public class CreateNewPasswordViewModel extends ViewModel {

    private final MutableLiveData<Boolean> _isLoading = new MutableLiveData<>(false);
    private final MutableLiveData<PasswordValidation> _passwordValidation = new MutableLiveData<>();
    private final MutableLiveData<String> _confirmPasswordError = new MutableLiveData<>();
    private final MutableLiveData<PasswordStrength> _passwordStrength = new MutableLiveData<>();
    private final MutableLiveData<String> _errorMessage = new MutableLiveData<>();
    private final MutableLiveData<CreatePasswordResult> _createPasswordSuccess = new MutableLiveData<>();

    // Dependencies
    private AuthService authService;
    private AuthManager authManager;
    private String userEmail;
    private String verificationToken;

    public LiveData<Boolean> isLoading() { return _isLoading; }
    public LiveData<PasswordValidation> passwordValidation() { return _passwordValidation; }
    public LiveData<String> confirmPasswordError() { return _confirmPasswordError; }
    public LiveData<PasswordStrength> passwordStrength() { return _passwordStrength; }
    public LiveData<String> errorMessage() { return _errorMessage; }
    public LiveData<CreatePasswordResult> createPasswordSuccess() { return _createPasswordSuccess; }

    public void init(AuthService authService, AuthManager authManager, String email, String token) {
        this.authService = authService;
        this.authManager = authManager;
        this.userEmail = email;
        this.verificationToken = token;
    }

    public void checkPasswordStrength(String password) {
        PasswordStrength strength = calculatePasswordStrength(password);
        _passwordStrength.setValue(strength);
    }

    public void createNewPassword(String newPassword, String confirmPassword) {
        // Validate password
        PasswordValidation passwordValidation = validatePassword(newPassword);
        _passwordValidation.setValue(passwordValidation);

        // Validate confirm password
        String confirmError = validateConfirmPassword(newPassword, confirmPassword);
        _confirmPasswordError.setValue(confirmError);

        if (!passwordValidation.isValid() || confirmError != null) {
            return;
        }

        _isLoading.setValue(true);
        _errorMessage.setValue(null);

        // Simulate API call with fake data
        new Handler().postDelayed(() -> {
            _isLoading.setValue(false);

            // Always success for demo
            CreatePasswordResult result = new CreatePasswordResult(
                    true,
                    "Password has been reset successfully!"
            );
            _createPasswordSuccess.setValue(result);
        }, 2000);
    }

    private PasswordValidation validatePassword(String password) {
        if (password == null || password.isEmpty()) {
            return new PasswordValidation("Password is required", false);
        }

        if (password.length() < 8) {
            return new PasswordValidation("Password must be at least 8 characters", false);
        }

        if (!password.matches(".*[A-Z].*")) {
            return new PasswordValidation("Password must contain at least one uppercase letter", false);
        }

        if (!password.matches(".*[a-z].*")) {
            return new PasswordValidation("Password must contain at least one lowercase letter", false);
        }

        if (!password.matches(".*\\d.*")) {
            return new PasswordValidation("Password must contain at least one number", false);
        }

        return new PasswordValidation(null, true);
    }

    private String validateConfirmPassword(String password, String confirmPassword) {
        if (confirmPassword == null || confirmPassword.isEmpty()) {
            return "Please confirm your password";
        }

        if (!password.equals(confirmPassword)) {
            return "Passwords do not match";
        }

        return null;
    }

    private PasswordStrength calculatePasswordStrength(String password) {
        if (password == null || password.isEmpty()) {
            return new PasswordStrength(0, "Enter a password", 0xFFE53935, PasswordStrength.StrengthLevel.WEAK);
        }

        int score = 0;
        String feedback;
        int color;
        PasswordStrength.StrengthLevel level;

        // Check length
        if (password.length() >= 8) score += 25;
        if (password.length() >= 12) score += 10;

        // Check character types
        if (password.matches(".*[a-z].*")) score += 15;
        if (password.matches(".*[A-Z].*")) score += 15;
        if (password.matches(".*\\d.*")) score += 15;
        if (password.matches(".*[!@#$%^&*()].*")) score += 20;

        // Determine level and color
        if (score < 30) {
            level = PasswordStrength.StrengthLevel.WEAK;
            feedback = "Weak";
            color = 0xFFE53935; // Red
        } else if (score < 60) {
            level = PasswordStrength.StrengthLevel.FAIR;
            feedback = "Fair";
            color = 0xFFFF9800; // Orange
        } else if (score < 85) {
            level = PasswordStrength.StrengthLevel.GOOD;
            feedback = "Good";
            color = 0xFF2196F3; // Blue
        } else {
            level = PasswordStrength.StrengthLevel.STRONG;
            feedback = "Strong";
            color = 0xFF4CAF50; // Green
        }

        return new PasswordStrength(score, feedback, color, level);
    }
}
