package vn.edu.fpt.zentryapp.student.ui.setting;

import android.content.Context;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import vn.edu.fpt.zentryapp.auth.client.AuthManager;
import vn.edu.fpt.zentryapp.databinding.FragmentStudentSettingDeviceInfoBinding;
import vn.edu.fpt.zentryapp.service.DeviceApiService;
import vn.edu.fpt.zentryapp.service.DeviceInfoHelper;
import vn.edu.fpt.zentryapp.student.data.model.response.DeviceChangeRequestBody;
import vn.edu.fpt.zentryapp.student.data.model.response.DeviceInfoResponse;
import vn.edu.fpt.zentryapp.auth.client.ApiClient;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class StudentSettingDeviceInfoFragment extends Fragment {

    private FragmentStudentSettingDeviceInfoBinding binding;
    private final String TAG = "StudentSettingDeviceInfoFragment";
    private AuthManager authManager;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = FragmentStudentSettingDeviceInfoBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        authManager = AuthManager.getInstance(requireContext());

        setupClickListeners();
        loadDeviceInfoFromApi();
    }

    private void setupClickListeners() {
        // Back button
        binding.ivStudentSettingDeviceInfoBack.setOnClickListener(v -> requireActivity().onBackPressed());

        // Nút Request change
        binding.btnStudentSettingDeviceInfoUpdate.setOnClickListener(v -> requestChangeDevice());
    }

    private void loadDeviceInfoFromApi() {
        String deviceId = authManager.getDeviceId();
        if (deviceId == null) {
            Toast.makeText(requireContext(), "Device ID not found", Toast.LENGTH_LONG).show();
            binding.btnStudentSettingDeviceInfoUpdate.setEnabled(false);
            return;
        }

        DeviceApiService api = ApiClient.getClient(requireContext()).create(DeviceApiService.class);

        api.getDeviceInfo(deviceId).enqueue(new Callback<DeviceInfoResponse>() {
            @Override
            public void onResponse(@NonNull Call<DeviceInfoResponse> call, @NonNull Response<DeviceInfoResponse> response) {
                if (response.isSuccessful() && response.body() != null && response.body().Success && response.body().Data != null) {
                    DeviceInfoResponse.DeviceInfo info = response.body().Data;

                    // HIỂN THỊ LÊN UI
                    binding.tvStudentSettingDeviceInfoNameValue.setText(info.DeviceName);
                    binding.tvStudentSettingDeviceInfoDateValue.setText(formatDate(info.CreatedAt));
                    binding.tvDeviceModelValue.setText(info.Model);
                    binding.tvDeviceManufacturerValue.setText(info.Manufacturer);
                    binding.tvDevicePlatformValue.setText(info.Platform);
                    binding.tvDeviceOsVersionValue.setText(info.OsVersion);
                    binding.tvDeviceAppVersionValue.setText(info.AppVersion);

                    // SO SÁNH DeviceToken
                    String deviceTokenLocal = authManager.getDeviceToken();
                    Log.d(TAG, "localDeviceToken: " + deviceTokenLocal);
                    Log.d(TAG, "apiDeviceToken: " + info.DeviceToken);

                    boolean tokenMatch = info.DeviceToken != null && info.DeviceToken.equals(deviceTokenLocal);

                    // Xử lý status Pending
                    if ("Pending".equalsIgnoreCase(info.Status)) {
                        binding.btnStudentSettingDeviceInfoUpdate.setEnabled(false);
                        binding.btnStudentSettingDeviceInfoUpdate.setText("Change request sent (Pending review)");
                        Toast.makeText(requireContext(), "Change request has been sent and pending approval.", Toast.LENGTH_SHORT).show();
                    } else if (!tokenMatch) {
                        binding.btnStudentSettingDeviceInfoUpdate.setEnabled(true);
                        binding.btnStudentSettingDeviceInfoUpdate.setText("Request change");
                    } else {
                        binding.btnStudentSettingDeviceInfoUpdate.setEnabled(false);
                        binding.btnStudentSettingDeviceInfoUpdate.setText("Device synced");
                    }
                } else {
                    Toast.makeText(requireContext(), "Cannot load device info.", Toast.LENGTH_LONG).show();
                }
            }

            @Override
            public void onFailure(@NonNull Call<DeviceInfoResponse> call, @NonNull Throwable t) {
                Toast.makeText(requireContext(), "API error: " + t.getMessage(), Toast.LENGTH_LONG).show();
            }
        });
    }

    private String formatDate(String apiString) {
        try {
            // VD input: "2025-08-18 21:07:49"
            SimpleDateFormat sdfApi = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US);
            Date date = sdfApi.parse(apiString);
            SimpleDateFormat sdfUi = new SimpleDateFormat("MMM dd, yyyy / HH:mm", Locale.getDefault());
            return date == null ? apiString : sdfUi.format(date);
        } catch (ParseException e) {
            return apiString != null ? apiString : "";
        }
    }

    private void requestChangeDevice() {
        String userId = authManager.getCurrentUserId();
        Context context = requireContext();

        // ✅ LẤY TẤT CẢ DỮ LIỆU TỪ HARDWARE (DeviceInfoHelper)
        String deviceName = DeviceInfoHelper.getDeviceName();
        String androidId = DeviceInfoHelper.getAndroidId(context);
        String platform = DeviceInfoHelper.getPlatform();
        String osVersion = DeviceInfoHelper.getOsVersion();
        String model = DeviceInfoHelper.getModel();
        String manufacturer = DeviceInfoHelper.getManufacturer();
        String appVersion = DeviceInfoHelper.getAppVersion(context);
        String pushToken = DeviceInfoHelper.generatePushNotificationToken(context); // Generate new token
        String reason = "Thiết bị cũ bị hỏng và cần thay thế bằng thiết bị mới.";

        Log.d(TAG, "Requesting device change with hardware data:");
        Log.d(TAG, "deviceName: " + deviceName);
        Log.d(TAG, "androidId: " + androidId);
        Log.d(TAG, "platform: " + platform);
        Log.d(TAG, "pushToken: " + pushToken);

        DeviceApiService api = ApiClient.getClient(requireContext()).create(DeviceApiService.class);

        DeviceChangeRequestBody requestBody = new DeviceChangeRequestBody(
                userId, reason, deviceName, androidId, platform,
                osVersion, model, manufacturer, appVersion, pushToken
        );

        // Disable button và show loading
        binding.btnStudentSettingDeviceInfoUpdate.setEnabled(false);
        binding.btnStudentSettingDeviceInfoUpdate.setText("Processing...");

        api.requestChangeDevice(requestBody).enqueue(new Callback<Void>() {
            @Override
            public void onResponse(@NonNull Call<Void> call, @NonNull Response<Void> response) {
                if (response.isSuccessful()) {
                    Toast.makeText(requireContext(), "Change request has been sent!", Toast.LENGTH_SHORT).show();

                    // ✅ Sau khi gửi thành công, cập nhật device token local để match với token mới
                    authManager.saveDeviceData(authManager.getDeviceId(), pushToken);

                    // Reload thông tin device từ API
                    loadDeviceInfoFromApi();
                } else {
                    Toast.makeText(requireContext(), "Request failed: " + response.code(), Toast.LENGTH_LONG).show();
                    binding.btnStudentSettingDeviceInfoUpdate.setEnabled(true);
                    binding.btnStudentSettingDeviceInfoUpdate.setText("Request change");
                }
            }

            @Override
            public void onFailure(@NonNull Call<Void> call, @NonNull Throwable t) {
                Toast.makeText(requireContext(), "Request error: " + t.getMessage(), Toast.LENGTH_LONG).show();
                binding.btnStudentSettingDeviceInfoUpdate.setEnabled(true);
                binding.btnStudentSettingDeviceInfoUpdate.setText("Request change");
            }
        });
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
