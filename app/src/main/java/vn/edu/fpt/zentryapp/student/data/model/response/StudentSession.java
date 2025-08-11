package vn.edu.fpt.zentryapp.student.data.model.response;

import android.util.Log;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@AllArgsConstructor
@NoArgsConstructor
@Setter
public class StudentSession {
    private String sessionId;
    private int sessionNumber;
    private String sessionName;
    private String sessionDate;
    private String startTime;
    private String endTime;
    private String roomInfo;
    private String attendanceStatus;
    private String courseId;
    private String courseName;
    private String topic;


    public String getTimeRange() {
        if (startTime != null && endTime != null) {
            return startTime + " - " + endTime;
        }
        return "Time not available";
    }

    public String getFormattedDate() {
        try {
            SimpleDateFormat inputFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
            SimpleDateFormat outputFormat = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());
            Date date = inputFormat.parse(sessionDate);
            return date != null ? outputFormat.format(date) : sessionDate;
        } catch (ParseException e) {
            return sessionDate;
        }
    }

    public String getSessionTitle() {
        return sessionName != null ? sessionName : "Session " + sessionNumber;
    }

    public boolean isPresent() {
        return "Present".equalsIgnoreCase(attendanceStatus) || "Attended".equalsIgnoreCase(attendanceStatus);
    }

    public boolean isAbsent() {
        return "Absent".equalsIgnoreCase(attendanceStatus);
    }

    public boolean isLate() {
        return "Late".equalsIgnoreCase(attendanceStatus);
    }

    public String getAttendanceDisplayText() {
        switch (attendanceStatus.toLowerCase()) {
            case "present":
            case "attended":
                return "Present";
            case "absent":
                return "Absent";
            case "late":
                return "Late";
            default:
                return attendanceStatus;
        }
    }

    public int getAttendanceColor() {
        switch (attendanceStatus.toLowerCase()) {
            case "present":
            case "attended":
                return 0xFF4CAF50; // Green
            case "absent":
                return 0xFFE53935; // Red
            case "late":
                return 0xFFFF9800; // Orange
            default:
                return 0xFF757575; // Gray
        }
    }

    // Helper methods
    public boolean isFutureSession() {
        try {
            SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
            Date sessionDate = format.parse(this.sessionDate);
            Date today = new Date();

            if (sessionDate != null) {
                // So sánh chỉ ngày, bỏ qua giờ
                Calendar sessionCal = Calendar.getInstance();
                Calendar todayCal = Calendar.getInstance();

                sessionCal.setTime(sessionDate);
                todayCal.setTime(today);

                return sessionCal.get(Calendar.YEAR) > todayCal.get(Calendar.YEAR) ||
                        (sessionCal.get(Calendar.YEAR) == todayCal.get(Calendar.YEAR) &&
                                sessionCal.get(Calendar.DAY_OF_YEAR) > todayCal.get(Calendar.DAY_OF_YEAR));
            }
        } catch (ParseException e) {
            Log.e("StudentSession", "Error parsing session date", e);
        }
        return false;
    }

    public String getDisplayStatus() {
        if (isFutureSession()) {
            return "Future";
        }
        return getAttendanceDisplayText(); // Existing method
    }

    public int getStatusColor() {
        if (isFutureSession()) {
            return 0xFF9E9E9E; // Gray for future
        }
        return getAttendanceColor(); // Existing method
    }
}
