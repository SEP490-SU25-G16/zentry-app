package vn.edu.fpt.zentryapp.lecturer.data.api;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.GET;
import retrofit2.http.POST;
import retrofit2.http.PUT;
import retrofit2.http.Path;
import retrofit2.http.Query;
import vn.edu.fpt.zentryapp.lecturer.data.model.response.ClassSectionResponse;
import vn.edu.fpt.zentryapp.lecturer.data.model.response.LecturerDailyScheduleClassSectionResponse;
import vn.edu.fpt.zentryapp.lecturer.data.model.response.StartSessionRequest;
import vn.edu.fpt.zentryapp.lecturer.data.model.response.LecturerStartSessionResponse;
import vn.edu.fpt.zentryapp.lecturer.data.model.responsedto.ApiResponseDto;
import vn.edu.fpt.zentryapp.lecturer.data.model.responsedto.ClassSessionsDataDto;
import vn.edu.fpt.zentryapp.lecturer.data.model.responsedto.HomeDataDto;
import vn.edu.fpt.zentryapp.lecturer.data.model.responsedto.MonthlyCalendarDataDto;
import vn.edu.fpt.zentryapp.lecturer.data.model.responsedto.SemesterCoursesDataDto;
import vn.edu.fpt.zentryapp.lecturer.data.model.responsedto.SessionAttendanceDataDto;
import vn.edu.fpt.zentryapp.lecturer.data.model.responsedto.UserDto;

public interface LecturerApiService {
    @GET("api/schedules/lecturer/daily-schedule")
    Call<LecturerDailyScheduleClassSectionResponse> getDailySchedule(
            @Query("lecturerId") String lecturerId,
            @Query("date") String date
    );
    @GET("/api/class-sections/lecturers/{lecturer_id}/home")
    Call<ApiResponseDto<HomeDataDto>> getHomeData(@Path("lecturer_id") String lecturerId);
    @POST("api/attendance/sessions/{sessionId}/start")
    Call<LecturerStartSessionResponse> startSession(@Path("sessionId") String sessionId, @Body StartSessionRequest request);
    @GET("/api/courses/lecturer/{lecturer_id}/semesters/{semester_code}")
    Call<ApiResponseDto<SemesterCoursesDataDto>> getSemesterCourses(
            @Path("lecturer_id") String lecturerId,
            @Path("semester_code") String semesterCode
    );
    @GET("/api/class-sections/{class_section_id}/overview-sessions")
    Call<ApiResponseDto<ClassSessionsDataDto>> getClassOverviewSessions(@Path("class_section_id") String classSectionId);
    @GET("/api/attendance/sessions/{session_id}/details")
    Call<ApiResponseDto<SessionAttendanceDataDto>> getSessionAttendanceDetails(@Path("session_id") String sessionId);
    @GET("/api/schedules/lecturer/{lecturer_id}/monthly-calendar")
    Call<ApiResponseDto<MonthlyCalendarDataDto>> getMonthlyCalendar(
            @Path("lecturer_id") String lecturerId,
            @Query("month") int month,
            @Query("year") int year
    );
    @GET("/api/user/{user_id}")
    Call<ApiResponseDto<UserDto>> getUserProfile(@Path("user_id") String userId);
    @PUT("attendance/sessions/{sessionId}/students/{studentId}/status")
    Call<ApiResponseDto<Void>> updateStudentAttendanceStatus(
            @Path("sessionId") String sessionId,
            @Path("studentId") String studentId
    );
}