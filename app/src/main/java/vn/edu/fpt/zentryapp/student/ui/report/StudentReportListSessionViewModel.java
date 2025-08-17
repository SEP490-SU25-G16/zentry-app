package vn.edu.fpt.zentryapp.student.ui.report;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import android.content.Context;
import android.util.Log;

import java.util.ArrayList;
import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import vn.edu.fpt.zentryapp.auth.client.ApiClient;
import vn.edu.fpt.zentryapp.auth.client.AuthManager;
import vn.edu.fpt.zentryapp.lecturer.data.model.responsedto.ApiResponseDto;
import vn.edu.fpt.zentryapp.student.data.api.StudentApiService;
import vn.edu.fpt.zentryapp.student.data.model.response.CourseInfo;
import vn.edu.fpt.zentryapp.student.data.model.response.StudentReport;
import vn.edu.fpt.zentryapp.student.data.model.response.StudentSession;
import vn.edu.fpt.zentryapp.student.data.model.responsedto.StudentSessionDto;
import vn.edu.fpt.zentryapp.student.data.model.responsedto.StudentSessionsDataDto;

public class StudentReportListSessionViewModel extends ViewModel {
    private final String TAG = "StudentReportListSession";

    private final MutableLiveData<Boolean> _isLoading = new MutableLiveData<>(false);
    private final MutableLiveData<List<StudentSession>> _sessions = new MutableLiveData<>();
    private final MutableLiveData<CourseInfo> _courseInfo = new MutableLiveData<>();
    private final MutableLiveData<String> _errorMessage = new MutableLiveData<>();
    private final MutableLiveData<String> _successMessage = new MutableLiveData<>();

    // API service
    private StudentApiService apiService;
    private AuthManager authManager;
    private StudentReport studentReport; // Store the passed report data

    public LiveData<Boolean> isLoading() { return _isLoading; }
    public LiveData<List<StudentSession>> sessions() { return _sessions; }
    public LiveData<CourseInfo> courseInfo() { return _courseInfo; }
    public LiveData<String> errorMessage() { return _errorMessage; }
    public LiveData<String> successMessage() { return _successMessage; }

    public void init(Context context, AuthManager authManager, StudentReport studentReport) {
        this.authManager = authManager;
        this.studentReport = studentReport;
        this.apiService = ApiClient.getClient(context).create(StudentApiService.class);

        // Set course info from passed StudentReport
        setCourseInfoFromReport(studentReport);

        // Load sessions from API
        loadStudentSessions();
    }

    private void setCourseInfoFromReport(StudentReport report) {
        CourseInfo courseInfo = new CourseInfo();
        courseInfo.setCourseId(report.getClassId());
        courseInfo.setCourseName(report.getCourseName());
        courseInfo.setCourseCode(report.getCourseCode());
        courseInfo.setSectionCode(report.getSectionCode());
        courseInfo.setGrade(report.getSectionCode()); // Use section code as grade
        courseInfo.setAttendanceRate(report.getAttendanceRate());
        _courseInfo.setValue(courseInfo);
    }

    public void loadStudentSessions() {
        if (Boolean.TRUE.equals(_isLoading.getValue())) {
            return;
        }

        _isLoading.setValue(true);
        _errorMessage.setValue(null);

        String studentId = authManager.getCurrentUserId();

        Call<ApiResponseDto<StudentSessionsDataDto>> call = apiService.getStudentSessions(studentId);
        call.enqueue(new Callback<ApiResponseDto<StudentSessionsDataDto>>() {
            @Override
            public void onResponse(Call<ApiResponseDto<StudentSessionsDataDto>> call,
                                   Response<ApiResponseDto<StudentSessionsDataDto>> response) {
                _isLoading.setValue(false);

                if (response.isSuccessful() && response.body() != null) {
                    ApiResponseDto<StudentSessionsDataDto> apiResponse = response.body();

                    if (apiResponse.isSuccess() && apiResponse.getData() != null) {
                        processStudentSessions(apiResponse.getData());
                        _successMessage.setValue("Sessions loaded successfully");
                    } else {
                        _errorMessage.setValue(apiResponse.getError() != null ?
                                apiResponse.getError() : "Failed to load sessions");
                    }
                } else {
                    _errorMessage.setValue("Failed to load data: " + response.message());
                }
            }

            @Override
            public void onFailure(Call<ApiResponseDto<StudentSessionsDataDto>> call, Throwable t) {
                _isLoading.setValue(false);
                _errorMessage.setValue("Network error: " + t.getMessage());
                Log.e(TAG, "API call failed", t);
            }
        });
    }

    private void processStudentSessions(StudentSessionsDataDto sessionsData) {
        List<StudentSession> sessions = mapSessionDtosToStudentSessions(sessionsData.getSessions());
        _sessions.setValue(sessions);

        // Update course info with actual session counts
        updateCourseInfoWithSessionCounts(sessions);
    }

    private List<StudentSession> mapSessionDtosToStudentSessions(List<StudentSessionDto> sessionDtos) {
        List<StudentSession> sessions = new ArrayList<>();

        if (sessionDtos == null) return sessions;

        for (StudentSessionDto dto : sessionDtos) {
            StudentSession session = new StudentSession();
            session.setSessionId(dto.getSessionId());
            session.setSessionNumber(dto.getSessionNumber());
            session.setSessionName(dto.getSessionName());
            session.setSessionDate(dto.getSessionDate());
            session.setStartTime(dto.getStartTime());
            session.setEndTime(dto.getEndTime());
            session.setRoomInfo(dto.getRoomInfo());
            session.setAttendanceStatus(dto.getAttendanceStatus());

            // Set course info from studentReport if available
            if (studentReport != null) {
                session.setCourseId(studentReport.getClassId());
                session.setCourseName(studentReport.getCourseName());
            }

            sessions.add(session);
        }

        return sessions;
    }

    private void updateCourseInfoWithSessionCounts(List<StudentSession> sessions) {
        CourseInfo currentCourseInfo = _courseInfo.getValue();
        if (currentCourseInfo == null) return;

        int totalSessions = sessions.size();
        int attendedSessions = 0;

        for (StudentSession session : sessions) {
            if (session.isPresent()) {
                attendedSessions++;
            }
        }

        double attendanceRate = totalSessions > 0 ? (double) attendedSessions / totalSessions * 100 : 0;

        currentCourseInfo.setTotalSessions(totalSessions);
        currentCourseInfo.setAttendedSessions(attendedSessions);
        currentCourseInfo.setAttendanceRate(attendanceRate);

        _courseInfo.setValue(currentCourseInfo);
    }

    public void refreshData() {
        loadStudentSessions();
    }

    public void onSessionClicked(StudentSession studentSession) {
        Log.d(TAG, "Session clicked: " + studentSession.getSessionTitle());
        // Handle session click if needed - sessions are view-only for students
    }
}
