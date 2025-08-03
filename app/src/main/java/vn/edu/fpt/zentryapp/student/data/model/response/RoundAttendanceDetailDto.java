package vn.edu.fpt.zentryapp.student.data.model.response;

import com.google.gson.annotations.SerializedName;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class RoundAttendanceDetailDto {
    @SerializedName("RoundId")
    private String roundId;

    @SerializedName("RoundNumber")
    private int roundNumber;

    @SerializedName("IsAttended")
    private boolean isAttended;

    @SerializedName("AttendedTime")
    private String attendedTime; // yyyy-MM-dd HH:mm:ss format
}
