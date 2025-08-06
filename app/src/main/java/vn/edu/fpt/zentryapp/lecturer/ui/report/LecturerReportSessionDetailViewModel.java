package vn.edu.fpt.zentryapp.lecturer.ui.report;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import android.os.Handler;

import java.util.ArrayList;
import java.util.List;

import vn.edu.fpt.zentryapp.auth.client.AuthManager;
import vn.edu.fpt.zentryapp.lecturer.data.model.response.SessionDetailInfo;
import vn.edu.fpt.zentryapp.lecturer.data.model.response.Student;

public class LecturerReportSessionDetailViewModel extends ViewModel {

    /* ---------- LiveData ---------- */
    private final MutableLiveData<Boolean> _isLoading = new MutableLiveData<>(false);
    private final MutableLiveData<SessionDetailInfo> _sessionInfo = new MutableLiveData<>();
    private final MutableLiveData<List<Student>> _students = new MutableLiveData<>();
    private final MutableLiveData<String> _errorMessage = new MutableLiveData<>();
    private final MutableLiveData<Boolean> _attendanceUpdated = new MutableLiveData<>();

    public LiveData<Boolean> isLoading()          { return _isLoading; }
    public LiveData<SessionDetailInfo> sessionInfo() { return _sessionInfo; }
    public LiveData<List<Student>> students()     { return _students; }
    public LiveData<String> errorMessage()        { return _errorMessage; }
    public LiveData<Boolean> attendanceUpdated()  { return _attendanceUpdated; }

    private String sessionId;
    private int sessionNumber;

    /* ---------- Init ---------- */
    public void init(AuthManager auth, String sessionId,
                     String courseCode, String courseName,
                     String className, int sessionNumber) {
        this.sessionId      = sessionId;
        this.sessionNumber  = sessionNumber;
        /* auth, courseCode … vẫn có thể lưu nếu cần */

        loadSessionDetail();
        loadStudentAttendance();
    }

    /* ---------- Loaders (mock) ---------- */
    private void loadSessionDetail() {
        _isLoading.setValue(true);
        new Handler().postDelayed(() -> {
            SessionDetailInfo info = new SessionDetailInfo();
            info.setSessionId(sessionId);
            info.setSessionNumber(sessionNumber);
            info.setTotalStudents(32);
            info.setPresentStudents(29);
            info.setStatus("COMPLETED");
            info.setCreatedTime(System.currentTimeMillis() - 2 * 60 * 60 * 1_000); // 2h trước
            _sessionInfo.setValue(info);
        }, 400);
    }

    private void loadStudentAttendance() {
        new Handler().postDelayed(() -> {
            _students.setValue(generateMockStudents());
            _isLoading.setValue(false);
        }, 800);
    }

    private List<Student> generateMockStudents() {
        List<Student> list = new ArrayList<>();
        for (int i = 1; i <= 32; i++) {
            Student s = new Student(
                    "ST" + i,
                    "HS" + (1000 + i),
                    "Student " + i,
                    Math.random() < .85
            );
            list.add(s);
        }
        return list;
    }

    /* ---------- Toggle attendance ---------- */
    public void toggleStudentAttendance(Student student) {
        List<Student> cur = _students.getValue();
        if (cur == null) return;

        for (Student s : cur) {
            if (s.getStudentId().equals(student.getStudentId())) {
                s.setPresent(!s.isPresent());
                break;
            }
        }
        _students.setValue(new ArrayList<>(cur));
        _attendanceUpdated.setValue(true);
    }
}

