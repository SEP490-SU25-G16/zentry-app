package vn.edu.fpt.zentryapp.student.data.model.response;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class CourseInfo {
    private final String courseId;
    private final String courseName;
    private final String grade;
    private final int attendedSessions;
    private final int totalSessions;

    public String getSessionCountText() {
        return attendedSessions + "/" + totalSessions + " Sessions";
    }
}
