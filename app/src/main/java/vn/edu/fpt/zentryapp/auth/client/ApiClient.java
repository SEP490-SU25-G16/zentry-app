package vn.edu.fpt.zentryapp.auth.client;

import android.content.Context;

import okhttp3.OkHttpClient;
import okhttp3.logging.HttpLoggingInterceptor;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;
import java.util.concurrent.TimeUnit;

public class ApiClient {
    // https://khanhlongtran-sep490.online/
    private static final String BASE_URL =  "http://172.20.10.5:8080/"; // ipconfig  => change to call API
    private static Retrofit retrofit = null;

    public static Retrofit getClient(Context context) {
        if (retrofit == null) {
            AuthManager authManager = AuthManager.getInstance(context);

            HttpLoggingInterceptor logging = new HttpLoggingInterceptor();
            logging.setLevel(HttpLoggingInterceptor.Level.BODY);

            OkHttpClient client = new OkHttpClient.Builder()
                    .addInterceptor(new AuthInterceptor(authManager))
                    .addInterceptor(logging) // Chỉ cho development
                    .connectTimeout(200, TimeUnit.SECONDS) // 🔧 NEW: Connection timeout
                    .readTimeout(200, TimeUnit.SECONDS)    // 🔧 NEW: Read timeout for large data
                    .writeTimeout(200, TimeUnit.SECONDS)   // 🔧 NEW: Write timeout for large data
                    .build();

            retrofit = new Retrofit.Builder()
                    .baseUrl(BASE_URL)
                    .client(client)
                    .addConverterFactory(GsonConverterFactory.create())
                    .build();
        }
        return retrofit;
    }
}
