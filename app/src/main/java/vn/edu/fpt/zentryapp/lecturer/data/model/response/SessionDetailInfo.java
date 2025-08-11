package vn.edu.fpt.zentryapp.lecturer.data.model.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.util.Date;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class SessionDetailInfo {
    private String sessionId;
    private String courseCode;
    private String courseName;
    private String className;
    private String sessionTitle;
    private String roomInfo;
    private String room;
    private String grade;
    private int sessionNumber;
    private Date sessionDate;
    private String startTime;
    private String endTime;
    private int totalStudents;
    private int presentStudents;
    private String status;
    private long createdTime;

    // Helper methods
    public String getAttendanceSummary() {
        return presentStudents + "/" + totalStudents + " - Attendance";
    }

    public String getSessionTitle() {
        return "Session - " + sessionNumber;
    }

    public boolean canEditAttendance() {
        long currentTime = System.currentTimeMillis();
        long sessionEndTime = sessionDate != null ? sessionDate.getTime() : createdTime;
        long timeDifference = currentTime - sessionEndTime;

        // Allow edit within 24 hours (24 * 60 * 60 * 1000 ms)
        return timeDifference <= 24 * 60 * 60 * 1000L;
    }
}