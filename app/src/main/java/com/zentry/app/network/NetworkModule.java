package com.zentry.app.network;

import android.content.Context;
import retrofit2.Retrofit;

/**
 * Centralized Network Module - Entry point cho tất cả network operations
 * Singleton pattern để đảm bảo consistency across app
 */
public final class NetworkModule {
    private static volatile NetworkModule instance;
    private final AuthManager authManager;
    private final Retrofit retrofit;

    private NetworkModule(Context context) {
        this.authManager = new AuthManager(context.getApplicationContext());
        this.retrofit = RetrofitClient.getInstance(authManager);
    }

    /**
     * Thread-safe singleton
     */
    public static NetworkModule getInstance(Context context) {
        if (instance == null) {
            synchronized (NetworkModule.class) {
                if (instance == null) {
                    instance = new NetworkModule(context);
                }
            }
        }
        return instance;
    }

    /**
     * Get AuthManager instance
     */
    public AuthManager getAuthManager() {
        return authManager;
    }

    /**
     * Get Retrofit instance - tự động handle auth
     */
    public Retrofit getRetrofit() {
        return retrofit;
    }

    /**
     * Create API service - works cho tất cả APIs (auth + non-auth)
     * AuthInterceptor sẽ tự động detect và handle
     */
    public <T> T createService(Class<T> serviceClass) {
        return retrofit.create(serviceClass);
    }

    /**
     * Reset toàn bộ network layer (logout, switch account, etc.)
     */
    public static synchronized void reset() {
        if (instance != null) {
            instance.authManager.logout();
            RetrofitClient.reset();
            instance = null;
        }
    }
}