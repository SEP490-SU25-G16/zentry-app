package vn.edu.fpt.zentryapp.faceid.data.api;

import okhttp3.MultipartBody;
import okhttp3.RequestBody;
import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.GET;
import retrofit2.http.PATCH;
import retrofit2.http.Multipart;
import retrofit2.http.POST;
import retrofit2.http.Part;
import retrofit2.http.Path;
import vn.edu.fpt.zentryapp.faceid.data.model.response.FaceIdRequestStatusResponse;
import vn.edu.fpt.zentryapp.faceid.data.model.response.FaceIdResponse;
import vn.edu.fpt.zentryapp.faceid.data.model.response.FaceIdVerifyResponse;
import vn.edu.fpt.zentryapp.lecturer.data.model.request.FaceIdRequestCreateRequest;
import vn.edu.fpt.zentryapp.lecturer.data.model.response.FaceIdRequestCreateResponse;

/**
 * API interface for Face ID operations
 */
public interface FaceIdApiController {
    
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
    @POST("api/faceid/requests/{requestId}/verify")
    Call<FaceIdVerifyResponse> verifyFaceId(
            @Path("requestId") String requestId,
            @Part("userId") RequestBody userId,
            @Part MultipartBody.Part embedding,
            @Part("threshold") RequestBody threshold // optional; pass null to omit
    );

    /**
     * Legacy ad-hoc verify (no request binding). Kept for backward compatibility.
     */
    @Multipart
    @POST("api/faceid/verify")
    Call<FaceIdResponse> verifyFaceIdAdhoc(
            @Part MultipartBody.Part embedding,
            @Part("userId") RequestBody userId
    );

    @PATCH("api/faceid/requests/{requestId}/cancel")
    Call<Void> cancelFaceIdRequest(@Path("requestId") String requestId);

    @GET("api/faceid/requests/{requestId}/status")
    Call<FaceIdRequestStatusResponse> getFaceIdRequestStatus(@Path("requestId") String requestId);

    @POST("api/faceid/requests")
    Call<FaceIdRequestCreateResponse> createFaceIdRequest(@Body FaceIdRequestCreateRequest request);
} 