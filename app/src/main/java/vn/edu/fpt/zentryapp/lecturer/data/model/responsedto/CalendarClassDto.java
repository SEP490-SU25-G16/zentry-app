package vn.edu.fpt.zentryapp.lecturer.data.model.responsedto;

import com.google.gson.annotations.SerializedName;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@AllArgsConstructor
@NoArgsConstructor
public class CalendarClassDto {
    @SerializedName("StartTime")
    private String startTime;

    @SerializedName("CourseName")
    private String courseName;

    @SerializedName("SectionCode")
    private String sectionCode;

    @SerializedName("RoomName")
    private String roomName;

    @SerializedName("Building")
    private String building;

    @SerializedName("SessionId")
    private String sessionId;

    @SerializedName("ClassSectionId")
    private String classSectionId;
}
