package vn.edu.fpt.zentryapp.lecturer.data.model.response;

import com.google.gson.annotations.SerializedName;

import lombok.AllArgsConstructor;
import lombok.Getter;

@AllArgsConstructor
@Getter
public class RoundDetail {
    @SerializedName("RoundId")
    private String roundId;

    @SerializedName("SessionId")
    private String sessionId;

    @SerializedName("RoundNumber")
    private int roundNumber;

    @SerializedName("AttendedCount")
    private int attendedCount;

    @SerializedName("TotalStudents")
    private int totalStudents;

    @SerializedName("Status")
    private String status;

    @SerializedName("StartTime")
    private String startTime;

    @SerializedName("EndTime")
    private String endTime;
}
