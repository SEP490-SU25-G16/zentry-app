package vn.edu.fpt.zentryapp.lecturer.ui.home;

import androidx.lifecycle.*;
import android.content.Context;
import java.util.*;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

import vn.edu.fpt.zentryapp.auth.client.ApiClient;
import vn.edu.fpt.zentryapp.auth.client.AuthManager;
import vn.edu.fpt.zentryapp.lecturer.data.api.LecturerApiService;
import vn.edu.fpt.zentryapp.lecturer.data.model.response.ClassSectionData;
import vn.edu.fpt.zentryapp.lecturer.data.model.response.ClassSectionResponse;
import vn.edu.fpt.zentryapp.lecturer.data.model.response.ExamModel;
import vn.edu.fpt.zentryapp.lecturer.data.model.response.SessionModel;
import vn.edu.fpt.zentryapp.lecturer.data.model.response.WeeklyModel;

public class LecturerHomeViewModel extends ViewModel {

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
        if (authManager == null) return;

        String lecturerId = authManager.getCurrentUserId();
        if (lecturerId == null) {
            _errorMessage.setValue("User not authenticated");
            return;
        }

        _isLoading.setValue(true);
        _errorMessage.setValue(null);

        apiService.getTodayClasses(lecturerId).enqueue(new Callback<ClassSectionResponse>() {
            @Override
            public void onResponse(Call<ClassSectionResponse> call, Response<ClassSectionResponse> response) {
                _isLoading.setValue(false);

                if (response.isSuccessful() && response.body() != null) {
                    ClassSectionResponse apiResponse = response.body();
                    if (apiResponse.isSuccess() && apiResponse.getData() != null) {
                        processClassSections(apiResponse.getData());
                    } else {
                        _errorMessage.setValue(apiResponse.getError() != null ?
                                apiResponse.getError() : "No classes found for today");
                    }
                } else {
                    _errorMessage.setValue("Failed to load today's classes");
                }
            }

            @Override
            public void onFailure(Call<ClassSectionResponse> call, Throwable t) {
                _isLoading.setValue(false);
                _errorMessage.setValue("Network error: " + t.getMessage());
            }
        });
    }

    private void processClassSections(List<ClassSectionData> classSections) {
        // Convert API data to UI models
        List<SessionModel> sessionList = convertToSessionModels(classSections);
        List<WeeklyModel> weeklyList = convertToWeeklyModels(classSections);

        _sessions.setValue(sessionList);
        _weekly.setValue(weeklyList);

        // Keep exams empty for now (no exam data in this API)
        _exams.setValue(new ArrayList<>());
    }

    private List<SessionModel> convertToSessionModels(List<ClassSectionData> classSections) {
        List<SessionModel> sessions = new ArrayList<>();

        for (ClassSectionData classSection : classSections) {
            // Create session title
            String sessionTitle = classSection.getCourseName() + " - " + classSection.getSectionCode();

            // Get schedule info
            String scheduleInfo = "";
            if (classSection.getSchedules() != null && !classSection.getSchedules().isEmpty()) {
                ClassSectionData.Schedule schedule = classSection.getSchedules().get(0);
                scheduleInfo = schedule.getScheduleInfo() + " | " + schedule.getRoomInfo();
            }

            // Calculate time remaining (you may need to implement this based on schedule time)
            String timeRemaining = "Starting soon"; // Placeholder

            sessions.add(new SessionModel(sessionTitle, scheduleInfo, timeRemaining));
        }

        return sessions;
    }

    private List<WeeklyModel> convertToWeeklyModels(List<ClassSectionData> classSections) {
        List<WeeklyModel> weeklyList = new ArrayList<>();

        for (ClassSectionData classSection : classSections) {
            String courseTitle = classSection.getCourseName() + " - " + classSection.getSectionCode();

            // Parse session progress (e.g., "1" means session 1)
            int currentSession = 1;
            try {
                currentSession = Integer.parseInt(classSection.getSessionProgress());
            } catch (NumberFormatException e) {
                // Keep default
            }

            String presented = currentSession + "/" + classSection.getTotalSessions() + " Presented";
            String sessions = (currentSession - 1) + "/" + classSection.getTotalSessions() + " Sessions";

            // Calculate completion percentage
            double completionRate = ((double) (currentSession - 1) / classSection.getTotalSessions()) * 100;
            String completion = String.format("%.0f%%", completionRate);

            weeklyList.add(new WeeklyModel(courseTitle, presented, sessions, completion));
        }

        return weeklyList;
    }
}
