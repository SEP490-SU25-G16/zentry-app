package vn.edu.fpt.zentryapp.student.data.model.responsedto;

import com.google.gson.annotations.SerializedName;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@AllArgsConstructor
@NoArgsConstructor
public class WeeklyCourseDto {
    @SerializedName("CourseCode")
    private String courseCode;

    @SerializedName("CourseName")
    private String courseName;

    @SerializedName("SectionCode")
    private String sectionCode;

    @SerializedName("TotalSessionsInWeek")
    private int totalSessionsInWeek;

    @SerializedName("AttendedSessions")
    private int attendedSessions;

    @SerializedName("AttendancePercentage")
    private double attendancePercentage;
}
