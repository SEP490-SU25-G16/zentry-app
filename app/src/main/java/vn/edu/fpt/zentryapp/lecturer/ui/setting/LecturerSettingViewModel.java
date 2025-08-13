package vn.edu.fpt.zentryapp.lecturer.ui.setting;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import android.os.Handler;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;
import vn.edu.fpt.zentryapp.auth.client.AuthManager;

public class LecturerSettingViewModel extends ViewModel {

    // LiveData cho UI state
    private final MutableLiveData<Boolean> _isLoading = new MutableLiveData<>(false);
    private final MutableLiveData<String> _errorMessage = new MutableLiveData<>();
    private final MutableLiveData<Boolean> _logoutSuccess = new MutableLiveData<>();
    private final MutableLiveData<AppSettings> _appSettings = new MutableLiveData<>();

    // Public getters cho Fragment observe
    public LiveData<Boolean> isLoading() {
        return _isLoading;
    }


    public LiveData<String> errorMessage() {
        return _errorMessage;
    }

    public LiveData<Boolean> logoutSuccess() {
        return _logoutSuccess;
    }

    public LiveData<AppSettings> appSettings() {
        return _appSettings;
    }

    private AuthManager authManager;

    public void init(AuthManager authManager) {
        this.authManager = authManager;
        loadAppSettings();
    }


    /**
     * Load app settings
     */
    private void loadAppSettings() {
        // Simulate loading app settings
        new Handler().postDelayed(() -> {
            AppSettings settings = new AppSettings(
                    true, // notifications enabled
                    false, // dark mode
                    "en", // language
                    true, // face id enabled
                    "1.0.0", // app version
                    "Device Model XYZ" // device info
            );

            _appSettings.setValue(settings);
        }, 300);
    }

    /**
     * Perform logout operation
     */
    public void performLogout() {
        _isLoading.setValue(true);

        // Simulate logout API call
        new Handler().postDelayed(() -> {
            if (authManager != null) {
                // Clear tokens and user data
                authManager.clearTokens();

                _logoutSuccess.setValue(true);
                _isLoading.setValue(false);
            } else {
                _errorMessage.setValue("Logout failed");
                _isLoading.setValue(false);
            }
        }, 1000);
    }

    /**
     * Toggle notification setting
     */
    public void toggleNotifications(boolean enabled) {
        AppSettings currentSettings = _appSettings.getValue();
        if (currentSettings != null) {
            currentSettings.setNotificationsEnabled(enabled);
            _appSettings.setValue(currentSettings);
        }
    }

    /**
     * Extract name from email
     */
    private String extractNameFromEmail(String email) {
        if (email == null || !email.contains("@")) {
            return "Lecturer";
        }

        String username = email.split("@")[0];
        if (username.length() > 0) {
            // Capitalize first letter and replace dots/underscores with spaces
            String name = username.replace(".", " ").replace("_", " ");
            String[] words = name.split(" ");
            StringBuilder result = new StringBuilder();

            for (String word : words) {
                if (word.length() > 0) {
                    result.append(word.substring(0, 1).toUpperCase());
                    if (word.length() > 1) {
                        result.append(word.substring(1).toLowerCase());
                    }
                    result.append(" ");
                }
            }

            return result.toString().trim();
        }

        return "Lecturer";
    }

    /**
     * App settings data
     */
    @Getter
    @Setter
    @AllArgsConstructor
    public static class AppSettings {
        private boolean notificationsEnabled;
        private boolean darkModeEnabled;
        private String language;
        private boolean faceIdEnabled;
        private String appVersion;
        private String deviceInfo;
    }
}
