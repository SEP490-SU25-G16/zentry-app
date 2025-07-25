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

        // Week 1
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

        // Week 2
        sessions.add(new Session(
                "S006",
                "Session - 6",
                "08/01/2025",
                "Attended",
                courseId,
                "Mathematics",
                "Polynomial Functions"
        ));

        sessions.add(new Session(
                "S007",
                "Session - 7",
                "09/01/2025",
                "Attended",
                courseId,
                "Mathematics",
                "Factoring Polynomials"
        ));

        sessions.add(new Session(
                "S008",
                "Session - 8",
                "10/01/2025",
                "Late",
                courseId,
                "Mathematics",
                "Rational Functions"
        ));

        sessions.add(new Session(
                "S009",
                "Session - 9",
                "11/01/2025",
                "Absent",
                courseId,
                "Mathematics",
                "Exponential Functions"
        ));

        sessions.add(new Session(
                "S010",
                "Session - 10",
                "12/01/2025",
                "Attended",
                courseId,
                "Mathematics",
                "Logarithmic Functions"
        ));

        // Week 3
        sessions.add(new Session(
                "S011",
                "Session - 11",
                "15/01/2025",
                "Attended",
                courseId,
                "Mathematics",
                "Trigonometric Functions"
        ));

        sessions.add(new Session(
                "S012",
                "Session - 12",
                "16/01/2025",
                "Attended",
                courseId,
                "Mathematics",
                "Unit Circle and Angles"
        ));

        sessions.add(new Session(
                "S013",
                "Session - 13",
                "17/01/2025",
                "Late",
                courseId,
                "Mathematics",
                "Trigonometric Identities"
        ));

        sessions.add(new Session(
                "S014",
                "Session - 14",
                "18/01/2025",
                "Attended",
                courseId,
                "Mathematics",
                "Inverse Trigonometric Functions"
        ));

        sessions.add(new Session(
                "S015",
                "Session - 15",
                "19/01/2025",
                "Absent",
                courseId,
                "Mathematics",
                "Sequences and Series"
        ));

        // Week 4
        sessions.add(new Session(
                "S016",
                "Session - 16",
                "22/01/2025",
                "Attended",
                courseId,
                "Mathematics",
                "Arithmetic Sequences"
        ));

        sessions.add(new Session(
                "S017",
                "Session - 17",
                "23/01/2025",
                "Attended",
                courseId,
                "Mathematics",
                "Geometric Sequences"
        ));

        sessions.add(new Session(
                "S018",
                "Session - 18",
                "24/01/2025",
                "Attended",
                courseId,
                "Mathematics",
                "Limits and Continuity"
        ));

        sessions.add(new Session(
                "S019",
                "Session - 19",
                "25/01/2025",
                "Late",
                courseId,
                "Mathematics",
                "Introduction to Derivatives"
        ));

        sessions.add(new Session(
                "S020",
                "Session - 20",
                "26/01/2025",
                "Attended",
                courseId,
                "Mathematics",
                "Derivative Rules"
        ));

        // Week 5
        sessions.add(new Session(
                "S021",
                "Session - 21",
                "29/01/2025",
                "Attended",
                courseId,
                "Mathematics",
                "Chain Rule"
        ));

        sessions.add(new Session(
                "S022",
                "Session - 22",
                "30/01/2025",
                "Absent",
                courseId,
                "Mathematics",
                "Applications of Derivatives"
        ));

        sessions.add(new Session(
                "S023",
                "Session - 23",
                "31/01/2025",
                "Attended",
                courseId,
                "Mathematics",
                "Optimization Problems"
        ));

        sessions.add(new Session(
                "S024",
                "Session - 24",
                "01/02/2025",
                "Attended",
                courseId,
                "Mathematics",
                "Related Rates"
        ));

        sessions.add(new Session(
                "S025",
                "Session - 25",
                "02/02/2025",
                "Late",
                courseId,
                "Mathematics",
                "Introduction to Integrals"
        ));

        // Week 6
        sessions.add(new Session(
                "S026",
                "Session - 26",
                "05/02/2025",
                "Attended",
                courseId,
                "Mathematics",
                "Fundamental Theorem of Calculus"
        ));

        sessions.add(new Session(
                "S027",
                "Session - 27",
                "06/02/2025",
                "Attended",
                courseId,
                "Mathematics",
                "Integration Techniques"
        ));

        sessions.add(new Session(
                "S028",
                "Session - 28",
                "07/02/2025",
                "Attended",
                courseId,
                "Mathematics",
                "Applications of Integrals"
        ));

        sessions.add(new Session(
                "S029",
                "Session - 29",
                "08/02/2025",
                "Absent",
                courseId,
                "Mathematics",
                "Area and Volume Calculations"
        ));

        sessions.add(new Session(
                "S030",
                "Session - 30",
                "09/02/2025",
                "Attended",
                courseId,
                "Mathematics",
                "Final Review and Assessment"
        ));

        return sessions;
    }

    public void onSessionClicked(Session session) {
        // TODO: Handle session click for navigation
    }
}
