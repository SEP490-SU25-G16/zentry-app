package vn.edu.fpt.zentryapp.student.ui.home;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import android.os.Handler;

import java.util.ArrayList;
import java.util.List;

import lombok.Getter;
import vn.edu.fpt.zentryapp.auth.client.AuthManager;
import vn.edu.fpt.zentryapp.student.data.model.response.StudentCourse;

public class StudentHomeViewModel extends ViewModel {

    // LiveData cho UI state
    private final MutableLiveData<Boolean> _isLoading = new MutableLiveData<>(false);
    private final MutableLiveData<Boolean> _isRefreshing = new MutableLiveData<>(false);
    private final MutableLiveData<List<StudentCourse>> _courses = new MutableLiveData<>();
    private final MutableLiveData<String> _errorMessage = new MutableLiveData<>();
    private final MutableLiveData<String> _successMessage = new MutableLiveData<>();
    private final MutableLiveData<UserProfile> _userProfile = new MutableLiveData<>();
    private final MutableLiveData<String> _greeting = new MutableLiveData<>();
    private final MutableLiveData<String> _subGreeting = new MutableLiveData<>();

    // Public getters cho Fragment observe
    public LiveData<Boolean> isLoading() {
        return _isLoading;
    }

    public LiveData<Boolean> isRefreshing() {
        return _isRefreshing;
    }

    public LiveData<List<StudentCourse>> courses() {
        return _courses;
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
        loadCourses();
        generateGreeting();
    }

    /**
     * Load student courses data
     */
    public void loadCourses() {
        if (Boolean.TRUE.equals(_isLoading.getValue())) {
            return; // Prevent multiple simultaneous requests
        }

        _isLoading.setValue(true);
        _errorMessage.setValue(null);

        // Simulate network delay
        new Handler().postDelayed(() -> {
            try {
                List<StudentCourse> mockCourses = generateMockCourses();
                _courses.setValue(mockCourses);
                _successMessage.setValue("Courses loaded successfully");
            } catch (Exception e) {
                _errorMessage.setValue("Failed to load courses: " + e.getMessage());
            } finally {
                _isLoading.setValue(false);
            }
        }, 1000);
    }

    /**
     * Refresh courses data
     */
    public void refreshCourses() {
        _isRefreshing.setValue(true);
        _errorMessage.setValue(null);

        new Handler().postDelayed(() -> {
            try {
                List<StudentCourse> mockCourses = generateMockCourses();
                _courses.setValue(mockCourses);
            } catch (Exception e) {
                _errorMessage.setValue("Failed to refresh courses: " + e.getMessage());
            } finally {
                _isRefreshing.setValue(false);
            }
        }, 500);
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
        String subGreeting;

        if (hour < 12) {
            greeting = "Good Morning";
            subGreeting = "Ready to learn something new today?";
        } else if (hour < 17) {
            greeting = "Good Afternoon";
            subGreeting = "Hope your classes are going well!";
        } else {
            greeting = "Good Evening";
            subGreeting = "Time to review today's lessons.";
        }

        if (authManager != null && authManager.isLoggedIn()) {
            String email = authManager.getCurrentUserEmail();
            String name = extractNameFromEmail(email);
            greeting = "Hi, " + name;
        }

        _greeting.setValue(greeting);
        _subGreeting.setValue(subGreeting);
    }

    /**
     * Extract name from email
     */
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

    /**
     * Generate mock student courses data
     */
    private List<StudentCourse> generateMockCourses() {
        List<StudentCourse> courses = new ArrayList<>();

        courses.add(new StudentCourse(
                "SC001",
                "Lập trình căn bản",
                "CSE101",
                "SE1801",
                "BE-201",
                "Nguyễn Văn A",
                95,
                8,
                10,
                "Fall 2024",
                "2024-2025",
                "Active"
        ));

        courses.add(new StudentCourse(
                "SC002",
                "Cấu trúc dữ liệu và giải thuật",
                "CSE201",
                "SE1801",
                "BE-203",
                "Trần Thị B",
                88,
                12,
                15,
                "Fall 2024",
                "2024-2025",
                "Active"
        ));

        courses.add(new StudentCourse(
                "SC003",
                "Lập trình Web",
                "CSE301",
                "SE1801",
                "BE-105",
                "Lê Văn C",
                92,
                6,
                8,
                "Fall 2024",
                "2024-2025",
                "Active"
        ));

        courses.add(new StudentCourse(
                "SC004",
                "Cơ sở dữ liệu",
                "CSE202",
                "SE1801",
                "BE-302",
                "Phạm Thị D",
                85,
                10,
                12,
                "Fall 2024",
                "2024-2025",
                "Active"
        ));

        return courses;
    }

    /**
     * Handle course item click
     */
    public void onCourseClicked(StudentCourse course) {
        // TODO: Navigate to course detail screen
    }

    /**
     * User profile data
     */
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