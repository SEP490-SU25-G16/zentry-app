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

    public LiveData<Boolean> isLoading() {
        return _isLoading;
    }

    public LiveData<List<ClassSession>> sessions() {
        return _sessions;
    }

    public LiveData<String> errorMessage() {
        return _errorMessage;
    }

    public LiveData<String> successMessage() {
        return _successMessage;
    }

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

        sessions.add(new ClassSession("R001", "Round 1", "8:50",
                "Attended"));
        sessions.add(new ClassSession("R002", "Round 2", "8:50",
                "Attended"));
        sessions.add(new ClassSession("R003", "Round 3", "8:50",
                "Attended"));
        sessions.add(new ClassSession("R004", "Round 4", "8:50",
                "Attended"));

        return sessions;
    }

    public void onSessionClicked(ClassSession session) {
        // TODO: Handle session click
    }
}
