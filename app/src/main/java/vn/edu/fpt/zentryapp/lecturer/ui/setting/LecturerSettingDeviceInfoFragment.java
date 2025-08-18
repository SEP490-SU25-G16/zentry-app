package vn.edu.fpt.zentryapp.lecturer.ui.setting;

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

import com.google.protobuf.Api;

import vn.edu.fpt.zentryapp.auth.client.AuthManager;
import vn.edu.fpt.zentryapp.databinding.FragmentLecturerSettingDeviceInfoBinding;
import vn.edu.fpt.zentryapp.auth.client.ApiClient;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import vn.edu.fpt.zentryapp.service.DeviceApiService;
import vn.edu.fpt.zentryapp.service.DeviceInfoHelper;
import vn.edu.fpt.zentryapp.student.data.model.response.DeviceChangeRequestBody;
import vn.edu.fpt.zentryapp.student.data.model.response.DeviceInfoResponse;

public class LecturerSettingDeviceInfoFragment extends Fragment {

    private FragmentLecturerSettingDeviceInfoBinding binding;
    private final String TAG = "LecturerSettingDeviceInfoFragment";
    private AuthManager authManager;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        binding = FragmentLecturerSettingDeviceInfoBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view,
                              @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        authManager = AuthManager.getInstance(requireContext());

        binding.ivDeviceInfoBack.setOnClickListener(v -> requireActivity().onBackPressed());

        loadDeviceInfoFromApi();

        binding.btnDeviceInfoUpdate.setOnClickListener(v -> requestChangeDevice());
    }

    private void loadDeviceInfoFromApi() {
        String deviceId = authManager.getDeviceId();
        if (deviceId == null) {
            Toast.makeText(requireContext(), "Device ID not found", Toast.LENGTH_LONG).show();
            binding.btnDeviceInfoUpdate.setEnabled(false);
            return;
        }

        DeviceApiService api = ApiClient.getClient(requireContext()).create(DeviceApiService.class);
        api.getDeviceInfo(deviceId).enqueue(new Callback<DeviceInfoResponse>() {
            @Override
            public void onResponse(@NonNull Call<DeviceInfoResponse> call, @NonNull Response<DeviceInfoResponse> response) {
                if (response.isSuccessful() && response.body() != null && response.body().Success && response.body().Data != null) {
                    DeviceInfoResponse.DeviceInfo info = response.body().Data;
                    binding.tvDeviceInfoNameValue.setText(info.DeviceName);
                    binding.tvDeviceInfoDateValue.setText(formatDate(info.CreatedAt));
                    binding.tvDeviceModelValue.setText(info.Model);
                    binding.tvDeviceManufacturerValue.setText(info.Manufacturer);
                    binding.tvDevicePlatformValue.setText(info.Platform);
                    binding.tvDeviceOsVersionValue.setText(info.OsVersion);
                    binding.tvDeviceAppVersionValue.setText(info.AppVersion);

                    String localDeviceToken = authManager.getDeviceToken();
                    Log.d(TAG, "localDeviceToken " + localDeviceToken);
                    Log.d(TAG, "DeviceToken " + info.DeviceToken);

                    boolean tokenMatch = info.DeviceToken != null && info.DeviceToken.equals(localDeviceToken);

                    if ("Pending".equalsIgnoreCase(info.Status)) {
                        // Đang chờ duyệt chuyển đổi, show thông báo
                        binding.btnDeviceInfoUpdate.setEnabled(false);
                        binding.btnDeviceInfoUpdate.setText("Change request sent (Pending review)");
                        Toast.makeText(requireContext(), "Change request has been sent and pending approval.", Toast.LENGTH_SHORT).show();
                    } else if (!tokenMatch) {
                        binding.btnDeviceInfoUpdate.setEnabled(true);
                        binding.btnDeviceInfoUpdate.setText("Request change");
                    } else {
                        binding.btnDeviceInfoUpdate.setEnabled(false);
                        binding.btnDeviceInfoUpdate.setText("Device synced");
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

        // Lấy trực tiếp từ hardware/OS
        String deviceName = DeviceInfoHelper.getDeviceName();
        String androidId = DeviceInfoHelper.getAndroidId(context);
        String platform = DeviceInfoHelper.getPlatform();
        String osVersion = DeviceInfoHelper.getOsVersion();
        String model = DeviceInfoHelper.getModel();
        String manufacturer = DeviceInfoHelper.getManufacturer();
        String appVersion = DeviceInfoHelper.getAppVersion(context);
        // Token sử dụng token app hiện tại đang lưu, thường cập nhật khi login/device register
        String pushToken = authManager.getDeviceToken();
        String reason = "Thiết bị cũ cần thay thế bằng thiết bị mới.";

        // Tạo requestBody
        DeviceChangeRequestBody requestBody = new DeviceChangeRequestBody(
                userId, reason, deviceName, androidId, platform,
                osVersion, model, manufacturer, appVersion, pushToken
        );
        binding.btnDeviceInfoUpdate.setEnabled(false);
        binding.btnDeviceInfoUpdate.setText("Processing...");

        ApiClient.getClient(requireContext()).create(DeviceApiService.class).requestChangeDevice(requestBody).enqueue(new Callback<Void>() {
            @Override
            public void onResponse(@NonNull Call<Void> call, @NonNull Response<Void> response) {
                if (response.isSuccessful()) {
                    Toast.makeText(requireContext(), "Change request has been sent!", Toast.LENGTH_SHORT).show();
                    loadDeviceInfoFromApi();
                } else {
                    Toast.makeText(requireContext(), "Request failed: " + response.code(), Toast.LENGTH_LONG).show();
                    binding.btnDeviceInfoUpdate.setEnabled(true);
                    binding.btnDeviceInfoUpdate.setText("Request change");
                }
            }
            @Override
            public void onFailure(@NonNull Call<Void> call, @NonNull Throwable t) {
                Toast.makeText(requireContext(), "Request error: " + t.getMessage(), Toast.LENGTH_LONG).show();
                binding.btnDeviceInfoUpdate.setEnabled(true);
                binding.btnDeviceInfoUpdate.setText("Request change");
            }
        });
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}