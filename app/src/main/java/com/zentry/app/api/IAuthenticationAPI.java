package com.zentry.app.api;

import com.zentry.app.model.request.LoginRequest;
import com.zentry.app.model.response.TokenModel;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.POST;

public interface IAuthenticationAPI {
    @POST("api/auth/sign-in")
    Call<TokenModel> login(@Body LoginRequest request);
}
