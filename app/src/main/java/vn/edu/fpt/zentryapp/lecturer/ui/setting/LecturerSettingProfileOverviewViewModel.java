package vn.edu.fpt.zentryapp.lecturer.ui.setting;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import android.content.Context;
import android.util.Log;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import vn.edu.fpt.zentryapp.auth.client.ApiClient;
import vn.edu.fpt.zentryapp.auth.client.AuthManager;
import vn.edu.fpt.zentryapp.lecturer.data.api.LecturerApiService;
import vn.edu.fpt.zentryapp.lecturer.data.model.response.UserProfile;
import vn.edu.fpt.zentryapp.lecturer.data.model.responsedto.ApiResponseDto;
import vn.edu.fpt.zentryapp.lecturer.data.model.responsedto.UserDto;

public class LecturerSettingProfileOverviewViewModel extends ViewModel {
    private final String TAG = "LecturerProfileViewModel";

    private final MutableLiveData<Boolean> _isLoading = new MutableLiveData<>(false);
    private final MutableLiveData<UserProfile> _userProfile = new MutableLiveData<>();
    private final MutableLiveData<String> _errorMessage = new MutableLiveData<>();

    // API service
    private LecturerApiService apiService;
    private AuthManager authManager;

    public LiveData<Boolean> isLoading() { return _isLoading; }
    public LiveData<UserProfile> userProfile() { return _userProfile; }
    public LiveData<String> errorMessage() { return _errorMessage; }

    public void init(Context context, AuthManager authManager) {
        this.authManager = authManager;
        this.apiService = ApiClient.getClient(context).create(LecturerApiService.class);
        loadUserProfile();
    }

    public void loadUserProfile() {
        _isLoading.setValue(true);
        _errorMessage.setValue(null);

        String userId = authManager.getCurrentUserId();

        Call<ApiResponseDto<UserDto>> call = apiService.getUserProfile(userId);
        call.enqueue(new Callback<ApiResponseDto<UserDto>>() {
            @Override
            public void onResponse(Call<ApiResponseDto<UserDto>> call,
                                   Response<ApiResponseDto<UserDto>> response) {
                _isLoading.setValue(false);

                if (response.isSuccessful() && response.body() != null) {
                    ApiResponseDto<UserDto> apiResponse = response.body();

                    if (apiResponse.isSuccess() && apiResponse.getData() != null) {
                        UserProfile profile = mapUserDtoToUserProfile(apiResponse.getData());
                        _userProfile.setValue(profile);
                    } else {
                        _errorMessage.setValue(apiResponse.getError() != null ?
                                apiResponse.getError() : "Failed to load profile");
                    }
                } else {
                    _errorMessage.setValue("Failed to load profile: " + response.message());
                }
            }

            @Override
            public void onFailure(Call<ApiResponseDto<UserDto>> call, Throwable t) {
                _isLoading.setValue(false);
                _errorMessage.setValue("Network error: " + t.getMessage());
                Log.e(TAG, "API call failed", t);
            }
        });
    }

    private UserProfile mapUserDtoToUserProfile(UserDto userDto) {
        UserProfile profile = new UserProfile();
        profile.setUserId(userDto.getUserId());
        profile.setAccountId(userDto.getAccountId());
        profile.setEmail(userDto.getEmail());
        profile.setFullName(userDto.getFullName());
        profile.setPhoneNumber(userDto.getPhoneNumber());
        profile.setRole(userDto.getRole());
        profile.setStatus(userDto.getStatus());
        profile.setCreatedAt(userDto.getCreatedAt());
        profile.setHasFaceId(userDto.isHasFaceId());
        profile.setFaceIdLastUpdated(userDto.getFaceIdLastUpdated());

        return profile;
    }

    public void refreshProfile() {
        loadUserProfile();
    }
}
