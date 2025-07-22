package vn.edu.fpt.zentryapp.auth;

import androidx.annotation.NonNull;

import java.io.IOException;

import okhttp3.Interceptor;
import okhttp3.Request;
import okhttp3.Response;

/**
 * Interceptor to add authentication headers to requests
 */
public class AuthInterceptor implements Interceptor {
    private final String token;

    /**
     * Constructor with token
     * @param token Authentication token
     */
    public AuthInterceptor(String token) {
        this.token = token;
    }

    @NonNull
    @Override
    public Response intercept(@NonNull Chain chain) throws IOException {
        Request originalRequest = chain.request();
        
        // Skip if token is null or empty
        if (token == null || token.isEmpty()) {
            return chain.proceed(originalRequest);
        }
        
        // Add authorization header
        Request.Builder builder = originalRequest.newBuilder()
                .header("Authorization", "Bearer " + token);
        
        Request request = builder.build();
        return chain.proceed(request);
    }
}
