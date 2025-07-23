package vn.edu.fpt.zentryapp.student.ui.report;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import android.os.Handler;

import java.util.ArrayList;
import java.util.List;

import vn.edu.fpt.zentryapp.auth.client.AuthManager;
import vn.edu.fpt.zentryapp.student.data.model.response.CourseInfo;
import vn.edu.fpt.zentryapp.student.data.model.response.Session;

public class StudentReportListSessionViewModel extends ViewModel {

    private final MutableLiveData<Boolean> _isLoading = new MutableLiveData<>(false);
    private final MutableLiveData<List<Session>> _sessions = new MutableLiveData<>();
    private final MutableLiveData<CourseInfo> _courseInfo = new MutableLiveData<>();
    private final MutableLiveData<String> _errorMessage = new MutableLiveData<>();
    private final MutableLiveData<String> _successMessage = new MutableLiveData<>();

    public LiveData<Boolean> isLoading() { return _isLoading; }
    public LiveData<List<Session>> sessions() { return _sessions; }
    public LiveData<CourseInfo> courseInfo() { return _courseInfo; }
    public LiveData<String> errorMessage() { return _errorMessage; }
    public LiveData<String> successMessage() { return _successMessage; }

    private AuthManager authManager;

    public void init(AuthManager authManager, String courseId) {
        this.authManager = authManager;
        loadCourseInfo(courseId);
        loadSessions(courseId);
    }

    public void loadSessions(String courseId) {
        if (Boolean.TRUE.equals(_isLoading.getValue())) {
            return;
        }

        _isLoading.setValue(true);
        _errorMessage.setValue(null);

        new Handler().postDelayed(() -> {
            try {
                List<Session> mockSessions = generateMockSessions(courseId);
                _sessions.setValue(mockSessions);
                _successMessage.setValue("Sessions loaded successfully");
            } catch (Exception e) {
                _errorMessage.setValue("Failed to load sessions: " + e.getMessage());
            } finally {
                _isLoading.setValue(false);
            }
        }, 1000);
    }

    private void loadCourseInfo(String courseId) {
        // Mock course info
        CourseInfo mockCourseInfo = new CourseInfo(
                courseId,
                "Mathematics",
                "Grade 07",
                12,
                20
        );
        _courseInfo.setValue(mockCourseInfo);
    }

    private List<Session> generateMockSessions(String courseId) {
        List<Session> sessions = new ArrayList<>();

        sessions.add(new Session(
                "S001",
                "Session - 1",
                "01/01/2025",
                "Attended",
                courseId,
                "Mathematics",
                "Introduction to Algebra"
        ));

        sessions.add(new Session(
                "S002",
                "Session - 2",
                "02/01/2025",
                "Absent",
                courseId,
                "Mathematics",
                "Linear Equations"
        ));

        sessions.add(new Session(
                "S003",
                "Session - 3",
                "03/01/2025",
                "Attended",
                courseId,
                "Mathematics",
                "Quadratic Functions"
        ));

        sessions.add(new Session(
                "S004",
                "Session - 4",
                "04/01/2025",
                "Late",
                courseId,
                "Mathematics",
                "Graphing Functions"
        ));

        sessions.add(new Session(
                "S005",
                "Session - 5",
                "05/01/2025",
                "Attended",
                courseId,
                "Mathematics",
                "Systems of Equations"
        ));

        return sessions;
    }

    public void onSessionClicked(Session session) {
        // TODO: Handle session click for navigation
    }
}
