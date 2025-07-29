package vn.edu.fpt.zentryapp.lecturer.data.model.response;

import com.google.gson.annotations.SerializedName;

import java.util.List;

public class ClassSectionData {
    @SerializedName("ScheduleId")
    private String scheduleId;

    @SerializedName("ClassSectionId")
    private String classSectionId;

    @SerializedName("CourseCode")
    private String courseCode;

    @SerializedName("CourseName")
    private String courseName;

    @SerializedName("SectionCode")
    private String sectionCode;

    @SerializedName("RoomName")
    private String roomName;

    @SerializedName("Building")
    private String building;

    @SerializedName("StartTime")
    private String startTime;

    @SerializedName("EndTime")
    private String endTime;

    @SerializedName("EnrolledStudentsCount")
    private int enrolledStudentsCount;

    @SerializedName("SessionStatus")
    private String sessionStatus;

    @SerializedName("CanStartSession")
    private boolean canStartSession;

    @SerializedName("DateInfo")
    private String dateInfo;

    @SerializedName("Sessions")
    private List<SessionData> sessions;

    // Getters
    public String getScheduleId() { return scheduleId; }
    public String getClassSectionId() { return classSectionId; }
    public String getCourseCode() { return courseCode; }
    public String getCourseName() { return courseName; }
    public String getSectionCode() { return sectionCode; }
    public String getRoomName() { return roomName; }
    public String getBuilding() { return building; }
    public String getStartTime() { return startTime; }
    public String getEndTime() { return endTime; }
    public int getEnrolledStudentsCount() { return enrolledStudentsCount; }
    public String getSessionStatus() { return sessionStatus; }
    public boolean isCanStartSession() { return canStartSession; }
    public String getDateInfo() { return dateInfo; }
    public List<SessionData> getSessions() { return sessions; }
}