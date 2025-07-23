package vn.edu.fpt.zentryapp.student.data.model.response;

import lombok.Getter;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

@Getter
public class CalendarEvent {
    private final String id;
    private final String title;
    private final String description;
    private final Date date;
    private final String time;
    private final String type; // "class", "meeting", "exam", etc.
    private final String color; // Color code for timeline indicator

    public CalendarEvent(String id, String title, String description, Date date,
                         String time, String type, String color) {
        this.id = id;
        this.title = title;
        this.description = description;
        this.date = date;
        this.time = time;
        this.type = type;
        this.color = color;
    }

    public String getFormattedDate() {
        SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
        return formatter.format(date);
    }

    public String getDisplayDescription() {
        return title + ": " + description;
    }
}
