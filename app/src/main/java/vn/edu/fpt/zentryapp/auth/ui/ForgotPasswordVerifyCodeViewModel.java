package vn.edu.fpt.zentryapp.auth.ui;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import android.os.CountDownTimer;
import android.os.Handler;

import vn.edu.fpt.zentryapp.auth.client.AuthManager;
import vn.edu.fpt.zentryapp.auth.models.CountdownState;
import vn.edu.fpt.zentryapp.auth.models.ForgotPasswordResult;
import vn.edu.fpt.zentryapp.auth.models.VerifyCodeResult;
import vn.edu.fpt.zentryapp.auth.services.AuthService;

public class ForgotPasswordVerifyCodeViewModel extends ViewModel {

    private final MutableLiveData<Boolean> _isLoading = new MutableLiveData<>(false);
    private final MutableLiveData<String> _codeError = new MutableLiveData<>();
    private final MutableLiveData<String> _errorMessage = new MutableLiveData<>();
    private final MutableLiveData<CountdownState> _countdownState = new MutableLiveData<>();
    private final MutableLiveData<ForgotPasswordResult> _resendCodeResult = new MutableLiveData<>();
    private final MutableLiveData<VerifyCodeResult> _verifyCodeSuccess = new MutableLiveData<>();

    // Dependencies
    private AuthService authService;
    private AuthManager authManager;
    private String userEmail;

    // Countdown timer
    private CountDownTimer countdownTimer;
    private static final int COUNTDOWN_DURATION = 60; // 60 seconds
    private String validCode = "123456"; // Fake verification code

    public LiveData<Boolean> isLoading() { return _isLoading; }
    public LiveData<String> codeError() { return _codeError; }
    public LiveData<String> errorMessage() { return _errorMessage; }
    public LiveData<CountdownState> countdownState() { return _countdownState; }
    public LiveData<ForgotPasswordResult> resendCodeResult() { return _resendCodeResult; }
    public LiveData<VerifyCodeResult> verifyCodeSuccess() { return _verifyCodeSuccess; }

    public void init(AuthService authService, AuthManager authManager, String email) {
        this.authService = authService;
        this.authManager = authManager;
        this.userEmail = email;

        // Start countdown immediately
        startCountdown();
    }

    public void verifyCode(String code) {
        if (code == null || code.trim().isEmpty()) {
            _codeError.setValue("Please enter verification code");
            return;
        }

        if (code.length() != 6) {
            _codeError.setValue("Verification code must be 6 digits");
            return;
        }

        _isLoading.setValue(true);
        _codeError.setValue(null);
        _errorMessage.setValue(null);

        // Simulate API call with fake data
        new Handler().postDelayed(() -> {
            _isLoading.setValue(false);

            if (validCode.equals(code)) {
                // Success - generate fake token
                String fakeToken = "fake-token-" + System.currentTimeMillis();
                VerifyCodeResult result = new VerifyCodeResult(
                        true,
                        "Code verified successfully",
                        fakeToken
                );
                _verifyCodeSuccess.setValue(result);
            } else {
                _codeError.setValue("Invalid verification code. Please try again.");
            }
        }, 1500);
    }

    public void resendCode() {
        _isLoading.setValue(true);

        // Generate new fake code
        validCode = String.valueOf((int) (Math.random() * 900000) + 100000);

        // Simulate API call
        new Handler().postDelayed(() -> {
            _isLoading.setValue(false);

            ForgotPasswordResult result = new ForgotPasswordResult(
                    true,
                    "New verification code has been sent to your email",
                    userEmail
            );
            _resendCodeResult.setValue(result);

            // Restart countdown
            startCountdown();
        }, 1000);
    }

    private void startCountdown() {
        if (countdownTimer != null) {
            countdownTimer.cancel();
        }

        countdownTimer = new CountDownTimer(COUNTDOWN_DURATION * 1000, 1000) {
            @Override
            public void onTick(long millisUntilFinished) {
                int secondsRemaining = (int) (millisUntilFinished / 1000);
                CountdownState state = new CountdownState(
                        false,
                        "Resend code in " + secondsRemaining + "s",
                        secondsRemaining
                );
                _countdownState.setValue(state);
            }

            @Override
            public void onFinish() {
                CountdownState state = new CountdownState(
                        true,
                        "Resend code",
                        0
                );
                _countdownState.setValue(state);
            }
        };
        countdownTimer.start();
    }

    @Override
    protected void onCleared() {
        super.onCleared();
        if (countdownTimer != null) {
            countdownTimer.cancel();
        }
    }
}
