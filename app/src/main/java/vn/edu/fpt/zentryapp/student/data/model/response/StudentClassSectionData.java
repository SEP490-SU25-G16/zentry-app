package vn.edu.fpt.zentryapp.student.data.model.response;

import com.google.gson.annotations.SerializedName;

import java.util.List;

import vn.edu.fpt.zentryapp.lecturer.data.model.response.SessionData;

// StudentClassSectionData.java
public class StudentClassSectionData {
    @SerializedName("ScheduleId")
    private String scheduleId;

    @SerializedName("ClassSectionId")
    private String classSectionId;

    @SerializedName("CourseId")
    private String courseId;

    @SerializedName("CourseCode")
    private String courseCode;

    @SerializedName("CourseName")
    private String courseName;

    @SerializedName("SectionCode")
    private String sectionCode;

    @SerializedName("LecturerId")
    private String lecturerId;

    @SerializedName("LecturerName")
    private String lecturerName;

    @SerializedName("RoomId")
    private String roomId;

    @SerializedName("RoomName")
    private String roomName;

    @SerializedName("Building")
    private String building;

    @SerializedName("StartTime")
    private String startTime;

    @SerializedName("EndTime")
    private String endTime;

    @SerializedName("Weekday")
    private String weekday;

    @SerializedName("DateInfo")
    private String dateInfo;

    @SerializedName("Sessions")
    private List<SessionData> sessions;

    @SerializedName("StudentId")
    private String studentId;

    // Getters (same as lecturer but add StudentId)
    public String getScheduleId() { return scheduleId; }
    public String getClassSectionId() { return classSectionId; }
    public String getCourseId() { return courseId; }
    public String getCourseCode() { return courseCode; }
    public String getCourseName() { return courseName; }
    public String getSectionCode() { return sectionCode; }
    public String getLecturerId() { return lecturerId; }
    public String getLecturerName() { return lecturerName; }
    public String getRoomId() { return roomId; }
    public String getRoomName() { return roomName; }
    public String getBuilding() { return building; }
    public String getStartTime() { return startTime; }
    public String getEndTime() { return endTime; }
    public String getWeekday() { return weekday; }
    public String getDateInfo() { return dateInfo; }
    public List<SessionData> getSessions() { return sessions; }
    public String getStudentId() { return studentId; }
}
