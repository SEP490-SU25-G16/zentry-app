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
    private String studentId;
    private String studentName;
    private boolean finalStatus; // true = attended, false = absent
    private int totalRounds;
    private int attendedRounds;
    private int roundNumber;

    public String getStudentCode() {
        if (studentId != null && studentId.length() >= 6) {
            return studentId.substring(0, 6).toUpperCase();
        }
        return "N/A";
    }

    public String getAttendanceStatus() {
        return finalStatus ? "Attended" : "Absent";
    }

    public int getAttendanceStatusColor() {
        return finalStatus ? 0xFF4CAF50 : 0xFFE53935; // Green : Red
    }
}