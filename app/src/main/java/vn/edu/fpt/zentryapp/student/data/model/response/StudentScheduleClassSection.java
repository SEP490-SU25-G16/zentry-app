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

    // Các trường đã có
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

    // ===== CÁC TRƯỜNG MỚI BỔ SUNG =====
    @SerializedName("ScheduleId")
    private String scheduleId;

    @SerializedName("CourseId")
    private String courseId;

    @SerializedName("LecturerId")
    private String lecturerId;

    @SerializedName("RoomId")
    private String roomId;

    @SerializedName("DateInfo")
    private String dateInfo;

    @SerializedName("StudentAttendanceStatus")
    private String studentAttendanceStatus;
    // Đã có sẵn trong model
    public String getCourseDisplay() {
        return courseName + " - " + sectionCode;
    }

    public String getDayTimeDisplay() {
        return dayOfWeek + " " + getTimeDisplay();
    }

    public String getTimeDisplay() {
        return formatTime(startTime) + " - " + formatTime(endTime);
    }
    private String formatTime(String timeStr) {
        if (timeStr != null && timeStr.length() >= 5) {
            return timeStr.substring(0, 5); // Chỉ lấy HH:mm
        }
        return timeStr;
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

    public String getBuildingRoomDisplay() {
        return building + " - " + room;
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

    // Kiểm tra trạng thái điểm danh của sinh viên
    public boolean hasAttended() {
        return "Present".equalsIgnoreCase(studentAttendanceStatus) ||
                "Attended".equalsIgnoreCase(studentAttendanceStatus);
    }

    public boolean isAbsent() {
        return "Absent".equalsIgnoreCase(studentAttendanceStatus);
    }

}
