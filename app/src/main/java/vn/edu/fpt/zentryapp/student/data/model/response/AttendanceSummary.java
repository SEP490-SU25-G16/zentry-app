package vn.edu.fpt.zentryapp.student.data.model.response;

import lombok.Getter;

@Getter
public class AttendanceSummary {
    private final String studentId;
    private final String studentName;
    private final String studentEmail;
    private final int totalSessions;
    private final int attendedSessions;
    private final int lateSessions;
    private final int absentSessions;

    public AttendanceSummary(String studentId, String studentName, String studentEmail,
                             int totalSessions, int attendedSessions, int lateSessions, int absentSessions) {
        this.studentId = studentId;
        this.studentName = studentName;
        this.studentEmail = studentEmail;
        this.totalSessions = totalSessions;
        this.attendedSessions = attendedSessions;
        this.lateSessions = lateSessions;
        this.absentSessions = absentSessions;
    }

    public float getAttendancePercentage() {
        if (totalSessions == 0) return 0;
        return (float) attendedSessions / totalSessions * 100;
    }

    public String getAttendanceText() {
        return String.format("%.1f%% (%d/%d)", getAttendancePercentage(), attendedSessions, totalSessions);
    }

    public int getAttendanceColor() {
        float percentage = getAttendancePercentage();
        if (percentage >= 90) {
            return android.graphics.Color.parseColor("#4CAF50");
        } else if (percentage >= 75) {
            return android.graphics.Color.parseColor("#FF9800");
        } else {
            return android.graphics.Color.parseColor("#F44336");
        }
    }
}
