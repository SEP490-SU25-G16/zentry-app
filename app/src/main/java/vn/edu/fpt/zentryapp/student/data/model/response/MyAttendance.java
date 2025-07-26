package vn.edu.fpt.zentryapp.student.data.model.response;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor

public class MyAttendance {
    private final int totalSessions;
    private final int attendedSessions;
    private final int absentSessions;
    private final int lateSessions;

    public float getAttendancePercentage() {
        if (totalSessions == 0) return 0;
        return (float) attendedSessions / totalSessions * 100;
    }

    public String getAttendanceGrade() {
        float percentage = getAttendancePercentage();
        if (percentage >= 95) {
            return "Excellent";
        } else if (percentage >= 85) {
            return "Good";
        } else if (percentage >= 70) {
            return "Average";
        } else {
            return "Poor";
        }
    }

    public int getAttendanceColor() {
        float percentage = getAttendancePercentage();
        if (percentage >= 85) {
            return android.graphics.Color.parseColor("#059669"); // Green
        } else if (percentage >= 70) {
            return android.graphics.Color.parseColor("#F59E0B"); // Orange
        } else {
            return android.graphics.Color.parseColor("#DC2626"); // Red
        }
    }
}
