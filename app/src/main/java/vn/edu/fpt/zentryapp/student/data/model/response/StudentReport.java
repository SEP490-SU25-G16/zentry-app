package vn.edu.fpt.zentryapp.student.data.model.response;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class StudentReport {
    private final String id;
    private final String courseName;
    private final String courseCode;
    private final String className;
    private final String room;
    private final String lecturer;
    private final String attendanceStatus; // "Present", "Absent", "Late"
    private final int completedTasks;
    private final int totalTasks;
    private final String semester;
    private final String academicYear;

    public int getProgressPercentage() {
        if (totalTasks == 0) return 0;
        return (int) ((float) completedTasks / totalTasks * 100);
    }

    public String getClassInfo() {
        return className + " - " + room + " - " + lecturer;
    }

    public String getTaskProgressText() {
        return "Task completed (" + completedTasks + "/" + totalTasks + ")";
    }

    public boolean isPresent() {
        return "Present".equalsIgnoreCase(attendanceStatus);
    }
}
