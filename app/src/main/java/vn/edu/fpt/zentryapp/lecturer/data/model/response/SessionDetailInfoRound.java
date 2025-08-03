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

    public String getDurationDisplay() {
        long hours = duration / (60 * 60 * 1000);
        long minutes = (duration % (60 * 60 * 1000)) / (60 * 1000);

        return String.format(Locale.getDefault(), "%02d:%02d", hours, minutes);
    }
}