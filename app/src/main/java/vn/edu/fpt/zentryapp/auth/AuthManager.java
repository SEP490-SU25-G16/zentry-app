package vn.edu.fpt.zentryapp.auth;

import android.content.Context;
import android.content.SharedPreferences;

/**
 * Manager class for authentication related operations
 */
public class AuthManager {
    private static final String PREF_NAME = "auth_prefs";
    private static final String KEY_TOKEN = "auth_token";
    private static final String KEY_USER_ID = "user_id";
    
    private static AuthManager instance;
    private final SharedPreferences preferences;
    
    private AuthManager(Context context) {
        preferences = context.getApplicationContext().getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
    }
    
    /**
     * Get singleton instance of AuthManager
     * @param context Application context
     * @return AuthManager instance
     */
    public static synchronized AuthManager getInstance(Context context) {
        if (instance == null) {
            instance = new AuthManager(context.getApplicationContext());
        }
        return instance;
    }
    
    /**
     * Save authentication token
     * @param token Authentication token
     */
    public void saveToken(String token) {
        preferences.edit().putString(KEY_TOKEN, token).apply();
    }
    
    /**
     * Get authentication token
     * @return Authentication token or null if not available
     */
    public String getToken() {
        return preferences.getString(KEY_TOKEN, null);
    }
    
    /**
     * Save user ID
     * @param userId User ID
     */
    public void saveUserId(String userId) {
        preferences.edit().putString(KEY_USER_ID, userId).apply();
    }
    
    /**
     * Get user ID
     * @return User ID or null if not available
     */
    public String getUserId() {
        return preferences.getString(KEY_USER_ID, "user123"); // Default value for testing
    }
    
    /**
     * Check if user is logged in
     * @return true if user is logged in, false otherwise
     */
    public boolean isLoggedIn() {
        return getToken() != null;
    }
    
    /**
     * Clear authentication data (logout)
     */
    public void logout() {
        preferences.edit().clear().apply();
    }
}
