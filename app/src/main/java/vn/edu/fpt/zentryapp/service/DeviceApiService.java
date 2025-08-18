package vn.edu.fpt.zentryapp.service;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.GET;
import retrofit2.http.POST;
import retrofit2.http.Path;
import vn.edu.fpt.zentryapp.student.data.model.response.DeviceChangeRequestBody;
import vn.edu.fpt.zentryapp.student.data.model.response.DeviceInfoResponse;

public interface DeviceApiService {
    @GET("api/devices/{deviceId}")
    Call<DeviceInfoResponse> getDeviceInfo(@Path("deviceId") String deviceId);

    @POST("api/devices/request-change")
    Call<Void> requestChangeDevice(@Body DeviceChangeRequestBody body);

}
