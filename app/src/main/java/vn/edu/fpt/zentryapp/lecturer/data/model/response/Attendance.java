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
    private String studentCode;  // Này sẽ lưu StudentCode từ API
    private String studentName;
    private boolean finalStatus; // true = attended, false = absent
    private int totalRounds;
    private int attendedRounds;
    private int roundNumber;

    public String getStudentCode() {
        return studentCode != null ? studentCode : "N/A";
    }

    public String getAttendanceStatus() {
        return finalStatus ? "Attended" : "Absent";
    }

    public int getAttendanceStatusColor() {
        return finalStatus ? 0xFF4CAF50 : 0xFFE53935; // Green : Red
    }
}
