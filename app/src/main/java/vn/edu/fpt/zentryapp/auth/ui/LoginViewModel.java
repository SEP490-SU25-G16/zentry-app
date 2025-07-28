package vn.edu.fpt.zentryapp.auth.ui;

import android.os.Handler;
import android.text.TextUtils;
import android.util.Log;
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
import vn.edu.fpt.zentryapp.auth.models.UserInfo;
import vn.edu.fpt.zentryapp.auth.services.AuthService;

public class LoginViewModel extends ViewModel {
    // LiveData cho UI state
    private final MutableLiveData<Boolean> _isLoading = new MutableLiveData<>(false);
    private final MutableLiveData<String> _errorMessage = new MutableLiveData<>();
    private final MutableLiveData<LoginSuccess> _loginSuccess = new MutableLiveData<>();
    private final MutableLiveData<String> _emailError = new MutableLiveData<>();
    private final MutableLiveData<String> _passwordError = new MutableLiveData<>();

    // Public getters cho Fragment observe
    public LiveData<Boolean> isLoading() {
        return _isLoading;
    }

    public LiveData<String> errorMessage() {
        return _errorMessage;
    }

    public LiveData<LoginSuccess> loginSuccess() {
        return _loginSuccess;
    }

    public LiveData<String> emailError() {
        return _emailError;
    }

    public LiveData<String> passwordError() {
        return _passwordError;
    }

    private AuthService authService;
    private AuthManager authManager;
    // Fake accounts for testing
    private static final String LECTURER_EMAIL = "lecturer@fpt.edu.vn";
    private static final String LECTURER_PASSWORD = "123456";
    private static final String STUDENT_EMAIL = "student@fpt.edu.vn";
    private static final String STUDENT_PASSWORD = "123456";

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

        // Check if using fake accounts first
        if (isFakeAccount(email, password)) {
            performFakeLogin(email, password);
        } else {
            performRealLogin(email, password);
        }
    }

    /**
     * Check if email/password combination is a fake test account
     */
    private boolean isFakeAccount(String email, String password) {
        return (LECTURER_EMAIL.equalsIgnoreCase(email) && LECTURER_PASSWORD.equals(password)) ||
                (STUDENT_EMAIL.equalsIgnoreCase(email) && STUDENT_PASSWORD.equals(password));
    }

    /**
     * Perform fake login with simulated delay
     */
    private void performFakeLogin(String email, String password) {
        // Simulate network delay
        new Handler().postDelayed(() -> {
            try {
                TokenResponse fakeResponse = generateFakeTokenResponse(email);

                // Save auth data
                authManager.saveAuthData(fakeResponse.getToken(), fakeResponse.getUserInfo());

                // Determine navigation based on role
                String role = fakeResponse.getUserInfo().getRole();
                boolean isLecturer = "Lecturer".equalsIgnoreCase(role);

                _loginSuccess.setValue(new LoginSuccess(
                        fakeResponse.getUserInfo().getEmail(),
                        role,
                        isLecturer
                ));

            } catch (Exception e) {
                _errorMessage.setValue("Lỗi fake login: " + e.getMessage());
            } finally {
                _isLoading.setValue(false);
            }
        }, 1500); // 1.5 second delay to simulate real network call
    }

    /**
     * Generate fake token response based on email
     */
    private TokenResponse generateFakeTokenResponse(String email) {
        UserInfo userInfo;
        String token;

        if (LECTURER_EMAIL.equalsIgnoreCase(email)) {
            // Fake Lecturer Account
            userInfo = new UserInfo(
                    "LEC001",
                    LECTURER_EMAIL,
                    "Lecturer"
            );
            token = "fake_lecturer_token_" + System.currentTimeMillis();

        } else if (STUDENT_EMAIL.equalsIgnoreCase(email)) {
            // Fake Student Account
            userInfo = new UserInfo(
                    "STU001",
                    STUDENT_EMAIL,
                    "Student"
            );
            token = "fake_student_token_" + System.currentTimeMillis();

        } else {
            throw new IllegalArgumentException("Unknown fake account");
        }

        return new TokenResponse(token, userInfo);
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
        _isLoading.setValue(true);
        LoginRequest request = new LoginRequest(email, password);

        authService.login(request).enqueue(new Callback<TokenResponse>() {
            @Override
            public void onResponse(Call<TokenResponse> call, Response<TokenResponse> response) {
                _isLoading.setValue(false);

                // ✅ Log response details
                Log.d("LoginViewModel", "Response code: " + response.code());
                Log.d("LoginViewModel", "Response message: " + response.message());
                Log.d("LoginViewModel", "Response body: " + (response.body() != null ? "Not null" : "NULL"));

                if (response.isSuccessful() && response.body() != null) {
                    TokenResponse tokenResponse = response.body();

                    Log.d("LoginViewModel", "Login successful for: " + tokenResponse.getUserInfo().getEmail());

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
                    // ✅ Log error response details
                    Log.e("LoginViewModel", "Login failed - Code: " + response.code());
                    try {
                        if (response.errorBody() != null) {
                            String errorBody = response.errorBody().string();
                            Log.e("LoginViewModel", "Error body: " + errorBody);
                        }
                    } catch (Exception e) {
                        Log.e("LoginViewModel", "Could not read error body", e);
                    }

                    _errorMessage.setValue("Đăng nhập thất bại. Vui lòng kiểm tra thông tin.");
                }
            }

            @Override
            public void onFailure(Call<TokenResponse> call, Throwable t) {
                _isLoading.setValue(false);

                // ✅ Log chi tiết lỗi network
                Log.e("LoginViewModel", "Network call failed", t);
                Log.e("LoginViewModel", "Error type: " + t.getClass().getSimpleName());
                Log.e("LoginViewModel", "Error message: " + t.getMessage());

                // ✅ Log request URL để debug
                if (call.request() != null) {
                    Log.e("LoginViewModel", "Request URL: " + call.request().url());
                    Log.e("LoginViewModel", "Request method: " + call.request().method());
                }

                String errorMessage = "Lỗi kết nối";

                // Chi tiết hóa error message based on exception type
                if (t instanceof java.net.SocketTimeoutException) {
                    errorMessage = "Kết nối quá chậm. Vui lòng thử lại";
                    Log.e("LoginViewModel", "Timeout error");
                } else if (t instanceof java.net.UnknownHostException) {
                    errorMessage = "Không có kết nối internet hoặc server không tồn tại";
                } else if (t instanceof java.net.ConnectException) {
                    errorMessage = "Không thể kết nối đến server";
                    Log.e("LoginViewModel", "Connection refused - Server might be down");
                } else if (t instanceof javax.net.ssl.SSLException) {
                    errorMessage = "Lỗi bảo mật kết nối";
                    Log.e("LoginViewModel", "SSL error");
                }

                _errorMessage.setValue(errorMessage);
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
