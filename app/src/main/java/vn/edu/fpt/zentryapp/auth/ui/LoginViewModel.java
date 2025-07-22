package vn.edu.fpt.zentryapp.auth.ui;

import android.text.TextUtils;
import android.util.Patterns;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import lombok.AllArgsConstructor;
import lombok.Getter;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import vn.edu.fpt.zentryapp.auth.client.AuthManager;
import vn.edu.fpt.zentryapp.auth.models.LoginRequest;
import vn.edu.fpt.zentryapp.auth.models.TokenResponse;
import vn.edu.fpt.zentryapp.auth.services.AuthService;

public class LoginViewModel extends ViewModel {
    // LiveData cho UI state
    private final MutableLiveData<Boolean> _isLoading = new MutableLiveData<>(false);
    private final MutableLiveData<String> _errorMessage = new MutableLiveData<>();
    private final MutableLiveData<LoginSuccess> _loginSuccess = new MutableLiveData<>();
    private final MutableLiveData<String> _emailError = new MutableLiveData<>();
    private final MutableLiveData<String> _passwordError = new MutableLiveData<>();
    // Public getters cho Fragment observe
    public LiveData<Boolean> isLoading() { return _isLoading; }
    public LiveData<String> errorMessage() { return _errorMessage; }
    public LiveData<LoginSuccess> loginSuccess() { return _loginSuccess; }
    public LiveData<String> emailError() { return _emailError; }
    public LiveData<String> passwordError() { return _passwordError; }
    private AuthService authService;
    private AuthManager authManager;
    public void init(AuthService authService, AuthManager authManager) {
        this.authService = authService;
        this.authManager = authManager;
    }

    public void login(String email, String password) {
        // Clear previous errors
        clearErrors();

        // Validate input
        if (!validateInput(email, password)) {
            return;
        }

        // Show loading
        _isLoading.setValue(true);

        performRealLogin(email, password);
    }

    private boolean validateInput(String email, String password) {
        boolean isValid = true;

        if (TextUtils.isEmpty(email)) {
            _emailError.setValue("Email không được để trống");
            isValid = false;
        } else if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            _emailError.setValue("Email không hợp lệ");
            isValid = false;
        }

        if (TextUtils.isEmpty(password)) {
            _passwordError.setValue("Password không được để trống");
            isValid = false;
        } else if (password.length() < 6) {
            _passwordError.setValue("Password phải có ít nhất 6 ký tự");
            isValid = false;
        }

        return isValid;
    }
    private void performRealLogin(String email, String password) {
        LoginRequest request = new LoginRequest(email, password);

        authService.login(request).enqueue(new Callback<TokenResponse>() {
            @Override
            public void onResponse(Call<TokenResponse> call, Response<TokenResponse> response) {
                _isLoading.setValue(false);

                if (response.isSuccessful() && response.body() != null) {
                    TokenResponse tokenResponse = response.body();

                    // Save auth data
                    authManager.saveAuthData(tokenResponse.getToken(), tokenResponse.getUserInfo());

                    // Determine navigation based on role
                    String role = tokenResponse.getUserInfo().getRole();
                    boolean isLecturer = "Lecturer".equalsIgnoreCase(role);

                    _loginSuccess.setValue(new LoginSuccess(
                            tokenResponse.getUserInfo().getEmail(),
                            role,
                            isLecturer
                    ));
                } else {
                    _errorMessage.setValue("Đăng nhập thất bại. Vui lòng kiểm tra thông tin.");
                }
            }

            @Override
            public void onFailure(Call<TokenResponse> call, Throwable t) {
                _isLoading.setValue(false);
                _errorMessage.setValue("Lỗi kết nối. Vui lòng thử lại.");
            }
        });
    }

    private void clearErrors() {
        _errorMessage.setValue(null);
        _emailError.setValue(null);
        _passwordError.setValue(null);
    }

    // Inner class for login success data
    @Getter
    @AllArgsConstructor
    public static class LoginSuccess {
        private final String email;
        private final String role;
        private final boolean isLecturer;
    }
}
