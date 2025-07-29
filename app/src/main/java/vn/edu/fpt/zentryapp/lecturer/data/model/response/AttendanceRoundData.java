package vn.edu.fpt.zentryapp.lecturer.data.model.response;

import com.google.gson.annotations.SerializedName;

import lombok.AllArgsConstructor;
import lombok.Getter;

@AllArgsConstructor
@Getter
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

}
