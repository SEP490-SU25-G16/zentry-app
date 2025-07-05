package vn.edu.fpt.zentryapp.lecturer.ui.setting;

import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import vn.edu.fpt.zentryapp.R;
import vn.edu.fpt.zentryapp.databinding.FragmentLecturerSettingDeviceInfoBinding;

public class LecturerSettingDeviceInfoFragment extends Fragment {

    private FragmentLecturerSettingDeviceInfoBinding binding;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        // Inflate layout và binding view
        binding = FragmentLecturerSettingDeviceInfoBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view,
                              @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // Xử lý nút back toolbar, gọi back của Activity
        binding.ivDeviceInfoBack.setOnClickListener(v -> requireActivity().onBackPressed());

        // TODO: Hiển thị thông tin thiết bị lấy từ args, ViewModel hoặc API
        // Ví dụ:
        // binding.tvDeviceInfoNameValue.setText(deviceName);
        // binding.tvDeviceInfoIdValue.setText(deviceId);
        // binding.tvDeviceInfoDateValue.setText(deviceDate);

        // Xử lý nút Update để cập nhật thông tin thiết bị
        binding.btnDeviceInfoUpdate.setOnClickListener(v -> {
            // TODO: Gọi API cập nhật thông tin thiết bị

            // Sau khi update thành công, quay lại màn hình trước
            requireActivity().onBackPressed();
        });
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
