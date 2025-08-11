package vn.edu.fpt.zentryapp.lecturer.data.model.responsedto;

import com.google.gson.annotations.SerializedName;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@AllArgsConstructor
@Getter
@NoArgsConstructor
public class WeeklyOverviewDto {
    @SerializedName("ClassId")
    private String classId;

    @SerializedName("ClassName")
    private String className;

    @SerializedName("CourseCode")
    private String courseCode;

    @SerializedName("SectionCode")
    private String sectionCode;

    @SerializedName("EnrolledStudents")
    private int enrolledStudents;

    @SerializedName("TotalSessions")
    private int totalSessions;

    @SerializedName("CurrentSession")
    private int currentSession;

    @SerializedName("SessionsThisWeek")
    private int sessionsThisWeek;

    @SerializedName("CompletedSessionsThisWeek")
    private int completedSessionsThisWeek;

    @SerializedName("AttendanceRate")
    private double attendanceRate;

    @SerializedName("WeekProgress")
    private WeekProgressDto weekProgress;

    @Getter
    @AllArgsConstructor
    @NoArgsConstructor
    public static class WeekProgressDto {
        @SerializedName("Completed")
        private int completed;

        @SerializedName("Total")
        private int total;

        @SerializedName("Percentage")
        private double percentage;
    }
}

