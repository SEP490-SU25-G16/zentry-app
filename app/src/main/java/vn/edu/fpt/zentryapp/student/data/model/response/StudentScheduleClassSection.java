package vn.edu.fpt.zentryapp.student.data.model.response;

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

@Data
@NoArgsConstructor
@AllArgsConstructor
@Getter
public class StudentScheduleClassSection implements Serializable {
    private static final long serialVersionUID = 1L;

    @SerializedName("SessionId")
    private String sessionId;

    @SerializedName("ClassSectionId")
    private String classSectionId;

    @SerializedName("CourseCode")
    private String courseCode;

    @SerializedName("CourseName")
    private String courseName;

    @SerializedName("SectionCode")
    private String sectionCode;

    @SerializedName("Weekday")
    private String dayOfWeek;

    @SerializedName("StartTime")
    private String startTime; // "13:04:24" format from API

    @SerializedName("EndTime")
    private String endTime;   // "13:34:24" format from API

    @SerializedName("RoomName")
    private String room;

    @SerializedName("Building")
    private String building;

    @SerializedName("LecturerName")
    private String lecturer;

    @SerializedName("SessionStatus")
    private String status;

    // Display methods - similar to lecturer
    public String getDateTimeDisplay() {
        Date today = new Date();
        SimpleDateFormat dayFormat = new SimpleDateFormat("EEEE", Locale.getDefault());
        SimpleDateFormat dateFormat = new SimpleDateFormat("dd/MM", Locale.getDefault());

        return dayFormat.format(today) + " " + dateFormat.format(today) +
                " " + formatTime(startTime) + " - " + formatTime(endTime);
    }

    public String getRoomDisplay() {
        return building + " - " + room;
    }

    public String getCourseDisplay() {
        return courseName + " - " + sectionCode;
    }

    // Helper method to format time from "13:04:24" to "13:04"
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

                Calendar calendar = Calendar.getInstance();
                calendar.set(Calendar.HOUR_OF_DAY, hour);
                calendar.set(Calendar.MINUTE, minute);
                calendar.set(Calendar.SECOND, 0);
                calendar.set(Calendar.MILLISECOND, 0);

                return calendar.getTime();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    // Display methods for UI
    public String getTimeDisplay() {
        return formatTime(startTime) + " - " + formatTime(endTime);
    }

    public String getDayTimeDisplay() {
        return dayOfWeek + " " + getTimeDisplay();
    }

    public String getBuildingRoomDisplay() {
        return building + " - " + room;
    }

    // Legacy compatibility methods (for existing adapter code)
    public String getClassNameWithGrade() {
        return courseName + " - " + sectionCode;
    }

    public String getScheduleTime() {
        return dayOfWeek + " " + formatTime(startTime) + " - " + formatTime(endTime);
    }

    // Alias methods for compatibility
    public String getGrade() {
        return sectionCode;
    }

    // Simplified status methods - server handles the logic
    public boolean isActive() {
        return "Active".equalsIgnoreCase(status);
    }

    public boolean isPending() {
        return "Pending".equalsIgnoreCase(status);
    }

    public boolean isCompleted() {
        return "Completed".equalsIgnoreCase(status);
    }

    public boolean isMissed() {
        return "Missed".equalsIgnoreCase(status);
    }


}
