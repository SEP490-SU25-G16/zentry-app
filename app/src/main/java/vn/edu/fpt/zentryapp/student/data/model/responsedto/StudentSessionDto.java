package vn.edu.fpt.zentryapp.student.data.model.responsedto;

import com.google.gson.annotations.SerializedName;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@AllArgsConstructor
@NoArgsConstructor
public class StudentSessionDto {
    @SerializedName("SessionId")
    private String sessionId;

    @SerializedName("SessionNumber")
    private int sessionNumber;

    @SerializedName("SessionName")
    private String sessionName;

    @SerializedName("SessionDate")
    private String sessionDate;

    @SerializedName("StartTime")
    private String startTime;

    @SerializedName("EndTime")
    private String endTime;

    @SerializedName("RoomInfo")
    private String roomInfo;

    @SerializedName("AttendanceStatus")
    private String attendanceStatus;
}
