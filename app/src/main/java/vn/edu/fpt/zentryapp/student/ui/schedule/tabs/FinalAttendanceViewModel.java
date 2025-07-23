package vn.edu.fpt.zentryapp.student.ui.schedule.tabs;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import android.os.Handler;

import vn.edu.fpt.zentryapp.auth.client.AuthManager;
import vn.edu.fpt.zentryapp.student.data.model.response.MyAttendance;

public class FinalAttendanceViewModel extends ViewModel {

    private final MutableLiveData<Boolean> _isLoading = new MutableLiveData<>(false);
    private final MutableLiveData<MyAttendance> _myAttendance = new MutableLiveData<>();
    private final MutableLiveData<String> _errorMessage = new MutableLiveData<>();
    private final MutableLiveData<String> _successMessage = new MutableLiveData<>();

    public LiveData<Boolean> isLoading() { return _isLoading; }
    public LiveData<MyAttendance> myAttendance() { return _myAttendance; }
    public LiveData<String> errorMessage() { return _errorMessage; }
    public LiveData<String> successMessage() { return _successMessage; }

    private AuthManager authManager;

    public void init(AuthManager authManager, String classId) {
        this.authManager = authManager;
        loadMyAttendance(classId);
    }

    public void loadMyAttendance(String classId) {
        _isLoading.setValue(true);
        _errorMessage.setValue(null);

        new Handler().postDelayed(() -> {
            try {
                MyAttendance myAttendanceData = generateMyAttendanceData();
                _myAttendance.setValue(myAttendanceData);
                _successMessage.setValue("My attendance loaded successfully");
            } catch (Exception e) {
                _errorMessage.setValue("Failed to load attendance: " + e.getMessage());
            } finally {
                _isLoading.setValue(false);
            }
        }, 1000);
    }

    private MyAttendance generateMyAttendanceData() {
        // Mock data cho attendance của student hiện tại
        return new MyAttendance(
                20,  // totalSessions
                18,  // attendedSessions
                2,   // absentSessions
                0    // lateSessions
        );
    }

    public void onAttendanceCardClicked() {
        // TODO: Handle attendance card click - có thể navigate đến chi tiết
    }
}
