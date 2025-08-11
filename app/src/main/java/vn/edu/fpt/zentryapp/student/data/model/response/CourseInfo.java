package vn.edu.fpt.zentryapp.student.data.model.response;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class CourseInfo {
    private String courseId;
    private String courseName;
    private String courseCode;
    private String sectionCode;
    private String grade;
    private int totalSessions;
    private int attendedSessions;
    private double attendanceRate;

    public String getSessionCountText() {
        return attendedSessions + "/" + totalSessions + " Sessions";
    }

    public String getAttendanceText() {
        return String.format("%.1f%% Attendance", attendanceRate);
    }
}
