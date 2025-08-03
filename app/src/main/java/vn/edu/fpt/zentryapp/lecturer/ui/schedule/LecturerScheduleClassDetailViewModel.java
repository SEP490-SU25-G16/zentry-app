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
import vn.edu.fpt.zentryapp.service.BLEAttendanceService;
import vn.edu.fpt.zentryapp.student.data.model.response.ClassSectionDetailDto;
import vn.edu.fpt.zentryapp.student.data.model.response.ClassSectionDetailResponse;
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
    private final MutableLiveData<Boolean> _canAddFaceId = new MutableLiveData<>(true);

    // API Service
    private AttendanceApiService apiService;
    private AuthManager authManager;
    @SuppressLint("StaticFieldLeak")
    private Context context;
    private LecturerScheduleClassSection session;
    // Public getters
    public LiveData<SessionDetailInfoRound> sessionInfo() { return _sessionInfo; }
    public LiveData<List<Round>> listHistoryRounds() { return _listHistoryRounds; }
    public LiveData<List<Attendance>> listAttendance() { return _listAttendance; }

    public LiveData<String> errorMessage() { return _errorMessage; }
    public LiveData<Boolean> canAddFaceId() { return _canAddFaceId; }

    public void init(Context context, AuthManager authManager, LecturerScheduleClassSection session) {
        this.context = context;
        this.authManager = authManager;
        this.session = session;
        this.apiService = ApiClient.getClient(context).create(AttendanceApiService.class);

        setupAttendanceCalculatedReceiver();
        loadSessionInfo();
        loadListRounds();
        loadListFinalAttendances();
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

                                // ✅ FIXED: Check session status từ API response
                                boolean canAddFaceId = "Active".equalsIgnoreCase(apiResponse.getData().getSessionStatus());
                                _canAddFaceId.setValue(canAddFaceId);

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
     * Load attendance rounds from API
     */
    private void loadListRounds() {
        if (session == null) {
            _errorMessage.setValue("Session data not available");
            return;
        }
        _errorMessage.setValue(null);

        String sessionId = getSessionId();
        Log.d(TAG, "Loading attendance rounds for session: " + sessionId);

        apiService.getListRounds(sessionId)
                .enqueue(new Callback<RoundsDataResponse>() {
                    @Override
                    public void onResponse(Call<RoundsDataResponse> call, Response<RoundsDataResponse> response) {
                        if (response.isSuccessful() && response.body() != null) {
                            RoundsDataResponse apiResponse = response.body();

                            if (apiResponse.isSuccess()) {
                                // ✅ UPDATED: Map và filter chỉ completed rounds
                                List<Round> allRounds = mapApiDataToAttendanceRounds(apiResponse.getData());
                                List<Round> completedRounds = filterCompletedRounds(allRounds);

                                _listHistoryRounds.setValue(completedRounds);

                                Log.d(TAG, "✅ Loaded " + allRounds.size() + " total rounds, " +
                                        completedRounds.size() + " completed rounds displayed");

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
                        Log.e(TAG, "❌ Network Error", t);
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

        // ✅ Check các status values có thể indicate completed
        switch (status.toLowerCase()) {
            case "completed":
                return true;

            case "active":
                return false;

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
        String sessionId = getSessionId();
        Log.d(TAG, "Loading final attendance for session: " + sessionId);
        // TODO: Uncomment when API is ready
        apiService.getListAttendances(sessionId)
                .enqueue(new Callback<AttendanceResponse>() {
                    @Override
                    public void onResponse(Call<AttendanceResponse> call, Response<AttendanceResponse> response) {
                        if (response.isSuccessful() && response.body() != null) {
                            AttendanceResponse apiResponse = response.body();

                            if (apiResponse.isSuccess()) {
                                List<Attendance> finalAttendance = mapApiDataToFinalAttendance(apiResponse.getData());
                                _listAttendance.setValue(finalAttendance);

                                Log.d(TAG, "Loaded " + finalAttendance.size() + " final attendance records");
                            } else {
                                String error = apiResponse.getError() != null ? apiResponse.getError() : "Unknown API error";
                                _errorMessage.setValue(error);
                                Log.e(TAG, "Final Attendance API Error: " + error);
                            }
                        } else {
                            String error = "HTTP Error: " + response.code();
                            _errorMessage.setValue(error);
                            Log.e(TAG, "Final Attendance HTTP Error: " + error);
                        }
                    }

                    @Override
                    public void onFailure(Call<AttendanceResponse> call, Throwable t) {
                        String error = "Network Error: " + t.getMessage();
                        _errorMessage.setValue(error);
                        Log.e(TAG, "Final Attendance Network Error", t);
                    }
                });
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
                                _listAttendance.setValue(attendanceList);

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

        for (StudentAttendanceDto studentDto : roundResult.getStudentsAttendance()) {
            try {
                Attendance attendance = new Attendance();

                // ✅ Map basic student info
                attendance.setStudentId(studentDto.getStudentId());
                attendance.setStudentName(studentDto.getDisplayName());
                attendance.setFinalStatus(studentDto.isAttended());
                attendance.setRoundNumber(roundResult.getRoundNumber());
                attendanceList.add(attendance);

            } catch (Exception e) {
                Log.e(TAG, "Error mapping round attendance for student: " + studentDto.getFullName(), e);
            }
        }

        Log.d(TAG, "✅ Mapped " + attendanceList.size() + " students for round " + roundResult.getRoundNumber());
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
}
