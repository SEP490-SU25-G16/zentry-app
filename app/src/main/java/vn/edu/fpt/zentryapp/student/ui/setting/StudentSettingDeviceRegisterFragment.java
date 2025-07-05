package vn.edu.fpt.zentryapp.student.ui.setting;

import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import vn.edu.fpt.zentryapp.R;
import vn.edu.fpt.zentryapp.databinding.FragmentStudentSettingDeviceRegisterBinding;

public class StudentSettingDeviceRegisterFragment extends Fragment {

    private FragmentStudentSettingDeviceRegisterBinding binding;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        // Inflate layout và binding view
        binding = FragmentStudentSettingDeviceRegisterBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view,
                              @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // Xử lý nút back toolbar, gọi back của Activity
        binding.ivStudentSettingDeviceRegisterBack.setOnClickListener(v -> requireActivity().onBackPressed());

        // Xử lý nút Register để đăng ký thiết bị
        binding.btnStudentSettingDeviceRegister.setOnClickListener(v -> {
            // TODO: Gọi API đăng ký thiết bị

            // Sau khi đăng ký thành công, quay lại màn hình trước
            requireActivity().onBackPressed();
        });
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
