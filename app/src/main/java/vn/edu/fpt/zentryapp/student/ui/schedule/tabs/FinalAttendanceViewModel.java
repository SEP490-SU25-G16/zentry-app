package vn.edu.fpt.zentryapp.student.ui.schedule.tabs;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import android.content.Context;
import android.util.Log;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import vn.edu.fpt.zentryapp.auth.client.ApiClient;
import vn.edu.fpt.zentryapp.auth.client.AuthManager;
import vn.edu.fpt.zentryapp.lecturer.data.model.response.FinalAttendanceData;
import vn.edu.fpt.zentryapp.lecturer.data.model.response.FinalAttendanceResponse;
import vn.edu.fpt.zentryapp.service.AttendanceApiService;
import vn.edu.fpt.zentryapp.student.data.model.response.MyAttendance;

public class FinalAttendanceViewModel extends ViewModel {
    private static final String TAG = "FinalAttendanceVM";

    private final MutableLiveData<Boolean> _isLoading = new MutableLiveData<>(false);
    private final MutableLiveData<MyAttendance> _myAttendance = new MutableLiveData<>();
    private final MutableLiveData<String> _errorMessage = new MutableLiveData<>();
    private final MutableLiveData<String> _successMessage = new MutableLiveData<>();

    // API Service
    private AttendanceApiService apiService;
    private AuthManager authManager;
    private Context context;
    private String sessionId;

    // Public getters
    public LiveData<Boolean> isLoading() { return _isLoading; }
    public LiveData<MyAttendance> myAttendance() { return _myAttendance; }
    public LiveData<String> errorMessage() { return _errorMessage; }
    public LiveData<String> successMessage() { return _successMessage; }

    public void init(Context context, AuthManager authManager, String sessionId) {
        this.context = context;
        this.authManager = authManager;
        this.sessionId = sessionId;
        this.apiService = ApiClient.getClient(context).create(AttendanceApiService.class);

        loadMyAttendance();
    }

    /**
     * 🔧 LOAD student's attendance từ API và filter theo StudentId
     */
    private void loadMyAttendance() {
        if (authManager == null || !authManager.isLoggedIn()) {
            _errorMessage.setValue("User not logged in");
            return;
        }

        _isLoading.setValue(true);
        _errorMessage.setValue(null);

        String currentUserId = authManager.getCurrentUserId();
        Log.d(TAG, "Loading attendance for student: " + currentUserId + ", session: " + sessionId);

        apiService.getFinalAttendance(sessionId)
                .enqueue(new Callback<FinalAttendanceResponse>() {
                    @Override
                    public void onResponse(Call<FinalAttendanceResponse> call, Response<FinalAttendanceResponse> response) {
                        _isLoading.setValue(false);

                        if (response.isSuccessful() && response.body() != null) {
                            FinalAttendanceResponse apiResponse = response.body();

                            if (apiResponse.isSuccess()) {
                                // 🔧 LỌC ra record của student hiện tại dựa trên StudentId
                                FinalAttendanceData myRecord = findMyAttendanceRecord(apiResponse.getData(), currentUserId);

                                if (myRecord != null) {
                                    MyAttendance myAttendance = mapToMyAttendance(myRecord);
                                    _myAttendance.setValue(myAttendance);
                                    _successMessage.setValue("My attendance loaded successfully");

                                    Log.d(TAG, "✅ Found my attendance: " + myRecord.getStatus());
                                } else {
                                    _errorMessage.setValue("Your attendance record not found in this session");
                                    Log.w(TAG, "⚠️ Student record not found for userId: " + currentUserId);
                                }
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
                    public void onFailure(Call<FinalAttendanceResponse> call, Throwable t) {
                        _isLoading.setValue(false);
                        String error = "Network Error: " + t.getMessage();
                        _errorMessage.setValue(error);
                        Log.e(TAG, "❌ Network Error", t);
                    }
                });
    }

    /**
     * 🔧 TÌM record của student hiện tại từ list
     */
    private FinalAttendanceData findMyAttendanceRecord(List<FinalAttendanceData> allRecords, String currentUserId) {
        if (allRecords == null || allRecords.isEmpty() || currentUserId == null) {
            return null;
        }

        for (FinalAttendanceData record : allRecords) {
            if (currentUserId.equals(record.getStudentId())) {
                Log.d(TAG, "Found matching record for studentId: " + currentUserId);
                return record;
            }
        }

        Log.w(TAG, "No matching record found for studentId: " + currentUserId);
        return null;
    }

    /**
     * 🔧 MAP FinalAttendanceData sang MyAttendance model
     */
    private MyAttendance mapToMyAttendance(FinalAttendanceData apiRecord) {
        // Clean student name
        String studentName = cleanStudentName(apiRecord.getStudentFullName());

        // Determine attendance status
        boolean isPresent = isStudentPresent(apiRecord.getStatus(), apiRecord.getDetailedAttendanceStatus());

        // Parse last attendance time
        Date lastAttendanceTime = parseDateTime(apiRecord.getLastAttendanceTime());

        // TODO: Get total rounds from rounds API nếu cần chính xác
        // Hiện tại tạm thời assume based on status
        int totalSessions = 10; // Default hoặc lấy từ API khác
        int attendedSessions = isPresent ? totalSessions : 0; // Simplified logic
        int absentSessions = totalSessions - attendedSessions;

        return new MyAttendance(
                apiRecord.getStudentId(),
                studentName,
                apiRecord.getEmail(),
                totalSessions,
                attendedSessions,
                absentSessions,
                lastAttendanceTime,
                isPresent,
                apiRecord.getStatus(),
                apiRecord.getDetailedAttendanceStatus()
        );
    }

    /**
     * 🔧 CLEAN student name (remove "- Student" suffix)
     */
    private String cleanStudentName(String fullName) {
        if (fullName == null) return "Unknown Student";

        if (fullName.endsWith(" - Student")) {
            return fullName.substring(0, fullName.length() - " - Student".length());
        }

        return fullName;
    }

    /**
     * 🔧 XÁC ĐỊNH student có present không
     */
    private boolean isStudentPresent(String status, String detailedStatus) {
        // Check both status fields
        if ("Present".equalsIgnoreCase(status) || "Attended".equalsIgnoreCase(status)) {
            return true;
        }

        if ("Present".equalsIgnoreCase(detailedStatus) || "Attended".equalsIgnoreCase(detailedStatus)) {
            return true;
        }

        // Default is absent
        return false;
    }

    /**
     * 🔧 PARSE datetime string
     */
    private Date parseDateTime(String dateTimeString) {
        if (dateTimeString == null || dateTimeString.trim().isEmpty()) {
            return new Date(); // Current time as fallback
        }

        try {
            SimpleDateFormat isoFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.getDefault());
            Date parsed = isoFormat.parse(dateTimeString.trim());
            return parsed != null ? parsed : new Date();
        } catch (Exception e) {
            Log.e(TAG, "Error parsing datetime: " + dateTimeString, e);
            return new Date();
        }
    }

    /**
     * 🔧 REFRESH attendance data
     */
    public void refreshMyAttendance() {
        loadMyAttendance();
    }
}
