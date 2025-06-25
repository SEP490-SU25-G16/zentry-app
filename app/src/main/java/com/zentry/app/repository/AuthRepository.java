package com.zentry.app.repository;

import com.zentry.app.api.IAuthenticationAPI;
import com.zentry.app.model.request.LoginRequest;
import com.zentry.app.model.response.TokenModel;
import com.zentry.app.network.AuthManager;
import com.zentry.app.network.RetrofitClient;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class AuthRepository {
    private final IAuthenticationAPI api;
    private final AuthManager authManager;

    private boolean isMockingLogin = false;
    public AuthRepository(AuthManager authManager) {
        this.authManager = authManager;
        this.api = RetrofitClient.getInstance(authManager).create(IAuthenticationAPI.class);
    }

    /**
     * Phương thức để bật/tắt chế độ mock login.
     * Thường dùng cho các môi trường DEBUG/TEST.
     */
    public void setMockingLogin(boolean isMockingLogin) {
        this.isMockingLogin = isMockingLogin;
    }

    /**
     * Login user với callback
     */
    public void login(LoginRequest request, LoginCallback callback) {

        if (isMockingLogin) {
            // --- LOGIC MOCK LOGIN ---
            new android.os.Handler().postDelayed(() -> {
                // Giả lập logic kiểm tra user/pass
                if ("test@example.com".equals(request.getUserName()) && "password".equals(request.getPassword())) {
                    // Tạo một TokenModel giả lập
                    TokenModel mockToken = new TokenModel(
                            "mock_access_token_for_test",
                            "mock_refresh_token_for_test",
                            "mock_user_id_123",
                            "lecturer", // Hoặc "lecturer" để test vai trò khác
                            3600 // Thời gian hết hạn giả lập
                    );

                    // Lưu token và user info giả lập vào AuthManager
                    authManager.saveTokens(mockToken.getAccessToken(), mockToken.getRefreshToken());
                    authManager.saveUserInfo(mockToken.getUserId(), mockToken.getRole());

                    callback.onSuccess(mockToken);
                } else {
                    callback.onError("Mock Login: Invalid test email or password.");
                }
            }, 1000); // Giả lập độ trễ 1 giây để giống gọi API thật
            return; // Quan trọng: Thoát khỏi phương thức sau khi mock
        }

        api.login(request).enqueue(new Callback<TokenModel>() {
            @Override
            public void onResponse(Call<TokenModel> call, Response<TokenModel> response) {
                if (response.isSuccessful() && response.body() != null) {
                    TokenModel tokenModel = response.body();

                    // Lưu tokens vào AuthManager
                    authManager.saveTokens(
                            tokenModel.getAccessToken(),
                            tokenModel.getRefreshToken()
                    );

                    // Lưu thông tin user (ID và Role)
                    // Đảm bảo TokenModel có getUserId() và getRole()
                    authManager.saveUserInfo(tokenModel.getUserId(), tokenModel.getRole()); // <--- CẬP NHẬT: LƯU CẢ ROLE

                    callback.onSuccess(tokenModel);
                } else {
                    String errorMessage = "Invalid credentials";
                    try {
                        if (response.errorBody() != null) {
                            // Parse error message từ server nếu có
                            errorMessage = response.errorBody().string();
                        }
                    } catch (Exception e) {
                        // Giữ default error message
                    }
                    callback.onError(errorMessage);
                }
            }

            @Override
            public void onFailure(Call<TokenModel> call, Throwable t) {
                callback.onError(t.getMessage() != null ? t.getMessage() : "Network error occurred");
            }
        });
    }

    /**
     * Logout user
     */
    public void logout(LogoutCallback callback) {
        // Có thể gọi API logout để invalidate token trên server
        // api.logout().enqueue(...)

        // Clear local tokens và trạng thái remember me
        authManager.logout(); // authManager.logout() đã bao gồm clear tokens và remember me

        // Reset Retrofit instance để clear cache (nếu cần)
        RetrofitClient.reset();

        if (callback != null) {
            callback.onSuccess();
        }
    }

    /**
     * Kiểm tra trạng thái login
     */
    public boolean isLoggedIn() {
        return authManager.isLoggedIn();
    }

    /**
     * Lấy user ID hiện tại
     */
    public String getCurrentUserId() {
        return authManager.getUserId();
    }

    /**
     * Lấy user Role hiện tại
     */
    public String getUserRole() { // <--- BỔ SUNG PHƯƠNG THỨC NÀY
        return authManager.getUserRole();
    }

    /**
     * Lưu trạng thái "Remember Me"
     */
    public void saveRememberMePreference(boolean remember) { // <--- BỔ SUNG PHƯƠNG THỨC NÀY
        authManager.saveRememberMePreference(remember);
    }

    /**
     * Lấy trạng thái "Remember Me"
     */
    public boolean getRememberMePreference() { // <--- BỔ SUNG PHƯƠNG THỨC NÀY
        return authManager.getRememberMePreference();
    }

    /**
     * Callback interface cho login
     */
    public interface LoginCallback {
        void onSuccess(TokenModel tokenModel);
        void onError(String message);
    }

    /**
     * Callback interface cho logout
     */
    public interface LogoutCallback {
        void onSuccess();
        void onError(String message);
    }
}