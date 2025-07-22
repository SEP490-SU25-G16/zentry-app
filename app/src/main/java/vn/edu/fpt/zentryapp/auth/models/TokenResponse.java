package vn.edu.fpt.zentryapp.auth.models;

import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class TokenResponse {
    private String token;
    private UserInfo userInfo;
}