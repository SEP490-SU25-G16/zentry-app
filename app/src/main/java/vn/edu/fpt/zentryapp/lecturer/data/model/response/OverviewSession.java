package vn.edu.fpt.zentryapp.lecturer.data.model.response;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class OverviewSession {
    private String sessionId;     // Giữ lại để identify
    private int sessionNumber;    // Dùng cho getSessionTitle()
    private Date date;            // Dùng cho getFormattedDate()
    private int totalStudents;    // Dùng cho getAttendanceSummary()
    private int presentStudents;  // Dùng cho getAttendanceSummary()

    // Helper methods - chỉ giữ những cái đang dùng
    public String getAttendanceSummary() {
        return presentStudents + "/" + totalStudents + " - Attendance";
    }

    public String getFormattedDate() {
        if (date == null) return "";
        SimpleDateFormat format = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());
        return format.format(date);
    }

    public String getSessionTitle() {
        return "Session - " + sessionNumber;
    }

}
