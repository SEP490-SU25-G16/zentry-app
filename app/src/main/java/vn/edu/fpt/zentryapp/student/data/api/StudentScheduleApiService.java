package vn.edu.fpt.zentryapp.student.data.api;

import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.Query;
import vn.edu.fpt.zentryapp.student.data.model.response.StudentDailyScheduleResponse;

// StudentScheduleApiService.java
public interface StudentScheduleApiService {
    @GET("api/class-sections/student/daily-schedule")
    Call<StudentDailyScheduleResponse> getDailySchedule(
            @Query("studentId") String studentId,
            @Query("date") String date
    );
}
