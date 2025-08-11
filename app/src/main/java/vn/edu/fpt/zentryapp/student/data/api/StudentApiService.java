package vn.edu.fpt.zentryapp.student.data.api;

import java.util.List;

import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.Path;
import retrofit2.http.Query;
import vn.edu.fpt.zentryapp.lecturer.data.model.responsedto.ApiResponseDto;
import vn.edu.fpt.zentryapp.lecturer.data.model.responsedto.UserDto;
import vn.edu.fpt.zentryapp.student.data.model.response.StudentDailyScheduleClassSectionResponse;
import vn.edu.fpt.zentryapp.student.data.model.responsedto.StudentClassReportDto;
import vn.edu.fpt.zentryapp.student.data.model.responsedto.StudentHomeDataDto;
import vn.edu.fpt.zentryapp.student.data.model.responsedto.StudentSessionsDataDto;

// StudentScheduleApiService.java
public interface StudentApiService {
    @GET("api/schedules/student/daily-schedule")
    Call<StudentDailyScheduleClassSectionResponse> getStudentDailyScheduleClassSection(
            @Query("studentId") String studentId,
            @Query("date") String date
    );

    @GET("/api/class-sections/students/{student_id}/home")
    Call<ApiResponseDto<StudentHomeDataDto>> getStudentHomeData(@Path("student_id") String studentId);

    @GET("/api/class-sections/{student_id}/classes")
    Call<ApiResponseDto<List<StudentClassReportDto>>> getStudentClasses(@Path("student_id") String studentId);
    @GET("/api/attendance/students/{student_id}/sessions")
    Call<ApiResponseDto<StudentSessionsDataDto>> getStudentSessions(@Path("student_id") String studentId);
    @GET("/api/user/{user_id}")
    Call<ApiResponseDto<UserDto>> getUserProfile(@Path("user_id") String userId);
}
