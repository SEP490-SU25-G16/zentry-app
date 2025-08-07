package vn.edu.fpt.zentryapp.student.data.api;

import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.Query;
import vn.edu.fpt.zentryapp.student.data.model.response.StudentDailyScheduleClassSectionResponse;

// StudentScheduleApiService.java
public interface StudentScheduleApiService {
    @GET("api/schedules/student/daily-schedule")
    Call<StudentDailyScheduleClassSectionResponse> getStudentDailyScheduleClassSection(
            @Query("studentId") String studentId,
            @Query("date") String date
    );
}
