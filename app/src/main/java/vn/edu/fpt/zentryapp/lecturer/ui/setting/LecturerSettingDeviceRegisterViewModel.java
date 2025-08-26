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
import vn.edu.fpt.zentryapp.service.AttendanceApiService;
import vn.edu.fpt.zentryapp.student.data.model.request.DeviceRegistrationRequest;
import vn.edu.fpt.zentryapp.student.data.model.response.DeviceRegistrationResponse;
import vn.edu.fpt.zentryapp.service.DeviceInfoHelper;

public class LecturerSettingDeviceRegisterViewModel extends ViewModel {
    private static final String TAG = "LecturerDeviceRegisterVM";

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
     * Register device with server for lecturer
     */
    public void registerDevice(Context context) {
        if (authManager == null || !authManager.isLoggedIn()) {
            _errorMessage.setValue("User not logged in");
            return;
        }

        // Verify user is lecturer
        if (!authManager.isLecturer()) {
            _errorMessage.setValue("Only lecturers can register devices");
            return;
        }

        _isLoading.setValue(true);
        _errorMessage.setValue(null);
        _successMessage.setValue(null);

        Log.d(TAG, "Starting lecturer device registration...");

        // Create registration request
        DeviceRegistrationRequest request = createRegistrationRequest(context);

        Log.d(TAG, "Lecturer registration request created:");
        Log.d(TAG, "  User ID: " + request.getUserId());
        Log.d(TAG, "  Android Id: " + request.getAndroidId());
        Log.d(TAG, "  Device Name: " + request.getDeviceName());
        Log.d(TAG, "  Platform: " + request.getPlatform());
        Log.d(TAG, "  User Role: " + authManager.getCurrentUserRole());

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
                                _successMessage.setValue("Lecturer device registered successfully");

                                Log.d(TAG, "✅ Lecturer device registered successfully");
                                Log.d(TAG, "  Device ID: " + apiResponse.getData().getDeviceId());
                                Log.d(TAG, "  Device Token: " + apiResponse.getData().getDeviceToken());
                            } else {
                                handleHttpError(response.code());                            }
                        } else {
                            String error = "HTTP Error: " + response.code();
                            _errorMessage.setValue(error);
                            Log.e(TAG, "❌ Lecturer Registration " + error);
                        }
                    }

                    @Override
                    public void onFailure(Call<DeviceRegistrationResponse> call, Throwable t) {
                        _isLoading.setValue(false);
                        String error = "Network Error: " + t.getMessage();
                        _errorMessage.setValue(error);
                        Log.e(TAG, "❌ Lecturer Registration Network Error", t);
                    }
                });
    }
    private void handleHttpError(int responseCode) {
        String errorMessage;

        switch (responseCode) {
            case 409:
                errorMessage = "This device is already registered with another account. Please contact support if this is your device.";
                Log.e(TAG, "❌ HTTP 409: Device already registered to another user");
                break;
            case 400:
                errorMessage = "Invalid device information. Please try again.";
                Log.e(TAG, "❌ HTTP 400: Bad request");
                break;
            case 401:
                errorMessage = "Authentication failed. Please login again.";
                Log.e(TAG, "❌ HTTP 401: Unauthorized");
                break;
            case 403:
                errorMessage = "Access denied. You don't have permission to register devices.";
                Log.e(TAG, "❌ HTTP 403: Forbidden");
                break;
            case 500:
                errorMessage = "Server error. Please try again later.";
                Log.e(TAG, "❌ HTTP 500: Internal server error");
                break;
            default:
                errorMessage = "Registration failed with error code: " + responseCode + ". Please try again.";
                Log.e(TAG, "❌ HTTP " + responseCode + ": Unknown error");
                break;
        }

        _errorMessage.setValue(errorMessage);
    }
    /**
     * Create registration request from device info for lecturer
     */
    private DeviceRegistrationRequest createRegistrationRequest(Context context) {
        DeviceRegistrationRequest request = new DeviceRegistrationRequest();

        request.setUserId(authManager.getCurrentUserId());
        request.setAndroidId(DeviceInfoHelper.getAndroidId(context));
        // Enhanced device name for lecturer
        String baseName = DeviceInfoHelper.getDeviceName();
        request.setDeviceName("Lecturer " + baseName);

        request.setPlatform(DeviceInfoHelper.getPlatform());
        request.setOsVersion(DeviceInfoHelper.getOsVersion());
        request.setModel(DeviceInfoHelper.getModel());
        request.setManufacturer(DeviceInfoHelper.getManufacturer());
        request.setAppVersion(DeviceInfoHelper.getAppVersion(context));
        request.setPushNotificationToken(DeviceInfoHelper.generatePushNotificationToken(context));

        return request;
    }
}
