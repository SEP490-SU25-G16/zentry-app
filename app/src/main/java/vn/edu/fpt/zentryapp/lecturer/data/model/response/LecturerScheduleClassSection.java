package vn.edu.fpt.zentryapp.lecturer.data.model.response;

import com.google.gson.annotations.SerializedName;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

@Getter
@Data
@NoArgsConstructor
@AllArgsConstructor
public class LecturerScheduleClassSection implements Serializable {
    private static final long serialVersionUID = 1L;

    @SerializedName("ScheduleId")
    private String scheduleId;

    @SerializedName("SessionId")
    private String sessionId;

    @SerializedName("ClassSectionId")
    private String classSectionId;

    @SerializedName("CourseId")
    private String courseId;

    @SerializedName("CourseCode")
    private String courseCode;

    @SerializedName("CourseName")
    private String courseName;

    @SerializedName("SectionCode")
    private String sectionCode;

    @SerializedName("LecturerId")
    private String lecturerId;

    @SerializedName("LecturerName")
    private String lecturerName;

    @SerializedName("RoomId")
    private String roomId;

    @SerializedName("RoomName")
    private String roomName;

    @SerializedName("Building")
    private String building;

    @SerializedName("Weekday")
    private String weekday;

    @SerializedName("StartTime")
    private String startTime; // "18:51:56" format from API

    @SerializedName("EndTime")
    private String endTime;   // "19:21:56" format from API

    @SerializedName("DateInfo")
    private String dateInfo;  // "2025-08-02" format

    @SerializedName("SessionStatus")
    private String sessionStatus;

    @SerializedName("StudentAttendanceStatus")
    private String studentAttendanceStatus;

    private String formatTime(String timeStr) {
        if (timeStr != null && timeStr.length() >= 5) {
            return timeStr.substring(0, 5);
        }
        return timeStr;
    }

    // Convert API time string to Date object for comparison
    public Date getStartTimeAsDate() {
        return parseTimeToDate(startTime);
    }

    public Date getEndTimeAsDate() {
        return parseTimeToDate(endTime);
    }

    private Date parseTimeToDate(String timeStr) {
        try {
            if (timeStr == null) return null;

            String[] timeParts = timeStr.split(":");
            if (timeParts.length >= 2) {
                int hour = Integer.parseInt(timeParts[0]);
                int minute = Integer.parseInt(timeParts[1]);
                int second = timeParts.length >= 3 ? Integer.parseInt(timeParts[2]) : 0; // ✅ ADDED: Parse seconds

                Calendar calendar = Calendar.getInstance();
                calendar.set(Calendar.HOUR_OF_DAY, hour);
                calendar.set(Calendar.MINUTE, minute);
                calendar.set(Calendar.SECOND, second); // ✅ CHANGED: Set seconds instead of 0
                calendar.set(Calendar.MILLISECOND, 0);

                return calendar.getTime();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }
    // Thêm method này vào class LecturerScheduleClassSection
    public String getTimeDisplay() {
        return formatTime(startTime) + " - " + formatTime(endTime);
    }
    public String getTimeDisplayWithSeconds() {
        return startTime + " - " +endTime;
    }

    public String getWeekdayTimeDisplay() {
        return weekday + " " + getTimeDisplayWithSeconds();
    }

    public String getBuildingRoomDisplay() {
        return building + " - " + roomName;
    }

    // Alias for compatibility with adapter
    public String getStatus() {
        return sessionStatus;
    }

    public void setStatus(String status) {
        this.sessionStatus = status;
    }
}
