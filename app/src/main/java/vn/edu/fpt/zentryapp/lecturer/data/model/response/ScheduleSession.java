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
public class ScheduleSession {
    private String sessionId;
    private String courseCode;
    private String courseName;
    private String className;
    private String room;
    private Date startTime;
    private Date endTime;
    private Date sessionDate;
    private String status; // "UPCOMING", "ONGOING", "COMPLETED", "CANCELLED"
    private boolean canStartInstant;
    private boolean canViewDetail;

    // Helper methods
    public String getTimeDisplay() {
        SimpleDateFormat timeFormat = new SimpleDateFormat("HH:mm", Locale.getDefault());
        return timeFormat.format(startTime) + " - " + timeFormat.format(endTime);
    }

    public String getDateTimeDisplay() {
        SimpleDateFormat dayFormat = new SimpleDateFormat("EEEE", Locale.getDefault());
        SimpleDateFormat dateFormat = new SimpleDateFormat("dd/MM", Locale.getDefault());
        SimpleDateFormat timeFormat = new SimpleDateFormat("HH:mm", Locale.getDefault());

        return dayFormat.format(sessionDate) + " " + dateFormat.format(sessionDate) +
                " " + timeFormat.format(startTime) + " - " + timeFormat.format(endTime);
    }

    public String getClassRoomDisplay() {
        return className + " - " + room;
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
                return 0xFF666666; // Default grey
        }
    }

    public String getStatusText() {
        switch (status) {
            case "ONGOING":
                return "In Progress";
            case "UPCOMING":
                return "Scheduled";
            case "COMPLETED":
                return "Completed";
            case "CANCELLED":
                return "Cancelled";
            default:
                return status;
        }
    }

    public boolean isCurrentTimeInSession() {
        long currentTime = System.currentTimeMillis();
        return currentTime >= startTime.getTime() && currentTime <= endTime.getTime();
    }

    public boolean isSessionPassed() {
        return System.currentTimeMillis() > endTime.getTime();
    }

    public boolean isSessionStartingSoon() {
        long currentTime = System.currentTimeMillis();
        long sessionStartTime = startTime.getTime();
        long timeDifference = sessionStartTime - currentTime;

        // Allow starting 15 minutes before session starts
        return timeDifference <= 15 * 60 * 1000L && timeDifference >= 0;
    }
}