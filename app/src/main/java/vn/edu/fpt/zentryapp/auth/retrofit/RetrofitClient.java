package vn.edu.fpt.zentryapp.auth.retrofit;

import java.util.concurrent.TimeUnit;

import okhttp3.OkHttpClient;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;
import vn.edu.fpt.zentryapp.auth.AuthInterceptor;
import vn.edu.fpt.zentryapp.auth.TrustAllCerts;

public class RetrofitClient {
    private static final String BASE_URL = "https://api.zentry.edu.vn/";
    private static Retrofit retrofit = null;

    /**
     * Get Retrofit client without authentication
     * @return Retrofit instance
     */
    public static Retrofit getClient() {
        if (retrofit == null) {
            OkHttpClient client = new OkHttpClient.Builder()
                    .connectTimeout(60, TimeUnit.SECONDS)
                    .readTimeout(60, TimeUnit.SECONDS)
                    .writeTimeout(60, TimeUnit.SECONDS)
                    .sslSocketFactory(TrustAllCerts.getSSLSocketFactory(), TrustAllCerts.getTrustManager())
                    .hostnameVerifier(TrustAllCerts.getHostnameVerifier())
                    .build();

            retrofit = new Retrofit.Builder()
                    .baseUrl(BASE_URL)
                    .client(client)
                    .addConverterFactory(GsonConverterFactory.create())
                    .build();
        }
        return retrofit;
    }

    /**
     * Get Retrofit client with authentication token
     * @param token Authentication token
     * @return Retrofit instance with authentication
     */
    public static Retrofit getClient(String token) {
        OkHttpClient client = new OkHttpClient.Builder()
                .connectTimeout(60, TimeUnit.SECONDS)
                .readTimeout(60, TimeUnit.SECONDS)
                .writeTimeout(60, TimeUnit.SECONDS)
                .sslSocketFactory(TrustAllCerts.getSSLSocketFactory(), TrustAllCerts.getTrustManager())
                .hostnameVerifier(TrustAllCerts.getHostnameVerifier())
                .addInterceptor(new AuthInterceptor(token))
                .build();

        return new Retrofit.Builder()
                .baseUrl(BASE_URL)
                .client(client)
                .addConverterFactory(GsonConverterFactory.create())
                .build();
    }
}
