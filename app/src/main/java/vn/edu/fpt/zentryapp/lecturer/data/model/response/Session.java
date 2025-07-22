package vn.edu.fpt.zentryapp.lecturer.data.model.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class Session {
    private String id;
    private String courseName;
    private String courseCode;
    private String className;
    private String room;
    private String startTime;
    private String endTime;
    private int totalStudents;
    private int presentStudents;
    private int totalTasks;
    private int completedTasks;
    private String date;
    private String status; // "COMPLETED", "ONGOING", "SCHEDULED"

    // Helper methods
    public int getAttendancePercentage() {
        if (totalStudents == 0) return 0;
        return (presentStudents * 100) / totalStudents;
    }

    public int getTaskCompletionPercentage() {
        if (totalTasks == 0) return 0;
        return (completedTasks * 100) / totalTasks;
    }

    public String getAttendanceSummary() {
        return "Attendance - " + getAttendancePercentage() + "%";
    }

    public String getTaskSummary() {
        return completedTasks + "/" + totalTasks + " tasks completed";
    }

    public String getClassInfo() {
        return className + " - " + room;
    }

    public String getTimeRange() {
        return startTime + " - " + endTime;
    }
}
