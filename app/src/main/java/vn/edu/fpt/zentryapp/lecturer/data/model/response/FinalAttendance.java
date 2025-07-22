package vn.edu.fpt.zentryapp.lecturer.data.model.response;


import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class FinalAttendance {
    private String studentId;
    private String studentCode;
    private String studentName;
    private String email;
    private boolean finalStatus; // true = attended, false = absent
    private int totalRounds;
    private int attendedRounds;
    private String avatarUrl;

    // Helper methods
    public String getAttendanceStatus() {
        return finalStatus ? "Attended" : "Absent";
    }

    public int getAttendanceStatusColor() {
        return finalStatus ? 0xFF4CAF50 : 0xFFE53935; // Green : Red
    }

    public String getAttendanceRatio() {
        return attendedRounds + "/" + totalRounds + " rounds";
    }

    public int getAttendancePercentage() {
        if (totalRounds == 0) return 0;
        return (attendedRounds * 100) / totalRounds;
    }
}