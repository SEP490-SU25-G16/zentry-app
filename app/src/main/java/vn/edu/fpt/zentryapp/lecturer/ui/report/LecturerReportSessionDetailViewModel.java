package vn.edu.fpt.zentryapp.lecturer.ui.report;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import android.content.Context;
import android.util.Log;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import vn.edu.fpt.zentryapp.auth.client.ApiClient;
import vn.edu.fpt.zentryapp.auth.client.AuthManager;
import vn.edu.fpt.zentryapp.lecturer.data.api.LecturerApiService;
import vn.edu.fpt.zentryapp.lecturer.data.model.response.OverviewSession;
import vn.edu.fpt.zentryapp.lecturer.data.model.response.SessionDetailInfo;
import vn.edu.fpt.zentryapp.lecturer.data.model.response.Student;
import vn.edu.fpt.zentryapp.lecturer.data.model.responsedto.ApiResponseDto;
import vn.edu.fpt.zentryapp.lecturer.data.model.responsedto.SessionAttendanceDataDto;
import vn.edu.fpt.zentryapp.lecturer.data.model.responsedto.SessionInfoDto;
import vn.edu.fpt.zentryapp.lecturer.data.model.responsedto.StudentAttendanceDto;

public class LecturerReportSessionDetailViewModel extends ViewModel {
    private final String TAG = "LecturerReportSessionDetail";
    private final MutableLiveData<Boolean> _isLoading = new MutableLiveData<>(false);
    private final MutableLiveData<SessionDetailInfo> _sessionInfo = new MutableLiveData<>();
    private final MutableLiveData<List<Student>> _students = new MutableLiveData<>();
    private final MutableLiveData<String> _errorMessage = new MutableLiveData<>();
    private final MutableLiveData<Boolean> _attendanceUpdated = new MutableLiveData<>();
    private final MutableLiveData<Boolean> _isUpdatingAttendance = new MutableLiveData<>(false);
    public LiveData<Boolean> isLoading() { return _isLoading; }
    public LiveData<SessionDetailInfo> sessionInfo() { return _sessionInfo; }
    public LiveData<List<Student>> students() { return _students; }
    public LiveData<String> errorMessage() { return _errorMessage; }
    public LiveData<Boolean> attendanceUpdated() { return _attendanceUpdated; }
    public LiveData<Boolean> isUpdatingAttendance() { return _isUpdatingAttendance; }
    // API service
    private LecturerApiService apiService;
    private AuthManager authManager;
    private String sessionId;
    private OverviewSession sessionData; // Data from previous screen

    /* ---------- Init ---------- */
    public void init(Context context, AuthManager authManager, String sessionId) {
        this.authManager = authManager;
        this.sessionId = sessionId;
        this.apiService = ApiClient.getClient(context).create(LecturerApiService.class);

        loadSessionAttendanceDetails();
    }

    public void initWithSessionData(Context context, AuthManager authManager, OverviewSession sessionData) {
        this.authManager = authManager;
        this.sessionData = sessionData;
        this.sessionId = sessionData.getSessionId();
        this.apiService = ApiClient.getClient(context).create(LecturerApiService.class);

        loadSessionAttendanceDetails();
    }

    /* ---------- Load Data from API ---------- */
    private void loadSessionAttendanceDetails() {
        if (sessionId == null || sessionId.isEmpty()) {
            _errorMessage.setValue("Session ID is required");
            return;
        }

        _isLoading.setValue(true);

        Call<ApiResponseDto<SessionAttendanceDataDto>> call = apiService.getSessionAttendanceDetails(sessionId);
        call.enqueue(new Callback<ApiResponseDto<SessionAttendanceDataDto>>() {
            @Override
            public void onResponse(Call<ApiResponseDto<SessionAttendanceDataDto>> call,
                                   Response<ApiResponseDto<SessionAttendanceDataDto>> response) {
                _isLoading.setValue(false);

                if (response.isSuccessful() && response.body() != null) {
                    ApiResponseDto<SessionAttendanceDataDto> apiResponse = response.body();

                    if (apiResponse.isSuccess() && apiResponse.getData() != null) {
                        processSessionAttendanceData(apiResponse.getData());
                    } else {
                        _errorMessage.setValue(apiResponse.getError() != null ?
                                apiResponse.getError() : "Failed to load session details");
                    }
                } else {
                    _errorMessage.setValue("Failed to load data: " + response.message());
                }
            }

            @Override
            public void onFailure(Call<ApiResponseDto<SessionAttendanceDataDto>> call, Throwable t) {
                _isLoading.setValue(false);
                _errorMessage.setValue("Network error: " + t.getMessage());
                Log.e(TAG, "API call failed", t);
            }
        });
    }

    private void processSessionAttendanceData(SessionAttendanceDataDto data) {
        // Process session info
        if (data.getSessionInfo() != null) {
            SessionDetailInfo sessionDetailInfo = mapSessionInfoToSessionDetailInfo(data.getSessionInfo());
            _sessionInfo.setValue(sessionDetailInfo);
        }

        // Process students data
        if (data.getStudents() != null) {
            List<Student> students = mapStudentDtosToStudents(data.getStudents());
            _students.setValue(students);
        }
    }

