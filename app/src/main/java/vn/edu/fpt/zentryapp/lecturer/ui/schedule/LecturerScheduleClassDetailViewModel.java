package vn.edu.fpt.zentryapp.lecturer.ui.schedule;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import android.content.Context;
import android.util.Log;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Random;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import vn.edu.fpt.zentryapp.auth.client.ApiClient;
import vn.edu.fpt.zentryapp.auth.client.AuthManager;
import vn.edu.fpt.zentryapp.lecturer.data.model.response.AttendanceRound;
import vn.edu.fpt.zentryapp.lecturer.data.model.response.AttendanceRoundData;
import vn.edu.fpt.zentryapp.lecturer.data.model.response.AttendanceRoundsResponse;
import vn.edu.fpt.zentryapp.lecturer.data.model.response.FinalAttendance;
import vn.edu.fpt.zentryapp.lecturer.data.model.response.FinalAttendanceData;
import vn.edu.fpt.zentryapp.lecturer.data.model.response.FinalAttendanceResponse;
import vn.edu.fpt.zentryapp.lecturer.data.model.response.SessionDetailInfoRound;
import vn.edu.fpt.zentryapp.service.AttendanceApiService;

public class LecturerScheduleClassDetailViewModel extends ViewModel {
    private static final String TAG = "ClassDetailViewModel";

    private final MutableLiveData<Boolean> _isLoading = new MutableLiveData<>(false);
    private final MutableLiveData<SessionDetailInfoRound> _sessionInfo = new MutableLiveData<>();
    private final MutableLiveData<List<AttendanceRound>> _attendanceRounds = new MutableLiveData<>();
    private final MutableLiveData<List<FinalAttendance>> _finalAttendance = new MutableLiveData<>();
    private final MutableLiveData<String> _errorMessage = new MutableLiveData<>();
    private final MutableLiveData<Boolean> _canAddFaceId = new MutableLiveData<>(true);

    // API Service
    private AttendanceApiService apiService;
    private AuthManager authManager;
    private Context context;
    private String sessionId;

    // Public getters
    public LiveData<Boolean> isLoading() { return _isLoading; }
    public LiveData<SessionDetailInfoRound> sessionInfo() { return _sessionInfo; }
    public LiveData<List<AttendanceRound>> attendanceRounds() { return _attendanceRounds; }
    public LiveData<List<FinalAttendance>> finalAttendance() { return _finalAttendance; }
    public LiveData<String> errorMessage() { return _errorMessage; }
    public LiveData<Boolean> canAddFaceId() { return _canAddFaceId; }

    public void init(Context context, AuthManager authManager, String sessionId) {
        this.context = context;
        this.authManager = authManager;
        this.sessionId = sessionId;
        this.apiService = ApiClient.getClient(context).create(AttendanceApiService.class);

        loadSessionInfo();
        loadAttendanceRounds(); // 🔧 Load từ API thay vì mock
    }

    private void loadSessionInfo() {
        // TODO: Implement session info API call if needed
        // For now, keep mock data hoặc pass từ navigation args
        SessionDetailInfoRound sessionInfo = generateSessionInfo();
        _sessionInfo.setValue(sessionInfo);

        boolean canAddFaceId = !isSessionCompleted(sessionInfo);
        _canAddFaceId.setValue(canAddFaceId);
    }

