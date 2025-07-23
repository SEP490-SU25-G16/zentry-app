package vn.edu.fpt.zentryapp.student.ui.schedule.tabs;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import android.os.Handler;

import java.util.ArrayList;
import java.util.List;

import vn.edu.fpt.zentryapp.auth.client.AuthManager;
import vn.edu.fpt.zentryapp.student.data.model.response.ClassSession;

public class ClassHistoryViewModel extends ViewModel {

    private final MutableLiveData<Boolean> _isLoading = new MutableLiveData<>(false);
    private final MutableLiveData<List<ClassSession>> _sessions = new MutableLiveData<>();
    private final MutableLiveData<String> _errorMessage = new MutableLiveData<>();
    private final MutableLiveData<String> _successMessage = new MutableLiveData<>();

    public LiveData<Boolean> isLoading() { return _isLoading; }
    public LiveData<List<ClassSession>> sessions() { return _sessions; }
    public LiveData<String> errorMessage() { return _errorMessage; }
    public LiveData<String> successMessage() { return _successMessage; }

    private AuthManager authManager;

    public void init(AuthManager authManager, String classId) {
        this.authManager = authManager;
        loadSessions(classId);
    }

    public void loadSessions(String classId) {
        _isLoading.setValue(true);
        _errorMessage.setValue(null);

        new Handler().postDelayed(() -> {
            try {
                List<ClassSession> mockSessions = generateMockSessions();
                _sessions.setValue(mockSessions);
                _successMessage.setValue("Sessions loaded successfully");
            } catch (Exception e) {
                _errorMessage.setValue("Failed to load sessions: " + e.getMessage());
            } finally {
                _isLoading.setValue(false);
            }
        }, 1000);
    }

    private List<ClassSession> generateMockSessions() {
        List<ClassSession> sessions = new ArrayList<>();

        sessions.add(new ClassSession("S001", "Session 1", "2024-01-15", "08:00 - 10:30",
                "Completed", 23, 25, "Introduction to Algebra"));
        sessions.add(new ClassSession("S002", "Session 2", "2024-01-17", "08:00 - 10:30",
                "Completed", 24, 25, "Linear Equations"));
        sessions.add(new ClassSession("S003", "Session 3", "2024-01-19", "08:00 - 10:30",
                "Cancelled", 0, 25, "Quadratic Functions"));
        sessions.add(new ClassSession("S004", "Session 4", "2024-01-22", "08:00 - 10:30",
                "Completed", 22, 25, "Graphing Functions"));

        return sessions;
    }

    public void onSessionClicked(ClassSession session) {
        // TODO: Handle session click
    }
}
