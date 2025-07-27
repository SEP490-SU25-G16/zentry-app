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
public class CalendarSession {
    private String sessionId;
    private String courseCode;
    private String courseName;
    private String className;
    private String room;
    private Date startTime;
    private Date endTime;
    private Date sessionDate;
    private String status;
    private String sessionType; // "LECTURE", "PRACTICE", "EXAM", "MEETING"

    // Helper methods
    public String getStartTimeDisplay() {
        SimpleDateFormat timeFormat = new SimpleDateFormat("HH:mm", Locale.getDefault());
        return timeFormat.format(startTime);
    }

    public String getTimeRangeDisplay() {
        SimpleDateFormat timeFormat = new SimpleDateFormat("HH:mm", Locale.getDefault());
        return timeFormat.format(startTime) + " - " + timeFormat.format(endTime);
    }

    public String getSessionDescription() {
        return courseName + ": " + className + " - " + room;
    }

    public int getStatusColor() {
        switch (status) {
            case "ONGOING":
                return 0xFF4CAF50; // Green
            case "UPCOMING":
                return 0xFF2196F3; // Blue
            case "COMPLETED":
                return 0xFF9E9E9E; // Grey
            case "CANCELLED":
                return 0xFFE53935; // Red
            default:
                return 0xFFFF4081; // Pink (default timeline color)
        }
    }

    public int getTypeColor() {
        switch (sessionType) {
            case "LECTURE":
                return 0xFFFF4081; // Pink
            case "PRACTICE":
                return 0xFF4CAF50; // Green
            case "EXAM":
                return 0xFFE53935; // Red
            case "MEETING":
                return 0xFFFF9800; // Orange
            default:
                return 0xFF2196F3; // Blue
        }
    }
}
