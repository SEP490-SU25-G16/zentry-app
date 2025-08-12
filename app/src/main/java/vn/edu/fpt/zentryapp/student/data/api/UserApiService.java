package vn.edu.fpt.zentryapp.student.data.api;

import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.Path;
import vn.edu.fpt.zentryapp.student.data.model.response.UserProfileDto;

/**
 * User API for retrieving profile info (including HasFaceId)
 */
public interface UserApiService {
    @GET("api/User/{userId}")
    Call<UserProfileDto> getUser(@Path("userId") String userId);
}


