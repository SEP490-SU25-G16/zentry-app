package vn.edu.fpt.zentryapp.lecturer.ui.schedule;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import android.annotation.SuppressLint;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.util.Log;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import vn.edu.fpt.zentryapp.auth.client.ApiClient;
import vn.edu.fpt.zentryapp.auth.client.AuthManager;
import vn.edu.fpt.zentryapp.faceid.data.api.FaceIdApiController;
import vn.edu.fpt.zentryapp.faceid.data.model.response.FaceIdRequestStatusResponse;
import vn.edu.fpt.zentryapp.lecturer.data.model.response.EndSessionRequest;
import vn.edu.fpt.zentryapp.lecturer.data.model.response.EndSessionResponse;
import vn.edu.fpt.zentryapp.lecturer.data.model.response.Round;
import vn.edu.fpt.zentryapp.lecturer.data.model.response.RoundDetail;
import vn.edu.fpt.zentryapp.lecturer.data.model.response.RoundResultDto;
import vn.edu.fpt.zentryapp.lecturer.data.model.response.RoundResultResponse;
import vn.edu.fpt.zentryapp.lecturer.data.model.response.RoundsDataResponse;
import vn.edu.fpt.zentryapp.lecturer.data.model.response.Attendance;
import vn.edu.fpt.zentryapp.lecturer.data.model.response.FinalAttendance;
import vn.edu.fpt.zentryapp.lecturer.data.model.response.AttendanceResponse;
import vn.edu.fpt.zentryapp.lecturer.data.model.response.LecturerScheduleClassSection;
import vn.edu.fpt.zentryapp.lecturer.data.model.response.SessionDetailInfoRound;
import vn.edu.fpt.zentryapp.lecturer.data.model.response.StudentAttendanceDto;
import vn.edu.fpt.zentryapp.service.AttendanceApiService;
import vn.edu.fpt.zentryapp.lecturer.data.model.request.FaceIdRequestCreateRequest;
import vn.edu.fpt.zentryapp.lecturer.data.model.response.FaceIdRequestCreateResponse;
import vn.edu.fpt.zentryapp.service.BLEAttendanceService;
import vn.edu.fpt.zentryapp.student.data.model.response.ScheduleDetailDto;
import vn.edu.fpt.zentryapp.student.data.model.response.ScheduleDetailResponse;

public class LecturerScheduleClassDetailViewModel extends ViewModel {
    private static final String TAG = "ClassDetailViewModel";
    private BroadcastReceiver attendanceCalculatedReceiver;
    private final MutableLiveData<SessionDetailInfoRound> _sessionInfo = new MutableLiveData<>();
    private final MutableLiveData<List<Round>> _listHistoryRounds = new MutableLiveData<>();
    private final MutableLiveData<List<Attendance>> _listAttendance = new MutableLiveData<>();
    private final MutableLiveData<RoundResultDto> _roundResult = new MutableLiveData<>();
    private final MutableLiveData<Boolean> _isLoadingRoundDetail = new MutableLiveData<>(false);
    private final MutableLiveData<String> _errorMessage = new MutableLiveData<>();
    private final MutableLiveData<Boolean> _canAddFaceId = new MutableLiveData<>(true); // Always enable for demo
    private final MutableLiveData<List<Attendance>> _listRoundAttendance = new MutableLiveData<>();
    private final MutableLiveData<Integer> _currentRoundNumber = new MutableLiveData<>();
    private final MutableLiveData<Boolean> _isEndingSession = new MutableLiveData<>(false);
    private final MutableLiveData<EndSessionResponse> _endSessionResult = new MutableLiveData<>();
    private final MutableLiveData<Boolean> _isCreatingFaceIdRequest = new MutableLiveData<>(false);
    private final MutableLiveData<String> _faceIdRequestSuccess = new MutableLiveData<>();
    private final MutableLiveData<String> _faceIdRequestError = new MutableLiveData<>();

    public LiveData<Boolean> isEndingSession() { return _isEndingSession; }
    public LiveData<EndSessionResponse> endSessionResult() { return _endSessionResult; }
    public LiveData<Boolean> isCreatingFaceIdRequest() { return _isCreatingFaceIdRequest; }
    public LiveData<String> faceIdRequestSuccess() { return _faceIdRequestSuccess; }
    public LiveData<String> faceIdRequestError() { return _faceIdRequestError; }
    // API Service
    private AttendanceApiService apiService;
    private FaceIdApiController faceIdApiController;
    // Expose status for UI (optional)
    private final MutableLiveData<FaceIdRequestStatusResponse> _faceIdRequestStatus = new MutableLiveData<>();
    public LiveData<FaceIdRequestStatusResponse> faceIdRequestStatus() { return _faceIdRequestStatus; }
    private AuthManager authManager;
    @SuppressLint("StaticFieldLeak")
    private Context context;
    private LecturerScheduleClassSection session;
    // Track active Face ID request IDs created in this session (best-effort local cache)
    private final List<String> activeFaceIdRequestIds = new ArrayList<>();
    // Public getters
    public LiveData<SessionDetailInfoRound> sessionInfo() { return _sessionInfo; }
    public LiveData<List<Round>> listHistoryRounds() { return _listHistoryRounds; }
    public LiveData<List<Attendance>> listFinalAttendance() { return _listAttendance; }

    public LiveData<String> errorMessage() { return _errorMessage; }
    public LiveData<Boolean> canAddFaceId() { return _canAddFaceId; }
    public LiveData<List<Attendance>> listRoundAttendance() { return _listRoundAttendance; }
    public LiveData<Integer> currentRoundNumber() { return _currentRoundNumber; }

