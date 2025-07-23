package vn.edu.fpt.zentryapp.student.ui.report;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import android.os.Handler;

import java.util.ArrayList;
import java.util.List;

import lombok.Getter;
import vn.edu.fpt.zentryapp.auth.client.AuthManager;
import vn.edu.fpt.zentryapp.student.data.model.response.StudentReport;

public class StudentReportViewModel extends ViewModel {

    private final MutableLiveData<Boolean> _isLoading = new MutableLiveData<>(false);
    private final MutableLiveData<List<StudentReport>> _reports = new MutableLiveData<>();
    private final MutableLiveData<String> _errorMessage = new MutableLiveData<>();
    private final MutableLiveData<String> _successMessage = new MutableLiveData<>();
    private final MutableLiveData<UserProfile> _userProfile = new MutableLiveData<>();
    private final MutableLiveData<String> _greeting = new MutableLiveData<>();
    private final MutableLiveData<String> _subGreeting = new MutableLiveData<>();

    public LiveData<Boolean> isLoading() { return _isLoading; }
    public LiveData<List<StudentReport>> reports() { return _reports; }
    public LiveData<String> errorMessage() { return _errorMessage; }
    public LiveData<String> successMessage() { return _successMessage; }
    public LiveData<UserProfile> userProfile() { return _userProfile; }
    public LiveData<String> greeting() { return _greeting; }
    public LiveData<String> subGreeting() { return _subGreeting; }

    private AuthManager authManager;

    public void init(AuthManager authManager) {
        this.authManager = authManager;
        loadUserProfile();
        loadReports();
        generateGreeting();
    }

    public void loadReports() {
        if (Boolean.TRUE.equals(_isLoading.getValue())) {
            return;
        }

        _isLoading.setValue(true);
        _errorMessage.setValue(null);

        new Handler().postDelayed(() -> {
            try {
                List<StudentReport> mockReports = generateMockReports();
                _reports.setValue(mockReports);
                _successMessage.setValue("Reports loaded successfully");
            } catch (Exception e) {
                _errorMessage.setValue("Failed to load reports: " + e.getMessage());
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
            subGreeting = "Check your daily attendance reports";
        } else if (hour < 17) {
            greeting = "Good Afternoon";
            subGreeting = "Review your class performance";
        } else {
            greeting = "Good Evening";
            subGreeting = "Time to check today's progress";
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

    private List<StudentReport> generateMockReports() {
        List<StudentReport> reports = new ArrayList<>();

        reports.add(new StudentReport(
                "SR001",
                "MATHEMATICS",
                "MATH101",
                "SE1801",
                "BE-201",
                "Gayan Iddamalgoda",
                "Present",
                10,
                10,
                "Fall 2024",
                "2024-2025"
        ));

        reports.add(new StudentReport(
                "SR002",
                "SCIENCE",
                "SCI101",
                "SE1801",
                "BE-203",
                "Tharidu Diwakara",
                "Absent",
                8,
                10,
                "Fall 2024",
                "2024-2025"
        ));

        reports.add(new StudentReport(
                "SR003",
                "ENGLISH",
                "ENG101",
                "SE1801",
                "BE-105",
                "Mary Johnson",
                "Present",
                9,
                10,
                "Fall 2024",
                "2024-2025"
        ));

        return reports;
    }

    public void onReportClicked(StudentReport report) {
        // TODO: Handle report click - navigate to session list
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
