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

import lombok.AllArgsConstructor;
import lombok.Getter;
import vn.edu.fpt.zentryapp.auth.client.AuthManager;
import vn.edu.fpt.zentryapp.lecturer.data.model.response.ScheduleSession;

public class LecturerScheduleViewModel extends ViewModel {

    // LiveData cho UI state
    private final MutableLiveData<Boolean> _isLoading = new MutableLiveData<>(false);
    private final MutableLiveData<List<ScheduleSession>> _todaySessions = new MutableLiveData<>();
    private final MutableLiveData<String> _errorMessage = new MutableLiveData<>();
    private final MutableLiveData<UserProfile> _userProfile = new MutableLiveData<>();
    private final MutableLiveData<String> _greeting = new MutableLiveData<>();
    private final MutableLiveData<String> _currentDate = new MutableLiveData<>();

    // Public getters cho Fragment observe
    public LiveData<Boolean> isLoading() {
        return _isLoading;
    }

    public LiveData<List<ScheduleSession>> todaySessions() {
        return _todaySessions;
    }

    public LiveData<String> errorMessage() {
        return _errorMessage;
    }

    public LiveData<UserProfile> userProfile() {
        return _userProfile;
    }

    public LiveData<String> greeting() {
        return _greeting;
    }

    public LiveData<String> currentDate() {
        return _currentDate;
    }

    private AuthManager authManager;

    public void init(AuthManager authManager) {
        this.authManager = authManager;
        loadUserProfile();
        loadTodaySessions();
        generateGreeting();
        updateCurrentDate();
    }

    /**
     * Load today's scheduled sessions
     */
    private void loadTodaySessions() {
        _isLoading.setValue(true);

        // Simulate network delay
        new Handler().postDelayed(() -> {
            List<ScheduleSession> sessions = generateTodaySchedule();
            _todaySessions.setValue(sessions);
            _isLoading.setValue(false);
        }, 1000);
    }

    /**
     * Refresh sessions
     */
    public void refreshSessions() {
        loadTodaySessions();
    }

    /**
     * Load user profile
     */
    private void loadUserProfile() {
        if (authManager != null && authManager.isLoggedIn()) {
            String email = authManager.getCurrentUserEmail();
            String role = authManager.getCurrentUserRole();
            String userId = authManager.getCurrentUserId();

            String name = extractNameFromEmail(email);
            _userProfile.setValue(new UserProfile(userId, name, email, role));
        }
    }

    /**
     * Generate greeting
     */
    private void generateGreeting() {
        Calendar calendar = Calendar.getInstance();
        int hour = calendar.get(Calendar.HOUR_OF_DAY);

        String greeting;
        if (hour < 12) {
            greeting = "Good Morning";
        } else if (hour < 17) {
            greeting = "Good Afternoon";
        } else {
            greeting = "Good Evening";
        }

        if (authManager != null && authManager.isLoggedIn()) {
            String email = authManager.getCurrentUserEmail();
            String name = extractNameFromEmail(email);
            greeting += ", " + name + "!";
        }

        _greeting.setValue(greeting);
    }

    /**
     * Update current date
     */
    private void updateCurrentDate() {
        SimpleDateFormat dateFormat = new SimpleDateFormat("EEEE, MMMM dd, yyyy", Locale.getDefault());
        String currentDate = dateFormat.format(new Date());
        _currentDate.setValue(currentDate);
    }

