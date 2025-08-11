package vn.edu.fpt.zentryapp.lecturer.data.model.responsedto;

import com.google.gson.annotations.SerializedName;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@AllArgsConstructor
@NoArgsConstructor

public class NextSessionDto {
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

    @SerializedName("Status")
    private String status;
}
