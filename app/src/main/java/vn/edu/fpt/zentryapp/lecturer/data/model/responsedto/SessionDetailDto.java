package vn.edu.fpt.zentryapp.lecturer.data.model.responsedto;

import com.google.gson.annotations.SerializedName;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class SessionDetailDto {
    @SerializedName("SessionId")
    private String sessionId;

    @SerializedName("SessionNumber")
    private int sessionNumber;

    @SerializedName("SessionName")
    private String sessionName;

    @SerializedName("SessionDate")
    private String sessionDate;

    @SerializedName("SessionTime")
    private String sessionTime;

    @SerializedName("EndTime")
    private String endTime;

    @SerializedName("RoomInfo")
    private String roomInfo;

    @SerializedName("AttendedCount")
    private int attendedCount;

    @SerializedName("TotalStudents")
    private int totalStudents;

    @SerializedName("AttendanceRate")
    private double attendanceRate;

    @SerializedName("Status")
    private String status;

}
