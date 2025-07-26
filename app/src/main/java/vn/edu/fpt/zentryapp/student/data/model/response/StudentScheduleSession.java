package vn.edu.fpt.zentryapp.student.data.model.response;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.io.Serializable;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

@Getter
@AllArgsConstructor
public class StudentScheduleSession implements Serializable {
    private static final long serialVersionUID = 1L;
    private final String sessionId;
    private final String className;
    private final String grade;
    private final String dayOfWeek;
    private final String startTime;
    private final String endTime;
    private final String room;
    private final String lecturer;
    private final String courseCode;

    public String getClassNameWithGrade() {
        return className + " Grade - " + grade;
    }

    public String getScheduleTime() {
        return dayOfWeek + " " + startTime + " - " + endTime;
    }

    /**
     * Check if the schedule is clickable (current time is within or after start time)
     */
    public boolean isClickable() {
        Calendar currentTime = Calendar.getInstance();
        Calendar scheduleTime = getScheduleDateTime();

        if (scheduleTime == null) return false;

        // Allow clicking 15 minutes before start time
        scheduleTime.add(Calendar.MINUTE, -15);

        return currentTime.after(scheduleTime) || currentTime.equals(scheduleTime);
    }

    /**
     * Check if the schedule is currently active (between start and end time)
     */
    public boolean isActive() {
        Calendar currentTime = Calendar.getInstance();
        Calendar startDateTime = getScheduleDateTime();
        Calendar endDateTime = getScheduleEndDateTime();

        if (startDateTime == null || endDateTime == null) return false;

        return currentTime.after(startDateTime) && currentTime.before(endDateTime);
    }

    /**
     * Get the schedule status for UI display
     */
    public ScheduleStatus getStatus() {
        if (isActive()) {
            return ScheduleStatus.ACTIVE;
        } else if (isClickable()) {
            return ScheduleStatus.CLICKABLE;
        } else {
            return ScheduleStatus.UPCOMING;
        }
    }

    private Calendar getScheduleDateTime() {
        try {
            Calendar calendar = Calendar.getInstance();

            // Get current week's day for this schedule
            int targetDayOfWeek = getDayOfWeekInt(dayOfWeek);
            int currentDayOfWeek = calendar.get(Calendar.DAY_OF_WEEK);

            // Calculate days difference
            int daysDifference = targetDayOfWeek - currentDayOfWeek;
            calendar.add(Calendar.DATE, daysDifference);

            // Parse and set time
            SimpleDateFormat timeFormat = new SimpleDateFormat("h:mm a", Locale.US);
            Date startTimeDate = timeFormat.parse(startTime);

            Calendar timeCalendar = Calendar.getInstance();
            timeCalendar.setTime(startTimeDate);

            calendar.set(Calendar.HOUR_OF_DAY, timeCalendar.get(Calendar.HOUR_OF_DAY));
            calendar.set(Calendar.MINUTE, timeCalendar.get(Calendar.MINUTE));
            calendar.set(Calendar.SECOND, 0);
            calendar.set(Calendar.MILLISECOND, 0);

            return calendar;
        } catch (ParseException e) {
            e.printStackTrace();
            return null;
        }
    }

    private Calendar getScheduleEndDateTime() {
        try {
            Calendar calendar = getScheduleDateTime();
            if (calendar == null) return null;

            SimpleDateFormat timeFormat = new SimpleDateFormat("h:mm a", Locale.US);
            Date endTimeDate = timeFormat.parse(endTime);

            Calendar timeCalendar = Calendar.getInstance();
            timeCalendar.setTime(endTimeDate);

            calendar.set(Calendar.HOUR_OF_DAY, timeCalendar.get(Calendar.HOUR_OF_DAY));
            calendar.set(Calendar.MINUTE, timeCalendar.get(Calendar.MINUTE));

            return calendar;
        } catch (ParseException e) {
            e.printStackTrace();
            return null;
        }
    }

    private int getDayOfWeekInt(String dayName) {
        switch (dayName.toLowerCase()) {
            case "sunday":
                return Calendar.SUNDAY;
            case "monday":
                return Calendar.MONDAY;
            case "tuesday":
                return Calendar.TUESDAY;
            case "wednesday":
                return Calendar.WEDNESDAY;
            case "thursday":
                return Calendar.THURSDAY;
            case "friday":
                return Calendar.FRIDAY;
            case "saturday":
                return Calendar.SATURDAY;
            default:
                return Calendar.MONDAY;
        }
    }

    public enum ScheduleStatus {
        UPCOMING,    // Not yet time
        CLICKABLE,   // Can click (15 mins before or after start)
        ACTIVE       // Currently in session
    }
}
