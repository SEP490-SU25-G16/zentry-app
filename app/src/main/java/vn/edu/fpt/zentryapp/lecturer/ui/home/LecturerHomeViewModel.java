package vn.edu.fpt.zentryapp.lecturer.ui.home;

import androidx.lifecycle.*;

import android.annotation.SuppressLint;
import android.content.Context;
import android.util.Log;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.TimeUnit;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

import vn.edu.fpt.zentryapp.auth.client.ApiClient;
import vn.edu.fpt.zentryapp.auth.client.AuthManager;
import vn.edu.fpt.zentryapp.lecturer.data.api.LecturerApiService;
import vn.edu.fpt.zentryapp.lecturer.data.model.response.ExamModel;
import vn.edu.fpt.zentryapp.lecturer.data.model.response.SessionModel;
import vn.edu.fpt.zentryapp.lecturer.data.model.response.WeeklyModel;
import vn.edu.fpt.zentryapp.lecturer.data.model.responsedto.ApiResponseDto;
import vn.edu.fpt.zentryapp.lecturer.data.model.responsedto.HomeDataDto;
import vn.edu.fpt.zentryapp.lecturer.data.model.responsedto.NextSessionDto;
import vn.edu.fpt.zentryapp.lecturer.data.model.responsedto.WeeklyOverviewDto;

public class LecturerHomeViewModel extends ViewModel {
    private final String TAG = "LecturerHomeViewModel";
    private final MutableLiveData<List<ExamModel>> _exams = new MutableLiveData<>();
    private final MutableLiveData<List<SessionModel>> _sessions = new MutableLiveData<>();
    private final MutableLiveData<List<WeeklyModel>> _weekly = new MutableLiveData<>();
    private final MutableLiveData<String> _errorMessage = new MutableLiveData<>();
    private final MutableLiveData<Boolean> _isLoading = new MutableLiveData<>();

    // API service
    private LecturerApiService apiService;
    private AuthManager authManager;

    public LiveData<List<ExamModel>> exams() { return _exams; }
    public LiveData<List<SessionModel>> sessions() { return _sessions; }
    public LiveData<List<WeeklyModel>> weekly() { return _weekly; }
    public LiveData<String> errorMessage() { return _errorMessage; }
    public LiveData<Boolean> isLoading() { return _isLoading; }

    public void init(Context context, AuthManager authManager) {
        this.authManager = authManager;
        this.apiService = ApiClient.getClient(context).create(LecturerApiService.class);
    }

    public void loadTodayClasses() {
        _isLoading.setValue(true);

        // Get lecturer ID from AuthManager
        String lecturerId = authManager.getCurrentUserId(); // Assume you have this method

        Call<ApiResponseDto<HomeDataDto>> call = apiService.getHomeData(lecturerId);
        call.enqueue(new Callback<ApiResponseDto<HomeDataDto>>() {
            @Override
            public void onResponse(Call<ApiResponseDto<HomeDataDto>> call, Response<ApiResponseDto<HomeDataDto>> response) {
                _isLoading.setValue(false);

                if (response.isSuccessful() && response.body() != null) {
                    ApiResponseDto<HomeDataDto> apiResponse = response.body();

                    if (apiResponse.isSuccess() && apiResponse.getData() != null) {
                        processHomeData(apiResponse.getData());
                    } else {
                        _errorMessage.setValue(apiResponse.getError() != null ?
                                apiResponse.getError() : "Unknown error occurred");
                    }
                } else {
                    _errorMessage.setValue("Failed to load data: " + response.message());
                }
            }

            @Override
            public void onFailure(Call<ApiResponseDto<HomeDataDto>> call, Throwable t) {
                _isLoading.setValue(false);
                _errorMessage.setValue("Network error: " + t.getMessage());
                Log.e(TAG, "API call failed", t);
            }
        });
    }

    private void processHomeData(HomeDataDto homeData) {
        // Convert API data to UI models
        List<SessionModel> sessionList = mapNextSessionsToSessionModels(homeData.getNextSessions());
        List<WeeklyModel> weeklyList = mapWeeklyOverviewToWeeklyModels(homeData.getWeeklyOverview());

        _sessions.setValue(sessionList);
        _weekly.setValue(weeklyList);

        _exams.setValue(new ArrayList<>());
    }

    private List<SessionModel> mapNextSessionsToSessionModels(List<NextSessionDto> nextSessions) {
        List<SessionModel> result = new ArrayList<>();

        if (nextSessions == null) return result;

        for (NextSessionDto session : nextSessions) {
            String title = session.getClassTitle();
            String scheduleInfo = formatScheduleInfo(session);
            String timeRemaining = calculateTimeRemaining(session);

            result.add(new SessionModel(title, scheduleInfo, timeRemaining));
        }

        return result;
    }

    private List<WeeklyModel> mapWeeklyOverviewToWeeklyModels(List<WeeklyOverviewDto> weeklyData) {
        List<WeeklyModel> result = new ArrayList<>();

        if (weeklyData == null) return result;

        for (WeeklyOverviewDto week : weeklyData) {
            String classTitle = week.getClassName(); // "Introduction to Computer Science - SE705"

            // Dòng trên: Hiển thị số học sinh trong lớp
            String presented = week.getEnrolledStudents() + " Students";

            String sessions = week.getSessionsThisWeek() + " Session";

            // Attendance rate hoặc có thể để trống nếu không cần
            String completion = String.format("%.0f%%", week.getAttendanceRate());

            result.add(new WeeklyModel(classTitle, presented, sessions, completion));
        }

        return result;
    }

    @SuppressLint("DefaultLocale")
    private String formatScheduleInfo(NextSessionDto session) {
        return String.format("%s %s - %s | %s",
                session.getStartDate(),
                session.getStartTime(),
                session.getEndTime(),
                session.getRoomInfo()
        );
    }

    private String calculateTimeRemaining(NextSessionDto session) {
        try {
            SimpleDateFormat dateTimeFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault());
            String startDateTime = session.getStartDate() + " " + session.getStartTime();
            Date sessionStart = dateTimeFormat.parse(startDateTime);
            Date now = new Date();

            if (sessionStart == null) return "Unknown";

            long diffInMillis = sessionStart.getTime() - now.getTime();

            if (diffInMillis <= 0) {
                return "Started";
            } else if (diffInMillis < TimeUnit.HOURS.toMillis(1)) {
                long minutes = TimeUnit.MILLISECONDS.toMinutes(diffInMillis);
                return minutes + " min left";
            } else if (diffInMillis < TimeUnit.DAYS.toMillis(1)) {
                long hours = TimeUnit.MILLISECONDS.toHours(diffInMillis);
                return hours + " hour(s) left";
            } else {
                long days = TimeUnit.MILLISECONDS.toDays(diffInMillis);
                return days + " day(s) left";
            }
        } catch (ParseException e) {
            Log.e(TAG, "Error parsing date", e);
            return "Starting soon";
        }
    }
}
