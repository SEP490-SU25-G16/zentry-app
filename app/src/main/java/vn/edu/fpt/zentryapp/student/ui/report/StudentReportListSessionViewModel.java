package vn.edu.fpt.zentryapp.student.ui.report;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import android.os.Handler;

import java.util.ArrayList;
import java.util.List;

import vn.edu.fpt.zentryapp.auth.client.AuthManager;
import vn.edu.fpt.zentryapp.student.data.model.response.CourseInfo;
import vn.edu.fpt.zentryapp.student.data.model.response.StudentSession;

public class StudentReportListSessionViewModel extends ViewModel {

    private final MutableLiveData<Boolean> _isLoading = new MutableLiveData<>(false);
    private final MutableLiveData<List<StudentSession>> _sessions = new MutableLiveData<>();
    private final MutableLiveData<CourseInfo> _courseInfo = new MutableLiveData<>();
    private final MutableLiveData<String> _errorMessage = new MutableLiveData<>();
    private final MutableLiveData<String> _successMessage = new MutableLiveData<>();

    public LiveData<Boolean> isLoading() { return _isLoading; }
    public LiveData<List<StudentSession>> sessions() { return _sessions; }
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
                List<StudentSession> mockStudentSessions = generateMockSessions(courseId);
                _sessions.setValue(mockStudentSessions);
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

