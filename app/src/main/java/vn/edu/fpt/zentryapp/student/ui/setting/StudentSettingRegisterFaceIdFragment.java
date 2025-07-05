package vn.edu.fpt.zentryapp.student.ui.setting;

import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import vn.edu.fpt.zentryapp.R;
import vn.edu.fpt.zentryapp.databinding.FragmentStudentSettingRegisterFaceIdBinding;

public class StudentSettingRegisterFaceIdFragment extends Fragment {

    private FragmentStudentSettingRegisterFaceIdBinding binding;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        binding = FragmentStudentSettingRegisterFaceIdBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view,
                              @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // Xử lý nút back toolbar, gọi back của Activity
        binding.ivStudentSettingRegisterFaceIdBack.setOnClickListener(v -> requireActivity().onBackPressed());

        // Xử lý nút Register để đăng ký Face ID
        binding.btnStudentSettingRegisterFaceId.setOnClickListener(v -> {
            // TODO: Gọi API hoặc mở chức năng đăng ký Face ID

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
