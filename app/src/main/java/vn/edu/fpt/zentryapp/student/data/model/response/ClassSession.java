package vn.edu.fpt.zentryapp.student.data.model.response;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class ClassSession {
    private final String id;
    private final String sessionNumber;
    private final String date;
    private final String myAttendanceStatus; // "Present", "Absent", "Late"
    private final String topic;

    public int getAttendanceColor() {
        switch (myAttendanceStatus.toLowerCase()) {
            case "present":
                return android.graphics.Color.parseColor("#059669");
            case "late":
                return android.graphics.Color.parseColor("#F59E0B");
            case "absent":
                return android.graphics.Color.parseColor("#DC2626");
            default:
                return android.graphics.Color.parseColor("#6B7280");
        }
    }

    public String getFormattedDate() {
        // Format: "Jan 15"
        return date;
    }
}
