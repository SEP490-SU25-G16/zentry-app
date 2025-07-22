package vn.edu.fpt.zentryapp.lecturer.data.model.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CourseInfo {
    private String courseCode;
    private String courseName;
    private String className;
    private String room;
    private String grade;
    private int totalStudents;
    private int totalSessions;
    private int completedSessions;
    private String semester;
    private String academicYear;

    // Helper methods
    public String getSessionProgress() {
        return completedSessions + "/" + totalSessions + " Sessions";
    }

    public String getStudentCount() {
        return totalStudents + " Students";
    }

    public String getGradeDisplay() {
        return "Grade " + grade;
    }
}