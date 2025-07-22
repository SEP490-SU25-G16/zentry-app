package vn.edu.fpt.zentryapp.lecturer.ui.schedule;


import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import android.os.Handler;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import lombok.Getter;
import vn.edu.fpt.zentryapp.auth.client.AuthManager;
import vn.edu.fpt.zentryapp.lecturer.data.model.response.CalendarSession;

public class LecturerScheduleCalendarViewModel extends ViewModel {

    // LiveData cho UI state
    private final MutableLiveData<Boolean> _isLoading = new MutableLiveData<>(false);
    private final MutableLiveData<List<CalendarSession>> _sessions = new MutableLiveData<>();
    private final MutableLiveData<String> _errorMessage = new MutableLiveData<>();
    private final MutableLiveData<String> _selectedDate = new MutableLiveData<>();
    private final MutableLiveData<Boolean> _hasSessionsForDate = new MutableLiveData<>();

    // Public getters cho Fragment observe
    public LiveData<Boolean> isLoading() {
        return _isLoading;
    }

    public LiveData<List<CalendarSession>> sessions() {
        return _sessions;
    }

    public LiveData<String> errorMessage() {
        return _errorMessage;
    }

    public LiveData<String> selectedDate() {
        return _selectedDate;
    }

    public LiveData<Boolean> hasSessionsForDate() {
        return _hasSessionsForDate;
    }

    private AuthManager authManager;
    private Calendar currentSelectedDate;

    public void init(AuthManager authManager) {
        this.authManager = authManager;

        // Initialize with today's date
        currentSelectedDate = Calendar.getInstance();
        loadSessionsForDate(currentSelectedDate);
    }

    /**
     * Load sessions for specific date
     */
    public void loadSessionsForDate(Calendar selectedDate) {
        this.currentSelectedDate = (Calendar) selectedDate.clone();

        _isLoading.setValue(true);

        // Update selected date display
        SimpleDateFormat dateFormat = new SimpleDateFormat("EEEE, MMMM dd, yyyy", Locale.getDefault());
        _selectedDate.setValue(dateFormat.format(selectedDate.getTime()));

        // Simulate network delay
        new Handler().postDelayed(() -> {
            List<CalendarSession> sessions = generateSessionsForDate(selectedDate);
            _sessions.setValue(sessions);
            _hasSessionsForDate.setValue(!sessions.isEmpty());
            _isLoading.setValue(false);
        }, 800);
    }

    /**
     * Load sessions for date from DatePicker
     */
    public void loadSessionsForDate(int year, int month, int dayOfMonth) {
        Calendar selectedDate = Calendar.getInstance();
        selectedDate.set(year, month, dayOfMonth);
        loadSessionsForDate(selectedDate);
    }

    /**
     * Refresh current date sessions
     */
    public void refreshSessions() {
        if (currentSelectedDate != null) {
            loadSessionsForDate(currentSelectedDate);
        }
    }

    /**
     * Generate mock sessions for specific date
     */
    private List<CalendarSession> generateSessionsForDate(Calendar date) {
        List<CalendarSession> sessions = new ArrayList<>();

        // Get day of week (1 = Sunday, 2 = Monday, etc.)
        int dayOfWeek = date.get(Calendar.DAY_OF_WEEK);

        // Generate different schedules based on day of week
        switch (dayOfWeek) {
            case Calendar.MONDAY:
                sessions.addAll(generateMondaySchedule(date));
                break;
            case Calendar.TUESDAY:
                sessions.addAll(generateTuesdaySchedule(date));
                break;
            case Calendar.WEDNESDAY:
                sessions.addAll(generateWednesdaySchedule(date));
                break;
            case Calendar.THURSDAY:
                sessions.addAll(generateThursdaySchedule(date));
                break;
            case Calendar.FRIDAY:
                sessions.addAll(generateFridaySchedule(date));
                break;
            case Calendar.SATURDAY:
                sessions.addAll(generateSaturdaySchedule(date));
                break;
            case Calendar.SUNDAY:
                // Usually no classes on Sunday, but might have meetings
                sessions.addAll(generateSundaySchedule(date));
                break;
        }

        // Update session status based on current time if selected date is today
        if (isToday(date)) {
            updateSessionStatus(sessions);
        } else if (date.before(Calendar.getInstance())) {
            // Past date - all sessions completed
            for (CalendarSession session : sessions) {
                session.setStatus("COMPLETED");
            }
        } else {
            // Future date - all sessions upcoming
            for (CalendarSession session : sessions) {
                session.setStatus("UPCOMING");
            }
        }

        return sessions;
    }

    /**
     * Generate Monday schedule
     */
    private List<CalendarSession> generateMondaySchedule(Calendar date) {
        List<CalendarSession> sessions = new ArrayList<>();

        sessions.add(createSession("MON_1", "CSE101", "Lập trình căn bản", "SE1801", "DE-201",
                date, 8, 0, 9, 30, "LECTURE"));
        sessions.add(createSession("MON_2", "CSE201", "Cấu trúc dữ liệu", "SE1802", "DE-203",
                date, 10, 0, 11, 30, "PRACTICE"));
        sessions.add(createSession("MON_3", "MEETING", "Faculty Meeting", "All Staff", "Conference Room",
                date, 14, 0, 15, 30, "MEETING"));

        return sessions;
    }

