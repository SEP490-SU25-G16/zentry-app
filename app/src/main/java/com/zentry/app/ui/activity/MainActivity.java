package com.zentry.app.ui.activity;

import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;

import com.zentry.app.R;
import com.zentry.app.api.IAuthenticationAPI;
import com.zentry.app.network.NetworkModule;

public class MainActivity extends AppCompatActivity {
    private NetworkModule networkModule;

    private IAuthenticationAPI authAPI;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Khởi tạo network module - CHỈ CẦN DÒN NÀY
        networkModule = NetworkModule.getInstance(this);

        // Tạo tất cả API services - AuthInterceptor tự động handle auth
        authAPI = networkModule.createService(IAuthenticationAPI.class);
    }
}