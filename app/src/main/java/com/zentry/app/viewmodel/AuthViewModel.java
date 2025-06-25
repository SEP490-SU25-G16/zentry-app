package com.zentry.app.viewmodel;

import android.app.Application;
import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.zentry.app.model.request.LoginRequest;
import com.zentry.app.model.response.TokenModel;
import com.zentry.app.network.AuthManager;
import com.zentry.app.repository.AuthRepository;

public class AuthViewModel extends AndroidViewModel {
    private final AuthRepository repository;
    // AuthManager không cần thiết phải là một trường riêng ở đây
    // vì nó đã được truyền vào repository.

    // LiveData cho UI
    private final MutableLiveData<Boolean> loginSuccess = new MutableLiveData<>();
    private final MutableLiveData<String> error = new MutableLiveData<>();
    private final MutableLiveData<Boolean> loading = new MutableLiveData<>();
    private final MutableLiveData<Boolean> logoutSuccess = new MutableLiveData<>();
    private final MutableLiveData<TokenModel> userToken = new MutableLiveData<>(); // Sẽ chứa thông tin token đầy đủ

    public AuthViewModel(@NonNull Application application) {
        super(application);
        // Khởi tạo AuthManager và truyền vào AuthRepository
        AuthManager authManager = new AuthManager(application.getApplicationContext());
        this.repository = new AuthRepository(authManager);

        repository.setMockingLogin(true);

        // Khởi tạo trạng thái loading
        loading.setValue(false);

        // Khởi tạo userToken nếu đã có token và user info được lưu
        if (repository.isLoggedIn()) {
            // Tạo một TokenModel từ dữ liệu đã lưu
            String accessToken = authManager.getAccessToken();
            String refreshToken = authManager.getRefreshToken();
            String userId = authManager.getUserId();
            String role = authManager.getUserRole();
            // expiresIn không có trong AuthManager, bạn có thể lấy từ lúc login
            // hoặc bỏ qua nếu không cần quản lý thời gian hết hạn ở đây.
            // Để đơn giản, ta có thể khởi tạo TokenModel mà không có expiresIn hoặc đặt giá trị mặc định.
            TokenModel currentToken = new TokenModel(accessToken, refreshToken, userId, role, 0);
            userToken.setValue(currentToken);
        }
    }

    /**
     * Login user
     */
    public void login(LoginRequest request) {
        if (loading.getValue() == Boolean.TRUE) {
            return; // Ngăn chặn nhiều request cùng lúc
        }

        loading.setValue(true);
        error.setValue(null); // Xóa lỗi trước đó

        repository.login(request, new AuthRepository.LoginCallback() {
            @Override
            public void onSuccess(TokenModel tokenModel) {
                loading.setValue(false);
                loginSuccess.setValue(true);
                userToken.setValue(tokenModel); // Cập nhật LiveData với TokenModel đầy đủ
            }

            @Override
            public void onError(String message) {
                loading.setValue(false);
                error.setValue(message);
                loginSuccess.setValue(false);
                userToken.setValue(null); // Xóa token khi có lỗi
            }
        });
    }

    /**
     * Logout user
     */
    public void logout() {
        if (loading.getValue() == Boolean.TRUE) {
            return;
        }

        loading.setValue(true);
        error.setValue(null);

        repository.logout(new AuthRepository.LogoutCallback() {
            @Override
            public void onSuccess() {
                loading.setValue(false);
                logoutSuccess.setValue(true);
                loginSuccess.setValue(false); // Đảm bảo trạng thái login reset
                userToken.setValue(null); // Xóa token khỏi LiveData
            }

            @Override
            public void onError(String message) {
                loading.setValue(false);
                error.setValue(message);
            }
        });
    }

    /**
     * Kiểm tra trạng thái login
     */
    public boolean isLoggedIn() {
        return repository.isLoggedIn();
    }

    /**
     * Lấy user ID hiện tại
     */
    public String getCurrentUserId() {
        return repository.getCurrentUserId();
    }

    /**
     * Clear error state
     */
    public void clearError() {
        error.setValue(null);
    }

    /**
     * Reset login success state
     */
    public void resetLoginState() {
        loginSuccess.setValue(false);
        logoutSuccess.setValue(false);
    }

    /**
     * Lưu trạng thái "Remember Me"
     */
    public void saveRememberMePreference(boolean remember) {
        repository.saveRememberMePreference(remember);
    }

    /**
     * Lấy trạng thái "Remember Me"
     */
    public boolean getRememberMePreference() {
        return repository.getRememberMePreference();
    }

    /**
     * Kiểm tra nếu user là Student (dựa trên role đã lưu)
     */
    public boolean isStudent() {
        String role = repository.getUserRole();
        return role != null && role.equalsIgnoreCase("student");
    }

    /**
     * Kiểm tra nếu user là Lecturer (dựa trên role đã lưu)
     */
    public boolean isLecturer() {
        String role = repository.getUserRole();
        return role != null && role.equalsIgnoreCase("lecturer");
    }

    // Getters cho LiveData
    public LiveData<Boolean> getLoginSuccess() {
        return loginSuccess;
    }

    public LiveData<String> getError() {
        return error;
    }

    public LiveData<Boolean> getLoading() {
        return loading;
    }

    public LiveData<Boolean> getLogoutSuccess() {
        return logoutSuccess;
    }

    public LiveData<TokenModel> getUserToken() {
        return userToken;
    }
}