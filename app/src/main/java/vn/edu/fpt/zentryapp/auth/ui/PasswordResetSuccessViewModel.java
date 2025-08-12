package vn.edu.fpt.zentryapp.auth.ui;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import android.os.CountDownTimer;

import vn.edu.fpt.zentryapp.auth.models.SuccessCountdownState;


public class PasswordResetSuccessViewModel extends ViewModel {

    private final MutableLiveData<SuccessCountdownState> _countdownState = new MutableLiveData<>();
    private final MutableLiveData<Boolean> _shouldNavigateToLogin = new MutableLiveData<>();
    private final MutableLiveData<String> _successMessage = new MutableLiveData<>();

    private CountDownTimer countdownTimer;
    private static final int AUTO_REDIRECT_DURATION = 5; // 5 seconds
    private boolean isCountdownActive = false;
    private String userEmail;

    public LiveData<SuccessCountdownState> countdownState() { return _countdownState; }
    public LiveData<Boolean> shouldNavigateToLogin() { return _shouldNavigateToLogin; }
    public LiveData<String> successMessage() { return _successMessage; }

    public void init(String email) {
        this.userEmail = email;

        // Set success message
        _successMessage.setValue("Your password has been reset successfully!");

        // Start auto-redirect countdown
        startCountdown();
    }

    public void navigateImmediately() {
        // Stop countdown and navigate immediately
        if (countdownTimer != null) {
            countdownTimer.cancel();
            isCountdownActive = false;
        }
        _shouldNavigateToLogin.setValue(true);
    }

    public void pauseCountdown() {
        if (countdownTimer != null) {
            countdownTimer.cancel();
            isCountdownActive = false;
        }
    }

    public void resumeCountdown(long remainingMs) {
        if (remainingMs > 0) {
            startCountdownWithDuration((int) (remainingMs / 1000));
        } else {
            _shouldNavigateToLogin.setValue(true);
        }
    }

    public boolean isCountdownActive() {
        return isCountdownActive;
    }

    public long getRemainingTimeMs() {
        // This would need to be tracked if implementing proper pause/resume
        return 0;
    }

    private void startCountdown() {
        startCountdownWithDuration(AUTO_REDIRECT_DURATION);
    }

    private void startCountdownWithDuration(int seconds) {
        if (countdownTimer != null) {
            countdownTimer.cancel();
        }

        isCountdownActive = true;

        countdownTimer = new CountDownTimer(seconds * 1000, 1000) {
            @Override
            public void onTick(long millisUntilFinished) {
                int secondsRemaining = (int) (millisUntilFinished / 1000);
                SuccessCountdownState state = new SuccessCountdownState(
                        "Sign In (" + secondsRemaining + ")",
                        "Redirecting to login in " + secondsRemaining + " seconds",
                        secondsRemaining
                );
                _countdownState.setValue(state);
            }

            @Override
            public void onFinish() {
                isCountdownActive = false;
                _shouldNavigateToLogin.setValue(true);
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
