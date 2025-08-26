package vn.edu.fpt.zentryapp.auth.services;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.POST;
import vn.edu.fpt.zentryapp.auth.models.ApiResponse;
import vn.edu.fpt.zentryapp.auth.models.LoginRequest;
import vn.edu.fpt.zentryapp.auth.models.TokenResponse;

public interface AuthService {
    @POST("api/auth/sign-in")
    Call<ApiResponse<TokenResponse>> login(@Body LoginRequest request);
}
