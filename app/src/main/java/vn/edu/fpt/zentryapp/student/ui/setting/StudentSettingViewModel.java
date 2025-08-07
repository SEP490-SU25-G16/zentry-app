package vn.edu.fpt.zentryapp.student.ui.setting;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import android.content.Context;
import android.util.Log;

import vn.edu.fpt.zentryapp.auth.client.AuthManager;

public class StudentSettingViewModel extends ViewModel {
    private static final String TAG = "StudentSettingVM";

    private final MutableLiveData<Boolean> _logoutSuccess = new MutableLiveData<>();
    private final MutableLiveData<String> _errorMessage = new MutableLiveData<>();

    private AuthManager authManager;

    // Public getters
    public LiveData<Boolean> logoutSuccess() { return _logoutSuccess; }
    public LiveData<String> errorMessage() { return _errorMessage; }

    public void init(Context context, AuthManager authManager) {
        this.authManager = authManager;
    }

    /**
     * Logout user
     */
    public void logout() {
        try {
            if (authManager != null) {
                String userEmail = authManager.getCurrentUserEmail();
                authManager.logout();

                _logoutSuccess.setValue(true);
                Log.d(TAG, "User logged out successfully: " + (userEmail != null ? userEmail : "Unknown"));
            } else {
                _errorMessage.setValue("Auth manager not available");
                Log.e(TAG, "Auth manager is null");
            }
        } catch (Exception e) {
            _errorMessage.setValue("Logout failed: " + e.getMessage());
            Log.e(TAG, "Logout failed", e);
        }
    }
}