    /**
     * Generate mock schedule for today
     */
    private List<ScheduleSession> generateTodaySchedule() {
        List<ScheduleSession> sessions = new ArrayList<>();
        Calendar today = Calendar.getInstance();

        // Session 1: Past session (8:00-9:30) - Can view detail, cannot start
        Calendar session1 = (Calendar) today.clone();
        session1.set(Calendar.HOUR_OF_DAY, 8);
        session1.set(Calendar.MINUTE, 0);
        Calendar session1End = (Calendar) session1.clone();
        session1End.set(Calendar.HOUR_OF_DAY, 9);
        session1End.set(Calendar.MINUTE, 30);

        sessions.add(new ScheduleSession(
                "SCH001",
                "CSE101",
                "Lập trình căn bản",
                "SE1801",
                "DE-201",
                session1.getTime(),
                session1End.getTime(),
                today.getTime(),
                "COMPLETED",
                false, // Cannot start - already passed
                true   // Can view detail
        ));

        // Session 2: Current ongoing session (current time ± 1 hour)
        Calendar session2 = (Calendar) today.clone();
        session2.add(Calendar.HOUR_OF_DAY, -1); // Started 1 hour ago
        Calendar session2End = (Calendar) today.clone();
        session2End.add(Calendar.HOUR_OF_DAY, 1); // Ends in 1 hour

        sessions.add(new ScheduleSession(
                "SCH002",
                "CSE201",
                "Cấu trúc dữ liệu và giải thuật",
                "SE1802",
                "DE-203",
                session2.getTime(),
                session2End.getTime(),
                today.getTime(),
                "ONGOING",
                true, // Can start instant - currently in session
                true  // Can view detail
        ));

        // Session 3: Upcoming session starting soon (in 30 minutes)
        Calendar session3 = (Calendar) today.clone();
        session3.add(Calendar.MINUTE, 30);
        Calendar session3End = (Calendar) session3.clone();
        session3End.add(Calendar.HOUR, 2);

        sessions.add(new ScheduleSession(
                "SCH003",
                "CSE301",
                "Lập trình Web",
                "SE1803",
                "DE-105",
                session3.getTime(),
                session3End.getTime(),
                today.getTime(),
                "UPCOMING",
                true, // Can start instant - starting soon
                false // Cannot view detail - no data yet
        ));

        // Session 4: Future session (in 4 hours)
        Calendar session4 = (Calendar) today.clone();
        session4.add(Calendar.HOUR_OF_DAY, 4);
        Calendar session4End = (Calendar) session4.clone();
        session4End.add(Calendar.HOUR, 2);

        sessions.add(new ScheduleSession(
                "SCH004",
                "CSE401",
                "Phát triển ứng dụng di động",
                "SE1804",
                "DE-302",
                session4.getTime(),
                session4End.getTime(),
                today.getTime(),
                "UPCOMING",
                false, // Cannot start - too early
                false  // Cannot view detail - no data yet
        ));

        // Update can start instant and can view detail flags
        updateSessionPermissions(sessions);

        return sessions;
    }

    /**
     * Update session permissions based on current time
     */
    private void updateSessionPermissions(List<ScheduleSession> sessions) {
        for (ScheduleSession session : sessions) {
            // Can start instant: if session is ongoing or starting within 15 minutes
            boolean canStart = session.isCurrentTimeInSession() || session.isSessionStartingSoon();
            session.setCanStartInstant(canStart);

            // Can view detail: if session is completed or ongoing
            boolean canView = session.isSessionPassed() || session.isCurrentTimeInSession();
            session.setCanViewDetail(canView);
        }
    }

    /**
     * Handle start instant class
     */
    public void startInstantClass(ScheduleSession session) {
        if (!session.isCanStartInstant()) {
            _errorMessage.setValue("Cannot start this session at the moment");
            return;
        }

        // TODO: Implement start instant class logic
        // This could involve:
        // 1. Creating a new session record
        // 2. Starting attendance tracking
        // 3. Notifying students
        // 4. Opening teaching interface

        _errorMessage.setValue("Starting class: " + session.getCourseName());
    }

    private String extractNameFromEmail(String email) {
        if (email == null || !email.contains("@")) {
            return "Lecturer";
        }

        String username = email.split("@")[0];
        if (username.length() > 0) {
            return username.substring(0, 1).toUpperCase() + username.substring(1);
        }

        return "Lecturer";
    }

    // Inner Classes
    @Getter
    @AllArgsConstructor
    public static class UserProfile {
        private final String id;
        private final String name;
        private final String email;
        private final String role;

    }
}
