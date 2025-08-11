package vn.edu.fpt.zentryapp.student.ui.report;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import android.content.Context;
import android.util.Log;

import java.util.ArrayList;
import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import vn.edu.fpt.zentryapp.auth.client.ApiClient;
import vn.edu.fpt.zentryapp.auth.client.AuthManager;
import vn.edu.fpt.zentryapp.lecturer.data.model.responsedto.ApiResponseDto;
import vn.edu.fpt.zentryapp.student.data.api.StudentApiService;
import vn.edu.fpt.zentryapp.student.data.model.response.StudentReport;
import vn.edu.fpt.zentryapp.student.data.model.responsedto.StudentClassReportDto;

public class StudentReportViewModel extends ViewModel {
    private final String TAG = "StudentReportViewModel";

    private final MutableLiveData<Boolean> _isLoading = new MutableLiveData<>(false);
    private final MutableLiveData<List<StudentReport>> _reports = new MutableLiveData<>();
    private final MutableLiveData<String> _errorMessage = new MutableLiveData<>();

    // API service
    private StudentApiService apiService;
    private AuthManager authManager;
    private List<StudentReport> allReports = new ArrayList<>(); // Cache for filtering

    public LiveData<Boolean> isLoading() { return _isLoading; }
    public LiveData<List<StudentReport>> reports() { return _reports; }
    public LiveData<String> errorMessage() { return _errorMessage; }

    public void init(Context context, AuthManager authManager) {
        this.authManager = authManager;
        this.apiService = ApiClient.getClient(context).create(StudentApiService.class);
        loadReports();
    }

    public void loadReports() {
        if (Boolean.TRUE.equals(_isLoading.getValue())) {
            return;
        }

        _isLoading.setValue(true);
        _errorMessage.setValue(null);

        String studentId = authManager.getCurrentUserId();

        Call<ApiResponseDto<List<StudentClassReportDto>>> call = apiService.getStudentClasses(studentId);
        call.enqueue(new Callback<ApiResponseDto<List<StudentClassReportDto>>>() {
            @Override
            public void onResponse(Call<ApiResponseDto<List<StudentClassReportDto>>> call,
                                   Response<ApiResponseDto<List<StudentClassReportDto>>> response) {
                _isLoading.setValue(false);

                if (response.isSuccessful() && response.body() != null) {
                    ApiResponseDto<List<StudentClassReportDto>> apiResponse = response.body();

                    if (apiResponse.isSuccess() && apiResponse.getData() != null) {
                        processStudentClasses(apiResponse.getData());
                    } else {
                        _errorMessage.setValue(apiResponse.getError() != null ?
                                apiResponse.getError() : "Failed to load classes");
                    }
                } else {
                    _errorMessage.setValue("Failed to load data: " + response.message());
                }
            }

            @Override
            public void onFailure(Call<ApiResponseDto<List<StudentClassReportDto>>> call, Throwable t) {
                _isLoading.setValue(false);
                _errorMessage.setValue("Network error: " + t.getMessage());
                Log.e(TAG, "API call failed", t);
            }
        });
    }

    private void processStudentClasses(List<StudentClassReportDto> studentClasses) {
        List<StudentReport> reports = mapStudentClassesToReports(studentClasses);
        allReports = new ArrayList<>(reports); // Cache for filtering
        _reports.setValue(reports);
    }

    private List<StudentReport> mapStudentClassesToReports(List<StudentClassReportDto> studentClasses) {
        List<StudentReport> reports = new ArrayList<>();

        if (studentClasses == null) return reports;

        for (StudentClassReportDto classDto : studentClasses) {
            StudentReport report = new StudentReport(
                    classDto.getClassId(),
                    classDto.getCourseName(),
                    classDto.getCourseCode(),
                    classDto.getSectionCode(),
                    classDto.getClassName(),
                    classDto.getLecturerName(),
                    classDto.getLecturerId(),
                    classDto.getAttendanceRate()
            );

            reports.add(report);
        }

        return reports;
    }

    /**
     * Search/filter reports
     */
    public void searchReports(String query) {
        if (query == null || query.trim().isEmpty()) {
            _reports.setValue(allReports); // Show all reports
            return;
        }

        List<StudentReport> filteredReports = new ArrayList<>();
        String lowerQuery = query.toLowerCase().trim();

        for (StudentReport report : allReports) {
            if (matchesQuery(report, lowerQuery)) {
                filteredReports.add(report);
            }
        }

        _reports.setValue(filteredReports);
    }

    private boolean matchesQuery(StudentReport report, String query) {
        return report.getCourseName().toLowerCase().contains(query) ||
                report.getCourseCode().toLowerCase().contains(query) ||
                report.getSectionCode().toLowerCase().contains(query) ||
                report.getClassName().toLowerCase().contains(query) ||
                (report.getLecturerName() != null &&
                        report.getLecturerName().toLowerCase().contains(query));
    }

    /**
     * Refresh data
     */
    public void refreshReports() {
        loadReports();
    }

    /**
     * Get report by class ID
     */
    public StudentReport getReportByClassId(String classId) {
        for (StudentReport report : allReports) {
            if (report.getClassId().equals(classId)) {
                return report;
            }
        }
        return null;
    }

    /**
     * Get reports by course code
     */
    public List<StudentReport> getReportsByCourseCode(String courseCode) {
        List<StudentReport> result = new ArrayList<>();
        for (StudentReport report : allReports) {
            if (report.getCourseCode().equals(courseCode)) {
                result.add(report);
            }
        }
        return result;
    }
}
