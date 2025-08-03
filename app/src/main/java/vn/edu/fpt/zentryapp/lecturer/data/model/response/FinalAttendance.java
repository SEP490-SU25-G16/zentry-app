package vn.edu.fpt.zentryapp.lecturer.data.model.response;

import com.google.gson.annotations.SerializedName;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class FinalAttendance {
    @SerializedName("StudentId")
    private String studentId;

    @SerializedName("StudentFullName")
    private String studentFullName;

    @SerializedName("Email")
    private String email;

    @SerializedName("PhoneNumber")
    private String phoneNumber;

    @SerializedName("Status")
    private String status;

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

    @SerializedName("LastAttendanceRecordId")
    private String lastAttendanceRecordId;

    @SerializedName("LastAttendanceTime")
    private String lastAttendanceTime;

}