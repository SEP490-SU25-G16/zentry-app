package com.zentry.app.network;

import java.util.concurrent.TimeUnit;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;

import okhttp3.OkHttpClient;
import okhttp3.logging.HttpLoggingInterceptor;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class RetrofitClient {
    private static Retrofit retrofitInstance = null;

    /**
     * Singleton Retrofit instance cho toàn bộ app
     * Tự động handle auth và non-auth APIs
     */
    public static Retrofit getInstance(AuthManager authManager) {
        if (retrofitInstance == null) {
            synchronized (RetrofitClient.class) {
                if (retrofitInstance == null) {
                    retrofitInstance = createRetrofit(authManager);
                }
            }
        }
        return retrofitInstance;
    }

    private static Retrofit createRetrofit(AuthManager authManager) {
        // Logging interceptor
        HttpLoggingInterceptor loggingInterceptor = new HttpLoggingInterceptor();
        loggingInterceptor.setLevel(HttpLoggingInterceptor.Level.BODY);

        OkHttpClient.Builder clientBuilder = new OkHttpClient.Builder()
                .connectTimeout(NetworkConfig.CONNECT_TIMEOUT, TimeUnit.SECONDS)
                .readTimeout(NetworkConfig.READ_TIMEOUT, TimeUnit.SECONDS)
                .writeTimeout(NetworkConfig.WRITE_TIMEOUT, TimeUnit.SECONDS)
                .addInterceptor(loggingInterceptor);

        // Thêm auth interceptor nếu có AuthManager
        if (authManager != null) {
            clientBuilder.addInterceptor(new AuthInterceptor(authManager));
        }

        // SSL configuration cho development (CHỈ dùng khi debug)
//        if (BuildConfig.DEBUG) {
            configureTrustAllSSL(clientBuilder);
//        }

        return new Retrofit.Builder()
                .baseUrl(NetworkConfig.getBaseUrl())
                .client(clientBuilder.build())
                .addConverterFactory(GsonConverterFactory.create())
                .build();
    }

    /**
     * Reset retrofit instance (dùng khi logout hoặc cần refresh toàn bộ)
     */
    public static void reset() {
        synchronized (RetrofitClient.class) {
            retrofitInstance = null;
        }
    }

    private static void configureTrustAllSSL(OkHttpClient.Builder builder) {
        try {
            TrustManager[] trustAllCerts = new TrustManager[]{new TrustAllCerts()};
            SSLContext sslContext = SSLContext.getInstance("SSL");
            sslContext.init(null, trustAllCerts, new java.security.SecureRandom());

            builder.sslSocketFactory(sslContext.getSocketFactory(), new TrustAllCerts())
                    .hostnameVerifier(new TrustAllHostnameVerifier());
        } catch (Exception e) {
            throw new RuntimeException("Failed to configure SSL", e);
        }
    }
}