    /**
     * 🔧 LOAD attendance rounds từ API thật
     */
    private void loadAttendanceRounds() {
        _isLoading.setValue(true);
        _errorMessage.setValue(null);

        Log.d(TAG, "Loading attendance rounds for session: " + sessionId);

        apiService.getAttendanceRounds(sessionId)
                .enqueue(new Callback<AttendanceRoundsResponse>() {
                    @Override
                    public void onResponse(Call<AttendanceRoundsResponse> call, Response<AttendanceRoundsResponse> response) {
                        _isLoading.setValue(false);

                        if (response.isSuccessful() && response.body() != null) {
                            AttendanceRoundsResponse apiResponse = response.body();

                            if (apiResponse.isSuccess()) {
                                List<AttendanceRound> rounds = mapApiDataToAttendanceRounds(apiResponse.getData());
                                _attendanceRounds.setValue(rounds);

                                // Load final attendance sau khi có rounds
                                loadFinalAttendance();

                                Log.d(TAG, "✅ Loaded " + rounds.size() + " attendance rounds");
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
                    public void onFailure(Call<AttendanceRoundsResponse> call, Throwable t) {
                        _isLoading.setValue(false);
                        String error = "Network Error: " + t.getMessage();
                        _errorMessage.setValue(error);
                        Log.e(TAG, "❌ Network Error", t);

                    }
                });
    }

    /**
     * 🔧 MAP API data sang AttendanceRound objects
     */
    private List<AttendanceRound> mapApiDataToAttendanceRounds(List<AttendanceRoundData> apiData) {
        List<AttendanceRound> rounds = new ArrayList<>();

        if (apiData == null || apiData.isEmpty()) {
            return rounds;
        }

        SimpleDateFormat isoFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.getDefault());

        for (AttendanceRoundData apiRound : apiData) {
            try {
                AttendanceRound round = new AttendanceRound();

                // Basic info
                round.setRoundId(apiRound.getRoundId());
                round.setSessionId(apiRound.getSessionId());
                round.setRoundNumber(apiRound.getRoundNumber());

                // Parse times
                Date startTime = isoFormat.parse(apiRound.getStartTime());
                Date endTime = isoFormat.parse(apiRound.getEndTime());
                round.setTimestamp(startTime); // hoặc có thể dùng endTime tùy logic

                // Attendance data
                round.setTotalStudents(apiRound.getTotalStudents());
                round.setPresentStudents(apiRound.getAttendedCount());

                // Status và type
                round.setRoundType(determineRoundType(apiRound.getRoundNumber(), apiData.size()));
                round.setLocation(extractLocationFromData(apiRound)); // Extract từ course info nếu có

                rounds.add(round);

                Log.d(TAG, "Mapped round " + apiRound.getRoundNumber() +
                        ": " + apiRound.getAttendedCount() + "/" + apiRound.getTotalStudents() +
                        " (" + apiRound.getStatus() + ")");

            } catch (Exception e) {
                Log.e(TAG, "Error mapping round " + apiRound.getRoundNumber(), e);
            }
        }

        return rounds;
    }

    /**
     * 🔧 XÁC ĐỊNH round type dựa trên vị trí
     */
    private String determineRoundType(int roundNumber, int totalRounds) {
        if (roundNumber == 1) {
            return "START";
        } else if (roundNumber == totalRounds) {
            return "END";
        } else {
            return "MIDDLE";
        }
    }

    /**
     * 🔧 EXTRACT location từ API data
     */
    private String extractLocationFromData(AttendanceRoundData apiRound) {
        // Có thể extract từ course info hoặc session info
        // Tạm thời return empty, sẽ được set từ session info
        return "";
    }
    /**
     * 🔧 LOAD final attendance từ API thật
     */
    private void loadFinalAttendance() {
        Log.d(TAG, "Loading final attendance for session: " + sessionId);

        apiService.getFinalAttendance(sessionId)
                .enqueue(new Callback<FinalAttendanceResponse>() {
                    @Override
                    public void onResponse(Call<FinalAttendanceResponse> call, Response<FinalAttendanceResponse> response) {
                        if (response.isSuccessful() && response.body() != null) {
                            FinalAttendanceResponse apiResponse = response.body();

                            if (apiResponse.isSuccess()) {
                                List<FinalAttendance> finalAttendance = mapApiDataToFinalAttendance(apiResponse.getData());
                                _finalAttendance.setValue(finalAttendance);

                                Log.d(TAG, "✅ Loaded " + finalAttendance.size() + " final attendance records");
                            } else {
                                String error = apiResponse.getError() != null ? apiResponse.getError() : "Unknown API error";
                                _errorMessage.setValue(error);
                                Log.e(TAG, "❌ Final Attendance API Error: " + error);
                            }
                        } else {
                            String error = "HTTP Error: " + response.code();
                            _errorMessage.setValue(error);
                            Log.e(TAG, "❌ Final Attendance HTTP Error: " + error);
                        }
                    }

                    @Override
                    public void onFailure(Call<FinalAttendanceResponse> call, Throwable t) {
                        String error = "Network Error: " + t.getMessage();
                        _errorMessage.setValue(error);
                        Log.e(TAG, "❌ Final Attendance Network Error", t);
                    }
                });
    }

    /**
     * 🔧 MAP API data sang FinalAttendance objects
     */
    private List<FinalAttendance> mapApiDataToFinalAttendance(List<FinalAttendanceData> apiData) {
        List<FinalAttendance> finalAttendanceList = new ArrayList<>();

        if (apiData == null || apiData.isEmpty()) {
            return finalAttendanceList;
        }

        SimpleDateFormat isoFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.getDefault());

        for (FinalAttendanceData apiStudent : apiData) {
            try {
                FinalAttendance student = new FinalAttendance();

                // Basic student info
                student.setStudentId(apiStudent.getStudentId());
                student.setStudentName(cleanStudentName(apiStudent.getStudentFullName()));
                student.setEmail(apiStudent.getEmail());

                // Generate student code từ email hoặc studentId
                student.setStudentCode(generateStudentCode(apiStudent.getEmail(), apiStudent.getStudentId()));

                // Attendance status
                boolean isPresent = isStudentPresent(apiStudent.getStatus(), apiStudent.getDetailedAttendanceStatus());
                student.setFinalStatus(isPresent);

                // TODO: Calculate rounds data nếu cần
                // Tạm thời set default values, có thể tính toán từ rounds data
                student.setTotalRounds(getTotalRoundsFromRoundsData());
                student.setAttendedRounds(isPresent ? student.getTotalRounds() : 0);

                finalAttendanceList.add(student);

                Log.d(TAG, "Mapped student: " + student.getStudentName() +
                        " - Status: " + (isPresent ? "Present" : "Absent"));

            } catch (Exception e) {
                Log.e(TAG, "Error mapping student: " + apiStudent.getStudentFullName(), e);
            }
        }

        return finalAttendanceList;
    }

    /**
     * 🔧 CLEAN student name (remove "- Student" suffix)
     */
    private String cleanStudentName(String fullName) {
        if (fullName == null) return "Unknown Student";

        // Remove "- Student" suffix nếu có
        if (fullName.endsWith(" - Student")) {
            return fullName.substring(0, fullName.length() - " - Student".length());
        }

        return fullName;
    }

    /**
     * 🔧 GENERATE student code từ email
     */
    private String generateStudentCode(String email, String studentId) {
        if (email != null && email.contains("student")) {
            // Extract số từ email: student453.bob@zentry.edu -> ST453
            try {
                String[] parts = email.split("\\.");
                if (parts.length > 0) {
                    String numberPart = parts[0].replaceAll("[^0-9]", "");
                    if (!numberPart.isEmpty()) {
                        return "ST" + numberPart;
                    }
                }
            } catch (Exception e) {
                Log.w(TAG, "Failed to extract student code from email: " + email);
            }
        }

        // Fallback: use last 6 chars of studentId
        if (studentId != null && studentId.length() >= 6) {
            return "ST" + studentId.substring(studentId.length() - 6).toUpperCase();
        }

        return "ST000000";
    }

    /**
     * 🔧 XÁC ĐỊNH student có present không
     */
    private boolean isStudentPresent(String status, String detailedStatus) {
        // Check cả status và detailedStatus
        if ("Present".equalsIgnoreCase(status) || "Attended".equalsIgnoreCase(status)) {
            return true;
        }

        if ("Present".equalsIgnoreCase(detailedStatus) || "Attended".equalsIgnoreCase(detailedStatus)) {
            return true;
        }

        // Default là absent
        return false;
    }

    /**
     * 🔧 LẤY total rounds từ rounds data đã load trước đó
     */
    private int getTotalRoundsFromRoundsData() {
        List<AttendanceRound> rounds = _attendanceRounds.getValue();
        if (rounds != null && !rounds.isEmpty()) {
            return rounds.size();
        }

        // Default fallback
        return 4;
    }

    /**
     * 🔧 REFRESH data
     */
    public void refreshData() {
        loadAttendanceRounds();
    }

    // ==================== EXISTING METHODS ====================

    private boolean isSessionCompleted(SessionDetailInfoRound sessionInfo) {
        if (sessionInfo == null) return false;
        String status = sessionInfo.getStatus();
        return "COMPLETED".equals(status) ||
                "FINISHED".equals(status) ||
                "ENDED".equals(status);
    }

    private SessionDetailInfoRound generateSessionInfo() {
        SessionDetailInfoRound info = new SessionDetailInfoRound();
        info.setSessionId(sessionId);
        info.setCourseCode("CSE101");
        info.setCourseName("Lập trình căn bản");
        info.setClassName("SE1801");
        info.setRoom("DE-201");
        info.setSessionDate(Calendar.getInstance().getTime());

        Calendar start = Calendar.getInstance();
        start.set(Calendar.HOUR_OF_DAY, 8);
        start.set(Calendar.MINUTE, 0);
        info.setStartTime(start.getTime());

        Calendar end = Calendar.getInstance();
        end.set(Calendar.HOUR_OF_DAY, 9);
        end.set(Calendar.MINUTE, 30);
        info.setEndTime(end.getTime());

        info.setTotalStudents(32);
        info.setTotalRounds(4);
        info.setStatus("IN PROGRESS");
        info.setDuration(90 * 60 * 1000L);

        return info;
    }


}