    public void init(Context context, AuthManager authManager, LecturerScheduleClassSection session) {
        Log.d(TAG, "========== ViewModel init() started ==========");
        Log.d(TAG, "📋 Context: " + (context != null ? "✅ Available" : "❌ NULL"));
        Log.d(TAG, "📋 AuthManager: " + (authManager != null ? "✅ Available" : "❌ NULL"));
        Log.d(TAG, "📋 Session: " + (session != null ? "✅ Available" : "❌ NULL"));

        if (session != null) {
            Log.d(TAG, "📋 Session details:");
            Log.d(TAG, "    • SessionId: " + session.getSessionId());
            Log.d(TAG, "    • CourseCode: " + session.getCourseCode());
            Log.d(TAG, "    • CourseName: " + session.getCourseName());
            Log.d(TAG, "    • Status: " + session.getSessionStatus());
        }

        this.context = context;
        this.authManager = authManager;
        this.session = session;
        Log.d(TAG, "🔧 Creating API service...");
        try {
            this.apiService = ApiClient.getClient(context).create(AttendanceApiService.class);
            this.faceIdApiController = ApiClient.getClient(context).create(FaceIdApiController.class);
            Log.d(TAG, "✅ API service created successfully");
        } catch (Exception e) {
            Log.e(TAG, "❌ Failed to create API service", e);
            _errorMessage.setValue("Failed to initialize API service: " + e.getMessage());
            return;
        }

        Log.d(TAG, "🔧 Setting up receivers and loading data...");
        try {
            setupAttendanceCalculatedReceiver();
            Log.d(TAG, "✅ Receiver setup completed");
        } catch (Exception e) {
            Log.e(TAG, "❌ Failed to setup receiver", e);
        }

        try {
            Log.d(TAG, "🔄 Starting loadSessionInfo()...");
            loadSessionInfo();
        } catch (Exception e) {
            Log.e(TAG, "❌ Failed to load session info", e);
        }

        try {
            Log.d(TAG, "🔄 Starting loadListRounds()...");
            loadListRounds();
            Log.d(TAG, "✅ loadListRounds() call completed (async)");
        } catch (Exception e) {
            Log.e(TAG, "❌ Failed to call loadListRounds()", e);
        }

        try {
            Log.d(TAG, "🔄 Starting loadListFinalAttendances()...");
            loadListFinalAttendances();
        } catch (Exception e) {
            Log.e(TAG, "❌ Failed to load final attendances", e);
        }

        Log.d(TAG, "========== ViewModel init() completed ==========");
    }

    private String getSessionId() {
        return session != null ? session.getSessionId() : "UNKNOWN_SESSION";
    }

