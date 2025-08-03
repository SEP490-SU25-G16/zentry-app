package vn.edu.fpt.zentryapp.lecturer.ui.schedule;

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
import vn.edu.fpt.zentryapp.lecturer.data.api.LecturerScheduleClassSectionService;
import vn.edu.fpt.zentryapp.lecturer.data.model.response.LecturerScheduleClassSection;
import vn.edu.fpt.zentryapp.lecturer.data.model.response.LecturerDailyScheduleClassSectionResponse;
import vn.edu.fpt.zentryapp.lecturer.data.model.response.StartSessionRequest;
import vn.edu.fpt.zentryapp.lecturer.data.model.response.LecturerStartSessionResponse;

public class LecturerScheduleViewModel extends ViewModel {
    private static final String TAG = "LecturerScheduleVM";

    // LiveData for UI state
    private final MutableLiveData<Boolean> _isLoading = new MutableLiveData<>(false);
    private final MutableLiveData<List<LecturerScheduleClassSection>> _todayClassSections = new MutableLiveData<>();
    private final MutableLiveData<String> _errorMessage = new MutableLiveData<>();

    // API Service
    private LecturerScheduleClassSectionService apiService;
    private AuthManager authManager;
    private Context context;

    // Public getters for LiveData
    public LiveData<Boolean> isLoading() { return _isLoading; }
    public LiveData<List<LecturerScheduleClassSection>> todaySessions() { return _todayClassSections; }
    public LiveData<String> errorMessage() { return _errorMessage; }

    public void init(Context context, AuthManager authManager) {
        this.context = context;
        this.authManager = authManager;
        this.apiService = ApiClient.getClient(context).create(LecturerScheduleClassSectionService.class);
        loadTodayClassSections();
    }

    /**
     * Load today's sessions from API - updated to work with new response format
     */
    private void loadTodayClassSections() {

        _isLoading.setValue(true);
        _errorMessage.setValue(null);

        String lecturerId = authManager.getCurrentUserId();
        String todayDate = getTodayDateString();

        Log.d(TAG, "Loading daily schedule for lecturer: " + lecturerId + ", date: " + todayDate);

        apiService.getDailySchedule(lecturerId, todayDate)
                .enqueue(new Callback<LecturerDailyScheduleClassSectionResponse>() {
                    @Override
                    public void onResponse(Call<LecturerDailyScheduleClassSectionResponse> call, Response<LecturerDailyScheduleClassSectionResponse> response) {
                        _isLoading.setValue(false);

                        if (response.isSuccessful() && response.body() != null) {
                            LecturerDailyScheduleClassSectionResponse apiResponse = response.body();

                            if (apiResponse.isSuccess()) {

                                List<LecturerScheduleClassSection> classSections = apiResponse.getData();
                                if (classSections == null) {
                                    classSections = new ArrayList<>();
                                }

                                _todayClassSections.setValue(classSections);
                                Log.d(TAG, "✅ Loaded " + classSections.size() + " classSections");
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
                    public void onFailure(Call<LecturerDailyScheduleClassSectionResponse> call, Throwable t) {
                        _isLoading.setValue(false);
                        String error = "Network Error: " + t.getMessage();
                        _errorMessage.setValue(error);
                        Log.e(TAG, "❌ Network Error", t);
                    }
                });
    }

    /**
     * Start session via API
     */
    public void startSession(LecturerScheduleClassSection session) {

        _errorMessage.setValue(null);

        String sessionId = session.getSessionId();
        Log.d(TAG, "Starting session via API: " + sessionId);

        String currentUserId = authManager.getCurrentUserId();
        StartSessionRequest request = new StartSessionRequest(currentUserId);

        apiService.startSession(sessionId, request)
                .enqueue(new Callback<LecturerStartSessionResponse>() {
                    @Override
                    public void onResponse(Call<LecturerStartSessionResponse> call, Response<LecturerStartSessionResponse> response) {

                        if (response.isSuccessful() && response.body() != null) {
                            LecturerStartSessionResponse apiResponse = response.body();

                            if (apiResponse.isSuccess()) {
                                Log.d(TAG, "✅ Session started successfully: " + apiResponse.getMessage());

                                refreshSessions();

                            } else {
                                String error = apiResponse.getError() != null ?
                                        apiResponse.getError() : "Failed to start session";
                                _errorMessage.setValue(error);
                                Log.e(TAG, "❌ Start session API error: " + error);
                            }
                        } else {
                            String error = "HTTP Error: " + response.code();
                            _errorMessage.setValue(error);
                            Log.e(TAG, "❌ Start session HTTP error: " + error);
                        }
                    }

                    @Override
                    public void onFailure(Call<LecturerStartSessionResponse> call, Throwable t) {
                        String error = "Network Error: " + t.getMessage();
                        _errorMessage.setValue(error);
                        Log.e(TAG, "❌ Start session network error", t);
                    }
                });
    }

    /**
     * Get today's date in yyyy-MM-dd format
     */
    private String getTodayDateString() {
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
        return dateFormat.format(new Date());
    }

    /**
     * Refresh sessions
     */
    public void refreshSessions() {
        loadTodayClassSections();
    }
}
