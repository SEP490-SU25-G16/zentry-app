package vn.edu.fpt.zentryapp.lecturer.data.model.response;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@AllArgsConstructor
@NoArgsConstructor
@Getter
public class LecturerReportClassSection {
    private String classId;        // Thêm field này
    private String courseName;
    private int studentCount;
    private int completedSessions;
    private int totalSessions;
    private double attendancePercentage;
    private String classInfo;
    private String scheduleInfo;
    private String status;
    private String courseCode;
    private String sectionCode;
    private String className;
}