    private List<StudentSession> generateMockSessions(String courseId) {
        List<StudentSession> studentSessions = new ArrayList<>();

        // Week 1
        studentSessions.add(new StudentSession(
                "S001",
                "Session - 1",
                "01/01/2025",
                "Attended",
                courseId,
                "Mathematics",
                "Introduction to Algebra"
        ));

        studentSessions.add(new StudentSession(
                "S002",
                "Session - 2",
                "02/01/2025",
                "Absent",
                courseId,
                "Mathematics",
                "Linear Equations"
        ));

        studentSessions.add(new StudentSession(
                "S003",
                "Session - 3",
                "03/01/2025",
                "Attended",
                courseId,
                "Mathematics",
                "Quadratic Functions"
        ));

        studentSessions.add(new StudentSession(
                "S004",
                "Session - 4",
                "04/01/2025",
                "Late",
                courseId,
                "Mathematics",
                "Graphing Functions"
        ));

        studentSessions.add(new StudentSession(
                "S005",
                "Session - 5",
                "05/01/2025",
                "Attended",
                courseId,
                "Mathematics",
                "Systems of Equations"
        ));

        // Week 2
        studentSessions.add(new StudentSession(
                "S006",
                "Session - 6",
                "08/01/2025",
                "Attended",
                courseId,
                "Mathematics",
                "Polynomial Functions"
        ));

        studentSessions.add(new StudentSession(
                "S007",
                "Session - 7",
                "09/01/2025",
                "Attended",
                courseId,
                "Mathematics",
                "Factoring Polynomials"
        ));

        studentSessions.add(new StudentSession(
                "S008",
                "Session - 8",
                "10/01/2025",
                "Late",
                courseId,
                "Mathematics",
                "Rational Functions"
        ));

        studentSessions.add(new StudentSession(
                "S009",
                "Session - 9",
                "11/01/2025",
                "Absent",
                courseId,
                "Mathematics",
                "Exponential Functions"
        ));

        studentSessions.add(new StudentSession(
                "S010",
                "Session - 10",
                "12/01/2025",
                "Attended",
                courseId,
                "Mathematics",
                "Logarithmic Functions"
        ));

        // Week 3
        studentSessions.add(new StudentSession(
                "S011",
                "Session - 11",
                "15/01/2025",
                "Attended",
                courseId,
                "Mathematics",
                "Trigonometric Functions"
        ));

        studentSessions.add(new StudentSession(
                "S012",
                "Session - 12",
                "16/01/2025",
                "Attended",
                courseId,
                "Mathematics",
                "Unit Circle and Angles"
        ));

        studentSessions.add(new StudentSession(
                "S013",
                "Session - 13",
                "17/01/2025",
                "Late",
                courseId,
                "Mathematics",
                "Trigonometric Identities"
        ));

        studentSessions.add(new StudentSession(
                "S014",
                "Session - 14",
                "18/01/2025",
                "Attended",
                courseId,
                "Mathematics",
                "Inverse Trigonometric Functions"
        ));

        studentSessions.add(new StudentSession(
                "S015",
                "Session - 15",
                "19/01/2025",
                "Absent",
                courseId,
                "Mathematics",
                "Sequences and Series"
        ));

        // Week 4
        studentSessions.add(new StudentSession(
                "S016",
                "Session - 16",
                "22/01/2025",
                "Attended",
                courseId,
                "Mathematics",
                "Arithmetic Sequences"
        ));

        studentSessions.add(new StudentSession(
                "S017",
                "Session - 17",
                "23/01/2025",
                "Attended",
                courseId,
                "Mathematics",
                "Geometric Sequences"
        ));

        studentSessions.add(new StudentSession(
                "S018",
                "Session - 18",
                "24/01/2025",
                "Attended",
                courseId,
                "Mathematics",
                "Limits and Continuity"
        ));

        studentSessions.add(new StudentSession(
                "S019",
                "Session - 19",
                "25/01/2025",
                "Late",
                courseId,
                "Mathematics",
                "Introduction to Derivatives"
        ));

        studentSessions.add(new StudentSession(
                "S020",
                "Session - 20",
                "26/01/2025",
                "Attended",
                courseId,
                "Mathematics",
                "Derivative Rules"
        ));

        // Week 5
        studentSessions.add(new StudentSession(
                "S021",
                "Session - 21",
                "29/01/2025",
                "Attended",
                courseId,
                "Mathematics",
                "Chain Rule"
        ));

        studentSessions.add(new StudentSession(
                "S022",
                "Session - 22",
                "30/01/2025",
                "Absent",
                courseId,
                "Mathematics",
                "Applications of Derivatives"
        ));

        studentSessions.add(new StudentSession(
                "S023",
                "Session - 23",
                "31/01/2025",
                "Attended",
                courseId,
                "Mathematics",
                "Optimization Problems"
        ));

        studentSessions.add(new StudentSession(
                "S024",
                "Session - 24",
                "01/02/2025",
                "Attended",
                courseId,
                "Mathematics",
                "Related Rates"
        ));

        studentSessions.add(new StudentSession(
                "S025",
                "Session - 25",
                "02/02/2025",
                "Late",
                courseId,
                "Mathematics",
                "Introduction to Integrals"
        ));

        // Week 6
        studentSessions.add(new StudentSession(
                "S026",
                "Session - 26",
                "05/02/2025",
                "Attended",
                courseId,
                "Mathematics",
                "Fundamental Theorem of Calculus"
        ));

        studentSessions.add(new StudentSession(
                "S027",
                "Session - 27",
                "06/02/2025",
                "Attended",
                courseId,
                "Mathematics",
                "Integration Techniques"
        ));

        studentSessions.add(new StudentSession(
                "S028",
                "Session - 28",
                "07/02/2025",
                "Attended",
                courseId,
                "Mathematics",
                "Applications of Integrals"
        ));

        studentSessions.add(new StudentSession(
                "S029",
                "Session - 29",
                "08/02/2025",
                "Absent",
                courseId,
                "Mathematics",
                "Area and Volume Calculations"
        ));

        studentSessions.add(new StudentSession(
                "S030",
                "Session - 30",
                "09/02/2025",
                "Attended",
                courseId,
                "Mathematics",
                "Final Review and Assessment"
        ));

        return studentSessions;
    }

    public void onSessionClicked(StudentSession studentSession) {
        // TODO: Handle session click for navigation
    }
}
