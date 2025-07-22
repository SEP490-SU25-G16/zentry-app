package vn.edu.fpt.zentryapp.lecturer.ui.report;

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
import vn.edu.fpt.zentryapp.lecturer.data.model.response.CourseInfo;
import vn.edu.fpt.zentryapp.lecturer.data.model.response.SessionDetail;

public class LecturerReportListSessionViewModel extends ViewModel {

    // LiveData cho UI state
    private final MutableLiveData<Boolean> _isLoading = new MutableLiveData<>(false);
    private final MutableLiveData<CourseInfo> _courseInfo = new MutableLiveData<>();
    private final MutableLiveData<List<SessionDetail>> _sessions = new MutableLiveData<>();
    private final MutableLiveData<String> _errorMessage = new MutableLiveData<>();

    // Public getters cho Fragment observe
    public LiveData<Boolean> isLoading() {
        return _isLoading;
    }

    public LiveData<CourseInfo> courseInfo() {
        return _courseInfo;
    }

    public LiveData<List<SessionDetail>> sessions() {
        return _sessions;
    }

    public LiveData<String> errorMessage() {
        return _errorMessage;
    }

    private AuthManager authManager;
    private String courseCode;
    private String className;
    private String courseName;

    public void init(AuthManager authManager, String courseCode, String className, String courseName) {
        this.authManager = authManager;
        this.courseCode = courseCode;
        this.className = className;
        this.courseName = courseName;

        loadCourseInfo();
        loadSessions();
    }

    /**
     * Load course information
     */
    private void loadCourseInfo() {
        _isLoading.setValue(true);

        // Simulate network delay
        new Handler().postDelayed(() -> {
            CourseInfo courseInfo = generateCourseInfo(courseCode, courseName, className);
            _courseInfo.setValue(courseInfo);
        }, 500);
    }

    /**
     * Load all sessions for this course and class
     */
    private void loadSessions() {
        // Simulate network delay
        new Handler().postDelayed(() -> {
            List<SessionDetail> sessions = generateMockSessions(courseCode, courseName, className);
            _sessions.setValue(sessions);
            _isLoading.setValue(false);
        }, 1000);
    }

    /**
     * Refresh sessions data
     */
    public void refreshSessions() {
        loadCourseInfo();
        loadSessions();
    }

    /**
     * Generate mock course info
     */
    private CourseInfo generateCourseInfo(String courseCode, String courseName, String className) {
        CourseInfo courseInfo = new CourseInfo();
        courseInfo.setCourseCode(courseCode);
        courseInfo.setCourseName(courseName);
        courseInfo.setClassName(className);
        courseInfo.setRoom("DE-201");
        courseInfo.setGrade("07");
        courseInfo.setTotalStudents(32);
        courseInfo.setTotalSessions(20);
        courseInfo.setCompletedSessions(12);
        courseInfo.setSemester("Fall 2024");
        courseInfo.setAcademicYear("2024-2025");

        return courseInfo;
    }

    /**
     * Generate mock sessions data
     */
    private List<SessionDetail> generateMockSessions(String courseCode, String courseName, String className) {
        List<SessionDetail> sessions = new ArrayList<>();

        Calendar calendar = Calendar.getInstance();
        calendar.set(2024, Calendar.NOVEMBER, 1); // Start from Nov 1, 2024

        // Generate 20 sessions (some completed, some upcoming)
        for (int i = 1; i <= 20; i++) {
            SessionDetail session = new SessionDetail();
            session.setSessionId("S" + courseCode + "_" + i);
            session.setCourseCode(courseCode);
            session.setCourseName(courseName);
            session.setClassName(className);
            session.setRoom("DE-201");
            session.setSessionNumber(i);
            session.setDate(calendar.getTime());
            session.setStartTime("08:00");
            session.setEndTime("09:30");
            session.setTotalStudents(32);
            session.setDescription("Session " + i + " of " + courseName);

            // Set attendance and status based on session number
            Date today = new Date();
            if (calendar.getTime().before(today)) {
                // Past sessions - completed
                session.setStatus("COMPLETED");
                session.setPresentStudents(generateRandomAttendance(32, i));
            } else if (i == 13) {
                // Current session - ongoing
                session.setStatus("ONGOING");
                session.setPresentStudents(30);
            } else {
                // Future sessions - upcoming
                session.setStatus("UPCOMING");
                session.setPresentStudents(0);
            }

            sessions.add(session);

            // Move to next session date (every 3 days)
            calendar.add(Calendar.DAY_OF_MONTH, 3);
        }

        return sessions;
    }

    /**
     * Generate realistic attendance numbers
     */
    private int generateRandomAttendance(int total, int sessionNumber) {
        // Simulate realistic attendance patterns
        double baseAttendance = 0.85; // 85% base attendance
        double variation = Math.random() * 0.15; // ±15% variation

        // First few sessions have higher attendance
        if (sessionNumber <= 3) {
            baseAttendance = 0.95;
        }
        // Mid-term sessions might have lower attendance
        else if (sessionNumber >= 8 && sessionNumber <= 10) {
            baseAttendance = 0.75;
        }

        int attendance = (int) (total * (baseAttendance + variation));
        return Math.max(15, Math.min(total, attendance)); // Ensure reasonable bounds
    }

    /**
     * Handle session item click
     */
    public void onSessionClicked(SessionDetail session) {
        // This can be observed by Fragment to navigate
    }
}