package vn.edu.fpt.zentryapp.lecturer.data.model.response;

import com.google.gson.annotations.SerializedName;

// FinalAttendanceData.java
public class FinalAttendanceData {
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

    @SerializedName("DetailedAttendanceStatus")
    private String detailedAttendanceStatus;

    @SerializedName("LastAttendanceTime")
    private String lastAttendanceTime;

    // Getters
    public String getStudentId() { return studentId; }
    public String getStudentFullName() { return studentFullName; }
    public String getEmail() { return email; }
    public String getPhoneNumber() { return phoneNumber; }
    public String getStatus() { return status; }
    public String getEnrollmentId() { return enrollmentId; }
    public String getEnrolledAt() { return enrolledAt; }
    public String getEnrollmentStatus() { return enrollmentStatus; }
    public String getSessionId() { return sessionId; }
    public String getClassSectionId() { return classSectionId; }
    public String getScheduleId() { return scheduleId; }
    public String getCourseId() { return courseId; }
    public String getClassInfo() { return classInfo; }
    public String getSessionStartTime() { return sessionStartTime; }
    public String getLastAttendanceRecordId() { return lastAttendanceRecordId; }
    public String getDetailedAttendanceStatus() { return detailedAttendanceStatus; }
    public String getLastAttendanceTime() { return lastAttendanceTime; }
}