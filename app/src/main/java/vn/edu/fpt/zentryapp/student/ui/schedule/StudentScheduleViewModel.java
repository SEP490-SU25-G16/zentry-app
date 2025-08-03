package vn.edu.fpt.zentryapp.student.ui.schedule;

import androidx.annotation.NonNull;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import android.content.Context;
import android.util.Log;

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
import vn.edu.fpt.zentryapp.student.data.api.StudentScheduleApiService;
import vn.edu.fpt.zentryapp.student.data.model.response.StudentDailyScheduleClassSectionResponse;
import vn.edu.fpt.zentryapp.student.data.model.response.StudentScheduleClassSection;

public class StudentScheduleViewModel extends ViewModel {
    private static final String TAG = "StudentScheduleVM";

    private final MutableLiveData<Boolean> _isLoading = new MutableLiveData<>(false);
    private final MutableLiveData<List<StudentScheduleClassSection>> _sessions = new MutableLiveData<>();
    private final MutableLiveData<String> _errorMessage = new MutableLiveData<>();

    // API Service
    private StudentScheduleApiService apiService;
    private AuthManager authManager;

    // Public getters
    public LiveData<Boolean> isLoading() { return _isLoading; }
    public LiveData<List<StudentScheduleClassSection>> sessions() { return _sessions; }
    public LiveData<String> errorMessage() { return _errorMessage; }

    public void init(Context context, AuthManager authManager) {
        this.authManager = authManager;
        this.apiService = ApiClient.getClient(context).create(StudentScheduleApiService.class);

        loadSessions();
    }

    public void loadSessions() {
        if (Boolean.TRUE.equals(_isLoading.getValue())) {
            return;
        }

        _isLoading.setValue(true);
        _errorMessage.setValue(null);

        String studentId = authManager.getCurrentUserId();
        String todayDate = getTodayDateString();

        Log.d(TAG, "Loading daily schedule for student: " + studentId + ", date: " + todayDate);

        apiService.getStudentDailyScheduleClassSection(studentId, todayDate)
                .enqueue(new Callback<StudentDailyScheduleClassSectionResponse>() {
                    @Override
                    public void onResponse(@NonNull Call<StudentDailyScheduleClassSectionResponse> call,
                                           @NonNull Response<StudentDailyScheduleClassSectionResponse> response) {
                        _isLoading.setValue(false);

                        if (response.isSuccessful() && response.body() != null) {
                            StudentDailyScheduleClassSectionResponse apiResponse = response.body();

                            if (apiResponse.isSuccess()) {
                                List<StudentScheduleClassSection> sessions = apiResponse.getData();

                                _sessions.setValue(sessions);
                                Log.d(TAG, "✅ Loaded " + (sessions != null ? sessions.size() : 0) + " student sessions");
                            } else {
                                String error = apiResponse.getError() != null ? apiResponse.getError() : "Unknown API error";
                                _errorMessage.setValue(error);
                                Log.e(TAG, "❌ API Error: " + error);
                            }
                        } else {
                            String error = "HTTP Error: " + response.code();
                            _errorMessage.setValue(error);
                            Log.e(TAG, "❌ " + error);
                        }
                    }

                    @Override
                    public void onFailure(@NonNull Call<StudentDailyScheduleClassSectionResponse> call, @NonNull Throwable t) {
                        _isLoading.setValue(false);
                        String error = "Network Error: " + t.getMessage();
                        _errorMessage.setValue(error);
                        Log.e(TAG, "❌ Network Error", t);
                    }
                });
    }

    /**
     * Refresh sessions data
     */
    public void refreshSessions() {
        loadSessions();
    }

    /**
     * Get today's date in yyyy-MM-dd format
     */
    private String getTodayDateString() {
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
        return dateFormat.format(new Date());
    }
}
