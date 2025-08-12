package vn.edu.fpt.zentryapp.lecturer.data.model.response;

import com.google.gson.annotations.SerializedName;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class FinalAttendance {
    @SerializedName("StudentId")
    private String studentId;

    @SerializedName("StudentCode")
    private String studentCode;

    @SerializedName("FullName")
    private String fullName;

    @SerializedName("Email")
    private String email;

    @SerializedName("AttendanceStatus")
    private String attendanceStatus;

    @SerializedName("EnrollmentId")
    private String enrollmentId;

    @SerializedName("EnrolledAt")
    private String enrolledAt;

    @SerializedName("EnrollmentStatus")
    private String enrollmentStatus;

    @SerializedName("SessionId")
    private String sessionId;

    @SerializedName("ClassSectionId")
    private String classSectionId;

    @SerializedName("ScheduleId")
    private String scheduleId;

    @SerializedName("CourseId")
    private String courseId;

    @SerializedName("ClassInfo")
    private String classInfo;

    @SerializedName("SessionStartTime")
    private String sessionStartTime;

    // Helper methods để backward compatibility với code cũ
    public String getStudentFullName() {
        return fullName;
    }

    public String getStatus() {
        return attendanceStatus;
    }
}
