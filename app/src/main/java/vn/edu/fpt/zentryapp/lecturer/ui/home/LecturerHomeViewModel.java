package vn.edu.fpt.zentryapp.lecturer.ui.home;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import android.os.Handler;

import java.util.ArrayList;
import java.util.List;

import lombok.Getter;
import vn.edu.fpt.zentryapp.auth.client.AuthManager;
import vn.edu.fpt.zentryapp.lecturer.data.model.response.Course;

public class LecturerHomeViewModel extends ViewModel {

    // LiveData cho UI state
    private final MutableLiveData<Boolean> _isLoading = new MutableLiveData<>(false);
    private final MutableLiveData<List<Course>> _courses = new MutableLiveData<>();
    private final MutableLiveData<String> _errorMessage = new MutableLiveData<>();
    private final MutableLiveData<UserProfile> _userProfile = new MutableLiveData<>();
    private final MutableLiveData<String> _greeting = new MutableLiveData<>();

    // Public getters cho Fragment observe
    public LiveData<Boolean> isLoading() {
        return _isLoading;
    }

    public LiveData<List<Course>> courses() {
        return _courses;
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

    private AuthManager authManager;

    public void init(AuthManager authManager) {
        this.authManager = authManager;
        loadUserProfile();
        loadCourses();
        generateGreeting();
    }

    /**
     * Load courses data (fake data for now)
     */
    public void loadCourses() {
        _isLoading.setValue(true);

        // Simulate network delay
        new Handler().postDelayed(() -> {
            List<Course> mockCourses = generateMockCourses();
            _courses.setValue(mockCourses);
            _isLoading.setValue(false);
        }, 1000);
    }

    /**
     * Refresh courses data
     */
    public void refreshCourses() {
        loadCourses();
    }

    /**
     * Load user profile from AuthManager
     */
    private void loadUserProfile() {
        if (authManager != null && authManager.isLoggedIn()) {
            String email = authManager.getCurrentUserEmail();
            String role = authManager.getCurrentUserRole();
            String userId = authManager.getCurrentUserId();

            // Extract name from email (simple approach)
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
     * Extract name from email (simple implementation)
     */
    private String extractNameFromEmail(String email) {
        if (email == null || !email.contains("@")) {
            return "Lecturer";
        }

        String username = email.split("@")[0];
        // Convert username to display name (capitalize first letter)
        if (username.length() > 0) {
            return username.substring(0, 1).toUpperCase() + username.substring(1);
        }

        return "Lecturer";
    }

    /**
     * Generate mock courses data
     */
    private List<Course> generateMockCourses() {
        List<Course> courses = new ArrayList<>();

        courses.add(new Course(
                "CSE101",
                "Lập trình căn bản",
                "CSE101",
                "SE1801",
                "BE-201",
                12,
                10,
                null,
                "Fall 2024",
                "2024-2025"
        ));

        courses.add(new Course(
                "CSE201",
                "Cấu trúc dữ liệu và giải thuật",
                "CSE201",
                "SE1802",
                "BE-203",
                15,
                15,
                null,
                "Fall 2024",
                "2024-2025"
        ));

        courses.add(new Course(
                "CSE301",
                "Lập trình Web",
                "CSE301",
                "SE1803",
                "BE-105",
                10,
                7,
                null,
                "Fall 2024",
                "2024-2025"
        ));

        courses.add(new Course(
                "CSE401",
                "Phát triển ứng dụng di động",
                "CSE401",
                "SE1804",
                "BE-302",
                8,
                5,
                null,
                "Fall 2024",
                "2024-2025"
        ));

        courses.add(new Course(
                "CSE501",
                "Machine Learning",
                "CSE501",
                "AI1801",
                "BE-401",
                20,
                12,
                null,
                "Fall 2024",
                "2024-2025"
        ));

        return courses;
    }

    /**
     * Handle course item click
     */
    public void onCourseClicked(Course course) {
        // TODO: Navigate to course detail or handle click
        // This can be observed by Fragment to navigate
    }

    // Inner Classes

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