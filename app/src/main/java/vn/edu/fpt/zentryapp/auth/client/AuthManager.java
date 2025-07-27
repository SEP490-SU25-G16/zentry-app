package vn.edu.fpt.zentryapp.auth.client;

import android.content.Context;
import android.content.SharedPreferences;
import com.google.gson.Gson;
import vn.edu.fpt.zentryapp.auth.models.UserInfo;

public class AuthManager {
    private static final String PREF_NAME = "auth_prefs";
    private static final String ACCESS_TOKEN = "access_token";
    private static final String USER_INFO = "user_info";

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

    // Lưu token và user info (chỉ lưu, chưa dùng để auth)
    public void saveAuthData(String accessToken, UserInfo userInfo) {
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putString(ACCESS_TOKEN, accessToken);
        editor.putString(USER_INFO, gson.toJson(userInfo));
        editor.apply();
    }

    public String getAccessToken() {
        return sharedPreferences.getString(ACCESS_TOKEN, null);
    }

    public UserInfo getUserInfo() {
        String json = sharedPreferences.getString(USER_INFO, null);
        return json != null ? gson.fromJson(json, UserInfo.class) : null;
    }

    // Check đã login chưa (base trên việc có token)
    public boolean isLoggedIn() {
        return getAccessToken() != null;
    }

    public void clearTokens() {
        sharedPreferences.edit().clear().apply();
    }

    // Helper methods để lấy thông tin user
    public String getCurrentUserId() {
        UserInfo userInfo = getUserInfo();
        return userInfo != null ? userInfo.getAccountId() : null;
    }

    public String getCurrentUserEmail() {
        UserInfo userInfo = getUserInfo();
        return userInfo != null ? userInfo.getEmail() : null;
    }

    public String getCurrentUserRole() {
        UserInfo userInfo = getUserInfo();
        return userInfo != null ? userInfo.getRole() : null;
    }
}
