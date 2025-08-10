package vn.edu.fpt.zentryapp.faceid.data.api;

import okhttp3.MultipartBody;
import okhttp3.RequestBody;
import retrofit2.Call;
import retrofit2.http.Multipart;
import retrofit2.http.POST;
import retrofit2.http.Part;
import vn.edu.fpt.zentryapp.faceid.data.model.response.FaceIdResponse;

/**
 * API interface for Face ID operations
 */
public interface FaceIdApi {
    
    /**
     * Register a new face ID
     * @param embedding Face embedding data
     * @param userId User ID
     * @return Response indicating success or failure
     */
    @Multipart
    @POST("api/faceid/register")
    Call<FaceIdResponse> registerFaceId(
            @Part MultipartBody.Part embedding,
            @Part("userId") RequestBody userId
    );
    
    /**
     * Update an existing face ID
     * @param embedding Face embedding data
     * @param userId User ID
     * @return Response indicating success or failure
     */
    @Multipart
    @POST("api/faceid/update")
    Call<FaceIdResponse> updateFaceId(
            @Part MultipartBody.Part embedding,
            @Part("userId") RequestBody userId
    );
    
    /**
     * Verify a face ID
     * @param embedding Face embedding data
     * @param userId User ID
     * @return Response indicating success or failure
     */
    @Multipart
    @POST("api/faceid/verify")
    Call<FaceIdResponse> verifyFaceId(
            @Part MultipartBody.Part embedding,
            @Part("userId") RequestBody userId
    );
} 