package vn.edu.fpt.zentryapp.lecturer.data.model.response;

import com.google.gson.annotations.SerializedName;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class Round {
    @SerializedName("roundId")
    private String roundId;

    @SerializedName("sessionId")
    private String sessionId;

    @SerializedName("roundNumber")
    private int roundNumber;

    @SerializedName("attendedCount")
    private int attendedCount;

    @SerializedName("totalStudents")
    private int totalStudents;

    @SerializedName("status")
    private String status;

    @SerializedName("startTime")
    private String startTime;

    @SerializedName("endTime")
    private String endTime;

    // Parsed dates for easier use
    private Date startDateTime;
    private Date endDateTime;

    // Helper methods
    public String getAttendanceDisplay() {
        return attendedCount + "/" + totalStudents + " attended";
    }

    public String getRoundTitle() {
        return "Round " + roundNumber;
    }

    public double getAttendancePercentage() {
        return totalStudents > 0 ? (double) attendedCount / totalStudents * 100 : 0;
    }

    public boolean isCompleted() {
        return "COMPLETED".equalsIgnoreCase(status);
    }
}


