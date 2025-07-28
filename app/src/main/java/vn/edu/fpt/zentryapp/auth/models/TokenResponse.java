package vn.edu.fpt.zentryapp.auth.models;

import com.google.gson.annotations.SerializedName;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class TokenResponse {
    @SerializedName("Token")
    private String token;
    @SerializedName("UserInfo")
    private UserInfo userInfo;
}