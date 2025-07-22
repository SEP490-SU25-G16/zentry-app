package vn.edu.fpt.zentryapp.lecturer.ui.report;

import android.os.Handler;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import lombok.AllArgsConstructor;
import lombok.Getter;
import vn.edu.fpt.zentryapp.auth.client.AuthManager;
import vn.edu.fpt.zentryapp.lecturer.data.model.response.Session;

public class LecturerReportViewModel extends ViewModel {

    // LiveData cho UI state
    private final MutableLiveData<Boolean> _isLoading = new MutableLiveData<>(false);
    private final MutableLiveData<List<Session>> _todaySessions = new MutableLiveData<>();
    private final MutableLiveData<String> _errorMessage = new MutableLiveData<>();
    private final MutableLiveData<UserProfile> _userProfile = new MutableLiveData<>();
    private final MutableLiveData<String> _greeting = new MutableLiveData<>();
    private final MutableLiveData<String> _currentDate = new MutableLiveData<>();

    // Public getters cho Fragment observe
    public LiveData<Boolean> isLoading() {
        return _isLoading;
    }

    public LiveData<List<Session>> todaySessions() {
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
     * Load today's sessions (fake data for now)
     */
    public void loadTodaySessions() {
        _isLoading.setValue(true);

        // Simulate network delay
        new Handler().postDelayed(() -> {
            List<Session> mockSessions = generateMockSessions();
            _todaySessions.setValue(mockSessions);
            _isLoading.setValue(false);
        }, 1000);
    }

    /**
     * Refresh today's sessions
     */
    public void refreshSessions() {
        loadTodaySessions();
    }

    /**
     * Load user profile from AuthManager
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
     * Generate greeting based on time of day
     */
    private void generateGreeting() {
        java.util.Calendar calendar = java.util.Calendar.getInstance();
        int hour = calendar.get(java.util.Calendar.HOUR_OF_DAY);

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
     * Extract name from email
     */
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

    /**
     * Generate mock sessions data for today
     */
    private List<Session> generateMockSessions() {
        List<Session> sessions = new ArrayList<>();

        String today = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(new Date());

        sessions.add(new Session(
                "S001",
                "Lập trình căn bản",
                "CSE101",
                "SE1801",
                "DE-201",
                "08:00",
                "09:30",
                35,
                33,
                5,
                5,
                today,
                "COMPLETED"
        ));

        sessions.add(new Session(
                "S002",
                "Cấu trúc dữ liệu và giải thuật",
                "CSE201",
                "SE1802",
                "DE-203",
                "09:45",
                "11:15",
                32,
                29,
                8,
                7,
                today,
                "COMPLETED"
        ));

        sessions.add(new Session(
                "S003",
                "Lập trình Web",
                "CSE301",
                "SE1803",
                "DE-105",
                "13:30",
                "15:00",
                30,
                27,
                6,
                4,
                today,
                "ONGOING"
        ));

        sessions.add(new Session(
                "S004",
                "Phát triển ứng dụng di động",
                "CSE401",
                "SE1804",
                "DE-302",
                "15:15",
                "16:45",
                28,
                0,
                10,
                6,
                today,
                "SCHEDULED"
        ));

        sessions.add(new Session(
                "S005",
                "Machine Learning",
                "CSE501",
                "AI1801",
                "DE-401",
                "17:00",
                "18:30",
                25,
                0,
                12,
                8,
                today,
                "SCHEDULED"
        ));

        return sessions;
    }

    /**
     * Handle session item click
     */
    public void onSessionClicked(Session session) {
        // This can be observed by Fragment to navigate
    }

    // Inner Classes

    /**
     * User profile data
     */
    @Getter
    @AllArgsConstructor
    public static class UserProfile {
        private final String id;
        private final String name;
        private final String email;
        private final String role;

    }
}
