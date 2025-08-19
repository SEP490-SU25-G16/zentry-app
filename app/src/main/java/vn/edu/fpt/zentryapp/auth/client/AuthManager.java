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
    private static final String HAS_FACE_ID_KEY = "has_face_id";
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

    public void saveAuthData(String accessToken, UserInfo userInfo) {
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putString(ACCESS_TOKEN, accessToken);
        editor.putString(USER_INFO, gson.toJson(userInfo));
        editor.apply();

        Log.d(TAG, "Auth data saved for user: " + userInfo.getEmail());
    }

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
        boolean loggedIn = token != null && userInfo != null;
        return loggedIn;
    }

    // Check device đã được register chưa
    public boolean isDeviceRegistered() {
        String deviceId = getDeviceId();
        String deviceToken = getDeviceToken();
        boolean registered = deviceId != null && deviceToken != null;
        return registered;
    }

    // User info helpers
    public String getCurrentUserId() {
        UserInfo userInfo = getUserInfo();
        String userId = userInfo != null ? userInfo.getId() : null;
        return userId;
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

    public void logout() {
        sharedPreferences.edit().clear().apply();
    }

    public void clearTokens() {
        logout();
    }
    public boolean hasRole(String role) {
        String currentRole = getCurrentUserRole();
        return currentRole != null && currentRole.equalsIgnoreCase(role);
    }

    public boolean isStudent() {
        return hasRole("Student");
    }

    public boolean isLecturer() {
        return hasRole("Lecturer");
    }



    // Per-user key
    private String faceIdKeyForCurrentUser() {
        String uid = getCurrentUserId();
        return (uid != null && !uid.isEmpty())
                ? HAS_FACE_ID_KEY + "_" + uid
                : HAS_FACE_ID_KEY + "_anonymous";
    }

    // Setter
    public void setFaceIdRegistered(boolean value) {
        sharedPreferences.edit()
                .putBoolean(faceIdKeyForCurrentUser(), value)
                .apply();
    }

    // Getter
    public boolean isFaceIdRegistered() {
        return sharedPreferences.getBoolean(faceIdKeyForCurrentUser(), false);
    }

}
