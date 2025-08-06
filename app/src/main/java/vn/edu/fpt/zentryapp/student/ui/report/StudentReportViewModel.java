package vn.edu.fpt.zentryapp.student.ui.report;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import android.os.Handler;

import java.util.ArrayList;
import java.util.List;

import vn.edu.fpt.zentryapp.auth.client.AuthManager;
import vn.edu.fpt.zentryapp.student.data.model.response.StudentReport;

public class StudentReportViewModel extends ViewModel {

    private final MutableLiveData<Boolean> _isLoading = new MutableLiveData<>(false);
    private final MutableLiveData<List<StudentReport>> _reports = new MutableLiveData<>();
    private final MutableLiveData<String> _errorMessage = new MutableLiveData<>();

    public LiveData<Boolean> isLoading() { return _isLoading; }
    public LiveData<List<StudentReport>> reports() { return _reports; }
    public LiveData<String> errorMessage() { return _errorMessage; }

    private AuthManager authManager;

    public void init(AuthManager authManager) {
        this.authManager = authManager;
        loadReports();
    }

    public void loadReports() {
        if (Boolean.TRUE.equals(_isLoading.getValue())) {
            return;
        }

        _isLoading.setValue(true);
        _errorMessage.setValue(null);

        new Handler().postDelayed(() -> {
            try {
                List<StudentReport> mockReports = generateMockReports();
                _reports.setValue(mockReports);
            } catch (Exception e) {
                _errorMessage.setValue("Failed to load reports: " + e.getMessage());
            } finally {
                _isLoading.setValue(false);
            }
        }, 1000);
    }

    private List<StudentReport> generateMockReports() {
        List<StudentReport> reports = new ArrayList<>();

        reports.add(new StudentReport(
                "SR001",
                "Mathematics",
                "MATH101",
                "G701",
                "Hasha",
                20,
                18
        ));

        reports.add(new StudentReport(
                "SR002",
                "Science",
                "SCI101",
                "G702",
                "Gayan Iddamalgoda",
                18,
                15
        ));

        reports.add(new StudentReport(
                "SR003",
                "English",
                "ENG101",
                "G701",
                "Mary Johnson",
                16,
                16
        ));

        return reports;
    }
}
