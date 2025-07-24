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

        // Các session đã tham gia
        sessions.add(new ClassSession("R001", "Round 1: Introduction to Algebra", "8:50",
                "Attended"));
        sessions.add(new ClassSession("R002", "Round 2: Linear Equations", "9:45",
                "Attended"));
        sessions.add(new ClassSession("R003", "Round 3: Quadratic Functions", "10:40",
                "Attended"));
        sessions.add(new ClassSession("R004", "Round 4: Graph Analysis", "11:35",
                "Attended"));
        sessions.add(new ClassSession("R005", "Round 5: Problem Solving", "13:30",
                "Attended"));

        // Các session vắng mặt
        sessions.add(new ClassSession("R006", "Round 6: Advanced Topics", "14:25",
                "Absent"));
        sessions.add(new ClassSession("R007", "Round 7: Review Session", "15:20",
                "Absent"));

        // Các session sắp tới
        sessions.add(new ClassSession("R008", "Round 8: Practice Test", "8:50",
                "Upcoming"));
        sessions.add(new ClassSession("R009", "Round 9: Final Review", "9:45",
                "Upcoming"));
        sessions.add(new ClassSession("R010", "Round 10: Final Exam", "10:40",
                "Upcoming"));

        // Thêm các session khác với thời gian và trạng thái đa dạng
        sessions.add(new ClassSession("R011", "Round 11: Geometry Basics", "8:00",
                "Attended"));
        sessions.add(new ClassSession("R012", "Round 12: Trigonometry", "9:30",
                "Late"));
        sessions.add(new ClassSession("R013", "Round 13: Statistics", "11:00",
                "Attended"));
        sessions.add(new ClassSession("R014", "Round 14: Probability", "13:00",
                "Cancelled"));
        sessions.add(new ClassSession("R015", "Round 15: Mock Exam", "14:30",
                "Upcoming"));

        return sessions;
    }


    public void onSessionClicked(ClassSession session) {
        // TODO: Handle session click
    }
}
