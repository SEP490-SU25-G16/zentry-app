package vn.edu.fpt.zentryapp.student.data.model.response;

import com.google.gson.annotations.SerializedName;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class StudentFinalAttendanceDto {
    @SerializedName("StudentId")
    private String studentId;

    @SerializedName("StudentCode")
    private String studentCode;

    @SerializedName("FullName")
    private String fullName;

    @SerializedName("SessionId")
    private String sessionId;

    @SerializedName("SessionStatus")
    private String sessionStatus;

    @SerializedName("FinalAttendancePercentage")
    private double finalAttendancePercentage;

    @SerializedName("TotalRounds")
    private int totalRounds;

    @SerializedName("AttendedRoundsCount")
    private int attendedRoundsCount;

    @SerializedName("MissedRoundsCount")
    private int missedRoundsCount;
    @SerializedName("FinalStatus")      // ✅ Bổ sung trường này
    private String finalStatus;         // "Attended", "Absent", "Future"

    @SerializedName("RoundDetails")
    private List<RoundAttendanceDetailDto> roundDetails = new ArrayList<>();
}