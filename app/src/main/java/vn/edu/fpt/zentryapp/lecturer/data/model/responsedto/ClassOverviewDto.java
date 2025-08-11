package vn.edu.fpt.zentryapp.lecturer.data.model.responsedto;

import com.google.gson.annotations.SerializedName;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@AllArgsConstructor
@NoArgsConstructor
public class ClassOverviewDto {
    @SerializedName("ClassId")
    private String classId;

    @SerializedName("CourseName")
    private String courseName;

    @SerializedName("CourseCode")
    private String courseCode;

    @SerializedName("SectionCode")
    private String sectionCode;

    @SerializedName("ClassName")
    private String className;

    @SerializedName("EnrolledStudents")
    private int enrolledStudents;

    @SerializedName("CompletedSessions")
    private int completedSessions;

    @SerializedName("TotalSessions")
    private int totalSessions;

    @SerializedName("AttendanceRate")
    private double attendanceRate;

    @SerializedName("LecturerName")
    private String lecturerName;

    @SerializedName("LecturerId")
    private String lecturerId;

    @SerializedName("RoomInfos")
    private List<String> roomInfos;

    @SerializedName("SemesterInfo")
    private String semesterInfo;

}
