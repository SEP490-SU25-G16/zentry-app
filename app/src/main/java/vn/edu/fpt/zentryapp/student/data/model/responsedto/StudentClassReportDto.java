package vn.edu.fpt.zentryapp.student.data.model.responsedto;

import com.google.gson.annotations.SerializedName;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@AllArgsConstructor
@NoArgsConstructor
public class StudentClassReportDto {
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

    @SerializedName("LecturerName")
    private String lecturerName;

    @SerializedName("LecturerId")
    private String lecturerId;

    @SerializedName("AttendanceRate")
    private double attendanceRate;
}
