package vn.edu.fpt.zentryapp.student.data.model.response;

import lombok.Getter;

@Getter
public class ClassSession {
    private final String id;
    private final String sessionNumber;
    private final String date;
    private final String myAttendanceStatus; // "Present", "Absent", "Late"
    private final String topic;

    public ClassSession(String id, String sessionNumber, String date, String myAttendanceStatus, String topic) {
        this.id = id;
        this.sessionNumber = sessionNumber;
        this.date = date;
        this.myAttendanceStatus = myAttendanceStatus;
        this.topic = topic;
    }

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
