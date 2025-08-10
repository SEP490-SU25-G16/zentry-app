package vn.edu.fpt.zentryapp.service;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.GET;
import retrofit2.http.POST;
import retrofit2.http.Path;
import vn.edu.fpt.zentryapp.lecturer.data.model.response.CalculateAttendanceResponse;
import vn.edu.fpt.zentryapp.lecturer.data.model.response.EndSessionRequest;
import vn.edu.fpt.zentryapp.lecturer.data.model.response.EndSessionResponse;
import vn.edu.fpt.zentryapp.lecturer.data.model.response.RoundResultResponse;
import vn.edu.fpt.zentryapp.lecturer.data.model.response.RoundsDataResponse;
import vn.edu.fpt.zentryapp.lecturer.data.model.response.AttendanceResponse;
import vn.edu.fpt.zentryapp.student.data.model.request.DeviceRegistrationRequest;
import vn.edu.fpt.zentryapp.student.data.model.response.ClassSectionDetailResponse;
import vn.edu.fpt.zentryapp.student.data.model.response.DeviceRegistrationResponse;
import vn.edu.fpt.zentryapp.student.data.model.response.ScheduleDetailResponse;
import vn.edu.fpt.zentryapp.student.data.model.response.StudentFinalAttendanceResponse;

public interface AttendanceApiService {
    @POST("api/attendance/sessions/scan")
    Call<AttendanceApiResponse> submitAttendanceScan(@Body AttendanceModels.AttendanceSubmission submission);
    @GET("api/attendance/sessions/{sessionId}/rounds")
    Call<RoundsDataResponse> getListRounds(@Path("sessionId") String sessionId);
    @GET("api/attendance/sessions/{sessionId}/final")
    Call<AttendanceResponse> getListAttendances(@Path("sessionId") String sessionId);
    @POST("api/attendance/sessions/{sessionId}/rounds/{roundId}/calculate-attendance")
    Call<CalculateAttendanceResponse> calculateAttendance(
            @Path("sessionId") String sessionId,
            @Path("roundId") String roundId
    );
    @GET("api/attendance/sessions/{sessionId}/students/{studentId}/final-result")
    Call<StudentFinalAttendanceResponse> getStudentFinalAttendance(
            @Path("sessionId") String sessionId,
            @Path("studentId") String studentId
    );
    @GET("api/schedules/{scheduleId}/detail")
    Call<ScheduleDetailResponse> getScheduleDetail(
            @Path("scheduleId") String scheduleId
    );

    @GET("api/attendance/rounds/{roundId}/result")
    Call<RoundResultResponse> getRoundResult(@Path("roundId") String roundId);

    @POST("api/devices/register")
    Call<DeviceRegistrationResponse> registerDevice(@Body DeviceRegistrationRequest request);

    @POST("api/attendance/sessions/{sessionId}/end")
    Call<EndSessionResponse> endSession(
            @Path("sessionId") String sessionId,
            @Body EndSessionRequest body
    );

}