    private SessionDetailInfo mapSessionInfoToSessionDetailInfo(SessionInfoDto sessionInfoDto) {
        SessionDetailInfo sessionDetailInfo = new SessionDetailInfo();
        sessionDetailInfo.setSessionId(sessionInfoDto.getSessionId());
        sessionDetailInfo.setSessionNumber(sessionInfoDto.getSessionNumber());
        sessionDetailInfo.setSessionTitle(sessionInfoDto.getSessionName());
        sessionDetailInfo.setTotalStudents(sessionInfoDto.getTotalStudents());
        sessionDetailInfo.setPresentStudents(sessionInfoDto.getAttendedCount());
        sessionDetailInfo.setStatus(sessionInfoDto.getStatus());
        sessionDetailInfo.setRoomInfo(sessionInfoDto.getRoomInfo());
        // Parse session date and time
        Date sessionDateTime = parseSessionDateTime(sessionInfoDto.getSessionDate(), sessionInfoDto.getSessionTime());
        sessionDetailInfo.setCreatedTime(sessionDateTime != null ? sessionDateTime.getTime() : System.currentTimeMillis());
        return sessionDetailInfo;
    }

    private List<Student> mapStudentDtosToStudents(List<StudentAttendanceDto> studentDtos) {
        List<Student> students = new ArrayList<>();

        for (StudentAttendanceDto dto : studentDtos) {
            Student student = new Student();
            student.setStudentId(dto.getStudentId());
            student.setStudentCode(dto.getStudentCode());
            student.setFullName(dto.getFullName());
            student.setEmail(dto.getEmail());
            student.setAttendanceStatus(dto.getAttendanceStatus());
            student.setEnrollmentId(dto.getEnrollmentId());
            student.setEnrollmentStatus(dto.getEnrollmentStatus());
            student.setEnrolledAt(dto.getEnrolledAt());

            students.add(student);
        }

        return students;
    }

    private Date parseSessionDateTime(String dateString, String timeString) {
        try {
            SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault());
            return format.parse(dateString + " " + timeString);
        } catch (ParseException e) {
            Log.e(TAG, "Error parsing session date time", e);
            return new Date(); // Return current date as fallback
        }
    }

    /* ---------- Toggle attendance ---------- */
    public void toggleStudentAttendance(Student student) {
        if (sessionId == null || sessionId.isEmpty()) {
            _errorMessage.setValue("Session ID is required");
            return;
        }

        if (student.getStudentId() == null || student.getStudentId().isEmpty()) {
            _errorMessage.setValue("Student ID is required");
            return;
        }

        // ✅ Call API to update attendance
        updateAttendanceOnServer(student);
    }

    private void updateAttendanceOnServer(Student student) {
        _isUpdatingAttendance.setValue(true);
        _errorMessage.setValue(null);

        Log.d(TAG, "🔄 Updating attendance for student: " + student.getFullName() +
                " (ID: " + student.getStudentId() + ") in session: " + sessionId);

        Call<ApiResponseDto<Void>> call = apiService.updateStudentAttendanceStatus(sessionId, student.getStudentId());
        call.enqueue(new Callback<ApiResponseDto<Void>>() {
            @Override
            public void onResponse(Call<ApiResponseDto<Void>> call, Response<ApiResponseDto<Void>> response) {
                _isUpdatingAttendance.setValue(false);

                if (response.isSuccessful() && response.body() != null) {
                    ApiResponseDto<Void> apiResponse = response.body();

                    if (apiResponse.isSuccess()) {
                        // ✅ API call successful, update UI
                        updateStudentAttendanceInUI(student);
                        _attendanceUpdated.setValue(true);

                        Log.d(TAG, "✅ Successfully updated attendance for: " + student.getFullName());
                    } else {
                        String error = apiResponse.getError() != null ? apiResponse.getError() : "Failed to update attendance";
                        _errorMessage.setValue("Update failed: " + error);
                        Log.e(TAG, "❌ API Error: " + error);
                    }
                } else {
                    String error = "HTTP Error: " + response.code() + " - " + response.message();
                    _errorMessage.setValue("Update failed: " + error);
                    Log.e(TAG, "❌ " + error);
                }
            }

            @Override
            public void onFailure(Call<ApiResponseDto<Void>> call, Throwable t) {
                _isUpdatingAttendance.setValue(false);
                String error = "Network Error: " + t.getMessage();
                _errorMessage.setValue("Update failed: " + error);
                Log.e(TAG, "❌ Network Error", t);
            }
        });
    }

    private void updateStudentAttendanceInUI(Student updatedStudent) {
        List<Student> currentStudents = _students.getValue();
        if (currentStudents == null) return;

        // ✅ Find and update the student in the list
        for (Student student : currentStudents) {
            if (student.getStudentId().equals(updatedStudent.getStudentId())) {
                // Toggle the attendance status
                student.setPresent(!student.isPresent());
                break;
            }
        }

        // ✅ Update the LiveData to refresh UI
        _students.setValue(new ArrayList<>(currentStudents));

        // ✅ Update session attendance counts
        updateSessionAttendanceCounts(currentStudents);
    }

    private void updateSessionAttendanceCounts(List<Student> students) {
        SessionDetailInfo currentSessionInfo = _sessionInfo.getValue();
        if (currentSessionInfo == null) return;

        int presentCount = 0;
        for (Student student : students) {
            if (student.isPresent()) {
                presentCount++;
            }
        }

        currentSessionInfo.setPresentStudents(presentCount);
        _sessionInfo.setValue(currentSessionInfo);
    }

    /* ---------- Refresh data ---------- */
    public void refreshData() {
        loadSessionAttendanceDetails();
    }
}
