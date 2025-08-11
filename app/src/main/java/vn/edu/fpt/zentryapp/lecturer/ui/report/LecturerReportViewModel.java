package vn.edu.fpt.zentryapp.lecturer.ui.report;

import android.util.Log;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import java.util.ArrayList;
import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import vn.edu.fpt.zentryapp.auth.client.AuthManager;
import vn.edu.fpt.zentryapp.auth.client.ApiClient;
import vn.edu.fpt.zentryapp.lecturer.data.api.LecturerApiService;
import vn.edu.fpt.zentryapp.lecturer.data.model.response.LecturerReportClassSection;
import vn.edu.fpt.zentryapp.lecturer.data.model.responsedto.ApiResponseDto;
import vn.edu.fpt.zentryapp.lecturer.data.model.responsedto.SemesterCourseDto;
import vn.edu.fpt.zentryapp.lecturer.data.model.responsedto.SemesterCoursesDataDto;

public class LecturerReportViewModel extends ViewModel {
    private final String TAG = "LecturerReportViewModel";

    // LiveData cho UI state
    private final MutableLiveData<Boolean> _isLoading = new MutableLiveData<>(false);
    private final MutableLiveData<List<LecturerReportClassSection>> _classrooms = new MutableLiveData<>();
    private final MutableLiveData<String> _errorMessage = new MutableLiveData<>();

    // API service
    private LecturerApiService apiService;
    private AuthManager authManager;

    // Public getters cho Fragment observe
    public LiveData<Boolean> isLoading() {
        return _isLoading;
    }

    public LiveData<List<LecturerReportClassSection>> classrooms() {
        return _classrooms;
    }

    public LiveData<String> errorMessage() {
        return _errorMessage;
    }

    public void init(AuthManager authManager) {
        this.authManager = authManager;
        // Initialize API service - bạn cần pass context từ Fragment
        // this.apiService = ApiClient.getClient(context).create(LecturerApiService.class);
        loadClassrooms();
    }

    public void initWithContext(android.content.Context context, AuthManager authManager) {
        this.authManager = authManager;
        this.apiService = ApiClient.getClient(context).create(LecturerApiService.class);
        loadClassrooms();
    }

    /**
     * Load classrooms data from API
     */
    public void loadClassrooms() {
        if (apiService == null) {
            Log.e(TAG, "API service not initialized. Call initWithContext() first.");
            _errorMessage.setValue("Service not initialized");
            return;
        }

        _isLoading.setValue(true);

        String lecturerId = authManager.getCurrentUserId();
        String semesterCode = "FA24"; // Có thể dynamic sau này

        Call<ApiResponseDto<SemesterCoursesDataDto>> call = apiService.getSemesterCourses(lecturerId, semesterCode);
        call.enqueue(new Callback<ApiResponseDto<SemesterCoursesDataDto>>() {
            @Override
            public void onResponse(Call<ApiResponseDto<SemesterCoursesDataDto>> call,
                                   Response<ApiResponseDto<SemesterCoursesDataDto>> response) {
                _isLoading.setValue(false);

                if (response.isSuccessful() && response.body() != null) {
                    ApiResponseDto<SemesterCoursesDataDto> apiResponse = response.body();

                    if (apiResponse.isSuccess() && apiResponse.getData() != null) {
                        processSemesterCourses(apiResponse.getData());
                    } else {
                        _errorMessage.setValue(apiResponse.getError() != null ?
                                apiResponse.getError() : "Failed to load courses");
                    }
                } else {
                    _errorMessage.setValue("Failed to load data: " + response.message());
                }
            }

            @Override
            public void onFailure(Call<ApiResponseDto<SemesterCoursesDataDto>> call, Throwable t) {
                _isLoading.setValue(false);
                _errorMessage.setValue("Network error: " + t.getMessage());
                Log.e(TAG, "API call failed", t);
            }
        });
    }

    private void processSemesterCourses(SemesterCoursesDataDto semesterData) {
        List<LecturerReportClassSection> classrooms = mapSemesterCoursesToClassSections(
                semesterData.getSemesterCourses());
        _classrooms.setValue(classrooms);
    }

    private List<LecturerReportClassSection> mapSemesterCoursesToClassSections(
            List<SemesterCourseDto> semesterCourses) {
        List<LecturerReportClassSection> result = new ArrayList<>();

        if (semesterCourses == null) return result;

        for (SemesterCourseDto course : semesterCourses) {
            // Lưu trữ đầy đủ thông tin từ API để có thể pass về sau
            result.add(new LecturerReportClassSection(
                    course.getClassId(),              // classId
                    course.getCourseName(),           // courseName
                    course.getEnrolledStudents(),     // studentCount
                    course.getCompletedSessions(),    // completedSessions
                    course.getTotalSessions(),        // totalSessions
                    course.getAttendanceRate(),       // attendancePercentage
                    course.getCourseCode() + " - " + course.getSectionCode(), // classInfo
                    "Sessions: " + course.getCompletedSessions() + "/" + course.getTotalSessions(), // scheduleInfo
                    "ACTIVE",                         // status
                    course.getCourseCode(),           // courseCode
                    course.getSectionCode(),          // sectionCode
                    course.getClassName()             // className
            ));

        }

        return result;
    }




    /**
     * Handle classroom item click
     */
    public void onClassroomClicked(LecturerReportClassSection classroom) {
        Log.d(TAG, "Classroom clicked: " + classroom.getCourseName());
    }

    /**
     * Search classrooms by name
     */
    public void searchClassrooms(String query) {
        List<LecturerReportClassSection> currentList = _classrooms.getValue();
        if (currentList == null) return;

        if (query == null || query.trim().isEmpty()) {
            loadClassrooms();
            return;
        }

        List<LecturerReportClassSection> filteredList = new ArrayList<>();
        String lowerQuery = query.toLowerCase().trim();

        for (LecturerReportClassSection classroom : currentList) {
            if (classroom.getCourseName().toLowerCase().contains(lowerQuery) ||
                    classroom.getClassInfo().toLowerCase().contains(lowerQuery)) {
                filteredList.add(classroom);
            }
        }

        _classrooms.setValue(filteredList);
    }

    /**
     * Filter by status
     */
    public void filterByStatus(String status) {
        List<LecturerReportClassSection> currentList = _classrooms.getValue();
        if (currentList == null) return;

        if (status == null || status.equals("ALL")) {
            loadClassrooms();
            return;
        }

        List<LecturerReportClassSection> filteredList = new ArrayList<>();
        for (LecturerReportClassSection classroom : currentList) {
            if (status.equals(classroom.getStatus())) {
                filteredList.add(classroom);
            }
        }

        _classrooms.setValue(filteredList);
    }
}
