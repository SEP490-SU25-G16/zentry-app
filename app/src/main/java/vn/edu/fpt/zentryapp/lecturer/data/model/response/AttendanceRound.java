package vn.edu.fpt.zentryapp.lecturer.data.model.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AttendanceRound {
    private String roundId;
    private String sessionId;
    private int roundNumber;
    private Date timestamp;
    private int totalStudents;
    private int presentStudents;
    private String roundType; // "START", "MIDDLE", "END"
    private String location;


    public String getFormattedTime() {
        if (timestamp == null) return "--:--";

        // ✅ Đảm bảo format theo local timezone
        SimpleDateFormat format = new SimpleDateFormat("HH:mm", Locale.getDefault());
        format.setTimeZone(TimeZone.getDefault()); // Explicitly set local timezone
        return format.format(timestamp);
    }

    public String getFormattedDate() {
        if (timestamp == null) return "--/--/----";

        SimpleDateFormat format = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());
        format.setTimeZone(TimeZone.getDefault()); // Explicitly set local timezone
        return format.format(timestamp);
    }


    public String getAttendanceDisplay() {
        return presentStudents + "/" + totalStudents + " attended";
    }

    public int getAttendancePercentage() {
        if (totalStudents == 0) return 0;
        return (presentStudents * 100) / totalStudents;
    }

    public String getRoundTitle() {
        return "Round " + roundNumber;
    }
}

