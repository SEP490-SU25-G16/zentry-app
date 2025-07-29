package vn.edu.fpt.zentryapp.lecturer.data.model.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class LecturerScheduleSession implements Serializable {
    private static final long serialVersionUID = 1L;
    private String sessionId;
    private String courseCode;
    private String courseName;
    private String className;
    private String room;
    private Date startTime;
    private Date endTime;
    private Date sessionDate;
    private String status;
    private boolean canStartInstant;
    private boolean canViewDetail;

    public String getDateTimeDisplay() {
        SimpleDateFormat dayFormat = new SimpleDateFormat("EEEE", Locale.getDefault());
        SimpleDateFormat dateFormat = new SimpleDateFormat("dd/MM", Locale.getDefault());
        SimpleDateFormat timeFormat = new SimpleDateFormat("HH:mm", Locale.getDefault());

        return dayFormat.format(sessionDate) + " " + dateFormat.format(sessionDate) +
                " " + timeFormat.format(startTime) + " - " + timeFormat.format(endTime);
    }

    public String getClassRoomDisplay() {
        return className + " - " + room;
    }
}