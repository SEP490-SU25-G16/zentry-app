// MyAttendance.java
package vn.edu.fpt.zentryapp.student.data.model.response;

import java.util.Date;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class MyAttendance {
    private String studentId;
    private String studentName;
    private String email;
    private int totalSessions;
    private int attendedSessions;
    private int absentSessions;
    private Date lastAttendanceTime;
    private boolean isPresent;
    private String status;
    private String detailedStatus;

    public double getAttendancePercentage() {
        if (totalSessions == 0) return 0.0;
        return (double) attendedSessions / totalSessions * 100.0;
    }

    public String getAttendanceGrade() {
        double percentage = getAttendancePercentage();
        if (percentage >= 80.0) {
            return "EXCELLENT";
        } else if (percentage >= 60.0) {
            return "GOOD";
        } else if (percentage >= 40.0) {
            return "FAIR";
        } else {
            return "POOR";
        }
    }

    public int getAttendanceColor() {
        double percentage = getAttendancePercentage();
        if (percentage >= 80.0) {
            return 0xFF4CAF50; // Green
        } else if (percentage >= 60.0) {
            return 0xFF2196F3; // Blue
        } else if (percentage >= 40.0) {
            return 0xFFFF9800; // Orange
        } else {
            return 0xFFF44336; // Red
        }
    }
}
