package vn.edu.fpt.zentryapp.auth.ui;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import android.os.CountDownTimer;
import android.text.TextUtils;

import lombok.AllArgsConstructor;
import lombok.Getter;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import vn.edu.fpt.zentryapp.auth.client.AuthManager;
import vn.edu.fpt.zentryapp.auth.services.AuthService;

public class ForgotPasswordVerifyCodeViewModel extends ViewModel {

    // Constants
    private static final long RESEND_INTERVAL_MS = 60 * 1000; // 60 seconds
    private static final int REQUIRED_CODE_LENGTH = 4;

    // LiveData cho UI state
    private final MutableLiveData<Boolean> _isLoading = new MutableLiveData<>(false);
    private final MutableLiveData<String> _errorMessage = new MutableLiveData<>();
    private final MutableLiveData<String> _codeError = new MutableLiveData<>();
    private final MutableLiveData<VerifyCodeSuccess> _verifyCodeSuccess = new MutableLiveData<>();
    private final MutableLiveData<ResendCodeResult> _resendCodeResult = new MutableLiveData<>();
    private final MutableLiveData<CountdownState> _countdownState = new MutableLiveData<>();

    // Public getters cho Fragment observe
    public LiveData<Boolean> isLoading() {
        return _isLoading;
    }

    public LiveData<String> errorMessage() {
        return _errorMessage;
    }

    public LiveData<String> codeError() {
        return _codeError;
    }

    public LiveData<VerifyCodeSuccess> verifyCodeSuccess() {
        return _verifyCodeSuccess;
    }

    public LiveData<ResendCodeResult> resendCodeResult() {
        return _resendCodeResult;
    }

    public LiveData<CountdownState> countdownState() {
        return _countdownState;
    }

    private AuthService authService;
    private AuthManager authManager;
    private CountDownTimer countDownTimer;
    private String userEmail; // Email from previous screen

    public void init(AuthService authService, AuthManager authManager, String email) {
        this.authService = authService;
        this.authManager = authManager;
        this.userEmail = email;

        // Start initial countdown
        startResendCountdown();
    }

    /**
     * Verify OTP code
     */
    public void verifyCode(String code) {
        // Clear previous errors
        clearErrors();

        // Validate code
        if (!validateCode(code)) {
            return;
        }

        // Show loading
        _isLoading.setValue(true);

        // Tạm thời dùng fake verification
        performFakeVerifyCode(code);

        // TODO: Uncomment when ready to use real API
        // performRealVerifyCode(code);
    }

    /**
     * Resend verification code
     */
    public void resendCode() {
        if (userEmail == null || userEmail.isEmpty()) {
            _errorMessage.setValue("Email not found. Please go back and try again.");
            return;
        }

        _isLoading.setValue(true);

        // Tạm thời dùng fake resend
        performFakeResendCode();

        // TODO: Uncomment when ready to use real API
        // performRealResendCode();
    }

    /**
     * Start resend countdown timer
     */
    public void startResendCountdown() {
        if (countDownTimer != null) {
            countDownTimer.cancel();
        }

        _countdownState.setValue(new CountdownState(false, RESEND_INTERVAL_MS / 1000));

        countDownTimer = new CountDownTimer(RESEND_INTERVAL_MS, 1000) {
            @Override
            public void onTick(long millisUntilFinished) {
                long secondsRemaining = millisUntilFinished / 1000;
                _countdownState.setValue(new CountdownState(false, secondsRemaining));
            }

            @Override
            public void onFinish() {
                _countdownState.setValue(new CountdownState(true, 0));
            }
        };

        countDownTimer.start();
    }

    /**
     * Validate verification code
     */
    private boolean validateCode(String code) {
        if (TextUtils.isEmpty(code)) {
            _codeError.setValue("Verification code is required");
            return false;
        }

        if (code.length() < REQUIRED_CODE_LENGTH) {
            _codeError.setValue("Enter valid " + REQUIRED_CODE_LENGTH + " digit code");
            return false;
        }

        if (!code.matches("\\d+")) {
            _codeError.setValue("Code should contain only numbers");
            return false;
        }

        return true;
    }

    /**
     * Fake verify code simulation
     */
    private void performFakeVerifyCode(String code) {
        // Simulate network delay
        new android.os.Handler().postDelayed(() -> {
            _isLoading.setValue(false);

            // Simulate success for codes like "1234", "0000"
            if ("1234".equals(code) || "0000".equals(code)) {
                // Generate fake token for testing
                String fakeToken = "fake_verification_token_" + System.currentTimeMillis();

                _verifyCodeSuccess.setValue(new VerifyCodeSuccess(
                        userEmail,
                        "Code verified successfully",
                        true,
                        fakeToken // Pass fake token
                ));
            } else {
                _errorMessage.setValue("Invalid verification code. Please try again.");
            }
        }, 1500);
    }

