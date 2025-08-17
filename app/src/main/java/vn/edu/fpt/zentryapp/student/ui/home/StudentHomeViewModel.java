package vn.edu.fpt.zentryapp.student.ui.home;

import androidx.lifecycle.*;
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
import vn.edu.fpt.zentryapp.lecturer.data.model.response.ExamModel;
import vn.edu.fpt.zentryapp.lecturer.data.model.response.SessionModel;
import vn.edu.fpt.zentryapp.lecturer.data.model.response.WeeklyModel;
import vn.edu.fpt.zentryapp.lecturer.data.model.responsedto.ApiResponseDto;
import vn.edu.fpt.zentryapp.student.data.api.StudentApiService;
import vn.edu.fpt.zentryapp.student.data.model.responsedto.StudentHomeDataDto;
import vn.edu.fpt.zentryapp.student.data.model.responsedto.StudentNextSessionDto;
import vn.edu.fpt.zentryapp.student.data.model.responsedto.StudentWeeklyReviewDto;
import vn.edu.fpt.zentryapp.student.data.model.responsedto.WeeklyCourseDto;

public class StudentHomeViewModel extends ViewModel {
    private final String TAG = "StudentHomeViewModel";

    private final MutableLiveData<List<ExamModel>> _exams = new MutableLiveData<>();
    private final MutableLiveData<List<SessionModel>> _sessions = new MutableLiveData<>();
    private final MutableLiveData<List<WeeklyModel>> _weekly = new MutableLiveData<>();
    private final MutableLiveData<String> _errorMessage = new MutableLiveData<>();
    private final MutableLiveData<Boolean> _isLoading = new MutableLiveData<>();

    // API service
    private StudentApiService apiService;
    private AuthManager authManager;

    public LiveData<List<ExamModel>> exams() { return _exams; }
    public LiveData<List<SessionModel>> sessions() { return _sessions; }
    public LiveData<List<WeeklyModel>> weekly() { return _weekly; }
    public LiveData<String> errorMessage() { return _errorMessage; }
    public LiveData<Boolean> isLoading() { return _isLoading; }

    public void init(Context context, AuthManager authManager) {
        this.authManager = authManager;
        this.apiService = ApiClient.getClient(context).create(StudentApiService.class);
    }

    public void loadStudentHomeData() {
        _isLoading.setValue(true);

        String studentId = authManager.getCurrentUserId();

        Call<ApiResponseDto<StudentHomeDataDto>> call = apiService.getStudentHomeData(studentId);
        call.enqueue(new Callback<ApiResponseDto<StudentHomeDataDto>>() {
            @Override
            public void onResponse(Call<ApiResponseDto<StudentHomeDataDto>> call,
                                   Response<ApiResponseDto<StudentHomeDataDto>> response) {
                _isLoading.setValue(false);

                if (response.isSuccessful() && response.body() != null) {
                    ApiResponseDto<StudentHomeDataDto> apiResponse = response.body();

                    if (apiResponse.isSuccess() && apiResponse.getData() != null) {
                        processStudentHomeData(apiResponse.getData());
                    } else {
                        _errorMessage.setValue(apiResponse.getError() != null ?
                                apiResponse.getError() : "Failed to load home data");
                    }
                }
            }

            @Override
            public void onFailure(Call<ApiResponseDto<StudentHomeDataDto>> call, Throwable t) {
                _isLoading.setValue(false);
                _errorMessage.setValue("Network error: " + t.getMessage());
                Log.e(TAG, "API call failed", t);
            }
        });
    }

    private void processStudentHomeData(StudentHomeDataDto homeData) {
        // Convert API data to UI models
        List<SessionModel> sessionList = mapNextSessionsToSessionModels(homeData.getNextSessions());
        List<WeeklyModel> weeklyList = mapWeeklyReviewToWeeklyModels(homeData.getWeeklyReview());

        _sessions.setValue(sessionList);
        _weekly.setValue(weeklyList);

        // Keep exams empty for now (no exam data in this API)
        _exams.setValue(new ArrayList<>());
    }

    private List<SessionModel> mapNextSessionsToSessionModels(List<StudentNextSessionDto> nextSessions) {
        List<SessionModel> result = new ArrayList<>();

        if (nextSessions == null) return result;

        for (StudentNextSessionDto session : nextSessions) {
            String title = session.getClassTitle();
            String scheduleInfo = formatScheduleInfo(session);
            String timeRemaining = calculateTimeRemaining(session);

            result.add(new SessionModel(title, scheduleInfo, timeRemaining));
        }

        return result;
    }

    private List<WeeklyModel> mapWeeklyReviewToWeeklyModels(StudentWeeklyReviewDto weeklyReview) {
        List<WeeklyModel> result = new ArrayList<>();

        if (weeklyReview == null || weeklyReview.getCourses() == null) return result;

        for (WeeklyCourseDto course : weeklyReview.getCourses()) {
            String courseTitle = course.getCourseName() + " - " + course.getSectionCode();
            String attended = course.getAttendedSessions() + "/" + course.getTotalSessionsInWeek() + " Attended";
            String sessions = course.getAttendedSessions() + "/" + course.getTotalSessionsInWeek();
            String completion = String.format("%.0f%%", course.getAttendancePercentage());

            result.add(new WeeklyModel(courseTitle, attended, sessions, completion));
        }

        return result;
    }

    private String formatScheduleInfo(StudentNextSessionDto session) {
        return String.format("%s %s - %s | %s | %s",
                session.getStartDate(),
                session.getStartTime(),
                session.getEndTime(),
                session.getRoomInfo(),
                session.getLecturerName()
        );
    }

    private String calculateTimeRemaining(StudentNextSessionDto session) {
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

    public void refreshData() {
        loadStudentHomeData();
    }
}
