package vn.edu.fpt.zentryapp.auth.client;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;
import com.google.gson.Gson;
import vn.edu.fpt.zentryapp.auth.models.UserInfo;

public class AuthManager {
    private static final String TAG = "AuthManager";
    private static final String PREF_NAME = "auth_prefs";
    private static final String ACCESS_TOKEN = "access_token";
    private static final String USER_INFO = "user_info";
    private static final String DEVICE_TOKEN = "device_token";
    private static final String DEVICE_ID = "device_id";

    private static AuthManager instance;
    private final SharedPreferences sharedPreferences;
    private final Gson gson;

    // Private constructor để ngăn tạo instance từ bên ngoài
    private AuthManager(Context context) {
        this.sharedPreferences = context.getApplicationContext()
                .getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        this.gson = new Gson();
    }

    // Singleton getInstance method
    public static synchronized AuthManager getInstance(Context context) {
        if (instance == null) {
            instance = new AuthManager(context);
        }
        return instance;
    }

    // ✅ Save auth data after login
    public void saveAuthData(String accessToken, UserInfo userInfo) {
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putString(ACCESS_TOKEN, accessToken);
        editor.putString(USER_INFO, gson.toJson(userInfo));
        editor.apply();

        Log.d(TAG, "Auth data saved for user: " + userInfo.getEmail());
    }

    // ✅ Save device registration data
    public void saveDeviceData(String deviceId, String deviceToken) {
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putString(DEVICE_ID, deviceId);
        editor.putString(DEVICE_TOKEN, deviceToken);
        editor.apply();

        Log.d(TAG, "Device data saved - ID: " + deviceId);
    }

    // Getters
    public String getAccessToken() {
        return sharedPreferences.getString(ACCESS_TOKEN, null);
    }

    public UserInfo getUserInfo() {
        String json = sharedPreferences.getString(USER_INFO, null);
        return json != null ? gson.fromJson(json, UserInfo.class) : null;
    }

    public String getDeviceId() {
        return sharedPreferences.getString(DEVICE_ID, null);
    }

    public String getDeviceToken() {
        return sharedPreferences.getString(DEVICE_TOKEN, null);
    }

    // Check đã login chưa
    public boolean isLoggedIn() {
        String token = getAccessToken();
        UserInfo userInfo = getUserInfo();
        return token != null && userInfo != null;
    }

    // Check device đã được register chưa
    public boolean isDeviceRegistered() {
        String deviceId = getDeviceId();
        String deviceToken = getDeviceToken();
        return deviceId != null && deviceToken != null;
    }

    // User info helpers
    public String getCurrentUserId() {
        UserInfo userInfo = getUserInfo();
        return userInfo != null ? userInfo.getId() : null;
    }

    public String getCurrentUserEmail() {
        UserInfo userInfo = getUserInfo();
        return userInfo != null ? userInfo.getEmail() : null;
    }

    public String getCurrentUserRole() {
        UserInfo userInfo = getUserInfo();
        return userInfo != null ? userInfo.getRole() : null;
    }

    public String getCurrentUserName() {
        UserInfo userInfo = getUserInfo();
        return userInfo != null ? userInfo.getFullName() : null;
    }

    // ✅ NEW: Logout functionality
    public void logout() {
        String userEmail = getCurrentUserEmail();
        // Clear all stored data
        sharedPreferences.edit().clear().apply();

        Log.d(TAG, "User logged out: " + (userEmail != null ? userEmail : "Unknown"));
    }

    // Legacy method for backward compatibility
    public void clearTokens() {
        logout();
    }

    // ✅ NEW: Check if user has specific role
    public boolean hasRole(String role) {
        String currentRole = getCurrentUserRole();
        return currentRole != null && currentRole.equalsIgnoreCase(role);
    }

    // ✅ NEW: Check if user is student
    public boolean isStudent() {
        return hasRole("Student");
    }

    // ✅ NEW: Check if user is lecturer
    public boolean isLecturer() {
        return hasRole("Lecturer");
    }
}
