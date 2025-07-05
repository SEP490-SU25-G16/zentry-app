package vn.edu.fpt.zentryapp.student.ui.setting;

import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import vn.edu.fpt.zentryapp.R;
import vn.edu.fpt.zentryapp.databinding.FragmentStudentSettingUpdateFaceIdBinding;

public class StudentSettingUpdateFaceIdFragment extends Fragment {

    private FragmentStudentSettingUpdateFaceIdBinding binding;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        binding = FragmentStudentSettingUpdateFaceIdBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view,
                              @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // Xử lý nút back toolbar, gọi back của Activity
        binding.ivBack.setOnClickListener(v -> requireActivity().onBackPressed());

        // TODO: Hiển thị avatar Face ID và thời gian cập nhật lần cuối
        // Ví dụ:
        // binding.ivFaceAvatar.setImageBitmap(...);
        // binding.tvLastUpdate.setText("Last update: JAN 27, 2025   /   11:45");

        // Xử lý nút Update để cập nhật Face ID
        binding.btnUpdateFace.setOnClickListener(v -> {
            // TODO: Gọi API hoặc mở chức năng cập nhật Face ID

            // Sau khi cập nhật thành công, cập nhật UI hoặc quay lại màn hình trước
        });
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
