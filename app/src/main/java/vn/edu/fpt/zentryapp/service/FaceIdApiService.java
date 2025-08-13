package vn.edu.fpt.zentryapp.service;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.POST;
import vn.edu.fpt.zentryapp.lecturer.data.model.request.FaceIdRequestCreateRequest;
import vn.edu.fpt.zentryapp.lecturer.data.model.response.FaceIdRequestCreateResponse;

/** Service interface for Face ID related endpoints */
public interface FaceIdApiService {
    @POST("api/faceid/requests")
    Call<FaceIdRequestCreateResponse> createFaceIdRequest(@Body FaceIdRequestCreateRequest request);
}
