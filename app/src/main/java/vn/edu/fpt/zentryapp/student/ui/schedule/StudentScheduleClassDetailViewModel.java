package vn.edu.fpt.zentryapp.student.ui.schedule;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import android.content.Context;
import android.util.Log;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import vn.edu.fpt.zentryapp.auth.client.ApiClient;
import vn.edu.fpt.zentryapp.auth.client.AuthManager;
import vn.edu.fpt.zentryapp.service.AttendanceApiService;
import vn.edu.fpt.zentryapp.student.data.model.response.ScheduleDetailDto;
import vn.edu.fpt.zentryapp.student.data.model.response.ScheduleDetailResponse;
import vn.edu.fpt.zentryapp.student.data.model.response.StudentScheduleClassSection;
import vn.edu.fpt.zentryapp.student.data.model.response.StudentFinalAttendanceDto;
import vn.edu.fpt.zentryapp.student.data.model.response.StudentFinalAttendanceResponse;
import vn.edu.fpt.zentryapp.student.data.model.response.ClassSectionDetailDto;
import vn.edu.fpt.zentryapp.student.data.model.response.ClassSectionDetailResponse;

public class StudentScheduleClassDetailViewModel extends ViewModel {
    private static final String TAG = "StudentClassDetailVM";

    private final MutableLiveData<Boolean> _isLoading = new MutableLiveData<>(false);
    private final MutableLiveData<StudentFinalAttendanceDto> _studentFinalAttendance = new MutableLiveData<>();
    private final MutableLiveData<ScheduleDetailDto> _classSectionDetail = new MutableLiveData<>();
    private final MutableLiveData<String> _errorMessage = new MutableLiveData<>();

    // API Service
    private AttendanceApiService apiService;
    private AuthManager authManager;
    private StudentScheduleClassSection session;

    // Public getters
    public LiveData<Boolean> isLoading() { return _isLoading; }
    public LiveData<StudentFinalAttendanceDto> studentFinalAttendance() { return _studentFinalAttendance; }
    public LiveData<ScheduleDetailDto> classSectionDetail() { return _classSectionDetail; }
    public LiveData<String> errorMessage() { return _errorMessage; }

    public void init(Context context, AuthManager authManager, StudentScheduleClassSection session) {
        this.authManager = authManager;
        this.session = session;
        this.apiService = ApiClient.getClient(context).create(AttendanceApiService.class);

        // Load both class section detail and student final attendance
        loadClassSectionDetail();
        loadStudentFinalAttendance();
    }

    /**
     * Load class section detail from API
     */
    private void loadClassSectionDetail() {
        if (session == null) {
            _errorMessage.setValue("Session data not available");
            return;
        }

        String scheduleId = getScheduleId();
        if (scheduleId == null) {
            _errorMessage.setValue("Schedule ID not available");
            return;
        }

        Log.d(TAG, "Loading schedule detail for: " + scheduleId);

        // ✅ FIXED: Sử dụng getScheduleDetail API
        apiService.getScheduleDetail(scheduleId)
                .enqueue(new Callback<ScheduleDetailResponse>() {
                    @Override
                    public void onResponse(Call<ScheduleDetailResponse> call,
                                           Response<ScheduleDetailResponse> response) {
                        if (response.isSuccessful() && response.body() != null) {
                            ScheduleDetailResponse apiResponse = response.body();

                            if (apiResponse.isSuccess()) {
                                // ✅ FIXED: Set schedule detail data
                                _classSectionDetail.setValue(apiResponse.getData());
                                Log.d(TAG, "✅ Loaded schedule detail successfully");
                            } else {
                                String error = apiResponse.getError() != null ? apiResponse.getError() : "Unknown API error";
                                _errorMessage.setValue("Schedule Detail: " + error);
                                Log.e(TAG, "❌ Schedule Detail API Error: " + error);
                            }
                        } else {
                            String error = "HTTP Error: " + response.code();
                            _errorMessage.setValue("Schedule Detail: " + error);
                            Log.e(TAG, "❌ Schedule Detail " + error);
                        }
                    }

                    @Override
                    public void onFailure(Call<ScheduleDetailResponse> call, Throwable t) {
                        String error = "Network Error: " + t.getMessage();
                        _errorMessage.setValue("Schedule Detail: " + error);
                        Log.e(TAG, "❌ Schedule Detail Network Error", t);
                    }
                });
    }

    // ✅ UPDATED: Get schedule ID from session
    private String getScheduleId() {
        if (session == null) return null;

        return session.getScheduleId();
    }

    /**
     * Load student final attendance from API
     */
    private void loadStudentFinalAttendance() {
        if (authManager == null || !authManager.isLoggedIn() || session == null) {
            _errorMessage.setValue("User not logged in or session data not available");
            return;
        }

        _isLoading.setValue(true);
        _errorMessage.setValue(null);

        String studentId = authManager.getCurrentUserId();
        String sessionId = session.getSessionId();

        Log.d(TAG, "Loading final attendance for student: " + studentId + ", session: " + sessionId);

        apiService.getStudentFinalAttendance(sessionId, studentId)
                .enqueue(new Callback<StudentFinalAttendanceResponse>() {
                    @Override
                    public void onResponse(Call<StudentFinalAttendanceResponse> call,
                                           Response<StudentFinalAttendanceResponse> response) {
                        _isLoading.setValue(false);

                        if (response.isSuccessful() && response.body() != null) {
                            StudentFinalAttendanceResponse apiResponse = response.body();

                            if (apiResponse.isSuccess()) {
                                _studentFinalAttendance.setValue(apiResponse.getData());
                                Log.d(TAG, "✅ Loaded student final attendance successfully");
                            } else {
                                String error = apiResponse.getError() != null ? apiResponse.getError() : "Unknown API error";
                                _errorMessage.setValue("Attendance: " + error);
                                Log.e(TAG, "❌ Attendance API Error: " + error);
                            }
                        } else {
                            String error = "HTTP Error: " + response.code();
                            _errorMessage.setValue("Attendance: " + error);
                            Log.e(TAG, "❌ Attendance " + error);
                        }
                    }

                    @Override
                    public void onFailure(Call<StudentFinalAttendanceResponse> call, Throwable t) {
                        _isLoading.setValue(false);
                        String error = "Network Error: " + t.getMessage();
                        _errorMessage.setValue("Attendance: " + error);
                        Log.e(TAG, "❌ Attendance Network Error", t);
                    }
                });
    }
}
