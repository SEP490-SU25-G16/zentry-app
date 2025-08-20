package vn.edu.fpt.zentryapp.lecturer.data.model.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Getter
public class Attendance {
    private String studentCode;
    private String studentName;
    private String status; // "Attended", "Absent", "Future"
    private int totalRounds;
    private int attendedRounds;
    private int roundNumber;

    public String getStudentCode() {
        return studentCode != null ? studentCode : "N/A";
    }

    public String getAttendanceStatus() {
        // Trả về luôn chữ ban đầu hoặc viết hoa chữ đầu
        return status != null ? capitalizeFirst(status) : "Unknown";
    }

    public int getAttendanceStatusColor() {
        // Green nếu attended, Red nếu absent, Gray nếu future
        if ("Attended".equalsIgnoreCase(status)) return 0xFF4CAF50;
        if ("Present".equalsIgnoreCase(status)) return 0xFF4CAF50;
        if ("Future".equalsIgnoreCase(status)) return 0xFFAAAAAA;
        return 0xFFE53935; // Absent
    }

    private String capitalizeFirst(String input) {
        if (input == null || input.isEmpty()) return input;
        return input.substring(0, 1).toUpperCase() + input.substring(1).toLowerCase();
    }

    public boolean isFuture() { return "Future".equalsIgnoreCase(status); }
    public boolean isAttended() { return "Attended".equalsIgnoreCase(status); }
    public boolean isAbsent() { return "Absent".equalsIgnoreCase(status); }
}
