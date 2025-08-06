package vn.edu.fpt.zentryapp.lecturer.data.model.response;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@AllArgsConstructor
@NoArgsConstructor
@Getter
public class LecturerReportClassSection {
    private String courseName;
    private int studentCount;
    private int currentSessions;
    private int totalSessions;
    private double attendancePercentage;
    private String classInfo;
    private String timeRange;
    private String status;

}
