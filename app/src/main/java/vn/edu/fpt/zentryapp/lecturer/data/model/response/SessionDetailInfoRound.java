package vn.edu.fpt.zentryapp.lecturer.data.model.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class SessionDetailInfoRound {
    private String sessionId;
    private String courseCode;
    private String courseName;
    private String className;
    private String room;
    private Date sessionDate;
    private Date startTime;
    private Date endTime;
    private int totalStudents;
    private int totalRounds;
    private String status;
    private long duration;

    // Helper methods
    public String getStudentCountDisplay() {
        return totalStudents + " Students";
    }

    // Thêm method formatDuration vào class SessionDetailInfoRound
    private String formatDuration(long milliseconds) {
        if (milliseconds < 0) {
            return "PAST (" + formatDuration(-milliseconds) + " ago)";
        }

        long seconds = milliseconds / 1000;
        long minutes = seconds / 60;
        long hours = minutes / 60;

        if (hours > 0) {
            return hours + "h " + (minutes % 60) + "m " + (seconds % 60) + "s";
        } else if (minutes > 0) {
            return minutes + "m " + (seconds % 60) + "s";
        } else {
            return seconds + "s";
        }
    }

    public String getDurationDisplay() {
        return formatDuration(duration);
    }

}