    private void setupAttendanceCalculatedReceiver() {
        attendanceCalculatedReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                if (BLEAttendanceService.ACTION_ATTENDANCE_CALCULATED.equals(intent.getAction())) {
                    String receivedSessionId = intent.getStringExtra(BLEAttendanceService.EXTRA_SESSION_ID);
                    if (getSessionId().equals(receivedSessionId)) {
                        Log.d(TAG, "Received attendance calculated broadcast - refreshing data");
                        refreshData();
                    }
                }
            }
        };

        IntentFilter filter = new IntentFilter(BLEAttendanceService.ACTION_ATTENDANCE_CALCULATED);
        androidx.localbroadcastmanager.content.LocalBroadcastManager
                .getInstance(context)
                .registerReceiver(attendanceCalculatedReceiver, filter);

        Log.d(TAG, "Attendance calculated receiver registered");
    }

    private String getScheduleId() {
        return session != null ? session.getScheduleId() : null;
    }

    /**
     * Load session detail info from API
     */
    private void loadSessionInfo() {
        String scheduleId = getScheduleId();
        if (scheduleId == null) {
            _errorMessage.setValue("Schedule ID not available");
            return;
        }

        Log.d(TAG, "Loading schedule detail for: " + scheduleId);

        // ✅ FIXED: Sử dụng getScheduleDetail thay vì getClassSectionDetail
        apiService.getScheduleDetail(scheduleId)
                .enqueue(new Callback<ScheduleDetailResponse>() {
                    @Override
                    public void onResponse(Call<ScheduleDetailResponse> call,
                                           Response<ScheduleDetailResponse> response) {
                        if (response.isSuccessful() && response.body() != null) {
                            ScheduleDetailResponse apiResponse = response.body();

                            if (apiResponse.isSuccess()) {
                                // ✅ FIXED: Map schedule detail to session info
                                SessionDetailInfoRound sessionInfo = mapScheduleToSessionInfo(apiResponse.getData());
                                _sessionInfo.setValue(sessionInfo);

                                // Always enable the face ID button for demo purposes
                                _canAddFaceId.setValue(true);
                                Log.d(TAG, "✅ Loaded schedule detail successfully");
                            } else {
                                String error = apiResponse.getError() != null ? apiResponse.getError() : "Unknown API error";
                                _errorMessage.setValue("Schedule Info: " + error);
                                Log.e(TAG, "❌ Schedule Info API Error: " + error);
                            }
                        } else {
                            String error = "HTTP Error: " + response.code();
                            _errorMessage.setValue("Schedule Info: " + error);
                            Log.e(TAG, "❌ Schedule Info " + error);
                        }
                    }

                    @Override
                    public void onFailure(Call<ScheduleDetailResponse> call, Throwable t) {
                        String error = "Network Error: " + t.getMessage();
                        _errorMessage.setValue("Schedule Info: " + error);
                        Log.e(TAG, "❌ Schedule Info Network Error", t);
                    }
                });
    }

    // ✅ FIXED: Rename method và parameter type
    private SessionDetailInfoRound mapScheduleToSessionInfo(ScheduleDetailDto scheduleDetail) {
        SessionDetailInfoRound info = new SessionDetailInfoRound();

        // ✅ Map basic info from schedule detail
        info.setCourseName(scheduleDetail.getCourseName());
        info.setSessionId(getSessionId());

        // ✅ FIXED: Map from session object if available
        if (session != null) {
            info.setCourseCode(session.getCourseCode());
            info.setClassName(session.getSectionCode());
            info.setRoom(session.getBuildingRoomDisplay());
            info.setStartTime(session.getStartTimeAsDate());
            info.setEndTime(session.getEndTimeAsDate());
            info.setStatus(session.getSessionStatus());
            info.setSessionDate(new Date()); // Current date

            // ✅ Calculate duration from session times
            if (session.getStartTimeAsDate() != null && session.getEndTimeAsDate() != null) {
                long duration = session.getEndTimeAsDate().getTime() - session.getStartTimeAsDate().getTime();
                info.setDuration(duration);
            } else {
                // ✅ Use duration from schedule detail
                long durationMs = scheduleDetail.getDurationInMinutes() * 60 * 1000L;
                info.setDuration(durationMs);
            }
        } else {
            // ✅ ENHANCED: Use data from schedule detail when session is null
            info.setCourseCode(scheduleDetail.getSectionCode());
            info.setClassName(scheduleDetail.getSectionCode());
            info.setRoom(scheduleDetail.getFormattedRoomDisplay());
            info.setStatus(scheduleDetail.getSessionStatus());
            info.setSessionDate(new Date());

            // ✅ Parse time from schedule detail
            info.setStartTime(parseTimeOnly(scheduleDetail.getStartTime()));
            info.setEndTime(parseTimeOnly(scheduleDetail.getEndTime()));

            // ✅ Use duration from schedule detail
            long durationMs = scheduleDetail.getDurationInMinutes() * 60 * 1000L;
            info.setDuration(durationMs);
        }

        // ✅ Map student count from schedule detail
        info.setTotalStudents(scheduleDetail.getEnrolledStudentsCount());
        info.setTotalRounds(4);

        Log.d(TAG, "Mapped schedule info: " + scheduleDetail.getCourseName() +
                " (" + scheduleDetail.getFormattedStudentCount() + ", " +
                scheduleDetail.getFormattedDuration() + ")");

        return info;
    }

    // ✅ NEW: Helper method để parse TimeOnly format
    private Date parseTimeOnly(String timeString) {
        try {
            if (timeString == null || timeString.isEmpty()) return null;

            String[] timeParts = timeString.split(":");
            if (timeParts.length >= 2) {
                int hour = Integer.parseInt(timeParts[0]);
                int minute = Integer.parseInt(timeParts[1]);

                Calendar calendar = Calendar.getInstance();
                calendar.set(Calendar.HOUR_OF_DAY, hour);
                calendar.set(Calendar.MINUTE, minute);
                calendar.set(Calendar.SECOND, 0);
                calendar.set(Calendar.MILLISECOND, 0);

                return calendar.getTime();
            }
        } catch (Exception e) {
            Log.e(TAG, "Error parsing time: " + timeString, e);
        }
        return null;
    }

    /**
     * Filter rounds by current time - only show rounds that should be visible
     */
    private List<Round> filterRoundsByCurrentTime(List<Round> allRounds) {
        if (allRounds == null || allRounds.isEmpty()) return new ArrayList<>();

        long currentTime = System.currentTimeMillis();
        List<Round> visibleRounds = new ArrayList<>();

        // Tìm round hiện tại đang active
        Round currentActiveRound = null;
        for (Round round : allRounds) {
            if (round.getStartDateTime() != null && round.getEndDateTime() != null &&
                    round.getStartDateTime().getTime() <= currentTime &&
                    currentTime <= round.getEndDateTime().getTime()) {
                currentActiveRound = round;
                break;
            }
        }

        if (currentActiveRound != null) {
            // Nếu có round đang active, hiển thị tất cả rounds từ round 1 đến round hiện tại
            int currentRoundNumber = currentActiveRound.getRoundNumber();
            for (Round round : allRounds) {
                if (round.getRoundNumber() <= currentRoundNumber) {
                    visibleRounds.add(round);
                }
            }
        } else {
            // Nếu không có round nào đang active, kiểm tra xem có round nào đã kết thúc chưa
            Round lastCompletedRound = null;
            for (Round round : allRounds) {
                if (round.getEndDateTime() != null && round.getEndDateTime().getTime() < currentTime) {
                    if (lastCompletedRound == null || round.getRoundNumber() > lastCompletedRound.getRoundNumber()) {
                        lastCompletedRound = round;
                    }
                }
            }

            if (lastCompletedRound != null) {
                // Hiển thị tất cả rounds từ 1 đến round cuối đã hoàn thành
                int lastRoundNumber = lastCompletedRound.getRoundNumber();
                for (Round round : allRounds) {
                    if (round.getRoundNumber() <= lastRoundNumber) {
                        visibleRounds.add(round);
                    }
                }
            }
            // Nếu chưa có round nào bắt đầu thì không hiển thị gì
        }

        // Sort by round number để đảm bảo thứ tự đúng
        visibleRounds.sort((r1, r2) -> Integer.compare(r1.getRoundNumber(), r2.getRoundNumber()));

        return visibleRounds;
    }

    /**
     * Load attendance rounds from API
     */
    private void loadListRounds() {
        if (session == null) {
            Log.e(TAG, "❌ Session is null, cannot load rounds");
            _errorMessage.setValue("Session data not available");
            return;
        }
        _errorMessage.setValue(null);

        String sessionId = getSessionId();
        Log.d(TAG, "📤 Loading rounds for sessionId: " + sessionId);

        apiService.getListRounds(sessionId)
                .enqueue(new Callback<RoundsDataResponse>() {
                    @Override
                    public void onResponse(Call<RoundsDataResponse> call, Response<RoundsDataResponse> response) {
                        if (response.isSuccessful() && response.body() != null) {
                            RoundsDataResponse apiResponse = response.body();

                            if (apiResponse.isSuccess()) {
                                // Map data
                                List<Round> allRounds = mapApiDataToAttendanceRounds(apiResponse.getData());

                                // Filter completed rounds
                                List<Round> completedRounds = filterCompletedRounds(allRounds);

                                // Filter by current time before setting to LiveData
                                List<Round> visibleRounds = filterRoundsByCurrentTime(completedRounds);

                                // Set filtered value to LiveData
                                _listHistoryRounds.setValue(visibleRounds);

                                Log.d(TAG, "✅ Successfully loaded " + allRounds.size() + " total rounds, " +
                                        completedRounds.size() + " completed rounds, " +
                                        visibleRounds.size() + " visible rounds displayed");

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
                    public void onFailure(Call<RoundsDataResponse> call, Throwable t) {
                        String error = "Network Error: " + t.getMessage();
                        _errorMessage.setValue(error);
                        Log.e(TAG, "❌ Network Error: " + error, t);
                    }
                });
    }

    private List<Round> filterCompletedRounds(List<Round> allRounds) {
        List<Round> completedRounds = new ArrayList<>();

        if (allRounds == null || allRounds.isEmpty()) {
            return completedRounds;
        }

        for (Round round : allRounds) {
            if (isRoundCompleted(round)) {
                completedRounds.add(round);
                Log.d(TAG, "✅ Included completed round " + round.getRoundNumber() +
                        " - Status: " + round.getStatus());
            } else {
                Log.d(TAG, "⏭️ Skipped non-completed round " + round.getRoundNumber() +
                        " - Status: " + round.getStatus());
            }
        }

        Log.d(TAG, "Filtered " + completedRounds.size() + " completed rounds out of " + allRounds.size() + " total");
        return completedRounds;
    }

    private boolean isRoundCompleted(Round round) {
        if (round == null) {
            return false;
        }

        String status = round.getStatus();
        if (status == null) {
            return false;
        }
        Date currentTime = new Date();
        // ✅ Check các status values có thể indicate completed
        switch (status.toLowerCase()) {
            case "completed":
                return true;

            case "active":
                // ✅ THÊM: Check nếu Active + đã quá endTime → coi như completed
                if (round.getEndDateTime() != null) {
                    boolean isPastEndTime = currentTime.after(round.getEndDateTime());

                    Log.d(TAG, String.format("Round %d - Status: ACTIVE, EndTime: %s, CurrentTime: %s, isPastEndTime: %s → %s",
                            round.getRoundNumber(),
                            round.getEndDateTime(),
                            currentTime,
                            isPastEndTime,
                            isPastEndTime ? "✅ Include (Past End)" : "❌ Skip (Still Active)"));

                    return isPastEndTime; // Chỉ include nếu đã quá endTime
                } else {
                    Log.d(TAG, "Round " + round.getRoundNumber() + " - Status: ACTIVE, No EndTime → ❌ Skip");
                    return false; // Active nhưng không có endTime → không include
                }

            default:
                // ✅ Additional check: nếu có endTime thì có thể coi là completed
                boolean hasEndTime = round.getEndTime() != null && !round.getEndTime().isEmpty();
                Log.d(TAG, "Round " + round.getRoundNumber() + " has unknown status '" + status +
                        "', hasEndTime: " + hasEndTime);
                return hasEndTime;
        }
    }

    /**
     * Load final attendance from API
     */
    public void loadListFinalAttendances() {
        Log.d(TAG, "🚀 loadListFinalAttendances() method ENTRY");

        // ✅ Check pre-conditions
        Log.d(TAG, "📋 Pre-check values:");
        Log.d(TAG, "    • session: " + (session != null ? "✅ Available" : "❌ NULL"));
        Log.d(TAG, "    • apiService: " + (apiService != null ? "✅ Available" : "❌ NULL"));
        Log.d(TAG, "    • context: " + (context != null ? "✅ Available" : "❌ NULL"));

        if (session == null) {
            Log.e(TAG, "❌ Session is null, cannot load final attendance");
            _errorMessage.setValue("Session data not available");
            return;
        }

        if (apiService == null) {
            Log.e(TAG, "❌ API service is null, cannot load final attendance");
            _errorMessage.setValue("API service not available");
            return;
        }

        String sessionId = getSessionId();
        Log.d(TAG, "========== loadListFinalAttendances() started ==========");
        Log.d(TAG, "📤 Loading final attendance for sessionId: " + sessionId);

        // ✅ Validate sessionId
        if (sessionId == null || sessionId.isEmpty() || "UNKNOWN_SESSION".equals(sessionId)) {
            Log.e(TAG, "❌ Invalid session ID: " + sessionId);
            _errorMessage.setValue("Invalid session ID");
            return;
        }

        try {
            Log.d(TAG, "🔄 Creating API call...");
            Call<AttendanceResponse> call = apiService.getListAttendances(sessionId);
            Log.d(TAG, "✅ API call created: " + call.request().url());

            call.enqueue(new Callback<AttendanceResponse>() {
                @Override
                public void onResponse(Call<AttendanceResponse> call, Response<AttendanceResponse> response) {
                    Log.d(TAG, "📥 Final Attendance API Response received");
                    Log.d(TAG, "  • Response code: " + response.code());
                    Log.d(TAG, "  • Response successful: " + response.isSuccessful());
                    Log.d(TAG, "  • Response body null: " + (response.body() == null));

                    if (response.isSuccessful() && response.body() != null) {
                        AttendanceResponse apiResponse = response.body();
                        Log.d(TAG, "  • API Success: " + apiResponse.isSuccess());
                        Log.d(TAG, "  • API Error: " + apiResponse.getError());
                        Log.d(TAG, "  • API Message: " + apiResponse.getMessage());

                        if (apiResponse.isSuccess()) {
                            Log.d(TAG, "🔄 Processing final attendance data...");

                            // Log raw API data
                            if (apiResponse.getData() != null) {
                                Log.d(TAG, "📋 Raw final attendance count: " + apiResponse.getData().size());
                                for (int i = 0; i < apiResponse.getData().size(); i++) {
                                    FinalAttendance student = apiResponse.getData().get(i);
                                    Log.d(TAG, String.format("  Student %d: ID=%s, Name=%s, Status=%s",
                                            (i + 1),
                                            student.getStudentId(),
                                            student.getStudentFullName(),
                                            student.getStatus()));
                                }
                            } else {
                                Log.w(TAG, "⚠️ Final attendance API data is null");
                            }

                            List<Attendance> finalAttendance = mapApiDataToFinalAttendance(apiResponse.getData());
                            Log.d(TAG, "📊 Mapped final attendance count: " + finalAttendance.size());

                            Log.d(TAG, "📡 Setting final attendance to LiveData...");
                            _listAttendance.setValue(finalAttendance);

                            Log.d(TAG, "✅ Successfully loaded " + finalAttendance.size() + " final attendance records");
                        } else {
                            String error = apiResponse.getError() != null ? apiResponse.getError() : "Unknown API error";
                            _errorMessage.setValue("Final Attendance: " + error);
                            Log.e(TAG, "❌ Final Attendance API Error: " + error);
                        }
                    } else {
                        String error = "HTTP Error: " + response.code();
                        if (response.errorBody() != null) {
                            try {
                                String errorBody = response.errorBody().string();
                                Log.e(TAG, "❌ Final Attendance Error body: " + errorBody);
                                error += " - " + errorBody;
                            } catch (Exception e) {
                                Log.e(TAG, "Failed to read final attendance error body", e);
                            }
                        }
                        _errorMessage.setValue("Final Attendance: " + error);
                        Log.e(TAG, "❌ Final Attendance " + error);
                    }
                    Log.d(TAG, "========== loadListFinalAttendances() response completed ==========");
                }

                @Override
                public void onFailure(Call<AttendanceResponse> call, Throwable t) {
                    String error = "Network Error: " + t.getMessage();
                    _errorMessage.setValue("Final Attendance: " + error);
                    Log.e(TAG, "❌ Final Attendance Network Error: " + error, t);
                    Log.e(TAG, "❌ Final Attendance Call URL: " + call.request().url());
                    Log.d(TAG, "========== loadListFinalAttendances() failed ==========");
                }
            });

            Log.d(TAG, "✅ Final attendance API call enqueued successfully");

        } catch (Exception e) {
            Log.e(TAG, "❌ Exception during final attendance API call setup", e);
            _errorMessage.setValue("Failed to load final attendance: " + e.getMessage());
        }

        Log.d(TAG, "========== loadListFinalAttendances() call setup completed ==========");
    }


    /**
     * Load attendance for specific round
     */
    public void loadListRoundAttendances(String roundId) {
        if (roundId == null || roundId.isEmpty()) {
            _errorMessage.setValue("Round ID not available");
            return;
        }

        _isLoadingRoundDetail.setValue(true);
        _errorMessage.setValue(null);

        Log.d(TAG, "Loading round attendance for round: " + roundId);

        apiService.getRoundResult(roundId)
                .enqueue(new Callback<RoundResultResponse>() {
                    @Override
                    public void onResponse(Call<RoundResultResponse> call, Response<RoundResultResponse> response) {
                        _isLoadingRoundDetail.setValue(false);

                        if (response.isSuccessful() && response.body() != null) {
                            RoundResultResponse apiResponse = response.body();

                            if (apiResponse.isSuccess()) {
                                RoundResultDto roundResult = apiResponse.getData();
                                _roundResult.setValue(roundResult);

                                List<Attendance> attendanceList = mapRoundResultToAttendanceList(roundResult);

                                // ✅ CHANGED: Set to round attendance LiveData instead of general attendance
                                _listRoundAttendance.setValue(attendanceList);
                                _currentRoundNumber.setValue(roundResult.getRoundNumber());

                                Log.d(TAG, "✅ Loaded round " + roundResult.getRoundNumber() +
                                        " attendance: " + roundResult.getAttendedCount() + "/" +
                                        roundResult.getTotalStudents() + " students present");

                            } else {
                                String error = apiResponse.getError() != null ? apiResponse.getError() : "Unknown API error";
                                _errorMessage.setValue("Round Attendance: " + error);
                                Log.e(TAG, "❌ Round Attendance API Error: " + error);
                            }
                        } else {
                            String error = "HTTP Error: " + response.code();
                            _errorMessage.setValue("Round Attendance: " + error);
                            Log.e(TAG, "❌ Round Attendance " + error);
                        }
                    }

                    @Override
                    public void onFailure(Call<RoundResultResponse> call, Throwable t) {
                        _isLoadingRoundDetail.setValue(false);
                        String error = "Network Error: " + t.getMessage();
                        _errorMessage.setValue("Round Attendance: " + error);
                        Log.e(TAG, "❌ Round Attendance Network Error", t);
                    }
                });
    }

    private List<Attendance> mapRoundResultToAttendanceList(RoundResultDto roundResult) {
        List<Attendance> attendanceList = new ArrayList<>();

        if (roundResult == null || roundResult.getStudentsAttendance() == null) {
            return attendanceList;
        }

        // ✅ Get current user ID to exclude lecturer
        String currentUserId = null;
        if (authManager != null) {
            currentUserId = authManager.getCurrentUserId();
            Log.d(TAG, "Current user ID (lecturer): " + currentUserId);
        }

        for (StudentAttendanceDto studentDto : roundResult.getStudentsAttendance()) {
            try {
                // ✅ Skip lecturer's record
                if (currentUserId != null && currentUserId.equals(studentDto.getStudentId())) {
                    Log.d(TAG, "⏭️ Skipping lecturer record: " + studentDto.getDisplayName() +
                            " (ID: " + studentDto.getStudentId() + ")");
                    continue;
                }

                Attendance attendance = new Attendance();

                // ✅ Map basic student info
                attendance.setStudentId(studentDto.getStudentId());
                attendance.setStudentName(studentDto.getDisplayName());
                attendance.setFinalStatus(studentDto.isAttended());
                attendance.setRoundNumber(roundResult.getRoundNumber());
                attendanceList.add(attendance);

                Log.d(TAG, "✅ Added student: " + studentDto.getDisplayName() +
                        " (ID: " + studentDto.getStudentId() + ")");

            } catch (Exception e) {
                Log.e(TAG, "Error mapping round attendance for student: " + studentDto.getFullName(), e);
            }
        }

        Log.d(TAG, "✅ Mapped " + attendanceList.size() + " students for round " + roundResult.getRoundNumber() +
                " (excluding lecturer)");
        return attendanceList;
    }

    private List<Round> mapApiDataToAttendanceRounds(List<RoundDetail> apiData) {
        List<Round> rounds = new ArrayList<>();

        if (apiData == null || apiData.isEmpty()) {
            Log.w(TAG, "No round data to map");
            return rounds;
        }

        SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault());

        for (RoundDetail apiRound : apiData) {
            try {
                Round round = new Round();

                // ✅ Map tất cả các field từ server
                round.setRoundId(apiRound.getRoundId());
                round.setSessionId(apiRound.getSessionId());
                round.setRoundNumber(apiRound.getRoundNumber());
                round.setAttendedCount(apiRound.getAttendedCount());
                round.setTotalStudents(apiRound.getTotalStudents());
                round.setStatus(apiRound.getStatus());
                round.setStartTime(apiRound.getStartTime());
                round.setEndTime(apiRound.getEndTime());

                // ✅ Parse dates cho dễ sử dụng
                try {
                    Date startDateTime = format.parse(apiRound.getStartTime());
                    round.setStartDateTime(startDateTime);

                    if (apiRound.getEndTime() != null && !apiRound.getEndTime().isEmpty()) {
                        Date endDateTime = format.parse(apiRound.getEndTime());
                        round.setEndDateTime(endDateTime);
                    }
                } catch (Exception dateException) {
                    Log.e(TAG, "Error parsing dates for round " + apiRound.getRoundNumber(), dateException);
                }

                rounds.add(round);

                Log.d(TAG, "✅ Mapped round " + apiRound.getRoundNumber() +
                        " - Status: " + apiRound.getStatus() +
                        " - Attendance: " + apiRound.getAttendedCount() + "/" + apiRound.getTotalStudents() +
                        " - Start: " + apiRound.getStartTime() +
                        " - End: " + apiRound.getEndTime());

            } catch (Exception e) {
                Log.e(TAG, "❌ Error mapping round " + apiRound.getRoundNumber(), e);
            }
        }

        Log.d(TAG, "✅ Successfully mapped " + rounds.size() + " out of " + apiData.size() + " rounds");
        return rounds;
    }

    private List<Attendance> mapApiDataToFinalAttendance(List<FinalAttendance> apiData) {
        List<Attendance> finalAttendanceList = new ArrayList<>();

        if (apiData == null || apiData.isEmpty()) {
            return finalAttendanceList;
        }

        for (FinalAttendance apiStudent : apiData) {
            try {
                Attendance student = new Attendance();

                student.setStudentId(apiStudent.getStudentId());
                student.setStudentName(apiStudent.getStudentFullName());

                boolean isPresent = isStudentPresent(apiStudent.getStatus());                student.setFinalStatus(isPresent);
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

    private boolean isStudentPresent(String status) {
        if ("Present".equalsIgnoreCase(status) || "Attended".equalsIgnoreCase(status)) {
            return true;
        }
        return false;
    }

    private boolean isSessionEnded() {
        try {
            if (session == null) return true;
            String status = session.getSessionStatus();
            if (status != null && !"Active".equalsIgnoreCase(status)) return true;
            Date end = session.getEndTimeAsDate();
            if (end != null && new Date().after(end)) return true;
        } catch (Exception ignored) {}
        return false;
    }

    private int getTotalRoundsFromRoundsData() {
        List<Round> rounds = _listHistoryRounds.getValue();
        if (rounds != null && !rounds.isEmpty()) {
            return rounds.size();
        }
        return 4;
    }

    private boolean isSessionCompleted(SessionDetailInfoRound sessionInfo) {
        if (sessionInfo == null) return true;

        String status = sessionInfo.getStatus();
        if (status == null) return true;

        boolean isActive = "Active".equalsIgnoreCase(status);
        boolean isCompleted = !isActive;

        Log.d(TAG, "Session status: " + status + " → Is completed: " + isCompleted);

        return isCompleted;
    }
    public void refreshData() {
        loadListRounds();
        loadListFinalAttendances();
    }

    @Override
    protected void onCleared() {
        super.onCleared();

        if (attendanceCalculatedReceiver != null && context != null) {
            androidx.localbroadcastmanager.content.LocalBroadcastManager
                    .getInstance(context)
                    .unregisterReceiver(attendanceCalculatedReceiver);

            Log.d(TAG, "Attendance calculated receiver unregistered");
        }
    }

    public void endSession() {
        String sessionId = getSessionId();
        String userId = authManager.getCurrentUserId();

        if (sessionId == null) {
            _errorMessage.setValue("Session ID not available");
            return;
        }

        if (userId == null) {
            _errorMessage.setValue("User ID not available");
            return;
        }

        _isEndingSession.setValue(true);
        _errorMessage.setValue(null);

        Log.d(TAG, "Ending session: " + sessionId + " for user: " + userId);

        // Tạo request body
        EndSessionRequest requestBody = new EndSessionRequest(userId);

        apiService.endSession(sessionId, requestBody)
                .enqueue(new Callback<EndSessionResponse>() {
                    @Override
                    public void onResponse(Call<EndSessionResponse> call, Response<EndSessionResponse> response) {
                        _isEndingSession.setValue(false);

                        if (response.isSuccessful() && response.body() != null) {
                            EndSessionResponse apiResponse = response.body();
                            Log.d(TAG, "🔍 API Response Debug:");
                            Log.d(TAG, "  • apiResponse.isSuccess(): " + apiResponse.isSuccess());
                            Log.d(TAG, "  • apiResponse.getData(): " + (apiResponse.getData() != null));
                            Log.d(TAG, "  • apiResponse.getError(): " + apiResponse.getError());
                            Log.d(TAG, "  • apiResponse.getMessage(): " + apiResponse.getMessage());
                            if (apiResponse.isSuccess()) {
                                _endSessionResult.setValue(apiResponse);
                                Log.d(TAG, "✅ Session ended successfully: " + apiResponse.getMessage());

                                // Stop BLE Service
                                stopBLEAttendanceService();

                                // Revoke active Face ID verification requests for this session (best-effort)
                                try {
                                    if (!activeFaceIdRequestIds.isEmpty()) {
                                        for (String reqId : new ArrayList<>(activeFaceIdRequestIds)) {
                                            faceIdApiController.cancelFaceIdRequest(reqId).enqueue(new retrofit2.Callback<Void>() {
                                                @Override
                                                public void onResponse(retrofit2.Call<Void> call, retrofit2.Response<Void> resp) {
                                                    android.util.Log.d(TAG, "Cancelled FaceId request: " + reqId);
                                                    activeFaceIdRequestIds.remove(reqId);
                                                }

                                                @Override
                                                public void onFailure(retrofit2.Call<Void> call, Throwable t) {
                                                    android.util.Log.e(TAG, "Cancel FaceId request failed: " + reqId, t);
                                                }
                                            });
                                        }
                                    }
                                } catch (Exception e) {
                                    android.util.Log.e(TAG, "Error revoking FaceId requests on end session", e);
                                }

                            } else {
                                String error = apiResponse.getError() != null ? apiResponse.getError() : "Failed to end session";
                                _errorMessage.setValue("End Session: " + error);
                                Log.e(TAG, "❌ End Session API Error: " + error);
                            }
                        } else {
                            String error = "HTTP Error: " + response.code();
                            _errorMessage.setValue("End Session: " + error);
                            Log.e(TAG, "❌ End Session " + error);
                        }
                    }

                    @Override
                    public void onFailure(Call<EndSessionResponse> call, Throwable t) {
                        _isEndingSession.setValue(false);
                        String error = "Network Error: " + t.getMessage();
                        _errorMessage.setValue("End Session: " + error);
                        Log.e(TAG, "❌ End Session Network Error", t);
                    }
                });
    }

    // Method để stop BLE Service
    private void stopBLEAttendanceService() {
        try {
            Intent serviceIntent = new Intent(context, BLEAttendanceService.class);
            serviceIntent.setAction("STOP_ATTENDANCE");
            context.stopService(serviceIntent);
            Log.d(TAG, "✅ BLE Attendance Service stopped");
        } catch (Exception e) {
            Log.e(TAG, "Failed to stop BLE service", e);
        }
    }

    // ================= Face ID Request Creation =================
    public void createFaceIdRequest(int expiresInMinutes, String title, String body) {
        if (session == null) {
            _faceIdRequestError.setValue("Session not available");
            return;
        }
        // Block creating Face ID request if the session is already ended
        if (isSessionEnded()) {
            _faceIdRequestError.setValue("Session already ended. Cannot create Face ID request.");
            return;
        }
        if (authManager == null || !authManager.isLoggedIn()) {
            _faceIdRequestError.setValue("Lecturer not authenticated");
            return;
        }
        if (faceIdApiController == null) {
            _faceIdRequestError.setValue("FaceId API service unavailable");
            return;
        }

        _isCreatingFaceIdRequest.setValue(true);
        _faceIdRequestError.setValue(null);
        _faceIdRequestSuccess.setValue(null);

        // Validate and resolve required identifiers
        String lecturerId = session.getLecturerId();
        if (lecturerId == null || lecturerId.trim().isEmpty()) {
            // Fallback to current authenticated user as lecturer
            String currentUserId = authManager != null ? authManager.getCurrentUserId() : null;
            if (currentUserId != null && !currentUserId.trim().isEmpty()) {
                lecturerId = currentUserId;
                Log.w(TAG, "LecturerId missing in session. Falling back to current userId=" + lecturerId);
            } else {
                _isCreatingFaceIdRequest.setValue(false);
                _faceIdRequestError.setValue("Lecturer ID not available");
                Log.e(TAG, "Cannot create Face ID request: lecturerId is null/empty");
                return;
            }
        }

        String sessionId = session.getSessionId();
        if (sessionId == null || sessionId.trim().isEmpty()) {
            _isCreatingFaceIdRequest.setValue(false);
            _faceIdRequestError.setValue("Session ID not available");
            Log.e(TAG, "Cannot create Face ID request: sessionId is null/empty");
            return;
        }

        String classSectionId = session.getClassSectionId();
        if (classSectionId == null || classSectionId.trim().isEmpty()) {
            _isCreatingFaceIdRequest.setValue(false);
            _faceIdRequestError.setValue("Class section ID not available");
            Log.e(TAG, "Cannot create Face ID request: classSectionId is null/empty");
            return;
        }

        FaceIdRequestCreateRequest request = new FaceIdRequestCreateRequest(
                lecturerId,
                sessionId,
                classSectionId,
                expiresInMinutes,
                title,
                body
        );

        Log.d(TAG, "Creating Face ID request: session=" + session.getSessionId() + " expiresIn=" + expiresInMinutes + "m");
        faceIdApiController.createFaceIdRequest(request)
                .enqueue(new Callback<FaceIdRequestCreateResponse>() {
                    @Override
                    public void onResponse(Call<FaceIdRequestCreateResponse> call, Response<FaceIdRequestCreateResponse> response) {
                        _isCreatingFaceIdRequest.setValue(false);
                        if (response.isSuccessful() && response.body() != null) {
                            FaceIdRequestCreateResponse apiResponse = response.body();
                            if (apiResponse.isEffectiveSuccess()) {
                                String msg = apiResponse.getMessage() != null ? apiResponse.getMessage() : "Face ID request created";
                                _faceIdRequestSuccess.setValue(msg);
                                Log.d(TAG, "Face ID request created successfully");
                                // Cache requestId locally for later revoke on end session
                                try {
                                    String reqId = apiResponse.getRequestId();
                                    if ((reqId == null || reqId.isEmpty()) && apiResponse.getData() != null) {
                                        reqId = apiResponse.getData().getRequestId();
                                    }
                                    if (reqId != null && !reqId.isEmpty()) {
                                        activeFaceIdRequestIds.add(reqId);
                                        Log.d(TAG, "Cached active FaceId requestId: " + reqId);
                                    }
                                } catch (Exception ignored) {}

                                // Publish verify deadline for downstream (optional consumption)
                                try {
                                    long deadlineMs = System.currentTimeMillis() + (expiresInMinutes * 60L * 1000L);
                                    Log.d(TAG, "Verify window deadline (ms): " + deadlineMs);
                                    // If you have a shared event bus / navigation, pass deadline here.
                                } catch (Exception ignored) {}
                            } else {
                                // Try to extract success from root-level fields even if Success=false
                                if (apiResponse.getRequestId() != null && !apiResponse.getRequestId().isEmpty()) {
                                    String msg = apiResponse.getMessage() != null ? apiResponse.getMessage() : "Face ID request created";
                                    _faceIdRequestSuccess.setValue(msg);
                                    Log.w(TAG, "Face ID request treated as success via root fields: requestId=" + apiResponse.getRequestId());
                                    return;
                                }
                                String err = apiResponse.getError() != null ? apiResponse.getError() : "Failed to create Face ID request";
                                _faceIdRequestError.setValue(err);
                                Log.e(TAG, "Face ID request API error: " + err);
                            }
                        } else {
                            String err = "HTTP Error: " + response.code();
                            _faceIdRequestError.setValue(err);
                            Log.e(TAG, "Face ID request " + err);
                        }
                    }

                    @Override
                    public void onFailure(Call<FaceIdRequestCreateResponse> call, Throwable t) {
                        _isCreatingFaceIdRequest.setValue(false);
                        String err = "Network Error: " + t.getMessage();
                        _faceIdRequestError.setValue(err);
                        Log.e(TAG, "Face ID request network error", t);
                    }
                });
    }
}
