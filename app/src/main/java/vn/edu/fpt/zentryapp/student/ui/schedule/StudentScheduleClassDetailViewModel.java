package vn.edu.fpt.zentryapp.student.ui.schedule;

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
import vn.edu.fpt.zentryapp.lecturer.data.model.response.AttendanceRoundData;
import vn.edu.fpt.zentryapp.lecturer.data.model.response.AttendanceRoundsResponse;
import vn.edu.fpt.zentryapp.lecturer.data.model.response.FinalAttendanceData;
import vn.edu.fpt.zentryapp.lecturer.data.model.response.FinalAttendanceResponse;
import vn.edu.fpt.zentryapp.service.AttendanceApiService;

public class StudentScheduleClassDetailViewModel extends ViewModel {
    private static final String TAG = "StudentClassDetailVM";

    private final MutableLiveData<Boolean> _isLoading = new MutableLiveData<>(false);
    private final MutableLiveData<List<AttendanceRoundData>> _attendanceRounds = new MutableLiveData<>();
    private final MutableLiveData<List<FinalAttendanceData>> _finalAttendance = new MutableLiveData<>();
    private final MutableLiveData<String> _errorMessage = new MutableLiveData<>();


    // API Service
    private AttendanceApiService apiService;
    private AuthManager authManager;
    private Context context;
    private String sessionId;

    // Public getters
    public LiveData<Boolean> isLoading() { return _isLoading; }
    public LiveData<List<AttendanceRoundData>> attendanceRounds() { return _attendanceRounds; }
    public LiveData<List<FinalAttendanceData>> finalAttendance() { return _finalAttendance; }
    public LiveData<String> errorMessage() { return _errorMessage; }

    public void init(Context context, AuthManager authManager, String sessionId) {
        this.context = context;
        this.authManager = authManager;
        this.sessionId = sessionId;
        this.apiService = ApiClient.getClient(context).create(AttendanceApiService.class);

        // 🔧 CHỈ load attendance data, không load class detail
        loadAttendanceData();
    }

    /**
     * 🔧 LOAD attendance rounds và final attendance
     */
    private void loadAttendanceData() {
        _isLoading.setValue(true);
        _errorMessage.setValue(null);

        Log.d(TAG, "Loading attendance data for session: " + sessionId);

        // Load rounds và final attendance parallel
        loadAttendanceRounds();
        loadFinalAttendance();
    }

    /**
     * 🔧 LOAD attendance rounds từ API
     */
    private void loadAttendanceRounds() {
        apiService.getAttendanceRounds(sessionId)
                .enqueue(new Callback<AttendanceRoundsResponse>() {
                    @Override
                    public void onResponse(Call<AttendanceRoundsResponse> call, Response<AttendanceRoundsResponse> response) {
                        if (response.isSuccessful() && response.body() != null) {
                            AttendanceRoundsResponse apiResponse = response.body();

                            if (apiResponse.isSuccess()) {
                                _attendanceRounds.setValue(apiResponse.getData());
                                Log.d(TAG, "✅ Loaded " + apiResponse.getData().size() + " attendance rounds");
                            } else {
                                String error = apiResponse.getError() != null ? apiResponse.getError() : "Unknown API error";
                                _errorMessage.setValue("Rounds: " + error);
                                Log.e(TAG, "❌ Rounds API Error: " + error);
                            }
                        } else {
                            String error = "HTTP Error: " + response.code();
                            _errorMessage.setValue("Rounds: " + error);
                            Log.e(TAG, "❌ Rounds HTTP Error: " + error);
                        }

                        checkLoadingComplete();
                    }

                    @Override
                    public void onFailure(Call<AttendanceRoundsResponse> call, Throwable t) {
                        String error = "Network Error: " + t.getMessage();
                        _errorMessage.setValue("Rounds: " + error);
                        Log.e(TAG, "❌ Rounds Network Error", t);

                        checkLoadingComplete();
                    }
                });
    }

    /**
     * 🔧 LOAD final attendance từ API
     */
    private void loadFinalAttendance() {
        apiService.getFinalAttendance(sessionId)
                .enqueue(new Callback<FinalAttendanceResponse>() {
                    @Override
                    public void onResponse(Call<FinalAttendanceResponse> call, Response<FinalAttendanceResponse> response) {
                        if (response.isSuccessful() && response.body() != null) {
                            FinalAttendanceResponse apiResponse = response.body();

                            if (apiResponse.isSuccess()) {
                                _finalAttendance.setValue(apiResponse.getData());
                                Log.d(TAG, "✅ Loaded " + apiResponse.getData().size() + " final attendance records");
                            } else {
                                String error = apiResponse.getError() != null ? apiResponse.getError() : "Unknown API error";
                                _errorMessage.setValue("Final Attendance: " + error);
                                Log.e(TAG, "❌ Final Attendance API Error: " + error);
                            }
                        } else {
                            String error = "HTTP Error: " + response.code();
                            _errorMessage.setValue("Final Attendance: " + error);
                            Log.e(TAG, "❌ Final Attendance HTTP Error: " + error);
                        }

                        checkLoadingComplete();
                    }

                    @Override
                    public void onFailure(Call<FinalAttendanceResponse> call, Throwable t) {
                        String error = "Network Error: " + t.getMessage();
                        _errorMessage.setValue("Final Attendance: " + error);
                        Log.e(TAG, "❌ Final Attendance Network Error", t);

                        checkLoadingComplete();
                    }
                });
    }

    /**
     * 🔧 CHECK if both API calls completed
     */
    private void checkLoadingComplete() {
        // Set loading false khi cả hai API calls đã complete
        // (có thể implement logic phức tạp hơn nếu cần)
        _isLoading.setValue(false);
    }

    /**
     * 🔧 REFRESH attendance data
     */
    public void refreshAttendanceData() {
        loadAttendanceData();
    }

    public void onNotificationClicked() {
        // TODO: Handle notification action
    }
}
