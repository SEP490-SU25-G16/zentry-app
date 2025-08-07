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
    private boolean isPresent;

    // Helper methods
    public String getAttendanceStatus() {
        return isPresent ? "Attended" : "Absented";
    }

    public int getAttendanceStatusColor() {
        return isPresent ? 0xFF059669 : 0xFFE53935; // Green : Red
    }

    public String getDisplayName() {
        return fullName != null ? fullName : "Student " + studentCode;
    }
}