    /**
     * Fake resend code simulation
     */
    private void performFakeResendCode() {
        // Simulate network delay
        new android.os.Handler().postDelayed(() -> {
            _isLoading.setValue(false);

            _resendCodeResult.setValue(new ResendCodeResult(
                    userEmail,
                    "Verification code has been sent again",
                    true
            ));

            // Start countdown again
            startResendCountdown();
        }, 1000);
    }

    /**
     * Real verify code API call (for future use)
     */
    private void performRealVerifyCode(String code) {
    /*
    VerifyCodeRequest request = new VerifyCodeRequest(userEmail, code);

    authService.verifyCode(request).enqueue(new Callback<VerifyCodeResponse>() {
        @Override
        public void onResponse(Call<VerifyCodeResponse> call, Response<VerifyCodeResponse> response) {
            _isLoading.setValue(false);

            if (response.isSuccessful() && response.body() != null) {
                VerifyCodeResponse verifyResponse = response.body();

                if (verifyResponse.isSuccess()) {
                    _verifyCodeSuccess.setValue(new VerifyCodeSuccess(
                        userEmail,
                        verifyResponse.getMessage(),
                        true,
                        verifyResponse.getToken() // Real token từ API response
                    ));
                } else {
                    _errorMessage.setValue(verifyResponse.getMessage());
                }
            } else {
                _errorMessage.setValue("Verification failed. Please try again.");
            }
        }

        @Override
        public void onFailure(Call<VerifyCodeResponse> call, Throwable t) {
            _isLoading.setValue(false);
            _errorMessage.setValue("Network error. Please try again.");
        }
    });
    */
    }

    /**
     * Real resend code API call (commented for now)
     */
    private void performRealResendCode() {
        /*
        ResendCodeRequest request = new ResendCodeRequest(userEmail);

        authService.resendCode(request).enqueue(new Callback<ResendCodeResponse>() {
            @Override
            public void onResponse(Call<ResendCodeResponse> call, Response<ResendCodeResponse> response) {
                _isLoading.setValue(false);

                if (response.isSuccessful() && response.body() != null) {
                    ResendCodeResponse resendResponse = response.body();

                    _resendCodeResult.setValue(new ResendCodeResult(
                        userEmail,
                        resendResponse.getMessage(),
                        resendResponse.isSuccess()
                    ));

                    if (resendResponse.isSuccess()) {
                        startResendCountdown();
                    }
                } else {
                    _errorMessage.setValue("Failed to resend code. Please try again.");
                }
            }

            @Override
            public void onFailure(Call<ResendCodeResponse> call, Throwable t) {
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
        _codeError.setValue(null);
    }

    @Override
    protected void onCleared() {
        super.onCleared();
        if (countDownTimer != null) {
            countDownTimer.cancel();
        }
    }

    // Inner Classes

    /**
     * Countdown state wrapper
     */
    @Getter
    @AllArgsConstructor
    public static class CountdownState {
        private final boolean canResend;
        private final long secondsRemaining;

        public String getDisplayText() {
            if (canResend) {
                return "Resend Code";
            } else {
                return "Resend Code in " + secondsRemaining + "s";
            }
        }
    }

    /**
     * Verify code success data
     */
    @Getter
    @AllArgsConstructor
    public static class VerifyCodeSuccess {
        private final String email;
        private final String message;
        private final boolean success;
        private final String verificationToken;
    }

    /**
     * Resend code result data
     */
    @Getter
    @AllArgsConstructor
    public static class ResendCodeResult {
        private final String email;
        private final String message;
        private final boolean success;
    }

    /**
     * Verify code request model (for future API use)
     */
    @Getter
    @AllArgsConstructor
    public static class VerifyCodeRequest {
        private final String email;
        private final String code;
    }

    /**
     * Verify code response model (for future API use)
     */
    @Getter
    @AllArgsConstructor
    public static class VerifyCodeResponse {
        private final String message;
        private final boolean success;
        private final String token; // Optional: reset token for next step
    }

    /**
     * Resend code request model (for future API use)
     */
    @Getter
    @AllArgsConstructor
    public static class ResendCodeRequest {
        private final String email;
    }

    /**
     * Resend code response model (for future API use)
     */
    @Getter
    @AllArgsConstructor
    public static class ResendCodeResponse {
        private final String message;
        private final boolean success;
    }
}
