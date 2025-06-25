package com.zentry.app.network;

import android.util.Log;

import androidx.annotation.NonNull;

import com.zentry.app.model.response.TokenModel;

import org.json.JSONObject;

import java.io.IOException;

import okhttp3.Interceptor;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class AuthInterceptor implements Interceptor {
    private final AuthManager authManager;

    public AuthInterceptor(AuthManager authManager) {
        this.authManager = authManager;
    }

    @NonNull
    @Override
    public Response intercept(@NonNull Chain chain) throws IOException {
        Request originalRequest = chain.request();
        String accessToken = authManager.getAccessToken();

        if (accessToken == null) {
            return chain.proceed(originalRequest);
        }

        Request authorizedRequest = originalRequest.newBuilder()
                .header(NetworkConfig.HEADER_AUTHORIZATION, NetworkConfig.BEARER_PREFIX + accessToken)
                .build();

        Response response = chain.proceed(authorizedRequest);

        if (response.code() == NetworkConfig.HTTP_UNAUTHORIZED) {
            response.close();

            synchronized (this) {
                String currentToken = authManager.getAccessToken();
                if (!accessToken.equals(currentToken)) {
                    Request newRequest = originalRequest.newBuilder()
                            .header(NetworkConfig.HEADER_AUTHORIZATION, NetworkConfig.BEARER_PREFIX + currentToken)
                            .build();
                    return chain.proceed(newRequest);
                }

                TokenModel newTokens = refreshToken();
                if (newTokens != null) {
                    authManager.saveTokens(newTokens.getAccessToken(), newTokens.getRefreshToken());

                    // Sau khi refresh token, có thể bạn cần cập nhật lại UserInfo
                    // Tuy nhiên, API refresh thường chỉ trả về token, không phải user info đầy đủ.
                    // Nếu API refresh của bạn CŨNG trả về userId và role, bạn nên lưu lại ở đây.
                    // Nếu không, bạn cần cân nhắc logic:
                    // 1. User info chỉ được lưu khi login ban đầu.
                    // 2. Hoặc có một endpoint riêng để lấy user info.
                    // Hiện tại, chúng ta sẽ giữ lại user ID và role đã lưu từ lần login trước
                    // vì API refresh thường không trả về những thông tin đó.

                    Request retryRequest = originalRequest.newBuilder()
                            .header(NetworkConfig.HEADER_AUTHORIZATION, NetworkConfig.BEARER_PREFIX + newTokens.getAccessToken())
                            .build();
                    return chain.proceed(retryRequest);
                } else {
                    authManager.clearTokens();
                }
            }
        }

        return response;
    }

    private TokenModel refreshToken() {
        String refreshToken = authManager.getRefreshToken();
        if (refreshToken == null) {
            return null;
        }

        Log.d("AuthInterceptor", "Refreshing token...");

        try {
            JSONObject jsonBody = new JSONObject();
            jsonBody.put("refreshToken", refreshToken);

            RequestBody requestBody = RequestBody.create(
                    MediaType.parse(NetworkConfig.CONTENT_TYPE_JSON),
                    jsonBody.toString()
            );

            Request request = new Request.Builder()
                    .url(NetworkConfig.getEndpointUrl(NetworkConfig.AUTH_REFRESH_ENDPOINT))
                    .post(requestBody)
                    .build();

            OkHttpClient client = new OkHttpClient();
            Response response = client.newCall(request).execute();

            if (response.isSuccessful() && response.body() != null) {
                String responseBody = response.body().string();
                JSONObject jsonObject = new JSONObject(responseBody);

                // --- CẬP NHẬT DÒNG NÀY ĐỂ SỬ DỤNG CONSTRUCTOR CÓ THAM SỐ ---
                // Lưu ý: userId, role, và expiresIn không có trong response refresh token
                // Nên chúng ta sẽ truyền null/giá trị mặc định.
                // Điều này có nghĩa là TokenModel trả về từ refresh chỉ chứa access/refresh token.
                // User ID và Role sẽ được lấy từ AuthManager (là dữ liệu từ lần login ban đầu).
                TokenModel tokenModel = new TokenModel(
                        jsonObject.getString("accessToken"),
                        jsonObject.getString("refreshToken"),
                        authManager.getUserId(), // Giữ lại userId đã lưu
                        authManager.getUserRole(), // Giữ lại role đã lưu
                        0 // Hoặc một giá trị mặc định cho expiresIn nếu API không trả về
                );

                Log.d("AuthInterceptor", "Token refreshed successfully");
                return tokenModel;
            } else {
                Log.e("AuthInterceptor", "Token refresh failed: " + response.code() + " " + response.message());
            }

        } catch (Exception e) {
            Log.e("AuthInterceptor", "Error refreshing token", e);
        }

        return null;
    }
}