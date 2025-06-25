package com.zentry.app.network;

import android.content.Context;
import android.content.SharedPreferences;

public class AuthManager {
    private static final String PREF_NAME = "auth_prefs";
    private static final String ACCESS_TOKEN_KEY = "access_token";
    private static final String REFRESH_TOKEN_KEY = "refresh_token";
    private static final String USER_ID_KEY = "user_id";
    private static final String USER_ROLE_KEY = "user_role"; // <--- BỔ SUNG KEY NÀY
    private static final String IS_LOGGED_IN_KEY = "is_logged_in";
    private static final String REMEMBER_ME_KEY = "remember_me"; // <--- BỔ SUNG KEY NÀY

    private final SharedPreferences sharedPreferences;
    private final SharedPreferences.Editor editor;

    public AuthManager(Context context) {
        this.sharedPreferences = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        this.editor = sharedPreferences.edit();
    }

    /**
     * Lưu tokens sau khi login hoặc refresh
     */
    public void saveTokens(String accessToken, String refreshToken) {
        editor.putString(ACCESS_TOKEN_KEY, accessToken);
        editor.putString(REFRESH_TOKEN_KEY, refreshToken);
        editor.putBoolean(IS_LOGGED_IN_KEY, true);
        editor.apply();
    }

    /**
     * Lấy access token
     */
    public String getAccessToken() {
        return sharedPreferences.getString(ACCESS_TOKEN_KEY, null);
    }

    /**
     * Lấy refresh token
     */
    public String getRefreshToken() {
        return sharedPreferences.getString(REFRESH_TOKEN_KEY, null);
    }

    /**
     * Lưu thông tin user (ID và Role)
     */
    public void saveUserInfo(String userId, String role) { // <--- CẬP NHẬT: THÊM THAM SỐ ROLE
        editor.putString(USER_ID_KEY, userId);
        editor.putString(USER_ROLE_KEY, role); // <--- LƯU ROLE
        editor.apply();
    }

    /**
     * Lấy user ID
     */
    public String getUserId() {
        return sharedPreferences.getString(USER_ID_KEY, null);
    }

    /**
     * Lấy user Role
     */
    public String getUserRole() { // <--- BỔ SUNG PHƯƠNG THỨC NÀY
        return sharedPreferences.getString(USER_ROLE_KEY, null);
    }

    /**
     * Kiểm tra user đã login chưa
     */
    public boolean isLoggedIn() {
        return sharedPreferences.getBoolean(IS_LOGGED_IN_KEY, false)
                && getAccessToken() != null
                && getRefreshToken() != null;
    }

    /**
     * Kiểm tra có token không (thường dùng cho việc refresh token)
     */
    public boolean hasValidTokens() {
        return getAccessToken() != null && getRefreshToken() != null;
    }

    /**
     * Lưu trạng thái "Remember Me"
     */
    public void saveRememberMePreference(boolean remember) { // <--- BỔ SUNG PHƯƠNG THỨC NÀY
        editor.putBoolean(REMEMBER_ME_KEY, remember);
        editor.apply();
    }

    /**
     * Lấy trạng thái "Remember Me"
     */
    public boolean getRememberMePreference() { // <--- BỔ SUNG PHƯƠNG THỨC NÀY
        return sharedPreferences.getBoolean(REMEMBER_ME_KEY, false);
    }

    /**
     * Clear tất cả thông tin auth
     */
    public void clearTokens() {
        editor.clear();
        editor.apply();
    }

    /**
     * Logout user
     */
    public void logout() {
        clearTokens();
        // Khi logout, ta cũng nên clear trạng thái "remember me"
        saveRememberMePreference(false); // <--- Thêm dòng này để reset remember me khi logout
    }
}