package vn.edu.fpt.zentryapp.student.data.model.responsedto;

import com.google.gson.annotations.SerializedName;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@AllArgsConstructor
@NoArgsConstructor

public class StudentNextSessionDto {
    @SerializedName("SessionId")
    private String sessionId;

    @SerializedName("ClassSectionId")
    private String classSectionId;

    @SerializedName("ClassTitle")
    private String classTitle;

    @SerializedName("CourseCode")
    private String courseCode;

    @SerializedName("SectionCode")
    private String sectionCode;

    @SerializedName("StartDate")
    private String startDate;

    @SerializedName("StartTime")
    private String startTime;

    @SerializedName("EndDate")
    private String endDate;

    @SerializedName("EndTime")
    private String endTime;

    @SerializedName("RoomInfo")
    private String roomInfo;

    @SerializedName("EnrolledStudents")
    private int enrolledStudents;

    @SerializedName("LecturerName")
    private String lecturerName;

    @SerializedName("AttendanceStatus")
    private String attendanceStatus;

    @SerializedName("Status")
    private String status;
}
