package com.zentry.app.model.response;

import com.google.gson.annotations.SerializedName;

public class TokenModel {
    @SerializedName("access_token")
    private String accessToken;
    @SerializedName("refresh_token")
    private String refreshToken;
    @SerializedName("user_id")
    private String userId;
    @SerializedName("role") // <--- BỔ SUNG TRƯỜNG NÀY
    private String role;
    @SerializedName("expires_in") // Nếu có, để quản lý thời gian hết hạn
    private long expiresIn;

    // Constructor
    public TokenModel(String accessToken, String refreshToken, String userId, String role, long expiresIn) {
        this.accessToken = accessToken;
        this.refreshToken = refreshToken;
        this.userId = userId;
        this.role = role;
        this.expiresIn = expiresIn;
    }

    // Getters và Setters
    public String getAccessToken() {
        return accessToken;
    }

    public void setAccessToken(String accessToken) {
        this.accessToken = accessToken;
    }

    public String getRefreshToken() {
        return refreshToken;
    }

    public void setRefreshToken(String refreshToken) {
        this.refreshToken = refreshToken;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public String getRole() { // <--- BỔ SUNG GETTER CHO ROLE
        return role;
    }

    public void setRole(String role) { // <--- BỔ SUNG SETTER CHO ROLE
        this.role = role;
    }

    public long getExpiresIn() {
        return expiresIn;
    }

    public void setExpiresIn(long expiresIn) {
        this.expiresIn = expiresIn;
    }
}