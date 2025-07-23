package vn.edu.fpt.zentryapp.student.ui.schedule;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import android.os.Handler;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;

import lombok.Getter;
import vn.edu.fpt.zentryapp.auth.client.AuthManager;
import vn.edu.fpt.zentryapp.student.data.model.response.Schedule;

public class StudentScheduleViewModel extends ViewModel {

    private final MutableLiveData<Boolean> _isLoading = new MutableLiveData<>(false);
    private final MutableLiveData<List<Schedule>> _schedules = new MutableLiveData<>();
    private final MutableLiveData<String> _errorMessage = new MutableLiveData<>();
    private final MutableLiveData<String> _successMessage = new MutableLiveData<>();
    private final MutableLiveData<UserProfile> _userProfile = new MutableLiveData<>();
    private final MutableLiveData<String> _greeting = new MutableLiveData<>();
    private final MutableLiveData<String> _subGreeting = new MutableLiveData<>();

    public LiveData<Boolean> isLoading() {
        return _isLoading;
    }

    public LiveData<List<Schedule>> schedules() {
        return _schedules;
    }

    public LiveData<String> errorMessage() {
        return _errorMessage;
    }

    public LiveData<String> successMessage() {
        return _successMessage;
    }

    public LiveData<UserProfile> userProfile() {
        return _userProfile;
    }

    public LiveData<String> greeting() {
        return _greeting;
    }

    public LiveData<String> subGreeting() {
        return _subGreeting;
    }

    private AuthManager authManager;

    public void init(AuthManager authManager) {
        this.authManager = authManager;
        loadUserProfile();
        loadSchedules();
        generateGreeting();
    }

    public void loadSchedules() {
        if (Boolean.TRUE.equals(_isLoading.getValue())) {
            return;
        }

        _isLoading.setValue(true);
        _errorMessage.setValue(null);

        new Handler().postDelayed(() -> {
            try {
                List<Schedule> mockSchedules = generateMockSchedules();
                _schedules.setValue(mockSchedules);
                _successMessage.setValue("Schedules loaded successfully");
            } catch (Exception e) {
                _errorMessage.setValue("Failed to load schedules: " + e.getMessage());
            } finally {
                _isLoading.setValue(false);
            }
        }, 1000);
    }

    private void loadUserProfile() {
        if (authManager != null && authManager.isLoggedIn()) {
            String email = authManager.getCurrentUserEmail();
            String role = authManager.getCurrentUserRole();
            String userId = authManager.getCurrentUserId();

            String name = extractNameFromEmail(email);
            _userProfile.setValue(new UserProfile(userId, name, email, role));
        }
    }

    private void generateGreeting() {
        java.util.Calendar calendar = java.util.Calendar.getInstance();
        int hour = calendar.get(java.util.Calendar.HOUR_OF_DAY);

        String greeting;
        String subGreeting;

        if (hour < 12) {
            greeting = "Good Morning";
            subGreeting = "Ready for today's classes?";
        } else if (hour < 17) {
            greeting = "Good Afternoon";
            subGreeting = "Check your upcoming classes";
        } else {
            greeting = "Good Evening";
            subGreeting = "Review tomorrow's schedule";
        }

        if (authManager != null && authManager.isLoggedIn()) {
            String email = authManager.getCurrentUserEmail();
            String name = extractNameFromEmail(email);
            greeting = "Hi, " + name;
        }

        _greeting.setValue(greeting);
        _subGreeting.setValue(subGreeting);
    }

    private String extractNameFromEmail(String email) {
        if (email == null || !email.contains("@")) {
            return "Student";
        }

        String username = email.split("@")[0];
        if (username.length() > 0) {
            return username.substring(0, 1).toUpperCase() + username.substring(1);
        }

        return "Student";
    }

    private List<Schedule> generateMockSchedules() {
        List<Schedule> schedules = new ArrayList<>();

        // Get current time for testing
        Calendar now = Calendar.getInstance();
        String currentDay = getCurrentDayName();

        // Add a schedule for current time (should be clickable)
        schedules.add(new Schedule(
                "SCH001",
                "Mathematics",
                "07",
                currentDay,
                getCurrentTimeFormatted(-30), // Started 30 minutes ago
                getCurrentTimeFormatted(30),  // Ends in 30 minutes
                "BE-201",
                "Nguyễn Văn A",
                "MATH07"
        ));

        // Add a schedule for later today (should not be clickable yet)
        schedules.add(new Schedule(
                "SCH002",
                "Physics",
                "08",
                currentDay,
                getCurrentTimeFormatted(60),  // Starts in 1 hour
                getCurrentTimeFormatted(120), // Ends in 2 hours
                "BE-203",
                "Trần Thị B",
                "PHY08"
        ));

        // Add a schedule that's about to start (should be clickable)
        schedules.add(new Schedule(
                "SCH003",
                "Chemistry",
                "07",
                currentDay,
                getCurrentTimeFormatted(10),  // Starts in 10 minutes
                getCurrentTimeFormatted(70),  // Ends in 70 minutes
                "BE-105",
                "Lê Văn C",
                "CHEM07"
        ));

        return schedules;
    }

    private String getCurrentDayName() {
        String[] days = {"Sunday", "Monday", "Tuesday", "Wednesday", "Thursday", "Friday", "Saturday"};
        Calendar calendar = Calendar.getInstance();
        return days[calendar.get(Calendar.DAY_OF_WEEK) - 1];
    }

    private String getCurrentTimeFormatted(int offsetMinutes) {
        Calendar calendar = Calendar.getInstance();
        calendar.add(Calendar.MINUTE, offsetMinutes);

        SimpleDateFormat format = new SimpleDateFormat("h:mm a", Locale.US);
        return format.format(calendar.getTime());
    }


    public void onScheduleClicked(Schedule schedule) {
        // TODO: Handle schedule click for navigation
    }

    @Getter
    public static class UserProfile {
        private final String id;
        private final String name;
        private final String email;
        private final String role;

        public UserProfile(String id, String name, String email, String role) {
            this.id = id;
            this.name = name;
            this.email = email;
            this.role = role;
        }
    }
}
