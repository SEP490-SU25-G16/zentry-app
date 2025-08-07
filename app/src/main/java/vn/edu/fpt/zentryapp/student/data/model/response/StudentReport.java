package vn.edu.fpt.zentryapp.student.data.model.response;

import java.io.Serializable;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class StudentReport implements Serializable {
    private static final long serialVersionUID = 1L;

    private final String id;
    private final String courseName;
    private final String courseCode;
    private final String className;
    private final String lecturer;
    private final int totalSessions;
    private final int attendedSessions;

    // Helper methods
    public String getCourseTitle() {
        return courseName + " - " + className;
    }

    public String getLecturerInfo() {
        return "Lecturer: " + lecturer;
    }

    public String getSessionsText() {
        return attendedSessions + "/" + totalSessions + " Sessions";
    }

    public int getAttendancePercentage() {
        if (totalSessions == 0) return 0;
        return (int) ((float) attendedSessions / totalSessions * 100);
    }

    public String getAttendancePercentageText() {
        return getAttendancePercentage() + "%";
    }
}
