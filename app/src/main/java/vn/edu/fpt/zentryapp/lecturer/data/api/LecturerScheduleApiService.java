package vn.edu.fpt.zentryapp.lecturer.data.api;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.GET;
import retrofit2.http.POST;
import retrofit2.http.Path;
import retrofit2.http.Query;
import vn.edu.fpt.zentryapp.lecturer.data.model.response.DailyScheduleResponse;
import vn.edu.fpt.zentryapp.lecturer.data.model.response.StartSessionRequest;
import vn.edu.fpt.zentryapp.lecturer.data.model.response.StartSessionResponse;

public interface LecturerScheduleApiService {
    @GET("api/class-sections/lecturer/daily-schedule")
    Call<DailyScheduleResponse> getDailySchedule(
            @Query("lecturerId") String lecturerId,
            @Query("date") String date
    );
    @POST("api/attendance/sessions/{sessionId}/start")
    Call<StartSessionResponse> startSession(@Path("sessionId") String sessionId, @Body StartSessionRequest request);
}