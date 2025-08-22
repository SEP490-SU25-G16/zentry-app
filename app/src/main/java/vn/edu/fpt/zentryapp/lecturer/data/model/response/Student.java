package vn.edu.fpt.zentryapp.lecturer.data.model.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class Student {
    private String studentId;
    private String studentCode;
    private String fullName;
    private String email;
    private String attendanceStatus; // "present" | "absent" | "future"
    private String enrollmentId;
    private String enrollmentStatus;
    private String enrolledAt;

    public boolean isPresent() {
        return "present".equalsIgnoreCase(attendanceStatus);
    }
    public boolean isAbsent() {
        return "absent".equalsIgnoreCase(attendanceStatus);
    }
    public boolean isFuture() {
        return "future".equalsIgnoreCase(attendanceStatus);
    }
    public int getAttendanceStatusColor() {
        if (isFuture()) return 0xFFAAAAAA;
        return isPresent() ? 0xFF059669 : 0xFFE53935;
    }
    public String getDisplayName() {
        return fullName != null ? fullName : "Student " + studentCode;
    }
}
