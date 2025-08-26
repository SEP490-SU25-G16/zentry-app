package vn.edu.fpt.zentryapp.lecturer.ui.setting;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import android.os.Handler;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;
import vn.edu.fpt.zentryapp.auth.client.AuthManager;

public class LecturerSettingViewModel extends ViewModel {

    // LiveData cho UI state
    private final MutableLiveData<Boolean> _isLoading = new MutableLiveData<>(false);
    private final MutableLiveData<String> _errorMessage = new MutableLiveData<>();

    // Public getters cho Fragment observe
    public LiveData<Boolean> isLoading() {
        return _isLoading;
    }

    public LiveData<String> errorMessage() {
        return _errorMessage;
    }

    private AuthManager authManager;

    public void init(AuthManager authManager) {
        this.authManager = authManager;
    }

}
