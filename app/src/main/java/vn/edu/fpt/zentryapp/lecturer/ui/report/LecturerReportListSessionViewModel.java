package vn.edu.fpt.zentryapp.lecturer.ui.report;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import android.os.Handler;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

import vn.edu.fpt.zentryapp.auth.client.AuthManager;
import vn.edu.fpt.zentryapp.lecturer.data.model.response.CourseInfo;
import vn.edu.fpt.zentryapp.lecturer.data.model.response.OverviewSession;

public class LecturerReportListSessionViewModel extends ViewModel {

    // LiveData cho UI state
    private final MutableLiveData<Boolean> _isLoading = new MutableLiveData<>(false);
    private final MutableLiveData<CourseInfo> _courseInfo = new MutableLiveData<>();
    private final MutableLiveData<List<OverviewSession>> _sessions = new MutableLiveData<>();
    private final MutableLiveData<String> _errorMessage = new MutableLiveData<>();

    // Public getters cho Fragment observe
    public LiveData<Boolean> isLoading() {
        return _isLoading;
    }

    public LiveData<CourseInfo> courseInfo() {
        return _courseInfo;
    }

    public LiveData<List<OverviewSession>> sessions() {
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
            List<OverviewSession> sessions = generateMockSessions();
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
     * Generate mock sessions data - simplified
     */
    private List<OverviewSession> generateMockSessions() {
        List<OverviewSession> sessions = new ArrayList<>();

        Calendar calendar = Calendar.getInstance();
        calendar.set(2024, Calendar.NOVEMBER, 1);

        // Generate 20 sessions
        for (int i = 1; i <= 20; i++) {
            OverviewSession session = new OverviewSession();
            session.setSessionId("S" + courseCode + "_" + i);
            session.setSessionNumber(i);
            session.setDate(calendar.getTime());
            session.setTotalStudents(32);
            session.setPresentStudents(generateRandomAttendance(32, i));

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
        double baseAttendance = 0.85;
        double variation = Math.random() * 0.15;

        if (sessionNumber <= 3) {
            baseAttendance = 0.95;
        } else if (sessionNumber >= 8 && sessionNumber <= 10) {
            baseAttendance = 0.75;
        }

        int attendance = (int) (total * (baseAttendance + variation));
        return Math.max(15, Math.min(total, attendance));
    }
}
