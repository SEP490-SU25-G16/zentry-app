package vn.edu.fpt.zentryapp.auth.client;

import android.util.Log;
import androidx.annotation.NonNull;
import lombok.RequiredArgsConstructor;
import java.io.IOException;
import okhttp3.Interceptor;
import okhttp3.Request;
import okhttp3.Response;

@RequiredArgsConstructor
public class AuthInterceptor implements Interceptor {
    private static final String TAG = "AuthInterceptor";
    private final AuthManager authManager;

    @NonNull
    @Override
    public Response intercept(@NonNull Chain chain) throws IOException {
        Request originalRequest = chain.request();
        String requestUrl = originalRequest.url().toString();

        // Skip auth cho các endpoint không cần authentication
        if (shouldSkipAuth(requestUrl)) {
            return chain.proceed(originalRequest);
        }

        // TODO: Tạm thời comment toàn bộ auth logic, sẽ enable sau
        /*
        String accessToken = authManager.getAccessToken();
        if (accessToken == null) {
            return chain.proceed(originalRequest);
        }

        // Thêm Authorization header
        Request authenticatedRequest = originalRequest.newBuilder()
                .header("Authorization", "Bearer " + accessToken)
                .build();

        Response response = chain.proceed(authenticatedRequest);

        // Handle different response codes
        handleResponseCode(response.code());

        return response;
        */

        // Hiện tại chỉ proceed request gốc không có auth
        return chain.proceed(originalRequest);
    }

    private boolean shouldSkipAuth(String url) {
        // Update URL patterns theo API của bạn
        return url.contains("sign-in") ||           // api/auth/sign-in
//                url.contains("forgot-password") ||   // Forgot password endpoint
                url.contains("api/auth/");           // Skip toàn bộ auth endpoints
    }

    /**
     * Xử lý response codes khác nhau
     * TODO: Sẽ enable lại khi implement full auth
     */
    private void handleResponseCode(int responseCode) {
        switch (responseCode) {
            case 401:
                Log.w(TAG, "Unauthorized access"); // Tạm thời chỉ log
                // authManager.clearTokens(); // Comment tạm thời
                break;
            case 403:
                Log.w(TAG, "Access forbidden for current user");
                break;
            case 500:
                Log.e(TAG, "Server internal error");
                break;
            default:
                break;
        }
    }
}
