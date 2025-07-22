package vn.edu.fpt.zentryapp.auth.ui;

import android.os.CountDownTimer;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import org.checkerframework.checker.units.qual.A;

import lombok.AllArgsConstructor;
import lombok.Getter;

public class PasswordResetSuccessViewModel extends ViewModel {

    // Constants
    private static final long REDIRECT_DELAY_MS = 3_000L; // 3 seconds
    private static final long COUNTDOWN_INTERVAL = 1000L; // 1 second

    // LiveData cho UI state
    private final MutableLiveData<CountdownState> _countdownState = new MutableLiveData<>();
    private final MutableLiveData<Boolean> _shouldNavigateToLogin = new MutableLiveData<>();
    private final MutableLiveData<String> _successMessage = new MutableLiveData<>();

    // Public getters cho Fragment observe
    public LiveData<CountdownState> countdownState() {
        return _countdownState;
    }

    public LiveData<Boolean> shouldNavigateToLogin() {
        return _shouldNavigateToLogin;
    }

    public LiveData<String> successMessage() {
        return _successMessage;
    }

    private CountDownTimer countDownTimer;
    private String userEmail;
    private boolean isCountdownActive = false;

    public void init(String email) {
        this.userEmail = email;

        // Set success message
        if (email != null && !email.isEmpty()) {
            _successMessage.setValue("Password reset successfully for " + email);
        } else {
            _successMessage.setValue("Password reset successfully!");
        }
        // Start automatic countdown
        startAutoRedirectCountdown();
    }

    /**
     * Start automatic redirect countdown
     */
    public void startAutoRedirectCountdown() {
        if (isCountdownActive) return;

        isCountdownActive = true;
        long totalSeconds = REDIRECT_DELAY_MS / 1000;

        _countdownState.setValue(new CountdownState(true, totalSeconds));

        countDownTimer = new CountDownTimer(REDIRECT_DELAY_MS, COUNTDOWN_INTERVAL) {
            @Override
            public void onTick(long millisUntilFinished) {
                long secondsRemaining = millisUntilFinished / 1000;
                _countdownState.setValue(new CountdownState(true, secondsRemaining));
            }

            @Override
            public void onFinish() {
                isCountdownActive = false;
                _countdownState.setValue(new CountdownState(false, 0));
                _shouldNavigateToLogin.setValue(true);
            }
        };

        countDownTimer.start();
    }

    /**
     * Stop countdown and navigate immediately
     */
    public void navigateImmediately() {
        if (countDownTimer != null) {
            countDownTimer.cancel();
            isCountdownActive = false;
        }

        _countdownState.setValue(new CountdownState(false, 0));
        _shouldNavigateToLogin.setValue(true);
    }

    /**
     * Pause countdown (when fragment goes to background)
     */
    public void pauseCountdown() {
        if (countDownTimer != null) {
            countDownTimer.cancel();
            isCountdownActive = false;
        }
    }

    /**
     * Resume countdown (when fragment comes back to foreground)
     */
    public void resumeCountdown(long remainingMs) {
        if (isCountdownActive || remainingMs <= 0) return;

        isCountdownActive = true;
        _countdownState.setValue(new CountdownState(true, remainingMs / 1000));

        countDownTimer = new CountDownTimer(remainingMs, COUNTDOWN_INTERVAL) {
            @Override
            public void onTick(long millisUntilFinished) {
                long secondsRemaining = millisUntilFinished / 1000;
                _countdownState.setValue(new CountdownState(true, secondsRemaining));
            }

            @Override
            public void onFinish() {
                isCountdownActive = false;
                _countdownState.setValue(new CountdownState(false, 0));
                _shouldNavigateToLogin.setValue(true);
            }
        };

        countDownTimer.start();
    }

    /**
     * Check if countdown is currently active
     */
    public boolean isCountdownActive() {
        return isCountdownActive;
    }

    /**
     * Get remaining time in milliseconds
     */
    public long getRemainingTimeMs() {
        CountdownState currentState = _countdownState.getValue();
        return currentState != null ? currentState.getSecondsRemaining() * 1000 : 0;
    }


    @Override
    protected void onCleared() {
        super.onCleared();
        if (countDownTimer != null) {
            countDownTimer.cancel();
        }
        isCountdownActive = false;
    }

    // Inner Classes

    /**
     * Countdown state wrapper
     */
    @Getter
    @AllArgsConstructor
    public static class CountdownState {
        private final boolean isActive;
        private final long secondsRemaining;

        public String getCountdownText() {
            if (!isActive) {
                return "Redirecting to Sign In...";
            } else {
                return "Redirecting in " + secondsRemaining + "s";
            }
        }

        public String getButtonText() {
            if (!isActive) {
                return "Sign In";
            } else {
                return "Sign In (" + secondsRemaining + "s)";
            }
        }
    }

    /**
     * Success configuration data (for customization)
     */
    @Getter
    @AllArgsConstructor
    public static class SuccessConfig {
        private final String title;
        private final String message;
        private final String buttonText;
        private final long autoRedirectDelay;

        public static SuccessConfig getDefault() {
            return new SuccessConfig(
                    "Password Reset Successful!",
                    "Your password has been reset successfully",
                    "Sign In",
                    3000L
            );
        }
    }
}
