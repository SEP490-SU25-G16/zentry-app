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
import vn.edu.fpt.zentryapp.student.data.model.response.StudentScheduleSession;

public class StudentScheduleViewModel extends ViewModel {

    private final MutableLiveData<Boolean> _isLoading = new MutableLiveData<>(false);
    private final MutableLiveData<List<StudentScheduleSession>> _schedules = new MutableLiveData<>();
    private final MutableLiveData<String> _errorMessage = new MutableLiveData<>();
    private final MutableLiveData<String> _successMessage = new MutableLiveData<>();
    private final MutableLiveData<UserProfile> _userProfile = new MutableLiveData<>();
    private final MutableLiveData<String> _greeting = new MutableLiveData<>();
    private final MutableLiveData<String> _subGreeting = new MutableLiveData<>();

    public LiveData<Boolean> isLoading() {
        return _isLoading;
    }

    public LiveData<List<StudentScheduleSession>> schedules() {
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
                List<StudentScheduleSession> mockStudentScheduleSessions = generateMockSchedules();
                _schedules.setValue(mockStudentScheduleSessions);
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

    private List<StudentScheduleSession> generateMockSchedules() {
        List<StudentScheduleSession> studentScheduleSessions = new ArrayList<>();

        String currentDay = getCurrentDayName();

        // ========== SCHEDULES ĐÃ QUA (Past) - Ở TRÊN ==========

        // Schedule đã kết thúc từ sáng sớm
        studentScheduleSessions.add(new StudentScheduleSession(
                "SCH001",
                "Mathematics",
                "07",
                currentDay,
                getCurrentTimeFormatted(-360), // Started 6 hours ago (6:00 AM)
                getCurrentTimeFormatted(-270), // Ended 4.5 hours ago (7:30 AM)
                "BE-201",
                "Nguyễn Văn A",
                "MATH07"
        ));

        // Schedule đã kết thúc từ sáng
        studentScheduleSessions.add(new StudentScheduleSession(
                "SCH002",
                "English Literature",
                "08",
                currentDay,
                getCurrentTimeFormatted(-240), // Started 4 hours ago (8:00 AM)
                getCurrentTimeFormatted(-150), // Ended 2.5 hours ago (9:30 AM)
                "BE-301",
                "Phạm Thị D",
                "ENG08"
        ));

        // Schedule vừa kết thúc gần đây
        studentScheduleSessions.add(new StudentScheduleSession(
                "SCH003",
                "Physics",
                "09",
                currentDay,
                getCurrentTimeFormatted(-120), // Started 2 hours ago (10:00 AM)
                getCurrentTimeFormatted(-30),  // Ended 30 minutes ago (11:30 AM)
                "BE-203",
                "Trần Thị B",
                "PHY09"
        ));

        // ========== SCHEDULE HIỆN TẠI (Current) ==========

        // Schedule đang diễn ra (clickable)
        studentScheduleSessions.add(new StudentScheduleSession(
                "SCH004",
                "Computer Science",
                "10",
                currentDay,
                getCurrentTimeFormatted(-15), // Started 15 minutes ago
                getCurrentTimeFormatted(75),  // Ends in 75 minutes
                "BE-401",
                "Lê Văn E",
                "CS10"
        ));

        // ========== SCHEDULES CHƯA BẮT ĐẦU (Future) - Ở DƯỚI ==========

        // Schedule sắp bắt đầu (gần kề - có thể clickable)
        studentScheduleSessions.add(new StudentScheduleSession(
                "SCH005",
                "Chemistry",
                "11",
                currentDay,
                getCurrentTimeFormatted(45),  // Starts in 45 minutes
                getCurrentTimeFormatted(135), // Ends in 2h 15m
                "BE-105",
                "Hoàng Văn F",
                "CHEM11"
        ));

        // Schedule chiều
        studentScheduleSessions.add(new StudentScheduleSession(
                "SCH006",
                "Biology",
                "12",
                currentDay,
                getCurrentTimeFormatted(120), // Starts in 2 hours
                getCurrentTimeFormatted(210), // Ends in 3.5 hours
                "BE-102",
                "Ngô Thị G",
                "BIO12"
        ));

        // Schedule muộn hơn
        studentScheduleSessions.add(new StudentScheduleSession(
                "SCH007",
                "History",
                "13",
                currentDay,
                getCurrentTimeFormatted(180), // Starts in 3 hours
                getCurrentTimeFormatted(270), // Ends in 4.5 hours
                "BE-304",
                "Đỗ Văn H",
                "HIST13"
        ));

        // Schedule cuối ngày
        studentScheduleSessions.add(new StudentScheduleSession(
                "SCH008",
                "Art & Design",
                "14",
                currentDay,
                getCurrentTimeFormatted(300), // Starts in 5 hours
                getCurrentTimeFormatted(390), // Ends in 6.5 hours
                "BE-501",
                "Vũ Thị I",
                "ART14"
        ));

        // Schedule tối
        studentScheduleSessions.add(new StudentScheduleSession(
                "SCH009",
                "Music Theory",
                "15",
                currentDay,
                getCurrentTimeFormatted(420), // Starts in 7 hours
                getCurrentTimeFormatted(510), // Ends in 8.5 hours
                "BE-502",
                "Bùi Văn K",
                "MUS15"
        ));

        return studentScheduleSessions;
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


    public void onScheduleClicked(StudentScheduleSession studentScheduleSession) {
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
