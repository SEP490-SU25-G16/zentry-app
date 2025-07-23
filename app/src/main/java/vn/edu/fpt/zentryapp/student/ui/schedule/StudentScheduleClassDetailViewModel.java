package vn.edu.fpt.zentryapp.student.ui.schedule;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import android.os.Handler;

import vn.edu.fpt.zentryapp.auth.client.AuthManager;
import vn.edu.fpt.zentryapp.student.data.model.response.ClassDetail;

public class StudentScheduleClassDetailViewModel extends ViewModel {

    private final MutableLiveData<Boolean> _isLoading = new MutableLiveData<>(false);
    private final MutableLiveData<ClassDetail> _classDetail = new MutableLiveData<>();
    private final MutableLiveData<String> _errorMessage = new MutableLiveData<>();
    private final MutableLiveData<String> _successMessage = new MutableLiveData<>();

    public LiveData<Boolean> isLoading() { return _isLoading; }
    public LiveData<ClassDetail> classDetail() { return _classDetail; }
    public LiveData<String> errorMessage() { return _errorMessage; }
    public LiveData<String> successMessage() { return _successMessage; }

    private AuthManager authManager;

    public void init(AuthManager authManager, String classId) {
        this.authManager = authManager;
        loadClassDetail(classId);
    }

    public void loadClassDetail(String classId) {
        if (Boolean.TRUE.equals(_isLoading.getValue())) {
            return;
        }

        _isLoading.setValue(true);
        _errorMessage.setValue(null);

        new Handler().postDelayed(() -> {
            try {
                ClassDetail mockClassDetail = generateMockClassDetail(classId);
                _classDetail.setValue(mockClassDetail);
                _successMessage.setValue("Class detail loaded successfully");
            } catch (Exception e) {
                _errorMessage.setValue("Failed to load class detail: " + e.getMessage());
            } finally {
                _isLoading.setValue(false);
            }
        }, 1000);
    }

    private ClassDetail generateMockClassDetail(String classId) {
        return new ClassDetail(
                classId,
                "Mathematics Class",
                "Grade 07",
                "Mathematics",
                "2h 30m",
                "00:14:50",
                "Nguyễn Văn A",
                "BE-201",
                "Monday, Wednesday, Friday 8:00 AM - 10:30 AM",
                25,
                23
        );
    }

    public void onAddClicked() {
        // TODO: Handle add action
    }

    public void onNotificationClicked() {
        // TODO: Handle notification action
    }
}
