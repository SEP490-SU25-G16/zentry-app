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
import vn.edu.fpt.zentryapp.lecturer.data.model.response.CourseInfo;
import vn.edu.fpt.zentryapp.lecturer.data.model.response.OverviewSession;
import vn.edu.fpt.zentryapp.lecturer.data.model.responsedto.ApiResponseDto;
import vn.edu.fpt.zentryapp.lecturer.data.model.responsedto.ClassOverviewDto;
import vn.edu.fpt.zentryapp.lecturer.data.model.responsedto.ClassSessionsDataDto;
import vn.edu.fpt.zentryapp.lecturer.data.model.responsedto.SessionDetailDto;

public class LecturerReportListSessionViewModel extends ViewModel {
    private final String TAG = "LecturerReportListSession";

    // LiveData cho UI state
    private final MutableLiveData<Boolean> _isLoading = new MutableLiveData<>(false);
    private final MutableLiveData<CourseInfo> _courseInfo = new MutableLiveData<>();
    private final MutableLiveData<List<OverviewSession>> _sessions = new MutableLiveData<>();
    private final MutableLiveData<String> _errorMessage = new MutableLiveData<>();

    // API service
    private LecturerApiService apiService;
    private AuthManager authManager;
    private String classId;

    // Public getters cho Fragment observe
    public LiveData<Boolean> isLoading() {
        return _isLoading;
    }

    public LiveData<CourseInfo> courseInfo() {
        return _courseInfo;
    }

    public LiveData<List<OverviewSession>> sessions() {
        return _sessions;
    }

    public LiveData<String> errorMessage() {
        return _errorMessage;
    }

    public void init(Context context, AuthManager authManager, String classId) {
        this.authManager = authManager;
        this.classId = classId;
        this.apiService = ApiClient.getClient(context).create(LecturerApiService.class);

        loadClassOverviewAndSessions();
    }

    /**
     * Load class overview and sessions from API
     */
    private void loadClassOverviewAndSessions() {
        if (classId == null || classId.isEmpty()) {
            _errorMessage.setValue("Class ID is required");
            return;
        }

        _isLoading.setValue(true);

        Call<ApiResponseDto<ClassSessionsDataDto>> call = apiService.getClassOverviewSessions(classId);
        call.enqueue(new Callback<ApiResponseDto<ClassSessionsDataDto>>() {
            @Override
            public void onResponse(Call<ApiResponseDto<ClassSessionsDataDto>> call,
                                   Response<ApiResponseDto<ClassSessionsDataDto>> response) {
                _isLoading.setValue(false);

                if (response.isSuccessful() && response.body() != null) {
                    ApiResponseDto<ClassSessionsDataDto> apiResponse = response.body();

                    if (apiResponse.isSuccess() && apiResponse.getData() != null) {
                        processClassSessionsData(apiResponse.getData());
                    } else {
                        _errorMessage.setValue(apiResponse.getError() != null ?
                                apiResponse.getError() : "Failed to load class sessions");
                    }
                } else {
                    _errorMessage.setValue("Failed to load data: " + response.message());
                }
            }

            @Override
            public void onFailure(Call<ApiResponseDto<ClassSessionsDataDto>> call, Throwable t) {
                _isLoading.setValue(false);
                _errorMessage.setValue("Network error: " + t.getMessage());
                Log.e(TAG, "API call failed", t);
            }
        });
    }

    private void processClassSessionsData(ClassSessionsDataDto data) {
        // Process overview data
        if (data.getOverview() != null) {
            CourseInfo courseInfo = mapOverviewToCourseInfo(data.getOverview());
            _courseInfo.setValue(courseInfo);
        }

        // Process sessions data
        if (data.getSessions() != null) {
            List<OverviewSession> sessions = mapSessionsToOverviewSessions(data.getSessions());
            _sessions.setValue(sessions);
        }
    }

    private CourseInfo mapOverviewToCourseInfo(ClassOverviewDto overview) {
        CourseInfo courseInfo = new CourseInfo();
        courseInfo.setCourseCode(overview.getCourseCode());
        courseInfo.setCourseName(overview.getCourseName());
        courseInfo.setClassName(overview.getClassName());
        courseInfo.setTotalStudents(overview.getEnrolledStudents());
        courseInfo.setTotalSessions(overview.getTotalSessions());
        courseInfo.setCompletedSessions(overview.getCompletedSessions());
        courseInfo.setSemester(overview.getSemesterInfo());

        // Set room info (get first room if available)
        if (overview.getRoomInfos() != null && !overview.getRoomInfos().isEmpty()) {
            courseInfo.setRoom(overview.getRoomInfos().get(0));
        }

        courseInfo.setGrade(overview.getSectionCode());

        return courseInfo;
    }

    private List<OverviewSession> mapSessionsToOverviewSessions(List<SessionDetailDto> sessionDtos) {
        List<OverviewSession> sessions = new ArrayList<>();

        for (SessionDetailDto dto : sessionDtos) {
            OverviewSession session = new OverviewSession();
            session.setSessionId(dto.getSessionId());
            session.setSessionNumber(dto.getSessionNumber());
            session.setTotalStudents(dto.getTotalStudents());
            session.setPresentStudents(dto.getAttendedCount());

            // Parse date
            Date sessionDate = parseDate(dto.getSessionDate());
            session.setDate(sessionDate);

            // Set session title
            session.setSessionTitle(dto.getSessionName());

            // Set time info
            session.setStartTime(dto.getSessionTime());
            session.setEndTime(dto.getEndTime());

            // Set room info
            session.setRoomInfo(dto.getRoomInfo());

            // Set status
            session.setStatus(dto.getStatus());

            sessions.add(session);
        }

        return sessions;
    }

    private Date parseDate(String dateString) {
        try {
            SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
            return format.parse(dateString);
        } catch (ParseException e) {
            Log.e(TAG, "Error parsing date: " + dateString, e);
            return new Date(); // Return current date as fallback
        }
    }

    /**
     * Refresh data
     */
    public void refreshData() {
        loadClassOverviewAndSessions();
    }
}
