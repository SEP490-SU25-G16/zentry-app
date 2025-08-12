package vn.edu.fpt.zentryapp.student.data.model.response;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.io.Serializable;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

@Getter
@AllArgsConstructor
@Setter
@NoArgsConstructor

public class CalendarEvent implements Serializable {
    private String eventId;
    private String sessionId;
    private String classSectionId;
    private String title;
    private String description;
    private Date eventDate;
    private String startTime;
    private String endTime;
    private String eventType;
    private String color;
    private String courseName;
    private String sectionCode;
    private String roomName;
    private String building;

    public String getDisplayDescription() {
        return title + ": " + description;
    }
    public String getTimeRange() {
        if (startTime != null && endTime != null) {
            return startTime + " - " + endTime;
        } else if (startTime != null) {
            return startTime;
        }
        return "All Day";
    }

    public String getRoomInfo() {
        if (roomName != null && building != null) {
            return roomName + " - " + building;
        }
        return roomName != null ? roomName : "";
    }

    public String getFullCourseName() {
        if (courseName != null && sectionCode != null) {
            return courseName + " - " + sectionCode;
        }
        return courseName != null ? courseName : title;
    }

    public String getFormattedDate() {
        if (eventDate != null) {
            SimpleDateFormat dateFormat = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());
            return dateFormat.format(eventDate);
        }
        return "";
    }

    public String getDisplayTitle() {
        return getFullCourseName();
    }

    public String getDisplaySubtitle() {
        String timeInfo = getTimeRange();
        String roomInfo = getRoomInfo();

        if (!timeInfo.isEmpty() && !roomInfo.isEmpty()) {
            return timeInfo + " | " + roomInfo;
        } else if (!timeInfo.isEmpty()) {
            return timeInfo;
        } else if (!roomInfo.isEmpty()) {
            return roomInfo;
        }
        return description != null ? description : "";
    }
}
