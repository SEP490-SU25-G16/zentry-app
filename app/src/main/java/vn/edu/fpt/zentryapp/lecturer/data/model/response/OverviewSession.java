package vn.edu.fpt.zentryapp.lecturer.data.model.response;

import java.io.Serializable;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class OverviewSession implements Serializable {
    private String sessionId;
    private int sessionNumber;
    private String sessionTitle;
    private Date date;
    private int totalStudents;
    private int presentStudents;
    private String startTime;
    private String endTime;
    private String roomInfo;
    private String status;

    public String getAttendanceSummary() {
        return presentStudents + "/" + totalStudents + " - Attendance";
    }

    public String getFormattedDate() {
        if (date == null) return "";
        SimpleDateFormat format = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());
        return format.format(date);
    }

    public String getSessionTitle() {
        return "Session - " + sessionNumber;
    }

}
