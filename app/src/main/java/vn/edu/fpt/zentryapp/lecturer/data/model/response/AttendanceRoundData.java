package vn.edu.fpt.zentryapp.lecturer.data.model.response;

import com.google.gson.annotations.SerializedName;

public class AttendanceRoundData {
    @SerializedName("RoundId")
    private String roundId;

    @SerializedName("SessionId")
    private String sessionId;

    @SerializedName("RoundNumber")
    private int roundNumber;

    @SerializedName("StartTime")
    private String startTime;

    @SerializedName("EndTime")
    private String endTime;

    @SerializedName("AttendedCount")
    private int attendedCount;

    @SerializedName("TotalStudents")
    private int totalStudents;

    @SerializedName("Status")
    private String status;

    @SerializedName("CreatedAt")
    private String createdAt;

    @SerializedName("UpdatedAt")
    private String updatedAt;

    @SerializedName("CourseId")
    private String courseId;

    @SerializedName("CourseCode")
    private String courseCode;

    @SerializedName("CourseName")
    private String courseName;

    @SerializedName("ClassSectionId")
    private String classSectionId;

    @SerializedName("SectionCode")
    private String sectionCode;

    // Getters
    public String getRoundId() { return roundId; }
    public String getSessionId() { return sessionId; }
    public int getRoundNumber() { return roundNumber; }
    public String getStartTime() { return startTime; }
    public String getEndTime() { return endTime; }
    public int getAttendedCount() { return attendedCount; }
    public int getTotalStudents() { return totalStudents; }
    public String getStatus() { return status; }
    public String getCreatedAt() { return createdAt; }
    public String getUpdatedAt() { return updatedAt; }
    public String getCourseId() { return courseId; }
    public String getCourseCode() { return courseCode; }
    public String getCourseName() { return courseName; }
    public String getClassSectionId() { return classSectionId; }
    public String getSectionCode() { return sectionCode; }
}
