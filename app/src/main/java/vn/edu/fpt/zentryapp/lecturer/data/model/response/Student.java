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
    private String avatarUrl;
    private boolean isPresent;
    private String attendanceNote;
    private long lastModifiedTime;

    // Helper methods
    public String getAttendanceStatus() {
        return isPresent ? "Attended" : "Absent";
    }

    public int getAttendanceStatusColor() {
        return isPresent ? 0xFF388E3C : 0xFFE53935; // Green : Red
    }

    public String getDisplayName() {
        return fullName != null ? fullName : "Student " + studentCode;
    }
}