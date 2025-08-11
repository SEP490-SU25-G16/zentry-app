package vn.edu.fpt.zentryapp.student.data.model.response;

import java.io.Serializable;

public class StudentReport implements Serializable {
    private String classId;
    private String courseName;
    private String courseCode;
    private String sectionCode;
    private String className;
    private String lecturerName;
    private String lecturerId;
    private double attendanceRate;
    private int totalSessions;
    private int attendedSessions;

    // Constructors
    public StudentReport() {}

    public StudentReport(String classId, String courseName, String courseCode, String sectionCode,
                         String className, String lecturerName, String lecturerId, double attendanceRate) {
        this.classId = classId;
        this.courseName = courseName;
        this.courseCode = courseCode;
        this.sectionCode = sectionCode;
        this.className = className;
        this.lecturerName = lecturerName;
        this.lecturerId = lecturerId;
        this.attendanceRate = attendanceRate;

        // Calculate estimated sessions based on attendance rate
        // You might need to call another API to get exact session counts
        this.totalSessions = estimateTotalSessions();
        this.attendedSessions = (int) Math.round(this.totalSessions * (attendanceRate / 100.0));
    }

    // Getters and Setters
    public String getClassId() { return classId; }
    public void setClassId(String classId) { this.classId = classId; }

    public String getCourseName() { return courseName; }
    public void setCourseName(String courseName) { this.courseName = courseName; }

    public String getCourseCode() { return courseCode; }
    public void setCourseCode(String courseCode) { this.courseCode = courseCode; }

    public String getSectionCode() { return sectionCode; }
    public void setSectionCode(String sectionCode) { this.sectionCode = sectionCode; }

    public String getClassName() { return className; }
    public void setClassName(String className) { this.className = className; }

    public String getLecturerName() { return lecturerName; }
    public void setLecturerName(String lecturerName) { this.lecturerName = lecturerName; }

    public String getLecturerId() { return lecturerId; }
    public void setLecturerId(String lecturerId) { this.lecturerId = lecturerId; }

    public double getAttendanceRate() { return attendanceRate; }
    public void setAttendanceRate(double attendanceRate) { this.attendanceRate = attendanceRate; }

    public int getTotalSessions() { return totalSessions; }
    public void setTotalSessions(int totalSessions) { this.totalSessions = totalSessions; }

    public int getAttendedSessions() { return attendedSessions; }
    public void setAttendedSessions(int attendedSessions) { this.attendedSessions = attendedSessions; }

    // Helper methods
    public String getClassInfo() {
        return courseCode + " - " + sectionCode;
    }

    public String getAttendanceDisplay() {
        return String.format("%.1f%%", attendanceRate);
    }

    public String getSessionProgress() {
        return attendedSessions + "/" + totalSessions + " Sessions";
    }

    public String getLecturerDisplayName() {
        return (lecturerName != null && !lecturerName.equals("N/A")) ? lecturerName : "No Lecturer";
    }

    private int estimateTotalSessions() {
        // Default estimation - could be improved with more data
        return 20;
    }

    // For backward compatibility with existing constructor calls
    public StudentReport(String classId, String courseName, String courseCode, String sectionCode,
                         String lecturerName, int totalSessions, int attendedSessions) {
        this.classId = classId;
        this.courseName = courseName;
        this.courseCode = courseCode;
        this.sectionCode = sectionCode;
        this.lecturerName = lecturerName;
        this.totalSessions = totalSessions;
        this.attendedSessions = attendedSessions;
        this.attendanceRate = totalSessions > 0 ? (double) attendedSessions / totalSessions * 100 : 0;
        this.className = courseName + " - " + sectionCode;
    }
}
