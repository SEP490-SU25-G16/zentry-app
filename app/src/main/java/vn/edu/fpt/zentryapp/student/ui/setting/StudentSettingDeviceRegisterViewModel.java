package vn.edu.fpt.zentryapp.student.ui.setting;

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
import vn.edu.fpt.zentryapp.service.AttendanceApiService;
import vn.edu.fpt.zentryapp.student.data.model.request.DeviceRegistrationRequest;
import vn.edu.fpt.zentryapp.student.data.model.response.DeviceRegistrationResponse;
import vn.edu.fpt.zentryapp.service.DeviceInfoHelper;

public class StudentSettingDeviceRegisterViewModel extends ViewModel {
    private static final String TAG = "DeviceRegisterVM";

    private final MutableLiveData<Boolean> _isLoading = new MutableLiveData<>(false);
    private final MutableLiveData<String> _successMessage = new MutableLiveData<>();
    private final MutableLiveData<String> _errorMessage = new MutableLiveData<>();
    private final MutableLiveData<Boolean> _isDeviceRegistered = new MutableLiveData<>(false);

    private AttendanceApiService apiService;
    private AuthManager authManager;

    // Public getters
    public LiveData<Boolean> isLoading() { return _isLoading; }
    public LiveData<String> successMessage() { return _successMessage; }
    public LiveData<String> errorMessage() { return _errorMessage; }
    public LiveData<Boolean> isDeviceRegistered() { return _isDeviceRegistered; }

    public void init(Context context, AuthManager authManager) {
        this.authManager = authManager;
        this.apiService = ApiClient.getClient(context).create(AttendanceApiService.class);

        // Check if device is already registered
        _isDeviceRegistered.setValue(authManager.isDeviceRegistered());
    }

    /**
     * Register device with server
     */
    public void registerDevice(Context context) {
        if (authManager == null || !authManager.isLoggedIn()) {
            _errorMessage.setValue("User not logged in");
            return;
        }

        _isLoading.setValue(true);
        _errorMessage.setValue(null);
        _successMessage.setValue(null);

        Log.d(TAG, "Starting device registration...");

        // Create registration request
        DeviceRegistrationRequest request = createRegistrationRequest(context);

        Log.d(TAG, "Registration request created:");
        Log.d(TAG, "  User ID: " + request.getUserId());
        Log.d(TAG, "  MAC Address: " + request.getAndroidId());
        Log.d(TAG, "  Device Name: " + request.getDeviceName());
        Log.d(TAG, "  Platform: " + request.getPlatform());

        // Call API
        apiService.registerDevice(request)
                .enqueue(new Callback<DeviceRegistrationResponse>() {
                    @Override
                    public void onResponse(Call<DeviceRegistrationResponse> call,
                                           Response<DeviceRegistrationResponse> response) {
                        _isLoading.setValue(false);

                        if (response.isSuccessful() && response.body() != null) {
                            DeviceRegistrationResponse apiResponse = response.body();

                            if (apiResponse.isSuccess() && apiResponse.getData() != null) {
                                // Save device data to AuthManager
                                authManager.saveDeviceData(
                                        apiResponse.getData().getDeviceId(),
                                        apiResponse.getData().getDeviceToken()
                                );

                                _isDeviceRegistered.setValue(true);
                                _successMessage.setValue("Device registered successfully");

                                Log.d(TAG, "✅ Device registered successfully");
                            } else {
                                String error = apiResponse.getError() != null ? apiResponse.getError() : "Registration failed";
                                _errorMessage.setValue(error);
                                Log.e(TAG, "❌ Registration API Error: " + error);
                            }
                        } else {
                            String error = "HTTP Error: " + response.code();
                            _errorMessage.setValue(error);
                            Log.e(TAG, "❌ Registration " + error);
                        }
                    }

                    @Override
                    public void onFailure(Call<DeviceRegistrationResponse> call, Throwable t) {
                        _isLoading.setValue(false);
                        String error = "Network Error: " + t.getMessage();
                        _errorMessage.setValue(error);
                        Log.e(TAG, "❌ Registration Network Error", t);
                    }
                });
    }

    /**
     * Create registration request from device info
     */
    private DeviceRegistrationRequest createRegistrationRequest(Context context) {
        DeviceRegistrationRequest request = new DeviceRegistrationRequest();

        request.setUserId(authManager.getCurrentUserId());
        request.setAndroidId(DeviceInfoHelper.getAndroidId(context));
        request.setDeviceName(DeviceInfoHelper.getDeviceName());
        request.setPlatform(DeviceInfoHelper.getPlatform());
        request.setOsVersion(DeviceInfoHelper.getOsVersion());
        request.setModel(DeviceInfoHelper.getModel());
        request.setManufacturer(DeviceInfoHelper.getManufacturer());
        request.setAppVersion(DeviceInfoHelper.getAppVersion(context));
        request.setPushNotificationToken(DeviceInfoHelper.generatePushNotificationToken(context));

        return request;
    }
}