    /**
     * Generate Tuesday schedule
     */
    private List<CalendarSession> generateTuesdaySchedule(Calendar date) {
        List<CalendarSession> sessions = new ArrayList<>();

        sessions.add(createSession("TUE_1", "CSE301", "Lập trình Web", "SE1803", "DE-105",
                date, 9, 0, 10, 30, "LECTURE"));
        sessions.add(createSession("TUE_2", "CSE401", "Mobile Development", "SE1804", "DE-302",
                date, 13, 30, 15, 0, "PRACTICE"));

        return sessions;
    }

    /**
     * Generate Wednesday schedule
     */
    private List<CalendarSession> generateWednesdaySchedule(Calendar date) {
        List<CalendarSession> sessions = new ArrayList<>();

        sessions.add(createSession("WED_1", "CSE101", "Lập trình căn bản", "SE1801", "DE-201",
                date, 8, 0, 9, 30, "PRACTICE"));
        sessions.add(createSession("WED_2", "CSE501", "Machine Learning", "AI1801", "DE-401",
                date, 10, 0, 11, 30, "LECTURE"));
        sessions.add(createSession("WED_3", "CSE201", "Cấu trúc dữ liệu", "SE1802", "DE-203",
                date, 15, 0, 16, 30, "EXAM"));

        return sessions;
    }

    /**
     * Generate Thursday schedule
     */
    private List<CalendarSession> generateThursdaySchedule(Calendar date) {
        List<CalendarSession> sessions = new ArrayList<>();

        sessions.add(createSession("THU_1", "CSE301", "Lập trình Web", "SE1803", "DE-105",
                date, 7, 30, 9, 0, "LECTURE"));
        sessions.add(createSession("THU_2", "CSE401", "Mobile Development", "SE1804", "DE-302",
                date, 14, 0, 15, 30, "LECTURE"));

        return sessions;
    }

    /**
     * Generate Friday schedule
     */
    private List<CalendarSession> generateFridaySchedule(Calendar date) {
        List<CalendarSession> sessions = new ArrayList<>();

        sessions.add(createSession("FRI_1", "CSE501", "Machine Learning", "AI1801", "DE-401",
                date, 8, 30, 10, 0, "PRACTICE"));
        sessions.add(createSession("FRI_2", "REVIEW", "Week Review Meeting", "Lecturers", "Meeting Room",
                date, 16, 0, 17, 0, "MEETING"));

        return sessions;
    }

    /**
     * Generate Saturday schedule (lighter schedule)
     */
    private List<CalendarSession> generateSaturdaySchedule(Calendar date) {
        List<CalendarSession> sessions = new ArrayList<>();

        sessions.add(createSession("SAT_1", "EXTRA", "Extra Class", "SE1801", "DE-201",
                date, 9, 0, 10, 30, "LECTURE"));

        return sessions;
    }

    /**
     * Generate Sunday schedule (usually empty or meetings only)
     */
    private List<CalendarSession> generateSundaySchedule(Calendar date) {
        List<CalendarSession> sessions = new ArrayList<>();

        // Occasionally have meetings on Sunday
        if (date.get(Calendar.WEEK_OF_MONTH) % 2 == 0) {
            sessions.add(createSession("SUN_1", "PLANNING", "Weekly Planning", "Department", "Office",
                    date, 10, 0, 11, 0, "MEETING"));
        }

        return sessions;
    }

    /**
     * Helper method to create session
     */
    private CalendarSession createSession(String sessionId, String courseCode, String courseName,
                                          String className, String room, Calendar date,
                                          int startHour, int startMinute, int endHour, int endMinute,
                                          String sessionType) {
        Calendar startTime = (Calendar) date.clone();
        startTime.set(Calendar.HOUR_OF_DAY, startHour);
        startTime.set(Calendar.MINUTE, startMinute);
        startTime.set(Calendar.SECOND, 0);

        Calendar endTime = (Calendar) date.clone();
        endTime.set(Calendar.HOUR_OF_DAY, endHour);
        endTime.set(Calendar.MINUTE, endMinute);
        endTime.set(Calendar.SECOND, 0);

        CalendarSession session = new CalendarSession();
        session.setSessionId(sessionId);
        session.setCourseCode(courseCode);
        session.setCourseName(courseName);
        session.setClassName(className);
        session.setRoom(room);
        session.setStartTime(startTime.getTime());
        session.setEndTime(endTime.getTime());
        session.setSessionDate(date.getTime());
        session.setStatus("UPCOMING");
        session.setSessionType(sessionType);

        return session;
    }

    /**
     * Check if date is today
     */
    private boolean isToday(Calendar date) {
        Calendar today = Calendar.getInstance();
        return date.get(Calendar.YEAR) == today.get(Calendar.YEAR) &&
                date.get(Calendar.DAY_OF_YEAR) == today.get(Calendar.DAY_OF_YEAR);
    }

    /**
     * Update session status based on current time for today's sessions
     */
    private void updateSessionStatus(List<CalendarSession> sessions) {
        long currentTime = System.currentTimeMillis();

        for (CalendarSession session : sessions) {
            if (currentTime < session.getStartTime().getTime()) {
                session.setStatus("UPCOMING");
            } else if (currentTime >= session.getStartTime().getTime() &&
                    currentTime <= session.getEndTime().getTime()) {
                session.setStatus("ONGOING");
            } else {
                session.setStatus("COMPLETED");
            }
        }
    }
}
