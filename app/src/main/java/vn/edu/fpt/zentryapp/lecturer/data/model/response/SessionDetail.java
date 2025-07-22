package vn.edu.fpt.zentryapp.lecturer.data.model.response;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class SessionDetail {
    private String sessionId;
    private String courseCode;
    private String courseName;
    private String className;
    private String room;
    private int sessionNumber;
    private Date date;
    private String startTime;
    private String endTime;
    private int totalStudents;
    private int presentStudents;
    private String status; // "COMPLETED", "ONGOING", "UPCOMING", "CANCELLED"
    private String description;

    // Helper methods
    public int getAttendancePercentage() {
        if (totalStudents == 0) return 0;
        return (presentStudents * 100) / totalStudents;
    }

    public String getAttendanceSummary() {
        return presentStudents + "/" + totalStudents + " - Attendance";
    }

    public String getFormattedDate() {
        if (date == null) return "";
        SimpleDateFormat format = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());
        return format.format(date);
    }

    public String getSessionTitle() {
        return "Session - " + sessionNumber;
    }

    public String getTimeRange() {
        return startTime + " - " + endTime;
    }
}