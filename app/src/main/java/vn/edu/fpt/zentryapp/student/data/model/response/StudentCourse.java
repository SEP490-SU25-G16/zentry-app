package vn.edu.fpt.zentryapp.student.data.model.response;

import lombok.Getter;

@Getter
public class StudentCourse {
    private final String id;
    private final String name;
    private final String courseCode;
    private final String className;
    private final String room;
    private final String lecturer;
    private final int attendanceRate; // Percentage
    private final int completedTasks;
    private final int totalTasks;
    private final String semester;
    private final String academicYear;
    private final String status; // "Active", "Completed", "Pending"

    public StudentCourse(String id, String name, String courseCode, String className,
                         String room, String lecturer, int attendanceRate,
                         int completedTasks, int totalTasks, String semester,
                         String academicYear, String status) {
        this.id = id;
        this.name = name;
        this.courseCode = courseCode;
        this.className = className;
        this.room = room;
        this.lecturer = lecturer;
        this.attendanceRate = attendanceRate;
        this.completedTasks = completedTasks;
        this.totalTasks = totalTasks;
        this.semester = semester;
        this.academicYear = academicYear;
        this.status = status;
    }

    public int getProgressPercentage() {
        if (totalTasks == 0) return 0;
        return (int) ((float) completedTasks / totalTasks * 100);
    }

    public String getClassInfo() {
        return className + " - " + room + " - " + lecturer;
    }

    public String getAttendanceText() {
        return "Attendance - " + attendanceRate + "%";
    }

    public String getTaskProgressText() {
        return "Task completed (" + completedTasks + "/" + totalTasks + ")";
    }
}