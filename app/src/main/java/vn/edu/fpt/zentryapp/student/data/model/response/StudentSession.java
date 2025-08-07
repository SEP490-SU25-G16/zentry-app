package vn.edu.fpt.zentryapp.student.data.model.response;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class StudentSession {
    private final String id;
    private final String title;
    private final String date;
    private final String attendanceStatus; // "Attended", "Absent", "Late"
    private final String courseId;
    private final String courseName;
    private final String description;

    public boolean isAttended() {
        return "Attended".equalsIgnoreCase(attendanceStatus);
    }

    public int getAttendanceColor() {
        switch (attendanceStatus.toLowerCase()) {
            case "attended":
                return android.graphics.Color.parseColor("#167F71");
            case "late":
                return android.graphics.Color.parseColor("#F57C00");
            case "absent":
            default:
                return android.graphics.Color.parseColor("#FF001E");
        }
    }
}
