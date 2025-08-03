package vn.edu.fpt.zentryapp.student.data.model.response;

import com.google.gson.annotations.SerializedName;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ScheduleDetailDto {
    // ✅ Schedule info
    @SerializedName("ScheduleId")
    private String scheduleId;

    @SerializedName("StartDate")
    private String startDate; // DateOnly từ server

    @SerializedName("EndDate")
    private String endDate; // DateOnly từ server

    @SerializedName("StartTime")
    private String startTime; // TimeOnly từ server

    @SerializedName("EndTime")
    private String endTime; // TimeOnly từ server

    @SerializedName("WeekDay")
    private String weekDay;

    // ✅ ClassSection info
    @SerializedName("ClassSectionId")
    private String classSectionId;

    @SerializedName("SectionCode")
    private String sectionCode;

    // ✅ Course info
    @SerializedName("CourseName")
    private String courseName;

    // ✅ Room info
    @SerializedName("RoomId")
    private String roomId;

    @SerializedName("RoomName")
    private String roomName;

    @SerializedName("Building")
    private String building;

    // ✅ Additional fields
    @SerializedName("EnrolledStudentsCount")
    private int enrolledStudentsCount;

    @SerializedName("DurationInMinutes")
    private int durationInMinutes;

    @SerializedName("SessionStatus")
    private String sessionStatus;

    // ✅ Helper methods
    public String getFormattedDuration() {
        if (durationInMinutes < 60) {
            return durationInMinutes + " mins";
        } else {
            int hours = durationInMinutes / 60;
            int mins = durationInMinutes % 60;
            if (mins == 0) {
                return hours + (hours == 1 ? " hour" : " hours");
            } else {
                return hours + (hours == 1 ? " hour " : " hours ") + mins + " mins";
            }
        }
    }

    public String getFormattedStudentCount() {
        return enrolledStudentsCount + (enrolledStudentsCount == 1 ? " Student" : " Students");
    }

    // ✅ NEW: Format time display
    public String getFormattedTimeRange() {
        return formatTime(startTime) + " - " + formatTime(endTime);
    }

    // ✅ NEW: Format room display
    public String getFormattedRoomDisplay() {
        return building + " - " + roomName;
    }

    // ✅ NEW: Format course display
    public String getFormattedCourseDisplay() {
        return courseName + " - " + sectionCode;
    }

    // ✅ Helper method to format time from TimeOnly
    private String formatTime(String timeString) {
        if (timeString == null || timeString.isEmpty()) return "";

        try {
            // TimeOnly format từ server có thể là "HH:mm:ss" hoặc "HH:mm"
            if (timeString.length() >= 5) {
                return timeString.substring(0, 5); // Extract HH:mm
            }
            return timeString;
        } catch (Exception e) {
            return timeString;
        }
    }

    // ✅ NEW: Check if session is active
    public boolean isSessionActive() {
        return "Active".equalsIgnoreCase(sessionStatus);
    }

    // ✅ NEW: Get day and time display
    public String getDayTimeDisplay() {
        return weekDay + " " + getFormattedTimeRange();
    }
}
