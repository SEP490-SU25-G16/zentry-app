package com.zentry.app.network;

/**
 * Centralized network configuration
 * Tất cả config về network đều nằm ở đây
 */
public final class NetworkConfig {

    // Base URLs
    public static final String BASE_URL = "https://api.zentry.com/"; // Production URL
    public static final String DEV_BASE_URL = "https://dev-api.zentry.com/"; // Development URL

    // API Endpoints
    public static final String AUTH_REFRESH_ENDPOINT = "api/authentication/refresh";
    public static final String AUTH_LOGIN_ENDPOINT = "api/authentication/login";
    public static final String AUTH_LOGOUT_ENDPOINT = "api/authentication/logout";

    // Timeouts (seconds)
    public static final int CONNECT_TIMEOUT = 30;
    public static final int READ_TIMEOUT = 30;
    public static final int WRITE_TIMEOUT = 30;

    // Headers
    public static final String HEADER_AUTHORIZATION = "Authorization";
    public static final String HEADER_CONTENT_TYPE = "Content-Type";
    public static final String HEADER_ACCEPT = "Accept";

    // Values
    public static final String BEARER_PREFIX = "Bearer ";
    public static final String CONTENT_TYPE_JSON = "application/json";

    // HTTP Status Codes
    public static final int HTTP_UNAUTHORIZED = 401;
    public static final int HTTP_FORBIDDEN = 403;

    /**
     * Get base URL based on build type
     */
    public static String getBaseUrl() {
        return BASE_URL;
    }

    /**
     * Get full endpoint URL
     */
    public static String getEndpointUrl(String endpoint) {
        return getBaseUrl() + endpoint;
    }

    // Private constructor để prevent instantiation
    private NetworkConfig() {
        throw new AssertionError("NetworkConfig should not be instantiated");
    }
}