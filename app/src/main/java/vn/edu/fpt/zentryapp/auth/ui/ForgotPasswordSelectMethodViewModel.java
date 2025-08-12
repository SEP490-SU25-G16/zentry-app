package vn.edu.fpt.zentryapp.auth.ui;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import android.os.Handler;
import android.util.Patterns;

import vn.edu.fpt.zentryapp.auth.client.AuthManager;
import vn.edu.fpt.zentryapp.auth.models.ForgotPasswordResult;
import vn.edu.fpt.zentryapp.auth.models.ValidationResult;
import vn.edu.fpt.zentryapp.auth.services.AuthService;

public class ForgotPasswordSelectMethodViewModel extends ViewModel {

    private final MutableLiveData<Boolean> _isLoading = new MutableLiveData<>(false);
    private final MutableLiveData<ValidationResult> _validationResult = new MutableLiveData<>();
    private final MutableLiveData<String> _errorMessage = new MutableLiveData<>();
    private final MutableLiveData<ForgotPasswordResult> _forgotPasswordSuccess = new MutableLiveData<>();

    // Dependencies
    private AuthService authService;
    private AuthManager authManager;

    public LiveData<Boolean> isLoading() { return _isLoading; }
    public LiveData<ValidationResult> validationResult() { return _validationResult; }
    public LiveData<String> errorMessage() { return _errorMessage; }
    public LiveData<ForgotPasswordResult> forgotPasswordSuccess() { return _forgotPasswordSuccess; }

    public void init(AuthService authService, AuthManager authManager) {
        this.authService = authService;
        this.authManager = authManager;
    }

    public void sendForgotPasswordRequest(String email) {
        // Validate email
        ValidationResult validation = validateEmail(email);
        _validationResult.setValue(validation);

        if (!validation.isValid()) {
            return;
        }

        _isLoading.setValue(true);
        _errorMessage.setValue(null);

        // Simulate API call with fake data
        new Handler().postDelayed(() -> {
            _isLoading.setValue(false);

            // Fake success response
            if (isValidEmailFormat(email)) {
                ForgotPasswordResult result = new ForgotPasswordResult(
                        true,
                        "Verification code has been sent to your email",
                        email
                );
                _forgotPasswordSuccess.setValue(result);
            } else {
                _errorMessage.setValue("Email not found in our system");
            }
        }, 2000);
    }

    private ValidationResult validateEmail(String email) {
        if (email == null || email.trim().isEmpty()) {
            return new ValidationResult(false, "Email is required");
        }

        if (!isValidEmailFormat(email)) {
            return new ValidationResult(false, "Please enter a valid email address");
        }

        return new ValidationResult(true, null);
    }

    private boolean isValidEmailFormat(String email) {
        return email != null && Patterns.EMAIL_ADDRESS.matcher(email).matches();
    }
}
