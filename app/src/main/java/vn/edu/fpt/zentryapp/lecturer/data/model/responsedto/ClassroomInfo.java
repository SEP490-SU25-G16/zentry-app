package vn.edu.fpt.zentryapp.lecturer.data.model.responsedto;

import java.io.Serializable;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@AllArgsConstructor
@NoArgsConstructor
public class ClassroomInfo implements Serializable {
    private String classId;
    private String courseName;
    private String courseCode;
    private String sectionCode;
    private String className;
    private int enrolledStudents;
    private int completedSessions;
    private int totalSessions;
    private double attendanceRate;

    public String getClassInfo() {
        return courseCode + " - " + sectionCode;
    }

    public String getScheduleInfo() {
        return "Sessions: " + completedSessions + "/" + totalSessions;
    }
 }
