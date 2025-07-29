package vn.edu.fpt.zentryapp.service;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.GET;
import retrofit2.http.POST;
import retrofit2.http.Path;
import vn.edu.fpt.zentryapp.lecturer.data.model.response.CalculateAttendanceResponse;
import vn.edu.fpt.zentryapp.lecturer.data.model.response.AttendanceRoundsResponse;
import vn.edu.fpt.zentryapp.lecturer.data.model.response.FinalAttendanceResponse;
import vn.edu.fpt.zentryapp.lecturer.data.model.response.RoundsResponse;

public interface AttendanceApiService {
    @POST("api/attendance/sessions/scan")
    Call<AttendanceApiResponse> submitAttendanceScan(@Body AttendanceModels.AttendanceSubmission submission);

    @GET("api/attendance/sessions/{sessionId}/rounds")
    Call<RoundsResponse> getSessionRounds(@Path("sessionId") String sessionId);

    @GET("api/attendance/sessions/{sessionId}/rounds")
    Call<AttendanceRoundsResponse> getAttendanceRounds(@Path("sessionId") String sessionId);
    @GET("api/attendance/sessions/{sessionId}/final")
    Call<FinalAttendanceResponse> getFinalAttendance(@Path("sessionId") String sessionId);
    @POST("api/attendance/sessions/{sessionId}/rounds/{roundId}/calculate-attendance")
    Call<CalculateAttendanceResponse> calculateAttendance(
            @Path("sessionId") String sessionId,
            @Path("roundId") String roundId
    );
}
