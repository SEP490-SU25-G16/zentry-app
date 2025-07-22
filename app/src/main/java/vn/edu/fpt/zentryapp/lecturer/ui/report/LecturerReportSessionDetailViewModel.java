package vn.edu.fpt.zentryapp.lecturer.ui.report;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import android.os.Handler;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Random;

import lombok.Getter;
import vn.edu.fpt.zentryapp.auth.client.AuthManager;
import vn.edu.fpt.zentryapp.lecturer.data.model.response.SessionDetailInfo;
import vn.edu.fpt.zentryapp.lecturer.data.model.response.Student;

public class LecturerReportSessionDetailViewModel extends ViewModel {

    // LiveData cho UI state
    private final MutableLiveData<Boolean> _isLoading = new MutableLiveData<>(false);
    private final MutableLiveData<SessionDetailInfo> _sessionInfo = new MutableLiveData<>();
    private final MutableLiveData<List<Student>> _students = new MutableLiveData<>();
    private final MutableLiveData<String> _errorMessage = new MutableLiveData<>();
    private final MutableLiveData<Boolean> _attendanceUpdated = new MutableLiveData<>();

    // Public getters cho Fragment observe
    public LiveData<Boolean> isLoading() {
        return _isLoading;
    }

    public LiveData<SessionDetailInfo> sessionInfo() {
        return _sessionInfo;
    }

    public LiveData<List<Student>> students() {
        return _students;
    }

    public LiveData<String> errorMessage() {
        return _errorMessage;
    }

    public LiveData<Boolean> attendanceUpdated() {
        return _attendanceUpdated;
    }

    private AuthManager authManager;
    private String sessionId;
    private String courseCode;
    private String courseName;
    private String className;
    private int sessionNumber;

    public void init(AuthManager authManager, String sessionId, String courseCode,
                     String courseName, String className, int sessionNumber) {
        this.authManager = authManager;
        this.sessionId = sessionId;
        this.courseCode = courseCode;
        this.courseName = courseName;
        this.className = className;
        this.sessionNumber = sessionNumber;

        loadSessionDetail();
        loadStudentAttendance();
    }

    /**
     * Load session detail information
     */
    private void loadSessionDetail() {
        _isLoading.setValue(true);

        // Simulate network delay
        new Handler().postDelayed(() -> {
            SessionDetailInfo sessionInfo = generateSessionInfo();
            _sessionInfo.setValue(sessionInfo);
        }, 500);
    }

    /**
     * Load student attendance list
     */
    private void loadStudentAttendance() {
        // Simulate network delay
        new Handler().postDelayed(() -> {
            List<Student> students = generateMockStudents();
            _students.setValue(students);
            _isLoading.setValue(false);
        }, 1000);
    }

    /**
     * Toggle student attendance status
     */
    public void toggleStudentAttendance(Student student) {
        List<Student> currentStudents = _students.getValue();
        if (currentStudents == null) return;

        // Find and update student
        for (Student s : currentStudents) {
            if (s.getStudentId().equals(student.getStudentId())) {
                s.setPresent(!s.isPresent());
                s.setLastModifiedTime(System.currentTimeMillis());
                break;
            }
        }

        // Update session info attendance count
        updateAttendanceCount(currentStudents);

        // Notify UI
        _students.setValue(new ArrayList<>(currentStudents));
        _attendanceUpdated.setValue(true);

        // TODO: Call API to save attendance changes
        // saveAttendanceToServer(student);
    }

    /**
     * Update attendance count in session info
     */
    private void updateAttendanceCount(List<Student> students) {
        SessionDetailInfo sessionInfo = _sessionInfo.getValue();
        if (sessionInfo == null) return;

        int presentCount = 0;
        for (Student student : students) {
            if (student.isPresent()) {
                presentCount++;
            }
        }

        sessionInfo.setPresentStudents(presentCount);
        _sessionInfo.setValue(sessionInfo);
    }

    /**
     * Generate mock session information
     */
    private SessionDetailInfo generateSessionInfo() {
        SessionDetailInfo sessionInfo = new SessionDetailInfo();
        sessionInfo.setSessionId(sessionId);
        sessionInfo.setCourseCode(courseCode);
        sessionInfo.setCourseName(courseName);
        sessionInfo.setClassName(className);
        sessionInfo.setRoom("DE-201");
        sessionInfo.setGrade("07");
        sessionInfo.setSessionNumber(sessionNumber);
        sessionInfo.setStartTime("08:00");
        sessionInfo.setEndTime("09:30");
        sessionInfo.setTotalStudents(32);
        sessionInfo.setPresentStudents(29);
        sessionInfo.setStatus("COMPLETED");

        // Set session date (for 24h edit logic)
        Calendar calendar = Calendar.getInstance();
        if (sessionNumber <= 12) {
            // Past sessions - some within 24h, some beyond
            calendar.add(Calendar.HOUR, -(sessionNumber * 2)); // Each session 2 hours ago
        } else {
            // Future sessions
            calendar.add(Calendar.DAY_OF_MONTH, sessionNumber - 12);
        }
        sessionInfo.setSessionDate(calendar.getTime());
        sessionInfo.setCreatedTime(calendar.getTimeInMillis());

        return sessionInfo;
    }

    /**
     * Generate mock student list with attendance
     */
    private List<Student> generateMockStudents() {
        List<Student> students = new ArrayList<>();
        Random random = new Random();

        String[] firstNames = {"Tharidu", "Hasitha", "Saman", "Kasun", "Nuwan", "Chamika",
                "Dilshan", "Mahinda", "Pradeep", "Sunil", "Gayan", "Ruwan",
                "Kamal", "Nimal", "Amal", "Vimal", "Sumal", "Kemal"};
        String[] lastNames = {"Silva", "Perera", "Fernando", "Jayawardena", "Gunasekara",
                "Wickramasinghe", "Rajapaksa", "Mendis", "De Silva", "Amarasinghe"};

        for (int i = 1; i <= 32; i++) {
            Student student = new Student();
            student.setStudentId("ST" + String.format("%03d", i));
            student.setStudentCode("HS" + String.format("%06d", 1000 + i));

            // Generate random name
            String firstName = firstNames[random.nextInt(firstNames.length)];
            String lastName = lastNames[random.nextInt(lastNames.length)];
            student.setFullName(firstName + " " + lastName);

            student.setEmail(student.getStudentCode().toLowerCase() + "@student.edu");

            // Random attendance (85% attendance rate)
            student.setPresent(random.nextDouble() < 0.85);

            student.setLastModifiedTime(System.currentTimeMillis());
            students.add(student);
        }

        return students;
    }

    /**
     * Refresh session data
     */
    public void refreshSession() {
        loadSessionDetail();
        loadStudentAttendance();
    }
}
