package vn.edu.fpt.zentryapp.lecturer.data.model.responsedto;

import com.google.gson.annotations.SerializedName;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@AllArgsConstructor
@NoArgsConstructor
public class SemesterCourseDto {
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
}
